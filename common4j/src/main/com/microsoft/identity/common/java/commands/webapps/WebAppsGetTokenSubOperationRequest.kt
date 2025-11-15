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
package com.microsoft.identity.common.java.commands.webapps

import com.google.gson.annotations.SerializedName

/**
 * Request parameters for WebAppsGetTokenSubOperation.
 */
data class WebAppsGetTokenSubOperationRequest(
    // If from MSAL JS, this is required. If from ESTS, this is optional.
    @SerializedName(FIELD_HOME_ACCOUNT_ID)
    val homeAccountId: String? = null,

    // Required.
    @SerializedName(FIELD_CLIENT_ID)
    val clientId: String,

    // Optional. If not passed, broker will use the default common authority.
    @SerializedName(FIELD_AUTHORITY)
    val authority: String? = DEFAULT_AUTHORITY,

    // Required; of type List.
    @SerializedName(FIELD_SCOPES)
    val scopes: String,

    // Required.
    @SerializedName(FIELD_REDIRECT_URI)
    val redirectUri: String,

    // Optional.
    @SerializedName(FIELD_CORRELATION_ID)
    val correlationId: String? = null,

    // Optional; possible values are "login", "consent", "select_account", "none".
    @SerializedName(FIELD_PROMPT)
    val prompt: String? = null,

    // Optional; claims request in JSON format.
    @SerializedName(FIELD_CLAIMS)
    val claims: String? = null,

    // If not provided, we assume this is false.
    @SerializedName(FIELD_IS_SECURITY_TOKEN_SERVICE)
    val isSecurityTokenService: Boolean = false,

    // Optional.
    @SerializedName(FIELD_NONCE)
    val nonce : String? = null,

    // Optional; OAuth protocol "state" parameter. We pass it back as-is in the response.
    @SerializedName(FIELD_STATE)
    val state: String? = null,

    // Optional.
    @SerializedName(FIELD_LOGIN_HINT)
    val loginHint: String? = null,

    // Optional.
    @SerializedName(FIELD_INSTANCE_AWARE)
    val instanceAware : Boolean = false,

    // Optional; additional extra query parameters to include in the token request.
    // Note: PoP token parameters will come through here.
    @SerializedName(FIELD_EXTRA_PARAMETERS)
    val extraParameters: Map<String, String>? = null
) {
    companion object {
        const val FIELD_HOME_ACCOUNT_ID = "accountId"
        const val FIELD_CLIENT_ID = "clientId"
        const val FIELD_AUTHORITY = "authority"
        const val FIELD_SCOPES = "scope"
        const val FIELD_REDIRECT_URI = "redirectUri"
        const val FIELD_CORRELATION_ID = "correlationId"
        const val FIELD_PROMPT = "prompt"
        const val FIELD_CLAIMS = "claims"
        const val FIELD_IS_SECURITY_TOKEN_SERVICE = "isSts"
        const val FIELD_NONCE = "nonce"
        const val FIELD_STATE = "state"
        const val FIELD_LOGIN_HINT = "loginHint"
        const val FIELD_INSTANCE_AWARE = "instanceAware"
        const val FIELD_EXTRA_PARAMETERS = "extraParameters"
        const val DEFAULT_AUTHORITY = "https://login.microsoftonline.com/common"
    }
}
