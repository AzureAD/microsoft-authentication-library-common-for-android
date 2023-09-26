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


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.microsoft.identity.common.java.configuration.LibraryConfiguration;
import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.telemetry.events.UiStartEvent;
import com.microsoft.identity.common.java.ui.AuthorizationAgent;
import com.microsoft.identity.common.internal.util.ProcessUtil;
import com.microsoft.identity.common.java.logging.DiagnosticContext;

import java.util.HashMap;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.AUTHORIZATION_AGENT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.AUTH_INTENT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REDIRECT_URI;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REQUEST_HEADERS;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REQUEST_URL;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.WEB_VIEW_ZOOM_CONTROLS_ENABLED;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.WEB_VIEW_ZOOM_ENABLED;
import static com.microsoft.identity.common.java.logging.DiagnosticContext.CORRELATION_ID;

/**
 * Constructs intents and/or fragments for interactive requests based on library configuration and current request.
 */
public class AuthorizationActivityFactory {

    /**
     * Return the correct authorization activity based on library configuration.
     *
     * @param context                    Android application context
     * @param authIntent                 Android intent used by the authorization activity to launch the specific implementation of authorization (BROWSER, EMBEDDED)
     * @param requestUrl                 The authorization request in URL format
     * @param redirectUri                The expected redirect URI associated with the authorization request
     * @param requestHeaders             Additional HTTP headers included with the authorization request
     * @param authorizationAgent         The means by which authorization should be performed (EMBEDDED, WEBVIEW) NOTE: This should move to library configuration
     * @param webViewZoomEnabled         This parameter is specific to embedded and controls whether webview zoom is enabled... NOTE: Needs refactoring
     * @param webViewZoomControlsEnabled This parameter is specific to embedded and controls whether webview zoom controls are enabled... NOTE: Needs refactoring
     * @return An android Intent which will be used by Android to create an AuthorizationActivity
     */
    public static Intent getAuthorizationActivityIntent(final Context context,
                                                        final Intent authIntent,
                                                        final String requestUrl,
                                                        final String redirectUri,
                                                        final HashMap<String, String> requestHeaders,
                                                        final AuthorizationAgent authorizationAgent,
                                                        final boolean webViewZoomEnabled,
                                                        final boolean webViewZoomControlsEnabled) {
        Intent intent;
        final LibraryConfiguration libraryConfig = LibraryConfiguration.getInstance();
        if (ProcessUtil.isBrokerProcess(context)) {
            intent = new Intent(context, BrokerAuthorizationActivity.class);
        } else if (libraryConfig.isAuthorizationInCurrentTask() && !authorizationAgent.equals(AuthorizationAgent.WEBVIEW)) {
        // We exclude the case when the authorization agent is already selected as WEBVIEW because of confusion
        // that results from attempting to use the CurrentTaskAuthorizationActivity in that case, because as webview
        // already uses the current task, attempting to manually simulate that behavior ends up supplying an incorrect
        // Fragment to the activity.
                intent = new Intent(context, CurrentTaskAuthorizationActivity.class);
        } else {
                intent = new Intent(context, AuthorizationActivity.class);
        }

        intent.putExtra(AUTH_INTENT, authIntent);
        intent.putExtra(REQUEST_URL, requestUrl);
        intent.putExtra(REDIRECT_URI, redirectUri);
        intent.putExtra(REQUEST_HEADERS, requestHeaders);
        intent.putExtra(AUTHORIZATION_AGENT, authorizationAgent);
        intent.putExtra(WEB_VIEW_ZOOM_CONTROLS_ENABLED, webViewZoomControlsEnabled);
        intent.putExtra(WEB_VIEW_ZOOM_ENABLED, webViewZoomEnabled);
        intent.putExtra(CORRELATION_ID, DiagnosticContext.INSTANCE.getRequestContext().get(DiagnosticContext.CORRELATION_ID));
        return intent;
    }

    /**
     * Returns the correct authorization fragment for local (non-broker) authorization flows.
     * Fragments include:
     * {@link WebViewAuthorizationFragment}
     * {@link BrowserAuthorizationFragment}
     * {@link CurrentTaskBrowserAuthorizationFragment}
     *
     * @param intent
     * @return returns an Fragment that's used as to authorize a token request.
     */
    public static Fragment getAuthorizationFragmentFromStartIntent(@NonNull final Intent intent) {
        Fragment fragment;
        final AuthorizationAgent authorizationAgent = (AuthorizationAgent) intent.getSerializableExtra(AUTHORIZATION_AGENT);
        Telemetry.emit(new UiStartEvent().putUserAgent(authorizationAgent));

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
     * Returns the correct authorization fragment for local (non-broker) authorization flows,
     * supplying a start bundle for the Fragment state.
     * Fragments include:
     * {@link WebViewAuthorizationFragment}
     * {@link BrowserAuthorizationFragment}
     * {@link CurrentTaskBrowserAuthorizationFragment}
     *
     * @param intent the intent to use to create the fragment.
     * @param bundle the bundle to add to the Fragment if it is an AuthorizationFragment.
     * @return returns an Fragment that's used as to authorize a token request.
     */
    public static Fragment getAuthorizationFragmentFromStartIntentWithState(@NonNull final Intent intent,
                                                                            @NonNull final Bundle bundle) {
        final Fragment fragment = getAuthorizationFragmentFromStartIntent(intent);
        if (fragment instanceof AuthorizationFragment) {
            final AuthorizationFragment authFragment = (AuthorizationFragment) fragment;
            authFragment.setInstanceState(bundle);
        }
        return fragment;
    }
}
