// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.internal.broker

import android.webkit.JavascriptInterface
import com.google.gson.GsonBuilder
import com.microsoft.identity.common.adal.internal.AuthenticationConstants
import com.microsoft.identity.common.internal.numberMatch.NumberMatchHelper
import com.microsoft.identity.common.internal.ui.webview.authux.AuthUxBridgeTelemetry
import com.microsoft.identity.common.internal.ui.webview.authux.AuthUxMeasurementObserver.BridgeCounter
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.opentelemetry.SpanExtension
import com.microsoft.identity.common.logging.Logger
import java.net.URI
import java.net.URISyntaxException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Immutable context for a single Auth UX `log_telemetry` message.
 *
 * @property correlationId Page-reported correlation ID used for telemetry correlation.
 * @property errorCode Non-empty error code validated by the bridge.
 * @property sessionId Optional Auth UX session identifier.
 * @property pageId Optional Auth UX page identifier.
 * @property trackingId Optional Auth UX tracking identifier.
 * @property version Optional telemetry contract version, retained as reported by the page.
 */
data class AuthUxTelemetryEvent(
    val correlationId: String,
    val errorCode: String,
    val sessionId: String? = null,
    val pageId: String? = null,
    val trackingId: String? = null,
    val version: String? = null
)

/**
 * Host-provided destination for validated Auth UX telemetry events from the JavaScript bridge.
 *
 * A sink is the terminal callback that consumes an event and applies the host-owned telemetry side
 * effect. The bridge remains responsible only for parsing, validation, and delivery.
 *
 * Called synchronously on the active transport's callback thread: the WebView JavaBridge thread
 * for `addJavascriptInterface`, or the UI thread for WebMessageListener. Implementations must be
 * non-blocking and make shared telemetry state thread-safe.
 */
fun interface AuthUxTelemetrySink {
    /**
     * @param event Validated telemetry context supplied by the page.
     * @return `true` if the event was handled, including deliberate policy exclusion; `false` if
     *  the sink is not ready. Returning `false` does not suppress later occurrences.
     *  Implementations should report a not-ready state with `false` rather than throwing.
     */
    fun tryConsumeAuthUxTelemetry(event: AuthUxTelemetryEvent): Boolean
}

/**
 * JavaScript bridge for Auth UX messages handled by the broker.
 *
 * @property telemetrySink Optional host callback that consumes validated
 *  [ActionNames.LOG_TELEMETRY] events. When absent, telemetry messages are parsed and validated
 *  without producing a side effect.
 */
