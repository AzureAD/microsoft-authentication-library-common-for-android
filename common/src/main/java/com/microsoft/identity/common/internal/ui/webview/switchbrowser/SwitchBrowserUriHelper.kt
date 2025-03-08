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

import android.net.Uri
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.SWITCH_BROWSER
import com.microsoft.identity.common.java.AuthenticationConstants.OAuth2
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.logging.Logger

/**
 * SwitchBrowserUriHelper is a helper class to build URIs for the switch browser challenge.
 */
object SwitchBrowserUriHelper {

    private const val TAG = "SwitchBrowserUriHelper"

    /**
     * Build the process uri for the switch browser challenge.
     *
     * @param uri The uri containing the switch browser code and action URL.
     * e.g. msauth://com.microsoft.identity.client/switch_browser?code=code&action_uri=action-uri
     *
     * @return The process uri constructed from the broker redirect uri.
     * e.g. action_uri?code=code
     */
    @Throws(ClientException::class, IllegalArgumentException::class, NullPointerException::class, UnsupportedOperationException::class)
    fun buildProcessUri(uri: Uri): Uri {
        val methodTag = "$TAG:buildProcessUri"
        // Get the SwitchBrowser purpose token from the broker redirect uri.
        val code = uri.getQueryParameter(
            SWITCH_BROWSER.CODE
        )
        if (code.isNullOrEmpty()) {
            // This should never happen, but if it does, we should log it and throw.
            val errorMessage = "switch browser code is null or empty"
            val exception = ClientException(ClientException.MALFORMED_URL, errorMessage)
            Logger.error(methodTag, errorMessage, exception)
            throw exception
        }
        // Get the process uri from the broker redirect uri.
        val actionUri = uri.getQueryParameter(
            SWITCH_BROWSER.ACTION_URI
        )
        if (actionUri.isNullOrEmpty()) {
            // This should never happen, but if it does, we should log it and throw.
            val errorMessage = "switch browser action uri is null or empty"
            val exception = ClientException(ClientException.MALFORMED_URL, errorMessage)
            Logger.error(methodTag, errorMessage, exception)
            throw exception
        }
        // Query parameters for the process uri.
        val queryParams = hashMapOf<String, String>()
        queryParams[SWITCH_BROWSER.CODE] = code
        queryParams[OAuth2.REDIRECT_URI] = "${uri.scheme}://${uri.authority}"
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
    @Throws(IllegalArgumentException::class, NullPointerException::class, UnsupportedOperationException::class)
    private fun buildSwitchBrowserUri(
        actionUri: String,
        queryParams: HashMap<String, String>
    ): Uri {
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
     * Build the resume uri for the switch browser challenge.
     *
     * @param actionUri The action uri to be opened.
     * @param redirectUrl The redirect url to be included in the resume uri.
     * @param clientId The client id to be included in the resume uri.
     *
     * @return The resume uri constructed from the bundle.
     * e.g. actionUri?client_id=client-id&redirect_uri=redirect-uri
     */
    fun buildResumeUri(actionUri: String, redirectUrl: String, clientId: String): Uri {
        // Query parameters for the resume uri.
        val queryParams = hashMapOf<String, String>()
        queryParams[OAuth2.CLIENT_ID] = clientId
        queryParams[OAuth2.REDIRECT_URI] = redirectUrl
        // Construct the uri to the resume endpoint.
        return buildSwitchBrowserUri(actionUri, queryParams)
    }

    /**
     * Check if the url is a switch browser redirect url
     *
     * The request is considered "switch_browser" if the URL
     * starts with the following pattern: {redirectUrl}/{switchBrowserPath}
     *
     * @param url The URL to be checked.
     * @param redirectUrl The redirect URL to be checked against.
     * @param switchBrowserPath The path to be checked against.
     * @return True if the request matches the pattern, false otherwise.
     */
    fun isSwitchBrowserRedirectUrl(url: String?, redirectUrl: String, switchBrowserPath: String): Boolean {
        if (url == null) {
            return false
        }
        return url.startsWith("${redirectUrl}/${switchBrowserPath}", ignoreCase = true)
    }
}
