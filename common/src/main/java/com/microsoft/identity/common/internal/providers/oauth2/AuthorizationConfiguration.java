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

import com.microsoft.identity.common.internal.ui.AuthorizationAgent;

import java.io.Serializable;

public class AuthorizationConfiguration implements Serializable {
    /**
     * Serial version id.
     */
    private static final long serialVersionUID = -8547851138779764480L;

    private static AuthorizationConfiguration sInstance = null;

    private AuthorizationAgent mAuthorizationAgent;

    private String mRedirectUrl;

    private AuthorizationConfiguration() {
        mAuthorizationAgent = AuthorizationAgent.BROWSER;
    }

    public static AuthorizationConfiguration getInstance() {
        if (sInstance == null) {
            sInstance = new AuthorizationConfiguration();
        }

        return sInstance;
    }

    /**
     * If the dev wants to specify the ui flow to use embedded webView.
     * you need to call AuthorizationConfiguration.getInstance().setAuthorizationAgent(AuthorizationAgent.WEBVIEW);
     * before initializing the authorization strategy. Otherwise, browser flow will be used as default.
     *
     * @param authorizationAgent AuthorizationAgent
     */
    public void setAuthorizationAgent(final AuthorizationAgent authorizationAgent) {
        mAuthorizationAgent = authorizationAgent;
    }

    public void setRedirectUrl(final String redirectUrl) {
        mRedirectUrl = redirectUrl;
    }

    public String getRedirectUrl() {
        return mRedirectUrl;
    }

    public AuthorizationAgent getAuthorizationAgent() {
        return mAuthorizationAgent;
    }
}