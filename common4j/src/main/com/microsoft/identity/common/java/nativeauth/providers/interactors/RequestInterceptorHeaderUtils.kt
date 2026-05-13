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
package com.microsoft.identity.common.java.nativeauth.providers.interactors

import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthHeaderValidator
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthRequestInterceptor
import java.net.URL

internal fun applyInterceptorHeaders(
    requestUrl: URL,
    headers: Map<String, String?>,
    requestInterceptor: NativeAuthRequestInterceptor?
): Map<String, String?> {
    if (requestInterceptor == null) return headers

    val additionalHeaders = requestInterceptor.additionalHeaders(requestUrl) ?: return headers
    val validHeaders = NativeAuthHeaderValidator.filterValidHeaders(additionalHeaders)
    if (validHeaders.isEmpty()) return headers

    val mergedHeaders = headers.toMutableMap()
    for ((field, value) in validHeaders) {
        val existingHeader = mergedHeaders.keys.firstOrNull { it.equals(field, ignoreCase = true) }
        if (existingHeader != null) {
            mergedHeaders.remove(existingHeader)
        }
        mergedHeaders[field] = value
    }

    return mergedHeaders
}
