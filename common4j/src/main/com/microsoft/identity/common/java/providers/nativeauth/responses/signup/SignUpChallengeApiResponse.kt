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
package com.microsoft.identity.common.java.providers.nativeauth.responses.signup

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.providers.nativeauth.IApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.ApiErrorResult
import com.microsoft.identity.common.java.util.isExpiredToken
import com.microsoft.identity.common.java.util.isOOB
import com.microsoft.identity.common.java.util.isPassword
import com.microsoft.identity.common.java.util.isRedirect
import com.microsoft.identity.common.java.util.isUnsupportedChallengeType
import java.net.HttpURLConnection

/**
 * Represents the raw response from the Sign Up /challenge endpoint.
 * Can be converted to SignUpChallengeApiResult using the provided toResult() method.
 */
data class SignUpChallengeApiResponse(
    @Expose override var statusCode: Int,
    @SerializedName("challenge_type") val challengeType: String?,
    @Expose @SerializedName("binding_method") val bindingMethod: String?,
    @Expose @SerializedName("interval") val interval: Int?,
    @SerializedName("challenge_target_label") val challengeTargetLabel: String?,
    @Expose @SerializedName("challenge_channel") val challengeChannel: String?,
    @SerializedName("signup_token") val signupToken: String?,
    @Expose @SerializedName("error") val error: String?,
    @Expose @SerializedName("error_description") val errorDescription: String?,
    @SerializedName("details") val details: List<Map<String, String>>?,
    @Expose @SerializedName("code_length") val codeLength: Int?
) : IApiResponse(statusCode) {

    companion object {
        private val TAG = SignUpChallengeApiResponse::class.java.simpleName
    }

    /**
     * Maps potential errors returned from the server response, and provide different states based on the response.
     * @see com.microsoft.identity.common.java.providers.nativeauth.responses.signup.SignUpChallengeApiResult
     */
    fun toResult(): SignUpChallengeApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.toResult")

        return when (statusCode) {

            // Handle 400 errors
            HttpURLConnection.HTTP_BAD_REQUEST -> {
                return when {
                    error.isUnsupportedChallengeType() -> {
                        SignUpChallengeApiResult.UnsupportedChallengeType(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            details = details,
                        )
                    }
                    error.isExpiredToken() -> {
                        SignUpChallengeApiResult.ExpiredToken(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            details = details,
                        )
                    }
                    else -> {
                        SignUpChallengeApiResult.UnknownError(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            details = details,
                        )
                    }
                }
            }

            // Handle challenge types
            HttpURLConnection.HTTP_OK -> {
                return when {
                    challengeType.isRedirect() -> {
                        SignUpChallengeApiResult.Redirect
                    }
                    challengeType.isOOB() -> {
                        return when {
                            challengeTargetLabel.isNullOrBlank() -> {
                                SignUpChallengeApiResult.UnknownError(
                                    error = ApiErrorResult.INVALID_STATE,
                                    errorDescription = "SignUp /challenge did not return a challenge_target_label with oob challenge type",
                                    details = details
                                )
                            }
                            challengeChannel.isNullOrBlank() -> {
                                SignUpChallengeApiResult.UnknownError(
                                    error = ApiErrorResult.INVALID_STATE,
                                    errorDescription = "SignUp /challenge did not return a challenge_channel with oob challenge type",
                                    details = details
                                )
                            }
                            codeLength == null -> {
                                SignUpChallengeApiResult.UnknownError(
                                    error = ApiErrorResult.INVALID_STATE,
                                    errorDescription = "SignUp /challenge did not return a code_length with oob challenge type",
                                    details = details
                                )
                            }
                            else -> {
                                SignUpChallengeApiResult.OOBRequired(
                                    signupToken = signupToken
                                        ?: return SignUpChallengeApiResult.UnknownError(
                                            error = ApiErrorResult.INVALID_STATE,
                                            errorDescription = "SignUp /challenge did not return a flow token with oob challenge type",
                                            details = details
                                        ),
                                    challengeTargetLabel = challengeTargetLabel,
                                    challengeChannel = challengeChannel,
                                    codeLength = codeLength
                                )
                            }
                        }
                    }
                    challengeType.isPassword() -> {
                        SignUpChallengeApiResult.PasswordRequired(
                            signupToken = signupToken
                                ?: return SignUpChallengeApiResult.UnknownError(
                                    error = ApiErrorResult.INVALID_STATE,
                                    errorDescription = "SignUp /challenge did not return a flow token with password challenge type",
                                    details = details
                                )
                        )
                    }
                    else -> {
                        SignUpChallengeApiResult.UnknownError(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            details = details,
                        )
                    }
                }
            }

            // Catch uncommon status codes
            else -> {
                SignUpChallengeApiResult.UnknownError(
                    error = error.orEmpty(),
                    errorDescription = errorDescription.orEmpty(),
                    details = details
                )
            }
        }
    }
}
