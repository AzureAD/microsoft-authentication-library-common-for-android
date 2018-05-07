package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;


import com.microsoft.identity.common.internal.providers.oauth2.AccessToken;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

public class MicrosoftStsAccessToken extends AccessToken {

    protected Long mExtExpiresIn;

    public MicrosoftStsAccessToken(TokenResponse response) {
        super(response);

        if (response instanceof MicrosoftStsTokenResponse) {
            final MicrosoftStsTokenResponse tokenResponse = (MicrosoftStsTokenResponse) response;
            mExtExpiresIn = tokenResponse.getExtExpiresIn();
        }
    }
}
