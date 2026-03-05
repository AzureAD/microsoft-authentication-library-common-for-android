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
import com.microsoft.identity.common.java.authscheme.WebAppsPopAuthenticationSchemeInternal
import com.microsoft.identity.common.java.base64.Base64Util
import com.microsoft.identity.common.java.commands.webapps.WebAppsGetTokenSubOperationRequest
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.exception.ErrorStrings
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
         * Validates that the redirect URI origin matches the sender origin for MSAL JS requests.
         *
         * @param redirectUri The redirect URI from the request.
         * @param senderUri The sender URI to compare against the redirect URI.
         * @throws ClientException if the redirect URI origin does not match the sender origin.
         */
        @JvmStatic
        fun validateMsalJsRedirectOrigin(redirectUri: String,
                                         senderUri: String) {
            if (!hasSameSchemeAndHost(senderUri, redirectUri)) {
                throw ClientException(
                    ErrorStrings.INVALID_REQUEST,
                    "The redirect URI origin does not match the sender origin."
                )
            }
        }

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
                    AuthenticationConstants.Broker.BROKER_WEB_APPS_SUCCESSFUL_RESULT,
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
        fun createErrorResponseBundle(t: Throwable, description: String?): Bundle {
            return Bundle().apply {
                putString(
                    AuthenticationConstants.Broker.BROKER_WEB_APPS_ERROR_RESULT,
                    createErrorResponseString(t, description)
                )
            }
        }

        @JvmStatic
        fun createErrorResponseString(t: Throwable, description: String?): String {
            val errorDescription = if (!description.isNullOrBlank()) {
                "$description: ${t.javaClass.simpleName}: ${t.message}"
            } else {
                "Error occurred during operation: ${t.javaClass.simpleName}: ${t.message}"
            }
            return ObjectMapper.serializeObjectToJsonString(WebAppError(t, errorDescription))
        }

        /**
         * Utility method to require a non-null and non-empty value, throwing a ClientException if null or empty.
         *
         * @param value The value to check.
         * @param name The name of the parameter (for error message).
         * @return The non-null, non-empty value.
         * @throws ClientException if the value is null or empty.
         */
        @JvmStatic
        @Throws(ClientException::class)
        fun <T> requireNotNullOrEmpty(value: T?, name: String): T {
            if (value == null) {
                throw ClientException(ClientException.MISSING_PARAMETER, "$name is null.")
            }
            if (value is CharSequence && value.isEmpty()) {
                throw ClientException(ClientException.MISSING_PARAMETER, "$name is empty.")
            }
            if (value is Collection<*> && value.isEmpty()) {
                throw ClientException(ClientException.MISSING_PARAMETER, "$name is empty.")
            }
            return value
        }

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

        /**
         * Parses and validates PoP (Proof of Possession) parameters from a
         * [WebAppsGetTokenSubOperationRequest], returning a [WebAppsPopAuthenticationSchemeInternal]
         * if the request specifies PoP, or null if Bearer/unspecified.
         *
         * Top-level fields (`tokenType`, `reqCnf`) take priority over values found in
         * `extraParameters`.
         *
         * @param request The token sub-operation request to parse.
         * @return A [WebAppsPopAuthenticationSchemeInternal] if tokenType is "pop" with a valid
         *         reqCnf, or null if no PoP params are present (Bearer is assumed).
         * @throws ClientException if PoP params are partially specified or otherwise invalid.
         */
        @JvmStatic
        @Throws(ClientException::class)
        fun parsePopAuthSchemeFromRequest(request: WebAppsGetTokenSubOperationRequest): WebAppsPopAuthenticationSchemeInternal? {
            try {
                // Check top-level fields first (higher priority than extraParameters).
                // Each field is resolved independently: use top-level if present, else fall back
                // to extraParameters.
                val tokenType = if (!request.tokenType.isNullOrBlank()) {
                    request.tokenType
                } else {
                    request.extraParameters?.get(WebAppsGetTokenSubOperationRequest.FIELD_TOKEN_TYPE)
                }

                val reqCnf = if (!request.reqCnf.isNullOrBlank()) {
                    request.reqCnf
                } else {
                    request.extraParameters?.get(WebAppsGetTokenSubOperationRequest.FIELD_REQ_CNF)
                }

                // If neither tokenType nor reqCnf is present, no PoP scheme needed.
                if (tokenType.isNullOrBlank() && reqCnf.isNullOrBlank()) {
                    return null
                }

                // Validate: tokenType is "pop" but reqCnf is missing or empty.
                if ("pop".equals(tokenType, ignoreCase = true) && reqCnf.isNullOrBlank()) {
                    throw ClientException(
                        ClientException.MISSING_PARAMETER,
                        "tokenType is 'pop' but reqCnf is missing or empty."
                    )
                }

                // Validate: reqCnf is provided but tokenType is missing or empty.
                if (!reqCnf.isNullOrBlank() && tokenType.isNullOrBlank()) {
                    throw ClientException(
                        ClientException.MISSING_PARAMETER,
                        "reqCnf is provided but tokenType is missing or empty."
                    )
                }

                // If tokenType is "pop" with a valid reqCnf, create and return the scheme.
                if ("pop".equals(tokenType, ignoreCase = true)) {
                    return WebAppsPopAuthenticationSchemeInternal(reqCnf!!)
                }

                // tokenType is present but not "pop" (e.g., "bearer"); no PoP scheme needed.
                return null
            } catch (e: Exception) {
                if (e is ClientException) throw e
                throw ClientException(
                    ErrorStrings.UNKNOWN_ERROR,
                    "Failed to parse PoP parameters from request: ${e.message}",
                    e
                )
            }
        }
    }
}
