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
package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.adal.internal.ADALUserInfo;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryAccessToken;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryAccount;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryTokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AccessToken;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;
import com.microsoft.identity.common.internal.util.DateUtilities;

import java.util.Date;

public class ADALTokenCacheItem implements ITokenCacheItem {

    private ADALUserInfo mUserInfo;

    private String mResource;

    private String mAuthority;

    private String mClientId;

    private String mAccessToken;

    private String mRefreshtoken;

    private String mRawIdToken;

    /**
     * This time is GMT.
     */
    private Date mExpiresOn;

    private boolean mIsMultiResourceRefreshToken;

    private String mTenantId;

    private String mFamilyClientId;

    private Date mExtendedExpiresOn;

    private String mSpeRing;

    public ADALTokenCacheItem() {
        // Empty
    }

    ADALTokenCacheItem(final ADALTokenCacheItem tokenCacheItem) {
        mAuthority = tokenCacheItem.getAuthority();
        mResource = tokenCacheItem.getResource();
        mClientId = tokenCacheItem.getClientId();
        mAccessToken = tokenCacheItem.getAccessToken();
        mRefreshtoken = tokenCacheItem.getRefreshToken();
        mRawIdToken = tokenCacheItem.getRawIdToken();
        mUserInfo = tokenCacheItem.getUserInfo();
        mExpiresOn = tokenCacheItem.getExpiresOn();
        mIsMultiResourceRefreshToken = tokenCacheItem.getIsMultiResourceRefreshToken();
        mTenantId = tokenCacheItem.getTenantId();
        mFamilyClientId = tokenCacheItem.getFamilyClientId();
        mExtendedExpiresOn = tokenCacheItem.getExtendedExpiresOn();
        mSpeRing = tokenCacheItem.getSpeRing();
    }

    ADALTokenCacheItem(final AzureActiveDirectoryOAuth2Strategy strategy,
                       final AzureActiveDirectoryAuthorizationRequest request,
                       final AzureActiveDirectoryTokenResponse response) {
        String issuerCacheIdentifier = strategy.getIssuerCacheIdentifier(request);
        AzureActiveDirectoryAccount account = strategy.createAccount(response);
        account.setEnvironment(issuerCacheIdentifier);
        AccessToken accessToken = strategy.getAccessTokenFromResponse(response);
        RefreshToken refreshToken = strategy.getRefreshTokenFromResponse(response);

        mAuthority = issuerCacheIdentifier;
        mResource = request.getScope();
        mClientId = request.getClientId();
        mAccessToken = accessToken.getAccessToken();
        mRefreshtoken = refreshToken.getRefreshToken();
        mRawIdToken = response.getIdToken();
        mUserInfo = new ADALUserInfo(account);
        mTenantId = account.getRealm();
        mExpiresOn = ((AzureActiveDirectoryAccessToken) accessToken).getExpiresOn();
        mExtendedExpiresOn = ((AzureActiveDirectoryAccessToken) accessToken).getExtendedExpiresOn();
        mIsMultiResourceRefreshToken = true;
        mFamilyClientId = (refreshToken).getFamilyId();
        mSpeRing = (response).getSpeRing();
    }

    /**
     * Coerce the supplied {@link ADALTokenCacheItem} to an MRRT.
     *
     * @param originalCacheItem The ADALTokenCacheItem to transform.
     * @return The supplied ADALTokenCacheItem as an MRRT.
     */
    public static ADALTokenCacheItem getAsMRRTTokenCacheItem(ADALTokenCacheItem originalCacheItem) {
        ADALTokenCacheItem newItem = new ADALTokenCacheItem(originalCacheItem);
        newItem.setResource(null);
        newItem.setAccessToken(null);
        return newItem;
    }

    /**
     * Coerce the supplied {@link ADALTokenCacheItem} to an FRT.
     *
     * @param originalCacheItem The ADALTokenCacheItem to transform.
     * @return The supplied ADALTokenCacheItem as an FRT.
     */
    public static ADALTokenCacheItem getAsFRTTokenCacheItem(ADALTokenCacheItem originalCacheItem) {
        ADALTokenCacheItem newItem = new ADALTokenCacheItem(originalCacheItem);
        newItem.setResource(null);
        newItem.setAccessToken(null);
        newItem.setClientId(null);
        return newItem;
    }


