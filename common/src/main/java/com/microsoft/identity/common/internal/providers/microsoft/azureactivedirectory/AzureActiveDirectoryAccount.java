package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;

import java.util.Map;

/**
 * Inherits from account and implements the getUniqueIdentifier method for returning a unique identifier for an AAD User
 * UTID, UID combined as a single identifier per current MSAL implementation
 */
public class AzureActiveDirectoryAccount extends MicrosoftAccount {

    public AzureActiveDirectoryAccount() {
        super();
    }

    /**
     * Constructor for AzureActiveDirectoryAccount object
     *
     * @param idToken Returned as part of the TokenResponse
     * @param uid     Returned via clientInfo of TokenResponse
     * @param uTid    Returned via ClientInfo of Token Response
     */
    public AzureActiveDirectoryAccount(IDToken idToken, String uid, final String uTid) {
        super(idToken, uid, uTid);
    }

    /**
     * Creates an AzureActiveDirectoryAccount based on the contents of the IDToken and based on the contents of the ClientInfo JSON
     * returned as part of the TokenResponse
     *
     * @param idToken
     * @return
     */
    public static AzureActiveDirectoryAccount create(final IDToken idToken, ClientInfo clientInfo) {

        final String uid;
        final String uTid;

        //TODO: objC code throws an exception when uid/utid is null.... something for us to consider
        if (clientInfo == null) {
            uid = "";
            uTid = "";
        } else {
            uid = clientInfo.getUid();
            uTid = clientInfo.getUtid();
        }

        return new AzureActiveDirectoryAccount(idToken, uid, uTid);
    }

    @Override
    public String getAuthorityType() {
        return "AAD";
    }

    @Override
    protected String getDisplayableId(Map<String, String> claims) {
        if (!StringExtensions.isNullOrBlank(claims.get(AzureActiveDirectoryIdToken.UPN))) {
            return claims.get(AzureActiveDirectoryIdToken.UPN);
        } else if (!StringExtensions.isNullOrBlank(claims.get(AzureActiveDirectoryIdToken.EMAIL))) {
            return claims.get(AzureActiveDirectoryIdToken.EMAIL);
        }

        return null;
    }
}
