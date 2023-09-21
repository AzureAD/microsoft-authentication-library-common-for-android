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
package com.microsoft.identity.common.java.util

import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.providers.nativeauth.IApiResponse
import java.net.HttpURLConnection

/**
 * Helper class to log responses received from Native Auth API
 */
object ApiResultUtil {
    /**
     * Log IApiResponse objects. IResult objects are returned from all native auth endpoints
     *
     * @param tag    The log tag to use.
     * @param response The result object to log.
     */
    // TODO: Do we consider response fields PII?
    fun logResponse(
        tag: String,
        response: IApiResponse
    ) {
        val TAG = tag + ":" + response.javaClass.simpleName
        if (response.statusCode < HttpURLConnection.HTTP_BAD_REQUEST) {
            LogSession.log(tag = TAG, logLevel = Logger.LogLevel.INFO, message = "Success Result")
        } else {
            val code = response.statusCode
            LogSession.log(tag = TAG, logLevel = Logger.LogLevel.WARN, message = "Failure Result (Status Code: $code)")
        }
        logExposedFieldsOfObject(TAG, response)
    }

    private fun logExposedFieldsOfObject(
        tag: String,
        `object`: Any
    ) {
        val TAG = tag + ":" + `object`.javaClass.simpleName
        LogSession.log(
            tag = TAG,
            logLevel = Logger.LogLevel.WARN,
            message = ObjectMapper.serializeExposedFieldsOfObjectToJsonString(`object`)
        )
    }
}
