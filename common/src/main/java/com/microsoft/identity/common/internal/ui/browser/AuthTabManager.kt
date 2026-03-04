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
import com.microsoft.identity.common.logging.Logger

/**
 * Encapsulates all AuthTab API interactions.
 *
 * AuthTab is a Chrome 137+ API (requires androidx.browser:browser:1.9.0+) that delivers
 * authentication results via an in-process [ActivityResultLauncher] callback instead of
 * intent-redirect chains.
 *
 * Each fragment instance should create its own [AuthTabManager]. Do NOT share instances
 * across fragments.
 *
 * [registerLauncher] MUST be called on the main thread before the fragment reaches the
 * CREATED state (i.e., in [androidx.fragment.app.Fragment.onCreate] or earlier).
 */
class AuthTabManager {

    companion object {
        private val TAG = AuthTabManager::class.java.simpleName

        /**
         * Returns true if AuthTab is supported on this device/browser combination.
         *
         * Thread-safe; may be called from any thread.
         *
         * @param context Android context used to query browser capabilities.
         * @return true if [AuthTabIntent] can be used, false otherwise.
         */
        @JvmStatic
        fun isAuthTabSupported(context: Context): Boolean {
            return try {
                CustomTabsClient.isAuthTabSupported(context)
            } catch (e: Exception) {
                Logger.warn(TAG, "isAuthTabSupported check failed.")
                false
            }
        }
    }

    /** Holds the registered [ActivityResultLauncher]. Set by [registerLauncher]. */
    private var mLauncher: ActivityResultLauncher<Intent>? = null

    /**
     * Registers the [ActivityResultLauncher] that AuthTab will use to deliver results.
     *
     * Must be called on the main thread before the owning fragment reaches the CREATED state.
     *
     * @param caller   The [ActivityResultCaller] (fragment or activity) to register with.
     * @param onResult Callback invoked with the mapped [RawAuthorizationResult] when the
     *                 AuthTab session concludes.
     * @return this, for chaining.
     */
    fun registerLauncher(
        caller: ActivityResultCaller,
        onResult: (RawAuthorizationResult) -> Unit
    ): AuthTabManager {
        val methodTag = "$TAG:registerLauncher"
        Logger.verbose(methodTag, "Registering AuthTab activity result launcher.")
        mLauncher = AuthTabIntent.registerActivityResultLauncher(caller) { authResult ->
            onResult(mapAuthResultToRawResult(authResult))
        }
        return this
    }

    /**
     * Launches an AuthTab session using a custom-scheme redirect URI.
     *
     * [registerLauncher] must be called before invoking this method.
     *
     * @param authUrl        The authorization endpoint URL to load.
     * @param redirectScheme The custom URL scheme that the server will redirect to upon completion.
     * @throws IllegalStateException if [registerLauncher] has not been called.
     */
    fun launch(authUrl: Uri, redirectScheme: String) {
        val launcher = mLauncher
            ?: throw IllegalStateException(
                "AuthTab launcher is not registered. Call registerLauncher() before launch()."
            )
        AuthTabIntent.Builder().build().launch(launcher, authUrl, redirectScheme)
    }

    /**
     * Launches an AuthTab session using an HTTPS redirect URI.
     *
     * [registerLauncher] must be called before invoking this method.
     *
     * @param authUrl        The authorization endpoint URL to load.
     * @param redirectHost   The HTTPS host that the server will redirect to upon completion.
     * @param redirectPath   The HTTPS path that the server will redirect to upon completion.
     * @throws IllegalStateException if [registerLauncher] has not been called.
     */
    fun launchWithHttpsRedirect(authUrl: Uri, redirectHost: String, redirectPath: String) {
        val launcher = mLauncher
            ?: throw IllegalStateException(
                "AuthTab launcher is not registered. Call registerLauncher() before launchWithHttpsRedirect()."
            )
        AuthTabIntent.Builder().build().launchWithHttpsRedirect(launcher, authUrl, redirectHost, redirectPath)
    }

    /**
     * Maps an [AuthTabIntent.AuthResult] to a [RawAuthorizationResult].
     *
     * @param authResult The raw result returned by the AuthTab activity.
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
                            "AuthTab returned RESULT_OK but no redirect URI was received."
                        )
                    )
                }
            }
            AuthTabIntent.RESULT_CANCELED ->
                RawAuthorizationResult.fromResultCode(RawAuthorizationResult.ResultCode.CANCELLED)
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
