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

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.Expose;
import com.microsoft.identity.common.exception.ArgumentException;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.dto.IAccountRecord;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.ui.browser.BrowserDescriptor;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class OperationParameters {

    private static final String TAG = OperationParameters.class.getSimpleName();

    private transient Context mAppContext;
    private transient OAuth2TokenCache mTokenCache;
    private transient boolean mIsSharedDevice;
    protected transient List<BrowserDescriptor> mBrowserSafeList;

    @Expose()
    private Set<String> mScopes;
    protected IAccountRecord mAccount;
    @Expose()
    private String clientId;
    @Expose()
    private String redirectUri;
    @Expose()
    private Authority mAuthority;
    @Expose()
    private String mClaimsRequestJson;
    @Expose()
    private SdkType mSdkType = SdkType.MSAL; // default value where we get a v2 id token;
    @Expose()
    private String mSdkVersion;
    @Expose()
    private String mApplicationName;
    @Expose()
    private String mApplicationVersion;
    @Expose()
    private String mRequiredBrokerProtocolVersion; //Move the required broker protocol var into parent class, as the interactive call also needs Bound Service.
    @Expose()
    private boolean mForceRefresh;
    @Expose
    private String mCorrelationId;

    public String getRequiredBrokerProtocolVersion() {
        return mRequiredBrokerProtocolVersion;
    }

    public void setRequiredBrokerProtocolVersion(@NonNull final String requiredBrokerProtocolVersion) {
        mRequiredBrokerProtocolVersion = requiredBrokerProtocolVersion;
    }

    public SdkType getSdkType() {
        return mSdkType;
    }

    public void setSdkType(@Nullable final SdkType sdkType) {
        this.mSdkType = sdkType;
    }

    public Context getAppContext() {
        return mAppContext;
    }

    public void setAppContext(@NonNull final Context mAppContext) {
        this.mAppContext = mAppContext;
    }

    public boolean getIsSharedDevice() {
        return mIsSharedDevice;
    }

    public void setIsSharedDevice(@NonNull final boolean isSharedDevice) {
        this.mIsSharedDevice = isSharedDevice;
    }

    public Set<String> getScopes() {
        return mScopes;
    }

    public void setScopes(@NonNull final Set<String> mScopes) {
        this.mScopes = mScopes;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(@NonNull final String clientId) {
        this.clientId = clientId;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(@NonNull final String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public void setTokenCache(@NonNull final OAuth2TokenCache cache) {
        this.mTokenCache = cache;
    }

    public OAuth2TokenCache getTokenCache() {
        return mTokenCache;
    }

    public Authority getAuthority() {
        return mAuthority;
    }

    public void setAuthority(@NonNull final Authority authority) {
        this.mAuthority = authority;
    }

    public void setAccount(@Nullable final IAccountRecord account) {
        mAccount = account;
    }

    public IAccountRecord getAccount() {
        return mAccount;
    }

    public String getClaimsRequestJson() {
        return mClaimsRequestJson;
    }

    public void setClaimsRequest(@Nullable final String claimsRequestJson) {
        mClaimsRequestJson = claimsRequestJson;
    }

    public String getSdkVersion() {
        return mSdkVersion;
    }

    public void setSdkVersion(@Nullable final String sdkVersion) {
        mSdkVersion = sdkVersion;
    }

    public String getApplicationName() {
        return mApplicationName;
    }

    public void setApplicationName(@Nullable final String applicationName) {
        mApplicationName = applicationName;
    }

    public String getApplicationVersion() {
        return mApplicationVersion;
    }

    public void setApplicationVersion(@Nullable final String applicationVersion) {
        mApplicationVersion = applicationVersion;
    }

    public void setForceRefresh(final boolean forceRefresh) {
        mForceRefresh = forceRefresh;
    }

    public boolean getForceRefresh() {
        return mForceRefresh;
    }

    public String getCorrelationId() {
        return mCorrelationId;
    }

    public void setCorrelationId(final String correlationId) {
        this.mCorrelationId = correlationId;
    }

    public void setBrowserSafeList(final List<BrowserDescriptor> browserSafeList) {
        this.mBrowserSafeList = browserSafeList;
    }

    /**
     * Get the list of browsers which are safe to launch for auth flow.
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
        if (!(o instanceof OperationParameters)) return false;

        OperationParameters that = (OperationParameters) o;

        if (mForceRefresh != that.mForceRefresh) return false;
        if (mScopes != null ? !mScopes.equals(that.mScopes) : that.mScopes != null) return false;
        if (mAccount != null ? !mAccount.equals(that.mAccount) : that.mAccount != null)
            return false;
        if (!getClientId().equals(that.getClientId())) return false;
        if (getRedirectUri() != null ? !getRedirectUri().equals(that.getRedirectUri()) : that.getRedirectUri() != null)
            return false;
        if (mAuthority != null ? !mAuthority.equals(that.mAuthority) : that.mAuthority != null)
            return false;
        return mClaimsRequestJson != null ? mClaimsRequestJson.equals(that.mClaimsRequestJson) : that.mClaimsRequestJson == null;
    }
    //CHECKSTYLE:ON

    //CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @SuppressWarnings("PMD")
    @Override
    public int hashCode() {
        int result = mScopes != null ? mScopes.hashCode() : 0;
        result = 31 * result + (mAccount != null ? mAccount.hashCode() : 0);
        result = 31 * result + getClientId().hashCode();
        result = 31 * result + (getRedirectUri() != null ? getRedirectUri().hashCode() : 0);
        result = 31 * result + (mAuthority != null ? mAuthority.hashCode() : 0);
        result = 31 * result + (mClaimsRequestJson != null ? mClaimsRequestJson.hashCode() : 0);
        result = 31 * result + (mForceRefresh ? 1 : 0);
        return result;
    }
    //CHECKSTYLE:ON


    /**
     * Since this is about validating MSAL Parameters and not an authorization request or token request.  I've placed this here.
     */
    public void validate() throws ArgumentException {
        final String methodName = ":validate";
        Logger.verbose(
                TAG + methodName,
                "Validating operation params..."
        );
        Boolean validScopeArgument = false;

        if (mScopes != null) {
            mScopes.removeAll(Arrays.asList("", null));
            if (mScopes.size() > 0) {
                validScopeArgument = true;
            }
        }

        if (!validScopeArgument) {
            if (this instanceof AcquireTokenSilentOperationParameters) {
                throw new ArgumentException(
                        ArgumentException.ACQUIRE_TOKEN_SILENT_OPERATION_NAME,
                        ArgumentException.SCOPE_ARGUMENT_NAME,
                        "scope is empty or null"
                );
            }
            if (this instanceof AcquireTokenOperationParameters) {
                throw new ArgumentException(
                        ArgumentException.ACQUIRE_TOKEN_OPERATION_NAME,
                        ArgumentException.SCOPE_ARGUMENT_NAME,
                        "scope is empty or null");
            }
        }

    }
}
