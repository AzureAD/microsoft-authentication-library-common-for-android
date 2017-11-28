package com.microsoft.identity.common.internal.cache;

import android.net.Uri;

import com.microsoft.identity.common.adal.internal.util.DateExtensions;
import com.microsoft.identity.common.internal.providers.azureactivedirectory.AzureActiveDirectoryAccount;

import java.util.Date;


public class ADALUserInfo {
    private String mUniqueId;

    private String mDisplayableId;

    private String mGivenName;

    private String mFamilyName;

    private String mIdentityProvider;

    private transient Uri mPasswordChangeUrl;

    private transient Date mPasswordExpiresOn;


    public ADALUserInfo(AzureActiveDirectoryAccount account){
        this.mUniqueId = account.getUserId();
        this.mDisplayableId = account.getDisplayableId();
        this.mGivenName = account.getGivenName();
        this.mFamilyName = account.getFamilyName();
        this.mIdentityProvider = account.getIdentityProvider();
        this.mPasswordChangeUrl = account.getPasswordChangeUrl();
        this.mPasswordExpiresOn = account.getPasswordExpiresOn();
    }


    /**
     * Gets unique user id.
     *
     * @return the unique id representing an user
     */
    public String getUserId() {
        return mUniqueId;
    }

    /**
     * @param userid The unique user id to set.
     */
    void setUserId(String userid) {
        mUniqueId = userid;
    }

    /**
     * Gets given name.
     *
     * @return the given name of the user
     */
    public String getGivenName() {
        return mGivenName;
    }

    /**
     * Gets family name.
     *
     * @return the family name of the user
     */
    public String getFamilyName() {
        return mFamilyName;
    }

    /**
     * Gets Identity provider.
     *
     * @return the identity provider
     */
    public String getIdentityProvider() {
        return mIdentityProvider;
    }

    /**
     * Gets displayable user name.
     *
     * @return the displayable user name
     */
    public String getDisplayableId() {
        return mDisplayableId;
    }

    /**
     * @param displayableId The displayable Id to set.
     */
    void setDisplayableId(String displayableId) {
        mDisplayableId = displayableId;
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
}
