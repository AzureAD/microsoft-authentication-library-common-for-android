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
import com.microsoft.identity.common.java.providers.nativeauth.responses.ApiErrorResult
import com.microsoft.identity.common.java.util.isInvalidGrant
import com.microsoft.identity.common.java.util.isRedirect
import com.microsoft.identity.common.java.util.isUserNotFound
import java.net.HttpURLConnection

/**
 * Represents the raw response from the /initiate endpoint.
 * Can be converted to SignInInitiateApiResult using the provided toResult() method.
 */
data class SignInInitiateApiResponse(
    @Expose override var statusCode: Int,
    @Expose @SerializedName("credential_token") val credentialToken: String?,
    @Expose @SerializedName("challenge_type") val challengeType: String?,
    @Expose @SerializedName("error") val error: String?,
    @Expose @SerializedName("error_description") val errorDescription: String?,
    @Expose @SerializedName("error_uri") val errorUri: String?,
    @Expose @SerializedName("details") val details: List<Map<String, String>>?,
    @Expose @SerializedName("error_codes") val errorCodes: List<Int>?,
    @Expose @SerializedName("inner_errors") val innerErrors: List<InnerError>?,
): IApiResponse(statusCode) {

    companion object {
        private val TAG = SignInInitiateApiResponse::class.java.simpleName
    }

    /**
     * Maps potential errors returned from the server response, and provide different states based on the response.
     * @see com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInInitiateApiResult
     */
    fun toResult(): SignInInitiateApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.toResult")

        return when (statusCode) {

            // Handle 400 errors
            HttpURLConnection.HTTP_BAD_REQUEST -> {
                if (error.isInvalidGrant()) {
                    return when {
                        errorCodes.isNullOrEmpty() -> {
                            SignInInitiateApiResult.UnknownError(
                                error = error.orEmpty(),
                                errorDescription = errorDescription.orEmpty(),
                                details = details,
                                errorCodes = errorCodes.orEmpty()
                            )
                        }
                        errorCodes[0].isUserNotFound() -> {
                            SignInInitiateApiResult.UserNotFound(
                                error = error.orEmpty(),
                                errorDescription = errorDescription.orEmpty(),
                                errorCodes = errorCodes
                            )
                        }
                        else -> {
                            SignInInitiateApiResult.UnknownError(
                                error = error.orEmpty(),
                                errorDescription = errorDescription.orEmpty(),
                                details = details,
                                errorCodes = errorCodes
                            )
                        }
                    }
                }
                else {
                    SignInInitiateApiResult.UnknownError(
                        error = error.orEmpty(),
                        errorDescription = errorDescription.orEmpty(),
                        details = details,
                        errorCodes = errorCodes.orEmpty()
                    )
                }
            }

            // Handle success and redirect
            HttpURLConnection.HTTP_OK -> {
                if (challengeType.isRedirect()) {
                    SignInInitiateApiResult.Redirect
                }
                else {
                    SignInInitiateApiResult.Success(
                        credentialToken = credentialToken
                            ?: return SignInInitiateApiResult.UnknownError(
                                error = ApiErrorResult.INVALID_STATE,
                                errorDescription = "SignIn /initiate did not return a flow token",
                                details = details,
                                errorCodes = errorCodes.orEmpty()
                            )
                    )
                }
            }

            // Catch uncommon status codes
            else -> {
                SignInInitiateApiResult.UnknownError(
                    error = error.orEmpty(),
                    errorDescription = errorDescription.orEmpty(),
                    details = details,
                    errorCodes = errorCodes.orEmpty()
                )
            }
        }
    }
}
