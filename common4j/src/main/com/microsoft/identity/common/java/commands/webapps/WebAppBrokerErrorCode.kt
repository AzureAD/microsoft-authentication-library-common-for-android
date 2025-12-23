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
import org.json.JSONException
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

        /**
         * Map a Throwable to a WebAppBrokerErrorCode.
         *
         * @param t The Throwable to map.
         * @return The corresponding WebAppBrokerErrorCode.
         */
        fun fromThrowable(t: Throwable): WebAppBrokerErrorCode {
            when (t) {
                is UserCancelException -> return USER_CANCEL
                is UiRequiredException -> return USER_INTERACTION_REQUIRED
                is IOException -> return NO_NETWORK
                is JsonParseException,
                is JsonSyntaxException,
                is JSONException,
                is IllegalStateException,
                is UnsupportedBrokerException,
                is NullPointerException -> return PERSISTENT_ERROR
            }

            // ClientException specific mapping
            if (t is ClientException) {
                val code = t.errorCode?.lowercase()
                return mapClientErrorCode(code)
            }

            // ServiceException mapping (HTTP based)
            if (t is ServiceException) {
                val http = t.httpStatusCode
                if (http == 429) return THROTTLED
                return PERSISTENT_ERROR
            }

            return UNEXPECTED
        }

        /**
         * Map ClientException error codes to WebAppBrokerErrorCode.
         *
         * @param code The ClientException error code.
         * @return The corresponding WebAppBrokerErrorCode.
         */
        private fun mapClientErrorCode(code: String?): WebAppBrokerErrorCode {
            return when (code) {
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
    }
}
