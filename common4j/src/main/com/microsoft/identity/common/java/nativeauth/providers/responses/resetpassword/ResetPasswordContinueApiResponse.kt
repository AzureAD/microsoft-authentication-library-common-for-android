//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.nativeauth.providers.IApiResponse
import com.microsoft.identity.common.java.nativeauth.util.isExpiredToken
import com.microsoft.identity.common.java.nativeauth.util.isInvalidGrant
import com.microsoft.identity.common.java.nativeauth.util.isInvalidOOBValue
import com.microsoft.identity.common.java.nativeauth.util.isRedirect
import java.net.HttpURLConnection

/**
 * Represents the raw response from the Reset Password /continue endpoint.
 * Can be converted to ResetPasswordChallengeApiResult using the provided toResult() method.
 */
class ResetPasswordContinueApiResponse(
    @Expose override var statusCode: Int,
    correlationId: String,
    @SerializedName("continuation_token") val continuationToken: String?,
    @Expose @SerializedName("challenge_type") val challengeType: String?,
    @Expose @SerializedName("expires_in") val expiresIn: Int?,
    @SerializedName("error") val error: String?,
    @SerializedName("error_description") val errorDescription: String?,
    @SerializedName("error_uri") val errorUri: String?,
    @SerializedName("suberror") val subError: String?
): IApiResponse(statusCode, correlationId) {

    companion object {
        private val TAG = ResetPasswordContinueApiResponse::class.java.simpleName
    }

    /**
     * Maps potential errors returned from the server response, and provide different states based on the response.
     * @see com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordContinueApiResult
     */
    fun toResult(): ResetPasswordContinueApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.toResult" )

        return when (statusCode) {

            // Handle 400 errors
            HttpURLConnection.HTTP_BAD_REQUEST -> {
                return when {
                    error.isInvalidGrant() -> {
                        return when {
                            subError.isInvalidOOBValue() -> {
                                ResetPasswordContinueApiResult.CodeIncorrect(
                                    error = error.orEmpty(),
                                    errorDescription = errorDescription.orEmpty(),
                                    subError = subError.orEmpty(),
                                    correlationId = correlationId
                                )
                            }
                            else -> {
                                ResetPasswordContinueApiResult.UnknownError(
                                    error = error.orEmpty(),
                                    errorDescription = errorDescription.orEmpty(),
                                    correlationId = correlationId
                                )
                            }
                        }
                    }
                    error.isExpiredToken() -> {
                        ResetPasswordContinueApiResult.ExpiredToken(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            correlationId = correlationId
                        )
                    }
                    else -> {
                        ResetPasswordContinueApiResult.UnknownError(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            correlationId = correlationId
                        )
                    }
                }
            }

            // Handle success and redirect
            HttpURLConnection.HTTP_OK -> {
                if (challengeType.isRedirect()) {
                    ResetPasswordContinueApiResult.Redirect(
                        correlationId = correlationId
                    )
                }
                else {
                    ResetPasswordContinueApiResult.PasswordRequired(
                        continuationToken = continuationToken
                            ?: return ResetPasswordContinueApiResult.UnknownError(
                                error = "invalid_state",
                                errorDescription = "ResetPassword /continue successful, but did not return a continuation token",
                                correlationId = correlationId
                            ),
                        expiresIn = expiresIn,
                        correlationId = correlationId
                    )
                }
            }

            // Catch uncommon status codes
            else -> {
                ResetPasswordContinueApiResult.UnknownError(
                    error = error.orEmpty(),
                    errorDescription = errorDescription.orEmpty(),
                    correlationId = correlationId
                )
            }
        }
    }
}
