package com.microsoft.identity.common.internal.providers.activedirectoryfederationservices;

import com.microsoft.identity.common.internal.providers.oauth2.IDToken;

import java.util.Map;

/**
 * ID Tokens only became available with ADFS 2016
 * ADFS 2016 supports custom claims in id tokens
 * see <a href='https://docs.microsoft.com/en-us/windows-server/identity/ad-fs/development/custom-id-tokens-in-ad-fs'>https://docs.microsoft.com/en-us/windows-server/identity/ad-fs/development/custom-id-tokens-in-ad-fs</a>
 *
 *  */
public class ActiveDirectoryFederationServicesIdToken extends IDToken {
    public ActiveDirectoryFederationServicesIdToken(String rawIdToken) {
        super(rawIdToken);
    }

    @Override
    public Map<String, String> getTokenClaims() {
        return super.getTokenClaims();
    }
}
