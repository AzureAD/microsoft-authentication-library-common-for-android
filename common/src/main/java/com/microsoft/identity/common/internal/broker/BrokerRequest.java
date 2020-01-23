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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Represents the broker request
 */
public class BrokerRequest implements Serializable {

    private class SerializedNames {
        final static String AUTHORITY = "authority";
        final static String SCOPE = "scopes";
        final static String REDIRECT = "redirect_uri";
        final static String CLIENT_ID = "client_id";
        final static String HOME_ACCOUNT_ID = "home_account_id";
        final static String LOCAL_ACCOUNT_ID = "local_account_id";
        final static String USERNAME = "username";
        final static String EXTRA_QUERY_STRING_PARAMETER = "extra_query_param";
        final static String CORRELATION_ID = "correlation_id";
        final static String PROMPT = "prompt";
        final static String CLAIMS = "claims";
        final static String FORCE_REFRESH = "force_refresh";
        final static String CLIENT_APP_NAME = "client_app_name";
        final static String CLIENT_APP_VERSION = "client_app_version";
        final static String CLIENT_VERSION = "client_version";
        final static String ENVIRONMENT = "environment";
        final static String MULTIPLE_CLOUDS_SUPPORTED = "multiple_clouds_supported";
        final static String AUTHORIZATION_AGENT = "authorization_agent";
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
     * Correlation id for the request, it should be a unique GUID.
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
    @SerializedName(SerializedNames.CLIENT_APP_NAME)
    private String mApplicationName;

    /**
     * Application version.
     */
    @NonNull
    @SerializedName((SerializedNames.CLIENT_APP_VERSION))
    private String mApplicationVersion;

    /**
     * Msal version.
     */
    @NonNull
    @SerializedName(SerializedNames.CLIENT_VERSION)
    private String mMsalVersion;

    /**
     * AAD Environment
     */
    @NonNull
    @SerializedName(SerializedNames.ENVIRONMENT)
    private String mEnvironment;

    /**
     * Boolean indicated whether app supports multiple clouds
     */
    @NonNull
    @SerializedName(SerializedNames.MULTIPLE_CLOUDS_SUPPORTED)
    private boolean mMultipleCloudsSupported;

    @NonNull
    @SerializedName(SerializedNames.AUTHORIZATION_AGENT)
    private String mAuthorizationAgent;


    private BrokerRequest(BrokerRequest.Builder builder) {
        mAuthority = builder.mAuthority;
        mScope = builder.mScope;
        mRedirect = builder.mRedirect;
        mClientId = builder.mClientId;
        mHomeAccountId = builder.mHomeAccountId;
        mLocalAccountId = builder.mLocalAccountId;
        mUserName = builder.mUserName;
        mExtraQueryStringParameter = builder.mExtraQueryStringParameter;
        mCorrelationId = builder.mCorrelationId;
        mPrompt = builder.mPrompt;
        mClaims = builder.mClaims;
        mForceRefresh = builder.mForceRefresh;
        mApplicationName = builder.mApplicationName;
        mApplicationVersion = builder.mApplicationVersion;
        mMsalVersion = builder.mMsalVersion;
        mEnvironment = builder.mEnvironment;
        mMultipleCloudsSupported = builder.mMultipleCloudsSupported;
        mAuthorizationAgent = builder.mAuthorizationAgent;
    }


    public String getAuthority() {
        return mAuthority;
    }

    public String getScope() {
        return mScope;
    }

    public String getRedirect() {
        return mRedirect;
    }

    public String getClientId() {
        return mClientId;
    }

    public String getHomeAccountId() {
        return mHomeAccountId;
    }

    public String getLocalAccountId() {
        return mLocalAccountId;
    }

    public String getUserName() {
        return mUserName;
    }

    public String getExtraQueryStringParameter() {
        return mExtraQueryStringParameter;
    }

    public String getCorrelationId() {
        return mCorrelationId;
    }

    public String getPrompt() {
        return mPrompt;
    }

    public String getClaims() {
        return mClaims;
    }

    public boolean getForceRefresh() {
        return mForceRefresh;
    }

    public String getApplicationName() {
        return mApplicationName;
    }

    public String getApplicationVersion() {
        return mApplicationVersion;
    }

    public String getMsalVersion() {
        return mMsalVersion;
    }

    public String getEnvironment() {
        return mEnvironment;
    }

    public boolean getMultipleCloudsSupported() {
        return mMultipleCloudsSupported;
    }

    public String getAuthorizationAgent() {
        return mAuthorizationAgent;
    }

