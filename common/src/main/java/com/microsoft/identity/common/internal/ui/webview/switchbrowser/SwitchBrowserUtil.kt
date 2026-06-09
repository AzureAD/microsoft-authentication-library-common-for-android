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
package com.microsoft.identity.common.internal.ui.webview.switchbrowser

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.SWITCH_BROWSER
import com.microsoft.identity.common.internal.ui.browser.AndroidBrowserSelector
import com.microsoft.identity.common.java.ui.BrowserDescriptor
import com.microsoft.identity.common.logging.Logger
import androidx.core.net.toUri

/**
 * Utility class for Switch Browser protocol
 */
object SwitchBrowserUtil {

    private const val TAG = "SwitchBrowserUtils"

    /**
     * Determines whether the Switch Browser protocol can be used for the current request.
     * Both conditions must be met:
     * 1. A compatible browser is installed on the device.
     * 2. The app manifest declares a handler for the switch_browser_resume redirect.
     *
     * @param context Android context for browser and manifest resolution.
     * @param redirectUri The app's redirect URI.
     * @return true if switch browser is supported, false otherwise.
     */
    @JvmStatic
    fun isSwitchBrowserSupported(context: Context, redirectUri: String): Boolean {
        val methodTag = "$TAG:isSwitchBrowserSupported"
        try {
            if (!isCompatibleBrowserInstalled(context)) {
                Logger.info(methodTag, "No compatible browser found for Switch Browser protocol.")
                return false
            }

            if (!isSwitchBrowserResumeHandlerRegistered(context, redirectUri)) {
                Logger.warn(
                    methodTag,
                    "SwitchBrowserRedirectActivity not registered in manifest for redirect URI. " +
                        "Switch Browser will not be enabled."
                )
                return false
            }
            return true
        } catch (e: Exception) {
            Logger.warn(methodTag, "Failed to check Switch Browser prerequisites: ${e.message}")
        }
        return false
    }

    /**
     * Checks whether a compatible browser (Chrome, Edge, or AEA) is installed on the device.
     *
     * @param context Android context for browser resolution.
     * @return true if a compatible browser is installed, false otherwise.
     */
    @JvmStatic
    fun isCompatibleBrowserInstalled(context: Context): Boolean {
        val browserSelector = AndroidBrowserSelector(context)
        val browser = browserSelector.selectBrowser(
            BrowserDescriptor.getBrowserSafeListForSwitchBrowser(),
            null
        )
        return browser != null
    }

    /**
     * Checks whether the app's manifest declares [SwitchBrowserRedirectActivity] with an
     * intent-filter that can handle the switch_browser_resume redirect URI. This
     * validates both that the activity is declared and that it can handle the resume URI.
     * Note: This check is for non-broker flows only. Broker uses its own subclass via a
     * different code path.
     *
     * @param context Android context for PackageManager access.
     * @param redirectUri The app's redirect URI.
     * @return true if a valid handler is registered, false otherwise.
     */
    @JvmStatic
    fun isSwitchBrowserResumeHandlerRegistered(
        context: Context,
        redirectUri: String
    ): Boolean {
        val resumeUri = (redirectUri.trimEnd('/') + "/" + SWITCH_BROWSER.RESUME_PATH).toUri()
        val intent = Intent(Intent.ACTION_VIEW, resumeUri).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addCategory(Intent.CATEGORY_BROWSABLE)
            setPackage(context.packageName)
        }
        val resolvedActivities = context.packageManager.queryIntentActivities(
            intent, PackageManager.MATCH_DEFAULT_ONLY
        )
        return resolvedActivities.isNotEmpty()
    }
}
