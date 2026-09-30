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
 * @property correlationId Correlation ID reported by the page (`correlationID`). Present — the
 *  deserializer rejects a payload without it — but may be empty, since the page controls the value.
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
 * **Threading.** The callback thread depends on the transport: `@JavascriptInterface` uses
 * WebView's JavaBridge thread, while `WebMessageListener` uses the UI thread. The telemetry state
 * may also be finalized from another thread, so sink implementations must use thread-safe state.
 */
fun interface AuthUxTelemetrySink {
    /**
     * Route an opaque Auth UX telemetry event to onboarding telemetry.
     *
     * **Threading.** Invoked on the transport callback thread. Implementations must be thread-safe
     * and must not block.
     *
     * @param event The validated telemetry context. [AuthUxTelemetryEvent.errorCode] is guaranteed
     *  non-empty and shape-checked; the remaining fields are best-effort page-supplied context,
     *  control-character-stripped and length-bounded but not shape-validated.
     * @return `true` if the sink took responsibility for the event — whether it recorded the code or
     *  deliberately dropped it by its own policy — and `false` if it is not ready to take it yet.
     *  Each page message is treated as an independent occurrence, so neither result suppresses a
     *  later occurrence.
     */
    fun tryConsumeAuthUxTelemetry(event: AuthUxTelemetryEvent): Boolean
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
 * @property telemetryOnly When true, rejects every action except [ActionNames.LOG_TELEMETRY].
 *  Used by the origin-scoped WebMessage listener so a message posted to that listener can never
 *  reach the number-matching store.
 */
class AuthUxJavaScriptInterface @JvmOverloads constructor(
    private val telemetrySink: AuthUxTelemetrySink? = null,
    private val telemetryOnly: Boolean = false
) {

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

        /** Maximum length of a page-supplied string echoed into a log line. */
        private const val MAX_LOGGED_VALUE_LENGTH = 64

        /**
         * Upper bound on a correlation ID. Generous: a GUID is 36 characters, so anything under
         * this is preserved verbatim. A value longer than this is not a usable join key anyway, so
         * truncating it costs nothing and stops a page pushing an unbounded string into every log
         * line on this path.
         */
        private const val MAX_CORRELATION_ID_LENGTH = 128

        /**
         * Upper bound on an optional page-supplied context value (`sessionID`, `pageId`,
         * `trackingId`, `v`) carried across the sink seam.
         *
         * Deliberately its own constant rather than a reuse of [MAX_CORRELATION_ID_LENGTH], even
         * though the two happen to be equal today: these bound unrelated things, and sharing one
         * would mean a future change to the correlation-ID bound (a cloud with longer IDs, say)
         * silently moved the bound on values that have nothing to do with correlation.
         *
         * The value is generous on purpose — these fields are recorded, not parsed, so the bound
         * exists to stop a page pushing an unbounded string into an uploaded blob, not to enforce
         * a shape. None of the known values comes close to it.
         */
        private const val MAX_CARRIED_VALUE_LENGTH = 128

        /**
         * Copy at most [limit] characters of an untrusted, page-supplied string, replacing any
         * control character with a space so a crafted value cannot forge log entries, and appending
         * a marker when the input was longer.
         *
         * Replacing rather than removing keeps the result the same length as the inspected prefix,
         * so a value's shape stays recognisable in a log line instead of silently closing up around
         * the removed characters.
         *
         * Walks at most [limit] characters rather than transforming the whole string and then
         * cutting it, so the work done here is bounded by the limit instead of by the size of the
         * page-supplied value. (The value itself is already materialized by the time the bridge is
         * called, so this bounds our own processing, not the caller's allocation.)
         */
        private fun sanitizeBounded(value: String, limit: Int): String {
            val kept = minOf(value.length, limit)
            val builder = StringBuilder(kept)
            for (i in 0 until kept) {
                val c = value[i]
                builder.append(if (c.isISOControl()) ' ' else c)
            }
            if (value.length > limit) {
                builder.append("...(truncated)")
            }
            return builder.toString()
        }

        /**
         * Bound an untrusted, page-supplied string before it reaches a log line: truncate to
         * [MAX_LOGGED_VALUE_LENGTH] and replace control characters (including CR/LF) with spaces so
         * a crafted value cannot forge log entries.
         */
        private fun sanitizeForLog(value: String?): String {
            if (value == null) {
                return "null"
            }
            return sanitizeBounded(value, MAX_LOGGED_VALUE_LENGTH)
        }

        /**
         * Make a page-supplied correlation ID safe to log **without destroying its value as a join
         * key**.
         *
         * Unlike [sanitizeForLog] this does not truncate at the log-display bound: the correlation
         * ID is forwarded to the telemetry sink and is the key used to join this event against
         * eSTS / Kusto records, so shortening a legitimate ID would silently break correlation.
         * Control characters (including CR/LF) are replaced with spaces so a crafted value cannot
         * forge log entries, and only absurd lengths — well past any real correlation ID — are cut,
         * since such a value is not a usable key anyway.
         */
        private fun sanitizeCorrelationId(value: String): String =
            sanitizeBounded(value, MAX_CORRELATION_ID_LENGTH)

        /**
         * Make an optional, page-supplied context value safe to carry across the sink seam.
         *
         * These fields (`sessionID`, `pageId`, `trackingId`, `v`) are not shape-validated the way
         * `errorCode` is — there is no contract shape to check — but they cross the same trust
         * boundary and reach the same onboarding blob, so they get the same control-character
         * stripping that keeps a crafted value from forging log entries in whichever consumer
         * eventually reads them, and a bound ([MAX_CARRIED_VALUE_LENGTH]) so a page cannot push an
         * unbounded string into an uploaded blob. Null is preserved as null: absent and empty are
         * different signals to a consumer, so this must not invent a value.
         */
        private fun sanitizeCarriedValue(value: String?): String? =
            value?.let { sanitizeBounded(it, MAX_CARRIED_VALUE_LENGTH) }

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

            // correlationID is page-controlled (the deserializer only requires that it be present
            // and a JSON string), and it is passed as the correlationID argument of the Logger
            // calls on this path — which common4j formats into the emitted line verbatim. Replace
            // control characters so no page-supplied value can forge log entries, but preserve the
            // value's length: it is also the telemetry join key, so truncating a legitimate ID
            // would silently break correlation.
            val correlationId = sanitizeCorrelationId(payloadObject.correlationId)

            Logger.info(
                methodTag,
                correlationId,
                "Parsed AuthUX JavaScript payload."
            )

            val span = SpanExtension.current()
            // Span attributes are bounded/neutralized the same way log lines are: these are
            // page-controlled strings, and an oversized or control-character-laden value would
            // otherwise ride straight into the telemetry backend. Dispatch below deliberately
            // compares the RAW values, so sanitizing here cannot change which branch is taken.
            val actionName = payloadObject.actionName
            span.setAttribute(AttributeName.authux_js_action_name.name, sanitizeForLog(actionName))
            val actionComponent = payloadObject.actionComponent
            span.setAttribute(
                AttributeName.authux_js_action_component.name,
                sanitizeForLog(actionComponent)
            )

            val parameters = payloadObject.params
            if (parameters == null) {
                Logger.warn(
                    methodTag,
                    correlationId,
                    "Payload from AuthUX contained no \"params\" field."
                )
                return
            }

            val operation = parameters.operation
            if (operation != null) {
                span.setAttribute(AttributeName.authux_js_operation.name, sanitizeForLog(operation))
            }

            Logger.info(
                methodTag,
                correlationId,
                "Action name: [${sanitizeForLog(actionName)}], operation: [${sanitizeForLog(operation)}]"
            )

            if (telemetryOnly && actionName != ActionNames.LOG_TELEMETRY) {
                Logger.warn(
                    methodTag,
                    correlationId,
                    "Telemetry-only Auth UX message handler rejected a non-telemetry action."
                )
                return
            }

            when {
                // Telemetry-only action, matched FIRST and dispatched purely on action_name so a
                // params.operation smuggled into a log_telemetry message can never reach the
                // number-match device store (H3). The append to the onboarding blob (with the
                // non-blocking exclusion list) is handled downstream by the supplied sink — see
                // AB#3688632.
                actionName == ActionNames.LOG_TELEMETRY ->
                    handleLogTelemetry(correlationId, parameters, methodTag)

                operation == OperationNames.NUMBER_MATCHING ->
                    NumberMatchHelper.storeNumberMatch(
                        parameters.sessionId,
                        parameters.codeMatch
                    )

                else ->
                    Logger.warn(
                        methodTag,
                        correlationId,
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
     * Every valid occurrence is offered to the sink, including repeated codes. The onboarding
     * telemetry contract is chronological and append-only; preserving duplicates allows repeated
     * page/server loops to remain visible in `blocking_errors`.
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

        val sink = telemetrySink
        if (sink == null) {
            // Not silent: without this a DRI cannot distinguish "the page never sent a code" from
            // "the host never wired a sink". Deliberately NOT recorded as handled — nothing
            // consumed it, so it must stay eligible if a sink is wired later; the attempt counter
            // above is what stops a page looping on this path.
            Logger.warn(
                methodTag,
                correlationId,
                "log_telemetry errorCode [$errorCode] received but no telemetry sink is wired; dropping."
            )
            return
        }

        val outcome = try {
            // Invoked outside the lock: never hold an internal lock across a host callback.
            if (sink.tryConsumeAuthUxTelemetry(
                    AuthUxTelemetryEvent(
                        correlationId = correlationId,
                        errorCode = errorCode,
                        // Stripped and bounded at the seam, like correlationId above. These are
                        // page-controlled and reach the same onboarding blob the errorCode
                        // validation exists to protect; a consumer that logs one would otherwise
                        // inherit the log-forging hole already closed for correlationId. Not
                        // shape-validated — unlike errorCode there is no contract shape to check.
                        sessionId = sanitizeCarriedValue(parameters.sessionId),
                        pageId = sanitizeCarriedValue(parameters.pageId),
                        trackingId = sanitizeCarriedValue(parameters.trackingId),
                        version = sanitizeCarriedValue(parameters.version)
                    )
                )
            ) {
                SinkOutcome.CONSUMED
            } else {
                SinkOutcome.NOT_CONSUMED
            }
        } catch (t: Throwable) {
            // Own try/catch so a throwing sink is not misdiagnosed as a payload parsing failure by
            // the caller's generic handler. Telemetry must never fail the auth flow.
            Logger.error(
                methodTag,
                correlationId,
                "Onboarding telemetry sink threw while handling Auth UX error code [$errorCode]; ignoring.",
                t
            )
            SinkOutcome.THREW
        }

        when (outcome) {
            SinkOutcome.NOT_CONSUMED -> {
                Logger.info(
                    methodTag,
                    correlationId,
                    "log_telemetry errorCode [$errorCode] was not consumed by the sink; "
                            + "leaving it eligible for retry."
                )
                return
            }

            SinkOutcome.THREW -> {
                // Nothing was recorded downstream, so do not set the span attribute or claim the
                // code was forwarded. A later page message remains an independent occurrence.
                return
            }

            SinkOutcome.CONSUMED -> Unit
        }

        // Set for any code a sink CONSUMED. Note "consumed" means the sink took responsibility for
        // the code, which includes deliberately dropping it by its own policy — so this attribute
        // records what the PAGE REPORTED and the sink accepted, not what downstream telemetry
        // ultimately stored. A sink that excludes some codes (as the onboarding sink does for the
        // non-onboarding AADSTS list) will legitimately produce a span carrying a code the blob does
        // not. What it can never do is carry a code no sink took: NOT_CONSUMED and THREW both skip
        // this. Single-valued by nature of setAttribute: on a flow reporting several codes the LAST
        // consumed code wins.
        SpanExtension.current()
            .setAttribute(AttributeName.authux_js_error_code.name, errorCode)
        Logger.info(
            methodTag,
            correlationId,
            "Forwarded Auth UX server error code [$errorCode] to onboarding telemetry."
        )
    }

    /** Outcome of handing one telemetry event to the host-supplied sink. */
    private enum class SinkOutcome {
        /** The sink recorded the event (or deliberately dropped it by policy). */
        CONSUMED,

        /** The sink is not ready to record yet; the code stays eligible for a retry. */
        NOT_CONSUMED,

        /** The sink threw. Retry is suppressed, but nothing was recorded downstream. */
        THREW
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
