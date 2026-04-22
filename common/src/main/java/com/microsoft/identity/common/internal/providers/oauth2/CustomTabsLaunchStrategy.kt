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
 * Browser launch strategy backed by [CustomTabsManager], with fallback to regular browser intent.
 */
internal class CustomTabsLaunchStrategy(
    private val activity: FragmentActivity,
    private val browserSupportsCustomTabs: Boolean,
    private val customTabsManager: CustomTabsManager = CustomTabsManager(activity)
) : BrowserLaunchStrategy {

    private val tag = CustomTabsLaunchStrategy::class.java.simpleName

    override fun launch(browserPackageName: String, processUri: String) {
        val methodTag = "$tag:launch"
        val browserIntent: Intent
        if (browserSupportsCustomTabs) {
            Logger.info(methodTag, "CustomTabsService is supported.")
            if (!customTabsManager.bind(activity, browserPackageName)) {
                Logger.warn(methodTag, "Failed to bind CustomTabsService.")
                browserIntent = createBrowserFallbackIntent(browserPackageName)
            } else {
                browserIntent = customTabsManager.customTabsIntent.intent
            }
        } else {
            Logger.warn(methodTag, "CustomTabsService is NOT supported")
            browserIntent = createBrowserFallbackIntent(browserPackageName)
        }

        browserIntent.setData(processUri.toUri())
        activity.startActivity(browserIntent)
    }

    override fun handlesCancellationOnResume(): Boolean = true

    override fun cleanup() {
        customTabsManager.unbind()
    }

    private fun createBrowserFallbackIntent(browserPackageName: String): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setPackage(browserPackageName)
        }
    }
}
