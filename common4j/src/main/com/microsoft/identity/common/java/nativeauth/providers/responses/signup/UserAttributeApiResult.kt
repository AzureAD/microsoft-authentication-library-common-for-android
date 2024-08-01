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
package com.microsoft.identity.common.java.nativeauth.providers.responses.signup

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.nativeauth.util.ILoggable

/**
 * This data structure represents the information about the required user
 * attribute for sign up API.
 */
data class UserAttributeApiResult(
    @SerializedName("name") val name: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("required") val required: Boolean?,
    @SerializedName("options") val options: UserAttributeOptionsApiResult?
) : ILoggable {
    override fun toUnsanitizedString() = "UserAttributeApiResult(name=$name, type=$type, required=$required" +
            "options=$options)"

    override fun toString(): String = toUnsanitizedString()
}
