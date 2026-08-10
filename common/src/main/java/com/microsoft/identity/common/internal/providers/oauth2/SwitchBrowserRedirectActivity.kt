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
package com.microsoft.identity.common.internal.providers.oauth2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.microsoft.identity.common.logging.Logger

/**
 * Activity that handles the browser redirect for Switch Browser protocol in non-broker flows.
 *
 * When the browser completes the Switch Browser challenge, it redirects to
 * `<redirect_uri>/switch_browser_resume?code=...&action_uri=...&state=...`.
 * This activity catches that redirect via its intent-filter and forwards
 * the data to [SwitchBrowserActivity], which resumes the WebView flow.
 *
 * This activity is declared as `exported="false"` in Common's manifest.
 * Apps that opt into Switch Browser must override this in their own manifest
 * with `exported="true"` and the appropriate intent-filter for their redirect URI scheme/host.
 *
 * Subclasses (e.g. BrokerBrowserRedirectActivity) can override [getAuthIntent] to handle
 * additional redirect scenarios beyond Switch Browser resume.
 */
open class SwitchBrowserRedirectActivity : Activity() {

    companion object {
        private val TAG: String = SwitchBrowserRedirectActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            processRedirectIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        processRedirectIntent(intent)
    }

    private fun processRedirectIntent(intent: Intent?) {
        val methodTag = "$TAG:processRedirectIntent"
        Logger.info(methodTag, "Received redirect from browser.")
        intent?.dataString?.let { intentDataString ->
            getAuthIntent(intentDataString)?.let { authIntent ->
                startActivity(authIntent)
            }
        } ?: run {
            Logger.warn(methodTag, "No data in redirect intent.")
        }
        finishAffinity()
    }

    /**
     * Build the intent to handle the redirect data.
     * Default implementation handles Switch Browser resume.
     * Subclasses can override to handle additional redirect types.
     *
     * @param intentDataString The URI data from the browser redirect.
     * @return The intent to start, or null if the data cannot be handled.
     */
    protected open fun getAuthIntent(intentDataString: String): Intent? {
        val methodTag = "$TAG:getAuthIntent"
        Logger.info(methodTag, "Switching to WebView via SwitchBrowserActivity.")
        return SwitchBrowserActivity.buildSwitchBrowserResumeIntent(
            applicationContext, intentDataString
        )
    }
}
