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
 * Encapsulates the possible responses from the broker.  Both successful response and error response.
 */
public class BrokerResult implements Serializable {


    private class SerializedNames {
        static final String ACCeSS_TOKEN = "broker.accessToken";
        static final String ID_TOKEN = "broker.idToken";
        static final String HOMEACCOUNT_ID = "broker.homeAccountId";
        static final String LOCAL_ACCOUNT_ID = "broker.localAccountId";
        static final String USERNAME = "broker.username";
        static final String CLIENT_ID = "broker.clientId";
        static final String SCOPE = "broker.scope";
        static final String TOKENTYPE = "broker.tokenType";
        static final String CLIENINFO = "broker.clientInfo";
        static final String AUTHORITY = "broker.authority";
        static final String ENVIRONMENT = "broker.environment";
        static final String EXPIRES_ON = "broker.expiresOn";
        static final String EXTENDED_EXPIRES_ON = "broker.extendedExpiresOn";
        static final String CACHED_AT = "broker.cachedAt";
        static final String SPE_RING = "broker.speRing";
        static final String REFRESH_TOKEN_AGE = "broker.refreshTokenAge";
        static final String SUCCESS = "broker.success";
        static final String ERROR_CODE = "broker.errorCode";
        static final String ERROR_MESSAGE = "broker.errorMessage";
        static final String CORRELATION_ID = "broker.correlationId";
        static final String SUB_ERROR_CODE = "broker.subErrorCode";
        static final String HTTP_STATUS_CODE = "broker.httpStatusCode";
        static final String HTTP_RESPONSE_HEADERS = "broker.httpResponseHeaders";
        static final String HTTP_RESPONSE_BODY = "broker.httpResponseBody";
        static final String CLI_TELEM_ERRORCODE = "broker.cliTelemErrorCode";
        static final String CLI_TELEM_SUB_ERROR_CODe = "broker.cliTelemSubErrorCode";
    }

    // Success parameters
    /**
     * Access token from the response
     */
    @Nullable
    @SerializedName(SerializedNames.ACCeSS_TOKEN)
    private String mAccessToken;

    /**
     * ID token from the response
     */
    @Nullable
    @SerializedName(SerializedNames.ID_TOKEN)
    private String mIdToken;

    /**
     * Home account id of the user.
     */
    @Nullable
    @SerializedName(SerializedNames.HOMEACCOUNT_ID)
    private String mHomeAccountId;

    /**
     * Local account id or user id of the User
     */
    @Nullable
    @SerializedName(SerializedNames.LOCAL_ACCOUNT_ID)
    private String mLocalAccountId;

    /**
     * Username of the User.
     */
    @Nullable
    @SerializedName(SerializedNames.USERNAME)
    private String mUserName;


    /**
     * Client id of the application
     */
    @Nullable
    @SerializedName(SerializedNames.CLIENT_ID)
    private String mClientId;


    /**
     * Scopes requested
     */
    @Nullable
    @SerializedName(SerializedNames.SCOPE)
    private String mScope;

    /**
     * Token type from the response
     */
    @Nullable
    @SerializedName(SerializedNames.TOKENTYPE)
    private String mTokenType;

    /**
     * Client Info from the response
     */
    @Nullable
    @SerializedName(SerializedNames.CLIENINFO)
    private String mClientInfo;

    /**
     * Authority from the response.
     */
    @Nullable
    @SerializedName(SerializedNames.AUTHORITY)
    private String mAuthority;

    /**
     * Environment used to cache token.
     */
    @Nullable
    @SerializedName(SerializedNames.ENVIRONMENT)
    private String mEnvironment;

    /**
     * Expires on value for the token.
     */
    @Nullable
    @SerializedName(SerializedNames.EXPIRES_ON)
    private long mExpiresOn;

    /**
     * Extended expires on value for the token
     */
    @Nullable
    @SerializedName(SerializedNames.EXTENDED_EXPIRES_ON)
    private long mExtendedExpiresOn;

    /**
     * Access token cache at time in millis
     */
    @Nullable
    @SerializedName(SerializedNames.CACHED_AT)
    private long mCachedAt;

    /**
     * Client telemetry SPE ring
     */
    @Nullable
    @SerializedName(SerializedNames.SPE_RING)
    private String mSpeRing;

    /**
     * Refresh token age from client telemetry
     */
    @Nullable
    @SerializedName(SerializedNames.REFRESH_TOKEN_AGE)
    private String mRefreshTokenAge;

