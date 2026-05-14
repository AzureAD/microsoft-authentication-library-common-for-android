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
package com.microsoft.identity.common.java.result;

import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.broker.IBrokerInfoProvider;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.java.broker.BrokerPerformanceMetrics;
import com.microsoft.identity.common.java.broker.IBrokerPerformanceMetricsProvider;
import com.microsoft.identity.common.java.telemetry.ClientDataInfo;

import javax.annotation.Nullable;

public class AcquireTokenResult implements IBrokerPerformanceMetricsProvider, IBrokerInfoProvider {

    private ILocalAuthenticationResult mLocalAuthenticationResult;
    private TokenResult mTokenResult;

    @SuppressWarnings(WarningType.rawtype_warning)
    private AuthorizationResult mAuthorizationResult;

    private Boolean mSucceeded = false;

    @Nullable
    private String mBrokerAppVersion;

    @Nullable
    private String mBrokerAppPackageName;

    private BrokerPerformanceMetrics mBrokerPerformanceMetrics;

    public void setLocalAuthenticationResult(ILocalAuthenticationResult result) {
        this.mLocalAuthenticationResult = result;
        this.mSucceeded = true;
    }

    public ILocalAuthenticationResult getLocalAuthenticationResult() {
        return this.mLocalAuthenticationResult;
    }

    public TokenResult getTokenResult() {
        return mTokenResult;
    }

    public void setTokenResult(TokenResult tokenResult) {
        this.mTokenResult = tokenResult;
    }

    public void setBrokerPerformanceMetrics(BrokerPerformanceMetrics brokerPerformanceMetrics) {
        this.mBrokerPerformanceMetrics = brokerPerformanceMetrics;
    }

    @Override
    public BrokerPerformanceMetrics getBrokerPerformanceMetrics() {
        return this.mBrokerPerformanceMetrics;
    }

    // Suppressing rawtype warnings due to the generic type AuthorizationResult
    @SuppressWarnings(WarningType.rawtype_warning)
    public AuthorizationResult getAuthorizationResult() {
        return mAuthorizationResult;
    }

    public void setAuthorizationResult(@SuppressWarnings(WarningType.rawtype_warning) AuthorizationResult authorizationResult) {
        this.mAuthorizationResult = authorizationResult;
    }

    public Boolean getSucceeded() {
        return mSucceeded;
    }

    public void setBrokerAppVersion(final String brokerVersion) {
        this.mBrokerAppVersion = brokerVersion;
    }

    public void setBrokerAppPackageName(final String brokerPackageName) {
        this.mBrokerAppPackageName = brokerPackageName;
    }

    @Override
    public String getBrokerAppVersion() {
        return mBrokerAppVersion;
    }

    @Override
    public String getBrokerAppPackageName() {
        return mBrokerAppPackageName;
    }

    /**
     * Gets the {@link ClientDataInfo} containing server-side telemetry data from the
     * x-ms-clientdata response header (/token) or clientdata redirect query parameter (/authorize).
     *
     * @return The ClientDataInfo, or null if not available.
     */
    @Nullable
    public ClientDataInfo getClientDataInfo() {
        if (mLocalAuthenticationResult instanceof LocalAuthenticationResult) {
            return ((LocalAuthenticationResult) mLocalAuthenticationResult).getClientDataInfo();
        }
        return null;
    }
}
