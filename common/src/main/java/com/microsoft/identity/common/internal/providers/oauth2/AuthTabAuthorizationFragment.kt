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

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.browser.auth.AuthTabIntent
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REDIRECT_URI
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REQUEST_URL
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.exception.ErrorStrings
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.opentelemetry.SpanExtension
import com.microsoft.identity.common.java.providers.RawAuthorizationResult
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAuthorizationErrorResponse
import com.microsoft.identity.common.logging.Logger

/**
 * Authorization fragment that uses Chrome AuthTab (available in Chrome 137+ via
 * androidx.browser:browser:1.9.0) for browser-based auth flows.
 *
 * AuthTab returns results via [ActivityResultCallback] instead of intent-based redirects,
 * improving security and simplifying the flow. This fragment falls back gracefully if the
 * browser does not support AuthTab.
 */
class AuthTabAuthorizationFragment : AuthorizationFragment() {

    private val TAG = AuthTabAuthorizationFragment::class.java.simpleName

    companion object {
        private const val AUTH_FLOW_STARTED = "authTabFlowStarted"
    }

    private var mAuthFlowStarted = false
    private var mRequestUrl: String? = null
    private var mRedirectUri: String? = null
    private var mAuthTabLauncher: ActivityResultLauncher<Uri>? = null

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        // Register launcher BEFORE the STARTED state (must be called in onCreate)
        mAuthTabLauncher = AuthTabIntent.registerActivityResultLauncher(this) { authResult ->
            handleAuthResult(authResult)
        }
        super.onCreate(savedInstanceState)
    }

    override fun onSaveInstanceState(@NonNull outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(AUTH_FLOW_STARTED, mAuthFlowStarted)
    }

    override fun extractState(@NonNull state: Bundle) {
        super.extractState(state)
        mRequestUrl = state.getString(REQUEST_URL)
        mRedirectUri = state.getString(REDIRECT_URI)
        mAuthFlowStarted = state.getBoolean(AUTH_FLOW_STARTED, false)
    }

    override fun onResume() {
        super.onResume()
        val methodTag = "$TAG:onResume"

        if (mAuthFlowStarted) {
            // Flow already started; if we end up back here it means the user cancelled
            // (AuthTab result callback was not triggered — e.g., user pressed back).
            Logger.info(methodTag, "AuthTab flow already started on resume; treating as cancellation.")
            cancelAuthorization(true)
            return
        }

        val requestUrl = mRequestUrl
        val redirectUri = mRedirectUri

        if (requestUrl.isNullOrBlank()) {
            Logger.error(methodTag, "Request URL is null or blank. Cannot launch AuthTab.", null)
            sendResult(RawAuthorizationResult.fromException(
                ClientException(ErrorStrings.AUTHORIZATION_INTENT_IS_NULL, "Request URL is null")))
            finish()
            return
        }

        val launcher = mAuthTabLauncher
        if (launcher == null) {
            Logger.error(methodTag, "ActivityResultLauncher is null. Cannot launch AuthTab.", null)
            sendResult(RawAuthorizationResult.fromException(
                ClientException(ErrorStrings.AUTHORIZATION_INTENT_IS_NULL, "AuthTab launcher is null")))
            finish()
            return
        }

        val redirectScheme = if (!redirectUri.isNullOrBlank()) {
            Uri.parse(redirectUri).scheme ?: ""
        } else {
            ""
        }

        Logger.info(methodTag, "Launching AuthTab for URL: $requestUrl with redirect scheme: $redirectScheme")
        mAuthFlowStarted = true

        val authTabIntent = AuthTabIntent.Builder().build()
        authTabIntent.launch(launcher, Uri.parse(requestUrl), redirectScheme)

        val span = SpanExtension.current()
        span.setAttribute(AttributeName.is_auth_tab_used.name(), true)
        span.setAttribute(AttributeName.auth_tab_supported.name(), true)
    }

    private fun handleAuthResult(authResult: AuthTabIntent.AuthResult) {
        val methodTag = "$TAG:handleAuthResult"
        Logger.info(methodTag, "AuthTab result received: ${authResult.resultCode}")

        SpanExtension.current().setAttribute(AttributeName.auth_tab_result_code.name(), authResult.resultCode.toLong())

        when (authResult.resultCode) {
            AuthTabIntent.AuthResult.RESULT_OK -> {
                val resultUri = authResult.resultUri
                if (resultUri != null) {
                    Logger.info(methodTag, "AuthTab completed successfully.")
                    sendResult(RawAuthorizationResult.fromRedirectUri(resultUri.toString()))
                } else {
                    Logger.error(methodTag, "AuthTab RESULT_OK but resultUri is null.", null)
                    sendResult(RawAuthorizationResult.fromException(
                        ClientException(MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED,
                            "AuthTab returned OK but resultUri is null")))
                }
                finish()
            }
            AuthTabIntent.AuthResult.RESULT_CANCELED -> {
                Logger.info(methodTag, "AuthTab was cancelled by the user.")
                cancelAuthorization(true)
            }
            AuthTabIntent.AuthResult.RESULT_VERIFICATION_FAILED -> {
                Logger.error(methodTag, "AuthTab verification failed.", null)
                sendResult(RawAuthorizationResult.fromException(
                    ClientException(MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED,
                        "AuthTab verification failed")))
                finish()
            }
            AuthTabIntent.AuthResult.RESULT_VERIFICATION_TIMED_OUT -> {
                Logger.error(methodTag, "AuthTab verification timed out.", null)
                sendResult(RawAuthorizationResult.fromException(
                    ClientException(MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED,
                        "AuthTab verification timed out")))
                finish()
            }
            else -> {
                Logger.warn(methodTag, "AuthTab returned unknown result code: ${authResult.resultCode}")
                cancelAuthorization(false)
            }
        }
    }
}
