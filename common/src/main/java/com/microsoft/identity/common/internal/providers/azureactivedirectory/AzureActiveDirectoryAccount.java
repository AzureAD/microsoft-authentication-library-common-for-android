package com.microsoft.identity.common.internal.providers.azureactivedirectory;

import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;
import com.microsoft.identity.common.internal.providers.oauth2.StandardIdTokenClaims;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Inherits from account and implements the getUniqueIdentifier method for returning a unique identifier for an AAD User
 * UTID, UID combined as a single identifier per current MSAL implementation
 */
public class AzureActiveDirectoryAccount extends Account {


    private String mDisplayableId; // Legacy Identifier -  UPN (preferred) or Email
    private String mUniqueId; // Legacy Identifier - Object Id (preferred) or Subject
    private String mName;
    private String mIdentityProvider;
    private String mUid;
    private String mUtid;
    private IDToken mIDToken;

    /**
     * Private constructor for AzureActiveDirectoryAccount object
     * @param idToken Returned as part of the TokenResponse
     * @param uid Returned via clientInfo of TokenResponse
     * @param uTid Returned via ClientInfo of Token Response
     */
    AzureActiveDirectoryAccount(IDToken idToken, String uid, final String uTid) {
        Map<String, String> claims = idToken.getTokenClaims();
        mUniqueId = getUniqueId(claims);
        mDisplayableId = getDisplayableId(claims);
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
    public static AzureActiveDirectoryAccount create(final IDToken idToken, ClientInfo clientInfo) {

        final String uid;
        final String uTid;

        if (clientInfo == null) {
            uid = "";
            uTid = "";
        } else {
            uid = clientInfo.getUid();
            uTid = clientInfo.getUtid();
        }

        return new AzureActiveDirectoryAccount(idToken, uid, uTid);
    }

    private String getDisplayableId(Map<String, String> claims){

        if (!StringExtensions.isNullOrBlank(claims.get(AzureActiveDirectoryIdTokenClaims.UPN))) {
            return claims.get(AzureActiveDirectoryIdTokenClaims.UPN);
        } else if (!StringExtensions.isNullOrBlank(claims.get(StandardIdTokenClaims.EMAIL))) {
            return claims.get(StandardIdTokenClaims.EMAIL);
        }

        return null;
    }

    private String getUniqueId(Map<String, String> claims){

        if (!StringExtensions.isNullOrBlank(claims.get(AzureActiveDirectoryIdTokenClaims.OJBECT_ID))) {
            return claims.get(AzureActiveDirectoryIdTokenClaims.OJBECT_ID);
        } else if (!StringExtensions.isNullOrBlank(claims.get(StandardIdTokenClaims.SUBJECT))) {
            return claims.get(StandardIdTokenClaims.SUBJECT);
        }

        return null;
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
    public String getUserId() {
        return mUniqueId;
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

    void setUserId(String userid) {
        mUniqueId = userid;
    }

    /**
     * Return the unique identifier for the account...
     * @return
     */
    public String getUniqueIdentifier(){
        return StringExtensions.base64UrlEncodeToString(mUid) + "." + StringExtensions.base64UrlEncodeToString(mUtid);
    }

    @Override
    public List<String> getCacheIdentifiers() {
        List<String> cacheIdentifiers = new ArrayList<String>();

        if(mDisplayableId != null)
            cacheIdentifiers.add(mDisplayableId);

        if(mUniqueId != null)
            cacheIdentifiers.add(mUniqueId);

        if(getUniqueIdentifier() !=null)
            cacheIdentifiers.add(getUniqueIdentifier());

        return cacheIdentifiers;
    }
}
