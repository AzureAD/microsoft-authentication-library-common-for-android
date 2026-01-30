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
import com.microsoft.identity.common.logging.Logger
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.StatusCode

/**
 * Sealed class representing the result of an Auth Tab authentication flow.
 * This provides a type-safe, clean API for handling Auth Tab results.
 */
sealed class AuthTabResult {
    /**
     * Authentication completed successfully with a result URI.
     * @param uri The redirect URI containing the authentication result
     */
    data class Success(val uri: Uri) : AuthTabResult()
    
    /**
     * User cancelled the authentication flow.
     */
    object Cancelled : AuthTabResult()
    
    /**
     * Auth Tab verification failed - redirect domain verification failed.
     * Caller should fall back to Custom Tabs.
     */
    object VerificationFailed : AuthTabResult()
    
    /**
     * Auth Tab verification timed out - redirect domain verification took too long.
     * Caller should fall back to Custom Tabs.
     */
    object VerificationTimedOut : AuthTabResult()
    
    /**
     * Auth Tab returned OK but with a null URI (unexpected state).
     */
    object SuccessWithNullUri : AuthTabResult()
    
    /**
     * Unknown result code from Auth Tab.
     * @param resultCode The raw result code
     */
    data class Unknown(val resultCode: Int) : AuthTabResult()
    
    /**
     * Convert this result to a RawAuthorizationResult for use with existing auth infrastructure.
     */
    fun toRawAuthorizationResult(): RawAuthorizationResult = when (this) {
        is Success -> RawAuthorizationResult.fromRedirectUri(uri.toString())
        is Cancelled -> RawAuthorizationResult.fromResultCode(RawAuthorizationResult.ResultCode.CANCELLED)
        is VerificationFailed -> RawAuthorizationResult.fromException(
            ClientException(
                AuthTabManager.AUTH_TAB_VERIFICATION_FAILED,
                "Auth Tab redirect domain verification failed"
            )
        )
        is VerificationTimedOut -> RawAuthorizationResult.fromException(
            ClientException(
                AuthTabManager.AUTH_TAB_VERIFICATION_TIMED_OUT,
                "Auth Tab redirect domain verification timed out"
            )
        )
        is SuccessWithNullUri -> RawAuthorizationResult.fromException(
            ClientException(
                ClientException.UNKNOWN_ERROR,
                "Auth Tab returned OK but with null result URI"
            )
        )
        is Unknown -> RawAuthorizationResult.fromException(
            ClientException(
                ClientException.UNKNOWN_ERROR,
                "Auth Tab returned unknown result code: $resultCode"
            )
        )
    }
    
    /**
     * Whether this result indicates the caller should fall back to Custom Tabs.
     * This is true for verification failures where Auth Tab couldn't complete.
     */
    val shouldFallBackToCustomTabs: Boolean
        get() = this is VerificationFailed || this is VerificationTimedOut
}

