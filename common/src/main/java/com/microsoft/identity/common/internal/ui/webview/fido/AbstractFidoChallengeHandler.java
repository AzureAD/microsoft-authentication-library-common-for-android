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
package com.microsoft.identity.common.internal.ui.webview.fido;

import android.webkit.WebView;

import com.microsoft.identity.common.internal.ui.webview.challengehandlers.IChallengeHandler;

import java.util.Map;

/**
 * Abstract class that handles a FidoChallenge.
 */
public abstract class AbstractFidoChallengeHandler implements IChallengeHandler<AbstractFidoChallenge, Void> {

    private final WebView mWebView;

    /**
     * Constructs an AbstractFidoChallengeHandler.
     * @param webView current WebView.
     */
    protected AbstractFidoChallengeHandler(WebView webView) {
        this.mWebView = webView;
    }

    /**
     * Makes a post request in the WebView with the submitUrl and headers.
     * @param submitUrl The url to which the client submits the response to the server's challenge.
     * @param headers Response header values.
     */
    public void respondToChallenge(String submitUrl, Map<String, String> headers) {
        //Submit url here.
    }
}
