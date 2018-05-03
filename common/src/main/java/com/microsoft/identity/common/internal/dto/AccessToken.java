package com.microsoft.identity.common.internal.dto;

import com.google.gson.annotations.SerializedName;

import static com.microsoft.identity.common.internal.dto.AccessToken.SerializedNames.ACCESS_TOKEN_TYPE;
import static com.microsoft.identity.common.internal.dto.AccessToken.SerializedNames.AUTHORITY;
import static com.microsoft.identity.common.internal.dto.AccessToken.SerializedNames.CLIENT_INFO;
import static com.microsoft.identity.common.internal.dto.AccessToken.SerializedNames.EXTENDED_EXPIRES_ON;
import static com.microsoft.identity.common.internal.dto.AccessToken.SerializedNames.REALM;
import static com.microsoft.identity.common.internal.dto.AccessToken.SerializedNames.TARGET;

public class AccessToken extends Credential {

    public static class SerializedNames extends Credential.SerializedNames {
        public static final String ACCESS_TOKEN_TYPE = "access_token_type";
        public static final String AUTHORITY = "authority";
        public static final String CLIENT_INFO = "client_info";
        public static final String EXTENDED_EXPIRES_ON = "extended_expires_on";
        public static final String REALM = "realm";
        public static final String TARGET = "target";
    }

    /**
     * The access token type provides the client with the information required to successfully
     * utilize the access token to make a protected resource request (along with type-specific
     * attributes).
     */
    @SerializedName(ACCESS_TOKEN_TYPE)
    private String mAccessTokenType;

    /**
     * Full authority URL of the issuer.
     */
    @SerializedName(AUTHORITY)
    private String mAuthority;

    /**
     * Full base64 encoded client info received from ESTS, if available. STS returns the clientInfo 
     * on both v1 and v2 for AAD. This field is used for extensibility purposes.
     */
    @SerializedName(CLIENT_INFO)
    private String mClientInfo;

    /**
     * Additional extended expiry time until when token is valid in case of server-side outage.
     */
    @SerializedName(EXTENDED_EXPIRES_ON)
    private String mExtendedExpiresOn;

    /**
     * Full tenant or organizational identifier that account belongs to. Can be null.
     */
    @SerializedName(REALM)
    private String mRealm;

    /**
     * Permissions that are included in the token. Formats for endpoints will be different. 
     * <p>
     * Mandatory, if credential is scoped down by some parameters or requirements (e.g. by
     * resource, scopes or permissions).
     */
    @SerializedName(TARGET)
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

    /**
     * Gets the access_token_type.
     *
     * @return The access_token_type to get.
     */
    public String getAccessTokenType() {
        return mAccessTokenType;
    }

    /**
     * Sets the access_token_type.
     *
     * @param accessTokenType The access_token_type to set.
     */
    public void setAccessTokenType(String accessTokenType) {
        this.mAccessTokenType = accessTokenType;
    }

    /**
     * Gets the authority.
     *
     * @return The authority to get.
     */
    public String getAuthority() {
        return mAuthority;
    }

    /**
     * Sets the authority.
     *
     * @param authority The authority to set.
     */
    public void setAuthority(String authority) {
        this.mAuthority = authority;
    }

    /**
     * Gets the extended_expires_on.
     *
     * @return The extended_expires_on to get.
     */
    public String getExtendedExpiresOn() {
        return mExtendedExpiresOn;
    }

    /**
     * Sets the extended_expires_on.
     *
     * @param extendedExpiresOn The extended_expires_on to set.
     */
    public void setExtendedExpiresOn(String extendedExpiresOn) {
        this.mExtendedExpiresOn = extendedExpiresOn;
    }
}
