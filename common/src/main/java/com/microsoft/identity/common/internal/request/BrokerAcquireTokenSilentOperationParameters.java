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
package com.microsoft.identity.common.internal.request;

import android.accounts.Account;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.exception.ArgumentException;
import com.microsoft.identity.common.internal.authscheme.BearerAuthenticationSchemeInternal;
import com.microsoft.identity.common.internal.broker.BrokerValidator;
import com.microsoft.identity.common.internal.cache.BrokerOAuth2TokenCache;

import java.util.List;

public class BrokerAcquireTokenSilentOperationParameters extends AcquireTokenSilentOperationParameters {
    /**
     * This is the Android Account Manager's {@link Account}
     */
    private Account mAccountManagerAccount;

    private String mCallerPackageName;

    private int mCallerUId;

    private String mCallerAppVersion;

    private String mHomeAccountId; // Home account id be null if the request if from Adal

    private String mLocalAccountId;

    private String mLoginHint;

    private List<Pair<String, String>> mExtraQueryStringParameters;

    private String mBrokerVersion;

    // Device state might not be propagated to MSODS yet, so we might want to wait before re-acquiring PRT.
    private int mSleepTimeBeforePrtAcquisition;

    public Account getAccountManagerAccount() {
        return mAccountManagerAccount;
    }

    public void setAccountManagerAccount(final Account accountManagerAccount) {
        this.mAccountManagerAccount = accountManagerAccount;
    }

    public String getCallerPackageName() {
        return mCallerPackageName;
    }

    public void setCallerPackageName(final String callerPackageName) {
        this.mCallerPackageName = callerPackageName;
    }

    public int getCallerUId() {
        return mCallerUId;
    }

    public String getCallerAppVersion() {
        return mCallerAppVersion;
    }

    public void setCallerAppVersion(final String callerAppVersion) {
        this.mCallerAppVersion = callerAppVersion;
    }

    public void setCallerUId(int callerUId) {
        this.mCallerUId = callerUId;
    }

    public String getHomeAccountId() {
        return mHomeAccountId;
    }

    public void setHomeAccountId(final String homeAccountId) {
        this.mHomeAccountId = homeAccountId;
    }

    public String getLocalAccountId() {
        return mLocalAccountId;
    }

    public void setLocalAccountId(String localAccountId) {
        this.mLocalAccountId = localAccountId;
    }

    public String getLoginHint() {
        return mLoginHint;
    }

    public void setLoginHint(final String loginHint) {
        this.mLoginHint = loginHint;
    }

    public List<Pair<String, String>> getExtraQueryStringParameters() {
        return mExtraQueryStringParameters;
    }

    public void setExtraQueryStringParameters(final List<Pair<String, String>> mExtraQueryStringParameters) {
        this.mExtraQueryStringParameters = mExtraQueryStringParameters;
    }

    public int getSleepTimeBeforePrtAcquisition() {
        return mSleepTimeBeforePrtAcquisition;
    }

    public void setSleepTimeBeforePrtAcquisition(final int sleepTimeBeforePrtAcquisition) {
        mSleepTimeBeforePrtAcquisition = sleepTimeBeforePrtAcquisition;
    }

    public String getBrokerVersion() {
        return mBrokerVersion;
    }

    public void setBrokerVersion(String brokerVersion) {
        this.mBrokerVersion = brokerVersion;
    }

    public BrokerAcquireTokenSilentOperationParameters() {

    }

    /**
     * Constructor to create BrokerAcquireTokenSilentOperationParameters from BrokerAcquireTokenOperationParameters
     *
     * @param acquireTokenOperationParameters
     */

    // TODO : Ideally this constructor should be removed/refactor in future when AcquireTokenParameters
    // can be a subclass of AcquireTokenSilentParameters in common and pattern can be followed with Broker parameters

