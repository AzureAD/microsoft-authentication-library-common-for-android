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
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import com.google.gson.stream.MalformedJsonException
import com.microsoft.identity.common.adal.internal.AuthenticationConstants
import com.microsoft.identity.common.internal.numberMatch.NumberMatchHelper
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.opentelemetry.SpanExtension
import com.microsoft.identity.common.logging.Logger
import java.net.URI
import java.net.URISyntaxException

/**
 * Immutable context for a single Auth UX `log_telemetry` message.
 *
 * Carries the full parsed telemetry context rather than the error code alone so that consumers
 * which later want the page/session identifiers — or the correlation ID, which is the join key for
 * Kusto / DRI correlation — can take them without a source-breaking change to
 * [AuthUxTelemetrySink]. Fields other than [correlationId] and [errorCode] are optional because the
 * page may omit them.
 *
 * @property correlationId Correlation ID reported by the page (`correlationID`). Non-empty:
 *  the deserializer rejects a payload without it.
 * @property errorCode Validated Auth UX server error code (e.g. `"530003"`). Guaranteed to be
 *  non-empty and to match the bridge's accepted error-code shape.
 * @property sessionId Auth UX session identifier (`params.sessionID`), when supplied.
 * @property pageId Auth UX page identifier (`params.pageId`, e.g. `"ConvergedTFA"`), when supplied.
 * @property trackingId Auth UX tracking identifier (`params.trackingId`), when supplied.
 * @property version Telemetry contract version (`params.v`) as reported by the page, when supplied.
 *  Kept as a string so a non-integer version can never fail the parse.
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
 * Callback seam used to forward an opaque Auth UX telemetry signal (a server error code) received
 * over the JS bridge to the onboarding telemetry sink.
 *
 * Kept deliberately minimal and decoupled from the concrete telemetry recorder so this bridge does
 * not take a dependency on the onboarding recorder plumbing. The concrete wiring — resolving the
 * active [com.microsoft.identity.common.internal.telemetry.OnboardingTelemetryRecorder] and
 * appending the code to the onboarding blob's blocking-errors list (subject to the non-blocking
 * exclusion list) — is supplied by the host and handled downstream (see AB#3688632).
 *
 * **Threading.** Implementations MUST be thread-safe. `@JavascriptInterface` methods are dispatched
 * on the WebView's private JavaBridge thread, not the UI thread, so a sink can be invoked
 * concurrently with UI-thread callers that touch the same telemetry recorder (for example
 * `AzureActiveDirectoryWebViewClient.onPageFinished` recording the last loaded domain).
 */
fun interface AuthUxTelemetrySink {
    /**
     * Route an opaque Auth UX telemetry event to onboarding telemetry.
     *
     * Called on the WebView JavaBridge thread; implementations must be thread-safe and must not
     * block. Throwing is tolerated (the bridge catches and logs) but never useful.
     *
     * @param event The validated telemetry context. [AuthUxTelemetryEvent.errorCode] is guaranteed
     *  non-empty and shape-checked; the remaining fields are best-effort page-supplied context.
     */
    fun onAuthUxTelemetry(event: AuthUxTelemetryEvent)
}

/**
 * JavaScript API to receive JSON string payloads from AuthUX in order to facilitate calling various
 * broker methods.
 *
 * @property telemetrySink Optional sink invoked for the non-mutating
 *  [ActionNames.LOG_TELEMETRY] action to forward an opaque Auth UX server error code to
 *  onboarding telemetry. When null (the default), `log_telemetry` messages are still parsed and
 *  validated but produce no telemetry side effect. Supplying a default keeps existing no-arg
 *  construction (e.g. from the WebView host) source-compatible.
 */
