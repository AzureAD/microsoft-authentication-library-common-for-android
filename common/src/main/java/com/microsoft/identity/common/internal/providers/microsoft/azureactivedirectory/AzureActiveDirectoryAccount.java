package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory;

import android.net.Uri;

import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.adal.internal.util.DateExtensions;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftIdToken;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

/**
 * Inherits from account and implements the getUniqueIdentifier method for returning a unique identifier for an AAD User
 * UTID, UID combined as a single identifier per current MSAL implementation
 */
public class AzureActiveDirectoryAccount extends Account {

    private String mDisplayableId; // Legacy Identifier -  UPN (preferred) or Email
    private String mUniqueId; // Legacy Identifier - Object Id (preferred) or Subject


    private String mName;
    private String mIdentityProvider;
    private String mUid;
    private String mUtid;
    private IDToken mIDToken;
    private Uri mPasswordChangeUrl;
    private Date mPasswordExpiresOn;
    private String mTenantId; // Tenant Id of the authority that issued the idToken... not necessarily the home tenant of the account

    private String mGivenName;
    private String mFamilyName;

    public AzureActiveDirectoryAccount() {
        // Default constructor.
    }

    /**
     * Private constructor for AzureActiveDirectoryAccount object
     *
     * @param idToken Returned as part of the TokenResponse
     * @param uid     Returned via clientInfo of TokenResponse
     * @param uTid    Returned via ClientInfo of Token Response
     */
    public AzureActiveDirectoryAccount(IDToken idToken, String uid, final String uTid) {
        mIDToken = idToken;
        Map<String, String> claims = idToken.getTokenClaims();
        mUniqueId = getUniqueId(claims);
        mDisplayableId = getDisplayableId(claims);
        mName = claims.get(AzureActiveDirectoryIdToken.NAME);
        mIdentityProvider = claims.get(AzureActiveDirectoryIdToken.ISSUER);
        mGivenName = claims.get(AzureActiveDirectoryIdToken.GIVEN_NAME);
        mFamilyName = claims.get(AzureActiveDirectoryIdToken.FAMILY_NAME);
        mTenantId = claims.get(AzureActiveDirectoryIdToken.TENANT_ID);
        mUid = uid;
        mUtid = uTid;

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

    /**
     * Creates an AzureActiveDirectoryAccount based on the contents of the IDToken and based on the contents of the ClientInfo JSON
     * returned as part of the TokenResponse
     *
     * @param idToken
     * @return
     */
    public static AzureActiveDirectoryAccount create(final IDToken idToken, ClientInfo clientInfo) {

        final String uid;
        final String uTid;

        //TODO: objC code throws an exception when uid/utid is null.... something for us to consider
        if (clientInfo == null) {
            uid = "";
            uTid = "";
        } else {
            uid = clientInfo.getUid();
            uTid = clientInfo.getUtid();
        }

        return new AzureActiveDirectoryAccount(idToken, uid, uTid);
    }

    protected String getDisplayableId(Map<String, String> claims) {
        if (!StringExtensions.isNullOrBlank(claims.get(AzureActiveDirectoryIdToken.UPN))) {
            return claims.get(AzureActiveDirectoryIdToken.UPN);
        } else if (!StringExtensions.isNullOrBlank(claims.get(AzureActiveDirectoryIdToken.EMAIL))) {
            return claims.get(AzureActiveDirectoryIdToken.EMAIL);
        }

        return null;
    }

    private String getUniqueId(Map<String, String> claims) {

        if (!StringExtensions.isNullOrBlank(claims.get(AzureActiveDirectoryIdToken.OJBECT_ID))) {
            return claims.get(AzureActiveDirectoryIdToken.OJBECT_ID);
        } else if (!StringExtensions.isNullOrBlank(claims.get(AzureActiveDirectoryIdToken.SUBJECT))) {
            return claims.get(AzureActiveDirectoryIdToken.SUBJECT);
        }

        return null;
    }

    public void setFirstName(String mGivenName) {
        this.mGivenName = mGivenName;
    }

    public void setLastName(String mFamilyName) {
        this.mFamilyName = mFamilyName;
    }

    /**
     * @return The displayable value in the UserPrincipleName(UPN) format. Can be null if not returned from the service.
     */
    public String getDisplayableId() {
        return mDisplayableId;
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

    // internal methods provided

    /**
     * Sets the displayableId of a user when making acquire token API call.
     *
     * @param displayableId
     */
    public void setDisplayableId(final String displayableId) {
        mDisplayableId = displayableId;
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

    void setUserId(String userid) {
        mUniqueId = userid;
    }

    /**
     * Return the unique identifier for the account...
     *
     * @return
     */
    public String getUniqueIdentifier() {
        return StringExtensions.base64UrlEncodeToString(mUid) + "." + StringExtensions.base64UrlEncodeToString(mUtid);
    }

    @Override
    public List<String> getCacheIdentifiers() {
        List<String> cacheIdentifiers = new ArrayList<String>();

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
        return getUid() + "." + getUtid();
    }

    @Override
    public String getEnvironment() {
        // TODO see AzureActiveDirectoryRefreshToken#getEnvironment
        // there's opportunity for code sharing here
        String environment = null;

        if (null != getIDToken() && null != getIDToken().getTokenClaims()) {
            environment = getIDToken().getTokenClaims().get(MicrosoftIdToken.ISSUER);
            if (!StringExtensions.isNullOrBlank(environment)) {
                try {
                    environment = new URL(environment).getHost();
                } catch (MalformedURLException e) {
                    // TODO log an error
                }
            }
        }

        return environment;
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
    public String getAuthorityType() {
        return "AAD";
    }

    @Override
    public String getGuestId() {
        String guestId = null;

        if (null != getIDToken() && null != getIDToken().getTokenClaims()) {
            guestId = getIDToken().getTokenClaims().get("altsecid");
        }

        return guestId;
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
        String avatarUrl = null;

        if (null != getIDToken() && null != getIDToken().getTokenClaims()) {
            avatarUrl = getIDToken().getTokenClaims().get(IDToken.PICTURE);
        }

        return avatarUrl;
    }
}
