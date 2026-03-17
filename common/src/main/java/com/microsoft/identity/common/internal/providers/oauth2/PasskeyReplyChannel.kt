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
package com.microsoft.identity.common.internal.providers.oauth2

import android.annotation.SuppressLint
import androidx.credentials.exceptions.CreateCredentialCancellationException
import com.microsoft.identity.common.internal.fido.WebAuthnJsonUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.credentials.exceptions.CreateCredentialInterruptedException
import androidx.credentials.exceptions.CreateCredentialProviderConfigurationException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import androidx.webkit.JavaScriptReplyProxy
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.opentelemetry.BaggageExtension
import com.microsoft.identity.common.java.opentelemetry.OTelUtility
import com.microsoft.identity.common.java.opentelemetry.SpanExtension
import com.microsoft.identity.common.java.opentelemetry.SpanName
import com.microsoft.identity.common.logging.Logger
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import org.json.JSONObject


/**
 * Communication channel for sending WebAuthn responses back to JavaScript via [JavaScriptReplyProxy].
 *
 * Formats messages as JSON containing status, data, and request type for WebAuthn credential operations.
 *
 * @property replyProxy Proxy for sending messages to JavaScript.
 * @property requestType Type of WebAuthn request (e.g., "create", "get"). Defaults to "unknown".
 * @property Context for OpenTelemetry span creation. Optional; if not provided, telemetry will be skipped with a warning.
 * @property telemetryScope Coroutine scope used to record additional telemetry in a background
 *   thread after the reply has already been posted, so callers are not blocked by telemetry work.
 *   Defaults to a new [CoroutineScope] backed by [Dispatchers.IO].
 */
