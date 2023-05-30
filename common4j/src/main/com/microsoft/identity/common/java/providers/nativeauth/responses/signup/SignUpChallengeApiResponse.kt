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
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.providers.nativeauth.IApiResponse
import com.microsoft.identity.common.java.util.isOOB
import com.microsoft.identity.common.java.util.isPassword
import com.microsoft.identity.common.java.util.isRedirect
import java.net.HttpURLConnection

data class SignUpChallengeApiResponse(
    @Expose override var statusCode: Int,
    @SerializedName("challenge_type") val challengeType: String?,
    @SerializedName("binding_method") val bindingMethod: String?,
    @SerializedName("interval") val interval: Int?,
    @Expose @SerializedName("challenge_target_label") val challengeTargetLabel: String?,
    @Expose @SerializedName("challenge_channel") val challengeChannel: String?,
    @SerializedName("signup_token") val signupToken: String?,
    @Expose @SerializedName("error") val error: String?,
    @Expose @SerializedName("error_codes") val errorCodes: List<Int>?,
    @Expose @SerializedName("error_description") val errorDescription: String?,
    @Expose @SerializedName("details") val details: List<Map<String, String>>?,
    @SerializedName("code_length") val codeLength: Int?
) : IApiResponse(statusCode) {

    private val TAG = SignUpChallengeApiResponse::class.java.simpleName

    fun toResult(): SignUpChallengeApiResult {
        return if (statusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
            SignUpChallengeApiResult.UnknownError(
                error = error.orEmpty(),
                errorDescription = errorDescription.orEmpty(),
                details
            )
        } else {
            if (challengeType.isRedirect()) {
                SignUpChallengeApiResult.Redirect
            } else if (challengeType.isOOB()) {
                if (challengeTargetLabel.isNullOrBlank()) {
                    throw ClientException("$TAG challengeTargetLabel can't be null or empty in oob state")
                }
                if (challengeChannel.isNullOrBlank()) {
                    throw ClientException("$TAG challengeChannel can't be null or empty in oob state")
                }
                if (codeLength == null) {
                    throw ClientException("$TAG codeLength can't be null or empty in oob state")
                }
                if (signupToken.isNullOrBlank()) {
                    throw ClientException("$TAG signupToken can't be null or empty in oob state")
                }
                SignUpChallengeApiResult.OOBRequired(
                    signupToken = signupToken,
                    challengeTargetLabel = challengeTargetLabel,
                    challengeChannel = challengeChannel,
                    codeLength = codeLength
                )
            } else if (challengeType.isPassword()) {
                if (signupToken.isNullOrBlank()) {
                    throw ClientException("$TAG signupToken can't be null or empty in password state")
                }
                SignUpChallengeApiResult.PasswordRequired(signupToken = signupToken)
            } else {
                SignUpChallengeApiResult.UnknownError(
                    error = error.orEmpty(),
                    errorDescription = errorDescription.orEmpty(),
                    details
                )
            }
        }
    }
}
