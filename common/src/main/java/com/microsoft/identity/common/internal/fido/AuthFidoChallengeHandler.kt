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
package com.microsoft.identity.common.internal.fido

import android.webkit.WebView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.IChallengeHandler
import com.microsoft.identity.common.java.constants.FidoConstants
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.opentelemetry.OTelUtility
import com.microsoft.identity.common.java.opentelemetry.SpanName
import com.microsoft.identity.common.logging.Logger
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.StatusCode
import kotlinx.coroutines.launch
import java.util.*

/**
 * Handles a FidoChallenge by either creating or authenticating with a passkey.
 *
 * @param fidoManager       IFidoManager instance.
 * @param webView           Current WebView.
 * @param spanContext       Current spanContext, if present.
 * @param lifecycleOwner    instance to get coroutine scope from.
 */
class AuthFidoChallengeHandler (
    private val fidoManager: IFidoManager,
    private val webView: WebView,
    private val spanContext : SpanContext?,
    private val lifecycleOwner: LifecycleOwner?
) : IChallengeHandler<FidoChallenge, Void> {
    val TAG = AuthFidoChallengeHandler::class.simpleName.toString()

    override fun processChallenge(fidoChallenge: FidoChallenge): Void? {
        val methodTag = "$TAG:processChallenge"
        val span = if (spanContext != null) {
            OTelUtility.createSpanFromParent(SpanName.Fido.name, spanContext)
        } else {
            OTelUtility.createSpan(SpanName.Fido.name)
        }
        span.setAttribute(
            AttributeName.fido_challenge_handler.name,
            TAG
        );
        // First verify submitUrl and context. Without these two, we can't respond back to the server.
        // If either one of these are missing or malformed, throw an exception and let the main WebViewClient handle it.
        val submitUrl = fidoChallenge.submitUrl.getOrThrow()
        val context = fidoChallenge.context.getOrThrow()
        val authChallenge: String
        val relyingPartyIdentifier: String
        val userVerificationPolicy: String
        val allowedCredentials: List<String>?
        try {
            authChallenge = fidoChallenge.challenge.getOrThrow()
            relyingPartyIdentifier = fidoChallenge.relyingPartyIdentifier.getOrThrow()
            userVerificationPolicy = fidoChallenge.userVerificationPolicy.getOrThrow()
            allowedCredentials = fidoChallenge.allowedCredentials.getOrThrow()
            fidoChallenge.version.getOrThrow()
            //Not currently using keyTypes, but should validate in case we use it in the future.
            fidoChallenge.keyTypes.getOrThrow()
        } catch (e : Exception) {
            respondToChallengeWithError(
                submitUrl = submitUrl,
                context = context,
                span = span,
                errorMessage = e.message.toString(),
                exception = e,
                methodTag = methodTag
            )
            return null
        }
        if (lifecycleOwner == null) {
            respondToChallengeWithError(
                submitUrl = submitUrl,
                context = context,
                span = span,
                errorMessage = "Cannot get lifecycle owner needed for FIDO API calls.",
                methodTag = methodTag
            )
            return null
        }
        lifecycleOwner.lifecycleScope.launch {
            try {
                val assertion = fidoManager.authenticate(
                    challenge = authChallenge,
                    relyingPartyIdentifier = relyingPartyIdentifier,
                    allowedCredentials = allowedCredentials,
                    userVerificationPolicy = userVerificationPolicy
                )
                span.setStatus(StatusCode.OK)
                respondToChallenge(
                    submitUrl = submitUrl,
                    assertion = assertion,
                    context = context,
                    span
                )
            } catch (e : Exception) {
                respondToChallengeWithError(
                    submitUrl = submitUrl,
                    context = context,
                    span = span,
                    errorMessage = e.message.toString(),
                    exception = e,
                    methodTag = methodTag
                )
            }
        }
        return null
    }

    /**
     * Makes a post request in the WebView with the submitUrl and headers.
     *
     * @param submitUrl The url to which the client submits the response to the server's challenge.
     * @param assertion string representing response with signed challenge.
     * @param context   Server state that needs to be maintained between challenge and response.
     * @param span      Current OTel span.
     */
    fun respondToChallenge(submitUrl: String,
                           assertion: String,
                           context: String,
                           span: Span
    ) {
        val methodTag = "$TAG:respondToChallenge"
        span.end()
        //We're splitting the context value here because ESTS is expected to also send the flow token in the same string.
        //They want us to send the flow token value via a header separate from the actual context value.
        val splitContextList = context.split(FidoConstants.PASSKEY_CONTEXT_DELIMITER)
        val actualContext: String
        val flowToken: String
        if (splitContextList.size == 2) {
            actualContext = splitContextList[0]
            flowToken = splitContextList[1]
        } else {
            //Put everything under the actual context header.
            actualContext = splitContextList[0]
            flowToken = ""
        }
        val header = mapOf(
            FidoConstants.PASSKEY_RESPONSE_ASSERTION_HEADER to assertion,
            FidoConstants.PASSKEY_RESPONSE_CONTEXT_HEADER to actualContext,
            FidoConstants.PASSKEY_RESPONSE_FLOWTOKEN_HEADER to flowToken
        )
        webView.post {
            Logger.info(methodTag, "Responding to Fido challenge.")
            webView.loadUrl(submitUrl, header)
        }
    }

    /**
     * Helper method to respond to the server with an error.
     *
     * @param submitUrl     The url to which the client submits the response to the server's challenge.
     * @param context       Server state that needs to be maintained between challenge and response.
     * @param span          current OTel Span.
     * @param errorMessage  Error message string.
     * @param exception     Exception associated with error. Default null.
     * @param assertion     String representing response with signed challenge.
     */
    fun respondToChallengeWithError(submitUrl: String,
                                    context: String,
                                    span: Span,
                                    errorMessage: String,
                                    exception: Exception? = null,
                                    methodTag: String? = "$TAG:respondToChallengeWithError"
    ) {
        Logger.error(methodTag, errorMessage, exception)
        if (exception != null) {
            span.recordException(exception)
            span.setStatus(StatusCode.ERROR)
        } else {
            span.setStatus(StatusCode.ERROR, errorMessage)
        }
        respondToChallenge(submitUrl, errorMessage, context, span)
    }
}
