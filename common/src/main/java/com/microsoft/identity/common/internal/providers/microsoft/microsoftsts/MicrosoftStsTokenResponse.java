package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

import java.util.Date;

/**
 * {@link TokenResponse} subclass for the Microsoft STS (V2).
 */
public class MicrosoftStsTokenResponse extends TokenResponse {

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

}
