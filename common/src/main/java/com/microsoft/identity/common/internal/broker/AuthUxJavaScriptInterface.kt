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
 * exclusion list) — is supplied by the host
 * (`AzureActiveDirectoryWebViewClient.createAuthUxJavaScriptInterface`); see AB#3688632.
 *
 * **Threading.** `@JavascriptInterface` methods are dispatched on the WebView's private JavaBridge
 * thread, not the UI thread. The bridge serializes its own calls per instance, but the telemetry
 * state a sink writes to is typically also touched from the UI thread (for example
 * `AzureActiveDirectoryWebViewClient.onPageFinished` recording the last loaded domain, or the host
 * serializing the blob at the end of the flow). The bridge cannot enforce safety on state it does
 * not own, so that state must be thread-safe; the shipped implementation satisfies this by making
 * the onboarding recorder's collections thread-safe rather than by relying on this note.
 */
fun interface AuthUxTelemetrySink {
    /**
     * Route an opaque Auth UX telemetry event to onboarding telemetry.
     *
     * **Threading.** Invoked on the WebView JavaBridge thread. WebView dispatches
     * `@JavascriptInterface` calls for a given WebView on a single private background thread, so
     * calls into one bridge instance do not overlap — note this is a *platform* property, not
     * something this class implements: [receiveAuthUxMessage] takes no lock. The bridge does
     * guarantee it never invokes a sink while holding an internal lock. It cannot serialize a sink
     * against other threads, so a host that also mutates the same telemetry state from the UI
     * thread (for example while serializing the onboarding blob) must make that state thread-safe
     * itself. Implementations must not block.
     *
     * @param event The validated telemetry context. [AuthUxTelemetryEvent.errorCode] is guaranteed
     *  non-empty and shape-checked; the remaining fields are best-effort page-supplied context.
     * @return `true` if the event was consumed and recorded, `false` if it was ignored (for example
     *  because the host has no active telemetry recorder yet). Returning `false` lets the bridge
     *  leave the code eligible for a later retry instead of suppressing it as already-forwarded, so
     *  a code that arrives before the host is ready is not lost.
     */
    fun onAuthUxTelemetry(event: AuthUxTelemetryEvent): Boolean
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
     * Error codes this instance will not offer to a sink again — either because a sink reported
     * consuming them, or because a sink threw while handling them (a host defect that retrying
     * cannot fix).
     *
     * A code is *not* recorded here when a sink reports it did **not** consume the event, so a code
     * that arrives before the host has a recorder stays eligible for a later retry rather than being
     * suppressed as already-handled.
     *
     * **Scope is this bridge instance only.** The WebView host re-registers the bridge on every
     * navigation (`OAuth2WebViewClient.onPageStarted`), and each registration constructs a new
     * `AuthUxJavaScriptInterface`, so this state resets per page load while the consumer it feeds
     * lives for the whole request. This is therefore a per-page flood guard, **not** a
     * request-wide de-duplication guarantee: the same code reported on two page loads is offered
     * twice, and session-wide de-duplication is the consumer's responsibility (the onboarding
     * recorder de-duplicates its blocking-errors list for exactly this reason).
     *
     * Guarded by itself; also guards [telemetryAttempts]. The sink is never invoked while this
     * lock is held.
     */
    private val handledErrorCodes = LinkedHashSet<String>()

    /**
     * Count of `log_telemetry` messages this instance has taken past validation, whether or not a
     * sink consumed them.
     *
     * [handledErrorCodes] alone cannot bound the work a page can cause: a code that is never
     * consumed is deliberately never recorded there, so neither the duplicate check nor the
     * distinct-code cap would ever fire for it. Without this counter a page posting the same code
     * in a loop while no sink is wired — or while the sink keeps declining — would re-invoke the
     * sink and re-log without limit. Guarded by [handledErrorCodes].
     */
    private var telemetryAttempts = 0

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
         * Maximum number of distinct error codes a single bridge instance will hand to a sink.
         * Bounds the damage a page that posts `log_telemetry` in a loop can do **within one page
         * load** — see [handledErrorCodes] for why the scope is per instance rather than per
         * request. Duplicates are suppressed and do not count toward the cap.
         */
        private const val MAX_FORWARDED_ERROR_CODES = 10

        /**
         * Maximum number of `log_telemetry` messages a single bridge instance will process past
         * validation, counted whether or not a sink consumes them. Bounds the paths
         * [MAX_FORWARDED_ERROR_CODES] cannot: a code that is never consumed is never recorded as
         * handled, so only this counter stops a page looping against an unwired or perpetually
         * declining sink. Set above [MAX_FORWARDED_ERROR_CODES] so that legitimate retries — a code
         * reported before the host attached its recorder — still get through.
         */
        private const val MAX_TELEMETRY_ATTEMPTS = 25

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
     * Every exit path is logged distinguishably so a DRI can tell from logcat alone whether a code
     * was forwarded, rejected as malformed, suppressed by the cap/dedupe, declined by the sink, or
     * dropped because no host sink was wired.
     *
     * **Concurrency.** The dedupe/cap bookkeeping is check-then-act across two critical sections
     * with the sink invocation between them (the sink is deliberately never called under the lock).
     * That gap cannot interleave in practice because WebView dispatches `@JavascriptInterface`
     * calls for one WebView on a single thread, so a bridge instance is only ever driven serially.
     * The state is still guarded so that even if a caller invoked this from another thread the
     * worst case is a bounded duplicate forward, never a corrupted set.
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

        synchronized(handledErrorCodes) {
            // Counted and checked FIRST, before the duplicate and distinct-code checks, because
            // those paths log and return: a page looping one already-handled code, or posting new
            // codes after the distinct-code cap is reached, would otherwise never reach this
            // counter and could spam the log without bound. Counting here bounds every path past
            // validation, whether or not a sink ends up consuming the code.
            if (telemetryAttempts >= MAX_TELEMETRY_ATTEMPTS) {
                if (telemetryAttempts == MAX_TELEMETRY_ATTEMPTS) {
                    // Logged once, on the transition, so hitting the cap is diagnosable without the
                    // cap message itself becoming the spam it exists to prevent.
                    telemetryAttempts++
                    Logger.warn(
                        methodTag,
                        correlationId,
                        "log_telemetry attempt cap ($MAX_TELEMETRY_ATTEMPTS) reached; "
                                + "ignoring further log_telemetry messages from this page."
                    )
                }
                return
            }
            telemetryAttempts++

            if (handledErrorCodes.contains(errorCode)) {
                Logger.info(
                    methodTag,
                    correlationId,
                    "log_telemetry errorCode [$errorCode] already handled; suppressing duplicate."
                )
                return
            }
            if (handledErrorCodes.size >= MAX_FORWARDED_ERROR_CODES) {
                Logger.warn(
                    methodTag,
                    correlationId,
                    "log_telemetry forwarding cap ($MAX_FORWARDED_ERROR_CODES) reached; "
                            + "dropping errorCode [$errorCode]."
                )
                return
            }
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
            if (sink.onAuthUxTelemetry(
                    AuthUxTelemetryEvent(
                        correlationId = correlationId,
                        errorCode = errorCode,
                        sessionId = parameters.sessionId,
                        pageId = parameters.pageId,
                        trackingId = parameters.trackingId,
                        version = parameters.version
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
                // Suppress retry only. A throwing sink is a host defect that retrying cannot fix,
                // and re-offering it would let a looping page re-invoke a broken sink. But nothing
                // was recorded downstream, so this deliberately does NOT set the span attribute or
                // claim the code was forwarded — the error log above is the whole story.
                synchronized(handledErrorCodes) { handledErrorCodes.add(errorCode) }
                return
            }

            SinkOutcome.CONSUMED -> Unit
        }

        synchronized(handledErrorCodes) { handledErrorCodes.add(errorCode) }
        // Set only for codes a sink actually took, so the span cannot report codes that downstream
        // telemetry never received. Single-valued by nature of setAttribute: on a flow reporting
        // several codes the LAST consumed code wins.
        span.setAttribute(AttributeName.authux_js_error_code.name, errorCode)
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