class AuthUxJavaScriptInterface @JvmOverloads constructor(
    private val telemetrySink: AuthUxTelemetrySink? = null,
    private val capabilities: AuthUxRequestCapabilities = AuthUxRequestCapabilities.BROKER
) : TelemetryAwareAuthUxMessageProcessor {
    @Volatile
    private var bridgeTelemetry: AuthUxBridgeTelemetry = AuthUxBridgeTelemetry.NONE

    // Serializes callbacks with close() so teardown waits for in-flight side effects and rejects queued work.
    private val lifecycleLock = Any()
    private val closed = AtomicBoolean(false)

    override val isClosed: Boolean
        get() = closed.get()

    // Store number matches in a static hash map
    // No need to persist this storage beyond the current broker process, but we need to keep them
    // long enough for AuthApp to call the broker api to fetch the number match
    companion object {
        val TAG = AuthUxJavaScriptInterface::class.java.simpleName
        private const val JAVASCRIPT_INTERFACE_NAME = "broker"

        /** Attaches bridge telemetry before publishing the processor, preserving existing constructors. */
        @JvmStatic
        fun createWithTelemetry(
            telemetrySink: AuthUxTelemetrySink?,
            capabilities: AuthUxRequestCapabilities,
            bridgeTelemetry: AuthUxBridgeTelemetry
        ): AuthUxJavaScriptInterface = AuthUxJavaScriptInterface(telemetrySink, capabilities).also {
            it.bridgeTelemetry = bridgeTelemetry
        }

        /**
         * Accepted shape for a `log_telemetry` error code.
         *
         * Supports numeric STS codes and symbolic recorder constants while rejecting whitespace,
         * control characters, and unbounded values. This validates only the wire shape; semantic
         * qualification for a specific telemetry consumer belongs to its [AuthUxTelemetrySink].
         */
        private val ERROR_CODE_REGEX = Regex("^[A-Za-z0-9_-]{1,32}$")

        fun getInterfaceName(): String {
            return JAVASCRIPT_INTERFACE_NAME
        }

        /**
         * Helper method to determine if uri is a valid Uri for the JS Interface
         * @param uriString uri being loaded
         * @return true if uri is a valid, safe uri, false otherwise
         */
        fun isValidUriForInterface(uriString: String?): Boolean {
            // If uri is null or empty, return false
            if (uriString.isNullOrEmpty()) {
                return false
            }

            val uri: URI
            try {
                uri = URI(uriString)
            } catch (e: URISyntaxException) {
                Logger.warn(TAG, "URI syntax validation failed for the Auth UX JavaScript interface.")
                return false
            }

            return isValidHostForInterface(uri.host)
        }

        /**
         * Checks [host] using the legacy case-sensitive Auth UX host suffix comparison.
         *
         * Callers must obtain [host] from strict [URI] parsing. This suffix comparison alone does
         * not validate hostname syntax.
         */
        fun isValidHostForInterface(host: String?): Boolean {
            if (host.isNullOrEmpty()) {
                return false
            }

            return host.endsWith(
                AuthenticationConstants.Broker.AAD_GLOBAL_URL_HOST_SUFFIX
            ) || host.endsWith(
                AuthenticationConstants.Broker.AAD_INTUNE_MDM_URL_HOST_SUFFIX
            ) || host.endsWith(
                AuthenticationConstants.Broker.AAD_US_URL_HOST_SUFFIX
            ) || host.endsWith(
                AuthenticationConstants.Broker.AAD_CHINA_URL_HOST_SUFFIX
            )
        }
    }

    /**
     * Method to receive a JSON string payload from AuthUX through JavaScript API.
     *
     * Dispatch is decided by the top-level `action_name` first, then by `params.operation`, so the
     * two supported actions are mutually exclusive.
     *
     * Number-match (`write_data`) — mutates the ephemeral number-match store:
     *         {
     *             "correlationID": "SOME_CORRELATION_ID" ,
     *             "action_name":"write_data",
     *             "action_component":"broker",
     *             "params":
     *             {
     *                 "operation": "number_matching",
     *                 "sessionID": "$mockSessionId",
     *                 "code_match": "$mockNumberMatchValue"
     *             }
     *         }
     *
     * Telemetry (`log_telemetry`) — non-mutating; forwards an opaque server error code to the
     * onboarding telemetry sink:
     *         {
     *             "correlationID": "SOME_CORRELATION_ID",
     *             "action_name":"log_telemetry",
     *             "action_component":"host",
     *             "params":
     *             {
     *                 "v": 1,
     *                 "sessionID": "SOME_SESSION_ID",
     *                 "errorCode": 530003,
     *                 "pageId": "ConvergedTFA",
     *                 "trackingId": "SOME_TRACKING_ID"
     *             }
     *         }
     * `errorCode` may be sent as a JSON number or string; it is captured as a string either way.
     * Note the recorded value reflects how the page serializes it — `530003.0` is captured as
     * `"530003.0"`, not `"530003"`, and would then be rejected by the error-code validation.
     * Unknown top-level and unknown `params` fields are tolerated and ignored, so the server can add
     * new key/value pairs without breaking this bridge.
     *
     * https://microsoft-my.sharepoint-df.com/:w:/p/veenasoman/EY1AZIeT8X5KrXVz97Vx520B3Jj0fBLSPlklnoRvcmbh0Q?e=VzNFd1&ovuser=72f988bf-86f1-41af-91ab-2d7cd011db47%2Cfadidurah%40microsoft.com&clickparams=eyJBcHBOYW1lIjoiVGVhbXMtRGVza3RvcCIsIkFwcFZlcnNpb24iOiI0OS8yNTA1MDQwMTYwOSIsIkhhc0ZlZGVyYXRlZFVzZXIiOmZhbHNlfQ%3D%3D
     */
    @JavascriptInterface
    fun receiveAuthUxMessage(jsonPayload: String) {
        process(jsonPayload)
    }

    /** Owns callback admission/completion for direct calls, including the JavaScript Interface route. */
    override fun process(jsonPayload: String) {
        val callbackTelemetry = bridgeTelemetry.admitCallback()
        try {
            processWithTelemetry(jsonPayload, callbackTelemetry)
        } finally {
            callbackTelemetry.completeCallback()
        }
    }

    /** Reuses the caller's admitted or no-op handle; the caller retains completion ownership. */
    override fun processWithTelemetry(jsonPayload: String, callbackTelemetry: AuthUxBridgeTelemetry) {
        var rejection: BridgeCounter? = BridgeCounter.REJECTED_PROCESSING
        try {
            synchronized(lifecycleLock) {
                rejection = processWhileOpen(jsonPayload, callbackTelemetry)
            }
        } finally {
            rejection?.let { callbackTelemetry.count(it) }
        }
    }

    // A normal processed message has no rejection; only rejected callbacks need a counter.
    private fun processWhileOpen(jsonPayload: String, callbackTelemetry: AuthUxBridgeTelemetry): BridgeCounter? {
        val methodTag = "$TAG:receiveAuthUxMessage"
        if (closed.get()) {
            Logger.info(methodTag, "Ignoring Auth UX payload after request closure.")
            return BridgeCounter.REJECTED_CLOSED
        }
        Logger.info(methodTag, "Received a payload from AuthUX through JavaScript API.")

        // Classify by stage: deserialization can throw unchecked shape/type errors too.
        val payloadObject = try {
            parseJsonToAuthUxJsonPayloadObject(jsonPayload)
        } catch (e: Exception) {
            Logger.error(
                methodTag,
                "Error Parsing JSON payload sent through JavaScriptInterface",
                e
            )
            return BridgeCounter.REJECTED_MALFORMED
        }

        try {
            // Inspect the parsed object without moving the existing fallible telemetry or routing.
            if (payloadObject?.actionName != ActionNames.LOG_TELEMETRY &&
                payloadObject?.params?.operation == OperationNames.NUMBER_MATCHING
            ) {
                callbackTelemetry.count(BridgeCounter.NUMBER_MATCH_IDENTIFIED)
            }

            val correlationId = payloadObject.correlationId

            Logger.info(
                methodTag,
                correlationId,
                "Parsed AuthUX JavaScript payload."
            )

            val span = SpanExtension.current()
            val actionName = payloadObject.actionName
            span.setAttribute(AttributeName.authux_js_action_name.name, actionName)
            val actionComponent = payloadObject.actionComponent
            span.setAttribute(AttributeName.authux_js_action_component.name, actionComponent)

            val parameters = payloadObject.params
            if (parameters == null) {
                Logger.warn(
                    methodTag,
                    correlationId,
                    "Payload from AuthUX contained no \"params\" field."
                )
                return BridgeCounter.REJECTED_MALFORMED
            }

            val operation = parameters.operation
            if (operation != null) {
                span.setAttribute(AttributeName.authux_js_operation.name, operation)
            }

            Logger.info(
                methodTag,
                correlationId,
                "Action name: [$actionName], operation: [$operation]"
            )

            return when {
                // Match telemetry first so params.operation cannot route it to Number Matching.
                actionName == ActionNames.LOG_TELEMETRY -> {
                    if (capabilities.telemetryAllowed) {
                        handleLogTelemetry(correlationId, parameters, methodTag)
                    } else {
                        Logger.warn(
                            methodTag,
                            correlationId,
                            "Auth UX telemetry is not allowed for this request."
                        )
                        BridgeCounter.REJECTED_CAPABILITY
                    }
                }

                operation == OperationNames.NUMBER_MATCHING && capabilities.numberMatchingAllowed -> {
                    NumberMatchHelper.storeNumberMatch(
                        parameters.sessionId,
                        parameters.codeMatch
                    )
                    null
                }

                else -> {
                    Logger.warn(
                        methodTag,
                        correlationId,
                        "Payload from AuthUX contained an unknown action/operation."
                    )
                    if (operation == OperationNames.NUMBER_MATCHING) BridgeCounter.REJECTED_CAPABILITY
                    else BridgeCounter.REJECTED_UNSUPPORTED
                }
            }
        } catch (e: Exception) { // If we run into exceptions, we don't want to kill the broker
            Logger.error(
                methodTag,
                "Unknown error occurred while processing the payload.",
                e
            )
            return BridgeCounter.REJECTED_PROCESSING
        }
    }

    override fun close() {
        synchronized(lifecycleLock) {
            closed.set(true)
        }
    }

    private fun parseJsonToAuthUxJsonPayloadObject(jsonString: String): AuthUxJsonPayload {
        val gson = GsonBuilder()
            .registerTypeAdapter(AuthUxJsonPayload::class.java, AuthUxJsonPayloadKTDeserializer())
            .create()
        return gson.fromJson(jsonString, AuthUxJsonPayload::class.java)
    }

    /**
     * Validates and forwards an [ActionNames.LOG_TELEMETRY] event to the host-supplied sink.
     *
     * The bridge owns no counters, deduplication, chronology, or recorder lifecycle.
     *
     * @param correlationId Correlation ID from the payload, used as the telemetry join key.
     * @param parameters Parsed `params` object of the message.
     * @param methodTag Log tag of the calling method.
     */
    private fun handleLogTelemetry(
        correlationId: String,
        parameters: AuthUxParams,
        methodTag: String
    ): BridgeCounter? {
        val errorCode = parameters.errorCode
        if (errorCode.isNullOrEmpty()) {
            Logger.warn(
                methodTag,
                correlationId,
                "log_telemetry payload contained no \"errorCode\"; ignoring (no-op)."
            )
            return BridgeCounter.REJECTED_MALFORMED
        }

        if (!ERROR_CODE_REGEX.matches(errorCode)) {
            Logger.warn(
                methodTag,
                correlationId,
                "log_telemetry errorCode failed validation (length=${errorCode.length}); dropping."
            )
            return BridgeCounter.REJECTED_MALFORMED
        }

        val sink = telemetrySink
        if (sink == null) {
            // Log the missing wiring; a later occurrence remains independently eligible.
            Logger.warn(
                methodTag,
                correlationId,
                "log_telemetry errorCode [$errorCode] received but no telemetry sink is wired; dropping."
            )
            return null
        }

        val consumed = try {
            sink.tryConsumeAuthUxTelemetry(
                AuthUxTelemetryEvent(
                    correlationId = correlationId,
                    errorCode = errorCode,
                    sessionId = parameters.sessionId,
                    pageId = parameters.pageId,
                    trackingId = parameters.trackingId,
                    version = parameters.version
                )
            )
        } catch (t: Throwable) {
            // Isolate sink failures so telemetry cannot fail authentication.
            Logger.error(
                methodTag,
                correlationId,
                "Onboarding telemetry sink threw while handling Auth UX error code [$errorCode]; ignoring.",
                t
            )
            return BridgeCounter.REJECTED_PROCESSING
        }

        if (!consumed) {
            Logger.info(
                methodTag,
                correlationId,
                "log_telemetry errorCode [$errorCode] was not consumed by the sink; "
                        + "leaving later occurrences eligible."
            )
            return null
        }

        // Record the last code accepted by the sink. Downstream policy may still exclude it from
        // the onboarding blob.
        SpanExtension.current()
            .setAttribute(AttributeName.authux_js_error_code.name, errorCode)
        Logger.info(
            methodTag,
            correlationId,
            "Forwarded Auth UX server error code [$errorCode] to onboarding telemetry."
        )
        return null
    }

    /**
     * Operation names dispatched via the `params.operation` field (number-match / `write_data` path).
     */
    object OperationNames {
        const val NUMBER_MATCHING = "number_matching"
    }

    /**
     * Top-level `action_name` values dispatched directly (independent of `params.operation`).
     */
    object ActionNames {
        const val LOG_TELEMETRY = "log_telemetry"
    }
}
