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
import androidx.browser.auth.AuthTabIntent
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REDIRECT_URI
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REQUEST_URL
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.exception.ErrorStrings
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.opentelemetry.OTelUtility
import com.microsoft.identity.common.java.opentelemetry.SpanExtension
import com.microsoft.identity.common.java.opentelemetry.SpanName
import com.microsoft.identity.common.java.providers.RawAuthorizationResult
import com.microsoft.identity.common.logging.Logger
import io.opentelemetry.api.trace.StatusCode

/**
 * Authorization fragment that uses AuthTab (Chrome 137+) for browser-based authentication flows.
 *
 * AuthTab returns results via [ActivityResultLauncher] instead of intent-based redirects,
 * improving security and simplifying the flow. Falls back to [BrowserAuthorizationFragment]
 * behavior is not handled here; the routing decision happens in [AuthorizationActivityFactory].
 *
 * The [ActivityResultLauncher] must be registered in [onCreate] before the STARTED lifecycle state.
 */
class AuthTabAuthorizationFragment : AuthorizationFragment() {

    companion object {
        private val TAG: String = AuthTabAuthorizationFragment::class.java.simpleName
        private const val AUTH_FLOW_STARTED = "authFlowStarted"
    }

    private var requestUrl: String? = null
    private var redirectUri: String? = null
    private var authFlowStarted = false

    private lateinit var authTabLauncher: ActivityResultLauncher<AuthTabIntent>

    override fun onCreate(savedInstanceState: Bundle?) {
        // Register the ActivityResultLauncher BEFORE calling super.onCreate(),
        // which transitions to STARTED state. This is required by the Activity Result API.
        authTabLauncher = AuthTabIntent.registerActivityResultLauncher(this, ::handleAuthResult)
        super.onCreate(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(REQUEST_URL, requestUrl)
        outState.putString(REDIRECT_URI, redirectUri)
        outState.putBoolean(AUTH_FLOW_STARTED, authFlowStarted)
    }

    override fun extractState(state: Bundle) {
        super.extractState(state)
        requestUrl = state.getString(REQUEST_URL)
        redirectUri = state.getString(REDIRECT_URI)
        authFlowStarted = state.getBoolean(AUTH_FLOW_STARTED, false)
    }

    override fun onResume() {
        super.onResume()
        val methodTag = "$TAG:onResume"

        if (authFlowStarted) {
            // The fragment resumed after returning from AuthTab without a result
            // (e.g., user pressed back). Treat this as user cancellation.
            Logger.info(methodTag, "AuthTab flow already started and resumed without result - treating as cancellation.")
            cancelAuthorization(true)
            return
        }

        val url = requestUrl
        val redirect = redirectUri
        if (url.isNullOrBlank() || redirect.isNullOrBlank()) {
            Logger.error(methodTag, "Missing requestUrl or redirectUri - Cannot launch AuthTab.", null)
            sendResult(RawAuthorizationResult.fromException(
                ClientException(ErrorStrings.UNKNOWN_ERROR, "Missing requestUrl or redirectUri for AuthTab flow.")
            ))
            finish()
            return
        }

        val scheme = Uri.parse(redirect).scheme
        if (scheme.isNullOrBlank()) {
            Logger.error(methodTag, "Could not extract redirect scheme from redirectUri: $redirect", null)
            sendResult(RawAuthorizationResult.fromException(
                ClientException(ErrorStrings.UNKNOWN_ERROR, "Could not extract redirect scheme from redirectUri.")
            ))
            finish()
            return
        }

        authFlowStarted = true
        Logger.info(methodTag, "Launching AuthTab for URL, redirect scheme: $scheme")
        val authTabIntent = AuthTabIntent.Builder().build()
        authTabIntent.launch(authTabLauncher, Uri.parse(url), scheme)
    }

    private fun handleAuthResult(result: AuthTabIntent.AuthResult) {
        val methodTag = "$TAG:handleAuthResult"
        val span = OTelUtility.createSpan(SpanName.AuthTabAuthorization.name)
        SpanExtension.makeCurrentSpan(span).use {
            try {
                span.setAttribute(AttributeName.is_auth_tab_used.name, true)
                span.setAttribute(AttributeName.auth_tab_result_code.name, result.resultCode)

                when (result.resultCode) {
                    AuthTabIntent.RESULT_OK -> {
                        val resultUri = result.resultUri
                        Logger.info(methodTag, "AuthTab returned RESULT_OK.")
                        span.setStatus(StatusCode.OK)
                        if (resultUri != null) {
                            sendResult(RawAuthorizationResult.fromRedirectUri(resultUri.toString()))
                        } else {
                            Logger.warn(methodTag, "AuthTab RESULT_OK but resultUri is null - treating as cancellation.")
                            sendResult(RawAuthorizationResult.ResultCode.CANCELLED)
                        }
                        finish()
                    }
                    AuthTabIntent.RESULT_CANCELED -> {
                        Logger.info(methodTag, "AuthTab returned RESULT_CANCELED - user cancelled authorization.")
                        span.setStatus(StatusCode.OK)
                        cancelAuthorization(true)
                    }
                    AuthTabIntent.RESULT_VERIFICATION_FAILED -> {
                        Logger.error(methodTag, "AuthTab returned RESULT_VERIFICATION_FAILED.", null)
                        span.setStatus(StatusCode.ERROR)
                        sendResult(RawAuthorizationResult.fromException(
                            ClientException(ErrorStrings.UNKNOWN_ERROR, "AuthTab verification failed.")
                        ))
                        finish()
                    }
                    AuthTabIntent.RESULT_VERIFICATION_TIMED_OUT -> {
                        Logger.error(methodTag, "AuthTab returned RESULT_VERIFICATION_TIMED_OUT.", null)
                        span.setStatus(StatusCode.ERROR)
                        sendResult(RawAuthorizationResult.fromException(
                            ClientException(ErrorStrings.UNKNOWN_ERROR, "AuthTab verification timed out.")
                        ))
                        finish()
                    }
                    else -> {
                        Logger.warn(methodTag, "AuthTab returned unknown result code: ${result.resultCode}")
                        span.setStatus(StatusCode.OK)
                        cancelAuthorization(true)
                    }
                }
            } catch (t: Throwable) {
                span.setStatus(StatusCode.ERROR)
                span.recordException(t)
                throw t
            } finally {
                span.end()
            }
        }
    }
}
