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
package com.microsoft.identity.common.java.nativeauth.providers.requests.resetpassword

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.nativeauth.providers.requests.NativeAuthRequest
import com.microsoft.identity.common.java.util.ArgUtils
import java.net.URL

/**
 * Represents a request to the Reset Password /challenge endpoint, and provides a create() function to instantiate the request using the provided parameters.
 */
class ResetPasswordChallengeRequest private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override val parameters: NativeAuthRequestParameters
) : NativeAuthRequest() {

    companion object {
        /**
         * Returns a request object using the provided parameters.
         * The request URL and headers passed will be set directly.
         * The clientId, continuation token, and challengeType will be mapped to the NativeAuthResetPasswordRequestBody object.
         *
         * Parameters that are null or empty will throw a ClientException.
         * @see com.microsoft.identity.common.java.exception.ClientException
         */
        fun create(
            clientId: String,
            continuationToken: String,
            challengeType: String?,
            requestUrl: String,
            headers: Map<String, String?>
        ): ResetPasswordChallengeRequest {
            // Check for empty Strings and empty Maps
            ArgUtils.validateNonNullArg(clientId, "clientId")
            ArgUtils.validateNonNullArg(continuationToken, "continuationToken")
            ArgUtils.validateNonNullArg(challengeType, "challengeType")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")

            return ResetPasswordChallengeRequest(
                requestUrl = URL(requestUrl),
                headers = headers,
                parameters = NativeAuthResetPasswordChallengeRequestParameters(
                    clientId = clientId,
                    continuationToken = continuationToken,
                    challengeType = challengeType
                )
            )
        }
    }

    override fun toUnsanitizedString(): String = "ResetPasswordChallengeRequest(requestUrl=$requestUrl, headers=$headers, parameters=$parameters)"

    override fun toString(): String = "ResetPasswordChallengeRequest()"

    data class NativeAuthResetPasswordChallengeRequestParameters(
        @SerializedName("client_id") override val clientId: String,
        @SerializedName("continuation_token") val continuationToken: String,
        @SerializedName("challenge_type") val challengeType: String?
    ) : NativeAuthRequestParameters() {
        override fun toUnsanitizedString(): String = "NativeAuthResetPasswordChallengeRequestParameters(clientId=$clientId, challengeType=$challengeType)"

        override fun toString(): String = "NativeAuthResetPasswordChallengeRequestParameters(clientId=$clientId)"
    }
}
