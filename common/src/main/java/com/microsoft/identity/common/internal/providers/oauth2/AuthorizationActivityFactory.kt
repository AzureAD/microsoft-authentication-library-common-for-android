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
package com.microsoft.identity.common.internal.providers.oauth2

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.microsoft.identity.common.adal.internal.AuthenticationConstants
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.OTEL_CONTEXT_CARRIER
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.SWITCH_BROWSER
import com.microsoft.identity.common.internal.msafederation.getIdProviderExtraQueryParamForAuthorization
import com.microsoft.identity.common.internal.msafederation.getIdProviderHeadersForAuthorization
import com.microsoft.identity.common.internal.msafederation.google.SignInWithGoogleApi.Companion.getInstance
import com.microsoft.identity.common.internal.msafederation.google.SignInWithGoogleCredential
import com.microsoft.identity.common.internal.msafederation.google.SignInWithGoogleParameters
import com.microsoft.identity.common.internal.ui.browser.AndroidBrowserSelector
import com.microsoft.identity.common.internal.util.CommonMoshiJsonAdapter
import com.microsoft.identity.common.internal.util.ProcessUtil
import com.microsoft.identity.common.java.AuthenticationConstants.OAuth2.UTID
import com.microsoft.identity.common.java.AuthenticationConstants.SdkPlatformFields.PRODUCT
import com.microsoft.identity.common.java.AuthenticationConstants.SdkPlatformFields.VERSION
import com.microsoft.identity.common.java.configuration.LibraryConfiguration
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.logging.DiagnosticContext
import com.microsoft.identity.common.java.opentelemetry.OtelContextExtension
import com.microsoft.identity.common.java.opentelemetry.SerializableSpanContext
import com.microsoft.identity.common.java.opentelemetry.SpanExtension
import com.microsoft.identity.common.java.opentelemetry.TextMapPropagatorExtension
import com.microsoft.identity.common.java.ui.AuthorizationAgent
import com.microsoft.identity.common.java.ui.BrowserDescriptor
import com.microsoft.identity.common.java.util.CommonURIBuilder
import com.microsoft.identity.common.logging.Logger
import java.net.URISyntaxException


/**
 * Constructs intents and/or fragments for interactive requests based on library configuration and current request.
 */
object AuthorizationActivityFactory {

    private val TAG: String = AuthorizationActivityFactory::class.java.simpleName

