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
package com.microsoft.identity.common.java.nativeauth.providers

import com.microsoft.identity.common.java.logging.Logger

/**
 * Validates custom headers provided by a [NativeAuthRequestInterceptor].
 * Enforces that header names start with "x-" and do not use reserved prefixes.
 */
object NativeAuthHeaderValidator {

    private val TAG = NativeAuthHeaderValidator::class.java.simpleName

    private val RESERVED_PREFIXES = listOf("x-ms-", "x-client-", "x-broker-", "x-app-")

    /**
     * Filters a map of headers, returning only those that are valid per the interceptor contract.
     * Invalid headers are logged as warnings and excluded from the result.
     *
     * @param headers The raw headers provided by the interceptor.
     * @return A map containing only valid headers using lowercase field names, or an empty map if none are valid.
     */
    fun filterValidHeaders(headers: Map<String, String>): Map<String, String> {
        val validHeaders = mutableMapOf<String, String>()

        for ((field, value) in headers) {
            val lowerField = field.lowercase()

            if (!lowerField.startsWith("x-")) {
                Logger.warn(
                    TAG,
                    "Additional header field \"$field\" must start with the \"x-\" prefix. Ignoring."
                )
                continue
            }

            var isReserved = false
            for (reserved in RESERVED_PREFIXES) {
                if (lowerField.startsWith(reserved)) {
                    Logger.warn(
                        TAG,
                        "Additional header field \"$field\" uses reserved prefix \"$reserved\". Ignoring."
                    )
                    isReserved = true
                    break
                }
            }

            if (!isReserved) {
                validHeaders[lowerField] = value
            }
        }

        return validHeaders
    }
}
