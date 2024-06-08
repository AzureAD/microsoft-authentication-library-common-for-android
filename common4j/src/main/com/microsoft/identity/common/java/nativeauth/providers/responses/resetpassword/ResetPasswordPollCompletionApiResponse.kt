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
import com.microsoft.identity.common.java.nativeauth.util.isPasswordBanned
import com.microsoft.identity.common.java.nativeauth.util.isPasswordInvalid
import com.microsoft.identity.common.java.nativeauth.util.isPasswordRecentlyUsed
import com.microsoft.identity.common.java.nativeauth.util.isPasswordTooLong
import com.microsoft.identity.common.java.nativeauth.util.isPasswordTooShort
import com.microsoft.identity.common.java.nativeauth.util.isPasswordTooWeak
import com.microsoft.identity.common.java.nativeauth.util.isPollInProgress
import com.microsoft.identity.common.java.nativeauth.util.isPollSucceeded
import com.microsoft.identity.common.java.nativeauth.util.isUserNotFound
import java.net.HttpURLConnection

/**
 * Represents the raw response from the Reset Password /poll_completion endpoint.
 * Can be converted to ResetPasswordChallengeApiResult using the provided toResult() method.
 */
class ResetPasswordPollCompletionApiResponse(
    @Expose override var statusCode: Int,
    correlationId: String,
    @SerializedName("continuation_token") val continuationToken: String?,
    @Expose @SerializedName("status") val status: String?,
    @SerializedName("expires_in") val expiresIn: Int?,
    @SerializedName("error") val error: String?,
    @SerializedName("error_description") val errorDescription: String?,
    @SerializedName("error_uri") val errorUri: String?,
    @SerializedName("suberror") val subError: String?
): IApiResponse(statusCode, correlationId) {

    override fun toUnsanitizedString(): String {
        return "ResetPasswordPollCompletionApiResponse(statusCode=$statusCode, " +
                "correlationId=$correlationId, status=$status, expiresIn=$expiresIn " +
                "error=$error, errorUri=$errorUri, errorDescription=$errorDescription, subError=$subError)"
    }

    override fun toString(): String = "ResetPasswordPollCompletionApiResponse(statusCode=$statusCode, " +
            "correlationId=$correlationId"

    companion object {
        private val TAG = ResetPasswordPollCompletionApiResponse::class.java.simpleName
    }

    /**
     * Maps potential errors returned from the server response, and provide different states based on the response.
     * @see com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordPollCompletionApiResult
     */
    fun toResult(): ResetPasswordPollCompletionApiResult {
        return when (statusCode) {

            // Handle 400 errors
            HttpURLConnection.HTTP_BAD_REQUEST -> {
                return when {
                    error.isInvalidGrant() -> {
                        return when {
                            subError.isPasswordBanned() || subError.isPasswordTooShort() || subError.isPasswordTooLong() || subError.isPasswordRecentlyUsed() ||
                                    subError.isPasswordTooWeak() || subError.isPasswordInvalid() -> {
                                ResetPasswordPollCompletionApiResult.PasswordInvalid(
                                    error = error.orEmpty(),
                                    errorDescription = errorDescription.orEmpty(),
                                    subError = subError.orEmpty(),
                                    correlationId = correlationId
                                )
                            }
                            else -> {
                                ResetPasswordPollCompletionApiResult.UnknownError(
                                    error = error.orEmpty(),
                                    errorDescription = errorDescription.orEmpty(),
                                    correlationId = correlationId
                                )
                            }
                        }
                    }
                    error.isExpiredToken() -> {
                        ResetPasswordPollCompletionApiResult.ExpiredToken(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            correlationId = correlationId
                        )
                    }
                    error.isUserNotFound() -> {
                        ResetPasswordPollCompletionApiResult.UserNotFound(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            correlationId = correlationId
                        )
                    }
                    else -> {
                        ResetPasswordPollCompletionApiResult.UnknownError(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            correlationId = correlationId
                        )
                    }
                }
            }

            // Handle success and redirect
            HttpURLConnection.HTTP_OK -> {
                return when {
                    status.isPollInProgress() -> {
                        ResetPasswordPollCompletionApiResult.InProgress(
                            correlationId = correlationId
                        )
                    }
                    status.isPollSucceeded() -> {
                        ResetPasswordPollCompletionApiResult.PollingSucceeded(
                            continuationToken = continuationToken,
                            expiresIn = expiresIn,
                            correlationId = correlationId
                        )
                    }
                    else -> {
                        ResetPasswordPollCompletionApiResult.PollingFailed(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            correlationId = correlationId
                        )
                    }
                }
            }

            // Catch uncommon status codes
            else -> {
                ResetPasswordPollCompletionApiResult.UnknownError(
                    error = error.orEmpty(),
                    errorDescription = errorDescription.orEmpty(),
                    correlationId = correlationId
                )
            }
        }
    }
}
