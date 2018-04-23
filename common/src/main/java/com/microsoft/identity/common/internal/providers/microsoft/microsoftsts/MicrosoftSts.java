package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;

import com.microsoft.identity.common.internal.providers.IdentityProvider;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Configuration;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;

public class MicrosoftSts extends IdentityProvider {

    @Override
    public OAuth2Strategy createOAuth2Strategy(OAuth2Configuration config) {
        if (config instanceof MicrosoftStsOAuth2Configuration) {
            return new MicrosoftStsOAuth2Strategy((MicrosoftStsOAuth2Configuration) config);
        }
        throw new IllegalArgumentException("Expected instance of AzureActiveDirectoryOAuth2Configuration in AzureActiveDirectory.CreateOAuth2Strategy");
    }
}
