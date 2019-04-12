//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.internal.request;

import android.app.Activity;
import android.util.Pair;

import com.google.gson.annotations.Expose;
import com.microsoft.identity.common.internal.providers.oauth2.OpenIdConnectPromptParameter;
import com.microsoft.identity.common.internal.ui.AuthorizationAgent;

import java.util.HashMap;
import java.util.List;

public class AcquireTokenOperationParameters extends OperationParameters {

    private transient Activity mActivity;
    private String mLoginHint;
    @Expose()
    private List<Pair<String, String>> mExtraQueryStringParameters;
    @Expose()
    private List<String> mExtraScopesToConsent;
    @Expose()
    private OpenIdConnectPromptParameter mOpenIdConnectPromptParameter;
    private HashMap<String, String> mRequestHeaders;

    public AuthorizationAgent getAuthorizationAgent() {
        return mAuthorizationAgent;
    }

    public void setAuthorizationAgent(AuthorizationAgent authorizationAgent) {
        mAuthorizationAgent = authorizationAgent;
    }

    @Expose()
    private AuthorizationAgent mAuthorizationAgent;

    public Activity getActivity() {
        return mActivity;
    }

    public void setActivity(Activity mActivity) {
        this.mActivity = mActivity;
    }

    public List<Pair<String, String>> getExtraQueryStringParameters() {
        return mExtraQueryStringParameters;
    }

    public void setExtraQueryStringParameters(List<Pair<String, String>> mExtraQueryStringParameters) {
        this.mExtraQueryStringParameters = mExtraQueryStringParameters;
    }

    public List<String> getExtraScopesToConsent() {
        return mExtraScopesToConsent;
    }

    public void setExtraScopesToConsent(List<String> mExtraScopesToConsent) {
        this.mExtraScopesToConsent = mExtraScopesToConsent;
    }

    public void setLoginHint(String loginHint) {
        this.mLoginHint = loginHint.trim();
    }

    public String getLoginHint() {
        return this.mLoginHint;
    }

    public OpenIdConnectPromptParameter getOpenIdConnectPromptParameter() {
        return mOpenIdConnectPromptParameter;
    }

    public void setOpenIdConnectPromptParameter(OpenIdConnectPromptParameter openIdConnectPromptParameter) {
        mOpenIdConnectPromptParameter = openIdConnectPromptParameter;
    }

    public HashMap<String, String> getRequestHeaders() {
        return mRequestHeaders;
    }

    public void setRequestHeaders(HashMap<String, String> requestHeaders) {
        this.mRequestHeaders = requestHeaders;
    }
}