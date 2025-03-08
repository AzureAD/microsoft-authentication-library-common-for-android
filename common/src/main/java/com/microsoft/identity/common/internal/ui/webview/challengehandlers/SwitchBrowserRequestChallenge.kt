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
 * SwitchBrowserRequestChallenge is a challenge to switch from WebView to browser.
 * It contains the URI to be opened in the new browser.
 */
data class SwitchBrowserRequestChallenge(
    val uri: Uri,
) {

    companion object {
        /**
         * Construct a SwitchBrowserRequestChallenge from the redirect URI.
         *
         * @param redirectUrl The redirect URL containing the switch browser code and action URI.
         * e.g. {redirectUrl}/switch_browser?code=code&action_uri=action-uri
         *
         * @return The SwitchBrowserRequestChallenge constructed from the redirect URI.
         * e.g. SwitchBrowserRequestChallenge(uri = action-uri?code=code)
         * params: redirectUri: Uri
         */
        @JvmStatic
        @Throws(Exception::class)
        fun constructFromRedirectUrl(redirectUrl: String): SwitchBrowserRequestChallenge {
            Uri.parse(redirectUrl).let { redirectUri ->
                SwitchBrowserUriHelper.buildProcessUri(redirectUri).let { processUri ->
                    return SwitchBrowserRequestChallenge(uri = processUri)
                }
            }
        }
    }
}
