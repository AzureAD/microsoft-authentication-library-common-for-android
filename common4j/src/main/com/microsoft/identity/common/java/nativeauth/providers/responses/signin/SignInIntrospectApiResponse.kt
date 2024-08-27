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
package com.microsoft.identity.common.java.nativeauth.providers.responses.signin

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.nativeauth.providers.IApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiErrorResult
import com.microsoft.identity.common.java.nativeauth.util.isOOB
import com.microsoft.identity.common.java.nativeauth.util.isPassword
import com.microsoft.identity.common.java.nativeauth.util.isRedirect
import java.lang.IllegalStateException
import java.net.HttpURLConnection

/**
 * Represents the raw response from the /oauth/v2.0/introspect endpoint.
 * Can be converted to SignInIntrospectApiResult using the provided toResult() method.
 */
class SignInIntrospectApiResponse(
    @Expose override var statusCode: Int,
    correlationId: String,
    @SerializedName("continuation_token") val continuationToken: String?,
    @Expose @SerializedName("challenge_type") val challengeType: String?,
    @Expose @SerializedName("methods") val methods: List<AuthenticationMethodApiResponse>?,
    @SerializedName("error") val error: String?,
    @SerializedName("error_codes") val errorCodes: List<Int>?,
    @SerializedName("error_description") val errorDescription: String?,
): IApiResponse(statusCode, correlationId) {

    override fun toUnsanitizedString(): String {
        return "SignInIntrospectApiResponse(statusCode=$statusCode, " +
                "correlationId=$correlationId, methods=$methods, " +
                "error=$error, errorDescription=$errorDescription, errorCodes=$errorCodes)"
    }

    override fun toString(): String = "SignInIntrospectApiResponse(statusCode=$statusCode, " +
            "correlationId=$correlationId, methods=$methods"

    /**
    * Maps potential errors returned from the server response, and provide different states based on the response.
    * @see com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInIntrospectApiResponse
    */
    fun toResult(): SignInIntrospectApiResult {
        return when (statusCode) {
            // Handle 400 errors
            HttpURLConnection.HTTP_BAD_REQUEST -> {
                SignInIntrospectApiResult.UnknownError(
                    error = error.orEmpty(),
                    errorDescription = errorDescription.orEmpty(),
                    errorCodes = errorCodes.orEmpty(),
                    correlationId = correlationId
                )
            }

            // Handle success and redirect
            HttpURLConnection.HTTP_OK -> {
                return when {
                    challengeType.isRedirect() -> {
                        SignInIntrospectApiResult.Redirect(
                            correlationId = correlationId
                        )
                    }
                    methods.isNullOrEmpty() -> {
                        SignInIntrospectApiResult.UnknownError(
                            error = ApiErrorResult.INVALID_STATE,
                            errorDescription = "oauth/v2.0/introspect did not return methods",
                            errorCodes = errorCodes.orEmpty(),
                            correlationId = correlationId
                        )
                    }
                    else -> {
                        try {
                            SignInIntrospectApiResult.Success(
                                correlationId = correlationId,
                                continuationToken = continuationToken
                                    ?: return SignInIntrospectApiResult.UnknownError(
                                        error = ApiErrorResult.INVALID_STATE,
                                        errorDescription = "oauth/v2.0/introspect did not return a continuation token",
                                        errorCodes = errorCodes.orEmpty(),
                                        correlationId = correlationId
                                    ),
                                methods = methods.toListOfAuthenticationMethodApiResult()
                            )
                        } catch (e: IllegalStateException) {
                            SignInIntrospectApiResult.UnknownError(
                                error = ApiErrorResult.INVALID_STATE,
                                errorDescription = "oauth/v2.0/introspect did not return valid methods: ${e.message}",
                                errorCodes = errorCodes.orEmpty(),
                                correlationId = correlationId
                            )
                        }
                    }
                }
            }
            // Catch uncommon status codes
            else -> {
                SignInIntrospectApiResult.UnknownError(
                    error = error.orEmpty(),
                    errorDescription = errorDescription.orEmpty(),
                    errorCodes = errorCodes.orEmpty(),
                    correlationId = correlationId
                )
            }
        }
    }
}
