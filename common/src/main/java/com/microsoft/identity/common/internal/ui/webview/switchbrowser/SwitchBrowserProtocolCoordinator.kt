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
import android.net.Uri
import android.os.Bundle
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.SWITCH_BROWSER
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

/**
 * SwitchBrowserProtocolCoordinator is responsible for coordinating the switch browser protocol.
 * Contains the handler to process the switch browser request and resume action.
 */
class SwitchBrowserProtocolCoordinator(
    val switchBrowserRequestHandler: SwitchBrowserRequestHandler,
    private val spanContext: SpanContext? = null) {

    /**
     * Indicates that the switch browser flow was initiated during this session.
     * Delegates to the handler's flag which is set at challenge time and never reset.
     */
    val wasSwitchBrowserFlowInitiated: Boolean
        get() = switchBrowserRequestHandler.wasSwitchBrowserFlowInitiated

    constructor(activity: Activity, spanContext: SpanContext?) : this(SwitchBrowserRequestHandler(activity, spanContext), spanContext)

    val span: Span by lazy {
        OTelUtility.createSpanFromParent(SpanName.SwitchBrowserResume.name, spanContext)
    }

    companion object {
        private const val TAG = "SwitchBrowserProtocolCoordinator"

        private const val ERROR_CODE_KEY = "error_code"
        private const val ERROR_MESSAGE_KEY = "error_message"

        /**
         * Checks if the given [url] is used to resume the switch browser flow.
         * This is determined by validating whether the URL starts with `[redirectUrl]/switch_browser_resume`.
         *
         * Returns `true` if the URL matches the expected pattern, `false` otherwise.
         */
        fun isSwitchBrowserResume(url: String?, redirectUrl: String): Boolean {
            return SwitchBrowserUriHelper.isSwitchBrowserRedirectUrl(url, redirectUrl, SWITCH_BROWSER.RESUME_PATH)
        }

        fun createErrorBundle(errorCode: String, errorMessage: String): Bundle {
            return Bundle().apply {
                putString(ERROR_CODE_KEY, errorCode)
                putString(ERROR_MESSAGE_KEY, errorMessage)
            }
        }
    }

    /**
     * Inspects the given [bundle] for error entries populated by [createErrorBundle].
     * If either [ERROR_CODE_KEY] or [ERROR_MESSAGE_KEY] is present, a [ClientException] is
     * constructed with those values and thrown immediately.
     *
     * @param bundle The bundle to inspect.
     * @throws ClientException if the bundle contains an error code or error message.
     */
    @Throws(ClientException::class)
    private fun throwIfBundleContainsError(bundle: Bundle) {
        val errorCode = bundle.getString(ERROR_CODE_KEY)
        val errorMessage = bundle.getString(ERROR_MESSAGE_KEY)
        if (!errorCode.isNullOrEmpty() || !errorMessage.isNullOrEmpty()) {
            throw ClientException(
                errorCode ?: ClientException.UNKNOWN_ERROR,
                errorMessage ?: "An unknown error occurred in the switch browser flow."
            )
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
        val methodTag = "$TAG:processSwitchBrowserResume"
        SpanExtension.makeCurrentSpan(span).use {
            try {
                throwIfBundleContainsError(extras)
                val actionUri = extras.getString(SWITCH_BROWSER.ACTION_URI)
                val code = extras.getString(SWITCH_BROWSER.CODE)
                val state = extras.getString(SWITCH_BROWSER.STATE)
                if (actionUri.isNullOrEmpty() || code.isNullOrEmpty()) {
                    throw ClientException(
                        ClientException.MISSING_PARAMETER,
                        "Action URI is null/empty: ${actionUri.isNullOrEmpty()}," +
                                " code is null/empty: ${code.isNullOrEmpty()}."
                    )
                }
                // Validate the state from auth request and redirect URL is the same.
                SwitchBrowserUriHelper.statesMatch(authorizationRequest, state)
                val resumeUri = SwitchBrowserUriHelper.buildResumeUri(actionUri, state)
                val headers = hashMapOf(AUTHORIZATION to "Bearer $code")
                onSuccessAction(resumeUri, headers)
                Logger.info(methodTag, "Switch browser resume action processed successfully.")
                span.setAttribute(AttributeName.is_switch_browser_resume_handled.name, true)
                span.setStatus(StatusCode.OK)
            } catch (t: Throwable) {
                span.setStatus(StatusCode.ERROR)
                span.recordException(t)
                throw t
            } finally {
                // Always clear the challenge state — this resume is one-shot. Leaving it set
                // on the error path would cause subsequent onResume() calls to re-enter the
                // resume flow with an already-consumed bundle and fail again.
                switchBrowserRequestHandler.resetChallengeState()
                span.end()
            }
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
        Logger.verbose(methodTag, "ExpectingRequest: ${switchBrowserRequestHandler.isSwitchBrowserChallengeActive}")
        return switchBrowserRequestHandler.isSwitchBrowserChallengeActive
    }
}
