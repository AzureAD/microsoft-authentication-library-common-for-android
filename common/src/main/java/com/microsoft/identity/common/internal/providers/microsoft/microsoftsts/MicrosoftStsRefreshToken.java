package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;

import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;

public class MicrosoftStsRefreshToken extends RefreshToken {

    public MicrosoftStsRefreshToken(MicrosoftStsTokenResponse response) {
        super(response);
    }

    @Override
    public String getUniqueUserId() {
        // TODO
        return null;
    }

    @Override
    public String getEnvironment() {
        // TODO
        return null;
    }

    @Override
    public String getClientId() {
        // TODO
        return null;
    }

    @Override
    public String getSecret() {
        // TODO
        return null;
    }

    @Override
    public String getTarget() {
        // TODO
        return null;
    }

    @Override
    public String getExpiresOn() {
        // TODO
        return null;
    }

    @Override
    public String getFamilyId() {
        // TODO
        return null;
    }
}
