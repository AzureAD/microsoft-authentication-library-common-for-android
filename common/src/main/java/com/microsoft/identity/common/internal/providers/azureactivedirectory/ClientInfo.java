package com.microsoft.identity.common.internal.providers.azureactivedirectory;

/**
 * Object representation of client_info returned by AAD's Token Endpoint.
 */
public class ClientInfo {

    /**
     * Unique identifier for a user in the current tenant.
     */
    protected String mUid;

    /**
     * Unique identifier for a tenant.
     */
    protected String mUtid;

    /**
     * Gets the user unique id.
     *
     * @return The user unique id to get.
     */
    public String getUid() {
        return mUid;
    }

    /**
     * Sets the user unique id.
     *
     * @param uid The user unique id to set.
     */
    public void setUid(String uid) {
        this.mUid = uid;
    }

    /**
     * Gets the tenant unique id.
     *
     * @return The tenant unique id to get.
     */
    public String getUtid() {
        return mUtid;
    }

    /**
     * Sets the tenant unique id.
     *
     * @param utid The tenant unique id to set.
     */
    public void setUtid(String utid) {
        this.mUtid = utid;
    }
}
