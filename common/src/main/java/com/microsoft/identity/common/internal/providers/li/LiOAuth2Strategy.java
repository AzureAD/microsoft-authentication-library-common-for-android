package com.microsoft.identity.common.internal.providers.li;

import com.microsoft.identity.common.BaseAccount;
import com.microsoft.identity.common.internal.dto.IAccountRecord;
import com.microsoft.identity.common.internal.dto.RefreshTokenRecord;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.net.HttpResponse;
import com.microsoft.identity.common.internal.net.ObjectMapper;
import com.microsoft.identity.common.internal.providers.oauth2.AccessToken;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResultFactory;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Configuration;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;
import com.microsoft.identity.common.internal.providers.oauth2.TokenErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;

import java.net.HttpURLConnection;
import java.util.List;

public class LiOAuth2Strategy extends OAuth2Strategy {

    private static final String TAG = LiOAuth2Strategy.class.getSimpleName();

    /**
     * Constructor of OAuth2Strategy.
     *
     * @param config generic OAuth2 configuration
     */
    public LiOAuth2Strategy(OAuth2Configuration config) {
        super(config);
        setTokenEndpoint("https://www.linkedin.com/oauth/v2/accessToken");
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
    public BaseAccount createAccount(TokenResponse response) {
        return null;
    }


    @Override
    public AuthorizationRequest.Builder createAuthorizationRequestBuilder() {
        return new LiAuthorizationRequest.Builder();
    }

    @Override
    public AuthorizationRequest.Builder createAuthorizationRequestBuilder(IAccountRecord account) {
        return new LiAuthorizationRequest.Builder();
    }

    @Override
    public TokenRequest createTokenRequest(AuthorizationRequest request, AuthorizationResponse response) {
        final String methodName = ":createTokenRequest";
        Logger.verbose(
                TAG + methodName,
                "Creating TokenRequest..."
        );
        TokenRequest tokenRequest = new TokenRequest();
        tokenRequest.setCodeVerifier(request.getPkceChallenge().getCodeVerifier());
        tokenRequest.setCode(response.getCode());
        tokenRequest.setRedirectUri(request.getRedirectUri());
        tokenRequest.setClientId(request.getClientId());

        return tokenRequest;
    }

    @Override
    public TokenRequest createRefreshTokenRequest(RefreshTokenRecord refreshToken, List scopes) {
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
        final String methodName = ":getTokenResultFromHttpResponse";
        Logger.verbose(
                TAG + methodName,
                "Getting TokenResult from HttpResponse..."
        );
        TokenResponse tokenResponse = null;
        TokenErrorResponse tokenErrorResponse = null;

        if (response.getStatusCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
            //An error occurred
            tokenErrorResponse = ObjectMapper.deserializeJsonStringToObject(response.getBody(), TokenErrorResponse.class);
        } else {
            tokenResponse = ObjectMapper.deserializeJsonStringToObject(response.getBody(), TokenResponse.class);
        }

        return new TokenResult(tokenResponse, tokenErrorResponse);
    }

    @Override
    public boolean supportsOIDC() {
        return false;
    }
}
