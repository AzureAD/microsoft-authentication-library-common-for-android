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
import com.microsoft.identity.common.java.constants.FidoConstants
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.opentelemetry.OTelUtility
import com.microsoft.identity.common.java.opentelemetry.SpanName
import com.microsoft.identity.common.logging.Logger
import io.opentelemetry.api.trace.StatusCode
import kotlinx.coroutines.launch

/**
 * Handles a FidoChallenge by either creating or authenticating with a passkey.
 */
class PasskeyFidoChallengeHandler
/**
 * Creates a PasskeyFidoChallengeHandler instance.
 * @param fidoManager IFidoManager instance.
 * @param webView Current WebView.
 * @param lifecycleOwner instance to get coroutine scope from.
 */(
    private val fidoManager: IFidoManager,
    webView: WebView,
    private val lifecycleOwner: LifecycleOwner?,
) : AbstractFidoChallengeHandler(webView) {
    val TAG = PasskeyFidoChallengeHandler::class.simpleName.toString()
    val span = OTelUtility.createSpan(SpanName.Fido.name)

    override fun processChallenge(fidoChallenge: IFidoChallenge): Void? {
        val methodTag = "$TAG:processChallenge"
        span.setAttribute(
            AttributeName.fido_challenge_handler.name,
            TAG);
        span.setAttribute(
            AttributeName.fido_challenge.name,
            fidoChallenge::class.simpleName.toString());
        var assertion: String
        if (lifecycleOwner != null
            && fidoChallenge is AuthFidoChallenge) {
            lifecycleOwner.lifecycleScope.launch {
                try {
                    assertion = fidoManager.authenticate(fidoChallenge)
                    span.setStatus(StatusCode.OK)
                } catch (e: Exception) {
                    assertion = e.message.toString()
                    Logger.error(methodTag, assertion, e)
                    span.setStatus(StatusCode.ERROR)
                }
                span.end()
                respondToChallenge(fidoChallenge.submitUrl, assertion, fidoChallenge.context)
            }
            return null
        }
        var errorMessage = "Failed based on parameter values."
        if (lifecycleOwner == null) {
            errorMessage += " Cannot get lifecycle owner needed for FIDO API calls."
        }
        if (fidoChallenge !is AuthFidoChallenge) {
            errorMessage += " FidoChallenge object is of type " + fidoChallenge::class.simpleName.toString() + ", which is unexpected and not supported."
        }
        Logger.error(methodTag, errorMessage, null)
        span.setStatus(StatusCode.ERROR)
        span.end()
        respondToChallenge(fidoChallenge.submitUrl, errorMessage, fidoChallenge.context)
        return null
    }

    /**
     * Makes a post request in the WebView with the submitUrl and headers.
     * @param submitUrl The url to which the client submits the response to the server's challenge.
     * @param context Server state that needs to be maintained between challenge and response.
     * @param assertion string representing response with signed challenge.
     */
    fun respondToChallenge(submitUrl: String,
                           assertion: String,
                           context: String) {
        val methodTag = "$TAG:respondToChallenge"
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
}
