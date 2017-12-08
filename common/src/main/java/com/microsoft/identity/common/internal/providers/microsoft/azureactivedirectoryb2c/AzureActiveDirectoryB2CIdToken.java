package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectoryb2c;

import com.microsoft.identity.common.internal.providers.oauth2.IDToken;

import java.util.Map;

/**
 * Azure Active Directory B2C Id Token
 * B2C supports customizing the claims contained in tokens
 * see <a href='https://docs.microsoft.com/en-us/azure/active-directory-b2c/active-directory-b2c-reference-tokens'>https://docs.microsoft.com/en-us/azure/active-directory-b2c/active-directory-b2c-reference-tokens</a>
 */
public class AzureActiveDirectoryB2CIdToken extends IDToken {
    public AzureActiveDirectoryB2CIdToken(String rawIdToken) {
        super(rawIdToken);
    }

    @Override
    public Map<String, String> getTokenClaims() {
        return super.getTokenClaims();
    }
}
