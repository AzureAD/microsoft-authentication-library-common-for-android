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
package com.microsoft.identity.common.java.nativeauth.providers.requests.v2

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.nativeauth.providers.requests.NativeAuthRequest
import com.microsoft.identity.common.java.util.ArgUtils
import java.net.URL

/**
 * Represents a request to the Native Auth V2 `/oauth2/v2.0/authorize-challenge` endpoint used to
 * start a flow, and provides a create() function to instantiate the request using the provided
 * parameters. The body is form-encoded, matching the V1 OAuth-style endpoints. No `scope`
 * parameter is sent here; requested scopes are retained in continuation state only for the later
 * authorization-code token exchange.
 */
data class AuthorizeChallengeStartRequest private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override val parameters: NativeAuthAuthorizeChallengeStartRequestParameters
) : NativeAuthRequest() {

    companion object {
        /**
         * Returns a request object using the provided parameters.
         * The request URL and headers passed will be set directly.
         *
         * Parameters that are null or empty will throw a ClientException.
         * @see com.microsoft.identity.common.java.exception.ClientException
         */
        fun create(
            clientId: String,
            challengeType: String,
            requestUrl: String,
            headers: Map<String, String?>
        ): AuthorizeChallengeStartRequest {
            ArgUtils.validateNonNullArg(clientId, "clientId")
            ArgUtils.validateNonNullArg(challengeType, "challengeType")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")

            return AuthorizeChallengeStartRequest(
                requestUrl = URL(requestUrl),
                headers = headers,
                parameters = NativeAuthAuthorizeChallengeStartRequestParameters(
                    clientId = clientId,
                    challengeType = challengeType
                )
            )
        }
    }

    override fun toUnsanitizedString(): String = "AuthorizeChallengeStartRequest(requestUrl=$requestUrl, headers=$headers, parameters=$parameters)"

    override fun toString(): String = "AuthorizeChallengeStartRequest()"

    /**
     * NativeAuthAuthorizeChallengeStartRequestParameters represents the request parameters sent as
     * part of the `/oauth2/v2.0/authorize-challenge` API call that starts a Native Auth V2 flow.
     * The wire body intentionally includes only `client_id` and `challenge_type`, not `scope`.
     */
    data class NativeAuthAuthorizeChallengeStartRequestParameters(
        @SerializedName("client_id") override val clientId: String,
        @SerializedName("challenge_type") val challengeType: String
    ) : NativeAuthRequestParameters() {
        override fun toUnsanitizedString(): String = "NativeAuthAuthorizeChallengeStartRequestParameters(clientId=$clientId, challengeType=$challengeType)"

        override fun toString(): String = toUnsanitizedString()
    }
}
