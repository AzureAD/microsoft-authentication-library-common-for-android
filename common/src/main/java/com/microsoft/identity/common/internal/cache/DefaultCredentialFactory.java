package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.internal.dto.AccessToken;
import com.microsoft.identity.common.internal.dto.RefreshToken;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

class DefaultCredentialFactory implements ICredentialFactory {

    @Override
    public AccessToken createAccessToken(OAuth2Strategy strategy, AuthorizationRequest request, TokenResponse response) {
        final AccessToken accessToken = new AccessToken();
        // TODO initialize
        return accessToken;
    }

    @Override
    public RefreshToken createRefreshToken(OAuth2Strategy strategy, AuthorizationRequest request, TokenResponse response) {
        final RefreshToken refreshToken = new RefreshToken();
        // TODO intialize
        return refreshToken;
    }
}
