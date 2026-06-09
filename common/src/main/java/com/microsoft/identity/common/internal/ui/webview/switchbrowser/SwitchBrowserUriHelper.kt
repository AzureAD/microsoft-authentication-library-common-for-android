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
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import com.microsoft.identity.common.java.opentelemetry.SpanExtension
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory
import com.microsoft.identity.common.logging.Logger
import io.opentelemetry.api.trace.StatusCode
import java.net.URL
import androidx.core.net.toUri

/**
 * SwitchBrowserUriHelper is a helper class to build URIs for the switch browser challenge.
 */
object SwitchBrowserUriHelper {

    private const val TAG = "SwitchBrowserUriHelper"

    internal val STATE_VALIDATION_REQUIRED: Boolean by lazy {
        CommonFlightsManager
            .getFlightsProvider()
            .isFlightEnabled(CommonFlight.SWITCH_BROWSER_PROTOCOL_REQUIRES_STATE)
    }

    /**
     * Extracts the base redirect URI (scheme + authority + all paths except the last one) from a full URI.
     *
     * This is useful for extracting the redirect base before the final path segment (e.g., "switch_browser").
     *
     * @param uri The full URI to extract the redirect from.
     * e.g. https://login.microsoftonline.com/androidbroker/com.microsoft.identity.testuserapp/switch_browser?action=1
     *
     * @return The base redirect URI containing scheme, authority, and all path segments except the last one.
     * e.g. https://login.microsoftonline.com/androidbroker/com.microsoft.identity.testuserapp
     *
     * @throws ClientException if the URI is missing a scheme or authority.
     */
    @Throws(ClientException::class)
    fun extractBaseRedirectUri(uri: Uri): String {
        val methodTag = "$TAG:extractRedirectUri"
        val scheme = uri.scheme
        if (scheme.isNullOrEmpty()) {
            val errorMessage = "URI is missing a scheme: '$uri'"
            val exception = ClientException(ClientException.MALFORMED_URL, errorMessage)
            Logger.error(methodTag, errorMessage, exception)
            throw exception
        }
        val authority = uri.authority
        if (authority.isNullOrEmpty()) {
            val errorMessage = "URI is missing an authority: '$uri'"
            val exception = ClientException(ClientException.MALFORMED_URL, errorMessage)
            Logger.error(methodTag, errorMessage, exception)
            throw exception
        }

        // Get the path segments and exclude the last one
        val path = uri.path
        val result = if (!path.isNullOrEmpty() && path != "/") {
            val segments = path.trim('/').split('/')
            if (segments.size > 1) {
                // Exclude the last segment (e.g., "switch_browser")
                val pathWithoutLast = segments.dropLast(1).joinToString("/")
                "$scheme://$authority/$pathWithoutLast"
            } else {
                // Only one segment, return scheme://authority
                "$scheme://$authority"
            }
        } else {
            // No path, return scheme://authority
            "$scheme://$authority"
        }

        return result
    }

    /**
     * Build the process uri for the switch browser challenge.
     *
     * @param uri The uri containing the switch browser code and action URL.
     * e.g. msauth://com.microsoft.identity.client/switch_browser?code=code&action_uri=action-uri
     *
     * @return The process uri constructed from the redirect uri.
     * e.g. action_uri?code=code
     */
    @Throws(ClientException::class, IllegalArgumentException::class, NullPointerException::class, UnsupportedOperationException::class)
    fun buildProcessUri(uri: Uri): Uri {
        val methodTag = "$TAG:buildProcessUri"
        // Get the SwitchBrowser purpose token from the redirect uri.
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
        // Get the process uri from the redirect uri.
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
        validateActionUri(actionUri)

        val state = uri.getQueryParameter(
            SWITCH_BROWSER.STATE
        )
        // Query parameters for the process uri.
        val queryParams = hashMapOf<String, String>()
        queryParams[SWITCH_BROWSER.CODE] = code
        addStateToQueryParams(queryParams, state, methodTag)
        // Construct the uri to the process endpoint.
        return buildSwitchBrowserUri(actionUri, queryParams)
    }

    /**
     * Build the resume uri for the switch browser challenge.
     *
     * @param actionUri The action uri to be opened.
     * @param state The state to be included in the switch browser uri.
     *
     * @return The resume uri constructed from the bundle.
     * e.g. actionUri
     */
    fun buildResumeUri(actionUri: String, state: String?): Uri {
        val methodTag = "$TAG:buildResumeUri"
        validateActionUri(actionUri)
        // Construct the uri to the resume endpoint.
        val queryParams = hashMapOf<String, String>()
        addStateToQueryParams(queryParams, state, methodTag)
        return buildSwitchBrowserUri(actionUri, queryParams)
    }

    /**
     * Builds the resume browser URI by appending [SWITCH_BROWSER.RESUME_PATH] to the base redirect URI.
     *
     * @param redirectUri The base redirect URI.
     * e.g. msauth://com.microsoft.identity.client
     *
     * @return The resume browser URI.
     * e.g. msauth://com.microsoft.identity.client/switch_browser_resume
     */
    fun buildResumeBrowserUri(redirectUri: String): Uri {
        return "$redirectUri/${SWITCH_BROWSER.RESUME_PATH}".toUri()
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
        val expectedUrl = "$redirectUrl/$switchBrowserPath"
        return url.startsWith(expectedUrl, ignoreCase = true)
    }

