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
package com.microsoft.identity.common.nativeauth.java.providers.responses.resetpassword

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.nativeauth.java.providers.IApiResponse
import com.microsoft.identity.common.nativeauth.java.providers.interactors.InnerError
import com.microsoft.identity.common.nativeauth.java.util.isExpiredToken
import com.microsoft.identity.common.nativeauth.java.util.isExplicitUserNotFound
import com.microsoft.identity.common.nativeauth.java.util.isPasswordBanned
import com.microsoft.identity.common.nativeauth.java.util.isPasswordRecentlyUsed
import com.microsoft.identity.common.nativeauth.java.util.isPasswordTooLong
import com.microsoft.identity.common.nativeauth.java.util.isPasswordTooShort
import com.microsoft.identity.common.nativeauth.java.util.isPasswordTooWeak
import com.microsoft.identity.common.nativeauth.java.util.isPollInProgress
import com.microsoft.identity.common.nativeauth.java.util.isPollSucceeded
import java.net.HttpURLConnection

/**
 * Represents the raw response from the Reset Password /poll_completion endpoint.
 * Can be converted to ResetPasswordChallengeApiResult using the provided toResult() method.
 */
class ResetPasswordPollCompletionApiResponse(
    @Expose override var statusCode: Int,
    @Expose @SerializedName("status") val status: String?,
    @SerializedName("signin_slt") val signinSlt: String?,
    @Expose @SerializedName("error") val error: String?,
    @Expose @SerializedName("error_description") val errorDescription: String?,
    @Expose @SerializedName("error_uri") val errorUri: String?,
    @SerializedName("details") val details: List<Map<String, String>>?,
    @SerializedName("inner_errors") val innerErrors: List<InnerError>?
): IApiResponse(statusCode) {

    companion object {
        private val TAG = ResetPasswordPollCompletionApiResponse::class.java.simpleName
    }

    /**
     * Maps potential errors returned from the server response, and provide different states based on the response.
     * @see com.microsoft.identity.common.nativeauth.java.providers.responses.resetpassword.ResetPasswordPollCompletionApiResult
     */
    fun toResult(): ResetPasswordPollCompletionApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.toResult")

        return when (statusCode) {

            // Handle 400 errors
            HttpURLConnection.HTTP_BAD_REQUEST -> {
                return when {
                    error.isPasswordBanned() || error.isPasswordTooShort() || error.isPasswordTooLong() || error.isPasswordRecentlyUsed() ||
                        error.isPasswordTooWeak() -> {
                        ResetPasswordPollCompletionApiResult.PasswordInvalid(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty()
                        )
                    }
                    error.isExpiredToken() -> {
                        ResetPasswordPollCompletionApiResult.ExpiredToken(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty()
                        )
                    }
                    error.isExplicitUserNotFound() -> {
                        ResetPasswordPollCompletionApiResult.UserNotFound(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty()
                        )
                    }
                    else -> {
                        ResetPasswordPollCompletionApiResult.UnknownError(
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
                    status.isPollInProgress() -> {
                        ResetPasswordPollCompletionApiResult.InProgress
                    }
                    status.isPollSucceeded() -> {
                        ResetPasswordPollCompletionApiResult.PollingSucceeded
                    }
                    else -> {
                        ResetPasswordPollCompletionApiResult.PollingFailed(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                        )
                    }
                }
            }

            // Catch uncommon status codes
            else -> {
                ResetPasswordPollCompletionApiResult.UnknownError(
                    error = error.orEmpty(),
                    errorDescription = errorDescription.orEmpty(),
                    details = details
                )
            }
        }
    }
}
