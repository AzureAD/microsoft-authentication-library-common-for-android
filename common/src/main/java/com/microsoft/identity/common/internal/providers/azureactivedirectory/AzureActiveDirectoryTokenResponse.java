package com.microsoft.identity.common.internal.providers.azureactivedirectory;

import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

/**
 * {@link TokenResponse} subclass for Azure AD.
 */
public class AzureActiveDirectoryTokenResponse extends TokenResponse {

    /**
     * The time when the access token expires. The date is represented as the number of seconds
     * from 1970-01-01T0:0:0Z UTC until the expiration time. This value is used to determine the
     * lifetime of cached tokens.
     *
     * @See <a href="https://docs.microsoft.com/en-us/azure/active-directory/develop/active-directory-protocols-oauth-code">Authorize access to web applications using OAuth 2.0 and Azure Active Directory</a>
     */
    protected String mExpiresOn;

    /**
     * The App ID URI of the web API (secured resource).
     *
     * @See <a href="https://docs.microsoft.com/en-us/azure/active-directory/develop/active-directory-protocols-oauth-code">Authorize access to web applications using OAuth 2.0 and Azure Active Directory</a>
     */
    protected String mResource;

    /**
     * An unsigned JSON Web Token (JWT). The app can base64Url decode the segments of this token
     * to request information about the user who signed in. The app can cache the values and
     * display them, but it should not rely on them for any authorization or security boundaries.
     *
     * @See <a href="https://docs.microsoft.com/en-us/azure/active-directory/develop/active-directory-protocols-oauth-code">Authorize access to web applications using OAuth 2.0 and Azure Active Directory</a>
     */
    protected String mIdToken;

    /**
     * Optionally extended access_token TTL. In the event of STS outage, this field may be used to
     * extend the valid lifetime of an access_token.
     */
    protected String mExtExpiresIn;

    /**
     * The time after which the issued access_token may be used.
     */
    protected String mNotBefore;

    /**
     * Information to uniquely identify the tenant and the user _within_ that tenant.
     */
    protected String mClientInfo;

    /**
     * Gets the response expires_on.
     *
     * @return The expires_on to get.
     */
    public String getExpiresOn() {
        return mExpiresOn;
    }

    /**
     * Sets the response expires_on.
     *
     * @param expiresOn The expires_on to set.
     */
    public void setExpiresOn(String expiresOn) {
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
     * Gets the response id_token.
     *
     * @return The id_token to get.
     */
    public String getIdToken() {
        return mIdToken;
    }

    /**
     * Sets the response id_token.
     *
     * @param idToken The id_token to set.
     */
    public void setIdToken(String idToken) {
        this.mIdToken = idToken;
    }

    /**
     * Gets the response ext_expires_in.
     *
     * @return The ext_expires_in to get.
     */
    public String getExtExpiresIn() {
        return mExtExpiresIn;
    }

    /**
     * Sets the response ext_expires_in.
     *
     * @param extExpiresIn The ext_expires_in to set.
     */
    public void setExtExpiresIn(String extExpiresIn) {
        this.mExtExpiresIn = extExpiresIn;
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
}
