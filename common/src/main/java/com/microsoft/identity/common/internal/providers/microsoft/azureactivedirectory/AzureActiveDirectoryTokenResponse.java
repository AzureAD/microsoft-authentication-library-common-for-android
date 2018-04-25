package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory;

import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftTokenResponse;

import java.util.Date;

/**
 * {@link TokenResponse} subclass for Azure AD.
 */
public class AzureActiveDirectoryTokenResponse extends MicrosoftTokenResponse {

    /**
     * The time when the access token expires. The date is represented as the number of seconds
     * from 1970-01-01T0:0:0Z UTC until the expiration time. This value is used to determine the
     * lifetime of cached tokens.
     *
     * @See <a href="https://docs.microsoft.com/en-us/azure/active-directory/develop/active-directory-protocols-oauth-code">Authorize access to web applications using OAuth 2.0 and Azure Active Directory</a>
     */
    protected Date mExpiresOn;

    /**
     * The App ID URI of the web API (secured resource).
     *
     * @See <a href="https://docs.microsoft.com/en-us/azure/active-directory/develop/active-directory-protocols-oauth-code">Authorize access to web applications using OAuth 2.0 and Azure Active Directory</a>
     */
    protected String mResource;

    /**
     * Optionally extended access_token TTL. In the event of STS outage, this field may be used to
     * extend the valid lifetime of an access_token.
     */
    protected Date mExtExpiresOn;

    /**
     * The time after which the issued access_token may be used.
     */
    protected String mNotBefore;

    /**
     * Information to uniquely identify the tenant and the user _within_ that tenant.
     */
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
     * The SPE Ring from which this token was issued.
     */
    protected String mSpeRing;

    /**
     * Gets the response expires_on.
     *
     * @return The expires_on to get.
     */
    public Date getExpiresOn() {
        return mExpiresOn;
    }

    /**
     * Sets the response expires_on.
     *
     * @param expiresOn The expires_on to set.
     */
    public void setExpiresOn(Date expiresOn) {
        this.mExpiresOn = expiresOn;
    }

    /**
     * Gets the response resource.
     *
     * @return The resource to get.
     */
    public String getResource() {
        return mResource;
    }

    /**
     * Sets the response resource.
     *
     * @param resource The resource to set.
     */
    public void setResource(String resource) {
        this.mResource = resource;
    }

    /**
     * Gets the response ext_expires_in.
     *
     * @return The ext_expires_in to get.
     */
    public Date getExtExpiresOn() {
        return mExtExpiresOn;
    }

    /**
     * Sets the response ext_expires_in.
     *
     * @param extExpiresOn The ext_expires_in to set.
     */
    public void setExtExpiresOn(Date extExpiresOn) {
        this.mExtExpiresOn = extExpiresOn;
    }

    /**
     * Gets the response not_before.
     *
     * @return The not_before to get.
     */
    public String getNotBefore() {
        return mNotBefore;
    }

    /**
     * Set the response not_before.
     *
     * @param notBefore The not_before to set.
     */
    public void setNotBefore(String notBefore) {
        this.mNotBefore = notBefore;
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
        this.mClientInfo = clientInfo;
    }

    /**
     * Gets the response spe ring (x-ms-clitelem).
     *
     * @return The spe ring.
     */
    public String getSpeRing() {
        return mSpeRing;
    }

    /**
     * Sets the response spe ring (x-ms-clitelem).
     *
     * @param speRing The spe ring to set.
     */
    public void setSpeRing(String speRing) {
        this.mSpeRing = speRing;
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
