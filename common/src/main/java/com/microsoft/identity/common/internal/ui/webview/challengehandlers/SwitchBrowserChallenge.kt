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
package com.microsoft.identity.common.internal.ui.webview.challengehandlers

import android.net.Uri
import com.microsoft.identity.common.adal.internal.AuthenticationConstants
import com.microsoft.identity.common.logging.Logger

/**
 * SwitchBrowserChallenge is a challenge to switch from WebView to browser.
 * It contains the URI to be opened in the new browser.
 */
data class SwitchBrowserChallenge(
    val uri: Uri,
) {

    companion object {

        private val TAG = SwitchBrowserChallenge::class.simpleName

        /**
         * Construct a SwitchBrowserChallenge from the redirect URI.
         *
         * @param redirectUri The redirect URI containing the switch browser code and action URI.
         * e.g. msauth://com.microsoft.identity.client/your-redirect-uri?code=your-switch-browser-code&action_uri=your-action-uri
         *
         * @return The SwitchBrowserChallenge constructed from the redirect URI.
         * e.g. SwitchBrowserChallenge(uri = your-action-uri?code=your-switch-browser-code)
         * params: redirectUri: Uri
         */
        @JvmStatic
        fun constructFromRedirectUri(redirectUri: Uri): SwitchBrowserChallenge? {
            val methodTag = "${TAG}:constructFromUri"

            val actionUri = redirectUri.getQueryParameter(
                AuthenticationConstants.SWITCH_BROWSER.ACTION_URI
            )
            val code = redirectUri.getQueryParameter(
                AuthenticationConstants.SWITCH_BROWSER.CODE
            )
            if (code.isNullOrEmpty()) {
                // This should never happen, but if it does, we should log it and return.
                Logger.warn(methodTag, "Switch browser code is null or empty ")
                return null
            }
            if (actionUri.isNullOrEmpty()) {
                // This should never happen, but if it does, we should log it and return.
                Logger.warn(methodTag, "Switch browser action URI is null or empty ")
                return null
            }

            val queryParams = HashMap<String, String>()
            queryParams[AuthenticationConstants.SWITCH_BROWSER.CODE] = code
            redirectUri.authority?.let { queryParams[AuthenticationConstants.OAuth2.REDIRECT_URI] = it }

            val paths = actionUri.split("/")
            val authority = paths[0]
            val uriBuilder = Uri.Builder()
                .scheme("https")
                .encodedAuthority(authority)
            for (i in 1 until paths.size) {
                uriBuilder.appendPath(paths[i])
            }
            for ((key, value) in queryParams.entries) {
                uriBuilder.appendQueryParameter(key, value)
            }
            return SwitchBrowserChallenge(uriBuilder.build())
        }
    }
}
