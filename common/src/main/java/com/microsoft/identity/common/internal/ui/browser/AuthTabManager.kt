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
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.browser.auth.AuthTabIntent
import androidx.browser.customtabs.CustomTabsClient
import androidx.fragment.app.FragmentActivity
import com.microsoft.identity.common.logging.Logger

/**
 * Wraps the AndroidX Browser 1.9.0 [AuthTabIntent] API to provide a cleaner interface for
 * launching Auth Tabs and receiving typed results in the switch-browser authentication flow.
 *
 * **Lifecycle contract**: [registerLauncher] MUST be called inside [FragmentActivity.onCreate]
 * (before the activity reaches the STARTED state), exactly once per instance.  Call [unregister]
 * in [FragmentActivity.onDestroy] to release references.
 */
class AuthTabManager {

    /**
     * Typed result of an Auth Tab operation.
     */
    sealed class AuthTabResult {
        /** Authentication completed successfully and the redirect URI was received. */
        data class Success(val resultUri: Uri) : AuthTabResult()

        /** User dismissed or cancelled the Auth Tab. */
        object Canceled : AuthTabResult()

        /** The Auth Tab redirect verification failed. */
        object VerificationFailed : AuthTabResult()

        /** The Auth Tab redirect verification timed out. */
        object VerificationTimedOut : AuthTabResult()
    }

    private var launcher: ActivityResultLauncher<Intent>? = null

    companion object {
        private val TAG: String = AuthTabManager::class.java.simpleName

        /**
         * Returns `true` if the browser identified by [browserPackage] supports Auth Tab on the
         * given [context].  Returns `false` on any exception to avoid crashing the caller.
         *
         * @param context        Android context used to query the browser.
         * @param browserPackage Package name of the browser to check.
         */
        fun isSupported(context: Context, browserPackage: String): Boolean {
            return try {
                CustomTabsClient.isAuthTabSupported(context, browserPackage)
            } catch (e: Exception) {
                Logger.warn(
                    "$TAG:isSupported",
                    "Exception checking Auth Tab support for $browserPackage: ${e.message}"
                )
                false
            }
        }
    }

    /**
     * Registers the [AuthTabIntent] activity-result launcher with [activity].
     *
     * Must be called in [FragmentActivity.onCreate] before the activity is STARTED.
     * The supplied [callback] is invoked on the main thread when the Auth Tab returns a result.
     *
     * @param activity The hosting [FragmentActivity].
     * @param callback Receives the typed [AuthTabResult] when Auth Tab completes.
     */
    fun registerLauncher(activity: FragmentActivity, callback: (AuthTabResult) -> Unit) {
        launcher = AuthTabIntent.registerActivityResultLauncher(activity) { resultCode, resultUri ->
            val result = when (resultCode) {
                AuthTabIntent.RESULT_OK ->
                    if (resultUri != null) AuthTabResult.Success(resultUri)
                    else AuthTabResult.VerificationFailed
                AuthTabIntent.RESULT_CANCELED -> AuthTabResult.Canceled
                AuthTabIntent.RESULT_VERIFICATION_FAILED -> AuthTabResult.VerificationFailed
                AuthTabIntent.RESULT_VERIFICATION_TIMED_OUT -> AuthTabResult.VerificationTimedOut
                else -> AuthTabResult.Canceled
            }
            callback(result)
        }
    }

    /**
     * Builds an [AuthTabIntent] and launches it with the registered launcher.
     *
     * @param uri            The URI to open in the Auth Tab.
     * @param redirectScheme The URI scheme used for the redirect back to the app.
     * @throws IllegalStateException if [registerLauncher] has not been called.
     */
    fun launch(uri: Uri, redirectScheme: String) {
        val activeLauncher = requireNotNull(launcher) {
            "AuthTabManager.launch() called before registerLauncher()"
        }
        val authTabIntent = AuthTabIntent.Builder().build()
        authTabIntent.launch(activeLauncher, uri, redirectScheme)
    }

    /**
     * Unregisters the launcher and releases all held references.
     * Safe to call multiple times.
     */
    fun unregister() {
        launcher = null
    }
}