/**
 * Manager class for Auth Tab operations.
 * 
 * Auth Tab is a specialized Custom Tab designed for authentication flows,
 * available in Chrome 137+ and androidx.browser:browser:1.9.0+.
 *
 * Key benefits of Auth Tab over Custom Tabs:
 * - Simplified callback mechanism using ActivityResultLauncher
 * - Enhanced security with direct data transfer via callback
 * - Streamlined UI focused on authentication
 * - No need for BrowserTabActivity intent filters
 *
 * Usage:
 * 1. Create AuthTabManager in activity's onCreate() BEFORE super.onCreate()
 * 2. Call registerLauncher() with a result callback
 * 3. Use isLauncherRegistered to check if Auth Tab is available
 * 4. Call launch() to start authentication
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

    private var authTabIntent: AuthTabIntent? = null
    private var launcher: ActivityResultLauncher<Intent>? = null
    
    /**
     * Whether the Auth Tab launcher has been successfully registered.
     * Check this before calling launch() to determine if Auth Tab is available.
     */
    var isLauncherRegistered: Boolean = false
        private set

    /**
     * Register the Auth Tab activity result launcher with a callback.
     * 
     * IMPORTANT: This must be called before the activity reaches STARTED state,
     * typically in onCreate() before super.onCreate().
     *
     * @param onResult Callback invoked when Auth Tab completes with an [AuthTabResult]
     * @return true if registration succeeded, false otherwise
     */
    fun registerLauncher(onResult: (AuthTabResult) -> Unit): Boolean {
        val methodTag = "$TAG:registerLauncher"
        
        if (isLauncherRegistered) {
            Logger.verbose(methodTag, "Launcher already registered")
            return true
        }

        // Check if activity is already started - launcher registration will fail
        val currentState = activity.lifecycle.currentState
        if (currentState.isAtLeast(Lifecycle.State.STARTED)) {
            Logger.error(
                methodTag, 
                "Cannot register Auth Tab launcher - activity is already in state: $currentState. " +
                    "Launcher must be registered before activity reaches STARTED state.",
                null
            )
            return false
        }

        return try {
            launcher = AuthTabIntent.registerActivityResultLauncher(
                activity as ActivityResultCaller
            ) { rawResult ->
                handleAuthResult(rawResult, onResult)
            }
            authTabIntent = AuthTabIntent.Builder().build()
            isLauncherRegistered = true
            Logger.info(methodTag, "Auth Tab launcher registered successfully")
            true
        } catch (e: Exception) {
            Logger.error(methodTag, "Failed to register Auth Tab launcher", e)
            isLauncherRegistered = false
            false
        }
    }

    /**
     * Launch Auth Tab for authentication.
     * 
     * This method automatically determines the correct launch mode based on the redirect URI:
     * - For HTTPS redirects: Uses Digital Asset Links verification with host and path
     * - For custom scheme redirects (msauth, msal{clientId}, etc.): Uses scheme-based redirect
     *
     * @param authorizationUri The authorization URL to load in the browser
     * @param redirectUri The redirect URI that will receive the authentication response
     * @throws ClientException if the launcher is not registered or redirect URI is invalid
     */
    fun launch(authorizationUri: Uri, redirectUri: Uri) {
        val methodTag = "$TAG:launch"
        val span = OTelUtility.createSpanFromParent(SpanName.AuthTabLaunch.name, spanContext)

        SpanExtension.makeCurrentSpan(span).use {
            try {
                if (!isLauncherRegistered || authTabIntent == null || launcher == null) {
                    throw ClientException(
                        ClientException.UNSUPPORTED_OPERATION,
                        "Auth Tab launcher is not registered. Call registerLauncher() before the activity starts."
                    )
                }
                
                val scheme = redirectUri.scheme
                require(!scheme.isNullOrBlank()) { "Redirect URI must have a scheme" }
                require(!scheme.equals("http", ignoreCase = true)) {
                    "HTTP scheme is not allowed with Auth Tab"
                }
                
                span.setAttribute("auth_tab_redirect_scheme", scheme)
                
                if (scheme.equals("https", ignoreCase = true)) {
                    // HTTPS redirect - use host and path for Digital Asset Links verification
                    val host = redirectUri.host
                    val path = redirectUri.path
                    
                    require(!host.isNullOrBlank()) { "HTTPS redirect URI must have a host" }
                    require(!path.isNullOrBlank()) { "HTTPS redirect URI must have a path" }
                    
                    Logger.info(methodTag, "Launching Auth Tab with HTTPS redirect: $host$path")
                    span.setAttribute("auth_tab_redirect_host", host)
                    span.setAttribute("auth_tab_redirect_path", path)
                    span.setAttribute("auth_tab_uses_https", true)
                    
                    authTabIntent!!.launch(launcher!!, authorizationUri, host, path)
                } else {
                    // Custom scheme redirect (msauth, msal{clientId}, etc.)
                    Logger.info(methodTag, "Launching Auth Tab with custom scheme: $scheme")
                    span.setAttribute("auth_tab_uses_https", false)
                    
                    authTabIntent!!.launch(launcher!!, authorizationUri, scheme)
                }

                span.setStatus(StatusCode.OK)
            } catch (e: Exception) {
                Logger.error(methodTag, "Failed to launch Auth Tab", e)
                span.setStatus(StatusCode.ERROR, e.message ?: "Unknown error")
                span.recordException(e)
                throw if (e is ClientException) e else ClientException(
                    ClientException.UNKNOWN_ERROR,
                    "Failed to launch Auth Tab: ${e.message}",
                    e
                )
            } finally {
                span.end()
            }
        }
    }

    /**
     * Internal handler for Auth Tab results.
     * Converts raw result to [AuthTabResult], adds telemetry, and invokes the callback.
     */
    private fun handleAuthResult(
        rawResult: AuthTabIntent.AuthResult,
        onResult: (AuthTabResult) -> Unit
    ) {
        val methodTag = "$TAG:handleAuthResult"
        val span = OTelUtility.createSpanFromParent(SpanName.AuthTabResult.name, spanContext)

        SpanExtension.makeCurrentSpan(span).use {
            try {
                span.setAttribute("auth_tab_result_code", rawResult.resultCode.toLong())

                val result: AuthTabResult = when (rawResult.resultCode) {
                    AuthTabIntent.RESULT_OK -> {
                        val uri = rawResult.resultUri
                        if (uri != null) {
                            Logger.info(methodTag, "Auth Tab completed successfully")
                            span.setAttribute("auth_tab_has_result_uri", true)
                            AuthTabResult.Success(uri)
                        } else {
                            Logger.warn(methodTag, "Auth Tab returned OK but with null URI")
                            span.setAttribute("auth_tab_has_result_uri", false)
                            AuthTabResult.SuccessWithNullUri
                        }
                    }
                    AuthTabIntent.RESULT_CANCELED -> {
                        Logger.info(methodTag, "Auth Tab was cancelled by user")
                        AuthTabResult.Cancelled
                    }
                    AuthTabIntent.RESULT_VERIFICATION_FAILED -> {
                        Logger.warn(methodTag, "Auth Tab verification failed - caller should fall back to Custom Tabs")
                        AuthTabResult.VerificationFailed
                    }
                    AuthTabIntent.RESULT_VERIFICATION_TIMED_OUT -> {
                        Logger.warn(methodTag, "Auth Tab verification timed out - caller should fall back to Custom Tabs")
                        AuthTabResult.VerificationTimedOut
                    }
                    else -> {
                        Logger.warn(methodTag, "Auth Tab returned unknown result code: ${rawResult.resultCode}")
                        AuthTabResult.Unknown(rawResult.resultCode)
                    }
                }

                span.setStatus(StatusCode.OK)
                onResult(result)
            } catch (e: Exception) {
                Logger.error(methodTag, "Error handling Auth Tab result", e)
                span.setStatus(StatusCode.ERROR, e.message ?: "Unknown error")
                span.recordException(e)
                // Still invoke callback with unknown result so caller can handle
                onResult(AuthTabResult.Unknown(-1))
            } finally {
                span.end()
            }
        }
    }
}
