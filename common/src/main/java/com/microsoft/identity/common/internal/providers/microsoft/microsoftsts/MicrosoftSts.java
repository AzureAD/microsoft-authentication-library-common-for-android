package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;

import com.microsoft.identity.common.internal.providers.IdentityProvider;

public class MicrosoftSts
        extends IdentityProvider<MicrosoftStsOAuth2Strategy, MicrosoftStsOAuth2Configuration> {

    @Override
    public MicrosoftStsOAuth2Strategy createOAuth2Strategy(MicrosoftStsOAuth2Configuration config) {
        return new MicrosoftStsOAuth2Strategy(config);
    }

}
