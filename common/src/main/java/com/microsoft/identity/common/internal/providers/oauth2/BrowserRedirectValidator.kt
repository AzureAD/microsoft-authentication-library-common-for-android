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
import androidx.annotation.VisibleForTesting
import com.microsoft.identity.common.internal.broker.BrokerValidator
import com.microsoft.identity.common.internal.broker.IBrokerValidator
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.exception.ErrorStrings

/**
 * Validates that no other application is registered to handle the same custom URL scheme
 * used for the redirect URI.
 *
 * A resolver is considered legitimate — and therefore skipped — when either:
 *  1. It belongs to our own application. This covers the MSAL client's
 *     `BrowserTabActivity` / `CurrentTaskBrowserTabActivity` as well as the broker's own
 *     `BrokerBrowserRedirectActivity`, which registers intent filters for BOTH the `msauth`
 *     scheme and the App Link host used for the broker redirect. Because the collision was
 *     with the app's own redirect handler, the redirect URI type (msauth vs App Link) is
 *     irrelevant.
 *  2. It belongs to a different but known, signature-verified Microsoft broker (e.g. a second
 *     broker such as Company Portal installed alongside Authenticator).
 *
 * Any other resolver is treated as an untrusted app listening on the scheme and fails closed.
 */
object BrowserRedirectValidator {

    /**
     * Verifies that no other application is listening on the custom URL scheme defined by
     * [redirectUri]. If an untrusted application's activity is found that handles the same
     * scheme, a [ClientException] with error code
     * [ErrorStrings.MULTIPLE_APPS_LISTENING_CUSTOM_URL_SCHEME] is thrown.
     *
     * @param context      The Android context used to query the PackageManager.
     * @param redirectUri  The redirect URI whose URL scheme will be checked.
     * @param useCurrentTask Retained for API compatibility; the legitimacy of a resolver is
     *                       determined by its package (own app or known broker), not by the
     *                       expected activity class.
     * @param brokerValidator Validates whether a foreign resolver package is a known,
     *                        signature-verified broker. Injectable for testing.
     * @throws ClientException if an untrusted application is found listening on the same URL scheme.
     */
    @JvmStatic
    @JvmOverloads
    @Throws(ClientException::class)
    fun validateNoMultipleAppsListening(
        context: Context,
        redirectUri: String,
        @Suppress("UNUSED_PARAMETER") useCurrentTask: Boolean,
        brokerValidator: IBrokerValidator = BrokerValidator(context)
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

        for (resolveInfo in resolvedActivities) {
            val activityInfo = resolveInfo.activityInfo ?: continue
            val resolvedPackage = activityInfo.packageName

            // (A) Our own app's activities are never competitors. This covers the MSAL client's
            // BrowserTabActivity/CurrentTaskBrowserTabActivity and the broker's own
            // BrokerBrowserRedirectActivity (which handles both the msauth scheme and the
            // App Link host used for the broker redirect).
            if (resolvedPackage == context.packageName) {
                continue
            }

            // (B) A different but known, signature-verified Microsoft broker legitimately
            // handling the broker redirect is not an attacker; only skip when signature-verified.
            if (brokerValidator.isValidBrokerPackage(resolvedPackage)) {
                continue
            }

            // Otherwise an unknown/untrusted app is listening on the scheme — fail closed.
            throw ClientException(
                ErrorStrings.MULTIPLE_APPS_LISTENING_CUSTOM_URL_SCHEME,
                "More than one app is listening for the URL scheme defined for BrowserTabActivity " +
                    "in the AndroidManifest. The package name of this other app is: $resolvedPackage"
            )
        }
    }
}
