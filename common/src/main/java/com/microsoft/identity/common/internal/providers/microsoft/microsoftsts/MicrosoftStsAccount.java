package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.ClientInfo;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;

import java.util.Map;

public class MicrosoftStsAccount extends MicrosoftAccount {

    public MicrosoftStsAccount(IDToken idToken, String uid, final String uTid) {
        super(idToken, uid, uTid);
    }

    /**
     * Creates an MicrosoftStsAccount based on the contents of the IDToken and based on the contents of the ClientInfo JSON
     * returned as part of the TokenResponse
     *
     * @param idToken    The IDToken for this Account.
     * @param clientInfo The ClientInfo for this Account.
     * @return The newly created MicrosoftStsAccount.
     */
    public static MicrosoftStsAccount create(final IDToken idToken, ClientInfo clientInfo) {

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

        return new MicrosoftStsAccount(idToken, uid, uTid);
    }

    @Override
    public String getAuthorityType() {
        return "MSSTS";
    }

    @Override
    protected String getDisplayableId(Map<String, String> claims) {
        if (!StringExtensions.isNullOrBlank(claims.get(MicrosoftStsIdToken.PREFERRED_USERNAME))) {
            return claims.get(MicrosoftStsIdToken.PREFERRED_USERNAME);
        } else if (!StringExtensions.isNullOrBlank(claims.get(MicrosoftStsIdToken.EMAIL))) {
            return claims.get(MicrosoftStsIdToken.EMAIL);
        }

        return null;
    }

}