    // Caveat : Currently this constructor should only be used  in Joined Account case.
    // The specific use case is when an interactive call is completed to get BrokerRT and acquireTokenSilent
    // token needs to be called to get token using PRT.
    // There won't be any homeaccountId or Account information here and hence should not be used for
    // Non joined silent case.
    public BrokerAcquireTokenSilentOperationParameters(@NonNull final BrokerAcquireTokenOperationParameters
                                                               acquireTokenOperationParameters) {
        setAppContext(acquireTokenOperationParameters.getAppContext());
        setTokenCache(acquireTokenOperationParameters.getTokenCache());
        setScopes(acquireTokenOperationParameters.getScopes());
        setClientId(acquireTokenOperationParameters.getClientId());
        setRedirectUri(acquireTokenOperationParameters.getRedirectUri());
        setAuthority(acquireTokenOperationParameters.getAuthority());
        setClaimsRequest(acquireTokenOperationParameters.getClaimsRequestJson());
        setCallerAppVersion(acquireTokenOperationParameters.getCallerAppVersion());
        setCallerPackageName(acquireTokenOperationParameters.getCallerPackageName());
        setCallerUId(acquireTokenOperationParameters.getCallerUId());
        setCorrelationId(acquireTokenOperationParameters.getCorrelationId());
        setLoginHint(acquireTokenOperationParameters.getLoginHint());
        setSdkType(acquireTokenOperationParameters.getSdkType());
        setExtraQueryStringParameters(
                acquireTokenOperationParameters.getExtraQueryStringParameters()
        );
        setAuthenticationScheme(
                null != acquireTokenOperationParameters.getAuthenticationScheme()
                        ? acquireTokenOperationParameters.getAuthenticationScheme()
                        : new BearerAuthenticationSchemeInternal() // If null, assume Bearer for back-compat
        );
    }

    @Override
    public void validate() throws ArgumentException {
        if (mCallerUId == 0) {
            throw new ArgumentException(
                    ArgumentException.ACQUIRE_TOKEN_SILENT_OPERATION_NAME,
                    "mCallerUId", "Caller Uid is not set"
            );
        }
        if (TextUtils.isEmpty(mCallerPackageName)) {
            throw new ArgumentException(
                    ArgumentException.ACQUIRE_TOKEN_SILENT_OPERATION_NAME,
                    "mCallerPackageName", "Caller package name is not set"
            );
        }
        if (getAuthority() == null) {
            throw new ArgumentException(
                    ArgumentException.ACQUIRE_TOKEN_SILENT_OPERATION_NAME,
                    "mAuthority", "Authority Url is not set"
            );
        }
        if (getScopes() == null || getScopes().isEmpty()) {
            throw new ArgumentException(
                    ArgumentException.ACQUIRE_TOKEN_SILENT_OPERATION_NAME,
                    "mScopes", "Scope or resource is not set"
            );
        }
        if (TextUtils.isEmpty(getClientId())) {
            throw new ArgumentException(
                    ArgumentException.ACQUIRE_TOKEN_SILENT_OPERATION_NAME,
                    "mClientId", "Client Id is not set"
            );
        }
        if (TextUtils.isEmpty(mCallerPackageName)) {
            throw new ArgumentException(
                    ArgumentException.ACQUIRE_TOKEN_SILENT_OPERATION_NAME,
                    "mCallerPackageName", "Caller package name is not set"
            );
        }
        if (SdkType.MSAL == getSdkType() &&
                !BrokerValidator.isValidBrokerRedirect(getRedirectUri(), getAppContext(), getCallerPackageName())) {
            throw new ArgumentException(
                    ArgumentException.ACQUIRE_TOKEN_SILENT_OPERATION_NAME,
                    "mRedirectUri", "The redirect URI doesn't match the uri" +
                    " generated with caller package name and signature"
            );
        }

        if (!(getTokenCache() instanceof BrokerOAuth2TokenCache)) {
            throw new ArgumentException(
                    ArgumentException.ACQUIRE_TOKEN_SILENT_OPERATION_NAME,
                    "AcquireTokenSilentOperationParameters",
                    "OAuth2Cache not an instance of BrokerOAuth2TokenCache"
            );
        }
        if (null == mAccountManagerAccount) {
            throw new ArgumentException(
                    ArgumentException.ACQUIRE_TOKEN_SILENT_OPERATION_NAME,
                    "mCallerPackageName", "Android Account manager Account is null"
            );
        }

    }

}
