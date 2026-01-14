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
 * Request parameters for WebAppsGetTokenSubOperation
 */
data class WebAppsGetTokenSubOperationResponse(

    // Optional; state passed in the request
    @SerializedName(FIELD_STATE)
    val state: String? = null,

    // Required.
    @SerializedName(FIELD_EXPIRES_IN)
    val expiresIn: Long,

    // Optional for now, but will be required once schema design is finalized.
    @SerializedName(FIELD_PROPERTIES)
    val properties: MatsProperties? = null,

    // Required; base64 string containing uid and utid.
    @SerializedName(FIELD_CLIENT_INFO)
    val clientInfo: String,

    // Required.
    @SerializedName(FIELD_ACCOUNT)
    val account: WebAppsAccountItem,

    // Required.
    @SerializedName(FIELD_ID_TOKEN)
    val idToken: String,

    // Required.
    @SerializedName(FIELD_ACCESS_TOKEN)
    val accessToken: String,

    // Required.
    @SerializedName(FIELD_SCOPES)
    val scopes: String
) {
    companion object {
        const val FIELD_STATE = "state"
        const val FIELD_EXPIRES_IN = "expires_in"
        const val FIELD_PROPERTIES = "properties"
        const val FIELD_CLIENT_INFO = "client_info"
        const val FIELD_ACCOUNT = "account"
        const val FIELD_ID_TOKEN = "id_token"
        const val FIELD_ACCESS_TOKEN = "access_token"
        const val FIELD_SCOPES = "scope"
    }
}
