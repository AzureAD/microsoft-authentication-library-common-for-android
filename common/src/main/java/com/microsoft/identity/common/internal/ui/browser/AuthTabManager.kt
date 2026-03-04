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
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.browser.customtabs.AuthTabIntent
import androidx.browser.customtabs.CustomTabsClient
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.providers.RawAuthorizationResult
import com.microsoft.identity.common.java.providers.RawAuthorizationResult.ResultCode
import com.microsoft.identity.common.logging.Logger

/**
 * Encapsulates all AuthTab API interactions.
 *
 * AuthTab is a Chrome 137+ API (androidx.browser:browser:1.9.0) that delivers
 * authentication results via an in-process ActivityResultLauncher callback instead
 * of intent-redirect chains.
 *
 * Do NOT store a Context reference as a field. Each fragment instance should
 * create its own AuthTabManager. MUST call [registerLauncher] on the main thread
 * before the fragment reaches the CREATED state.
 */
class AuthTabManager {

    private var mLauncher: ActivityResultLauncher<Intent>? = null

    companion object {
        private val TAG = AuthTabManager::class.java.simpleName

        /**
         * Checks whether the AuthTab API is supported on this device.
         *
         * Thread-safe; can be called from any thread.
         *
         * @param context Android application context.
         * @return true if AuthTab is supported, false otherwise.
         */
        @JvmStatic
        fun isAuthTabSupported(context: Context): Boolean {
            val methodTag = "$TAG:isAuthTabSupported"
            return try {
                CustomTabsClient.isAuthTabSupported(context)
            } catch (e: Exception) {
                Logger.warn(methodTag, "Failed to check AuthTab support: ${e.message}")
                false
            }
        }
    }

    /**
     * Registers the ActivityResultLauncher for AuthTab results.
     *
     * MUST be called on the main thread before the fragment reaches the CREATED state.
     *
     * @param caller   The ActivityResultCaller (Fragment or Activity) to register with.
     * @param onResult Callback invoked with a [RawAuthorizationResult] when the auth flow completes.
     * @return this instance for chaining.
     */
    fun registerLauncher(
        caller: ActivityResultCaller,
        onResult: (RawAuthorizationResult) -> Unit
    ): AuthTabManager {
        val methodTag = "$TAG:registerLauncher"
        mLauncher = AuthTabIntent.registerActivityResultLauncher(caller) { authResult ->
            onResult(mapAuthResultToRawResult(authResult))
        }
        Logger.info(methodTag, "AuthTab launcher registered.")
        return this
    }

    /**
     * Launches the AuthTab with a custom-scheme redirect URI.
     *
     * @param authUrl        The authorization URL to load.
     * @param redirectScheme The custom URI scheme used in the redirect URI.
     * @throws IllegalStateException if [registerLauncher] has not been called.
     */
    fun launch(authUrl: Uri, redirectScheme: String) {
        val launcher = mLauncher
            ?: throw IllegalStateException("AuthTabManager: launcher is not registered. Call registerLauncher() before launch().")
        val authTabIntent = AuthTabIntent.Builder().build()
        authTabIntent.launch(launcher, authUrl, redirectScheme)
    }

    /**
     * Launches the AuthTab with an HTTPS redirect URI verified via Digital Asset Links.
     *
     * @param authUrl        The authorization URL to load.
     * @param redirectHost   The host component of the HTTPS redirect URI.
     * @param redirectPath   The path component of the HTTPS redirect URI.
     * @throws IllegalStateException if [registerLauncher] has not been called.
     */
    fun launchWithHttpsRedirect(authUrl: Uri, redirectHost: String, redirectPath: String) {
        val launcher = mLauncher
            ?: throw IllegalStateException("AuthTabManager: launcher is not registered. Call registerLauncher() before launchWithHttpsRedirect().")
        val authTabIntent = AuthTabIntent.Builder().build()
        authTabIntent.launch(launcher, authUrl, redirectHost, redirectPath)
    }

    /**
     * Maps an [AuthTabIntent.AuthResult] to a [RawAuthorizationResult].
     *
     * @param authResult The result returned from the AuthTab activity.
     * @return A [RawAuthorizationResult] representing the outcome.
     */
    fun mapAuthResultToRawResult(authResult: AuthTabIntent.AuthResult): RawAuthorizationResult {
        return when (authResult.resultCode) {
            AuthTabIntent.RESULT_OK -> {
                val uri = authResult.resultUri
                if (uri != null) {
                    RawAuthorizationResult.fromRedirectUri(uri.toString())
                } else {
                    RawAuthorizationResult.fromException(
                        ClientException(
                            "authorization_result_not_found",
                            "AuthTab returned RESULT_OK but resultUri was null."
                        )
                    )
                }
            }
            AuthTabIntent.RESULT_CANCELED ->
                RawAuthorizationResult.fromResultCode(ResultCode.CANCELLED)
            AuthTabIntent.RESULT_VERIFICATION_FAILED ->
                RawAuthorizationResult.fromException(
                    ClientException(
                        "auth_tab_verification_failed",
                        "AuthTab verification failed."
                    )
                )
            AuthTabIntent.RESULT_VERIFICATION_TIMED_OUT ->
                RawAuthorizationResult.fromException(
                    ClientException(
                        "auth_tab_verification_timed_out",
                        "AuthTab verification timed out."
                    )
                )
            else ->
                RawAuthorizationResult.fromException(
                    ClientException(
                        "auth_tab_unknown_result",
                        "AuthTab returned an unknown result code: ${authResult.resultCode}"
                    )
                )
        }
    }
}
