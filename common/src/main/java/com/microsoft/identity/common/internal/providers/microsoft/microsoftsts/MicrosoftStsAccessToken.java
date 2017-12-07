package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;


import com.microsoft.identity.common.internal.providers.oauth2.AccessToken;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

import java.util.Date;

public class MicrosoftStsAccessToken extends AccessToken {

    protected Date mExtExpiresIn;

    public MicrosoftStsAccessToken(TokenResponse response) {
        super(response);

        if (response instanceof MicrosoftStsTokenResponse) {
            final MicrosoftStsTokenResponse tokenResponse = (MicrosoftStsTokenResponse) response;
            mExtExpiresIn = tokenResponse.getExtExpiresIn();
        }
    }
}