class PasskeyReplyChannel(
    private val replyProxy: JavaScriptReplyProxy,
    private val requestType: String = "unknown",
    private val otelContext: Context? = null,
    private val telemetryScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        const val TAG = "PasskeyReplyChannel"

        // JSON message structure keys
        const val STATUS_KEY = "status"
        const val DATA_KEY = "data"
        const val TYPE_KEY = "type"

        // DOMException error details keys
        const val DOM_EXCEPTION_MESSAGE_KEY = "domExceptionMessage"
        const val DOM_EXCEPTION_NAME_KEY = "domExceptionName"

        // Message status values
        const val SUCCESS_STATUS = "success"
        const val ERROR_STATUS = "error"

        // DOMException names per W3C WebAuthn specification
        const val DOM_EXCEPTION_NOT_ALLOWED_ERROR = "NotAllowedError"
        const val DOM_EXCEPTION_ABORT_ERROR = "AbortError"
        const val DOM_EXCEPTION_NOT_SUPPORTED_ERROR = "NotSupportedError"
        const val DOM_EXCEPTION_UNKNOWN_ERROR = "UnknownError"

        private val parentAttributeNames = arrayListOf(
            AttributeName.correlation_id,
            AttributeName.tenant_id,
            AttributeName.account_type,
            AttributeName.calling_package_name
        )
    }

    /**
     * Sealed class representing messages sent to JavaScript.
     */
    sealed class ReplyMessage {
        // Message type (e.g., "create", "get").
        abstract val type: String
        // Message status ("success" or "error").
        abstract val status: String
        // Message data as a JSON object.
        // Either credential data for success or {domExceptionMessage, domExceptionName} for error.
        abstract val data: JSONObject

        /**
         * Success message containing credential data.
         *
         * @property json JSON string with credential response data.
         * @property type Request type that succeeded.
         */
        class Success(val json: String, override val type: String) : ReplyMessage() {
            override val status = SUCCESS_STATUS
            override val data: JSONObject =
                runCatching { JSONObject(json) }.getOrElse { JSONObject() }
        }

        /**
         * Error message with DOMException details.
         *
         * @property domExceptionMessage Error description.
         * @property domExceptionName DOMException name per W3C spec.
         * @property type Request type that failed.
         */
        class Error(
            val domExceptionMessage: String,
            val domExceptionName: String = DOM_EXCEPTION_NOT_ALLOWED_ERROR,
            override val type: String
        ) : ReplyMessage() {
            override val status = ERROR_STATUS
            override val data: JSONObject
                get() {
                    return JSONObject().apply {
                        put(DOM_EXCEPTION_MESSAGE_KEY, domExceptionMessage)
                        put(DOM_EXCEPTION_NAME_KEY, domExceptionName)
                    }
                }
        }

        /** Serializes the message to JSON string. */
        override fun toString(): String {
            return JSONObject().apply {
                put(STATUS_KEY, status)
                put(DATA_KEY, data)
                put(TYPE_KEY, type)
            }.toString()
        }
    }

    /**
     * Resolves the parent [SpanContext] from the provided [otelContext].
     *
     * Returns `null` and emits a warning if no OTel context was supplied, so callers can still
     * create a root span rather than failing outright.
     *
     * @param methodTag Logging tag of the calling method, included in the warning message.
     * @return The [SpanContext] extracted from [otelContext], or `null`.
     */
    private fun resolveParentSpanContext(methodTag: String): SpanContext? =
        if (otelContext == null) {
            Logger.warn(methodTag, "No OpenTelemetry context provided. Telemetry will not be recorded for this operation.")
            null
        } else {
            SpanExtension.fromContext(otelContext).spanContext
        }

    /**
     * Posts a success message with credential data and returns immediately.
     *
     * The reply is sent to JavaScript right away. Additional telemetry attributes that require
     * inspecting the response (e.g. passkey_origin) are recorded asynchronously
     * on [telemetryScope] so that the caller is never blocked by telemetry work.
     *
     * @param json JSON string containing the credential response.
     */
    @SuppressLint("RequiresFeature", "Only called when feature is available")
    fun postSuccess(json: String) {
        val methodTag = "$TAG:postSuccess"
        val span = OTelUtility.createSpanFromParent(
            SpanName.PasskeyWebListener.name,
            resolveParentSpanContext(methodTag)
        )
        // We use a flag or a structured try-catch to ensure span.end()
        // is called exactly once.
        var handedOffToBackground = false

        try {
            // 1. Immediate Work
            val successMessage = ReplyMessage.Success(json, requestType).toString()
            replyProxy.postMessage(successMessage)

            span.setStatus(StatusCode.OK)
            span.setAttribute(AttributeName.passkey_operation_type.name, requestType)
            val otelContextCurrentSpan = Context.current().with(span)
            val baggage = BaggageExtension.fromContext(otelContext)

            // 2. Hand off post-success telemetry to background worker
            val job = telemetryScope.launch(otelContextCurrentSpan.asContextElement()) {
                recordPostSuccessTelemetry(span, json, baggage)
            }
            job.invokeOnCompletion {
                span.end()
            }
            handedOffToBackground = true

        } catch (throwable: Throwable) {
            // 3. Error Path: If postMessage fails OR launch fails
            span.recordException(throwable)
            span.setStatus(StatusCode.ERROR)
            Logger.error(methodTag, "Immediate execution failed", throwable)
            throw throwable
        } finally {
            // 4. Lifecycle Guard: Only end here if we didn't successfully
            // start the background task.
            if (!handedOffToBackground) {
                span.end()
            }
        }
    }

    /**
     * Records additional post-success telemetry on a background thread.
     *
     * This method is invoked only after the success reply has already been posted to JavaScript.
     * It must remain non-blocking for the caller path and should never prevent a successful
     * response from being returned.
     *
     * @param span The span created in [postSuccess], owned by the background task lifecycle.
     * @param json JSON payload returned from WebAuthn operation.
     */
    private fun recordPostSuccessTelemetry(span: Span, json: String, baggage: Baggage? = null) {
        try {
            SpanExtension.makeCurrentSpan(span).use {
                parentAttributeNames.forEach { attributeName ->
                    baggage?.getEntryValue(attributeName.name)?.let { value ->
                        span.setAttribute(attributeName.name, value)
                    }
                }

                if (requestType == PasskeyWebListener.CREATE_UNIQUE_KEY) {
                    WebAuthnJsonUtil.extractAaguidFromRegistrationResponse(json)?.let {
                        span.setAttribute(AttributeName.passkey_aaguid.name, it)
                    }
                    WebAuthnJsonUtil.extractOriginFromRegistrationResponse(json)?.let {
                        span.setAttribute(AttributeName.passkey_origin.name, it)
                    }
                }
            }
        } catch (exception: Exception) {
            Logger.warn(
                TAG,
                "Failed to record post-success passkey telemetry for requestType: $requestType, ${exception.message}"
            )
        } finally {
            // The background worker owns span completion for the success path.
            span.end()
        }
    }

    /**
     * Posts an error message based on a thrown exception.
     *
     * Maps credential exceptions to appropriate DOMException types.
     *
     * @param throwable Exception to convert and send.
     */
    @SuppressLint("RequiresFeature", "Only called when feature is available")
    fun postError(throwable: Throwable) {
        val methodTag = "$TAG:postError"
        val span = OTelUtility.createSpanFromParent(
            SpanName.PasskeyWebListener.name,
            resolveParentSpanContext(methodTag)
        )

        try {
            SpanExtension.makeCurrentSpan(span).use {
                val errorMessage = throwableToErrorMessage(throwable)
                replyProxy.postMessage(errorMessage.toString())
                span.setAttribute(AttributeName.passkey_operation_type.name, requestType)
                span.setAttribute(AttributeName.passkey_dom_exception_name.name, errorMessage.domExceptionName)
                span.setStatus(StatusCode.ERROR)
                span.recordException(throwable)
                Logger.error(methodTag, "RequestType: $requestType failed with error: $errorMessage", throwable)
            }
        } catch (unexpectedException: Throwable) {
            span.setStatus(StatusCode.ERROR)
            span.recordException(unexpectedException)
            span.setAttribute(AttributeName.passkey_operation_type.name, requestType)
            Logger.error(methodTag, "Reply message failed", unexpectedException)
            throw unexpectedException
        } finally {
            span.end() // Always end the span
        }
    }

    /**
     * Maps credential exceptions to DOMException error messages.
     *
     * Conversion table:
     * - Cancellation/No credential → NotAllowedError
     * - Interruption → AbortError
     * - Configuration → NotSupportedError
     * - Unknown → UnknownError
     *
     * @param throwable Exception to map.
     * @return Error message with appropriate DOMException details.
     */
    private fun throwableToErrorMessage(throwable: Throwable): ReplyMessage.Error {
        val errorMessage = throwable.message ?: "Unknown error (empty message)"

        val exceptionName = when (throwable) {
            // Cancellation exceptions - User cancelled
            is CreateCredentialCancellationException,
            is GetCredentialCancellationException,
            is NoCredentialException -> DOM_EXCEPTION_NOT_ALLOWED_ERROR

            // Interruption exceptions - Operation aborted
            is CreateCredentialInterruptedException,
            is GetCredentialInterruptedException -> DOM_EXCEPTION_ABORT_ERROR

            // Provider configuration exceptions - Not supported
            is CreateCredentialProviderConfigurationException,
            is GetCredentialProviderConfigurationException -> DOM_EXCEPTION_NOT_SUPPORTED_ERROR

            // Unknown exceptions
            is CreateCredentialUnknownException,
            is GetCredentialUnknownException -> DOM_EXCEPTION_UNKNOWN_ERROR

            // Default case for other exceptions
            else -> DOM_EXCEPTION_NOT_ALLOWED_ERROR
        }

        return ReplyMessage.Error(
            domExceptionMessage = errorMessage,
            domExceptionName = exceptionName,
            type = requestType
        )
    }
}
