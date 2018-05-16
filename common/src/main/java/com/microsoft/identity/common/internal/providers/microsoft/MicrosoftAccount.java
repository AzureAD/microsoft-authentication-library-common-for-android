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
package com.microsoft.identity.common.internal.providers.microsoft;

import android.net.Uri;

import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.adal.internal.util.DateExtensions;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.cache.SchemaUtil;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryIdToken;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

public abstract class MicrosoftAccount extends Account {

    private static final String TAG = MicrosoftAccount.class.getSimpleName();

    protected String mDisplayableId; // Legacy Identifier -  UPN (preferred) or Email
    protected String mUniqueId; // Legacy Identifier - Object Id (preferred) or Subject

    protected String mName;
    protected String mIdentityProvider;
    protected String mUid;
    protected String mUtid;
    protected IDToken mIDToken;
    protected Uri mPasswordChangeUrl;
    protected Date mPasswordExpiresOn;
    protected String mTenantId; // Tenant Id of the authority that issued the idToken... not necessarily the home tenant of the account
    protected String mGivenName;
    protected String mFamilyName;

    public MicrosoftAccount() {
        super();
        Logger.verbose(TAG, "Init: " + TAG);
    }

    public MicrosoftAccount(final IDToken idToken, final String uid, final String utid) {
        Logger.verbose(TAG, "Init: " + TAG);
        mIDToken = idToken;
        final Map<String, String> claims = idToken.getTokenClaims();
        mUniqueId = getUniqueId(claims);
        mDisplayableId = getDisplayableId(claims);
        mName = claims.get(AzureActiveDirectoryIdToken.NAME);
        mIdentityProvider = claims.get(AzureActiveDirectoryIdToken.ISSUER);
        mGivenName = claims.get(AzureActiveDirectoryIdToken.GIVEN_NAME);
        mFamilyName = claims.get(AzureActiveDirectoryIdToken.FAMILY_NAME);
        mTenantId = claims.get(AzureActiveDirectoryIdToken.TENANT_ID);
        mUid = uid;
        mUtid = utid;

        long mPasswordExpiration = 0;

        if (!StringExtensions.isNullOrBlank(claims.get(AzureActiveDirectoryIdToken.PASSWORD_EXPIRATION))) {
            mPasswordExpiration = Long.parseLong(claims.get(AzureActiveDirectoryIdToken.PASSWORD_EXPIRATION));
        }

        if (mPasswordExpiration > 0) {
            // pwd_exp returns seconds to expiration time
            // it returns in seconds. Date accepts milliseconds.
            Calendar expires = new GregorianCalendar();
            expires.add(Calendar.SECOND, (int) mPasswordExpiration);
            mPasswordExpiresOn = expires.getTime();
        }

        mPasswordChangeUrl = null;
        if (!StringExtensions.isNullOrBlank(claims.get(AzureActiveDirectoryIdToken.PASSWORD_CHANGE_URL))) {
            mPasswordChangeUrl = Uri.parse(claims.get(AzureActiveDirectoryIdToken.PASSWORD_CHANGE_URL));
        }
    }

    protected abstract String getDisplayableId(final Map<String, String> claims);

    private String getUniqueId(final Map<String, String> claims) {
        final String methodName = "getUniqueId";
        Logger.entering(TAG, methodName, claims);

        String uniqueId = null;

        if (!StringExtensions.isNullOrBlank(claims.get(AzureActiveDirectoryIdToken.OJBECT_ID))) {
            Logger.info(TAG + ":" + methodName, "Using ObjectId as uniqueId");
            uniqueId = claims.get(AzureActiveDirectoryIdToken.OJBECT_ID);
        } else if (!StringExtensions.isNullOrBlank(claims.get(AzureActiveDirectoryIdToken.SUBJECT))) {
            Logger.info(TAG + ":" + methodName, "Using Subject as uniqueId");
            uniqueId = claims.get(AzureActiveDirectoryIdToken.SUBJECT);
        }

        Logger.exiting(TAG, methodName, uniqueId);

        return uniqueId;
    }

    public void setFirstName(final String givenName) {
        mGivenName = givenName;
    }

    public void setLastName(final String familyName) {
        mFamilyName = familyName;
    }

    /**
     * @return The displayable value in the UserPrincipleName(UPN) format. Can be null if not
     * returned from the service.
     */
    public String getDisplayableId() {
        return mDisplayableId;
    }

    /**
     * Sets the displayableId of a user when making acquire token API call.
     *
     * @param displayableId
     */
    public void setDisplayableId(final String displayableId) {
        mDisplayableId = displayableId;
    }

