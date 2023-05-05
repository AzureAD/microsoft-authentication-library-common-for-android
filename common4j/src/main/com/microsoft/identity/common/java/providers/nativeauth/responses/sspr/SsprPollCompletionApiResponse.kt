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
package com.microsoft.identity.common.java.providers.nativeauth.responses.sspr

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.providers.nativeauth.IApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.interactors.InnerError
import com.microsoft.identity.common.java.util.isInvalidGrant
import com.microsoft.identity.common.java.util.isPollInProgress
import com.microsoft.identity.common.java.util.isPollSucceeded
import java.net.HttpURLConnection

class SsprPollCompletionApiResponse(
    @Expose override var statusCode: Int,
    @Expose @SerializedName("status") val status: String?,
    @Expose @SerializedName("signin_slt") val signinSlt: String?,
    @Expose @SerializedName("error") val error: String?,
    @Expose @SerializedName("error_description") val errorDescription: String?,
    @Expose @SerializedName("error_uri") val errorUri: String?,
    @Expose @SerializedName("error_codes") val errorCodes: List<Int>?,
    @Expose @SerializedName("inner_errors") val innerErrors: List<InnerError>?
): IApiResponse(statusCode) {

    fun toResult(): SsprPollCompletionApiResult {
        if (statusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
            if (error.isInvalidGrant()) {
                // TODO advanced error handling
                return SsprPollCompletionApiResult.UnknownError(error, errorDescription)
            } else {
                // TODO log the API response, in a PII-safe way
                return SsprPollCompletionApiResult.UnknownError(error, errorDescription)
            }
        } else {
            if (status.isPollInProgress()) {
                return SsprPollCompletionApiResult.InProgress
            } else if (status.isPollSucceeded()) {
                return SsprPollCompletionApiResult.PollingSucceeded
            } else {
                return SsprPollCompletionApiResult.PollingFailed(error.orEmpty(), errorDescription.orEmpty())
            }
        }
    }
}