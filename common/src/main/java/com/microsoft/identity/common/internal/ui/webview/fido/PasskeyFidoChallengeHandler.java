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

import com.microsoft.identity.common.java.ui.webview.authorization.IAuthorizationCompletionCallback;

/**
 * Handles a FidoChallenge by either creating or authenticating with a passkey.
 */
public class PasskeyFidoChallengeHandler extends AbstractFidoChallengeHandler {

    private final IFidoManager mFidoManager;
    private final IAuthorizationCompletionCallback mChallengeCallback;

    /**
     * Creates a PasskeyFidoChallengeHandler instance.
     * @param fidoManager IFidoManager instance.
     * @param webView Current WebView.
     * @param challengeCallback callback to invoke after challenge is handled.
     */
    public PasskeyFidoChallengeHandler(IFidoManager fidoManager, WebView webView, IAuthorizationCompletionCallback challengeCallback) {
        super(webView);
        mFidoManager = fidoManager;
        mChallengeCallback = challengeCallback;

    }

    @Override
    public Void processChallenge(AbstractFidoChallenge abstractFidoChallenge) {
        //First, is this a registration or an authorization challenge?
        //Pass the challenge to the manager's methods based on that
        //These two methods should return the same object... maybe header
        //Take this header, and pass this plus the url to the respondToChallenge method.
        return null;
    }
}
