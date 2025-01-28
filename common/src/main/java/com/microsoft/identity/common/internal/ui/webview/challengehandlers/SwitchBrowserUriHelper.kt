package com.microsoft.identity.common.internal.ui.webview.challengehandlers

import android.net.Uri
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.SWITCH_BROWSER
import com.microsoft.identity.common.java.AuthenticationConstants.OAuth2
import com.microsoft.identity.common.logging.Logger

/**
 * SwitchBrowserUriHelper is a helper class to build URIs for the switch browser challenge.
 */
object SwitchBrowserUriHelper {

    private const val TAG = "SwitchBrowserUriBuilder"

    /**
     * Build the process uri for the switch browser challenge.
     *
     * @param brokerRedirectUri The broker redirect uri containing the switch browser code and action URI.
     * e.g. msauth://com.microsoft.identity.client/your-redirect-uri?code=your-switch-browser-code&action_uri=your-action-uri
     *
     * @return The process uri constructed from the broker redirect uri.
     * e.g. your-action-uri?code=your-switch-browser-code
     */
    fun buildProcessUri(brokerRedirectUri: Uri): Uri? {
        val methodTag = "$TAG:buildProcessUri"
        // Get the session token from the broker redirect uri.
        val sessionToken = brokerRedirectUri.getQueryParameter(
            SWITCH_BROWSER.CODE
        )
        if (sessionToken.isNullOrEmpty()) {
            // This should never happen, but if it does, we should log it and return.
            Logger.warn(methodTag, "Switch browser code is null or empty ")
            return null
        }
        // Get the process uri from the broker redirect uri.
        val actionUri = brokerRedirectUri.getQueryParameter(
            SWITCH_BROWSER.ACTION_URI
        )
        if (actionUri.isNullOrEmpty()) {
            // This should never happen, but if it does, we should log it and return.
            Logger.warn(methodTag, "Switch browser action URI is null or empty ")
            return null
        }
        // Query parameters for the process uri.
        val queryParams = hashMapOf<String, String>()
        queryParams[SWITCH_BROWSER.CODE] = sessionToken
        queryParams[OAuth2.REDIRECT_URI] = Broker.NEW_BROKER_REDIRECT_URI
        // Construct the uri to the process endpoint.
        return buildSwitchBrowserUri(actionUri, queryParams)
    }

    /**
     * Build a generic switch browser uri.
     *
     * @param actionUri The action uri to be opened.
     * @param queryParams The query parameters to be included in the switch browser uri.
     *
     * @return The switch browser uri constructed from the action uri and query parameters.
     */
    private fun buildSwitchBrowserUri(
        actionUri: String,
        queryParams: HashMap<String, String>
    ): Uri? {
        val paths = actionUri.split("/")
        val authority = paths[0]
        val uriBuilder = Uri.Builder()
            .scheme("https")
            .encodedAuthority(authority)
        for (i in 1 until paths.size) {
            uriBuilder.appendPath(paths[i])
        }
        for ((key, value) in queryParams.entries) {
            uriBuilder.appendQueryParameter(key, value)
        }
        return uriBuilder.build()
    }

    /**
     * Check if the request is to switch the browser.
     *
     * The request is considered "switch_browser" if the URL contains
     * the action URI, code, and action parameters.
     *
     * @param uri The URI of the request.
     * @return True if the request contains the required parameters, false otherwise.
     */
    fun isSwitchBrowserRequest(uri: Uri?): Boolean {
        if (uri == null) {
            return false
        }
        val requiredParams =
            setOf(SWITCH_BROWSER.ACTION_URI, SWITCH_BROWSER.CODE, SWITCH_BROWSER.ACTION)
        return uri.queryParameterNames.containsAll(requiredParams)
    }
}
