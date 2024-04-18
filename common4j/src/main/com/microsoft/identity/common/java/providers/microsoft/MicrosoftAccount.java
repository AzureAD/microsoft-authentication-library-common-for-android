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

import com.microsoft.identity.common.java.BaseAccount;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryIdToken;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.ClientInfo;
import com.microsoft.identity.common.java.providers.oauth2.IDToken;
import com.microsoft.identity.common.java.util.Base64;
import com.microsoft.identity.common.java.util.CopyUtil;
import com.microsoft.identity.common.java.util.SchemaUtil;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.base64.Base64Flags;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Accessors(prefix = "m")
public abstract class MicrosoftAccount extends BaseAccount {

    public static final String AUTHORITY_TYPE_MS_STS = "MSSTS";

    private static final String TAG = MicrosoftAccount.class.getSimpleName();

    private String mDisplayableId; // Legacy Identifier -  UPN (preferred) or Email
    private String mUserId; // Legacy Identifier - Object Id (preferred) or Subject

    private String mName;
    private String mUid;
    private String mUtid;
    private IDToken mIDToken;
    private URL mPasswordChangeUrl;
    private Date mPasswordExpiresOn;
    private String mTenantId; // Tenant Id of the authority that issued the idToken... not necessarily the home tenant of the account
    private String mGivenName;
    private String mFamilyName;
    private String mMiddleName;
    private String mEnvironment;
    private String mRawClientInfo;

    /**
     * Constructor of MicrosoftAccount.
     */
    public MicrosoftAccount() {
        super();
        Logger.verbose(TAG, "Init: " + TAG);
    }

    /**
     * Constructor of MicrosoftAccount.
     *
     * @param idToken    id token of the Microsoft account.
     * @param clientInfo the client_info for this Account.
     */
    public MicrosoftAccount(@NonNull final IDToken idToken,
                            @NonNull final ClientInfo clientInfo) {
        Logger.verbose(TAG, "Init: " + TAG);
        mIDToken = idToken;
        mRawClientInfo = clientInfo.getRawClientInfo();
        final Map<String, ?> claims = idToken.getTokenClaims();
        mUserId = getUserId(claims);
        mDisplayableId = getDisplayableIdFromClaims(claims);
        mName = (String) claims.get(AzureActiveDirectoryIdToken.NAME);
        mGivenName = (String) claims.get(AzureActiveDirectoryIdToken.GIVEN_NAME);
        mFamilyName = (String) claims.get(AzureActiveDirectoryIdToken.FAMILY_NAME);
        mMiddleName = (String) claims.get(AzureActiveDirectoryIdToken.MIDDLE_NAME);
        if (!StringUtil.isNullOrEmpty((String) claims.get(AzureActiveDirectoryIdToken.TENANT_ID))) {
            mTenantId = (String) claims.get(AzureActiveDirectoryIdToken.TENANT_ID);
        } else if (!StringUtil.isNullOrEmpty(clientInfo.getUtid())) {
            Logger.warnPII(TAG, "realm is not returned from server. Use utid as realm.");
            mTenantId = clientInfo.getUtid();
        } else {
            // According to the spec, full tenant or organizational identifier that account belongs to.
            // Can be an empty string for non-AAD scenarios.
            Logger.warnPII(TAG, "realm and utid is not returned from server. Use empty string as default tid.");
            mTenantId = "";
        }

        mUid = clientInfo.getUid();
        mUtid = clientInfo.getUtid();

        long mPasswordExpiration = 0;
        final Object expiry = claims.get(AzureActiveDirectoryIdToken.PASSWORD_EXPIRATION);

        if (null != expiry) {
            mPasswordExpiration = Long.parseLong(expiry.toString());
        }

        if (mPasswordExpiration > 0) {
            // pwd_exp returns seconds to expiration time
            // it returns in seconds. Date accepts milliseconds.
            Calendar expires = new GregorianCalendar();
            expires.add(Calendar.SECOND, (int) mPasswordExpiration);
            mPasswordExpiresOn = expires.getTime();
        }

        mPasswordChangeUrl = null;
        final String passwordChangeUrl = (String) claims.get(AzureActiveDirectoryIdToken.PASSWORD_CHANGE_URL);

        if (!StringUtil.isNullOrEmpty(passwordChangeUrl)) {
            try {
                mPasswordChangeUrl = new URL(passwordChangeUrl);
            } catch (final MalformedURLException e) {
                Logger.error(TAG, "Failed to parse passwordChangeUrl.", e);
            }
        }
    }

    protected abstract String getDisplayableIdFromClaims(final Map<String, ?> claims);

    private String getUserId(final Map<String, ?> claims) {
        final String methodName = "getUniqueId";

        String uniqueId = null;

        if (!StringUtil.isNullOrEmpty((String) claims.get(AzureActiveDirectoryIdToken.OBJECT_ID))) {
            Logger.info(TAG + ":" + methodName, "Using ObjectId as uniqueId");
            uniqueId = (String) claims.get(AzureActiveDirectoryIdToken.OBJECT_ID);
        } else if (!StringUtil.isNullOrEmpty((String) claims.get(AzureActiveDirectoryIdToken.SUBJECT))) {
            Logger.info(TAG + ":" + methodName, "Using Subject as uniqueId");
            uniqueId = (String) claims.get(AzureActiveDirectoryIdToken.SUBJECT);
        }

        return uniqueId;
    }

