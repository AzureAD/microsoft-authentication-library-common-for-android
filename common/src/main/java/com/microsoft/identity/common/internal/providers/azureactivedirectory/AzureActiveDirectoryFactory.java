package com.microsoft.identity.common.internal.providers.azureactivedirectory;

import com.microsoft.identity.common.internal.providers.IdentityProviderFactory;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;

/**
 * Implements the IdentityProviderFactory base class...
 */
public class AzureActiveDirectoryFactory extends IdentityProviderFactory {

    public OAuth2Strategy createOAuth2Strategy(){
        return new AzureActiveDirectoryOAuth2Strategy();
    }

}
