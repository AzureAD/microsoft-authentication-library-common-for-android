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

import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.microsoft.identity.common.internal.ui.browser.CustomTabsManager
import com.microsoft.identity.common.logging.Logger

/**
 * [BrowserLaunchStrategy] that uses Custom Tabs (or a plain [Intent.ACTION_VIEW] fallback) to
 * open the authentication URL.
 */
class CustomTabsLaunchStrategy(
    private val activity: FragmentActivity
) : BrowserLaunchStrategy {

    companion object {
        private val TAG: String = CustomTabsLaunchStrategy::class.java.simpleName
    }

    private val customTabsManager = CustomTabsManager(activity)

    /**
     * Launches Custom Tabs (or a plain browser intent if service binding fails).
     */
    override fun launch(processUri: Uri, browserPackageName: String) {
        val methodTag = "$TAG:launch"

        val extras = activity.intent?.extras
        val browserSupportsCustomTabs = extras?.getBoolean(
            SwitchBrowserActivity.BROWSER_SUPPORTS_CUSTOM_TABS,
            false
        ) ?: false

        val browserIntent: Intent
        if (browserSupportsCustomTabs) {
            Logger.info(methodTag, "CustomTabsService is supported.")
            if (!customTabsManager.bind(activity, browserPackageName)) {
                Logger.warn(methodTag, "Failed to bind CustomTabsService.")
                browserIntent = Intent(Intent.ACTION_VIEW)
            } else {
                browserIntent = customTabsManager.customTabsIntent.intent
            }
        } else {
            Logger.warn(methodTag, "CustomTabsService is NOT supported")
            browserIntent = Intent(Intent.ACTION_VIEW)
            browserIntent.setPackage(browserPackageName)
        }

        browserIntent.setData(processUri)
        activity.startActivity(browserIntent)
    }

    /**
     * Custom Tabs does not provide explicit cancel callbacks; cancellation is inferred on resume.
     */
    override fun handlesCancellationOnResume(): Boolean = true

    /**
     * Unbinds the Custom Tabs service.
     */
    override fun cleanup() {
        customTabsManager.unbind()
    }
}
