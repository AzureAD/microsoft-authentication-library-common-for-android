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
package com.microsoft.identity.common.java.providers.nativeauth.responses.sspr

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.providers.nativeauth.IApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.interactors.InnerError
import com.microsoft.identity.common.java.util.isInvalidGrant
import com.microsoft.identity.common.java.util.isOtpCodeIncorrect
import com.microsoft.identity.common.java.util.isRedirect
import java.net.HttpURLConnection

class SsprContinueApiResponse(
    @Expose override var statusCode: Int,
    @Expose @SerializedName("password_reset_token") val passwordResetToken: String?,
    @Expose @SerializedName("password_submit_token") val passwordSubmitToken: String?,
    @Expose @SerializedName("challenge_type") val challengeType: String?,
    @Expose @SerializedName("expires_in") val expiresIn: Int?,
    @Expose @SerializedName("error") val error: String?,
    @Expose @SerializedName("error_description") val errorDescription: String?,
    @Expose @SerializedName("error_uri") val errorUri: String?,
    @Expose @SerializedName("error_codes") val errorCodes: List<Int>?,
    @Expose @SerializedName("inner_errors") val innerErrors: List<InnerError>?
): IApiResponse(statusCode) {

    fun toResult(): SsprContinueApiResult {
        if (statusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
            if (error.isInvalidGrant()) {
                if (errorCodes.isNullOrEmpty()) {
                    return SsprContinueApiResult.UnknownError(error.orEmpty(), errorDescription.orEmpty())
                } else if (!passwordResetToken.isNullOrBlank()) {
                    return SsprContinueApiResult.UnknownError(error.orEmpty(), "passwordResetToken received: " + errorDescription.orEmpty())
                } else if (errorCodes[0].isOtpCodeIncorrect()) {
                    // TODO: Should we pass the passwordResetToken here? I see doc mentions "in-case of error", but which error?
                    return SsprContinueApiResult.OOBIncorrect(error.orEmpty(), errorDescription.orEmpty())
                }
            }
            return SsprContinueApiResult.UnknownError(error, errorDescription)
        } else {
            if (challengeType.isRedirect()) {
                return SsprContinueApiResult.Redirect
            } else if (passwordSubmitToken.isNullOrBlank()) {
                throw ClientException("passwordSubmitToken cannot be null or blank in non-redirect success response from /Continue")
            } else {
                return SsprContinueApiResult.PasswordRequired(passwordSubmitToken, expiresIn)
            }
        }
    }
}