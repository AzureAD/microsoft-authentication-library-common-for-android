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
 * Successful response for WebApps token requests.
 */
data class WebAppsTokenResponsePayload(
    @SerializedName(FIELD_USER_NAME)
    val userName: String,

    @SerializedName(FIELD_HOME_ACCOUNT_ID)
    val homeAccountId: String,

    @SerializedName(FIELD_EXPIRES_IN)
    val expiresIn: String,

    @SerializedName(FIELD_ID_TOKEN)
    val idToken: String,

    @SerializedName(FIELD_ACCESS_TOKEN)
    val accessToken: String,

    @SerializedName(FIELD_PROPERTIES)
    val properties: Map<String, String>? = null
) {
    companion object {
        const val FIELD_USER_NAME = "userName"
        const val FIELD_HOME_ACCOUNT_ID = "homeAccountId"
        const val FIELD_EXPIRES_IN = "expiresIn"
        const val FIELD_ID_TOKEN = "idToken"
        const val FIELD_ACCESS_TOKEN = "accessToken"
        const val FIELD_PROPERTIES = "properties"
    }
}
