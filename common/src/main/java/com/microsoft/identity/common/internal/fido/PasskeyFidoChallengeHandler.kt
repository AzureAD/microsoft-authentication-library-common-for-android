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
import com.microsoft.identity.common.java.opentelemetry.IFidoTelemetryHelper
import com.microsoft.identity.common.logging.Logger
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
 * @param telemetryHelper IFidoTelemetryHelper instance.
 */(
    private val fidoManager: IFidoManager,
    webView: WebView,
    private val lifecycleOwner: LifecycleOwner?,
    telemetryHelper: IFidoTelemetryHelper
) : AbstractFidoChallengeHandler(webView, telemetryHelper) {
    val TAG = PasskeyFidoChallengeHandler::class.simpleName.toString()

    override fun processChallenge(fidoChallenge: IFidoChallenge): Void? {
        val methodTag = "$TAG:processChallenge"
        telemetryHelper.setFidoChallenge(fidoChallenge::class.simpleName.toString())
        telemetryHelper.setFidoChallengeHandler(TAG)
        if (lifecycleOwner != null && fidoChallenge is AuthFidoChallenge) {
            lifecycleOwner.lifecycleScope.launch {
                try {
                    val assertion = fidoManager.authenticate(fidoChallenge)
                    respondToChallenge(fidoChallenge.submitUrl, fidoChallenge.context, assertion)
                    telemetryHelper.setResultSuccess()
                } catch (e: Exception) {
                    //Exceptions here could be specific to the FIDO APIs being used.
                    Logger.error(methodTag, e.message, e)
                    telemetryHelper.setResultFailure(e)
                    //TODO: Look into responding with a more specific error message in assertion,
                    // once custom protocol is complete and more testing is done with CredMan.
                    respondToChallengeWithEmptyAssertion(fidoChallenge.submitUrl, fidoChallenge.context)
                }
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
        telemetryHelper.setResultFailure(errorMessage)
        respondToChallengeWithEmptyAssertion(fidoChallenge.submitUrl, fidoChallenge.context)
        return null
    }

    /**
     * Makes a post request in the WebView with the submitUrl and headers.
     * @param submitUrl The url to which the client submits the response to the server's challenge.
     * @param context Server state that needs to be maintained between challenge and response.
     * @param assertion string representing response with signed challenge.
     */
    private fun respondToChallenge(submitUrl: String,
                                   context: String,
                                   assertion: String) {
        val methodTag = "$TAG:respondToChallenge"
        val header = mapOf(
            FidoResponseField.Assertion.name to assertion,
            FidoResponseField.Context.name to context
        )
        webView.post {
            Logger.info(methodTag, "Responding to Fido challenge.")
            //TODO: server team might want us to postUrl instead of loadUrl. Edit this line accordingly once they make a decision.
            webView.loadUrl(submitUrl, header)
        }
    }

    /**
     * Makes a post request in the WebView with an empty assertion.
     * Should be called due to cancellation or error while getting assertion.
     * @param submitUrl The url to which the client submits the response to the server's challenge.
     * @param context Server state that needs to be maintained between challenge and response.
     */
    private fun respondToChallengeWithEmptyAssertion(submitUrl: String, context: String) {
        val methodTag = "$TAG:respondToChallengeWithEmptyAssertion"
        Logger.info(methodTag, "Responding to Fido challenge with empty assertion.")
        respondToChallenge(submitUrl, context, "")

    }
}