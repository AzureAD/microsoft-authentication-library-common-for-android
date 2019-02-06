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

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

/**
 * Represents the broker request
 */
public class BrokerRequest implements Parcelable {

    private class SerializedNames {
        public final static String AUTHORITY = "account.authority";
        public final static String SCOPE = "account.scope";
        public final static String REDIRECT = "account.redirect";
        public final static String CLIENT_ID = "account.clientid";
        public final static String ENVIRONMENT = "account.environment";
        public final static String REALM = "account.realm";
        public final static String HOME_ACCOUNT_ID = "account.homeAccountId";
        public final static String LOCAL_ACCOUNT_ID = "account.localAccountId";
        public final static String USERNAME = "account.username";
        public final static String CLIENT_INFO = "account.clientInfo";
        public final static String AUTHORITY_TYPE = "account.authorityType";
        public final static String EXTRA_QUERY_STRING_PARAMETER = "account.extra.query.param";
        public final static String CORRELATION_ID = "account.correlationid";
        public final static String NAME = "account.name";
        public final static String PROMPT = "account.prompt";
        public final static String CLAIMS = "account.claims";
        public final static String FORCE_REFRESH = "force.refresh";
        public final static String APPLICATION_NAME = "application.name";
        public final static String APPLICATION_UID = "application.uid";
        public final static String APPLICATION_VERSION = "application.version";
        public final static String MSAL_VERSION_KEY = "msal.version";
        public final static String EXPIRATION_BUFFER = "expiration.buffer";
    }

    @SerializedName(SerializedNames.AUTHORITY)
    private String mAuthority;

    @SerializedName(SerializedNames.SCOPE)
    private String mScope;

    @SerializedName(SerializedNames.REDIRECT)
    private String mRedirect;

    @SerializedName(SerializedNames.CLIENT_ID)
    private String mClientId;

    @SerializedName(SerializedNames.ENVIRONMENT)
    private String mEnvironment;

    @SerializedName(SerializedNames.REALM)
    private String mRealm;

    @SerializedName(SerializedNames.HOME_ACCOUNT_ID)
    private String mHomeAccountId;

    @SerializedName(SerializedNames.LOCAL_ACCOUNT_ID)
    private String mLocalAccountId;

    @SerializedName(SerializedNames.USERNAME)
    private String mUserName;

    @SerializedName(SerializedNames.CLIENT_INFO)
    private String mClientInfo;

    @SerializedName(SerializedNames.AUTHORITY_TYPE)
    private String mAuthorityType;

    @SerializedName(SerializedNames.EXTRA_QUERY_STRING_PARAMETER)
    private String mExtraQueryStringParameter;

    @SerializedName(SerializedNames.CORRELATION_ID)
    private String mCorrelationId;

    @SerializedName(SerializedNames.NAME)
    private String mName;

    @SerializedName(SerializedNames.PROMPT)
    private String mPrompt;

    @SerializedName(SerializedNames.CLAIMS)
    private String mClaims;

    @SerializedName(SerializedNames.FORCE_REFRESH)
    private boolean mForceRefresh;

    @SerializedName(SerializedNames.APPLICATION_NAME)
    private String mApplicationName;

    @SerializedName(SerializedNames.APPLICATION_UID)
    private int mUId;

    @SerializedName((SerializedNames.APPLICATION_VERSION))
    private String mApplicationVersion;

    @SerializedName(SerializedNames.MSAL_VERSION_KEY)
    private String mMsalVersion;

    @SerializedName(SerializedNames.EXPIRATION_BUFFER)
    private int mExpirationBuffer;


    public BrokerRequest() {

    }

    protected BrokerRequest(Parcel in) {
        mAuthority = in.readString();
        mScope = in.readString();
        mRedirect = in.readString();
        mClientId = in.readString();
        mEnvironment = in.readString();
        mRealm = in.readString();
        mHomeAccountId = in.readString();
        mLocalAccountId = in.readString();
        mUserName = in.readString();
        mClientInfo = in.readString();
        mAuthorityType = in.readString();
        mExtraQueryStringParameter = in.readString();
        mCorrelationId = in.readString();
        mName = in.readString();
        mPrompt = in.readString();
        mClaims = in.readString();
        int tmpMForceRefresh = in.readInt();
        mForceRefresh = tmpMForceRefresh != 0;
        mApplicationName = in.readString();
        mUId = in.readInt();
        mApplicationVersion = in.readString();
        mMsalVersion = in.readString();
        mExpirationBuffer = in.readInt();
    }

    public static final Creator<BrokerRequest> CREATOR = new Creator<BrokerRequest>() {
        @Override
        public BrokerRequest createFromParcel(Parcel in) {
            return new BrokerRequest(in);
        }

        @Override
        public BrokerRequest[] newArray(int size) {
            return new BrokerRequest[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mAuthority);
        dest.writeString(mScope);
        dest.writeString(mRedirect);
        dest.writeString(mClientId);
        dest.writeString(mEnvironment);
        dest.writeString(mRealm);
        dest.writeString(mHomeAccountId);
        dest.writeString(mLocalAccountId);
        dest.writeString(mUserName);
        dest.writeString(mClientInfo);
        dest.writeString(mAuthorityType);
        dest.writeString(mExtraQueryStringParameter);
        dest.writeString(mCorrelationId);
        dest.writeString(mName);
        dest.writeString(mPrompt);
        dest.writeString(mClaims);
        dest.writeInt(mForceRefresh ? 1 : 0);
        dest.writeString(mApplicationName);
        dest.writeInt(mUId);
        dest.writeString(mApplicationVersion);
        dest.writeString(mMsalVersion);
        dest.writeInt(mExpirationBuffer);
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


    public String getEnvironment() {
        return mEnvironment;
    }

    public void setEnvironment(String environment) {
        this.mEnvironment = environment;
    }

    public String getRealm() {
        return mRealm;
    }

    public void setRealm(String realm) {
        this.mRealm = realm;
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

    public String getClientInfo() {
        return mClientInfo;
    }

    public void setClientInfo(String clientInfo) {
        this.mClientInfo = clientInfo;
    }

    public String getAuthorityType() {
        return mAuthorityType;
    }

    public void setAuthorityType(String authorityType) {
        this.mAuthorityType = authorityType;
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

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
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

    public int getUId() {
        return mUId;
    }

    public void setUId(int uId) {
        this.mUId = uId;
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

    public int getExpirationBuffer() {
        return mExpirationBuffer;
    }

    public void setExpirationBuffer(int expirationBuffer) {
        this.mExpirationBuffer = expirationBuffer;
    }
}