    String getSpeRing() {
        return mSpeRing;
    }

    void setSpeRing(final String speRing) {
        mSpeRing = speRing;
    }

    /**
     * Get the user information.
     *
     * @return UserInfo object.
     */
    public ADALUserInfo getUserInfo() {
        return mUserInfo;
    }

    /**
     * Set the user information.
     *
     * @param info UserInfo object which contains user information.
     */
    public void setUserInfo(final ADALUserInfo info) {
        mUserInfo = info;
    }

    @Override
    public String getResource() {
        return mResource;
    }

    /**
     * Set the resource.
     *
     * @param resource resource identifier.
     */
    public void setResource(final String resource) {
        mResource = resource;
    }

    @Override
    public String getAuthority() {
        return mAuthority;
    }

    /**
     * Set the authority.
     *
     * @param authority String authority url.
     */
    public void setAuthority(final String authority) {
        mAuthority = authority;
    }

    @Override
    public String getClientId() {
        return mClientId;
    }

    /**
     * Set the client identifier.
     *
     * @param clientId client identifier string.
     */
    public void setClientId(final String clientId) {
        mClientId = clientId;
    }

    /**
     * Get the access token.
     *
     * @return the access token string.
     */
    public String getAccessToken() {
        return mAccessToken;
    }

    /**
     * Set the access token string.
     *
     * @param accessToken the access token string.
     */
    public void setAccessToken(final String accessToken) {
        mAccessToken = accessToken;
    }

    @Override
    public String getRefreshToken() {
        return mRefreshtoken;
    }

    /**
     * Set the fresh token string.
     *
     * @param refreshToken the refresh token string.
     */
    public void setRefreshToken(final String refreshToken) {
        mRefreshtoken = refreshToken;
    }

    /**
     * Get the expire date.
     *
     * @return the time the token get expired.
     */
    public Date getExpiresOn() {
        return DateUtilities.createCopy(mExpiresOn);
    }

    /**
     * Set the expire date.
     *
     * @param expiresOn the expire time.
     */
    public void setExpiresOn(final Date expiresOn) {
        mExpiresOn = DateUtilities.createCopy(expiresOn);
    }

    /**
     * Get the multi-resource refresh token flag.
     *
     * @return true if the token is a multi-resource refresh token, else return false.
     */
    public boolean getIsMultiResourceRefreshToken() {
        return mIsMultiResourceRefreshToken;
    }

    /**
     * Set the multi-resource refresh token flag.
     *
     * @param isMultiResourceRefreshToken true if the token is a multi-resource refresh token.
     */
    public void setIsMultiResourceRefreshToken(boolean isMultiResourceRefreshToken) {
        mIsMultiResourceRefreshToken = isMultiResourceRefreshToken;
    }

    /**
     * Get tenant identifier.
     *
     * @return the tenant identifier string.
     */
    public String getTenantId() {
        return mTenantId;
    }

    /**
     * Set tenant identifier.
     *
     * @param tenantId the tenant identifier string.
     */
    public void setTenantId(final String tenantId) {
        mTenantId = tenantId;
    }

    /**
     * Get raw ID token.
     *
     * @return raw ID token string.
     */
    public String getRawIdToken() {
        return mRawIdToken;
    }

    /**
     * Set raw ID token.
     *
     * @param rawIdToken raw ID token string.
     */
    public void setRawIdToken(final String rawIdToken) {
        mRawIdToken = rawIdToken;
    }

    /**
     * Get family client identifier.
     *
     * @return the family client ID string.
     */
    public final String getFamilyClientId() {
        return mFamilyClientId;
    }

    /**
     * Set family client identifier.
     *
     * @param familyClientId the family client ID string.
     */
    public final void setFamilyClientId(final String familyClientId) {
        mFamilyClientId = familyClientId;
    }

    /**
     * Set the extended expired time.
     *
     * @param extendedExpiresOn extended expired date.
     */
    public final void setExtendedExpiresOn(final Date extendedExpiresOn) {
        mExtendedExpiresOn = DateUtilities.createCopy(extendedExpiresOn);
    }

    /**
     * Get the extended expired time.
     *
     * @return the extended expired date.
     */
    public final Date getExtendedExpiresOn() {
        return DateUtilities.createCopy(mExtendedExpiresOn);
    }
}
