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
package com.microsoft.identity.common.internal.ui.webview.switchbrowser

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.SWITCH_BROWSER
import com.microsoft.identity.common.internal.providers.oauth2.DUNAActivity
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.SwitchBrowserRequestHandler
import com.microsoft.identity.common.java.AuthenticationConstants.AAD.AUTHORIZATION
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.opentelemetry.OTelUtility
import com.microsoft.identity.common.java.opentelemetry.SpanExtension
import com.microsoft.identity.common.java.opentelemetry.SpanName
import com.microsoft.identity.common.logging.Logger
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.StatusCode
import androidx.core.net.toUri

/**
 * SwitchBrowserProtocolCoordinator is responsible for coordinating the switch browser protocol.
 * Contains the handler to process the switch browser request and resume action.
 */
class SwitchBrowserProtocolCoordinator(
    val switchBrowserRequestHandler: SwitchBrowserRequestHandler,
    private val spanContext: SpanContext? = null) {

    constructor(activity: Activity, spanContext: SpanContext?) : this(SwitchBrowserRequestHandler(activity, spanContext), spanContext)

    val span: Span by lazy {
        OTelUtility.createSpanFromParent(SpanName.SwitchBrowserResume.name, spanContext)
    }

    companion object {
        private const val TAG = "SwitchBrowserProtocolCoordinator"

        /**
         * Checks if the given [url] is used to resume the switch browser flow.
         * This is determined by validating whether the URL starts with `[redirectUrl]/switch_browser_resume`.
         *
         * Returns `true` if the URL matches the expected pattern, `false` otherwise.
         */
        fun isSwitchBrowserResume(url: String?, redirectUrl: String): Boolean {
            return SwitchBrowserUriHelper.isSwitchBrowserRedirectUrl(url, redirectUrl, SWITCH_BROWSER.RESUME_PATH)
        }

        /**
         * Creates an intent to resume the BrokerAuthorizationActivity for the WebView.
         * Extracts the action_uri and code from the [intentDataString] and add those as extras of the intent.
         *
         * Returns the [Intent] used to start the WebView.
         */
        fun getIntentToResumeWebViewAuth(context: Context, intentDataString: String): Intent {
            val uri = intentDataString.toUri()
            val intent = Intent(context, DUNAActivity::class.java)

            intent.putExtra(
                SWITCH_BROWSER.ACTION_URI,
                uri.getQueryParameter(SWITCH_BROWSER.ACTION_URI)
            )
            intent.putExtra(
                SWITCH_BROWSER.CODE,
                uri.getQueryParameter(SWITCH_BROWSER.CODE)
            )
            intent.putExtra(
                SWITCH_BROWSER.STATE,
                uri.getQueryParameter(SWITCH_BROWSER.STATE)
            )
            return intent
        }
    }

    /**
     * Processes the switch browser resume action.
     *
     * @param extras The bundle containing the switch browser action URI and authorization code.
     * @param onSuccessAction The action to perform on success.
     *
     * The [onSuccessAction] function takes two parameters: the resume URL and the headers.
     * In this case, [onSuccessAction] will launch the WebView with the provided resume URI and headers.
     */
    @Throws(ClientException::class)
    fun processSwitchBrowserResume(
        authorizationRequest: String,
        extras: Bundle,
        onSuccessAction: (Uri, HashMap<String, String>) -> Unit
    ) {
        SpanExtension.makeCurrentSpan(span).use {
            val methodTag = "$TAG:processSwitchBrowserResume"
            val actionUri = extras.getString(SWITCH_BROWSER.ACTION_URI)
            val code = extras.getString(SWITCH_BROWSER.CODE)
            val state = extras.getString(SWITCH_BROWSER.STATE)
            if (actionUri.isNullOrEmpty() || code.isNullOrEmpty()) {
                val clientException = ClientException(
                    ClientException.MISSING_PARAMETER,
                    "Action URI is null/empty: ${actionUri.isNullOrEmpty()}," +
                            " code is null/empty: ${code.isNullOrEmpty()}."
                )
                span.setStatus(StatusCode.ERROR)
                span.recordException(clientException)
                span.end()
                throw clientException
            }
            SwitchBrowserUriHelper.statesMatch(authorizationRequest, state)
            // Validate the state from auth request and redirect URL is the same
            val resumeUri = SwitchBrowserUriHelper.buildResumeUri(actionUri, state)
            val authorizationHeaderValue = "Bearer $code"
            val headers = hashMapOf(AUTHORIZATION to authorizationHeaderValue)
            onSuccessAction(resumeUri, headers)
            // Reset the challenge state after processing the resume action
            switchBrowserRequestHandler.resetChallengeState()
            Logger.info(methodTag, "Switch browser resume action processed successfully.")
            span.setAttribute(AttributeName.is_switch_browser_resume_handled.name, true)
            span.setStatus(StatusCode.OK)
            span.end()
        }
    }

    /**
     * Check if the handler processed a switch browser request.
     * if so, it means we are resuming the switch browser flow.
     *
     * @return boolean
     */
    fun isExpectingSwitchBrowserResume(): Boolean {
        val methodTag = "$TAG:isExpectingSwitchBrowserResume"
        Logger.verbose(methodTag, "ExpectingRequest: ${switchBrowserRequestHandler.isChallengeHandled}")
        return switchBrowserRequestHandler.isChallengeHandled
    }
}
