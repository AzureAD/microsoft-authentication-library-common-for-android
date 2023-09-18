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
package com.microsoft.identity.common.java.providers.microsoft;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.java.commands.parameters.IHasExtraParameters;
import com.microsoft.identity.common.java.providers.oauth2.TokenRequest;

import java.util.UUID;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class MicrosoftTokenRequest extends TokenRequest implements IHasExtraParameters {

    public static final String CODE_VERIFIER = "code_verifier";
    public static final String CLIENT_INFO = "client_info";
    public static final String CORRELATION_ID = "client-request-id";
    public static final String ID_TOKEN_VERSION = "itver";
    public static final String MAM_VERSION = "mamver";
    public static final String CLAIMS = "claims";
    public static final String INSTANCE_AWARE = "instance_aware";
    public static final String CLIENT_APP_NAME = "x-app-name";
    public static final String CLIENT_APP_VERSION = "x-app-ver";
    public static final String MICROSOFT_ENROLLMENT_ID = "microsoft_enrollment_id";
    public static final String DEVICE_CODE = "device_code";

    public MicrosoftTokenRequest() {
        mClientInfoEnabled = "1";
    }

    @SerializedName(CODE_VERIFIER)
    private String mCodeVerifier;

    @Expose()
    @SerializedName(CLIENT_INFO)
    private String mClientInfoEnabled;

    @Expose()
    @SerializedName(CORRELATION_ID)
    private UUID mCorrelationId;

    @Expose()
    @SerializedName(ID_TOKEN_VERSION)
    private String mIdTokenVersion;

    @Expose()
    @SerializedName(MAM_VERSION)
    private String mMamVersion;

    @Expose()
    @SerializedName(CLAIMS)
    private String mClaims;

    @Expose()
    @SerializedName(INSTANCE_AWARE)
    private String mInstanceAware;

    @Expose()
    @SerializedName(CLIENT_APP_NAME)
    private String mClientAppName;

    @Expose()
    @SerializedName(CLIENT_APP_VERSION)
    private String mClientAppVersion;

    @Expose()
    @SerializedName(MICROSOFT_ENROLLMENT_ID)
    private String mMicrosoftEnrollmentId;

    @Expose()
    @SerializedName(DEVICE_CODE)
    private String mDeviceCode;

    private String mTokenScope;

    // Sent as part of headers if available, so marking it transient.
    private transient String mBrokerVersion;

    // Send PKeyAuth Header to token endpoint for required msal-broker protocol version 9.0.
    @Getter
    @Setter
    @Accessors(prefix = "m")
    private boolean mPKeyAuthHeaderAllowed;

    @Getter
    @Setter
    @Accessors(prefix = "m")
    private boolean mPasskeyAuthHeaderAllowed;

    public String getCodeVerifier() {
        return this.mCodeVerifier;
    }

    public void setCodeVerifier(String codeVerifier) {
        this.mCodeVerifier = codeVerifier;
    }

    public String getClientInfoEnabled() {
        return this.mClientInfoEnabled;
    }

    public void setCorrelationId(UUID correlationId) {
        mCorrelationId = correlationId;
    }

    public UUID getCorrelationId() {
        return mCorrelationId;
    }

    public String getIdTokenVersion() {
        return mIdTokenVersion;
    }

    public void setIdTokenVersion(final String mIdTokenVersion) {
        this.mIdTokenVersion = mIdTokenVersion;
    }

    public String getClaims() {
        return mClaims;
    }

    public void setClaims(final String claims) {
        this.mClaims = claims;
    }

    public String getInstanceAware() {
        return mInstanceAware;
    }

    public void setInstanceAware(final String instanceAware) {
        this.mInstanceAware = instanceAware;
    }

    public String getClientAppName() {
        return mClientAppName;
    }

    public void setClientAppName(String clientAppName) {
        this.mClientAppName = clientAppName;
    }

    public String getTokenScope() {
        return mTokenScope;
    }

    public void setTokenScope(String tokenScope) {
        this.mTokenScope = tokenScope;
    }

    public String getClientAppVersion() {
        return mClientAppVersion;
    }

    public void setClientAppVersion(final String clientAppVersion) {
        this.mClientAppVersion = clientAppVersion;
    }

    public String getMamVersion() {
        return mMamVersion;
    }

    public void setMamversion(final String mamVersion) {
        this.mMamVersion = mamVersion;
    }

    public String getBrokerVersion() {
        return mBrokerVersion;
    }

    public void setBrokerVersion(final String brokerVersion) {
        this.mBrokerVersion = brokerVersion;
    }

    public String getMicrosoftEnrollmentId() {
        return mMicrosoftEnrollmentId;
    }

    public void setMicrosoftEnrollmentId(String microsoftEnrollmentId) {
        this.mMicrosoftEnrollmentId = microsoftEnrollmentId;
    }

    @Nullable
    public String getDeviceCode() {
        return mDeviceCode;
    }

    public void setDeviceCode(final String deviceCode) {
        this.mDeviceCode = deviceCode;
    }
}