    /**
     * Boolean to indicate if the request succeeded without exceptions.
     */
    @NonNull
    @SerializedName(SerializedNames.SUCCESS)
    private boolean mSuccess;

    // Exception parameters

    /**
     * Error code corresponding to any of the {@link com.microsoft.identity.common.exception.ErrorStrings}
     */
    @Nullable
    @SerializedName(SerializedNames.ERROR_CODE)
    private String mErrorCode;

    /**
     * Error message
     */
    @Nullable
    @SerializedName(SerializedNames.ERROR_MESSAGE)
    private String mErrorMessage;

    /**
     * Correlation id of the request
     */
    @Nullable
    @SerializedName(SerializedNames.CORRELATION_ID)
    private String mCorrelationId;

    /**
     * Sub error code from the error response
     */
    @Nullable
    @SerializedName(SerializedNames.SUB_ERROR_CODE)
    private String mSubErrorCode;

    /**
     * Http Status code of the error response
     */
    @Nullable
    @SerializedName(SerializedNames.HTTP_STATUS_CODE)
    private String mHttpStatusCode;

    /**
     * Response headers or the error response in json format
     */
    @Nullable
    @SerializedName(SerializedNames.HTTP_RESPONSE_HEADERS)
    private String mHttpResponseHeaders;

    /**
     * Response body of the error response
     */
    @Nullable
    @SerializedName(SerializedNames.HTTP_RESPONSE_BODY)
    private String mHttpResponseBody;

    /**
     * Client telemetry error code
     */
    @Nullable
    @SerializedName(SerializedNames.CLI_TELEM_ERRORCODE)
    private String mCliTelemErrorCode;

    /**
     * Client telemetry sub error code
     */
    @Nullable
    @SerializedName(SerializedNames.CLI_TELEM_SUB_ERROR_CODe)
    private String mCliTelemSubErrorCode;


    private BrokerResult(@NonNull final Builder builder){
        mAccessToken = builder.mAccessToken;
        mIdToken = builder.mIdToken;
        mHomeAccountId = builder.mHomeAccountId;
        mLocalAccountId = builder.mLocalAccountId;
        mUserName = builder.mUserName;
        mTokenType = builder.mTokenType;
        mClientId = builder.mClientId;
        mScope = builder.mScope;
        mClientInfo = builder.mClientInfo;
        mAuthority = builder.mAuthority;
        mEnvironment = builder.mEnvironment;
        mExpiresOn = builder.mExpiresOn;
        mExtendedExpiresOn = builder.mExtendedExpiresOn;
        mCachedAt = builder.mCachedAt;
        mSpeRing = builder.mSpeRing;
        mRefreshTokenAge = builder.mRefreshTokenAge;
        mSuccess = builder.mSuccess;

        mErrorCode = builder.mErrorCode;
        mErrorMessage = builder.mErrorMessage;
        mCorrelationId = builder.mCorrelationId;
        mSubErrorCode = builder.mSubErrorCode;
        mHttpStatusCode = builder.mHttpStatusCode;
        mHttpResponseBody = builder.mHttpResponseBody;
        mHttpResponseHeaders = builder.mHttpResponseHeaders;
        mCliTelemErrorCode = builder.mCliTelemErrorCode;
        mCliTelemSubErrorCode = builder.mCliTelemSubErrorCode;
    }

    public String getCliTelemSubErrorCode() {
        return mCliTelemSubErrorCode;
    }

    public String getCliTelemErrorCode() {
        return mCliTelemErrorCode;
    }

    public String getHttpResponseBody() {
        return mHttpResponseBody;
    }

    public String getHttpResponseHeaders() {
        return mHttpResponseHeaders;
    }

    public String getHttpStatusCode() {
        return mHttpStatusCode;
    }

    public String getSubErrorCode() {
        return mSubErrorCode;
    }

    public String getCorrelationId() {
        return mCorrelationId;
    }

    public String getErrorMessage() {
        return mErrorMessage;
    }

    public String getErrorCode() {
        return mErrorCode;
    }

    public boolean isSuccess() {
        return mSuccess;
    }

    public String getRefreshTokenAge() {
        return mRefreshTokenAge;
    }

    public String getSpeRing() {
        return mSpeRing;
    }

    public long getCachedAt() {
        return mCachedAt;
    }

    public long getExtendedExpiresOn() {
        return mExtendedExpiresOn;
    }

    public long getExpiresOn() {
        return mExpiresOn;
    }

    public String getEnvironment() {
        return mEnvironment;
    }

    public String getAuthority() {
        return mAuthority;
    }

    public String getClientInfo() {
        return mClientInfo;
    }

