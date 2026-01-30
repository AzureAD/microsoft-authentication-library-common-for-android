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
import com.microsoft.identity.common.internal.ui.webview.switchbrowser.SwitchBrowserUriHelper

/**
 * SwitchBrowserChallenge is a challenge to switch from WebView to browser.
 * It contains the URI to be opened in the new browser.
 */
data class SwitchBrowserChallenge(
    val processUri: Uri,
    val authorizationUrl: String
) {
    
    /**
     * Extract the redirect scheme from the authorization URL.
     * The redirect URI is typically in the format msauth://<package>/<signature> or
     * https://<host>/<path>.
     *
     * @return The redirect scheme if found, null otherwise.
     */
    fun getRedirectScheme(): String? {
        return try {
            val redirectUri = getRedirectUri()
            redirectUri?.scheme
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get the full redirect URI parsed from the authorization URL.
     *
     * @return The parsed redirect URI, or null if not found.
     */
    fun getRedirectUri(): Uri? {
        return try {
            val authUri = Uri.parse(authorizationUrl)
            val redirectUriString = authUri.getQueryParameter("redirect_uri")
            redirectUriString?.let { Uri.parse(it) }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Check if the redirect URI uses HTTPS scheme.
     *
     * @return true if the redirect URI is HTTPS, false otherwise.
     */
    fun isHttpsRedirect(): Boolean {
        return getRedirectScheme()?.equals("https", ignoreCase = true) == true
    }

    companion object {
        /**
         * Construct a SwitchBrowserChallenge from the redirect URI.
         *
         * @param redirectUrl The redirect URL containing the switch browser code and action URI.
         * e.g. {redirectUrl}/switch_browser?code=code&action_uri=action-uri
         *
         * @return The SwitchBrowserChallenge constructed from the redirect URI.
         * e.g. SwitchBrowserChallenge(uri = action-uri?code=code)
         * params: redirectUri: Uri
         */
        @JvmStatic
        @Throws(Exception::class)
        fun constructFromRedirectUrl(redirectUrl: String, authorizationUrl: String): SwitchBrowserChallenge {
            Uri.parse(redirectUrl).let { redirectUri ->
                SwitchBrowserUriHelper.buildProcessUri(redirectUri).let { processUri ->
                    return SwitchBrowserChallenge(
                        processUri = processUri,
                        authorizationUrl = authorizationUrl
                    )
                }
            }
        }
    }
}
