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

package com.microsoft.identity.common.internal.ui.webview.authux

import android.webkit.WebView
import androidx.webkit.ScriptHandler
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.microsoft.identity.common.logging.Logger

/**
 * Injectable boundary around AndroidX WebKit APIs used by the Auth UX transport.
 */
interface AuthUxWebKitAdapter {
    fun isWebMessageListenerSupported(): Boolean

    fun isDocumentStartScriptSupported(): Boolean

    fun addWebMessageListener(
        webView: WebView,
        objectName: String,
        allowedOriginRules: Set<String>,
        listener: WebViewCompat.WebMessageListener
    )

    fun removeWebMessageListener(webView: WebView, objectName: String)

    fun addDocumentStartScript(
        webView: WebView,
        script: String,
        allowedOriginRules: Set<String>
    ): ScriptHandler
}

object DefaultAuthUxWebKitAdapter : AuthUxWebKitAdapter {
    override fun isWebMessageListenerSupported(): Boolean {
        val isSupported =
            WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)
        Logger.info(TAG, "WebMessageListener feature supported: $isSupported.")
        return isSupported
    }

    override fun isDocumentStartScriptSupported(): Boolean {
        val isSupported =
            WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
        Logger.info(TAG, "Document-start script feature supported: $isSupported.")
        return isSupported
    }

    override fun addWebMessageListener(
        webView: WebView,
        objectName: String,
        allowedOriginRules: Set<String>,
        listener: WebViewCompat.WebMessageListener
    ) {
        WebViewCompat.addWebMessageListener(webView, objectName, allowedOriginRules, listener)
        Logger.info(TAG, "Registered Auth UX WebMessageListener.")
    }

    override fun removeWebMessageListener(webView: WebView, objectName: String) {
        WebViewCompat.removeWebMessageListener(webView, objectName)
        Logger.info(TAG, "Removed Auth UX WebMessageListener.")
    }

    override fun addDocumentStartScript(
        webView: WebView,
        script: String,
        allowedOriginRules: Set<String>
    ): ScriptHandler {
        val scriptHandler =
            WebViewCompat.addDocumentStartJavaScript(webView, script, allowedOriginRules)
        Logger.info(TAG, "Registered Auth UX document-start script.")
        return scriptHandler
    }

    private val TAG = DefaultAuthUxWebKitAdapter::class.java.simpleName
}
