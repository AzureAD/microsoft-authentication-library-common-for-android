package com.microsoft.identity.common.internal.providers.azureactivedirectory;

import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;
import com.microsoft.identity.common.internal.providers.oauth2.StandardIdTokenClaims;

import java.util.Map;

/**
 * Inherits from account and implements the getUniqueIdentifier method for returning a unique identifier for an AAD User
 * UTID, UID combined as a single identifier per current MSAL implementation
 */
public class AzureActiveDirectoryAccount extends Account {


    private String mDisplayableId;
    private String mName;
    private String mIdentityProvider;
    private String mUid;
    private String mUtid;
    private IDToken mIDToken;

    /**
     * Internal constructor to create {@link User} from the {@link IdToken}.
     * User will be created with both {@link IdToken} and {@link ClientInfo}.
     */
    User(IDToken idToken, String uid, final String uTid) {
        Map<String, String> claims = idToken.getTokenClaims();
        mDisplayableId = claims.get(StandardIdTokenClaims.PREFERRED_USERNAME);
        mName = claims.get(StandardIdTokenClaims.NAME);
        mIdentityProvider = claims.get(AzureActiveDirectoryIdTokenClaims.ISSUER);
        mUid = uid;
        mUtid = uTid;
    }

    /**
     * Creates an AzureActiveDirectoryAccount based on the contents of the IDToken and based on the contents of the ClientInfo JSON
     * returned as part of the TokenResponse
     * @param idToken
     * @return
     */
    public static AzureActiveDirectoryAccount create(final IDToken idToken) {

        final String uid;
        final String uTid;
        /*
        if (clientInfo == null) {
            uid = "";
            uTid = "";
        } else {
            uid = clientInfo.getUniqueIdentifier();
            uTid = clientInfo.getUniqueTenantIdentifier();
        }

        */

        uid = "";
        uTid = "";

        return new AzureActiveDirectoryAccount(idToken, uid, uTid);
    }

    /**
     * @return The displayable value in the UserPrincipleName(UPN) format. Can be null if not returned from the service.
     */
    public String getDisplayableId() {
        return mDisplayableId;
    }

    /**
     * @return The given name of the user. Can be null if not returned from the service.
     */
    public String getName() {
        return mName;
    }

    /**
     * @return The identity provider of the user authenticated. Can be null if not returned from the service.
     */
    public String getIdentityProvider() {
        return mIdentityProvider;
    }

    /**
     * @return The unique identifier of the user, which is across tenant.
     */
    public String getUserIdentifier() {
        return getUniqueIdentifier();
    }

    // internal methods provided

    /**
     * Sets the displayableId of a user when making acquire token API call.
     *
     * @param displayableId
     */
    void setDisplayableId(final String displayableId) {
        mDisplayableId = displayableId;
    }

    String getUid() {
        return mUid;
    }

    void setUid(final String uid) {
        mUid = uid;
    }

    void setUtid(final String uTid) {
        mUtid = uTid;
    }

    String getUtid() {
        return mUtid;
    }

    /**
     * Return the unique identifier for the account...
     * @return
     */
    public String getUniqueIdentifier(){
        return StringExtensions.base64UrlEncodeToString(mUid) + "." + StringExtensions.base64UrlEncodeToString(mUtid);
    }
}
