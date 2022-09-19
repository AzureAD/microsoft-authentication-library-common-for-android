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
package com.microsoft.identity.common.internal.ui.webview.challengehandlers;

import android.webkit.WebView;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.java.challengehandlers.PKeyAuthChallenge;
import com.microsoft.identity.common.java.providers.RawAuthorizationResult;
import com.microsoft.identity.common.java.ui.webview.authorization.IAuthorizationCompletionCallback;
import com.microsoft.identity.common.logging.Logger;

import java.util.Map;

public final class PKeyAuthChallengeHandler implements IChallengeHandler<PKeyAuthChallenge, Void> {
    private static final String TAG = PKeyAuthChallengeHandler.class.getSimpleName();
    private final WebView mWebView;
    private final IAuthorizationCompletionCallback mChallengeCallback;

    /**
     * @param view
     * @param completionCallback
     */
    public PKeyAuthChallengeHandler(@NonNull final WebView view,
                                    @NonNull IAuthorizationCompletionCallback completionCallback) {
        mWebView = view;
        mChallengeCallback = completionCallback;
    }

    @Override
    public Void processChallenge(final PKeyAuthChallenge pKeyAuthChallenge) {
        final String methodTag = TAG + ":processChallenge";
        mWebView.stopLoading();
        mChallengeCallback.setPKeyAuthStatus(true);

        try {
            //Get no device cert response
            final Map<String, String> header = pKeyAuthChallenge.getChallengeHeader();

            mWebView.post(new Runnable() {
                @Override
                public void run() {
                    String loadUrl = pKeyAuthChallenge.getSubmitUrl();
                    Logger.info(methodTag, "Respond to pkeyAuth challenge");
                    Logger.infoPII(methodTag, "Challenge submit url:" + pKeyAuthChallenge.getSubmitUrl());

                    mWebView.loadUrl(loadUrl, header);
                }
            });
        } catch (final Throwable e) {
            // It should return error code and finish the
            // activity, so that onActivityResult implementation
            // returns errors to callback.
            //TODO log the request info
            mChallengeCallback.onChallengeResponseReceived(
                    RawAuthorizationResult.fromThrowable(e)
            );
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            if (e instanceof Error) {
                throw (Error) e;
            }
        }

        return null;
    }
}