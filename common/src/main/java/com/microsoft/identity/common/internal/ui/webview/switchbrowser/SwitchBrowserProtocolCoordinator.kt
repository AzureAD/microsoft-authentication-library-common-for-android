package com.microsoft.identity.common.internal.ui.webview.switchbrowser

import android.app.Activity
import android.os.Bundle
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.SWITCH_BROWSER
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.SwitchBrowserRequestHandler
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.SwitchBrowserUriHelper.buildResumeUri
import com.microsoft.identity.common.java.AuthenticationConstants.AAD.AUTHORIZATION
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.logging.Logger

class SwitchBrowserProtocolCoordinator(
    val switchBrowserRequestHandler: SwitchBrowserRequestHandler
) {
    constructor(activity: Activity) : this(SwitchBrowserRequestHandler(activity))

    companion object {
        private const val TAG = "SwitchBrowserProtocolCoordinator"
    }

    /**
     * Process the switch browser resume action.
     *
     * @param extras The extras bundle containing the switch browser action URI and code.
     * @param redirectUrl The redirect URL to be used in the resume URI.
     * @param clientId The client ID to be used in the resume URI.
     * @param onSuccessAction The action to be performed on success.
     * (in this case, it will be the launch the WebView with the resume URI)
     *
     */
    @Throws(ClientException::class)
    fun processSwitchBrowserResume(
        extras: Bundle,
        redirectUrl: String,
        clientId: String,
        onSuccessAction: (String, HashMap<String, String>) -> Unit
    ) {
        val actionUri = extras.getString(SWITCH_BROWSER.ACTION_URI)
        val code = extras.getString(SWITCH_BROWSER.CODE)
        if (actionUri.isNullOrEmpty() || code.isNullOrEmpty()) {
            throw ClientException(
                ClientException.MISSING_PARAMETER,
                "Action URI is null: ${actionUri == null}, code is null: ${code == null}"
            )
        }
        onSuccessAction(
            buildResumeUri(actionUri, redirectUrl, clientId).toString(),
            hashMapOf(AUTHORIZATION to code)
        )
        switchBrowserRequestHandler.resetChallengeState()
    }

    /**
     * Check if the extras contains the switch browser code and action uri.
     * also checks that the request is expecting a switch browser request.
     * if so, it means we are resuming the switch browser flow.
     *
     * @param extras Bundle containing the switch browser action URI and code.
     * @return boolean
     */
    fun isSwitchBrowserResume(extras: Bundle): Boolean {
        val methodTag = "$TAG:isSwitchBrowserResumeFlow"

        val containsActionUri = extras.containsKey(SWITCH_BROWSER.ACTION_URI)
        val containsCode = extras.containsKey(SWITCH_BROWSER.CODE)
        val expectingRequest = switchBrowserRequestHandler.isChallengeHandled
        Logger.verbose(
            methodTag,
            "action uri: $containsActionUri," +
                    " code: $containsCode," +
                    " expectingRequest: $expectingRequest"
        )
        return containsCode && containsActionUri && expectingRequest
    }
}