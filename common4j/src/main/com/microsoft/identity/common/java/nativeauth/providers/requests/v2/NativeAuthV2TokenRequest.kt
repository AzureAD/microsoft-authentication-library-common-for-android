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
 * Represents a request to the existing OAuth `/oauth2/v2.0/token` endpoint, exchanging a Native
 * Auth V2 authorization code for tokens.
 */
data class NativeAuthV2TokenRequest private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override val parameters: NativeAuthV2TokenRequestParameters
) : NativeAuthRequest() {

    companion object {
        private const val GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code"

        fun create(
            clientId: String,
            code: String,
            scopes: List<String>,
            claimsRequestJson: String? = null,
            requestUrl: String,
            headers: Map<String, String?>
        ): NativeAuthV2TokenRequest {
            ArgUtils.validateNonNullArg(clientId, "clientId")
            ArgUtils.validateNonNullArg(code, "code")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")
            ArgUtils.validateNonNullArg(scopes, "scopes")
            scopes.forEachIndexed { index, scope ->
                ArgUtils.validateNonNullArg(scope, "scopes[$index]")
            }

            return NativeAuthV2TokenRequest(
                requestUrl = URL(requestUrl),
                headers = headers,
                parameters = NativeAuthV2TokenRequestParameters(
                    clientId = clientId,
                    grantType = GRANT_TYPE_AUTHORIZATION_CODE,
                    code = code,
                    scope = scopes.joinToString(" "),
                    claimsRequestJson = claimsRequestJson
                )
            )
        }
    }

    override fun toUnsanitizedString(): String = "NativeAuthV2TokenRequest(requestUrl=$requestUrl, headers=$headers, parameters=$parameters)"

    override fun toString(): String = "NativeAuthV2TokenRequest()"

    /**
     * NativeAuthV2TokenRequestParameters represents the request parameters sent as part of the
     * `/oauth2/v2.0/token` API call that exchanges a Native Auth V2 authorization code for tokens.
     */
    data class NativeAuthV2TokenRequestParameters(
        @SerializedName("client_id") override val clientId: String,
        @SerializedName("grant_type") val grantType: String,
        @SerializedName("code") val code: String,
        @SerializedName("scope") val scope: String,
        @SerializedName("claims") val claimsRequestJson: String?,
        @SerializedName("client_info") private val clientInfo: Boolean = true
    ) : NativeAuthRequestParameters() {
        override fun toUnsanitizedString(): String = "NativeAuthV2TokenRequestParameters(clientId=$clientId, grantType=$grantType, scope=$scope, clientInfo=$clientInfo)"

        override fun toString(): String = toUnsanitizedString()
    }
}
