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

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.browser.auth.AuthTabIntent
import com.microsoft.identity.common.logging.Logger

/**
 * Manager class for launching AuthTab authentication flows.
 *
 * AuthTab is a specialized Custom Tab for authentication, available in Chrome 137+ and
 * AndroidX Browser 1.9.0+. It provides a secure, minimal UI dedicated to authentication,
 * delivering the result (redirect URI) directly to the app without requiring intent filters
 * or redirect-capture activities.
 *
 * Usage:
 * 1. Call [registerLauncher] in your [ComponentActivity.onCreate] (before the activity reaches
 *    the STARTED state).
 * 2. Call [launch] to start the authentication flow when ready.
 */
class AuthTabManager {

    companion object {
        private const val TAG = "AuthTabManager"
    }

    private var mLauncher: ActivityResultLauncher<Uri>? = null

    /**
     * Registers the [AuthTabIntent] activity result launcher with the given activity.
     *
     * This must be called before the activity reaches the STARTED state (i.e., in
     * [ComponentActivity.onCreate]).
     *
     * @param activity The [ComponentActivity] that will host the AuthTab flow.
     * @param callback The callback invoked with the redirect [Uri] when the auth flow completes,
     *                 or `null` if the flow was cancelled.
     */
    fun registerLauncher(
        activity: ComponentActivity,
        callback: ActivityResultCallback<Uri>
    ) {
        val methodTag = "$TAG:registerLauncher"
        Logger.info(methodTag, "Registering AuthTab activity result launcher.")
        mLauncher = AuthTabIntent.registerActivityResultLauncher(activity, callback)
    }

    /**
     * Launches an AuthTab authentication session for the given authorization [uri].
     *
     * [registerLauncher] must be called before invoking this method.
     *
     * @param uri The authorization endpoint [Uri] to load in the AuthTab.
     * @throws IllegalStateException if [registerLauncher] has not been called prior to this call.
     */
    fun launch(uri: Uri) {
        val methodTag = "$TAG:launch"
        val launcher = requireNotNull(mLauncher) {
            "AuthTabManager.launch() called before registerLauncher(). " +
                "Call registerLauncher() in Activity.onCreate() first."
        }
        Logger.info(methodTag, "Launching AuthTab.")
        AuthTabIntent.Builder().build().launch(launcher, uri)
    }
}
