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
package com.microsoft.identity.common.internal.ui.browser

import android.content.Context
import androidx.browser.customtabs.CustomTabsClient
import com.microsoft.identity.common.logging.Logger

/**
 * Utility singleton for detecting whether the AuthTab feature is supported on the current device
 * and browser.
 *
 * AuthTab is a specialized Custom Tab for authentication flows, available in Chrome 137+ with
 * AndroidX Browser 1.9.0+. Use [isAuthTabSupported] to check availability before attempting to
 * launch an AuthTab flow, allowing graceful fallback to standard browser or WebView strategies.
 */
object AuthTabCapabilityDetector {

    private const val TAG = "AuthTabCapabilityDetector"

    /**
     * Checks whether the AuthTab feature is supported for the given browser package.
     *
     * Returns `false` immediately if [packageName] is null or empty, without querying the browser.
     * Any exception thrown by the underlying [CustomTabsClient.isAuthTabSupported] call is caught
     * and logged, returning `false` to allow callers to fall back gracefully.
     *
     * @param context     The application or activity [Context] used to query the browser service.
     * @param packageName The package name of the browser to query for AuthTab support.
     * @return `true` if AuthTab is supported by the specified browser; `false` otherwise.
     */
    fun isAuthTabSupported(context: Context, packageName: String?): Boolean {
        val methodTag = "$TAG:isAuthTabSupported"
        if (packageName.isNullOrEmpty()) {
            Logger.warn(methodTag, "packageName is null or empty; AuthTab not supported.")
            return false
        }
        return try {
            CustomTabsClient.isAuthTabSupported(context, packageName)
        } catch (e: Exception) {
            Logger.warn(methodTag, "Exception while checking AuthTab support: ${e.message}")
            false
        }
    }
}
