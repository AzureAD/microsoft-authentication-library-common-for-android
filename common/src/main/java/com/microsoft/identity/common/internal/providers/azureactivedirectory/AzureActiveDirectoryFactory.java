package com.microsoft.identity.common.internal.providers.azureactivedirectory;

import com.microsoft.identity.common.internal.providers.IdentityProviderFactory;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;

public class AzureActiveDirectoryFactory extends IdentityProviderFactory {

    public OAuth2Strategy CreateOAuth2Strategy(){
        return new AzureActiveDirectoryOAuth2Strategy();
    }

}
