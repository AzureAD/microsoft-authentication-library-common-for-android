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

import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.browser.auth.AuthTabIntent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.opentelemetry.OTelUtility
import com.microsoft.identity.common.java.opentelemetry.SpanExtension
import com.microsoft.identity.common.java.opentelemetry.SpanName
import com.microsoft.identity.common.java.providers.RawAuthorizationResult
import com.microsoft.identity.common.java.util.ResultFuture
import com.microsoft.identity.common.logging.Logger
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.StatusCode
import java.util.concurrent.Future

/**
 * Manager class for Auth Tab operations.
 * Auth Tab is a specialized Custom Tab designed for authentication flows,
 * available in Chrome 137+ and androidx.browser:browser:1.9.0+.
 *
 * Key benefits of Auth Tab over Custom Tabs:
 * - Simplified callback mechanism using ActivityResultLauncher
 * - Enhanced security with direct data transfer via callback
 * - Streamlined UI focused on authentication
 * - No need for BrowserTabActivity intent filters
 *
 * @param activity The FragmentActivity that will host the Auth Tab
 * @param spanContext Optional OpenTelemetry span context for tracing
 */
class AuthTabManager(
    private val activity: FragmentActivity,
    private val spanContext: SpanContext? = null
) {
    companion object {
        private const val TAG = "AuthTabManager"

        /**
         * Error code for Auth Tab verification failure.
         * This occurs when the redirect domain verification fails.
         */
        const val AUTH_TAB_VERIFICATION_FAILED = "auth_tab_verification_failed"

        /**
         * Error code for Auth Tab verification timeout.
         * This occurs when the redirect domain verification times out.
         */
        const val AUTH_TAB_VERIFICATION_TIMED_OUT = "auth_tab_verification_timed_out"
    }

    private var resultFuture: ResultFuture<RawAuthorizationResult>? = null
    private var launcher: ActivityResultLauncher<Intent>? = null
    private var isLauncherRegistered = false

    /**
     * Register the Auth Tab activity result launcher.
     * This must be called before the activity is started (e.g., in onCreate).
     *
     * Note: The launcher must be registered before the activity reaches STARTED state.
     * @throws ClientException if the activity is already started or registration fails
     */
    fun registerLauncher() {
        val methodTag = "$TAG:registerLauncher"
        if (isLauncherRegistered) {
            Logger.verbose(methodTag, "Launcher already registered")
            return
        }

        // Check if activity is already started - launcher registration will fail
        val currentState = activity.lifecycle.currentState
        if (currentState.isAtLeast(Lifecycle.State.STARTED)) {
            val errorMsg = "Cannot register Auth Tab launcher - activity is already in state: $currentState. " +
                    "Launcher must be registered before activity reaches STARTED state."
            Logger.error(methodTag, errorMsg, null)
            throw ClientException(
                ClientException.UNSUPPORTED_OPERATION,
                errorMsg
            )
        }

        try {
            launcher = AuthTabIntent.registerActivityResultLauncher(
                activity as ActivityResultCaller
            ) { result ->
                handleAuthResult(result)
            }
            isLauncherRegistered = true
            Logger.info(methodTag, "Auth Tab launcher registered successfully")
        } catch (e: Exception) {
            Logger.error(methodTag, "Failed to register Auth Tab launcher", e)
            throw ClientException(
                ClientException.UNSUPPORTED_OPERATION,
                "Failed to register Auth Tab launcher: ${e.message}",
                e
            )
        }
    }

    /**
     * Launch Auth Tab with a custom redirect scheme.
     *
     * @param authorizationUri The authorization URL to load
     * @param redirectScheme The custom redirect scheme (e.g., "msauth" or "msal{client_id}")
     * @return A Future that will contain the RawAuthorizationResult
     * @throws ClientException if the launcher is not registered or redirect scheme is invalid
     */
    fun launchAuthTab(
        authorizationUri: Uri,
        redirectScheme: String
    ): Future<RawAuthorizationResult> {
        val methodTag = "$TAG:launchAuthTab"
        val span = OTelUtility.createSpanFromParent(SpanName.AuthTabLaunch.name, spanContext)

        return SpanExtension.makeCurrentSpan(span).use {
            try {
                validateLauncher()
                validateRedirectScheme(redirectScheme)

                resultFuture = ResultFuture()

                Logger.info(methodTag, "Launching Auth Tab with custom scheme: $redirectScheme")
                span.setAttribute("auth_tab_redirect_scheme", redirectScheme)

                val authTabIntent = AuthTabIntent.Builder().build()
                authTabIntent.launch(launcher!!, authorizationUri, redirectScheme)

                span.setStatus(StatusCode.OK)
                resultFuture!!
            } catch (e: Exception) {
                Logger.error(methodTag, "Failed to launch Auth Tab", e)
                span.setStatus(StatusCode.ERROR, e.message ?: "Unknown error")
                span.recordException(e)

                val exception = if (e is ClientException) e else ClientException(
                    ClientException.UNKNOWN_ERROR,
                    "Failed to launch Auth Tab: ${e.message}",
                    e
                )
                resultFuture = ResultFuture()
                resultFuture!!.setResult(RawAuthorizationResult.fromException(exception))
                resultFuture!!
            } finally {
                span.end()
            }
        }
    }

    /**
     * Launch Auth Tab with HTTPS redirect (requires Digital Asset Links verification).
     *
     * @param authorizationUri The authorization URL to load
     * @param redirectHost The redirect host (e.g., "login.microsoftonline.com")
     * @param redirectPath The redirect path (e.g., "/common/oauth2/nativeclient")
     * @return A Future that will contain the RawAuthorizationResult
     * @throws ClientException if the launcher is not registered or parameters are invalid
     */
    fun launchAuthTabHttps(
        authorizationUri: Uri,
        redirectHost: String,
        redirectPath: String
    ): Future<RawAuthorizationResult> {
        val methodTag = "$TAG:launchAuthTabHttps"
        val span = OTelUtility.createSpanFromParent(SpanName.AuthTabLaunch.name, spanContext)

        return SpanExtension.makeCurrentSpan(span).use {
            try {
                validateLauncher()
                require(redirectHost.isNotBlank()) { "Redirect host cannot be blank" }
                require(redirectPath.isNotBlank()) { "Redirect path cannot be blank" }

                resultFuture = ResultFuture()

                Logger.info(methodTag, "Launching Auth Tab with HTTPS redirect: $redirectHost$redirectPath")
                span.setAttribute("auth_tab_redirect_host", redirectHost)
                span.setAttribute("auth_tab_redirect_path", redirectPath)
                span.setAttribute("auth_tab_uses_https", true)

                val authTabIntent = AuthTabIntent.Builder().build()
                authTabIntent.launch(launcher!!, authorizationUri, redirectHost, redirectPath)

                span.setStatus(StatusCode.OK)
                resultFuture!!
            } catch (e: Exception) {
                Logger.error(methodTag, "Failed to launch Auth Tab with HTTPS", e)
                span.setStatus(StatusCode.ERROR, e.message ?: "Unknown error")
                span.recordException(e)

                val exception = if (e is ClientException) e else ClientException(
                    ClientException.UNKNOWN_ERROR,
                    "Failed to launch Auth Tab: ${e.message}",
                    e
                )
                resultFuture = ResultFuture()
                resultFuture!!.setResult(RawAuthorizationResult.fromException(exception))
                resultFuture!!
            } finally {
                span.end()
            }
        }
    }

    /**
     * Handle the Auth Tab result callback.
     * Converts the AuthResult to RawAuthorizationResult and completes the future.
     */
    private fun handleAuthResult(result: AuthTabIntent.AuthResult) {
        val methodTag = "$TAG:handleAuthResult"
        val span = OTelUtility.createSpanFromParent(SpanName.AuthTabResult.name, spanContext)

        SpanExtension.makeCurrentSpan(span).use {
            try {
                span.setAttribute("auth_tab_result_code", result.resultCode.toLong())

                val rawResult = when (result.resultCode) {
                    AuthTabIntent.RESULT_OK -> {
                        Logger.info(methodTag, "Auth Tab completed successfully")
                        val uri = result.resultUri
                        if (uri != null) {
                            span.setAttribute("auth_tab_has_result_uri", true)
                            RawAuthorizationResult.fromRedirectUri(uri.toString())
                        } else {
                            Logger.warn(methodTag, "Auth Tab returned OK but with null URI")
                            span.setAttribute("auth_tab_has_result_uri", false)
                            RawAuthorizationResult.fromException(
                                ClientException(
                                    ClientException.UNKNOWN_ERROR,
                                    "Auth Tab returned OK but with null result URI"
                                )
                            )
                        }
                    }

                    AuthTabIntent.RESULT_CANCELED -> {
                        Logger.info(methodTag, "Auth Tab was cancelled by user")
                        RawAuthorizationResult.fromResultCode(RawAuthorizationResult.ResultCode.CANCELLED)
                    }

                    AuthTabIntent.RESULT_VERIFICATION_FAILED -> {
                        Logger.warn(methodTag, "Auth Tab verification failed")
                        RawAuthorizationResult.fromException(
                            ClientException(
                                AUTH_TAB_VERIFICATION_FAILED,
                                "Auth Tab redirect domain verification failed"
                            )
                        )
                    }

                    AuthTabIntent.RESULT_VERIFICATION_TIMED_OUT -> {
                        Logger.warn(methodTag, "Auth Tab verification timed out")
                        RawAuthorizationResult.fromException(
                            ClientException(
                                AUTH_TAB_VERIFICATION_TIMED_OUT,
                                "Auth Tab redirect domain verification timed out"
                            )
                        )
                    }

                    else -> {
                        Logger.warn(methodTag, "Auth Tab returned unknown result code: ${result.resultCode}")
                        RawAuthorizationResult.fromException(
                            ClientException(
                                ClientException.UNKNOWN_ERROR,
                                "Auth Tab returned unknown result code: ${result.resultCode}"
                            )
                        )
                    }
                }

                resultFuture?.setResult(rawResult)
                span.setStatus(StatusCode.OK)
            } catch (e: Exception) {
                Logger.error(methodTag, "Error handling Auth Tab result", e)
                span.setStatus(StatusCode.ERROR, e.message ?: "Unknown error")
                span.recordException(e)

                resultFuture?.setResult(
                    RawAuthorizationResult.fromException(
                        ClientException(
                            ClientException.UNKNOWN_ERROR,
                            "Error handling Auth Tab result: ${e.message}",
                            e
                        )
                    )
                )
            } finally {
                span.end()
            }
        }
    }

    private fun validateLauncher() {
        if (!isLauncherRegistered || launcher == null) {
            throw ClientException(
                ClientException.UNSUPPORTED_OPERATION,
                "Auth Tab launcher is not registered. Call registerLauncher() before the activity starts."
            )
        }
    }

    private fun validateRedirectScheme(redirectScheme: String) {
        require(redirectScheme.isNotBlank()) { "Redirect scheme cannot be blank" }
        require(!redirectScheme.equals("http", ignoreCase = true)) {
            "HTTP scheme is not allowed with Auth Tab"
        }
    }
}