    /**
     * @return The given name of the user. Can be null if not returned from the service.
     */
    public String getName() {
        return mName;
    }

    /**
     * @return The identity provider of the user authenticated. Can be null if not returned from the service.
     */
    public String getIdentityProvider() {
        return mIdentityProvider;
    }

    /**
     * @return The unique identifier of the user, which is across tenant.
     */
    public String getUserId() {
        return mUniqueId;
    }

    /**
     * Gets the uid.
     *
     * @return The uid to get.
     */
    public String getUid() {
        return mUid;
    }

    /**
     * Sets the uid.
     *
     * @param uid The uid to set.
     */
    public void setUid(final String uid) {
        mUid = uid;
    }

    /**
     * Sets the utid.
     *
     * @param uTid The utid to set.
     */
    public void setUtid(final String uTid) {
        mUtid = uTid;
    }

    /**
     * Sets the name.
     *
     * @param name The name to set.
     */
    public void setName(final String name) {
        mName = name;
    }

    /**
     * Sets the identity provider.
     *
     * @param idp The identity provider to set.
     */
    public void setIdentityProvider(final String idp) {
        mIdentityProvider = idp;
    }

    /**
     * Gets the utid.
     *
     * @return The utid to get.
     */
    public String getUtid() {
        return mUtid;
    }

    void setUserId(final String userid) {
        mUniqueId = userid;
    }

    /**
     * Return the unique identifier for the account...
     *
     * @return
     */
    @Override
    public String getUniqueIdentifier() {
        return StringExtensions.base64UrlEncodeToString(mUid) + "." + StringExtensions.base64UrlEncodeToString(mUtid);
    }

    @Override
    public List<String> getCacheIdentifiers() {
        List<String> cacheIdentifiers = new ArrayList<>();

        if (mDisplayableId != null) {
            cacheIdentifiers.add(mDisplayableId);
        }

        if (mUniqueId != null) {
            cacheIdentifiers.add(mUniqueId);
        }

        if (getUniqueIdentifier() != null) {
            cacheIdentifiers.add(getUniqueIdentifier());
        }

        return cacheIdentifiers;
    }

    /**
     * Gets password change url.
     *
     * @return the password change uri
     */
    public Uri getPasswordChangeUrl() {
        return mPasswordChangeUrl;
    }

    /**
     * Gets password expires on.
     *
     * @return the time when the password will expire
     */
    public Date getPasswordExpiresOn() {
        return DateExtensions.createCopy(mPasswordExpiresOn);
    }

    public IDToken getIDToken() {
        return mIDToken;
    }

    @Override
    public String getUniqueUserId() {
        // TODO -- This method's functionality is duplicative of
        // Account#getUniqueIdentifier except that that implementation
        // was coded for the refactored ADAL cache which expects
        // uid/utid to be base64 encoded for legacy support.
        return getUid() + "." + getUtid();
    }

    @Override
    public String getEnvironment() {
        return SchemaUtil.getEnvironment(mIDToken);
    }

    @Override
    public String getRealm() {
        return mTenantId;
    }

    @Override
    public String getAuthorityAccountId() {
        return getUserId();
    }

    @Override
    public String getUsername() {
        return getDisplayableId();
    }

    @Override
    public String getGuestId() {
        return SchemaUtil.getGuestId(mIDToken);
    }

    @Override
    public String getFirstName() {
        return mGivenName;
    }

    @Override
    public String getLastName() {
        return mFamilyName;
    }

    @Override
    public String getAvatarUrl() {
        return SchemaUtil.getAvatarUrl(mIDToken);
    }

    @Override
    public String toString() {
        return "MicrosoftAccount{" +
                "mDisplayableId='" + mDisplayableId + '\'' +
                ", mUniqueId='" + mUniqueId + '\'' +
                ", mName='" + mName + '\'' +
                ", mIdentityProvider='" + mIdentityProvider + '\'' +
                ", mUid='" + mUid + '\'' +
                ", mUtid='" + mUtid + '\'' +
                ", mIDToken=" + mIDToken +
                ", mPasswordChangeUrl=" + mPasswordChangeUrl +
                ", mPasswordExpiresOn=" + mPasswordExpiresOn +
                ", mTenantId='" + mTenantId + '\'' +
                ", mGivenName='" + mGivenName + '\'' +
                ", mFamilyName='" + mFamilyName + '\'' +
                "} " + super.toString();
    }
}
