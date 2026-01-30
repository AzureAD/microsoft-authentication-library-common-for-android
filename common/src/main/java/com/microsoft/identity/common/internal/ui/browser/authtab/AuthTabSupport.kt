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
package com.microsoft.identity.common.internal.ui.browser.authtab

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsService
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import com.microsoft.identity.common.logging.Logger

/**
 * Utility object to detect Auth Tab support.
 * Auth Tab is available in Chrome 137+ and requires androidx.browser:browser:1.9.0+
 */
object AuthTabSupport {

    private const val TAG = "AuthTabSupport"

    /**
     * Check if Auth Tab is enabled via flight and supported by the default browser.
     * Auth Tab requires Chrome 137+ or another supporting browser.
     *
     * @param context The application context
     * @return true if Auth Tab is both enabled and supported, false otherwise
     */
    @JvmStatic
    fun isAuthTabAvailable(context: Context): Boolean {
        val methodTag = "$TAG:isAuthTabAvailable"
        
        // First check if the flight is enabled
        val isFlightEnabled = CommonFlightsManager
            .getFlightsProvider()
            .isFlightEnabled(CommonFlight.ENABLE_AUTH_TAB)
        
        if (!isFlightEnabled) {
            Logger.verbose(methodTag, "Auth Tab flight is disabled")
            return false
        }
        
        // Then check if the browser supports Auth Tab
        return isAuthTabSupported(context)
    }

    /**
     * Check if Auth Tab is supported by the default browser on the device.
     * This does NOT check the flight status.
     *
     * @param context The application context
     * @return true if the default browser supports Auth Tab, false otherwise
     */
    @JvmStatic
    fun isAuthTabSupported(context: Context): Boolean {
        val methodTag = "$TAG:isAuthTabSupported"
        return try {
            val defaultBrowserPackage = getDefaultBrowserPackage(context)
            if (defaultBrowserPackage == null) {
                Logger.warn(methodTag, "No default browser found")
                return false
            }
            
            val isSupported = CustomTabsClient.isAuthTabSupported(context, defaultBrowserPackage)
            Logger.info(methodTag, "Auth Tab supported by $defaultBrowserPackage: $isSupported")
            isSupported
        } catch (e: Exception) {
            // This can happen if the browser library is older or the method doesn't exist
            Logger.warn(methodTag, "Error checking Auth Tab support: ${e.message}")
            false
        }
    }

    /**
     * Check if Auth Tab is supported by a specific browser package.
     *
     * @param context The application context
     * @param browserPackage The package name of the browser to check
     * @return true if the specified browser supports Auth Tab, false otherwise
     */
    @JvmStatic
    fun isAuthTabSupportedByBrowser(context: Context, browserPackage: String): Boolean {
        val methodTag = "$TAG:isAuthTabSupportedByBrowser"
        return try {
            val isSupported = CustomTabsClient.isAuthTabSupported(context, browserPackage)
            Logger.info(methodTag, "Auth Tab supported by $browserPackage: $isSupported")
            isSupported
        } catch (e: Exception) {
            Logger.warn(methodTag, "Error checking Auth Tab support for $browserPackage: ${e.message}")
            false
        }
    }

    /**
     * Get the package name of the default browser on the device.
     *
     * @param context The application context
     * @return The package name of the default browser, or null if not found
     */
    @JvmStatic
    fun getDefaultBrowserPackage(context: Context): String? {
        val methodTag = "$TAG:getDefaultBrowserPackage"
        
        // First, try to get the default browser using VIEW intent
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com"))
        val resolveInfo: ResolveInfo? = context.packageManager.resolveActivity(
            browserIntent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        
        val defaultPackage = resolveInfo?.activityInfo?.packageName
        if (defaultPackage != null && defaultPackage != "android") {
            Logger.verbose(methodTag, "Default browser: $defaultPackage")
            return defaultPackage
        }
        
        // Fallback: find a browser that supports Custom Tabs
        return getCustomTabsSupportingBrowser(context)
    }

    /**
     * Get a browser package that supports Custom Tabs service.
     *
     * @param context The application context
     * @return The package name of a Custom Tabs supporting browser, or null if not found
     */
    private fun getCustomTabsSupportingBrowser(context: Context): String? {
        val methodTag = "$TAG:getCustomTabsSupportingBrowser"
        
        val customTabsIntent = Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION)
        val resolveInfoList: List<ResolveInfo> = context.packageManager.queryIntentServices(
            customTabsIntent,
            PackageManager.MATCH_ALL
        )
        
        if (resolveInfoList.isNotEmpty()) {
            val packageName = resolveInfoList[0].serviceInfo.packageName
            Logger.verbose(methodTag, "Found Custom Tabs supporting browser: $packageName")
            return packageName
        }
        
        Logger.warn(methodTag, "No Custom Tabs supporting browser found")
        return null
    }
}
