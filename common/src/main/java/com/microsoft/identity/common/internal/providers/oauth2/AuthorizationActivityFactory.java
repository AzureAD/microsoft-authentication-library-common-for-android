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
package com.microsoft.identity.common.internal.providers.oauth2;


import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.AUTHORIZATION_AGENT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.AUTH_INTENT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REDIRECT_URI;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REQUEST_HEADERS;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REQUEST_URL;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.WEB_VIEW_ZOOM_CONTROLS_ENABLED;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.WEB_VIEW_ZOOM_ENABLED;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.CLIENT_ID;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.SWITCH_BROWSER;
import static com.microsoft.identity.common.java.AuthenticationConstants.SdkPlatformFields.PRODUCT;
import static com.microsoft.identity.common.java.AuthenticationConstants.SdkPlatformFields.VERSION;
import static com.microsoft.identity.common.java.logging.DiagnosticContext.CORRELATION_ID;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.microsoft.identity.common.internal.msafederation.MsaFederationExtensions;
import com.microsoft.identity.common.internal.msafederation.google.SignInWithGoogleCredential;
import com.microsoft.identity.common.internal.msafederation.google.SignInWithGoogleParameters;
import com.microsoft.identity.common.internal.msafederation.google.SignInWithGoogleApi;
import com.microsoft.identity.common.internal.util.CommonMoshiJsonAdapter;
import com.microsoft.identity.common.internal.util.ProcessUtil;
import com.microsoft.identity.common.java.configuration.LibraryConfiguration;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.logging.DiagnosticContext;
import com.microsoft.identity.common.java.opentelemetry.SerializableSpanContext;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;
import com.microsoft.identity.common.java.ui.AuthorizationAgent;
import com.microsoft.identity.common.java.util.CommonURIBuilder;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Constructs intents and/or fragments for interactive requests based on library configuration and current request.
 */
public class AuthorizationActivityFactory {


    /**
     * Return the correct authorization activity based on library configuration.
     *
     * @param parameters The parameters to use to create the intent.
     * @return An android Intent which will be used by Android to create an AuthorizationActivity
     */
    public static Intent getAuthorizationActivityIntent(final @NonNull AuthorizationActivityParameters parameters) {
        final Intent intent;
        final LibraryConfiguration libraryConfig = LibraryConfiguration.getInstance();
        if (ProcessUtil.isBrokerProcess(parameters.getContext())) {
            intent = new Intent(parameters.getContext(), BrokerAuthorizationActivity.class);
            if (parameters.getRequestUrl().contains(SWITCH_BROWSER.PATH)) {
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                // In the case of a SwitchBrowser protocol, we need to transition from the browser to the WebView.
                // These flags ensure that we have a new task stack that allows for this transition.
            }
        } else if (libraryConfig.isAuthorizationInCurrentTask() && !parameters.getAuthorizationAgent().equals(AuthorizationAgent.WEBVIEW)) {
            // We exclude the case when the authorization agent is already selected as WEBVIEW because of confusion
            // that results from attempting to use the CurrentTaskAuthorizationActivity in that case, because as webview
            // already uses the current task, attempting to manually simulate that behavior ends up supplying an incorrect
            // Fragment to the activity.
            intent = new Intent(parameters.getContext(), CurrentTaskAuthorizationActivity.class);
        } else {
            intent = new Intent(parameters.getContext(), AuthorizationActivity.class);
        }

        intent.putExtra(AUTH_INTENT, parameters.getAuthIntent());
        intent.putExtra(REQUEST_URL, parameters.getRequestUrl());
        intent.putExtra(CLIENT_ID, parameters.getClientId());
        intent.putExtra(REDIRECT_URI, parameters.getRedirectUri());
        intent.putExtra(REQUEST_HEADERS, parameters.getRequestHeader());
        intent.putExtra(AUTHORIZATION_AGENT, parameters.getAuthorizationAgent());
        intent.putExtra(WEB_VIEW_ZOOM_CONTROLS_ENABLED, parameters.getWebViewZoomControlsEnabled());
        intent.putExtra(WEB_VIEW_ZOOM_ENABLED, parameters.getWebViewZoomEnabled());
        intent.putExtra(CORRELATION_ID, DiagnosticContext.INSTANCE.getRequestContext().get(DiagnosticContext.CORRELATION_ID));
        if (parameters.getSourceLibraryName() != null) {
            intent.putExtra(PRODUCT, parameters.getSourceLibraryName());
        }
        if (parameters.getSourceLibraryVersion() != null) {
            intent.putExtra(VERSION, parameters.getSourceLibraryVersion());
        }
        intent.putExtra(SerializableSpanContext.SERIALIZABLE_SPAN_CONTEXT, new CommonMoshiJsonAdapter().toJson(
                        SerializableSpanContext.builder()
                                .traceId(SpanExtension.current().getSpanContext().getTraceId())
                                .spanId(SpanExtension.current().getSpanContext().getSpanId())
                                .traceFlags(SpanExtension.current().getSpanContext().getTraceFlags().asByte())
                                .build()
                )
        );
        return intent;
    }