    /**
     * Check if state in the auth request matches the state provided.
     */
    fun statesMatch(authorizationUrl: String, state: String?) {
        val methodTag = "$TAG:statesMatch"
        if (!STATE_VALIDATION_REQUIRED) {
            Logger.info(methodTag, "State validation is not required.")
            return
        }
        val span = SpanExtension.current()
        // Validate the state from auth request and redirect URL is the same
        if (state.isNullOrEmpty()) {
            val clientException = ClientException(
                ClientException.STATE_MISMATCH,
                "State is null."
            )
            span.setStatus(StatusCode.ERROR)
            span.recordException(clientException)
            span.end()
            throw clientException
        }
        val authRequestState = authorizationUrl.toUri().getQueryParameter(SWITCH_BROWSER.STATE)
        if (authRequestState.isNullOrEmpty()) {
            val clientException = ClientException(
                ClientException.STATE_MISMATCH,
                "Authorization request state is null."
            )
            span.setStatus(StatusCode.ERROR)
            span.recordException(clientException)
            span.end()
            throw clientException
        }
        if (state != authRequestState) {
            val clientException = ClientException(
                ClientException.STATE_MISMATCH,
                "State does not match with the auth request state."
            )
            span.setStatus(StatusCode.ERROR)
            span.recordException(clientException)
            span.end()
            throw clientException
        }
        Logger.info(methodTag, "States match.")
    }

    /**
     * Add state to the query parameters If STATE_VALIDATION_REQUIRED is enabled.
     * If STATE_VALIDATION_REQUIRED is disabled, this method does nothing.
     * If STATE_VALIDATION_REQUIRED is enabled and state is null or empty, this method throws an exception.
     */
    private fun addStateToQueryParams(queryParams: HashMap<String, String>, state: String?, methodTag: String) {
        if (STATE_VALIDATION_REQUIRED) {
            if (state.isNullOrEmpty()) {
                // This should never happen, but if it does, we should log it and throw.
                val errorMessage = "State is null or empty"
                val exception = ClientException(ClientException.MISSING_PARAMETER, errorMessage)
                Logger.error(methodTag, errorMessage, exception)
                throw exception
            } else {
                queryParams[SWITCH_BROWSER.STATE] = state
            }
        }
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
        queryParams: HashMap<String, String> = hashMapOf()
    ): Uri {
        val uri = actionUri.toUri()

        val uriBuilder = uri.buildUpon()

        for ((key, value) in queryParams.entries) {
            uriBuilder.appendQueryParameter(key, value)
        }
        return uriBuilder.build()
    }

    /**
     * Validates the action URI to ensure it is well-formed and points to a valid Azure Active Directory authority.
     *
     * This function performs the following validations:
     * 1. Ensures Azure Active Directory cloud discovery has been performed
     * 2. Validates that the action URI string can be parsed as a valid URL
     * 3. Verifies that the URI host is a recognized AAD authority
     *
     * @param actionUriString The action URI string to validate. Must be a well-formed URL pointing to a valid AAD authority.
     *
     * @throws ClientException with error code [ClientException.IO_ERROR] if cloud discovery fails
     * @throws ClientException with error code [ClientException.MALFORMED_URL] if the URI string is malformed
     * @throws ClientException with error code [ClientException.UNKNOWN_AUTHORITY] if the URI host is not a valid AAD authority
     *
     * @see AzureActiveDirectory.ensureCloudDiscoveryForAuthority
     * @see AzureActiveDirectory.isValidCloudHost
     */
    private fun validateActionUri(actionUriString: String) {
        val methodTag = "$TAG:validateActionUri"
        // Ensure cloud discovery is complete for this authority.
        try {
            val actionUrlForDiscovery = URL(actionUriString)
            AzureActiveDirectory.ensureCloudDiscoveryForAuthority(actionUrlForDiscovery)
        } catch (e: Exception) {
            val errorMessage = "Failed to perform cloud discovery for AAD authorities."
            Logger.error(methodTag, errorMessage, e)
            throw ClientException(ClientException.IO_ERROR, errorMessage, e)
        }
        // Validate the action uri is not null or empty.
        val actionUrl: URL
        try {
            actionUrl = URL(actionUriString)
        } catch (e: java.net.MalformedURLException) {
            val errorMessage = "Malformed action URI: '$actionUriString'"
            Logger.error(methodTag, errorMessage, e)
            throw ClientException(ClientException.MALFORMED_URL, errorMessage, e)
        }
        if (!AzureActiveDirectory.isValidCloudHost(actionUrl)) {
            val errorMessage = "Authority '${actionUrl.host}' is not a valid AAD authority"
            val exception = ClientException(ClientException.UNKNOWN_AUTHORITY, errorMessage)
            Logger.error(methodTag, errorMessage, exception)
            throw exception
        }
    }
}
