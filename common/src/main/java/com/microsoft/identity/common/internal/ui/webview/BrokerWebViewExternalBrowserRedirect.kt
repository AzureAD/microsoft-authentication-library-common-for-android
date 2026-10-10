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
package com.microsoft.identity.common.internal.ui.webview

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.microsoft.identity.common.logging.Logger

/**
 * Recognizes and launches server-selected external URLs from broker-hosted WebViews.
 */
object BrokerWebViewExternalBrowserRedirect {
    private val TAG = BrokerWebViewExternalBrowserRedirect::class.java.simpleName

    /** Query parameter used by eSTS to opt a destination into external-browser routing. */
    const val QUERY_PARAMETER = "broker_webview_external_browser"

    /** The only query parameter value that opts into external-browser routing. */
    const val ENABLED_VALUE = "1"

    /**
     * Returns whether [url] is an HTTPS destination explicitly marked for external-browser routing.
     */
    @JvmStatic
    fun isMarked(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            uri.scheme.equals("https", ignoreCase = true) &&
                uri.getQueryParameter(QUERY_PARAMETER) == ENABLED_VALUE
        } catch (exception: IllegalArgumentException) {
            Logger.warn("$TAG:isMarked", "Unable to parse external-browser redirect marker.")
            false
        } catch (exception: UnsupportedOperationException) {
            Logger.warn("$TAG:isMarked", "External-browser redirect URL is not hierarchical.")
            false
        }
    }

    /**
     * Opens [url] unchanged with an external activity.
     *
     * @return `true` when the launch was requested, or `false` when no handler is available.
     */
    @JvmStatic
    fun launch(context: Context, url: String): Boolean {
        val methodTag = "$TAG:launch"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)
        if (intent.resolveActivity(context.packageManager) == null) {
            Logger.warn(methodTag, "Unable to find an app to handle the external-browser redirect.")
            return false
        }

        return try {
            context.startActivity(intent)
            true
        } catch (exception: ActivityNotFoundException) {
            Logger.error(methodTag, "External-browser redirect handler is no longer available.", exception)
            false
        } catch (exception: SecurityException) {
            Logger.error(methodTag, "External-browser redirect launch was denied.", exception)
            false
        }
    }
}
