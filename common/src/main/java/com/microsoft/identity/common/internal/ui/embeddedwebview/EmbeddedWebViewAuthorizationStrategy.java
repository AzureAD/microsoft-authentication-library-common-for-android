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

import android.annotation.SuppressLint;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
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
public class EmbeddedWebViewAuthorizationStrategy <GenericWebViewClient extends OAuth2WebViewClient,
        GenericAuthorizationRequest extends AuthorizationRequest,
        GenericAuthorizationResult extends AuthorizationResult>
        extends AuthorizationStrategy<GenericAuthorizationRequest, GenericAuthorizationResult> {

    private static final String TAG = StringExtensions.class.getSimpleName();
    private WebView mWebView;
    private String mStartUrl;

    /**
     * Perform the authorization request in the embedded web view and return the authorization result.
     *
     * @param request authorization request
     * @return generic AuthorizationResult
     */
    public GenericAuthorizationResult requestAuthorization(@Nullable final GenericAuthorizationRequest request) {
        Logger.verbose(TAG, "Perform the authorization request with embedded webView.");
        loadStartUrl();
        // requestAuthorization could not return the authorization result
        // The activity result is set in AuthenticationActivity.setResult()
        // And AuthenticationActivity in ADAL/MSAL is not moved into common
        return null;
    }

    /**
     * Constructor of EmbeddedWebViewAuthorizationStrategy.
     *
     * @param webViewClient Generic WebViewClient
     * @throws UnsupportedEncodingException thrown when the Character Encoding is not supported
     * @throws ClientException throw when error happens during the authorization
     */
    public EmbeddedWebViewAuthorizationStrategy(final GenericWebViewClient webViewClient, final WebView webView)
            throws UnsupportedEncodingException, ClientException {
        //TODO validate auth request in OAuth2Strategy.
        setUpWebView(webViewClient, webView);
        mStartUrl = webViewClient.getRequest().getAuthorizationStartUrl();
    }

    /**
     * Set up the web view configurations.
     * @param webViewClient AzureActiveDirectoryWebViewClient
     */
    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void setUpWebView(final GenericWebViewClient webViewClient, final WebView webView) {
        // Create the Web View to show the page
        mWebView = webView;
        WebSettings userAgentSetting = mWebView.getSettings();
        final String userAgent = userAgentSetting.getUserAgentString();
        mWebView.getSettings().setUserAgentString(
                userAgent + AuthenticationConstants.Broker.CLIENT_TLS_NOT_SUPPORTED);
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
