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
package com.microsoft.identity.common.internal.ui.webview;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import androidx.annotation.NonNull;
import android.view.View;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.ChallengeFactory;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.IAuthorizationCompletionCallback;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.IChallengeHandler;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.NtlmChallenge;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.NtlmChallengeHandler;
import com.microsoft.identity.common.internal.util.StringUtil;

public abstract class OAuth2WebViewClient extends WebViewClient {
    /* constants */
    private static final String TAG = OAuth2WebViewClient.class.getSimpleName();

    private final IAuthorizationCompletionCallback mCompletionCallback;
    private final Activity mActivity;

    /**
     * @return context
     */
    public Activity getActivity() {
        return mActivity;
    }

    /**
     * @return handler completion callback
     */
    IAuthorizationCompletionCallback getCompletionCallback() {
        return mCompletionCallback;
    }

    /**
     * Constructor for the OAuth2 basic web view client.
     *
     * @param activity app Context
     * @param callback Challenge completion callback
     */
    OAuth2WebViewClient(@NonNull final Activity activity,
                        @NonNull final IAuthorizationCompletionCallback callback) {
        //the validation of redirect url and authorization request should be in upper level before launching the webview.
        mActivity = activity;
        mCompletionCallback = callback;
    }

    @Override
    public void onReceivedHttpAuthRequest(WebView view, final HttpAuthHandler handler,
                                          String host, String realm) {
        // Create a dialog to ask for credentials and post it to the handler.
        Logger.info(TAG, "Receive the http auth request. Start the dialog to ask for creds. ");
        Logger.infoPII(TAG, "Host:" + host);

        //TODO TelemetryEvent.setNTLM(true); after the Telemetry is finished in common.
        // Use ChallengeFactory to produce a NtlmChallenge
        final NtlmChallenge ntlmChallenge = ChallengeFactory.getNtlmChallenge(view, handler, host, realm);

        // Init the NtlmChallengeHandler
        final IChallengeHandler<NtlmChallenge, Void> challengeHandler = new NtlmChallengeHandler(mActivity, mCompletionCallback);

        //Process the challenge through the NtlmChallengeHandler created
        challengeHandler.processChallenge(ntlmChallenge);
    }

    @Override
    public void onReceivedError(final WebView view,
                                final int errorCode,
                                final String description,
                                final String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);

        // Create result intent when webView received an error.
        final Intent resultIntent = new Intent();
        resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_CODE,
                "Error Code:" + errorCode);
        resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE,
                description);

        // Send the result back to the calling activity
        mCompletionCallback.onChallengeResponseReceived(
                AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR,
                resultIntent
        );
    }

    @Override
    public void onReceivedSslError(final WebView view,
                                   final SslErrorHandler handler,
                                   final SslError error) {
        // Developer does not have option to control this for now
        super.onReceivedSslError(view, handler, error);
        handler.cancel();

        // WebView received the ssl error and create the result intent.
        final Intent resultIntent = new Intent();
        resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_CODE,
                "Code:" + ERROR_FAILED_SSL_HANDSHAKE);
        resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE,
                error.toString());

        // Send the result back to the calling activity
        mCompletionCallback.onChallengeResponseReceived(
                AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR,
                resultIntent
        );
    }

    @Override
    public void onPageFinished(final WebView view,
                               final String url) {
        super.onPageFinished(view, url);
        // Once web view is fully loaded,set to visible
        view.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPageStarted(final WebView view,
                              final String url,
                              final Bitmap favicon) {
        checkStartUrl(url);
        Logger.info(TAG, "WebView starts loading.");
        super.onPageStarted(view, url, favicon);
    }

    private void checkStartUrl(final String url) {
        if (StringUtil.isEmpty(url)) {
            Logger.info(TAG, "onPageStarted: Null url for page to load.");
            return;
        }

        final Uri uri = Uri.parse(url);
        if (uri.isOpaque()) {
            Logger.info(TAG, "onPageStarted: Non-hierarchical loading uri.");
            Logger.infoPII(TAG, "start url: " + url);
        } else if (StringUtil.isEmpty(uri.getQueryParameter(AuthenticationConstants.OAuth2.CODE))) {
            Logger.infoPII(TAG, "Host: " + uri.getHost() + " Path: " + uri.getPath());
        } else {
            Logger.info(TAG, "Auth code is returned for the loading url.");
            Logger.infoPII(TAG, "Host: " + uri.getHost() + " Path: " + uri.getPath());
        }
    }
}