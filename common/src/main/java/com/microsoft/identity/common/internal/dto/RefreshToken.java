package com.microsoft.identity.common.internal.dto;

import com.google.gson.annotations.SerializedName;

public class RefreshToken extends Credential {

    /**
     * Full base64 encoded client info received from ESTS, if available. STS returns the clientInfo 
     * on both v1 and v2 for AAD. This field is used for extensibility purposes.
     */
    @SerializedName("client_info")
    private String mClientInfo;

    /**
     * 1st Party Application Family ID.
     */
    @SerializedName("family_id")
    private String mFamilyId;

    /**
     * Permissions that are included in the token. Formats for endpoints will be different. 
     * <p>
     * Mandatory, if credential is scoped down by some parameters or requirements (e.g. by
     * resource, scopes or permissions).
     */
    @SerializedName("target")
    private String mTarget;

    /**
     * The primary username that represents the user (corresponds to the preferred_username claim
     * in the v2.0 endpoint). It could be an email address, phone number, or a generic username
     * without a specified format. Its value is mutable and might change over time.
     */
    @SerializedName("username")
    private String mUsername;

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
     * Gets the family_id.
     *
     * @return The family_id to get.
     */
    public String getFamilyId() {
        return mFamilyId;
    }

    /**
     * Sets the family_id.
     *
     * @param familyId The family_id to set.
     */
    public void setFamilyId(String familyId) {
        this.mFamilyId = familyId;
    }
}
