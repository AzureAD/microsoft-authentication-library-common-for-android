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
package com.microsoft.identity.common.java.providers.nativeauth.requests.resetpassword

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthConstants
import com.microsoft.identity.common.java.providers.nativeauth.requests.NativeAuthRequest
import com.microsoft.identity.common.java.util.ArgUtils
import java.net.URL

/**
 * Represents a request to the Reset Password /continue endpoint, and provides a create() function to instantiate the request using the provided parameters.
 */
class ResetPasswordContinueRequest private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override var parameters: NativeAuthRequestParameters
) : NativeAuthRequest() {

    companion object {
        /**
         * Returns a request object using the provided parameters.
         * The request URL and headers passed will be set directly.
         * The clientId, password reset token, oob, and grantType will be mapped to the NativeAuthResetPasswordContinueRequestBody object.
         *
         * Parameters that are null or empty will throw a ClientException.
         * @see com.microsoft.identity.common.java.exception.ClientException
         */
        fun create(
            clientId: String,
            passwordResetToken: String,
            oob: String,
            requestUrl: String,
            headers: Map<String, String?>
        ): ResetPasswordContinueRequest {
            // Check for empty Strings and empty Maps
            ArgUtils.validateNonNullArg(clientId, "clientId")
            ArgUtils.validateNonNullArg(passwordResetToken, "passwordResetToken")
            ArgUtils.validateNonNullArg(oob, "oob")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")

            return ResetPasswordContinueRequest(
                requestUrl = URL(requestUrl),
                headers = headers,
                parameters = NativeAuthResetPasswordContinueRequestBody(
                    clientId = clientId,
                    grantType = NativeAuthConstants.GrantType.OOB,
                    passwordResetToken = passwordResetToken,
                    oob = oob
                )
            )
        }
    }

    /**
     * NativeAuthResetPasswordContinueRequestBody represents the request parameters sent as part of
     * /resetpassword/continue API call
     */
    data class NativeAuthResetPasswordContinueRequestBody(
        @SerializedName("client_id") override val clientId: String,
        @SerializedName("grant_type") val grantType: String,
        @SerializedName("password_reset_token") val passwordResetToken: String,
        @SerializedName("oob") val oob: String
    ) : NativeAuthRequestParameters()
}
