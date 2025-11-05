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
package com.microsoft.identity.common.java.commands

import com.google.gson.annotations.SerializedName

/**
 * Payload returned to caller to trigger interactive auth.
 */
data class WebAppsInteractiveRequest(
    @SerializedName(FIELD_HOME_ACCOUNT_ID)
    val homeAccountId: String?,

    @SerializedName(FIELD_CLIENT_ID)
    val clientId: String,

    @SerializedName(FIELD_AUTHORITY)
    val authority: String,

    @SerializedName(FIELD_SCOPE)
    val scope: String,

    @SerializedName(FIELD_REDIRECT)
    val redirect: String,

    @SerializedName(FIELD_CORRELATION_ID)
    val correlationId: String,

    @SerializedName(FIELD_PROMPT)
    val prompt: String?,

    @SerializedName(FIELD_USER_NAME)
    val userName: String?,

    @SerializedName(FIELD_NONCE)
    val nonce: String?,

    @SerializedName(FIELD_INSTANCE_AWARE)
    val instanceAware: Boolean? = false,

    @SerializedName(FIELD_CLAIMS)
    val claims: String?,

    @SerializedName(FIELD_EXTRA_OPTIONS)
    val extraOptions: Map<String, String>?
) {
    companion object {
        const val FIELD_HOME_ACCOUNT_ID = "homeAccountId"
        const val FIELD_CLIENT_ID = "clientId"
        const val FIELD_AUTHORITY = "authority"
        const val FIELD_SCOPE = "scope"
        const val FIELD_REDIRECT = "redirect"
        const val FIELD_CORRELATION_ID = "correlationId"
        const val FIELD_PROMPT = "prompt"
        const val FIELD_USER_NAME = "userName"
        const val FIELD_NONCE = "nonce"
        const val FIELD_INSTANCE_AWARE = "instance_aware"
        const val FIELD_CLAIMS = "claims"
        const val FIELD_EXTRA_OPTIONS = "extraOptions"
    }
}
