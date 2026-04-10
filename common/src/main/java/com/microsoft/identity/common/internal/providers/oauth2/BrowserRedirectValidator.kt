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

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.exception.ErrorStrings

/**
 * Validates that no other application is registered to handle the same custom URL scheme
 * used for the BrowserTabActivity redirect URI.
 */
object BrowserRedirectValidator {

    private const val BROWSER_TAB_ACTIVITY_CLASS =
        "com.microsoft.identity.client.BrowserTabActivity"
    private const val CURRENT_TASK_BROWSER_TAB_ACTIVITY_CLASS =
        "com.microsoft.identity.client.CurrentTaskBrowserTabActivity"

    /**
     * Verifies that no other application is listening on the custom URL scheme defined by
     * [redirectUri]. If another application's activity is found that handles the same scheme,
     * a [ClientException] with error code
     * [ErrorStrings.MULTIPLE_APPS_LISTENING_CUSTOM_URL_SCHEME] is thrown.
     *
     * @param context      The Android context used to query the PackageManager.
     * @param redirectUri  The redirect URI whose URL scheme will be checked.
     * @param useCurrentTask Whether the flow uses [CurrentTaskBrowserTabActivity] (true) or
     *                       [BrowserTabActivity] (false) as the expected activity class.
     * @throws ClientException if another application is found listening on the same URL scheme.
     */
    @JvmStatic
    @Throws(ClientException::class)
    fun validateNoMultipleAppsListening(
        context: Context,
        redirectUri: String,
        useCurrentTask: Boolean
    ) {
        val packageManager = context.packageManager ?: return

        val intent = Intent(Intent.ACTION_VIEW).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addCategory(Intent.CATEGORY_BROWSABLE)
            data = Uri.parse(redirectUri)
        }

        val resolvedActivities = packageManager.queryIntentActivities(
            intent,
            PackageManager.GET_RESOLVED_FILTER
        )

        val expectedActivityClassName = if (useCurrentTask) {
            CURRENT_TASK_BROWSER_TAB_ACTIVITY_CLASS
        } else {
            BROWSER_TAB_ACTIVITY_CLASS
        }

        for (resolveInfo in resolvedActivities) {
            val activityInfo = resolveInfo.activityInfo ?: continue
            // If this is our own registered BrowserTabActivity, it is expected — skip it.
            if (activityInfo.name == expectedActivityClassName &&
                activityInfo.packageName == context.packageName
            ) {
                continue
            }
            // Another application's activity is also listening on this URL scheme.
            val otherPackage = activityInfo.packageName
            throw ClientException(
                ErrorStrings.MULTIPLE_APPS_LISTENING_CUSTOM_URL_SCHEME,
                "More than one app is listening for the URL scheme defined for BrowserTabActivity " +
                    "in the AndroidManifest. The package name of this other app is: $otherPackage"
            )
        }
    }
}
