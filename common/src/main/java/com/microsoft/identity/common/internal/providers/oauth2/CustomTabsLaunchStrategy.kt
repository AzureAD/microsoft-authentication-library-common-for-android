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
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.microsoft.identity.common.internal.ui.browser.CustomTabsManager
import com.microsoft.identity.common.logging.Logger

/**
 * Browser launch strategy that uses Custom Tabs when supported.
 */
class CustomTabsLaunchStrategy(
    private val activity: FragmentActivity
) : BrowserLaunchStrategy {

    private val customTabsManager = CustomTabsManager(activity)

    companion object {
        private val TAG: String = CustomTabsLaunchStrategy::class.java.simpleName
    }

    override fun launch() {
        val methodTag = "$TAG:launch"
        val extras = activity.intent.extras
        if (extras == null) {
            finishWithError(
                methodTag,
                "Intent extras are missing - Cannot proceed with browser switch"
            )
            return
        }
        val browserPackageName = extras.getString(SwitchBrowserActivity.BROWSER_PACKAGE_NAME)
        val browserSupportsCustomTabs = extras.getBoolean(SwitchBrowserActivity.BROWSER_SUPPORTS_CUSTOM_TABS, false)
        val processUri = extras.getString(SwitchBrowserActivity.PROCESS_URI)

        if (browserPackageName.isNullOrBlank()) {
            finishWithError(methodTag, "No browser package name found in extras - Cannot proceed with browser switch")
            return
        }

        if (processUri.isNullOrBlank()) {
            finishWithError(methodTag, "No process URI found in extras - Cannot proceed with browser switch")
            return
        }

        Logger.info(
            methodTag,
            "Launching switch browser request on browser: $browserPackageName, Custom Tabs supported: $browserSupportsCustomTabs"
        )

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

        browserIntent.setData(processUri.toUri())
        activity.startActivity(browserIntent)
    }

    override fun handlesCancellationOnResume(): Boolean = true

    override fun cleanup() {
        customTabsManager.unbind()
    }

    private fun finishWithError(methodTag: String, message: String) {
        Logger.error(methodTag, message, null)
        activity.finish()
    }
}
