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
package com.microsoft.identity.common.java.nativeauth.providers.requests.signin

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthConstants
import com.microsoft.identity.common.java.nativeauth.providers.requests.NativeAuthRequest
import com.microsoft.identity.common.java.util.ArgUtils
import com.microsoft.identity.common.java.util.CharArrayJsonAdapter
import java.net.URL

/**
 * Represents a request to the OAuth /token endpoint, and provides a create() function to instantiate the request using the provided parameters.
 * There are separate create() methods depending on the grant type - OOB, Password, or Continuation Token.
 */
data class SignInTokenRequest private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override val parameters: NativeAuthRequestSignInTokenRequestParameters
) : NativeAuthRequest() {

    /**
     * Returns a request object using the provided parameters.
     * The request URL and headers passed will be set directly.
     * The NativeAuthRequestSignInChallengeParameters object will be populated with the provided username and OOB, and the grant type will be set to "oob".
     *
     * Parameters outside of scopes and challengeType that are null or empty will throw a ClientException.
     * @see com.microsoft.identity.common.java.exception.ClientException
     */
    companion object {
        fun createOOBTokenRequest(
            oob: String,
            continuationToken: String,
            clientId: String,
            scopes: List<String>? = null,
            challengeType: String? = null,
            requestUrl: String,
            headers: Map<String, String?>
        ): SignInTokenRequest {
            // Check for empty Strings and empty Maps
            ArgUtils.validateNonNullArg(oob, "oob")
            ArgUtils.validateNonNullArg(continuationToken, "continuationToken")
            ArgUtils.validateNonNullArg(clientId, "clientId")
            ArgUtils.validateNonNullArg(challengeType, "challengeType")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")


            return SignInTokenRequest(
                parameters = NativeAuthRequestSignInTokenRequestParameters(
                    oob = oob,
                    continuationToken = continuationToken,
                    clientId = clientId,
                    grantType = NativeAuthConstants.GrantType.OOB,
                    challengeType = challengeType,
                    scope = scopes?.joinToString(" ")
                ),
                requestUrl = URL(requestUrl),
                headers = headers,
            )
        }

        /**
         * Returns a request object using the provided parameters.
         * The request URL and headers passed will be set directly.
         * The NativeAuthRequestSignInChallengeParameters object will be populated with the provided username and password, and the grant type will be set to "password".
         *
         * Parameters outside of scopes and challengeType that are null or empty will throw a ClientException.
         * @see com.microsoft.identity.common.java.exception.ClientException
         */
        fun createPasswordTokenRequest(
            password: CharArray,
            continuationToken: String,
            clientId: String,
            scopes: List<String>? = null,
            challengeType: String? = null,
            requestUrl: String,
            headers: Map<String, String?>
        ): SignInTokenRequest {
            // Check for empty Strings and empty Maps
            ArgUtils.validateNonNullArg(password, "password")
            ArgUtils.validateNonNullArg(continuationToken, "continuationToken")
            ArgUtils.validateNonNullArg(clientId, "clientId")
            ArgUtils.validateNonNullArg(challengeType, "challengeType")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")


            return SignInTokenRequest(
                parameters = NativeAuthRequestSignInTokenRequestParameters(
                    password = password,
                    continuationToken = continuationToken,
                    clientId = clientId,
                    grantType = NativeAuthConstants.GrantType.PASSWORD,
                    challengeType = challengeType,
                    scope = scopes?.joinToString(" ")
                ),
                requestUrl = URL(requestUrl),
                headers = headers,
            )
        }

        /**
         * Returns a request object using the provided parameters.
         * The request URL and headers passed will be set directly.
         * The NativeAuthRequestSignInChallengeParameters object will be populated with the provided username and continuation token, and the grant type will be set to "continuation_token".
         *
         * Parameters outside of scopes and challengeType that are null or empty will throw a ClientException.
         * @see com.microsoft.identity.common.java.exception.ClientException
         */
        fun createContinuationTokenRequest(
            continuationToken: String,
            clientId: String,
            username: String,
            scopes: List<String>? = null,
            challengeType: String? = null,
            requestUrl: String,
            headers: Map<String, String?>
        ): SignInTokenRequest {
            // Check for empty Strings and empty Maps
            ArgUtils.validateNonNullArg(continuationToken, "continuationToken")
            ArgUtils.validateNonNullArg(clientId, "clientId")
            ArgUtils.validateNonNullArg(username, "username")
            ArgUtils.validateNonNullArg(challengeType, "challengeType")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")

            return SignInTokenRequest(
                parameters = NativeAuthRequestSignInTokenRequestParameters(
                    continuationToken = continuationToken,
                    clientId = clientId,
                    username = username,
                    grantType = NativeAuthConstants.GrantType.CONTINUATION_TOKEN,
                    challengeType = challengeType,
                    scope = scopes?.joinToString(" ")
                ),
                requestUrl = URL(requestUrl),
                headers = headers
            )
        }
    }

    override fun toUnsanitizedString(): String = "SignInTokenRequest(requestUrl=$requestUrl, headers=$headers, parameters=$parameters)"

    override fun toString(): String = "SignInTokenRequest()"

    data class NativeAuthRequestSignInTokenRequestParameters(
        val username: String? = null,
        @JsonAdapter(CharArrayJsonAdapter::class) val password: CharArray? = null,
        val oob: String? = null,
        @SerializedName("nca") val nca: Int? = null,  //nca=1 forces Azure AD to lookup the account via the signInNames attribute, rather than the userPrincipalName (default identifier for AAD).
        @SerializedName("client_info") private val clientInfo: Boolean = true,
        @SerializedName("client_id") override val clientId: String,
        @SerializedName("grant_type") val grantType: String,
        @SerializedName("continuation_token") val continuationToken: String? = null,
        @SerializedName("scope") val scope: String?,
        @SerializedName("challenge_type") val challengeType: String?
    ) : NativeAuthRequestParameters() {
        override fun toUnsanitizedString(): String = "NativeAuthRequestSignInTokenRequestParameters(nca=$nca, clientInfo=$clientInfo, clientId=$clientId, grantType=$grantType, scope=$scope, challengeType=$challengeType)"

        override fun toString(): String = "NativeAuthRequestSignInTokenRequestParameters(nca=$nca, clientInfo=$clientInfo, clientId=$clientId)"
    }
}
