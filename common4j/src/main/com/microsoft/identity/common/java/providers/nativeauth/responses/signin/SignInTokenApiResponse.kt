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
package com.microsoft.identity.common.java.providers.nativeauth.responses.signin

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.providers.nativeauth.IApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.interactors.InnerError
import com.microsoft.identity.common.java.util.isInvalidAuthenticationType
import com.microsoft.identity.common.java.util.isInvalidCredentials
import com.microsoft.identity.common.java.util.isInvalidGrant
import com.microsoft.identity.common.java.util.isInvalidRequest
import com.microsoft.identity.common.java.util.isMFARequired
import com.microsoft.identity.common.java.util.isOtpCodeIncorrect
import com.microsoft.identity.common.java.util.isUserNotFound

/**
 * Represents the raw response from the /token endpoint.
 * Can be converted to SignInTokenApiResult using the provided toResult() method.
 * Note: mainly used for representing error cases from the /token endpoint. Successful responses are otherwise mapped to MicrosoftStsTokenResponse instead.
 * @see com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse
 */
data class SignInTokenApiResponse(
    @Expose override var statusCode: Int,
    @Expose @SerializedName("token_type") val tokenType: String?,
    @Expose @SerializedName("scope") val scope: String?,
    @Expose @SerializedName("expires_in") val expiresIn: Long?,
    @Expose @SerializedName("ext_expires_in") val extExpiresIn: Long?,
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("refresh_token") val refreshToken: String?,
    @SerializedName("id_token") val idToken: String?,
    @Expose @SerializedName("error") val error: String?,
    @Expose @SerializedName("error_description") val errorDescription: String?,
    @Expose @SerializedName("error_uri") val errorUri: String?,
    @Expose @SerializedName("details") val details: List<Map<String, String>>?,
    @Expose @SerializedName("error_codes") val errorCodes: List<Int>?,
    @Expose @SerializedName("inner_errors") val innerErrors: List<InnerError>?,
    @Expose @SerializedName("credential_token") val credentialToken: String?,
    @Expose @SerializedName("client_info") val clientInfo: String?,
): IApiResponse(statusCode) {

    companion object {
        private val TAG = SignInTokenApiResponse::class.java.simpleName
    }

    /**
     * Maps potential errors returned from the server response.
     * @see com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInTokenApiResult
     */
    fun toErrorResult(): SignInTokenApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.toResult")

        if (error.isInvalidRequest()) {
            return when {
                errorCodes.isNullOrEmpty() -> {
                    SignInTokenApiResult.UnknownError(
                        error = error.orEmpty(),
                        errorDescription = errorDescription.orEmpty(),
                        details = details,
                        errorCodes = errorCodes.orEmpty()
                    )
                }
                errorCodes[0].isOtpCodeIncorrect() -> {
                    SignInTokenApiResult.CodeIncorrect(
                        error = error.orEmpty(),
                        errorDescription = errorDescription.orEmpty(),
                        errorCodes = errorCodes
                    )
                }
                else -> {
                    SignInTokenApiResult.UnknownError(
                        error = error.orEmpty(),
                        errorDescription = errorDescription.orEmpty(),
                        details = details,
                        errorCodes = errorCodes
                    )
                }
            }
        }

        return if (error.isInvalidGrant()) {
            return when {
                errorCodes.isNullOrEmpty() -> {
                    SignInTokenApiResult.UnknownError(
                        error = error.orEmpty(),
                        errorDescription = errorDescription.orEmpty(),
                        details = details,
                        errorCodes = errorCodes.orEmpty()
                    )
                }
                errorCodes[0].isUserNotFound() -> {
                    SignInTokenApiResult.UserNotFound(
                        error = error.orEmpty(),
                        errorDescription = errorDescription.orEmpty(),
                        errorCodes = errorCodes
                    )
                }
                errorCodes[0].isInvalidCredentials() -> {
                    SignInTokenApiResult.InvalidCredentials(
                        error = error.orEmpty(),
                        errorDescription = errorDescription.orEmpty(),
                        errorCodes = errorCodes
                    )
                }
                errorCodes[0].isMFARequired() -> {
                    SignInTokenApiResult.MFARequired(
                        error = error.orEmpty(),
                        errorDescription = errorDescription.orEmpty(),
                        errorCodes = errorCodes
                    )
                }
                errorCodes[0].isOtpCodeIncorrect() -> {
                    SignInTokenApiResult.CodeIncorrect(
                        error = error.orEmpty(),
                        errorDescription = errorDescription.orEmpty(),
                        errorCodes = errorCodes
                    )
                }
                errorCodes[0].isInvalidAuthenticationType() -> {
                    SignInTokenApiResult.InvalidAuthenticationType(
                        error = error.orEmpty(),
                        errorDescription = errorDescription.orEmpty(),
                        errorCodes = errorCodes
                    )
                }
                else -> {
                    SignInTokenApiResult.UnknownError(
                        error = error.orEmpty(),
                        errorDescription = errorDescription.orEmpty(),
                        details = details,
                        errorCodes = errorCodes
                    )
                }
            }
        }
        else {
            SignInTokenApiResult.UnknownError(
                error = error.orEmpty(),
                errorDescription = errorDescription.orEmpty(),
                details = details,
                errorCodes = errorCodes.orEmpty()
            )
        }
    }
}
