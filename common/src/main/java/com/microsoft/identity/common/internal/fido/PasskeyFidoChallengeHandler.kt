package com.microsoft.identity.common.internal.fido

import android.webkit.WebView
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.coroutineScope
import com.microsoft.identity.common.java.opentelemetry.IFidoTelemetryHelper
import com.microsoft.identity.common.java.ui.webview.authorization.IAuthorizationCompletionCallback
import com.microsoft.identity.common.logging.Logger
import kotlinx.coroutines.launch

/**
 * Handles a FidoChallenge by either creating or authenticating with a passkey.
 */
class PassKeyFidoChallengeHandler
/**
 * Creates a PassKeyFidoChallengeHandler instance.
 * @param fidoManager IFidoManager instance.
 * @param webView Current WebView.
 * @param challengeCallback callback to invoke after challenge is handled.
 * @param telemetryHelper IFidoTelemetryHelper instance.
 */(
    private val fidoManager: IFidoManager,
    webView: WebView,
    private val challengeCallback: IAuthorizationCompletionCallback,
    telemetryHelper: IFidoTelemetryHelper
) : AbstractFidoChallengeHandler(webView, telemetryHelper) {
    val TAG = PassKeyFidoChallengeHandler::class.simpleName

    override fun processChallenge(fidoChallenge: IFidoChallenge): Void? {
        val lifeCycleOwner = ViewTreeLifecycleOwner.get(webView)
        if (lifeCycleOwner != null && fidoChallenge is AuthFidoChallenge) {
            lifeCycleOwner.lifecycle.coroutineScope.launch {
                val assertion = fidoManager.authenticate(fidoChallenge)
                respondToChallenge(fidoChallenge.submitUrl, assertion)
            }
        } else {

        }

        //This all might need to be in a coroutine for IFidoManager.
        //First, is this a registration or an authorization challenge?
        //Pass the challenge to the manager's methods based on that
        //These two methods should return the same object... maybe header
        //Take this header, and pass this plus the url to the respondToChallenge method.
        //When an exception occurs, let's pass the empty header to the server to prevent repeat requests. Also update telemetryHelper
        return null
    }

    /**
     * Makes a post request in the WebView with the submitUrl and headers.
     * @param submitUrl The url to which the client submits the response to the server's challenge.
     * @param assertion string
     */
    private fun respondToChallenge(submitUrl: String, assertion: String) {
        val methodTag = "$TAG:respondToChallenge"
        val header = mapOf("assertion" to assertion)
        webView.post {
            Logger.info(methodTag, "Responding to fido challenge")

            webView.loadUrl(submitUrl, header)
        }
    }
}