    /**
     * Builder class for Broker Request.
     */
    public static class Builder {

        private String mAuthority;

        private String mScope;

        private String mRedirect;

        private String mClientId;

        private String mUserName;

        private String mHomeAccountId;

        private String mLocalAccountId;

        private String mExtraQueryStringParameter;

        private String mCorrelationId;

        private String mPrompt;

        private String mClaims;

        private boolean mForceRefresh;

        private String mApplicationName;

        private String mApplicationVersion;

        private String mMsalVersion;

        private String mEnvironment;

        private boolean mMultipleCloudsSupported;

        private String mAuthorizationAgent;


        /**
         * Authority for the request
         */
        public BrokerRequest.Builder authority(@NonNull final String authority) {
            this.mAuthority = authority;
            return this;
        }

        /**
         * Scopes for the request. This is expected to be of the format
         * "scope 1 scope2 scope3" with space as a delimiter
         */
        public BrokerRequest.Builder scope(@NonNull final String scope) {
            this.mScope = scope;
            return this;
        }

        /**
         * The redirect uri for the request.
         * // TODO : See if this is needed.
         */
        public BrokerRequest.Builder redirect(@NonNull final String redirect) {
            this.mRedirect = redirect;
            return this;
        }

        /**
         * The client id of the application.
         */
        public BrokerRequest.Builder clientId(@NonNull final String clientId) {
            this.mClientId = clientId;
            return this;
        }

        /**
         * The username for the request.
         */
        public BrokerRequest.Builder username(@Nullable final String userName) {
            this.mUserName = userName;
            return this;
        }

        /**
         * Home account id of the user. Needs to be set for silent request
         */
        public BrokerRequest.Builder homeAccountId(@Nullable final String userId) {
            this.mHomeAccountId = userId;
            return this;
        }

        /**
         * Local account id of the user. Needs to be set for silent request
         */
        public BrokerRequest.Builder localAccountId(@Nullable final String localAccountId) {
            this.mLocalAccountId = localAccountId;
            return this;
        }

        /**
         * Extra query parameters set for the request.
         */
        public BrokerRequest.Builder extraQueryStringParameter(@Nullable final String extraQueryStringParameter) {
            this.mExtraQueryStringParameter = extraQueryStringParameter;
            return this;
        }

        /**
         * Correlation id for the request, it should ba unique GUID.
         */
        public BrokerRequest.Builder correlationId(@Nullable final String correlationId) {
            this.mCorrelationId = correlationId;
            return this;
        }

        /**
         * Prompt for the request.
         * {@link com.microsoft.identity.common.internal.providers.oauth2.OpenIdConnectPromptParameter}
         * <p>
         * Default value : {@link com.microsoft.identity.common.internal.providers.oauth2.OpenIdConnectPromptParameter#SELECT_ACCOUNT}
         */
        public BrokerRequest.Builder prompt(@Nullable final String prompt) {
            this.mPrompt = prompt;
            return this;
        }

        /**
         * Claims for the request. This needs to be a valid json string.
         */
        public BrokerRequest.Builder claims(@Nullable final String claims) {
            this.mClaims = claims;
            return this;
        }

        /**
         * Boolean if set, will try to refresh the token instead of using it from cache.
         */
        public BrokerRequest.Builder forceRefresh(final boolean forceRefresh) {
            this.mForceRefresh = forceRefresh;
            return this;
        }

        /**
         * Application package name.
         */
        public BrokerRequest.Builder applicationName(@NonNull final String applicationName) {
            this.mApplicationName = applicationName;
            return this;
        }

        /**
         * Application version.
         */
        public BrokerRequest.Builder applicationVersion(@NonNull final String applicationVersion) {
            this.mApplicationVersion = applicationVersion;
            return this;
        }

        /**
         * Msal version.
         */
        @NonNull
        public BrokerRequest.Builder msalVersion(@NonNull final String version) {
            this.mMsalVersion = version;
            return this;
        }

        public BrokerRequest.Builder environment(@NonNull final String environment) {
            this.mEnvironment = environment;
            return this;
        }

        public BrokerRequest.Builder multipleCloudsSupported(@NonNull final boolean multipleCloudsSupported) {
            this.mMultipleCloudsSupported = multipleCloudsSupported;
            return this;
        }

        public BrokerRequest.Builder authorizationAgent(@NonNull final String authorizationAgent) {
            this.mAuthorizationAgent = authorizationAgent;
            return this;
        }


        /**
         * Builds and returns a BrokerRequest
         */
        public BrokerRequest build() {
            return new BrokerRequest(this);
        }
    }
}
