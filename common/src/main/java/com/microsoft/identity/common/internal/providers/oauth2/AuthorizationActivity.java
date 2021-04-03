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
import androidx.annotation.Nullable;

import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.telemetry.events.UiStartEvent;
import com.microsoft.identity.common.internal.ui.AuthorizationAgent;
import com.microsoft.identity.common.internal.ui.DualScreenActivity;
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

public class AuthorizationActivity extends DualScreenActivity {

    private AuthorizationFragment mFragment;

    public static Intent createStartIntent(final Context context,
                                           final Intent authIntent,
                                           final String requestUrl,
                                           final String redirectUri,
                                           final HashMap<String, String> requestHeaders,
                                           final AuthorizationAgent authorizationAgent,
                                           final boolean webViewZoomEnabled,
                                           final boolean webViewZoomControlsEnabled) {
        Intent intent;
        if (ProcessUtil.isBrokerProcess(context)) {
            intent = new Intent(context, BrokerAuthorizationActivity.class);
        } else {
            intent = new Intent(context, AuthorizationActivity.class) {

                @Override
                public Object clone() {
                    return super.clone();
                }
            };
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

    public static AuthorizationFragment getAuthorizationFragmentFromStartIntent(@NonNull final Intent intent) {
        AuthorizationFragment fragment;
        final AuthorizationAgent authorizationAgent = (AuthorizationAgent) intent.getSerializableExtra(AUTHORIZATION_AGENT);
        Telemetry.emit(new UiStartEvent().putUserAgent(authorizationAgent));

        if (authorizationAgent == AuthorizationAgent.WEBVIEW) {
            fragment = new WebViewAuthorizationFragment();
        } else {
            fragment = new BrowserAuthorizationFragment();
        }

        fragment.setInstanceState(intent.getExtras());
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFragment = getAuthorizationFragmentFromStartIntent(getIntent());
        setFragment(mFragment);
    }

    @Override
    public void onBackPressed() {
        if (!mFragment.onBackPressed()) {
            super.onBackPressed();
        }
    }
}