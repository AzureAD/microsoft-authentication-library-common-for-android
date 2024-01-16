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
import com.microsoft.identity.common.java.nativeauth.providers.interactors.InnerError
import com.microsoft.identity.common.java.nativeauth.util.isExpiredToken
import com.microsoft.identity.common.java.nativeauth.util.isOOB
import com.microsoft.identity.common.java.nativeauth.util.isRedirect
import com.microsoft.identity.common.java.nativeauth.util.isUnsupportedChallengeType
import java.net.HttpURLConnection

/**
 * Represents the raw response from the Reset Password /challenge endpoint.
 * Can be converted to ResetPasswordChallengeApiResult using the provided toResult() method.
 */
class ResetPasswordChallengeApiResponse(
    @Expose override var statusCode: Int,
    @SerializedName("continuation_token") val continuationToken: String?,
    @Expose @SerializedName("correlation_id") val correlationId: String?,
    @Expose @SerializedName("challenge_type") val challengeType: String?,
    @Expose @SerializedName("binding_method") val bindingMethod: String?,
    @SerializedName("challenge_target_label") val challengeTargetLabel: String?,
    @Expose @SerializedName("challenge_channel") val challengeChannel: String?,
    @Expose @SerializedName("code_length") val codeLength: Int?,
    @Expose @SerializedName("interval") val interval: Int?,
    @SerializedName("error") val error: String?,
    @SerializedName("error_description") val errorDescription: String?,
    @SerializedName("error_uri") val errorUri: String?,
): IApiResponse(statusCode) {

    companion object {
        private val TAG = ResetPasswordChallengeApiResponse::class.java.simpleName
    }

    /**
     * Maps potential errors returned from the server response, and provide different states based on the response.
     * @see com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordChallengeApiResult
     */
    fun toResult(): ResetPasswordChallengeApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.toResult")

        return when (statusCode) {

            // Handle 400 errors
            HttpURLConnection.HTTP_BAD_REQUEST -> {
                return when {
                    error.isExpiredToken() -> {
                        ResetPasswordChallengeApiResult.ExpiredToken(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            correlationId = correlationId
                        )
                    }
                    error.isUnsupportedChallengeType() -> {
                        ResetPasswordChallengeApiResult.UnsupportedChallengeType(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            correlationId = correlationId
                        )
                    }
                    else -> {
                        ResetPasswordChallengeApiResult.UnknownError(
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
                    challengeType.isRedirect() -> {
                        ResetPasswordChallengeApiResult.Redirect(
                            correlationId = correlationId
                        )
                    }
                    challengeType.isOOB() -> {
                        return when {
                            challengeTargetLabel.isNullOrBlank() -> {
                                ResetPasswordChallengeApiResult.UnknownError(
                                    error = "invalid_state",
                                    errorDescription = "ResetPassword /challenge did not return a challenge_target_label with oob challenge type",
                                    correlationId = correlationId
                                )
                            }
                            challengeChannel.isNullOrBlank() -> {
                                ResetPasswordChallengeApiResult.UnknownError(
                                    error = "invalid_state",
                                    errorDescription = "ResetPassword /challenge did not return a challenge_channel with oob challenge type",
                                    correlationId = correlationId
                                )
                            }
                            codeLength == null -> {
                                ResetPasswordChallengeApiResult.UnknownError(
                                    error = "invalid_state",
                                    errorDescription = "ResetPassword /challenge did not return a code_length with oob challenge type",
                                    correlationId = correlationId
                                )
                            }
                            else -> {
                                ResetPasswordChallengeApiResult.CodeRequired(
                                    continuationToken = continuationToken
                                        ?: return ResetPasswordChallengeApiResult.UnknownError(
                                            error = "invalid_state",
                                            errorDescription = "ResetPassword /challenge successful, but did not return a continuation token",
                                            correlationId = correlationId
                                        ),
                                    challengeTargetLabel = challengeTargetLabel,
                                    codeLength = codeLength,
                                    challengeChannel = challengeChannel,
                                    correlationId = correlationId
                                )
                            }
                        }
                    }
                    else -> {
                        ResetPasswordChallengeApiResult.UnknownError(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            correlationId = correlationId
                        )
                    }
                }
            }

            // Catch uncommon status codes
            else -> {
                ResetPasswordChallengeApiResult.UnknownError(
                    error = error.orEmpty(),
                    errorDescription = errorDescription.orEmpty(),
                    correlationId = correlationId
                )
            }
        }
    }
}
