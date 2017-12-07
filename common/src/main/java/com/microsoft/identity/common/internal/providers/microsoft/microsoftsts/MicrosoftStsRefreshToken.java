package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;

import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;

public class MicrosoftStsRefreshToken extends RefreshToken {

    public MicrosoftStsRefreshToken(MicrosoftStsTokenResponse response) {
        super(response);
    }
}
