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
package com.microsoft.identity.common.internal.ui.webview

/**
 * Record representing a JavaScript script to be injected into a WebView, along with metadata
 * about the script.
 *
 * @param id A unique identifier for the script.
 * @param script The JavaScript code to be injected.
 * @param allowedUrls An optional set of URL patterns where the script is allowed to be injected.
 * If null, the script can be injected into any URL. If non-null, the script will only be injected
 * into URLs that match one of the patterns in this set.
 */
class JsScriptRecord(
    val id: String,
    val script: String,
    private val allowedUrls: Set<String>?
) {

    /**
     * Checks whether this script is allowed to execute for the given [url].
     *
     * A script is considered allowed if:
     * - [allowedUrls] is `null`, meaning no restrictions.
     * - The provided [url] starts with any of the prefixes in [allowedUrls].
     *
     * @param url The URL to check against the allowed list.
     * @return `true` if the script can execute for this URL, `false` otherwise.
     */
    fun isAllowedForUrl(url: String): Boolean {
        // No restrictions — allowed for any URL
        if (allowedUrls == null) return true

        // Check if the URL starts with any allowed prefix
        return allowedUrls.any { prefix -> url.startsWith(prefix) }
    }
}