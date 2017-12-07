package com.microsoft.identity.common.internal.providers.azureactivedirectoryb2c;

import com.microsoft.identity.common.internal.providers.IdentityProvider;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Configuration;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;

/**
 * Azure Active Directory B2C is effectively it's own OpenID Provider.  This class is responsible
 * for creating the OAuth2Strategy for working the B2C Service.
 */
public class AzureActiveDirectoryB2C extends IdentityProvider {
    @Override
    public OAuth2Strategy createOAuth2Strategy(OAuth2Configuration config) {

        return new AzureActiveDirectoryB2COAuth2Strategy(config);
    }
}
