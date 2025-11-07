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
package com.microsoft.identity.common.java.commands.webapps

import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.exception.ErrorStrings
import com.microsoft.identity.common.java.exception.ServiceException
import com.microsoft.identity.common.java.exception.UiRequiredException
import com.microsoft.identity.common.java.exception.UnsupportedBrokerException
import com.microsoft.identity.common.java.exception.UserCancelException
import java.io.IOException

/**
 * Error codes for WebApps operations.
 */
enum class WebAppBrokerErrorCode {
    // Any unexpected error
    UNEXPECTED,
    // The request was cancelled by the user.
    USER_CANCEL,
    // User interaction is required to complete the request. This option is only applicable to requests made silently.
    USER_INTERACTION_REQUIRED,
    // This is Edge-specific and will be returned when the platform API needs to show UI,
    // but is not allowed to do so because of the canShowUI flag being false when API is called by Edge
    UI_NOT_ALLOWED,
    // Network is unavailable and request cannot be completed.
    NO_NETWORK,
    // Errors indicating operation can be re-tried
    TRANSIENT_ERROR,
    // Errors indicating operation cannot be re-tried. For example, when provided authority is not recognized/supported by broker
    PERSISTENT_ERROR,
    // Account is not found in the cache.
    ACCOUNT_UNAVAILABLE,
    // Platform broker invocation is disabled and cannot be performed.
    DISABLED,
    // There were too many requests in a short period of time and the current request is throttled.
    THROTTLED;

    companion object {
        // Simple names of relevant exception types
        val USER_CANCEL_EXCEPTION_NAME = UserCancelException::class.java.simpleName
        val UI_REQUIRED_EXCEPTION_NAME = UiRequiredException::class.java.simpleName
        val IO_EXCEPTION_NAME = IOException::class.java.simpleName
        val JSON_PARSE_EXCEPTION_NAME = JsonParseException::class.java.simpleName
        val JSON_SYNTAX_EXCEPTION_NAME = JsonSyntaxException::class.java.simpleName
        val ILLEGAL_STATE_EXCEPTION_NAME = IllegalStateException::class.java.simpleName
        val NULL_POINTER_EXCEPTION_NAME = NullPointerException::class.java.simpleName
        val CLIENT_EXCEPTION_NAME = ClientException::class.java.simpleName
        val SERVICE_EXCEPTION_NAME = ServiceException::class.java.simpleName
        val UNSUPPORTED_OPERATION_EXCEPTION_NAME = UnsupportedBrokerException::class.java.simpleName

        /**
         * Create a [WebAppBrokerErrorCode] from a [Throwable].
         *
         * @param t The Throwable to classify.
         * @return The corresponding WebAppBrokerErrorCode.
         */
        fun fromThrowable(t: Throwable): WebAppBrokerErrorCode {
            val simple = t::class.java.simpleName
            val clientCode = (t as? ClientException)?.errorCode
            val httpCode = (t as? ServiceException)?.httpStatusCode
            return classify(simple, clientCode, httpCode)
        }

        /**
         * Create a [WebAppBrokerErrorCode] from a [WebAppsErrorResponsePayload].
         *
         * @param errorResponse The WebAppsErrorResponsePayload to classify.
         * @return The corresponding WebAppBrokerErrorCode.
         */
        fun fromSerialized(errorResponse : WebAppsErrorResponsePayload): WebAppBrokerErrorCode {
            val simple = errorResponse.type.substringAfterLast('.')
            return classify(simple, errorResponse.clientErrorCode, errorResponse.httpStatusCode)
        }

        /**
         * Classify the error based on simple type name, client error code, and HTTP status code.
         *
         * @param simpleType The simple name of the exception type.
         * @param clientErrorCode The client error code, if available.
         * @param httpStatusCode The HTTP status code, if available.
         * @return The corresponding WebAppBrokerErrorCode.
         */
        private fun classify(simpleType: String,
                             clientErrorCode: String?,
                             httpStatusCode: Int?): WebAppBrokerErrorCode {
            return when {
                simpleType == USER_CANCEL_EXCEPTION_NAME -> USER_CANCEL
                simpleType == UI_REQUIRED_EXCEPTION_NAME -> USER_INTERACTION_REQUIRED
                simpleType == IO_EXCEPTION_NAME -> NO_NETWORK
                simpleType == JSON_PARSE_EXCEPTION_NAME ||
                        simpleType == JSON_SYNTAX_EXCEPTION_NAME ||
                        simpleType == ILLEGAL_STATE_EXCEPTION_NAME ||
                        simpleType == UNSUPPORTED_OPERATION_EXCEPTION_NAME ||
                        simpleType == NULL_POINTER_EXCEPTION_NAME -> PERSISTENT_ERROR
                clientErrorCode != null -> {
                    when (clientErrorCode.lowercase()) {
                        ClientException.DEVICE_NETWORK_NOT_AVAILABLE,
                        ErrorStrings.NO_NETWORK_CONNECTION_POWER_OPTIMIZATION -> NO_NETWORK
                        ClientException.MALFORMED_URL,
                        ClientException.MISSING_PARAMETER,
                        ErrorStrings.INVALID_REQUEST -> PERSISTENT_ERROR
                        ErrorStrings.SOCKET_TIMEOUT,
                        ErrorStrings.IO_ERROR -> TRANSIENT_ERROR
                        ErrorStrings.UNSUPPORTED_BROKER_VERSION_ERROR_CODE,
                        ErrorStrings.FLIGHT_DISABLED -> DISABLED
                        ClientException.ACCOUNT_NOT_FOUND -> ACCOUNT_UNAVAILABLE
                        ErrorStrings.UI_NOT_ALLOWED -> UI_NOT_ALLOWED
                        else -> UNEXPECTED
                    }
                }
                httpStatusCode == 429 -> THROTTLED
                simpleType == SERVICE_EXCEPTION_NAME -> PERSISTENT_ERROR
                else -> UNEXPECTED
            }
        }
    }
}
