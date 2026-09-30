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
package com.microsoft.identity.common.internal.broker

import android.net.Uri
import android.webkit.WebView
import androidx.annotation.UiThread
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.microsoft.identity.common.adal.internal.AuthenticationConstants
import com.microsoft.identity.common.logging.Logger

/**
 * Origin-scoped WebMessage transport for Auth UX Number Matching and telemetry messages.
 *
 * The listener accepts only HTTPS main-frame messages from the same Microsoft host suffixes used
 * by the existing Auth UX contract. Number Matching is enabled only for broker-hosted interactive
 * flows; telemetry can be enabled for both brokered and non-brokered flows.
 */
class AuthUxWebMessageListener(
    telemetrySink: AuthUxTelemetrySink?,
    allowNumberMatching: Boolean
) : WebViewCompat.WebMessageListener {

    private val messageHandler = AuthUxJavaScriptInterface(
        telemetrySink = telemetrySink,
        allowNumberMatching = allowNumberMatching
    )

    @UiThread
    override fun onPostMessage(
        view: WebView,
        message: WebMessageCompat,
        sourceOrigin: Uri,
        isMainFrame: Boolean,
        replyProxy: JavaScriptReplyProxy
    ) {
        val methodTag = "$TAG:onPostMessage"
        if (!isMainFrame) {
            Logger.warn(methodTag, "Ignoring an Auth UX message from a subframe.")
            return
        }
        if (!isAllowedOrigin(sourceOrigin)) {
            Logger.warn(methodTag, "Ignoring an Auth UX message from a disallowed origin.")
            return
        }
        val data = message.data
        if (data.isNullOrEmpty()) {
            Logger.warn(methodTag, "Ignoring an empty Auth UX message.")
            return
        }
        messageHandler.receiveAuthUxMessage(data)
    }

    companion object {
        private val TAG = AuthUxWebMessageListener::class.java.simpleName

        const val INTERFACE_NAME = "broker"

        private const val HTTPS_SCHEME = "https"
        private const val HTTPS_DEFAULT_PORT = 443

        private val ALLOWED_HOST_SUFFIXES = setOf(
            AuthenticationConstants.Broker.AAD_GLOBAL_URL_HOST_SUFFIX,
            AuthenticationConstants.Broker.AAD_US_URL_HOST_SUFFIX,
            AuthenticationConstants.Broker.AAD_CHINA_URL_HOST_SUFFIX,
            AuthenticationConstants.Broker.AAD_INTUNE_MDM_URL_HOST_SUFFIX
        )

        private val ALLOWED_ORIGIN_RULES = ALLOWED_HOST_SUFFIXES
            .mapTo(linkedSetOf()) { "$HTTPS_SCHEME://*$it" }

        /**
         * Attaches the unified Auth UX listener before the first WebView navigation.
         *
         * @return true when the listener was registered; false when the WebView implementation
         * does not support both WebMessage listeners and document-start scripts.
         */
        @JvmStatic
        @UiThread
        fun hook(
            webView: WebView,
            telemetrySink: AuthUxTelemetrySink?,
            allowNumberMatching: Boolean
        ): Boolean {
            val methodTag = "$TAG:hook"
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) ||
                !WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
            ) {
                Logger.info(
                    methodTag,
                    "Required WebMessage features are unsupported; Auth UX enhancements are disabled."
                )
                return false
            }
            WebViewCompat.addWebMessageListener(
                webView,
                INTERFACE_NAME,
                ALLOWED_ORIGIN_RULES,
                AuthUxWebMessageListener(telemetrySink, allowNumberMatching)
            )
            Logger.info(methodTag, "Unified Auth UX WebMessage listener registered.")
            return true
        }

        /** Removes the listener from its owning WebView. */
        @JvmStatic
        @UiThread
        fun unhook(webView: WebView) {
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
                return
            }
            WebViewCompat.removeWebMessageListener(webView, INTERFACE_NAME)
        }

        @JvmStatic
        fun getAllowedOriginRules(): Set<String> = ALLOWED_ORIGIN_RULES

        @JvmStatic
        internal fun isAllowedOrigin(origin: Uri): Boolean {
            if (!HTTPS_SCHEME.equals(origin.scheme, ignoreCase = true)) {
                return false
            }
            if (origin.port != -1 && origin.port != HTTPS_DEFAULT_PORT) {
                return false
            }
            val host = origin.host?.lowercase() ?: return false
            return ALLOWED_HOST_SUFFIXES.any { suffix -> host.endsWith(suffix) }
        }
    }
}
