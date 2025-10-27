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
import com.microsoft.identity.common.java.opentelemetry.SpanExtension
import com.microsoft.identity.common.logging.Logger
import io.opentelemetry.api.trace.StatusCode
import org.json.JSONObject
import kotlin.jvm.Throws


/**
 * Communication channel for sending WebAuthn responses back to JavaScript via [JavaScriptReplyProxy].
 *
 * Formats messages as JSON containing status, data, and request type for WebAuthn credential operations.
 *
 * @property replyProxy Proxy for sending messages to JavaScript.
 * @property requestType Type of WebAuthn request (e.g., "create", "get"). Defaults to "unknown".
 */
class PasskeyReplyChannel(
    private val replyProxy: JavaScriptReplyProxy,
    private val requestType: String = "unknown"
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
     * Posts a success message with credential data.
     *
     * @param json JSON string containing the credential response.
     */
    fun postSuccess(json: String) {
        val methodTag = "$TAG:postSuccess"
        send(ReplyMessage.Success(json, requestType))
        SpanExtension.current().apply {
            setStatus(StatusCode.OK)
            setAttribute(AttributeName.passkey_operation_type.name, requestType)
            end()
        }
        Logger.info(methodTag, "RequestType: $requestType, was successful.")
    }

    /**
     * Posts an error message with a custom error description.
     *
     * @param errorMessage Error description to send.
     */
    fun postError(errorMessage: String) {
        postErrorInternal(
            ReplyMessage.Error(domExceptionMessage = errorMessage, type = requestType)
        )
        SpanExtension.current().apply {
            setAttribute(AttributeName.passkey_operation_type.name, requestType)
            setStatus(StatusCode.ERROR)
            setAttribute(AttributeName.error_message.name, errorMessage)
            end()
        }
    }

    /**
     * Posts an error message based on a thrown exception.
     *
     * Maps credential exceptions to appropriate DOMException types.
     *
     * @param throwable Exception to convert and send.
     */
    fun postError(throwable: Throwable) {
        postErrorInternal(throwableToErrorMessage(throwable))
        SpanExtension.current().apply {
            setAttribute(AttributeName.passkey_operation_type.name, requestType)
            setStatus(StatusCode.ERROR)
            recordException(throwable)
            end()
        }
    }

    /**
     * Internal method to send error messages and log them.
     */
    private fun postErrorInternal(errorMessage: ReplyMessage.Error) {
        val methodTag = "$TAG:postError"
        send(errorMessage)
        Logger.error(methodTag, "RequestType: $requestType, failed with error: $errorMessage", null)
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

    /**
     * Sends a message to JavaScript via the reply proxy.
     */
    @Throws (Throwable::class)
    @SuppressLint("RequiresFeature", "Only called when feature is available")
    private fun send(message: ReplyMessage) {
        val methodTag = "$TAG:send"
        try {
            replyProxy.postMessage(message.toString())
        } catch (throwable: Throwable) {
            SpanExtension.current().apply {
                setStatus(StatusCode.ERROR)
                setAttribute(AttributeName.passkey_operation_type.name, requestType)
                recordException(throwable)
                end()
            }
            Logger.error(methodTag, "Reply message failed", throwable)
            throw throwable
        }
    }
}