    /**
     * Returns the correct authorization fragment for local (non-broker) authorization flows.
     * Fragments include:
     * {@link WebViewAuthorizationFragment}
     * {@link BrowserAuthorizationFragment}
     * {@link CurrentTaskBrowserAuthorizationFragment}
     *
     * @param intent The intent used to start the authorization flow.
     * @return returns an Fragment that's used as to authorize a token request.
     */
    public static Fragment getAuthorizationFragmentFromStartIntent(@NonNull final Intent intent) {
        Fragment fragment;
        final AuthorizationAgent authorizationAgent = (AuthorizationAgent) intent.getSerializableExtra(AUTHORIZATION_AGENT);

        final LibraryConfiguration libraryConfig = LibraryConfiguration.getInstance();

        if (authorizationAgent == AuthorizationAgent.WEBVIEW) {
            fragment = new WebViewAuthorizationFragment();
        } else {
            if (libraryConfig.isAuthorizationInCurrentTask()) {
                fragment = new CurrentTaskBrowserAuthorizationFragment();
            } else {
                fragment = new BrowserAuthorizationFragment();
            }
        }

        return fragment;
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
    public static Intent signInWithGoogleAndGetAuthorizationActivityIntent(
            @NonNull final AuthorizationActivityParameters authorizationActivityParameters,
            @NonNull final SignInWithGoogleParameters signInWithGoogleParameters) throws ClientException {
        final SignInWithGoogleCredential signInWithGoogleCredential = SignInWithGoogleApi.getInstance().signInSync(signInWithGoogleParameters);
        // add header
        final HashMap<String, String> authorizationActivityRequestHeaders = authorizationActivityParameters.getRequestHeader();
        final HashMap<String, String> requestHeadersWithGoogleAuthCredential = authorizationActivityRequestHeaders == null ? new HashMap<>() : new HashMap<>(authorizationActivityRequestHeaders);
        requestHeadersWithGoogleAuthCredential.putAll(MsaFederationExtensions.getIdProviderHeadersForAuthorization(signInWithGoogleCredential));

        // add id provider query parameter
        final String requestUrlWithIdProvider;
        try {
            final CommonURIBuilder uriBuilder = new CommonURIBuilder(authorizationActivityParameters.getRequestUrl());
            final Map.Entry<String, String> extraQueryParamForAuthorization = MsaFederationExtensions.getIdProviderExtraQueryParamForAuthorization(signInWithGoogleCredential);
            uriBuilder.addParameterIfAbsent(extraQueryParamForAuthorization.getKey(), extraQueryParamForAuthorization.getValue());
            requestUrlWithIdProvider = uriBuilder.build().toString();
        } catch (final URISyntaxException e) {
            throw new ClientException(ClientException.MALFORMED_URL, "Failed to add id provider query parameter to request URL", e);
        }
        final AuthorizationActivityParameters newAuthorizationActivityParameters = new AuthorizationActivityParameters(
                authorizationActivityParameters.getContext(),
                authorizationActivityParameters.getAuthIntent(),
                requestUrlWithIdProvider,
                authorizationActivityParameters.getRedirectUri(),
                requestHeadersWithGoogleAuthCredential,
                authorizationActivityParameters.getAuthorizationAgent(),
                authorizationActivityParameters.getClientId(),
                authorizationActivityParameters.getWebViewZoomEnabled(),
                authorizationActivityParameters.getWebViewZoomControlsEnabled(),
                authorizationActivityParameters.getSourceLibraryName(),
                authorizationActivityParameters.getSourceLibraryVersion()
        );
        return getAuthorizationActivityIntent(newAuthorizationActivityParameters);
    }
}
