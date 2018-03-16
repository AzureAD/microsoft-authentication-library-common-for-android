package com.microsoft.identity.common.internal.dto;

import com.google.gson.annotations.SerializedName;

public class AccessToken extends Credential {

    /**
     * The access token type provides the client with the information required to successfully
     * utilize the access token to make a protected resource request (along with type-specific
     * attributes).
     */
    @SerializedName("access_token_type")
    private String mAccessTokenType;

    /**
     * Full authority URL of the issuer.
     */
    @SerializedName("authority")
    private String mAuthority;

    /**
     * Absolute device time when entry was created in cache in milliseconds from epoch (1970).
     */
    @SerializedName("cached_at")
    private String mCachedAt;

    /**
     * Full base64 encoded client info received from ESTS, if available. STS returns the clientInfo 
     * on both v1 and v2 for AAD. This field is used for extensibility purposes.
     */
    @SerializedName("client_info")
    private String mClientInfo;

    /**
     * Token expiry time. This value should be calculated based on the current UTC time measured
     * locally and the value expires_in returned from the service. Measured in milliseconds from
     * epoch (1970).
     */
    @SerializedName("expires_on")
    private String mExpiresOn;

    /**
     * Additional extended expiry time until when token is valid in case of server-side outage.
     */
    @SerializedName("extended_expires_on")
    private String mExtendedExpiresOn;

    /**
     * Full tenant or organizational identifier that account belongs to. Can be null.
     */
    @SerializedName("realm")
    private String mRealm;

    /**
     * Permissions that are included in the token. Formats for endpoints will be different. 
     * <p>
     * Mandatory, if credential is scoped down by some parameters or requirements (e.g. by
     * resource, scopes or permissions).
     */
    @SerializedName("target")
    private String mTarget;

    /**
     * Gets the realm.
     *
     * @return The realm to get.
     */
    public String getRealm() {
        return mRealm;
    }

    /**
     * Sets the realm.
     *
     * @param realm The realm to set.
     */
    public void setRealm(final String realm) {
        mRealm = realm;
    }

    /**
     * Gets the target.
     *
     * @return The target to get.
     */
    public String getTarget() {
        return mTarget;
    }

    /**
     * Sets the target.
     *
     * @param target The target to set.
     */
    public void setTarget(final String target) {
        mTarget = target;
    }

    /**
     * Gets the cached_at.
     *
     * @return The cached_at to get.
     */
    public String getCachedAt() {
        return mCachedAt;
    }

    /**
     * Sets the cached_at.
     *
     * @param cachedAt The cached_at to set.
     */
    public void setCachedAt(final String cachedAt) {
        mCachedAt = cachedAt;
    }

    /**
     * Gets the expires_on.
     *
     * @return The expires_on to get.
     */
    public String getExpiresOn() {
        return mExpiresOn;
    }

    /**
     * Sets the expires_on.
     *
     * @param expiresOn The expires_on to set.
     */
    public void setExpiresOn(final String expiresOn) {
        mExpiresOn = expiresOn;
    }

    /**
     * Gets the client_info.
     *
     * @return The client_info to get.
     */
    public String getClientInfo() {
        return mClientInfo;
    }

    /**
     * Sets the client_info.
     *
     * @param clientInfo The clent_info to set.
     */
    public void setClientInfo(final String clientInfo) {
        mClientInfo = clientInfo;
    }

}
