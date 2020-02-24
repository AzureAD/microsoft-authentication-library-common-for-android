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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.gson.annotations.Expose;
import com.microsoft.identity.common.internal.providers.oauth2.OpenIdConnectPromptParameter;
import com.microsoft.identity.common.internal.ui.AuthorizationAgent;
import com.microsoft.identity.common.internal.ui.browser.BrowserDescriptor;

import java.util.HashMap;
import java.util.List;

public class AcquireTokenOperationParameters extends OperationParameters {

    private transient Activity mActivity;

    private transient HashMap<String, String> mRequestHeaders;

    private boolean mBrokerBrowserSupportEnabled;

    private transient Fragment mFragment;

    private String mLoginHint;

    @Expose()
    private boolean webViewZoomControlsEnabled;

    @Expose()
    private boolean webViewZoomEnabled;

    @Expose()
    private List<Pair<String, String>> mExtraQueryStringParameters;

    @Expose()
    private List<String> mExtraScopesToConsent;

    @Expose()
    private OpenIdConnectPromptParameter mOpenIdConnectPromptParameter;

    @Expose()
    private AuthorizationAgent mAuthorizationAgent;

    public AuthorizationAgent getAuthorizationAgent() {
        return mAuthorizationAgent;
    }

    public void setAuthorizationAgent(@Nullable final AuthorizationAgent authorizationAgent) {
        mAuthorizationAgent = authorizationAgent;
    }

    public Activity getActivity() {
        return mActivity;
    }

    public void setActivity(@NonNull final Activity activity) {
        this.mActivity = activity;
    }

    public Fragment getFragment() {
        return mFragment;
    }

    public void setFragment(@NonNull final Fragment fragment) {
        this.mFragment = fragment;
    }

    public List<Pair<String, String>> getExtraQueryStringParameters() {
        return mExtraQueryStringParameters;
    }

    public void setExtraQueryStringParameters(@Nullable final List<Pair<String, String>> mExtraQueryStringParameters) {
        this.mExtraQueryStringParameters = mExtraQueryStringParameters;
    }

    public List<String> getExtraScopesToConsent() {
        return mExtraScopesToConsent;
    }

    public void setExtraScopesToConsent(@Nullable final List<String> mExtraScopesToConsent) {
        this.mExtraScopesToConsent = mExtraScopesToConsent;
    }

    public void setLoginHint(@Nullable final String loginHint) {
        this.mLoginHint = loginHint != null ? loginHint.trim() : loginHint;
    }

    public String getLoginHint() {
        return this.mLoginHint;
    }

    public OpenIdConnectPromptParameter getOpenIdConnectPromptParameter() {
        return mOpenIdConnectPromptParameter;
    }

    public void setOpenIdConnectPromptParameter(@Nullable final OpenIdConnectPromptParameter openIdConnectPromptParameter) {
        mOpenIdConnectPromptParameter = openIdConnectPromptParameter;
    }

    public HashMap<String, String> getRequestHeaders() {
        return mRequestHeaders;
    }

    public void setRequestHeaders(@Nullable final HashMap<String, String> requestHeaders) {
        this.mRequestHeaders = requestHeaders;
    }

    public void setBrowserSafeList(final List<BrowserDescriptor> browserSafeList) {
        this.mBrowserSafeList = browserSafeList;
    }

    public boolean isBrokerBrowserSupportEnabled() {
        return mBrokerBrowserSupportEnabled;
    }

    public void setBrokerBrowserSupportEnabled(boolean brokerBrowserSupportEnabled) {
        this.mBrokerBrowserSupportEnabled = brokerBrowserSupportEnabled;
    }

    public void setWebViewZoomControlsEnabled(boolean webViewZoomControlsEnabled) {
        this.webViewZoomControlsEnabled = webViewZoomControlsEnabled;
    }

    public void setWebViewZoomEnabled(boolean webViewZoomEnabled) {
        this.webViewZoomEnabled = webViewZoomEnabled;
    }

    public boolean isWebViewZoomEnabled() {
        return webViewZoomEnabled;
    }

    public boolean isWebViewZoomControlsEnabled() {
        return webViewZoomControlsEnabled;
    }

    /**
     * Get the list of browsers which are safe to launch for auth flow.
     *
     * @return list of browser descriptors
     */
    public List<BrowserDescriptor> getBrowserSafeList() {
        return mBrowserSafeList;
    }

    //CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @SuppressWarnings("PMD")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AcquireTokenOperationParameters)) return false;
        if (!super.equals(o)) return false;

        AcquireTokenOperationParameters that = (AcquireTokenOperationParameters) o;

        if (mLoginHint != null ? !mLoginHint.equals(that.mLoginHint) : that.mLoginHint != null)
            return false;
        if (mExtraQueryStringParameters != null ? !mExtraQueryStringParameters.equals(that.mExtraQueryStringParameters) : that.mExtraQueryStringParameters != null)
            return false;
        if (mExtraScopesToConsent != null ? !mExtraScopesToConsent.equals(that.mExtraScopesToConsent) : that.mExtraScopesToConsent != null)
            return false;
        return mOpenIdConnectPromptParameter == that.mOpenIdConnectPromptParameter;
    }
    //CHECKSTYLE:ON

    //CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @SuppressWarnings("PMD")
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (mLoginHint != null ? mLoginHint.hashCode() : 0);
        result = 31 * result + (mExtraQueryStringParameters != null ? mExtraQueryStringParameters.hashCode() : 0);
        result = 31 * result + (mExtraScopesToConsent != null ? mExtraScopesToConsent.hashCode() : 0);
        result = 31 * result + (mOpenIdConnectPromptParameter != null ? mOpenIdConnectPromptParameter.hashCode() : 0);
        return result;
    }
    //CHECKSTYLE:ON

}