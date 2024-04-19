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
package com.microsoft.identity.common.java.nativeauth.providers.responses.signup

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.nativeauth.providers.IApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiErrorResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.UserAttributeApiResult
import com.microsoft.identity.common.java.nativeauth.util.isAttributeValidationFailed
import com.microsoft.identity.common.java.nativeauth.util.isAttributesRequired
import com.microsoft.identity.common.java.nativeauth.util.isCredentialRequired
import com.microsoft.identity.common.java.nativeauth.util.isExpiredToken
import com.microsoft.identity.common.java.nativeauth.util.isInvalidGrant
import com.microsoft.identity.common.java.nativeauth.util.isInvalidOOBValue
import com.microsoft.identity.common.java.nativeauth.util.isPasswordBanned
import com.microsoft.identity.common.java.nativeauth.util.isPasswordInvalid
import com.microsoft.identity.common.java.nativeauth.util.isPasswordRecentlyUsed
import com.microsoft.identity.common.java.nativeauth.util.isPasswordTooLong
import com.microsoft.identity.common.java.nativeauth.util.isPasswordTooShort
import com.microsoft.identity.common.java.nativeauth.util.isPasswordTooWeak
import com.microsoft.identity.common.java.nativeauth.util.isUserAlreadyExists
import com.microsoft.identity.common.java.nativeauth.util.isVerificationRequired
import com.microsoft.identity.common.java.nativeauth.util.toAttributeList
import java.net.HttpURLConnection

/**
 * Represents the raw response from the Sign Up /continue endpoint.
 * Can be converted to SignUpContinueApiResult using the provided toResult() method.
 */
class SignUpContinueApiResponse(
    @Expose override var statusCode: Int,
    correlationId: String,
    @SerializedName("continuation_token") val continuationToken: String?,
    @Expose @SerializedName("expires_in") val expiresIn: Int?,
    @Expose @SerializedName("unverified_attributes") val unverifiedAttributes: List<Map<String, String>>?,
    @Expose @SerializedName("invalid_attributes") val invalidAttributes: List<Map<String, String>>?,
    @Expose @SerializedName("required_attributes") val requiredAttributes: List<UserAttributeApiResult>?,
    @SerializedName("error") val error: String?,
    @SerializedName("error_codes") val errorCodes: List<Int>?,
    @SerializedName("error_description") val errorDescription: String?,
    @SerializedName("suberror") val subError: String?
) : IApiResponse(statusCode, correlationId) {

    override fun toUnsanitizedString(): String {
        return "SignUpContinueApiResponse(statusCode=$statusCode, " +
                "correlationId=$correlationId, expiresIn=$expiresIn, requiredAttributes=$requiredAttributes, " +
                "error=$error, errorCodes=$errorCodes, errorDescription=$errorDescription, subError=$subError)"
    }

    override fun toString(): String = "SignUpContinueApiResponse(statusCode=$statusCode, " +
            "correlationId=$correlationId"

    companion object {
        private val TAG = SignUpContinueApiResponse::class.java.simpleName
    }

    fun toResult(): SignUpContinueApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.toResult"
        )

        return when (statusCode) {

            // Handle 400 errors
            HttpURLConnection.HTTP_BAD_REQUEST -> {
                return when {
                    error.isInvalidGrant() -> {
                        return when {
                            subError.isPasswordTooWeak() || subError.isPasswordTooLong() || subError.isPasswordTooShort() || subError.isPasswordBanned() ||
                                    subError.isPasswordRecentlyUsed() || subError.isPasswordInvalid() -> {
                                SignUpContinueApiResult.InvalidPassword(
                                    error = error.orEmpty(),
                                    errorDescription = errorDescription.orEmpty(),
                                    subError = subError.orEmpty(),
                                    correlationId = correlationId
                                )
                            }
                            subError.isAttributeValidationFailed() -> {
                                SignUpContinueApiResult.InvalidAttributes(
                                    error = error.orEmpty(),
                                    errorDescription = errorDescription.orEmpty(),
                                    invalidAttributes = invalidAttributes?.toAttributeList()
                                        ?: return SignUpContinueApiResult.UnknownError(
                                            error = "invalid_state",
                                            errorDescription = "SignUp /continue did not return a invalid_attributes with validation_failed error",
                                            correlationId = correlationId
                                        ),
                                    subError = subError.orEmpty(),
                                    correlationId = correlationId
                                )
                            }
                            subError.isInvalidOOBValue() ->{
                                SignUpContinueApiResult.InvalidOOBValue(
                                    error = error.orEmpty(),
                                    errorDescription = errorDescription.orEmpty(),
                                    subError = subError.orEmpty(),
                                    correlationId = correlationId
                                )
                            }
                            else -> {
                                SignUpContinueApiResult.UnknownError(
                                    error = error.orEmpty(),
                                    errorDescription = errorDescription.orEmpty(),
                                    correlationId = correlationId
                                )
                            }
                        }
                    }
                    error.isUserAlreadyExists() -> {
                        SignUpContinueApiResult.UsernameAlreadyExists(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            correlationId = correlationId
                        )
                    }
                    error.isExpiredToken() -> {
                        SignUpContinueApiResult.ExpiredToken(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            correlationId = correlationId
                        )
                    }
                    error.isAttributesRequired() -> {
                        SignUpContinueApiResult.AttributesRequired(
                            continuationToken = continuationToken
                                ?: return SignUpContinueApiResult.UnknownError(
                                    error = ApiErrorResult.INVALID_STATE,
                                    errorDescription = "SignUp /continue did not return a continuation token with attributes_required error",
                                    correlationId = correlationId
                                ),
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            requiredAttributes = requiredAttributes
                                ?: return SignUpContinueApiResult.UnknownError(
                                    error = ApiErrorResult.INVALID_STATE,
                                    errorDescription = "SignUp /continue did not return required_attributes with attributes_required error",
                                    correlationId = correlationId
                                ),
                            correlationId = correlationId
                        )
                    }
                    error.isCredentialRequired() -> {
                        SignUpContinueApiResult.CredentialRequired(
                            continuationToken = continuationToken
                                ?: return SignUpContinueApiResult.UnknownError(
                                    error = ApiErrorResult.INVALID_STATE,
                                    errorDescription = "SignUp /continue did not return a continuation token with credential_required",
                                    correlationId = correlationId
                                ),
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            correlationId = correlationId
                        )
                    }
                    error.isVerificationRequired() -> {
                        SignUpContinueApiResult.UnknownError(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            correlationId = correlationId
                        )
                    }
                    else -> {
                        SignUpContinueApiResult.UnknownError(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            correlationId = correlationId
                        )
                    }
                }
            }

            // Handle success
            HttpURLConnection.HTTP_OK -> {
                SignUpContinueApiResult.Success(
                    continuationToken = continuationToken,
                    expiresIn = expiresIn,
                    correlationId = correlationId
                )
            }

            // Catch uncommon status codes
            else -> {
                SignUpContinueApiResult.UnknownError(
                    error = error.orEmpty(),
                    errorDescription = errorDescription.orEmpty(),
                    correlationId = correlationId
                )
            }
        }
    }
}
