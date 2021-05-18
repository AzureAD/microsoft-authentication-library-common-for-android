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

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.microsoft.identity.common.internal.configuration.MsalConfiguration;
import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.telemetry.events.UiStartEvent;
import com.microsoft.identity.common.internal.ui.AuthorizationAgent;
import com.microsoft.identity.common.internal.util.ProcessUtil;
import com.microsoft.identity.common.logging.DiagnosticContext;

import java.util.HashMap;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.AUTHORIZATION_AGENT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.AUTH_INTENT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REDIRECT_URI;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REQUEST_HEADERS;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REQUEST_URL;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.WEB_VIEW_ZOOM_CONTROLS_ENABLED;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.WEB_VIEW_ZOOM_ENABLED;

public class AuthorizationActivityFactory {

    /**
     * Return the correct authorization activty based on library configuration
     * @param context
     * @param authIntent
     * @param requestUrl
     * @param redirectUri
     * @param requestHeaders
     * @param authorizationAgent
     * @param webViewZoomEnabled
     * @param webViewZoomControlsEnabled
     * @return
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
        final MsalConfiguration libraryConfig = MsalConfiguration.getInstance();
        if (ProcessUtil.isBrokerProcess(context)) {
            intent = new Intent(context, BrokerAuthorizationActivity.class);
        } else {
            if(libraryConfig.isAuthorizationInCurrentTask()) {
                intent = new Intent(context, CurrentTaskAuthorizationActivity.class);
            }else{
                intent = new Intent(context, AuthorizationActivity.class);
            }
        }

        intent.putExtra(AUTH_INTENT, authIntent);
        intent.putExtra(REQUEST_URL, requestUrl);
        intent.putExtra(REDIRECT_URI, redirectUri);
        intent.putExtra(REQUEST_HEADERS, requestHeaders);
        intent.putExtra(AUTHORIZATION_AGENT, authorizationAgent);
        intent.putExtra(WEB_VIEW_ZOOM_CONTROLS_ENABLED, webViewZoomControlsEnabled);
        intent.putExtra(WEB_VIEW_ZOOM_ENABLED, webViewZoomEnabled);
        intent.putExtra(DiagnosticContext.CORRELATION_ID, DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID));
        return intent;
    }

    /**
     *
     * @param intent
     * @return
     */
    public static Fragment getAuthorizationFragmentFromStartIntent(@NonNull final Intent intent) {
        Fragment fragment;
        final AuthorizationAgent authorizationAgent = (AuthorizationAgent) intent.getSerializableExtra(AUTHORIZATION_AGENT);
        Telemetry.emit(new UiStartEvent().putUserAgent(authorizationAgent));

        final MsalConfiguration libraryConfig = MsalConfiguration.getInstance();

        if (authorizationAgent == AuthorizationAgent.WEBVIEW) {
            fragment = new WebViewAuthorizationFragment();
        } else {
            if(libraryConfig.isAuthorizationInCurrentTask()) {
                fragment = new CurrentTaskBrowserAuthorizationFragment();
            }else{
                fragment = new BrowserAuthorizationFragment();
            }
        }

        return fragment;
    }

}
