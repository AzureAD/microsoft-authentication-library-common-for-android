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
package com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.providers.nativeauth.IApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.interactors.InnerError
import com.microsoft.identity.common.java.util.isExpiredToken
import com.microsoft.identity.common.java.util.isOOB
import com.microsoft.identity.common.java.util.isRedirect
import com.microsoft.identity.common.java.util.isUnsupportedChallengeType
import java.net.HttpURLConnection

/**
 * Represents the raw response from the Reset Password /challenge endpoint.
 * Can be converted to ResetPasswordChallengeApiResult using the provided toResult() method.
 */
class ResetPasswordChallengeApiResponse(
    @Expose override var statusCode: Int,
    @Expose @SerializedName("password_reset_token") val passwordResetToken: String?,
    @Expose @SerializedName("challenge_type") val challengeType: String?,
    @Expose @SerializedName("binding_method") val bindingMethod: String?,
    @Expose @SerializedName("challenge_target_label") val challengeTargetLabel: String?,
    @Expose @SerializedName("challenge_channel") val challengeChannel: String?,
    @Expose @SerializedName("code_length") val codeLength: Int?,
    @Expose @SerializedName("interval") val interval: Int?,
    @Expose @SerializedName("error") val error: String?,
    @Expose @SerializedName("details") val details: List<Map<String, String>>?,
    @Expose @SerializedName("error_description") val errorDescription: String?,
    @Expose @SerializedName("error_uri") val errorUri: String?,
    @Expose @SerializedName("inner_errors") val innerErrors: List<InnerError>?
): IApiResponse(statusCode) {

    companion object {
        private val TAG = ResetPasswordChallengeApiResponse::class.java.simpleName
    }

    /**
     * Maps potential errors returned from the server response, and provide different states based on the response.
     * @see com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordChallengeApiResult
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
                            errorDescription = errorDescription.orEmpty()
                        )
                    }
                    error.isUnsupportedChallengeType() -> {
                        ResetPasswordChallengeApiResult.UnsupportedChallengeType(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty()
                        )
                    }
                    else -> {
                        ResetPasswordChallengeApiResult.UnknownError(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            details = details
                        )
                    }
                }
            }

            // Handle success and redirect
            HttpURLConnection.HTTP_OK -> {
                return when {
                    challengeType.isRedirect() -> {
                        ResetPasswordChallengeApiResult.Redirect
                    }
                    challengeType.isOOB() -> {
                        return when {
                            challengeTargetLabel.isNullOrBlank() -> {
                                ResetPasswordChallengeApiResult.UnknownError(
                                    error = "invalid_state",
                                    errorDescription = "ResetPassword /challenge did not return a challenge_target_label with oob challenge type",
                                    details = details
                                )
                            }
                            challengeChannel.isNullOrBlank() -> {
                                ResetPasswordChallengeApiResult.UnknownError(
                                    error = "invalid_state",
                                    errorDescription = "ResetPassword /challenge did not return a challenge_channel with oob challenge type",
                                    details = details
                                )
                            }
                            codeLength == null -> {
                                ResetPasswordChallengeApiResult.UnknownError(
                                    error = "invalid_state",
                                    errorDescription = "ResetPassword /challenge did not return a code_length with oob challenge type",
                                    details = details
                                )
                            }
                            else -> {
                                ResetPasswordChallengeApiResult.CodeRequired(
                                    passwordResetToken = passwordResetToken
                                        ?: return ResetPasswordChallengeApiResult.UnknownError(
                                            error = "invalid_state",
                                            errorDescription = "ResetPassword /challenge successful, but did not return a flow token",
                                            details = details
                                        ),
                                    challengeTargetLabel = challengeTargetLabel,
                                    codeLength = codeLength,
                                    challengeChannel = challengeChannel
                                )
                            }
                        }
                    }
                    else -> {
                        ResetPasswordChallengeApiResult.UnknownError(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            details = details
                        )
                    }
                }
            }

            // Catch uncommon status codes
            else -> {
                ResetPasswordChallengeApiResult.UnknownError(
                    error = error.orEmpty(),
                    errorDescription = errorDescription.orEmpty(),
                    details = details
                )
            }
        }
    }
}
