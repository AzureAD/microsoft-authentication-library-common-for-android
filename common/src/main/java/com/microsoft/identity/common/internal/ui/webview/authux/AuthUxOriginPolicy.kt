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

import android.net.Uri
import com.microsoft.identity.common.internal.broker.AuthUxJavaScriptInterface
import com.microsoft.identity.common.logging.Logger

/**
 * Defines the security boundary for exposing the WebMessageListener-based Auth UX bridge.
 *
 * AndroidX uses the allowed-origin rules at registration time to decide which documents receive
 * the native WebMessageListener object and the document-start facade. These rules must remain
 * narrow and identical for both registrations; callback validation is defense in depth, not a
 * substitute for preventing bridge exposure to an untrusted document.
 *
 * The WebMessageListener-based transport supports trusted Auth UX subdomains over HTTP port 80 and
 * HTTPS port 443. An initial URL outside that representable contract uses the
 * `addJavascriptInterface` bridge. An omitted port is interpreted as the scheme's default; an
 * explicit non-default port is a distinct origin and is not authorized by the registration rules.
 */
open class AuthUxOriginPolicy {
    /**
     * Returns whether [initialUrl] is covered by the fixed registration origin rules.
     */
    open fun isInitialUrlSupportedByRegistrationRules(initialUrl: String?): Boolean {
        if (initialUrl.isNullOrEmpty()) {
            Logger.info(TAG, "Initial Auth UX URL is unavailable for origin-rule validation.")
            return false
        }

        val isSupported = isOriginSupportedByRegistrationRules(Uri.parse(initialUrl))
        if (!isSupported) {
            Logger.info(
                TAG,
                "Initial Auth UX URL is not supported by WebMessageListener registration rules."
            )
        }

        return isSupported
    }

    /**
     * Returns the registration-time access-control rules shared by the native listener and
     * document-start facade.
     */
    open fun getRegistrationOriginRules(): Set<String> = REGISTRATION_ORIGIN_RULES

    /**
     * Allows callbacks only from a main frame whose origin matches the registration policy.
     */
    open fun isAllowedCallback(sourceOrigin: Uri, isMainFrame: Boolean): Boolean {
        if (!isMainFrame) {
            Logger.info(TAG, "Rejected Auth UX WebMessage callback from a subframe.")
            return false
        }

        return isOriginSupportedByRegistrationRules(sourceOrigin)
    }

    /**
     * Applies the same scheme, host, and port contract used by AndroidX registration rules.
     *
     * Android [Uri] uses `-1` when no port is written, so omitted ports and explicit defaults
     * (`:80` for HTTP and `:443` for HTTPS) are accepted. Other ports are rejected because they
     * identify distinct origins that the registration rules do not authorize.
     */
    private fun isOriginSupportedByRegistrationRules(origin: Uri): Boolean {
        if (origin.isOpaque || origin.host.isNullOrEmpty()) {
            Logger.info(TAG, "Origin is opaque or has no host.")
            return false
        }

        val defaultPort = when (origin.scheme?.lowercase()) {
            "http" -> HTTP_DEFAULT_PORT
            "https" -> HTTPS_DEFAULT_PORT
            else -> {
                Logger.info(TAG, "Origin uses a scheme unsupported by registration rules.")
                return false
            }
        }
        if (origin.port != NO_EXPLICIT_PORT && origin.port != defaultPort) {
            Logger.info(TAG, "Origin uses a non-default port unsupported by registration rules.")
            return false
        }

        val isTrustedHost = AuthUxJavaScriptInterface.isValidHostForInterface(origin.host)
        if (!isTrustedHost) {
            Logger.info(TAG, "Origin host is not trusted for Auth UX.")
        }

        return isTrustedHost
    }

    companion object {
        private val TAG = AuthUxOriginPolicy::class.java.simpleName

        private const val NO_EXPLICIT_PORT = -1
        private const val HTTP_DEFAULT_PORT = 80
        private const val HTTPS_DEFAULT_PORT = 443

        /**
         * Registration-time security boundary for the WebMessageListener-based Auth UX bridge.
         *
         * Do not replace with `"*"` or broaden independently of callback validation. Doing so would
         * expose the native object or facade to documents outside the approved Auth UX origins.
         */
        private val REGISTRATION_ORIGIN_RULES = setOf(
            "https://*.microsoftonline.com",
            "http://*.microsoftonline.com",
            "https://*.microsoftonline.us",
            "http://*.microsoftonline.us",
            "https://*.microsoftonline.cn",
            "http://*.microsoftonline.cn",
            "https://*.microsoft.com",
            "http://*.microsoft.com"
        )
    }
}
