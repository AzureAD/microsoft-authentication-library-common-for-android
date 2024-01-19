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
import com.microsoft.identity.common.java.nativeauth.util.*
import java.net.HttpURLConnection

/**
 * Represents the raw response from the Reset Password /submit endpoint.
 * Can be converted to ResetPasswordChallengeApiResult using the provided toResult() method.
 */
class ResetPasswordSubmitApiResponse(
    @Expose override var statusCode: Int,
    correlationId: String?,
    @SerializedName("continuation_token") val continuationToken: String?,
    @Expose @SerializedName("poll_interval") val pollInterval: Int?,
    @SerializedName("error") val error: String?,
    @SerializedName("error_description") val errorDescription: String?,
    @SerializedName("error_uri") val errorUri: String?,
    @SerializedName("suberror") val subError: String?
): IApiResponse(statusCode, correlationId) {

    companion object {
        private val TAG = ResetPasswordSubmitApiResponse::class.java.simpleName
        private const val MINIMUM_POLL_COMPLETION_INTERVAL_IN_SECONDS = 1
        private const val MAXIMUM_POLL_COMPLETION_INTERVAL_IN_SECONDS = 15
        private const val DEFAULT_POLL_COMPLETION_INTERVAL_IN_SECONDS = 2

    }

    /**
     * Maps potential errors returned from the server response, and provide different states based on the response.
     * @see com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordSubmitApiResult
     */
    fun toResult(): ResetPasswordSubmitApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.toResult"
        )

        return when (statusCode) {

            // Handle 400 errors
            HttpURLConnection.HTTP_BAD_REQUEST -> {
                return when {
                    subError.isPasswordBanned() || subError.isPasswordTooShort() || subError.isPasswordTooLong() || subError.isPasswordRecentlyUsed() ||
                            subError.isPasswordTooWeak() || subError.isPasswordInvalid() -> {
                        ResetPasswordSubmitApiResult.PasswordInvalid(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            subError = subError.orEmpty(),
                            correlationId = correlationId
                        )
                    }
                    error.isExpiredToken() -> {
                        ResetPasswordSubmitApiResult.ExpiredToken(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            correlationId = correlationId
                        )
                    }
                    else -> {
                        ResetPasswordSubmitApiResult.UnknownError(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            correlationId = correlationId
                        )
                    }
                }
            }

            // Handle success and redirect
            HttpURLConnection.HTTP_OK -> {
                ResetPasswordSubmitApiResult.SubmitSuccess(
                    continuationToken = continuationToken
                        ?: return ResetPasswordSubmitApiResult.UnknownError(
                            error = "invalid_state",
                            errorDescription = "ResetPassword /submit successful, but did not return a flow token",
                            correlationId = correlationId
                        ),
                    pollInterval = clampPollInterval(pollInterval),
                    correlationId = correlationId
                )
            }

            // Catch uncommon status codes
            else -> {
                ResetPasswordSubmitApiResult.UnknownError(
                    error = error.orEmpty(),
                    errorDescription = errorDescription.orEmpty(),
                    correlationId = correlationId
                )
            }
        }
    }

    private fun clampPollInterval(pollIntervalInSeconds: Int?): Int {
        if (pollIntervalInSeconds == null || pollIntervalInSeconds < MINIMUM_POLL_COMPLETION_INTERVAL_IN_SECONDS || pollIntervalInSeconds > MAXIMUM_POLL_COMPLETION_INTERVAL_IN_SECONDS)
        {
            return DEFAULT_POLL_COMPLETION_INTERVAL_IN_SECONDS
        }
        return pollIntervalInSeconds;
    }
}
