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
import android.os.Bundle
import android.os.Looper
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.SWITCH_BROWSER
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory
import com.microsoft.identity.common.logging.Logger
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
     * Extracts switch-browser resume query parameters from a URI and returns them in a Bundle.
     *
     * Extracts the following query parameters from the resume redirect URI:
     * - [SWITCH_BROWSER.ACTION_URI] - The broker action URI from the resume response
     * - [SWITCH_BROWSER.CODE] - The authorization code from the resume response
     * - [SWITCH_BROWSER.STATE] - The state parameter from the resume response
     * - [SWITCH_BROWSER.RESUME_REQUEST] - Set to `true` to indicate this is a resume delivery
     *
     * @param uri The resume redirect URI containing authentication response parameters
     * @return A [Bundle] containing the extracted switch-browser resume parameters
     */
    fun extractSwitchBrowserResumeParamsAsBundle(uri: Uri): Bundle {
        return Bundle().apply {
            putString(
                SWITCH_BROWSER.ACTION_URI,
                uri.getQueryParameter(SWITCH_BROWSER.ACTION_URI)
            )
            putString(
                SWITCH_BROWSER.CODE,
                uri.getQueryParameter(SWITCH_BROWSER.CODE)
            )
            putString(
                SWITCH_BROWSER.STATE,
                uri.getQueryParameter(SWITCH_BROWSER.STATE)
            )
            putBoolean(
                SWITCH_BROWSER.RESUME_REQUEST,
                true
            )
        }
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
     *
     * On mismatch this throws a [ClientException] with [ClientException.STATE_MISMATCH].
     * Span/telemetry concerns are intentionally left to the caller — this helper does
     * not touch the current OpenTelemetry span. Callers that wrap this in a span scope
     * are responsible for recording the exception and ending their span exactly once.
     */
    @Throws(ClientException::class)
    fun statesMatch(authorizationUrl: String, state: String?) {
        val methodTag = "$TAG:statesMatch"
        if (!STATE_VALIDATION_REQUIRED) {
            Logger.info(methodTag, "State validation is not required.")
            return
        }
        if (state.isNullOrEmpty()) {
            throw ClientException(
                ClientException.STATE_MISMATCH,
                "State is null."
            )
        }
        val authRequestState = authorizationUrl.toUri().getQueryParameter(SWITCH_BROWSER.STATE)
        if (authRequestState.isNullOrEmpty()) {
            throw ClientException(
                ClientException.STATE_MISMATCH,
                "Authorization request state is null."
            )
        }
        if (state != authRequestState) {
            throw ClientException(
                ClientException.STATE_MISMATCH,
                "State does not match with the auth request state."
            )
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
        // Cloud discovery below may issue a network call (cache miss). Running it on the
        // main thread will crash with NetworkOnMainThreadException whenever the AAD cloud
        // metadata cache is cold. All call paths that reach this helper (buildProcessUri /
        // buildResumeUri, both invoked from SwitchBrowserProtocolCoordinator) MUST hop to a
        // background dispatcher before invoking the URI builders. The async entry points
        // SwitchBrowserProtocolCoordinator.processSwitchBrowserRedirectAsync and
        // processSwitchBrowserResumeAsync exist for that reason. We log loudly here
        // (rather than throwing) so callers that happen to be exercising a warm cache still
        // succeed during rollout, but any regression is visible in logs/telemetry.
        warnIfOnMainThread(methodTag)
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

    /**
     * Logs an error if [validateActionUri] runs on the main thread. Cloud discovery
     * underneath may hit the network on a cold cache. Log-only (not a throw) to preserve
     * the warm-cache happy path during rollout.
     */
    private fun warnIfOnMainThread(methodTag: String) {
        val mainLooper = Looper.getMainLooper() ?: return // null in unit tests without Robolectric
        if (mainLooper === Looper.myLooper()) {
            Logger.error(
                methodTag,
                "validateActionUri invoked on the main thread; cloud discovery may trigger " +
                    "NetworkOnMainThreadException on cache miss. Call sites must hop to a " +
                    "background dispatcher (see SwitchBrowser*Async methods).",
                Throwable("Main-thread invocation of SwitchBrowserUriHelper.validateActionUri")
            )
        }
    }
}