    public String getClientId() {
        return mClientId;
    }

    public String getScope(){
        return mScope;
    }

    public String getTokenType() {
        return mTokenType;
    }

    public String getUserName() {
        return mUserName;
    }

    public String getLocalAccountId() {
        return mLocalAccountId;
    }

    public String getHomeAccountId() {
        return mHomeAccountId;
    }

    public String getIdToken() {
        return mIdToken;
    }

    public String getAccessToken() {
        return mAccessToken;
    }

    public static class Builder {

        private String mAccessToken;

        private String mIdToken;

        private String mHomeAccountId;

        private String mLocalAccountId;

        private String mUserName;

        private String mTokenType;

        private String mClientId;

        private String mScope;

        private String mClientInfo;

        private String mAuthority;

        private String mEnvironment;

        private long mExpiresOn;

        private long mExtendedExpiresOn;

        private long mCachedAt;

        private String mSpeRing;

        private String mRefreshTokenAge;

        private boolean mSuccess;

        // Exception parameters

        private String mErrorCode;

        private String mErrorMessage;

        private String mCorrelationId;

        private String mSubErrorCode;

        private String mHttpStatusCode;

        private String mHttpResponseHeaders;

        private String mHttpResponseBody;

        private String mCliTelemErrorCode;

        private String mCliTelemSubErrorCode;


        public Builder accessToken(@Nullable final String mAccessToken) {
            this.mAccessToken = mAccessToken;
            return this;
        }

        public Builder idToken(@Nullable final String mIdToken) {
            this.mIdToken = mIdToken;
            return this;
        }

        public Builder homeAccountId(@Nullable final String mHomeAccountId) {
            this.mHomeAccountId = mHomeAccountId;
            return this;
        }

        public Builder localAccountId(@Nullable final String mLocalAccountId) {
            this.mLocalAccountId = mLocalAccountId;
            return this;
        }

        public Builder userName(@Nullable final String userName) {
            this.mUserName = userName;
            return this;
        }

        public Builder tokenType(@Nullable final String mTokenType) {
            this.mTokenType = mTokenType;
            return this;
        }

        public Builder clientId(@Nullable final String clientId) {
            this.mClientId = clientId;
            return this;
        }

        public Builder scope(@Nullable final String scope) {
            this.mClientId = scope;
            return this;
        }

        public Builder clientInfo(@Nullable final String mClientInfo) {
            this.mClientInfo = mClientInfo;
            return this;
        }

        public Builder authority(@Nullable final String mAuthority) {
            this.mAuthority = mAuthority;
            return this;
        }

        public Builder environment(@Nullable final String mEnvironment) {
            this.mEnvironment = mEnvironment;
            return this;
        }

        public Builder expiresOn(long mExpiresOn) {
            this.mExpiresOn = mExpiresOn;
            return this;
        }

        public Builder extendedExpiresOn(long mExtendedExpiresOn) {
            this.mExtendedExpiresOn = mExtendedExpiresOn;
            return this;
        }

        public Builder cachedAt(long cachedAt) {
            this.mCachedAt = cachedAt;
            return this;
        }

        public Builder speRing(String speRing) {
            this.mSpeRing = speRing;
            return this;
        }

        public Builder refreshTokenAge(String refreshTokenAge) {
            this.mRefreshTokenAge = refreshTokenAge;
            return this;
        }

        public Builder success(boolean success){
            this.mSuccess = success;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.mErrorCode = errorCode;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.mErrorMessage = errorMessage;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.mCorrelationId = correlationId;
            return this;
        }

        public Builder subErrorCode(String subErrorCode) {
            this.mSubErrorCode = subErrorCode;
            return this;
        }

        public Builder httpStatusCode(String httpStatusCode) {
            this.mHttpStatusCode = httpStatusCode;
            return this;
        }

        public Builder httpResponseHeaders(String httpResponseHeaders) {
            this.mHttpResponseHeaders = httpResponseHeaders;
            return this;
        }

        public Builder httpResponseBody(String httpResponseBody) {
            this.mHttpResponseBody = httpResponseBody;
            return this;
        }

        public Builder cliTelemErrorCode(String cliTelemErrorCode) {
            this.mCliTelemErrorCode = cliTelemErrorCode;
            return this;
        }

        public Builder cliTelemSubErrorCode(String cliTelemSubErrorCode) {
            this.mCliTelemSubErrorCode = cliTelemSubErrorCode;
            return this;
        }

        public BrokerResult build(){
            return new BrokerResult(this);
        }
    }

}
