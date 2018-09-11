package com.microsoft.identity.common.internal.providers.li;

import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.internal.net.HttpResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AccessToken;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResultFactory;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Configuration;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;

public class LiOAuth2Strategy extends OAuth2Strategy {
    /**
     * Constructor of OAuth2Strategy.
     *
     * @param config generic OAuth2 configuration
     */
    public LiOAuth2Strategy(OAuth2Configuration config) {
        super(config);
    }

    @Override
    public AuthorizationResultFactory getAuthorizationResultFactory() {
        return new LiAuthorizationResultFactory();
    }

    @Override
    public String getIssuerCacheIdentifier(AuthorizationRequest request) {
        return null;
    }

    @Override
    public AccessToken getAccessTokenFromResponse(TokenResponse response) {
        return null;
    }

    @Override
    public RefreshToken getRefreshTokenFromResponse(TokenResponse response) {
        return null;
    }

    @Override
    public Account createAccount(TokenResponse response) {
        return null;
    }

    @Override
    public AuthorizationRequest.Builder createAuthorizationRequestBuilder() {
        return new LiAuthorizationRequest.Builder();
    }

    @Override
    public TokenRequest createTokenRequest(AuthorizationRequest request, AuthorizationResponse response) {
        return null;
    }

    @Override
    public TokenRequest createRefreshTokenRequest(com.microsoft.identity.common.internal.dto.RefreshToken refreshToken) {
        return null;
    }

    @Override
    protected void validateAuthorizationRequest(AuthorizationRequest request) {

    }

    @Override
    protected void validateTokenRequest(TokenRequest request) {

    }

    @Override
    protected TokenResult getTokenResultFromHttpResponse(HttpResponse response) {
        return null;
    }

    @Override
    public boolean supportsOIDC() {
        return false;
    }
}
