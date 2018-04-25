package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

import java.util.Date;

/**
 * {@link TokenResponse} subclass for the Microsoft STS (V2).
 */
public class MicrosoftStsTokenResponse extends TokenResponse {

    /**
     * The time when the access token expires. The date is represented as the number of seconds
     * from 1970-01-01T0:0:0Z UTC until the expiration time. This value is used to determine the
     * lifetime of cached tokens.
     *
     * @See <a href="https://docs.microsoft.com/en-us/azure/active-directory/develop/active-directory-protocols-oauth-code">Authorize access to web applications using OAuth 2.0 and Azure Active Directory</a>
     */
    protected Date mExpiresOn;

    /**
     * Optionally extended access_token TTL. In the event of STS outage, this field may be used to
     * extend the valid lifetime of an access_token.
     */
    protected Date mExtExpiresOn;

    /**
     * Optionally extended access_token TTL. In the event of STS outage, this field may be used to
     * extend the valid lifetime of an access_token.
     */
    @SerializedName("ext_expires_in")
    protected Date mExtExpiresIn;

    /**
     * Information to uniquely identify the tenant and the user _within_ that tenant.
     */
    @SerializedName("client_info")
    protected String mClientInfo;

    /**
     * Information to uniquely identify the family that the client application belongs to.
     */
    protected String mFamilyId;

    /**
     * The client_id of the application requesting a token.
     */
    protected transient String mClientId;

    /**
     * Gets the ext_expires_in.
     *
     * @return The ext_expires_in to get.
     */
    public Date getExtExpiresIn() {
        return mExtExpiresIn;
    }

    /**
     * Sets the ext_expires_in.
     *
     * @param extExpiresin The ext_expires_in to set.
     */
    public void setExtExpiresIn(Date extExpiresin) {
        mExtExpiresIn = extExpiresin;
    }

    /**
     * Gets the response client_info.
     *
     * @return The client_info to get.
     */
    public String getClientInfo() {
        return mClientInfo;
    }

    /**
     * Sets the response client_info.
     *
     * @param clientInfo The client_info to set.
     */
    public void setClientInfo(String clientInfo) {
        mClientInfo = clientInfo;
    }

    /**
     * Gets the response expires on.
     *
     * @return The expires on to get.
     */
    public Date getExpiresOn() {
        return mExpiresOn;
    }

    /**
     * Sets the response expires on.
     *
     * @param expiresOn The expires on to set.
     */
    public void setExpiresOn(Date expiresOn) {
        this.mExpiresOn = expiresOn;
    }

    /**
     * Gets the response ext expires on.
     *
     * @return The ext expires on to get.
     */
    public Date getExtExpiresOn() {
        return mExtExpiresOn;
    }

    /**
     * Sets the response ext expires on.
     *
     * @param extExpiresOn The expires on to set.
     */
    public void setExtExpiresOn(Date extExpiresOn) {
        this.mExtExpiresOn = extExpiresOn;
    }

    /**
     * Returns the family client id
     *
     * @return
     */
    public String getFamilyId() {
        return mFamilyId;
    }

    /**
     * Sets the family id
     *
     * @param familyId
     */
    public void setFamilyId(String familyId) {
        this.mFamilyId = familyId;
    }

    /**
     * Gets the client_id.
     *
     * @return The client_id to get.
     */
    public String getClientId() {
        return mClientId;
    }

    /**
     * Sets the client_id.
     *
     * @param clientId The client_id to set.
     */
    public void setClientId(final String clientId) {
        mClientId = clientId;
    }
}
