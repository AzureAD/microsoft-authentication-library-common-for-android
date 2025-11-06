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
 * Error response for WebApps token requests.
 */
data class WebAppsErrorResponsePayload(
    // Fully qualified class name
    @SerializedName(FIELD_TYPE)
    val type: String,

    @SerializedName(FIELD_MESSAGE)
    val message: String?,

    // Present for ClientException
    @SerializedName(FIELD_CLIENT_ERROR_CODE)
    val clientErrorCode: String?,

    // Present for ServiceException
    @SerializedName(FIELD_HTTP_STATUS_CODE)
    val httpStatusCode: Int?
) {
    companion object {
        const val FIELD_TYPE = "type"
        const val FIELD_MESSAGE = "message"
        const val FIELD_CLIENT_ERROR_CODE = "clientErrorCode"
        const val FIELD_HTTP_STATUS_CODE = "httpStatusCode"
    }
}
