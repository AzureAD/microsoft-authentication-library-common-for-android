package com.microsoft.identity.common.internal.authorities;

import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.ropc.ResourceOwnerPasswordCredentialsTestStrategy;

public class B2CTestAuthority extends AzureActiveDirectoryB2CAuthority {

    private static final String TAG = B2CTestAuthority.class.getName();

    public B2CTestAuthority(String authorityUrl) {
        super(authorityUrl);
    }

    @Override
    public OAuth2Strategy createOAuth2Strategy() {
        final String methodName = ":createOAuth2Strategy";
        Logger.verbose(
                TAG + methodName,
                "Creating OAuth2Strategy "
        );
        MicrosoftStsOAuth2Configuration config = new MicrosoftStsOAuth2Configuration();
        config.setMultipleCloudsSupported(false);
        config.setAuthorityUrl(this.getAuthorityURL());
        return new ResourceOwnerPasswordCredentialsTestStrategy(config);
    }
}
