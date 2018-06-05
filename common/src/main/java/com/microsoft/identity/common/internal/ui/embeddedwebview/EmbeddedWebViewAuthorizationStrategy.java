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
package com.microsoft.identity.common.internal.ui.embeddedwebview;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.ui.AuthorizationConfiguration;


public class EmbeddedWebViewAuthorizationStrategy extends AuthorizationStrategy {
    private WebView mWebView;
    private String mStartUrl;

    public AuthorizationResult requestAuthorization (AuthorizationRequest request) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    EmbeddedWebViewAuthorizationStrategy(Activity activity, AuthorizationRequest request) {
        if (activity == null || request == null) {
            throw new IllegalArgumentException("Null activity or request");
        }

        //TODO validate auth request
        setupWebView(activity, request);
        setupStartURL();

    }

    private void setupWebView(final Activity activity, final AuthorizationRequest request) {
        // Create the Web View to show the page
        mWebView = (WebView)activity.findViewById(activity.getResources().getIdentifier("webView1", "id",
                activity.getPackageName()));
        mStartUrl = "about:blank";
        mWebView.getSettings().setUserAgentString(
                mWebView.getSettings().getUserAgentString() + AuthenticationConstants.Broker.CLIENT_TLS_NOT_SUPPORTED);

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.requestFocus(View.FOCUS_DOWN);

        // Set focus to the view for touch event
        mWebView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                int action = event.getAction();
                if ((action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP) && !view.hasFocus()) {
                        view.requestFocus();
                }
                return false;
            }
        });

        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.setVisibility(View.INVISIBLE);

        selectWebViewClient(activity, request);
    }

    private void selectWebViewClient (final Activity activity, final AuthorizationRequest request) {
        // if it is an ADAL request, we set the AADWebViewClient
        // if it is a MSAL request, we set the MSSTSWebviewClient
        // if it is a broker request, we set the BrokerWebviewClient and broker always talk to MSAL

        if(AuthorizationConfiguration.getInstance().isADALRequest() &&
                request instanceof AzureActiveDirectoryAuthorizationRequest) {
            mWebView.setWebViewClient(new AADWebViewClient(activity, request.getRedirectUri(), (AzureActiveDirectoryAuthorizationRequest)request));
        }

        if(AuthorizationConfiguration.getInstance().isMSALRequest() &&
                request instanceof MicrosoftStsAuthorizationRequest) {
            mWebView.setWebViewClient(new MSSTSWebViewClient(activity, request.getRedirectUri(), (MicrosoftStsAuthorizationRequest)request));
        }

        if(AuthorizationConfiguration.getInstance().isBrokerRequest() &&
                request instanceof MicrosoftStsAuthorizationRequest) {
            mWebView.setWebViewClient(new MSSTSWebViewClient(activity, request.getRedirectUri(), (MicrosoftStsAuthorizationRequest)request));
        }
    }

    private void setupStartURL() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    void loadURL() {
        mWebView.post(new Runnable() {
            @Override
            public void run() {
                // load blank first to avoid error for not loading webview
                mWebView.loadUrl("about:blank");
                mWebView.loadUrl(mStartUrl);
            }
        });
    }

}
