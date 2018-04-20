package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory;

import android.util.Base64;

import com.microsoft.identity.common.adal.internal.util.JsonExtensions;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.exception.ServiceException;

import org.json.JSONException;

import java.nio.charset.Charset;
import java.util.Map;

/**
 * Object representation of client_info returned by AAD's Token Endpoint.
 */
public class ClientInfo {

    public static final String UNIQUE_IDENTIFIER = "uid";
    public static final String UNIQUE_TENANT_IDENTIFIER = "utid";

    /**
     * Constructor for ClientInfo object
     *
     * @param rawClientInfo
     */
    public ClientInfo(String rawClientInfo) throws ServiceException {

        /*
        NOTE: Server team would like us to emit telemetry when client Info is null...
         */
        if (StringExtensions.isNullOrBlank(rawClientInfo)) {
            mUid = "";
            mUtid = "";
            return;
        }

        // decode the client info first
        final String decodedClientInfo = new String(Base64.decode(rawClientInfo, Base64.URL_SAFE), Charset.forName(StringExtensions.ENCODING_UTF8));
        final Map<String, String> clientInfoItems;
        try {
            clientInfoItems = JsonExtensions.extractJsonObjectIntoMap(decodedClientInfo);
        } catch (final JSONException e) {
            throw new ServiceException("", ErrorStrings.INVALID_JWT, e);
        }

        mUid = clientInfoItems.get(ClientInfo.UNIQUE_IDENTIFIER);
        mUtid = clientInfoItems.get(ClientInfo.UNIQUE_TENANT_IDENTIFIER);

    }

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