    /**
     * Return the correct authorization activity based on library configuration.
     *
     * @param parameters The parameters to use to create the intent.
     * @return An android Intent which will be used by Android to create an AuthorizationActivity
     */
    @JvmStatic
    fun getAuthorizationActivityIntent(parameters: AuthorizationActivityParameters): Intent {
        val intent: Intent
        val libraryConfig = LibraryConfiguration.getInstance()
        if (ProcessUtil.isBrokerProcess(parameters.context)) {
            intent = Intent(parameters.context, BrokerAuthorizationActivity::class.java)
        } else if (libraryConfig.isAuthorizationInCurrentTask && parameters.authorizationAgent != AuthorizationAgent.WEBVIEW) {
            // We exclude the case when the authorization agent is already selected as WEBVIEW because of confusion
            // that results from attempting to use the CurrentTaskAuthorizationActivity in that case, because as webview
            // already uses the current task, attempting to manually simulate that behavior ends up supplying an incorrect
            // Fragment to the activity.
            intent = Intent(parameters.context, CurrentTaskAuthorizationActivity::class.java)
        } else if (parameters.webViewEnableSilentAuthorizationFlowTimeOutMs != null){
            intent = Intent(parameters.context, SilentAuthorizationActivity::class.java)
            intent.putExtra(
                AuthenticationConstants.AuthorizationIntentKey.WEB_VIEW_SILENT_AUTHORIZATION_FLOW_TIMEOUT,
                parameters.webViewEnableSilentAuthorizationFlowTimeOutMs
            )
        } else {
            intent = Intent(parameters.context, AuthorizationActivity::class.java)
        }

        // If switch browser is enabled, check browser availability and append switch_browser=1
        val effectiveRequestUrl = if (parameters.enableSwitchBrowser) {
            appendSwitchBrowserParam(parameters.context, parameters.requestUrl)
        } else {
            parameters.requestUrl
        }

        intent.apply {
            putExtra(
                AuthenticationConstants.AuthorizationIntentKey.AUTH_INTENT,
                parameters.authIntent
            )
            putExtra(
                AuthenticationConstants.AuthorizationIntentKey.REQUEST_URL,
                effectiveRequestUrl
            )
            putExtra(
                AuthenticationConstants.AuthorizationIntentKey.REDIRECT_URI,
                parameters.redirectUri
            )
            putExtra(
                AuthenticationConstants.AuthorizationIntentKey.REQUEST_HEADERS,
                parameters.requestHeader
            )
            putExtra(
                AuthenticationConstants.AuthorizationIntentKey.AUTHORIZATION_AGENT,
                parameters.authorizationAgent
            )
            putExtra(
                AuthenticationConstants.AuthorizationIntentKey.WEB_VIEW_ZOOM_CONTROLS_ENABLED,
                parameters.webViewZoomControlsEnabled
            )
            putExtra(
                AuthenticationConstants.AuthorizationIntentKey.WEB_VIEW_ZOOM_ENABLED,
                parameters.webViewZoomEnabled
            )
            putExtra(
                AuthenticationConstants.AuthorizationIntentKey.WEB_VIEW_WEB_CP_ENABLED,
                parameters.isWebViewWebCpEnabled
            )
            putExtra(
                DiagnosticContext.CORRELATION_ID,
                DiagnosticContext.INSTANCE.requestContext[DiagnosticContext.CORRELATION_ID]
            )
            putExtra(
                SerializableSpanContext.SERIALIZABLE_SPAN_CONTEXT, CommonMoshiJsonAdapter().toJson(
                    SerializableSpanContext.builder()
                        .traceId(SpanExtension.current().spanContext.traceId)
                        .spanId(SpanExtension.current().spanContext.spanId)
                        .traceFlags(SpanExtension.current().spanContext.traceFlags.asByte())
                        .build()
                )
            )
            putExtra(
                OTEL_CONTEXT_CARRIER,
                TextMapPropagatorExtension.inject(OtelContextExtension.current())
            )
            if (parameters.sourceLibraryName != null) {
                putExtra(PRODUCT, parameters.sourceLibraryName)
            }
            if (parameters.sourceLibraryVersion != null) {
                putExtra(VERSION, parameters.sourceLibraryVersion)
            }
            if (parameters.utid != null) {
                putExtra(UTID, parameters.utid)
            }
        }
        return intent
    }

    /**
     * Returns the correct authorization fragment for local (non-broker) authorization flows.
     * Fragments include:
     * [WebViewAuthorizationFragment]
     * [BrowserAuthorizationFragment]
     * [CurrentTaskBrowserAuthorizationFragment]
     *
     * Note: multiple-app URL scheme validation is NOT performed here. It is performed upstream
     * in [AndroidAuthorizationStrategy.launchIntent], where a checked [ClientException] can
     * propagate correctly through the command pipeline. Callers should ensure validation has
     * already been performed before invoking this factory method for browser flows.
     *
     * @param intent The intent used to start the authorization flow.
     * @return returns an Fragment that's used as to authorize a token request.
     */
    @JvmStatic
    fun getAuthorizationFragmentFromStartIntent(intent: Intent): Fragment {
        val fragment: Fragment
        val authorizationAgent =
            intent.getSerializableExtra(AuthenticationConstants.AuthorizationIntentKey.AUTHORIZATION_AGENT) as AuthorizationAgent?

        val libraryConfig = LibraryConfiguration.getInstance()

        fragment =
            if (authorizationAgent == AuthorizationAgent.WEBVIEW) {
                if (intent.hasExtra(AuthenticationConstants.AuthorizationIntentKey.WEB_VIEW_SILENT_AUTHORIZATION_FLOW_TIMEOUT)) {
                    SilentWebViewAuthorizationFragment()
                } else {
                    WebViewAuthorizationFragment()
                }
            } else {
                if (libraryConfig.isAuthorizationInCurrentTask) {
                    CurrentTaskBrowserAuthorizationFragment()
                } else {
                    BrowserAuthorizationFragment()
                }
            }

        return fragment
    }

    /**
     * This method first starts sign in with google flow displaying UX for user add/select a google account
     * and after success creates intent with result obtained from successful google sign in and other input
     * parameters.
     *
     * @param authorizationActivityParameters Parameters to create the auth intent
     * @param signInWithGoogleParameters      Parameters to first start sign in with google flow before creating the intent
     * @return An android Intent which will be used by Android to create an AuthorizationActivity
     */
    @JvmStatic
    fun signInWithGoogleAndGetAuthorizationActivityIntent(
        authorizationActivityParameters: AuthorizationActivityParameters,
        signInWithGoogleParameters: SignInWithGoogleParameters
    ): Intent {
        return getAuthorizationActivityIntent(
            authorizationActivityParameters,
            getInstance().signInSync(signInWithGoogleParameters)
        )
    }

