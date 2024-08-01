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
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.nativeauth.providers.IApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiErrorResult
import com.microsoft.identity.common.java.nativeauth.util.isIntrospectRequired
import com.microsoft.identity.common.java.nativeauth.util.isInvalidGrant
import com.microsoft.identity.common.java.nativeauth.util.isInvalidRequest
import com.microsoft.identity.common.java.nativeauth.util.isOOB
import com.microsoft.identity.common.java.nativeauth.util.isPassword
import com.microsoft.identity.common.java.nativeauth.util.isRedirect
import java.net.HttpURLConnection

/**
 * Represents the raw response from the /challenge endpoint.
 * Can be converted to SignInChallengeApiResult using the provided toResult() method.
 */
class SignInChallengeApiResponse(
    @Expose override var statusCode: Int,
    correlationId: String,
    @SerializedName("continuation_token") val continuationToken: String?,
    @Expose @SerializedName("challenge_type") val challengeType: String?,
    @Expose @SerializedName("binding_method") val bindingMethod: String?,
    @SerializedName("challenge_target_label") val challengeTargetLabel: String?,
    @Expose @SerializedName("challenge_channel") val challengeChannel: String?,
    @Expose @SerializedName("code_length") val codeLength: Int?,
    @Expose @SerializedName("interval") val interval: Int?,
    @SerializedName("error") val error: String?,
    @SerializedName("sub_error") val subError: String?,
    @SerializedName("error_codes") val errorCodes: List<Int>?,
    @SerializedName("error_description") val errorDescription: String?,
    @SerializedName("error_uri") val errorUri: String?,
): IApiResponse(statusCode, correlationId) {

    override fun toUnsanitizedString(): String {
        return "SignInChallengeApiResponse(statusCode=$statusCode, " +
                "correlationId=$correlationId, challengeType=$challengeType, " +
                "bindingMethod=$bindingMethod, challengeTargetLabel=$challengeTargetLabel, " +
                "challengeChannel=$challengeChannel, codeLength=$codeLength, interval=$interval, " +
                "error=$error, subError=$subError, errorDescription=$errorDescription, errorCodes=$errorCodes, " +
                "errorUri=$errorUri)"
    }

    override fun toString(): String = "SignInChallengeApiResponse(statusCode=$statusCode, " +
            "correlationId=$correlationId"

    /**
    * Maps potential errors returned from the server response, and provide different states based on the response.
    * @see com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInChallengeApiResult
    */
    fun toResult(): SignInChallengeApiResult {
        return when (statusCode) {

            // Handle 400 errors
            HttpURLConnection.HTTP_BAD_REQUEST -> {
                when {
                    error.isInvalidGrant() -> {
                        SignInChallengeApiResult.UnknownError(
                            error = error.orEmpty(),
                            subError = subError.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            errorCodes = errorCodes.orEmpty(),
                            correlationId = correlationId
                        )
                    }
                    error.isInvalidRequest() && subError.isIntrospectRequired() -> {
                        SignInChallengeApiResult.IntrospectRequired(
                            correlationId = correlationId,
                            continuationToken = continuationToken!! // TODO fix this; is continuation_token returned?
                        )
                    }
                    else -> {
                        SignInChallengeApiResult.UnknownError(
                            error = error.orEmpty(),
                            subError = subError.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            errorCodes = errorCodes.orEmpty(),
                            correlationId = correlationId
                        )
                    }
                }
            }

            // Handle success and redirect
            HttpURLConnection.HTTP_OK -> {
                return when {
                    challengeType.isRedirect() -> {
                        SignInChallengeApiResult.Redirect(
                            correlationId = correlationId
                        )
                    }
                    challengeType.isOOB() -> {
                        return when {
                            challengeTargetLabel.isNullOrBlank() -> {
                                SignInChallengeApiResult.UnknownError(
                                    error = ApiErrorResult.INVALID_STATE,
                                    subError = subError.orEmpty(),
                                    errorDescription = "oauth/v2.0/challenge did not return a challenge_target_label with oob challenge type",
                                    errorCodes = errorCodes.orEmpty(),
                                    correlationId = correlationId
                                )
                            }
                            challengeChannel.isNullOrBlank() -> {
                                SignInChallengeApiResult.UnknownError(
                                    error = ApiErrorResult.INVALID_STATE,
                                    subError = subError.orEmpty(),
                                    errorDescription = "oauth/v2.0/challenge did not return a challenge_channel with oob challenge type",
                                    errorCodes = errorCodes.orEmpty(),
                                    correlationId = correlationId
                                )
                            }
                            codeLength == null -> {
                                SignInChallengeApiResult.UnknownError(
                                    error = ApiErrorResult.INVALID_STATE,
                                    subError = subError.orEmpty(),
                                    errorDescription = "oauth/v2.0/challenge did not return a code_length with oob challenge type",
                                    errorCodes = errorCodes.orEmpty(),
                                    correlationId = correlationId
                                )
                            }
                            else -> {
                                SignInChallengeApiResult.OOBRequired(
                                    continuationToken = continuationToken
                                        ?: return SignInChallengeApiResult.UnknownError(
                                            error = ApiErrorResult.INVALID_STATE,
                                            subError = subError.orEmpty(),
                                            errorDescription = "oauth/v2.0/challenge did not return a continuation token with oob challenge type",
                                            errorCodes = errorCodes.orEmpty(),
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
                    challengeType.isPassword() -> {
                        SignInChallengeApiResult.PasswordRequired(
                            continuationToken = continuationToken
                                ?: return SignInChallengeApiResult.UnknownError(
                                    error = ApiErrorResult.INVALID_STATE,
                                    errorDescription = "oauth/v2.0/challenge did not return a continuation token with password challenge type",
                                    errorCodes = errorCodes.orEmpty(),
                                    subError = subError.orEmpty(),
                                    correlationId = correlationId
                                ),
                            correlationId = correlationId
                        )
                    }
                    else -> {
                        SignInChallengeApiResult.UnknownError(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            errorCodes = errorCodes.orEmpty(),
                            subError = subError.orEmpty(),
                            correlationId = correlationId
                        )
                    }
                }
            }

            // Catch uncommon status codes
            else -> {
                SignInChallengeApiResult.UnknownError(
                    error = error.orEmpty(),
                    errorDescription = errorDescription.orEmpty(),
                    errorCodes = errorCodes.orEmpty(),
                    subError = subError.orEmpty(),
                    correlationId = correlationId
                )
            }
        }
    }
}