    /**
     * @param givenName given name of the Microsoft account.
     */
    public synchronized void setFirstName(final String givenName) {
        mGivenName = givenName;
    }

    /**
     * @param familyName family name of the Microsoft account.
     */
    public synchronized void setFamilyName(final String familyName) {
        mFamilyName = familyName;
    }

    /**
     * @return The displayable value in the UserPrincipleName(UPN) format. Can be null if not
     * returned from the service.
     */
    public synchronized String getDisplayableId() {
        return mDisplayableId;
    }

    /**
     * Sets the displayableId of a user when making acquire token API call.
     *
     * @param displayableId displayable ID.
     */
    public synchronized void setDisplayableId(final String displayableId) {
        mDisplayableId = displayableId;
    }

    /**
     * @return The unique identifier of the user.
     * <p>
     * For v2, the OID claim in the ID token.
     */
    public synchronized String getUserId() {
        return mUserId;
    }

    /**
     * Gets the uid.
     *
     * @return The uid to get.
     */
    public synchronized String getUid() {
        return mUid;
    }

    /**
     * Sets the uid.
     *
     * @param uid The uid to set.
     */
    public synchronized void setUid(final String uid) {
        mUid = uid;
    }

    /**
     * Sets the utid.
     *
     * @param uTid The utid to set.
     */
    public synchronized void setUtid(final String uTid) {
        mUtid = uTid;
    }

    /**
     * Sets the name.
     *
     * @param name The name to set.
     */
    public synchronized void setName(final String name) {
        mName = name;
    }

    /**
     * Gets the utid.
     *
     * @return The utid to get.
     */
    public synchronized String getUtid() {
        return mUtid;
    }

    synchronized void setUserId(final String userid) {
        mUserId = userid;
    }

    /**
     * Return the unique identifier for the account...
     *
     * @return unique identifier string.
     */
    @Override
    public synchronized String getUniqueIdentifier() {
        return Base64.encodeToString(mUid, EnumSet.of(Base64Flags.URL_SAFE, Base64Flags.NO_WRAP))
                + "." +
                Base64.encodeToString(mUtid, EnumSet.of(Base64Flags.URL_SAFE, Base64Flags.NO_WRAP));
    }

    @Override
    public synchronized List<String> getCacheIdentifiers() {
        List<String> cacheIdentifiers = new ArrayList<>();

        if (mDisplayableId != null) {
            cacheIdentifiers.add(mDisplayableId);
        }

        if (mUserId != null) {
            cacheIdentifiers.add(mUserId);
        }

        if (getUniqueIdentifier() != null) {
            cacheIdentifiers.add(getUniqueIdentifier());
        }

        return cacheIdentifiers;
    }

    /**
     * Gets password change url.
     *
     * @return the password change uri.
     */
    public synchronized URL getPasswordChangeUrl() {
        return mPasswordChangeUrl;
    }

    /**
     * Gets password expires on.
     *
     * @return the time when the password will expire.
     */
    @Nullable
    public synchronized Date getPasswordExpiresOn() {
        return CopyUtil.copyIfNotNull(mPasswordExpiresOn);
    }

    /**
     * @return mIDToken of the Microsoft account.
     */
    public synchronized IDToken getIDToken() {
        return mIDToken;
    }

    @Override
    public synchronized String getHomeAccountId() {
        // TODO -- This method's functionality is duplicative of
        // Account#getUniqueIdentifier except that that implementation
        // was coded for the refactored ADAL cache which expects
        // uid/utid to be base64 encoded for legacy support.
        return getUid() + "." + getUtid();
    }

    public synchronized void setEnvironment(final String environment) {
        mEnvironment = environment;
    }

    @Override
    public synchronized String getEnvironment() {
        return mEnvironment;
    }

    @Override
    public synchronized String getRealm() {
        return mTenantId;
    }

    @Override
    public synchronized String getLocalAccountId() {
        return getUserId();
    }

    @Override
    public synchronized String getUsername() {
        return getDisplayableId();
    }

    @Override
    public synchronized String getAlternativeAccountId() {
        return SchemaUtil.getAlternativeAccountId(mIDToken);
    }

    @Override
    public synchronized String getFirstName() {
        return mGivenName;
    }

    @Override
    public synchronized String getFamilyName() {
        return mFamilyName;
    }

    /**
     * @return The given name of the user. Can be null if not returned from the service.
     */
    @Override
    public synchronized String getName() {
        return mName;
    }

    @Override
    public synchronized String getMiddleName() {
        return mMiddleName;
    }

    @Override
    public synchronized String getAvatarUrl() {
        return SchemaUtil.getAvatarUrl(mIDToken);
    }

    @Override
    public synchronized String getClientInfo() {
        return mRawClientInfo;
    }

    //CHECKSTYLE:OFF
    @Override
    public synchronized String toString() {
        return "MicrosoftAccount{" +
                "mDisplayableId='" + mDisplayableId + '\'' +
                ", mUserId='" + mUserId + '\'' +
                ", mName='" + mName + '\'' +
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
    //CHECKSTYLE:ON
}
