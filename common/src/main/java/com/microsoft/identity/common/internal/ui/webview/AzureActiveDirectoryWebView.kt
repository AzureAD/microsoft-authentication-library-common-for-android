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

import android.webkit.WebView
import android.content.Context
import android.util.AttributeSet
import android.webkit.WebViewClient
import com.microsoft.identity.common.logging.Logger

/**
 * WebView implementation used for authentication in client libraries.
 */
class AzureActiveDirectoryWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context,attrs, defStyleAttr) {

    private var azureActiveDirectoryWebViewClient: AzureActiveDirectoryWebViewClient? = null

    companion object {
        private const val TAG = "AzureActiveDirectoryWebView"
    }

    /**
     * Sets the URL to load on the webview client and continues loading the URL.
     */
    override fun loadUrl(url: String) {
        azureActiveDirectoryWebViewClient?.setActiveLoadUrl(url)
        super.loadUrl(url)
    }

    /**
     * Sets the URL to load on the webview client and continues loading the URL.
     */
    override fun loadUrl(url: String, additionalHttpHeaders: Map<String, String>) {
        azureActiveDirectoryWebViewClient?.setActiveLoadUrl(url)
        super.loadUrl(url, additionalHttpHeaders)
    }

    /**
     * Sets the [WebViewClient] for this WebView and tracks the [AzureActiveDirectoryWebViewClient]
     * instance for further use.
     */
    override fun setWebViewClient(webViewClient: WebViewClient) {
        val methodTag = "$TAG:setWebViewClient"
        // Add custom logic here if needed
        super.setWebViewClient(webViewClient)
        azureActiveDirectoryWebViewClient = webViewClient as? AzureActiveDirectoryWebViewClient
        Logger.info(methodTag, "AzureActiveDirectoryWebViewClient set: ${azureActiveDirectoryWebViewClient != null}")
    }
}