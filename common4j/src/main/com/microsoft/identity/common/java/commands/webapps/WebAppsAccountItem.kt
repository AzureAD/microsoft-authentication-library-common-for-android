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
 * Represents an account item in Web Apps broker communication.
 */
data class WebAppsAccountItem(
    // Required; UPN
    @SerializedName(FIELD_USER_NAME)
    val userName: String,

    // Required.
    @SerializedName(FIELD_HOME_ACCOUNT_ID)
    val homeAccountId: String,

    // Optional; we will most likely not use this field, as we will send the properties one level up.
    @SerializedName(FIELD_PROPERTIES)
    val properties: MatsProperties? = null
) {
    companion object {
        const val FIELD_USER_NAME = "userName"
        const val FIELD_HOME_ACCOUNT_ID = "id"
        const val FIELD_PROPERTIES = "properties"
    }
}