    /**
     * This method first starts sign in with google flow displaying UX for user add/select a google account
     * and after success creates intent with result obtained from successful google sign in and other input
     * parameters.
     *
     * @param authorizationActivityParameters Parameters to create the auth intent
     * @return An android Intent which will be used by Android to create an AuthorizationActivity
     */
    @JvmStatic
    @Throws(ClientException::class)
    fun getAuthorizationActivityIntent(
        authorizationActivityParameters: AuthorizationActivityParameters,
        signInWithGoogleCredential: SignInWithGoogleCredential
    ): Intent {

        // add header
        val requestHeadersWithGoogleAuthCredential =
            if (authorizationActivityParameters.requestHeader.isNullOrEmpty()) {
                HashMap()
            } else {
                HashMap(authorizationActivityParameters.requestHeader)
            }
        requestHeadersWithGoogleAuthCredential.putAll(signInWithGoogleCredential.getIdProviderHeadersForAuthorization())

        // add id provider query parameter
        val requestUrlWithIdProvider: String
        try {
            val uriBuilder = CommonURIBuilder(authorizationActivityParameters.requestUrl)
            val extraQueryParamForAuthorization =
                signInWithGoogleCredential.getIdProviderExtraQueryParamForAuthorization()
            uriBuilder.addParameterIfAbsent(
                extraQueryParamForAuthorization.key,
                extraQueryParamForAuthorization.value
            )
            requestUrlWithIdProvider = uriBuilder.build().toString()
        } catch (e: URISyntaxException) {
            throw ClientException(
                ClientException.MALFORMED_URL,
                "Failed to add id provider query parameter to request URL",
                e
            )
        }
        val newAuthorizationActivityParameters = authorizationActivityParameters.copy(
            requestUrl = requestUrlWithIdProvider,
            requestHeader = requestHeadersWithGoogleAuthCredential
        )
        return getAuthorizationActivityIntent(newAuthorizationActivityParameters)
    }

    /**
     * OnaAuth method
     * Returns the correct authorization fragment for local (non-broker) authorization flows,
     * supplying a start bundle for the Fragment state.
     * Fragments include:
     * [WebViewAuthorizationFragment]
     * [BrowserAuthorizationFragment]
     * [CurrentTaskBrowserAuthorizationFragment]
     *
     * @param intent the intent to use to create the fragment.
     * @param bundle the bundle to add to the Fragment if it is an AuthorizationFragment.
     * @return returns an Fragment that's used as to authorize a token request.
     */
    @JvmStatic
    fun getAuthorizationFragmentFromStartIntentWithState(intent: Intent, bundle: Bundle): Fragment {
        val fragment = getAuthorizationFragmentFromStartIntent(intent)
        if (fragment is AuthorizationFragment) {
            fragment.setInstanceState(bundle)
        }
        return fragment
    }

    /**
     * Checks if a browser compatible with the Switch Browser protocol is available on the device.
     * If so, appends `switch_browser=1` to the request URL to signal the server that the client
     * supports the Switch Browser protocol.
     *
     * @param context Android context for browser resolution.
     * @param requestUrl The original authorization request URL.
     * @return The request URL with `switch_browser=1` appended if a compatible browser is available,
     *         or the original URL if no compatible browser is found.
     */
    private fun appendSwitchBrowserParam(context: android.content.Context, requestUrl: String): String {
        val methodTag = "$TAG:appendSwitchBrowserParam"
        try {
            val browserSelector = AndroidBrowserSelector(context)
            val browser = browserSelector.selectBrowser(
                BrowserDescriptor.getBrowserSafeListForSwitchBrowser(),
                null
            )
            if (browser != null) {
                Logger.info(methodTag, "Compatible browser found for Switch Browser: ${browser.packageName}")
                val uriBuilder = CommonURIBuilder(requestUrl)
                uriBuilder.addParameterIfAbsent(SWITCH_BROWSER.SWITCH_BROWSER_EXTRA_QUERY_PARAM, "1")
                return uriBuilder.build().toString()
            } else {
                Logger.info(methodTag, "No compatible browser found for Switch Browser protocol.")
            }
        } catch (e: Exception) {
            Logger.warn(methodTag, "Failed to check browser availability for Switch Browser: ${e.message}")
        }
        return requestUrl
    }
}
