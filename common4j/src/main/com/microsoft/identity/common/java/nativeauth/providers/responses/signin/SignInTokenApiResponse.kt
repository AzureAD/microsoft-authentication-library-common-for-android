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
import com.microsoft.identity.common.java.nativeauth.util.isInvalidAuthenticationType
import com.microsoft.identity.common.java.nativeauth.util.isInvalidCredentials
import com.microsoft.identity.common.java.nativeauth.util.isInvalidGrant
import com.microsoft.identity.common.java.nativeauth.util.isInvalidOOBValue
import com.microsoft.identity.common.java.nativeauth.util.isInvalidRequest
import com.microsoft.identity.common.java.nativeauth.util.isMFARequired
import com.microsoft.identity.common.java.nativeauth.util.isUserNotFound

/**
 * Represents the raw response from the /token endpoint.
 * Can be converted to SignInTokenApiResult using the provided toResult() method.
 * Note: mainly used for representing error cases from the /token endpoint. Successful responses are otherwise mapped to MicrosoftStsTokenResponse instead.
 * @see com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse
 */
class SignInTokenApiResponse(
    @Expose override var statusCode: Int,
    correlationId: String,
    @SerializedName("error") val error: String?,
    @SerializedName("error_description") val errorDescription: String?,
    @SerializedName("error_uri") val errorUri: String?,
    @SerializedName("error_codes") val errorCodes: List<Int>?,
    @SerializedName("suberror") val subError: String?,
    @SerializedName("continuation_token") val continuationToken: String?,
    ): IApiResponse(statusCode, correlationId) {

    companion object {
        private val TAG = SignInTokenApiResponse::class.java.simpleName
    }

    /**
     * Maps potential errors returned from the server response.
     * @see com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResult
     */
    fun toErrorResult(): SignInTokenApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.toResult"
        )

        if (error.isInvalidRequest()) {
            return when {
                errorCodes.isNullOrEmpty() -> {
                    SignInTokenApiResult.UnknownError(
                        error = error.orEmpty(),
                        errorDescription = errorDescription.orEmpty(),
                        errorCodes = errorCodes.orEmpty(),
                        correlationId = correlationId
                    )
                }
                else -> {
                    SignInTokenApiResult.UnknownError(
                        error = error.orEmpty(),
                        errorDescription = errorDescription.orEmpty(),
                        errorCodes = errorCodes,
                        correlationId = correlationId
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
                        errorCodes = errorCodes.orEmpty(),
                        correlationId = correlationId
                    )
                }
                errorCodes[0].isInvalidCredentials() -> {
                    SignInTokenApiResult.InvalidCredentials(
                        error = error.orEmpty(),
                        errorDescription = errorDescription.orEmpty(),
                        errorCodes = errorCodes,
                        correlationId = correlationId
                    )
                }
                errorCodes[0].isMFARequired() -> {
                    SignInTokenApiResult.MFARequired(
                        error = error.orEmpty(),
                        errorDescription = errorDescription.orEmpty(),
                        errorCodes = errorCodes,
                        correlationId = correlationId
                    )
                }
                subError.isInvalidOOBValue() -> {
                    SignInTokenApiResult.CodeIncorrect(
                        error = error.orEmpty(),
                        errorDescription = errorDescription.orEmpty(),
                        errorCodes = errorCodes,
                        subError = subError.orEmpty(),
                        correlationId = correlationId
                    )
                }
                errorCodes[0].isInvalidAuthenticationType() -> {
                    SignInTokenApiResult.InvalidAuthenticationType(
                        error = error.orEmpty(),
                        errorDescription = errorDescription.orEmpty(),
                        errorCodes = errorCodes,
                        correlationId = correlationId
                    )
                }
                else -> {
                    SignInTokenApiResult.UnknownError(
                        error = error.orEmpty(),
                        errorDescription = errorDescription.orEmpty(),
                        errorCodes = errorCodes,
                        correlationId = correlationId
                    )
                }
            }
        }
        else if (error.isUserNotFound()) {
            SignInTokenApiResult.UserNotFound(
                error = error.orEmpty(),
                errorDescription = errorDescription.orEmpty(),
                errorCodes = errorCodes.orEmpty(),
                correlationId = correlationId
            )
        }
        else {
            SignInTokenApiResult.UnknownError(
                error = error.orEmpty(),
                errorDescription = errorDescription.orEmpty(),
                errorCodes = errorCodes.orEmpty(),
                correlationId = correlationId
            )
        }
    }
}
