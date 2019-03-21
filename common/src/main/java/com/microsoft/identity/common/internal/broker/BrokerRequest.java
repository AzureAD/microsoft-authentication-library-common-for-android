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
package com.microsoft.identity.common.internal.broker;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Represents the broker request
 */
public class BrokerRequest implements Serializable {


    private class SerializedNames {
        final static String AUTHORITY = "account.authority";
        final static String SCOPE = "account.scope";
        final static String REDIRECT = "account.redirect";
        final static String CLIENT_ID = "account.clientid";
        final static String HOME_ACCOUNT_ID = "account.homeAccountId";
        final static String LOCAL_ACCOUNT_ID = "account.localAccountId";
        final static String USERNAME = "account.username";
        final static String EXTRA_QUERY_STRING_PARAMETER = "account.extra.query.param";
        final static String CORRELATION_ID = "account.correlationid";
        final static String PROMPT = "account.prompt";
        final static String CLAIMS = "account.claims";
        final static String FORCE_REFRESH = "force.refresh";
        final static String APPLICATION_NAME = "application.name";
        final static String APPLICATION_VERSION = "application.version";
        final static String MSAL_VERSION = "msal.version";
    }

    /**
     * Authority for the request
     */
    @SerializedName(SerializedNames.AUTHORITY)
    @NonNull
    private String mAuthority;

    /**
     * Scopes for the request. This is expected to be of the format
     * "scope 1 scope2 scope3" with space as a delimiter
     */
    @NonNull
    @SerializedName(SerializedNames.SCOPE)
    private String mScope;

    /**
     * The redirect uri for the request.
     * // TODO : See if this is needed.
     */
    @NonNull
    @SerializedName(SerializedNames.REDIRECT)
    private String mRedirect;

    /**
     * The client id of the application.
     */
    @NonNull
    @SerializedName(SerializedNames.CLIENT_ID)
    private String mClientId;

    /**
     * The username for the request.
     */
    @NonNull
    @SerializedName(SerializedNames.USERNAME)
    private String mUserName;

    /**
     * Home account id of the user. Needs to be set for silent request
     */
    @Nullable
    @SerializedName(SerializedNames.HOME_ACCOUNT_ID)
    private String mHomeAccountId;

    /**
     * Local account id of the user. Needs to be set for silent request
     */
    @SerializedName(SerializedNames.LOCAL_ACCOUNT_ID)
    private String mLocalAccountId;

    /**
     * Extra query parameters set for the request.
     */
    @Nullable
    @SerializedName(SerializedNames.EXTRA_QUERY_STRING_PARAMETER)
    private String mExtraQueryStringParameter;

    /**
     * Correlation id for the request, it should ba unique GUID.
     */
    @NonNull
    @SerializedName(SerializedNames.CORRELATION_ID)
    private String mCorrelationId;

    /**
     * Prompt for the request.
     * {@link com.microsoft.identity.common.internal.providers.oauth2.OpenIdConnectPromptParameter}
     * <p>
     * Default value : {@link com.microsoft.identity.common.internal.providers.oauth2.OpenIdConnectPromptParameter#SELECT_ACCOUNT}
     */
    @Nullable
    @SerializedName(SerializedNames.PROMPT)
    private String mPrompt;

    /**
     * Claims for the request. This needs to be a valid json string.
     */
    @Nullable
    @SerializedName(SerializedNames.CLAIMS)
    private String mClaims;

    /**
     * Boolean if set, will try to refresh the token instead of using it from cache.
     */
    @Nullable
    @SerializedName(SerializedNames.FORCE_REFRESH)
    private boolean mForceRefresh;

    /**
     * Application package name.
     */
    @NonNull
    @SerializedName(SerializedNames.APPLICATION_NAME)
    private String mApplicationName;

    /**
     * Application version.
     */
    @NonNull
    @SerializedName((SerializedNames.APPLICATION_VERSION))
    private String mApplicationVersion;

    /**
     * Msal version.
     */
    @NonNull
    @SerializedName(SerializedNames.MSAL_VERSION)
    private String mMsalVersion;


    public BrokerRequest() {

    }

    public String getAuthority() {
        return mAuthority;
    }

    public void setAuthority(String authority) {
        this.mAuthority = authority;
    }

    public String getScope() {
        return mScope;
    }

    public void setScope(String authority) {
        this.mScope = authority;
    }

    public String getRedirect() {
        return mRedirect;
    }

    public void setRedirect(String redirect) {
        this.mRedirect = redirect;
    }

    public String getClientId() {
        return mClientId;
    }

    public void setClientId(String clientId) {
        this.mClientId = clientId;
    }

    public String getHomeAccountId() {
        return mHomeAccountId;
    }

    public void setHomeAccountId(String userId) {
        this.mHomeAccountId = userId;
    }

    public String getLocalAccountId() {
        return mLocalAccountId;
    }

    public void setLocalAccountId(String localAccountId) {
        this.mLocalAccountId = localAccountId;
    }

    public String getUserName() {
        return mUserName;
    }

    public void setUserName(String userName) {
        this.mUserName = userName;
    }

    public String getExtraQueryStringParameter() {
        return mExtraQueryStringParameter;
    }

    public void setExtraQueryStringParameter(String extraQueryStringParameter) {
        this.mExtraQueryStringParameter = extraQueryStringParameter;
    }

    public String getCorrelationId() {
        return mCorrelationId;
    }

    public void setCorrelationId(String correlationId) {
        this.mCorrelationId = correlationId;
    }

    public String getPrompt() {
        return mPrompt;
    }

    public void setPrompt(String prompt) {
        this.mPrompt = prompt;
    }

    public String getClaims() {
        return mClaims;
    }

    public void setClaims(String claims) {
        this.mClaims = claims;
    }

    public boolean getForceRefresh() {
        return mForceRefresh;
    }

    public void setForceRefresh(boolean forceRefresh) {
        this.mForceRefresh = forceRefresh;
    }

    public String getApplicationName() {
        return mApplicationName;
    }

    public void setApplicationName(String applicationName) {
        this.mApplicationName = applicationName;
    }

    public String getApplicationVersion() {
        return mApplicationVersion;
    }

    public void setApplicationVersion(String applicationVersion) {
        this.mApplicationVersion = applicationVersion;
    }

    public String getMsalVersion() {
        return mMsalVersion;
    }

    public void setMsalVersion(String version) {
        this.mMsalVersion = version;
    }

}
