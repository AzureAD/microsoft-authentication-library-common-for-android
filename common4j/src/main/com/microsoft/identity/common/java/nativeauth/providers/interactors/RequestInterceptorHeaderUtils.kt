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

/**
 * Applies additional interceptor headers to the base request headers for native auth interactors.
 *
 * Uses case-insensitive merge semantics matching iOS behavior: interceptor headers replace
 * matching base headers regardless of casing. Interceptor headers are first validated and
 * normalized to lowercase by [NativeAuthHeaderValidator], which filters out any non-`x-` prefixed
 * headers and reserved prefixes (`x-ms-`, `x-client-`, `x-broker-`, `x-app-`). This ensures that
 * mandatory SDK headers (e.g., `Content-Type`, `x-client-SKU`) cannot be overwritten by the
 * interceptor, since they either lack the `x-` prefix or use a reserved prefix.
 *
 * @param requestUrl The outbound request URL.
 * @param headers The base request headers.
 * @param requestInterceptor Optional interceptor providing additional headers.
 * @return The merged headers map with interceptor values taking precedence for valid custom headers.
 */
internal fun applyInterceptorHeaders(
    requestUrl: URL,
    headers: Map<String, String?>,
    requestInterceptor: NativeAuthRequestInterceptor?
): Map<String, String?> {
    if (requestInterceptor == null) return headers

    val additionalHeaders = requestInterceptor.additionalHeaders(requestUrl) ?: return headers
    // For case-insensitive merge, the headers in RESERVED_PREFIXES are filtered out
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
