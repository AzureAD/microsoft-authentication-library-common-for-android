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

import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.view.View;
import android.webkit.ClientCertRequest;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.util.StringUtil;

public abstract class OAuth2WebViewClient extends WebViewClient {

    /**
     * Redirect URLs are a critical part of the OAuth flow. After a user successfully authorizes an
     * application, the authorization server will redirect the user back to the application with
     * either an authorization code or access token in the URL.
     *
     * @TODO Will it be a part of the auth configuration class?
     */
    private final String mRedirectURL;

    private final AuthorizationRequest mRequest;

    private final Context mContext;

    static final String BLANK_PAGE = "about:blank";

    abstract void showSpinner(boolean showing);

    OAuth2WebViewClient(final Context context, final String redirectURL, final AuthorizationRequest request) {
        //the validation of redirect url and authorization request should be in upper level before launching the webview.
        if (null == context || null == request || StringUtil.isEmpty(redirectURL)) {
            throw new IllegalArgumentException("Null parameter to initialize OAuth2WebViewClient.");
        }

        mContext = context;
        mRedirectURL = redirectURL;
        mRequest = request;
    }

    /**
     * Notify the host application that a page has started loading.
     * @param view The WebView that is initiating the callback.
     * @param url The url to be loaded.
     * @param favicon The favicon for this page if it already exists in the
     *            database.
     */
    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        //Add logs of the start page
    }

    /**
     * Notify the host application that a page has finished loading. This method
     * is called only for main frame.
     * @param view The WebView that is initiating the callback.
     * @param url The url of the page.
     */
    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);

        /*
         * Once web view is fully loaded, set to visible.
         */
        view.setVisibility(View.VISIBLE);
        if (!url.startsWith(BLANK_PAGE)) {
            showSpinner(false);
        }
    }

    /**
     * Report web resource loading error to the host application. These errors usually indicate
     * inability to connect to the server.
     * @param view The WebView that is initiating the callback.
     * @param request The originating request.
     * @param error Information about the error occured.
     */
    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        super.onReceivedError(view, request, error);
        //TODO Log here
        //TODO Send error auth response
    }

    /**
     * Notify the host application that an SSL error occurred while loading a
     * resource. Call handler.cancel() and send the error auth response.
     * @param view
     * @param handler
     * @param error
     */
    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler,
                                   SslError error) {
        super.onReceivedSslError(view, handler, error);
        showSpinner(false);
        //TODO Log here
        //TODO Send error auth response
    }


    /**
     * Notifies the host application that the WebView received an HTTP
     * authentication request. The host application can use the supplied
     * {@link HttpAuthHandler} to set the WebView's response to the request.
     */
    @Override
    public void onReceivedHttpAuthRequest(WebView view, final HttpAuthHandler handler,
                                          String host, String realm) {
        //NTML handling

    }

    /** Notify the host application to handle a SSL client certificate
     * request. The host application is responsible for showing the UI
     * if desired and providing the keys. There are three ways to
     * respond: proceed(), cancel() or ignore(). Webview stores the response
     * in memory (for the life of the application) if proceed() or cancel() is
     * called and does not call onReceivedClientCertRequest() again for the
     * same host and port pair. Webview does not store the response if ignore()
     * is called. Note that, multiple layers in chromium network stack might be
     * caching the responses, so the behavior for ignore is only a best case
     * effort.
     *
     * This method is called on the UI thread. During the callback, the
     * connection is suspended.
     *
     **/
    @Override
    public void onReceivedClientCertRequest(WebView view, ClientCertRequest request) {
        super.onReceivedClientCertRequest(view, request);
    }

    /**
     * Give the host application a chance to take over the control when a new
     * url is about to be loaded in the current WebView. If WebViewClient is not
     * provided, by default WebView will ask Activity Manager to choose the
     * proper handler for the url. If WebViewClient is provided, return true
     * means the host application handles the url, while return false means the
     * current WebView handles the url.
     *
     * @param view The WebView that is initiating the callback.
     * @param request Object containing the details of the request.
     * @return True if the host application wants to leave the current WebView
     *         and handle the url itself, otherwise return false.
     */
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return super.shouldOverrideUrlLoading(view, request);
    }
}
