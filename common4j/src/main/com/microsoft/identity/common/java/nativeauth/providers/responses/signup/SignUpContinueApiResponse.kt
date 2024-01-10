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
import com.microsoft.identity.common.java.nativeauth.util.isInvalidOOBValue
import com.microsoft.identity.common.java.nativeauth.util.isInvalidRequest
import com.microsoft.identity.common.java.nativeauth.util.isOtpCodeIncorrect
import com.microsoft.identity.common.java.nativeauth.util.isPasswordBanned
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
data class SignUpContinueApiResponse(
    @Expose override var statusCode: Int,
    @SerializedName("signin_slt") val signInSLT: String?,
    @SerializedName("signup_token") val signupToken: String?,
    @Expose @SerializedName("unverified_attributes") val unverifiedAttributes: List<Map<String, String>>?,
    @SerializedName("invalid_attributes") val invalidAttributes: List<Map<String, String>>?,
    @Expose @SerializedName("required_attributes") val requiredAttributes: List<UserAttributeApiResult>?,
    @SerializedName("expires_in") val expiresIn: Int?,
    @SerializedName("error") val error: String?,
    @SerializedName("error_codes") val errorCodes: List<Int>?,
    @SerializedName("error_description") val errorDescription: String?,
    @SerializedName("details") val details: List<Map<String, String>>?
) : IApiResponse(statusCode) {

    companion object {
        private val TAG = SignUpContinueApiResponse::class.java.simpleName
    }

    fun toResult(): SignUpContinueApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.toResult")

        return when (statusCode) {

            // Handle 400 errors
            HttpURLConnection.HTTP_BAD_REQUEST -> {
                return when {
                    error.isPasswordTooWeak() || error.isPasswordTooLong() || error.isPasswordTooShort() || error.isPasswordBanned() ||
                            error.isPasswordRecentlyUsed() -> {
                        SignUpContinueApiResult.InvalidPassword(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty()
                        )
                    }
                    error.isUserAlreadyExists() -> {
                        SignUpContinueApiResult.UsernameAlreadyExists(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty()
                        )
                    }
                    error.isAttributeValidationFailed() -> {
                        SignUpContinueApiResult.InvalidAttributes(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            invalidAttributes = invalidAttributes?.toAttributeList()
                                ?: return SignUpContinueApiResult.UnknownError(
                                    error = ApiErrorResult.INVALID_STATE,
                                    errorDescription = "SignUp /continue did not return a invalid_attributes with validation_failed error",
                                    details = details
                                )
                        )
                    }
                    error.isExpiredToken() -> {
                        SignUpContinueApiResult.ExpiredToken(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty()
                        )
                    }
                    (error.isInvalidRequest() and errorCodes?.get(0).isOtpCodeIncorrect()) ->{ //Invalid otp code was sent
                        SignUpContinueApiResult.InvalidOOBValue(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty()
                        )
                    }
                    error.isAttributesRequired() -> {
                        SignUpContinueApiResult.AttributesRequired(
                            signupToken = signupToken
                                ?: return SignUpContinueApiResult.UnknownError(
                                    error = ApiErrorResult.INVALID_STATE,
                                    errorDescription = "SignUp /continue did not return a flow token with attributes_required error",
                                    details = details
                                ),
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            requiredAttributes = requiredAttributes
                                ?: return SignUpContinueApiResult.UnknownError(
                                    error = ApiErrorResult.INVALID_STATE,
                                    errorDescription = "SignUp /continue did not return required_attributes with attributes_required error",
                                    details = details
                                )
                        )
                    }
                    error.isCredentialRequired() -> {
                        SignUpContinueApiResult.CredentialRequired(
                            signupToken = signupToken
                                ?: return SignUpContinueApiResult.UnknownError(
                                    error = ApiErrorResult.INVALID_STATE,
                                    errorDescription = "SignUp /continue did not return a flow token with credential_required",
                                    details = details
                                ),
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty()
                        )
                    }
                    error.isVerificationRequired() -> {
                        SignUpContinueApiResult.UnknownError(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            details = details
                        )
                    }
                    else -> {
                        SignUpContinueApiResult.UnknownError(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            details = details
                        )
                    }
                }
            }

            // Handle success
            HttpURLConnection.HTTP_OK -> {
                SignUpContinueApiResult.Success(
                    signInSLT = signInSLT,
                    expiresIn = expiresIn
                )
            }

            // Catch uncommon status codes
            else -> {
                SignUpContinueApiResult.UnknownError(
                    error = error.orEmpty(),
                    errorDescription = errorDescription.orEmpty(),
                    details = details
                )
            }
        }
    }
}