class AuthUxJavaScriptInterface @JvmOverloads constructor(
    private val telemetrySink: AuthUxTelemetrySink? = null
) {

    /**
     * Distinct error codes already forwarded by this instance, used to enforce
     * [MAX_FORWARDED_ERROR_CODES] and to suppress duplicates so a page that posts in a loop cannot
     * bloat the onboarding blob. Bounded by the cap. Guarded by itself because
     * `@JavascriptInterface` calls arrive on the WebView JavaBridge thread.
     */
    private val forwardedErrorCodes = LinkedHashSet<String>()

    // Store number matches in a static hash map
    // No need to persist this storage beyond the current broker process, but we need to keep them
    // long enough for AuthApp to call the broker api to fetch the number match
    companion object {
        val TAG = AuthUxJavaScriptInterface::class.java.simpleName
        private const val JAVASCRIPT_INTERFACE_NAME = "broker"

        /**
         * Accepted shape for a `log_telemetry` error code.
         *
         * Deliberately permissive enough for both numeric STS codes (`"530003"`) and the symbolic
         * constants [com.microsoft.identity.common.java.telemetry.IOnboardingTelemetryRecorder.addBlockingError]
         * also accepts (e.g. `"BROKER_INSTALL"`), while excluding whitespace and control characters
         * so a page-supplied value can never inject a newline into a log line or an unbounded string
         * into the uploaded onboarding blob.
         */
        private val ERROR_CODE_REGEX = Regex("^[A-Za-z0-9_-]{1,32}$")

        /**
         * Maximum number of distinct error codes a single bridge instance will forward. Bounds the
         * onboarding blob against a page that posts `log_telemetry` in a loop. Duplicates are
         * suppressed and do not count toward the cap.
         */
        private const val MAX_FORWARDED_ERROR_CODES = 10

        /** Maximum length of a page-supplied string echoed into a log line. */
        private const val MAX_LOGGED_VALUE_LENGTH = 64

        /**
         * Bound an untrusted, page-supplied string before it reaches a log line: truncate to
         * [MAX_LOGGED_VALUE_LENGTH] and strip CR/LF so a crafted value cannot forge log entries.
         */
        private fun sanitizeForLog(value: String?): String {
            if (value == null) {
                return "null"
            }
            val flattened = value.replace('\n', ' ').replace('\r', ' ')
            return if (flattened.length <= MAX_LOGGED_VALUE_LENGTH) {
                flattened
            } else {
                flattened.substring(0, MAX_LOGGED_VALUE_LENGTH) + "...(truncated)"
            }
        }

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
                Logger.warn(TAG, "URISyntaxException received. uri: $uriString, Message: ${e.message}")
                return false
            }

            val host = uri.host

            // A scheme-only URI (e.g. "openid-vc://?request_uri=...") has a null host. Such URIs
            // are not AAD interface URIs, so reject them instead of throwing on a null host.
            if (host == null) {
                return false
            }

            // Otherwise, make sure uri is a valid uri
            // We only want to allow URIs that have the AAD uri hosts
            return host.endsWith(AuthenticationConstants.Broker.AAD_GLOBAL_URL_HOST_SUFFIX) ||
                    host.endsWith(AuthenticationConstants.Broker.AAD_INTUNE_MDM_URL_HOST_SUFFIX) ||
                    host.endsWith(AuthenticationConstants.Broker.AAD_US_URL_HOST_SUFFIX) ||
                    host.endsWith(AuthenticationConstants.Broker.AAD_CHINA_URL_HOST_SUFFIX)
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
     * onboarding telemetry sink (AB#3688631 / AB#3688632):
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
        val methodTag = "$TAG:receiveAuthUxMessage"
        Logger.info(methodTag, "Received a payload from AuthUX through JavaScript API.")

        try {
            val payloadObject = parseJsonToAuthUxJsonPayloadObject(jsonPayload)

            Logger.info(
                methodTag,
                "Correlation ID during JavaScript Call: [${payloadObject.correlationId}]"
            )

            val span = SpanExtension.current()
            val actionName = payloadObject.actionName
            span.setAttribute(AttributeName.authux_js_action_name.name, actionName)
            val actionComponent = payloadObject.actionComponent
            span.setAttribute(AttributeName.authux_js_action_component.name, actionComponent)

            val parameters = payloadObject.params
            if (parameters == null) {
                Logger.warn(methodTag, "Payload from AuthUX contained no \"params\" field.")
                return
            }

            val operation = parameters.operation
            if (operation != null) {
                span.setAttribute(AttributeName.authux_js_operation.name, operation)
            }

            Logger.info(
                methodTag,
                "Action name: [${sanitizeForLog(actionName)}], operation: [${sanitizeForLog(operation)}]"
            )

            when {
                // Telemetry-only action, matched FIRST and dispatched purely on action_name so a
                // params.operation smuggled into a log_telemetry message can never reach the
                // number-match device store (H3). The append to the onboarding blob (with the
                // non-blocking exclusion list) is handled downstream by the supplied sink — see
                // AB#3688632.
                actionName == ActionNames.LOG_TELEMETRY ->
                    handleLogTelemetry(payloadObject.correlationId, parameters, methodTag)

                operation == OperationNames.NUMBER_MATCHING ->
                    NumberMatchHelper.storeNumberMatch(
                        parameters.sessionId,
                        parameters.codeMatch
                    )

                else ->
                    Logger.warn(
                        methodTag,
                        "Payload from AuthUX contained an unknown action/operation."
                    )
            }
        } catch (e: Exception) { // If we run into exceptions, we don't want to kill the broker
            when (e) {
                is NullPointerException -> {
                    Logger.error(
                        methodTag,
                        "Payload with missing mandatory fields sent through JavaScriptInterface",
                        e
                    )
                }

                is MalformedJsonException, is JsonSyntaxException, is JsonParseException -> {
                    Logger.error(
                        methodTag,
                        "Error Parsing JSON payload sent through JavaScriptInterface",
                        e
                    )
                }

                else -> {
                    Logger.error(
                        methodTag,
                        "Unknown error occurred while processing the payload.",
                        e
                    )
                }
            }
        }
    }

    private fun parseJsonToAuthUxJsonPayloadObject(jsonString: String): AuthUxJsonPayload {
        val gson = GsonBuilder()
            .registerTypeAdapter(AuthUxJsonPayload::class.java, AuthUxJsonPayloadKTDeserializer())
            .create()
        return gson.fromJson(jsonString, AuthUxJsonPayload::class.java)
    }

    /**
     * Handle the non-mutating [ActionNames.LOG_TELEMETRY] action: validate the page-supplied error
     * code and forward it, with its telemetry context, to the host-supplied sink.
     *
     * Every exit path is logged distinguishably so a DRI can tell from logcat alone whether a code
     * was forwarded, rejected as malformed, suppressed by the cap/dedupe, or dropped because no
     * host sink was wired.
     *
     * @param correlationId Correlation ID from the payload, used as the telemetry join key.
     * @param parameters Parsed `params` object of the message.
     * @param methodTag Log tag of the calling method.
     */
    private fun handleLogTelemetry(
        correlationId: String,
        parameters: AuthUxParams,
        methodTag: String
    ) {
        val errorCode = parameters.errorCode
        if (errorCode.isNullOrEmpty()) {
            Logger.warn(
                methodTag,
                correlationId,
                "log_telemetry payload contained no \"errorCode\"; ignoring (no-op)."
            )
            return
        }

        // Validate BEFORE the value reaches any log line, so a crafted code carrying CR/LF cannot
        // forge log entries, and an unbounded string never reaches the uploaded onboarding blob.
        if (!ERROR_CODE_REGEX.matches(errorCode)) {
            Logger.warn(
                methodTag,
                correlationId,
                "log_telemetry errorCode failed validation (length=${errorCode.length}); dropping. "
                        + "Sanitized value: [${sanitizeForLog(errorCode)}]"
            )
            return
        }

        val span = SpanExtension.current()
        span.setAttribute(AttributeName.authux_js_error_code.name, errorCode)

        synchronized(forwardedErrorCodes) {
            if (forwardedErrorCodes.contains(errorCode)) {
                Logger.info(
                    methodTag,
                    correlationId,
                    "log_telemetry errorCode [$errorCode] already forwarded; suppressing duplicate."
                )
                return
            }
            if (forwardedErrorCodes.size >= MAX_FORWARDED_ERROR_CODES) {
                Logger.warn(
                    methodTag,
                    correlationId,
                    "log_telemetry forwarding cap ($MAX_FORWARDED_ERROR_CODES) reached; "
                            + "dropping errorCode [$errorCode]."
                )
                return
            }
            forwardedErrorCodes.add(errorCode)
        }

        val sink = telemetrySink
        if (sink == null) {
            // Not silent: without this a DRI cannot distinguish "the page never sent a code" from
            // "the host never wired a sink".
            Logger.warn(
                methodTag,
                correlationId,
                "log_telemetry errorCode [$errorCode] received but no telemetry sink is wired; dropping."
            )
            return
        }

        try {
            sink.onAuthUxTelemetry(
                AuthUxTelemetryEvent(
                    correlationId = correlationId,
                    errorCode = errorCode,
                    sessionId = parameters.sessionId,
                    pageId = parameters.pageId,
                    trackingId = parameters.trackingId,
                    version = parameters.version
                )
            )
            Logger.info(
                methodTag,
                correlationId,
                "Forwarded Auth UX server error code [$errorCode] to onboarding telemetry."
            )
        } catch (t: Throwable) {
            // Own try/catch so a throwing sink is not misdiagnosed as a payload parsing failure by
            // the caller's generic handler. Telemetry must never fail the auth flow.
            Logger.error(
                methodTag,
                correlationId,
                "Onboarding telemetry sink threw while handling Auth UX error code [$errorCode]; ignoring.",
                t
            )
        }
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
