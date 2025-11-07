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
package com.microsoft.identity.common.internal.util

import android.os.Bundle
import com.microsoft.identity.common.java.commands.webapps.WebAppError
import com.microsoft.identity.common.adal.internal.AuthenticationConstants
import com.microsoft.identity.common.java.base64.Base64Util
import com.microsoft.identity.common.java.commands.webapps.WebAppsErrorResponsePayload
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.util.ObjectMapper
import com.microsoft.identity.common.logging.Logger
import java.net.URI

/**
 * Utility class for Web Apps related operations.
 */
class WebAppsUtil {
    companion object {
        private val TAG = WebAppsUtil::class.simpleName

        const val DEFAULT_AUTHORITY = "https://login.microsoftonline.com/common"

        /**
         * Create a [Bundle] containing a successful response object.
         *
         * @param responseObject The response object to include in the bundle.
         * @return A [Bundle] containing the response object.
         */
        @JvmStatic
        fun getResponseBundle(responseObject: Any): Bundle {
            return Bundle().apply {
                putString(
                    AuthenticationConstants.Broker.BROKER_WEB_APPS_RESPONSE,
                    ObjectMapper.serializeObjectToJsonString(responseObject)
                )
            }
        }


        /**
         * Create a [Bundle] containing an error response from a [Throwable] and optional description.
         *
         * @param t The throwable to create the error response from.
         * @param description An optional description to include in the error response.
         * @return A [Bundle] containing the error response.
         */
        @JvmStatic
        fun createErrorResponse(t: Throwable, description: String?): Bundle {
            val errorDescription = if (!description.isNullOrBlank()) {
                "$description: ${t.javaClass.simpleName}: ${t.message}"
            } else {
                "Error occurred during operation: ${t.javaClass.simpleName}: ${t.message}"
            }
            return Bundle().apply {
                putString(
                    AuthenticationConstants.Broker.BROKER_WEB_APPS_ERROR,
                    ObjectMapper.serializeObjectToJsonString(WebAppError(t, errorDescription))
                )
            }
        }

        /**
         * Create a [Bundle] containing an error response from a serialized [WebAppsErrorResponsePayload] and optional description.
         *
         * @param response The serialized WebAppsErrorResponsePayload to create the error response from.
         * @param description An optional description to include in the error response.
         * @return A [Bundle] containing the error response.
         */
        @JvmStatic
        fun createErrorResponseFromSerialized(response: WebAppsErrorResponsePayload, description: String?): Bundle {
            val errorDescription = if (!description.isNullOrBlank()) {
                "$description: ${response.type.substringAfterLast('.')}: ${response.message}"
            } else {
                "Error occurred during operation: ${response.type.substringAfterLast('.')}: ${response.message}"
            }
            return Bundle().apply {
                putString(
                    AuthenticationConstants.Broker.BROKER_WEB_APPS_ERROR,
                    ObjectMapper.serializeObjectToJsonString(WebAppError(response, errorDescription))
                )
            }
        }

        /**
         * Utility method to require a non-null value, throwing a ClientException if null.
         *
         * @param value The value to check for nullity.
         * @param name The name of the parameter, used in the exception message.
         * @return The non-null value.
         * @throws ClientException if the value is null.
         */
        @JvmStatic
        @Throws(ClientException::class)
        fun <T> requireNotNullClient(value: T?, name: String): T =
            value ?: throw ClientException(ClientException.MISSING_PARAMETER, "$name is null.")

        /**
         * Computes the remaining seconds until the target epoch time.
         *
         * @param epochSecondsStr The target epoch time in seconds as a string.
         * @return The remaining seconds until the target time, or 0 if the target time has passed or is invalid.
         */
        @JvmStatic
        fun computeRemainingSeconds(epochSecondsStr: String?): Long {
            if (epochSecondsStr.isNullOrBlank()) return 0L
            return try {
                val target = epochSecondsStr.toLong()
                val now = System.currentTimeMillis() / 1000L
                val delta = target - now
                if (delta > 0) delta else 0L
            } catch (e: NumberFormatException) {
                Logger.warn("$TAG:computeRemainingSeconds", "Invalid epoch seconds: $epochSecondsStr")
                0L
            }
        }

        /**
         * Converts a homeAccountId of the form uid.utid into the raw client_info string
         * (Base64URL encoded JSON: {"uid":"<uid>","utid":"<utid>"}).
         *
         * @param homeAccountId The home account id (uid.utid).
         * @return Base64URL (unpadded) encoded client_info or null if input invalid.
         */
        @JvmStatic
        fun homeAccountIdToClientInfo(homeAccountId: String?): String? {
            if (homeAccountId.isNullOrBlank()) return null
            val parts = homeAccountId.split(".")
            if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) return null
            val json = "{\"uid\":\"${parts[0]}\",\"utid\":\"${parts[1]}\"}"
            return Base64Util.encodeUrlSafeString(json)
        }

        @JvmStatic
        fun getSchemeAndHost(url: String): String {
            val uri = try { URI(url.trim()) } catch (e: Exception) {
                throw IllegalArgumentException("Failed to parse URL for scheme and host validation in WebApps. The URL is invalid.", e)
            }
            val scheme = uri.scheme ?: throw IllegalArgumentException("Failed to parse URL for scheme and host validation in WebApps. The URL is invalid.")
            val host = uri.host ?: throw IllegalArgumentException("URL must include a host for WebApps validation.")
            return "${scheme.lowercase()}://${host.lowercase()}"
        }

        @JvmStatic
        fun hasSameSchemeAndHost(urlA: String, urlB: String): Boolean {
            return getSchemeAndHost(urlA) == getSchemeAndHost(urlB)
        }
    }
}
