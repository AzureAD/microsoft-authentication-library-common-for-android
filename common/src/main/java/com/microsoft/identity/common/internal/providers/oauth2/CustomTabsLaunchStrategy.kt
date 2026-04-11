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
 *
 * Because Custom Tabs does not deliver an explicit cancellation callback, the hosting activity
 * must infer cancellation by detecting a second [android.app.Activity.onResume] after the tab was
 * launched.  [handlesCancellationOnResume] therefore returns `true`.
 *
 * @param activity The hosting [FragmentActivity] used to bind the Custom Tabs service and start
 *                 the browser intent.
 */
class CustomTabsLaunchStrategy(
    private val activity: FragmentActivity
) : BrowserLaunchStrategy {

    companion object {
        private val TAG: String = CustomTabsLaunchStrategy::class.java.simpleName
    }

    private val customTabsManager = CustomTabsManager(activity)

    /**
     * `true` after the first [android.app.Activity.onResume] following the browser launch.
     * The hosting activity sets this to `true` after the initial resume so that a subsequent
     * resume (indicating the user backed out of the browser) is treated as cancellation.
     */
    var cctLaunched: Boolean = false

    /**
     * Launches Custom Tabs (or a plain browser intent if the service cannot be bound) for the
     * given [processUri].
     *
     * @param processUri        The URI to open in the browser.
     * @param browserPackageName Package name of the target browser.
     */
    override fun launch(processUri: Uri, browserPackageName: String) {
        val methodTag = "$TAG:launch"
        cctLaunched = false

        val extras = activity.intent?.extras
        val browserSupportsCustomTabs = extras?.getBoolean(
            SwitchBrowserActivity.BROWSER_SUPPORTS_CUSTOM_TABS, false
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
     * Custom Tabs does not call back on cancellation; the activity must detect it on the next
     * [android.app.Activity.onResume].
     */
    override fun handlesCancellationOnResume(): Boolean = true

    /**
     * Unbinds the Custom Tabs service connection.
     */
    override fun cleanup() {
        customTabsManager.unbind()
    }
}
