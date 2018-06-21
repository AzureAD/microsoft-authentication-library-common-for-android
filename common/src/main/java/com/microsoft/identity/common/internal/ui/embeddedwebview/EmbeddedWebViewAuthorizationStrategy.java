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
import android.support.annotation.NonNull;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;

import java.io.UnsupportedEncodingException;

/**
 * Serve as a class to do the OAuth2 auth code grant flow with Android embedded web view.
 */
public class EmbeddedWebViewAuthorizationStrategy extends AuthorizationStrategy {
    private static final String TAG = StringExtensions.class.getSimpleName();
    private WebView mWebView;
    private String mStartUrl;

    /**
     * Perform the authorization request in the embedded web view and return the authorization result.
     *
     * @param request authorization request
     * @return AuthorizationResult
     */
    public AuthorizationResult requestAuthorization(@NonNull final AuthorizationRequest request) {
        loadStartUrl();
        // TODO : Add state parameter from the AuthorizationRequest to the Intent parameter like below
        // intent.putExtra(MicrosoftAuthorizationResult.REQUEST_STATE_PARAMETER, request.getState());
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    /**
     * Constructor of EmbeddedWebViewAuthorizationStrategy.
     *
     * @param activity Authentication activity
     * @param request  Authorization request
     * @throws UnsupportedEncodingException
     * @throws ClientException
     */
    public EmbeddedWebViewAuthorizationStrategy(@NonNull final Activity activity,
                                                @NonNull final AuthorizationRequest request)
            throws UnsupportedEncodingException, ClientException {
        final AzureActiveDirectoryWebViewClient webViewClient = new AzureActiveDirectoryWebViewClient(activity, request);
        //TODO validate auth request in OAuth2Strategy.
        createWebView(activity, webViewClient);
        mStartUrl = request.getAuthorizationStartUrl();
    }

    /**
     * Set up the web view configurations.
     * @param activity  AuthenticationActivity
     * @param webViewClient AzureActiveDirectoryWebViewClient
     */
    private void createWebView(final Activity activity, final AzureActiveDirectoryWebViewClient webViewClient) {
        // Create the Web View to show the page
        mWebView = (WebView) activity.findViewById(activity.getResources().getIdentifier("webView1", "id",
                activity.getPackageName()));
        mWebView.getSettings().setUserAgentString(
                mWebView.getSettings().getUserAgentString() + AuthenticationConstants.Broker.USER_AGENT_VALUE_PKEY_AUTH);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.requestFocus(View.FOCUS_DOWN);

        // Set focus to the view for touch event
        mWebView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View view, final MotionEvent event) {
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
        mWebView.setWebViewClient(webViewClient);
    }

    /**
     * Load the start url for auth grant flow. It will load the black page first to avoid error for not loading web view.
     */
    private void loadStartUrl() {
        mWebView.post(new Runnable() {
            @Override
            public void run() {
                // load blank first to avoid error for not loading webview
                mWebView.loadUrl("about:blank");
                Logger.verbose(TAG, "Launching embedded WebView for acquiring auth code.");
                Logger.verbosePII(TAG, "The start url is" + mStartUrl);
                mWebView.loadUrl(mStartUrl);
            }
        });
    }
}
