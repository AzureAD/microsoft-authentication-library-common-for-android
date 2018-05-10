package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;

import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.net.HttpResponse;
import com.microsoft.identity.common.internal.net.ObjectMapper;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftTokenErrorResponse;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftTokenResponse;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.ClientInfo;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.TokenErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;

import java.net.URL;

public class MicrosoftStsOAuth2Strategy
        extends OAuth2Strategy
        <MicrosoftStsAccessToken,
                MicrosoftStsAccount,
                MicrosoftStsAuthorizationRequest,
                AuthorizationResponse,
                AuthorizationStrategy,
                MicrosoftStsOAuth2Configuration,
                MicrosoftStsRefreshToken,
                TokenRequest,
                MicrosoftStsTokenResponse,
                TokenResult> {

    private MicrosoftStsOAuth2Configuration mConfig;

    public MicrosoftStsOAuth2Strategy(MicrosoftStsOAuth2Configuration config) {
        super(config);
        mConfig = config;
        mTokenEndpoint = "https://login.microsoftonline.com/microsoft.com/oAuth2/v2.0/token";
    }

    @Override
    public String getIssuerCacheIdentifier(MicrosoftStsAuthorizationRequest request) {
        final URL authority = ((MicrosoftStsAuthorizationRequest) request).getAuthority();
        // TODO I don't think this is right... This is probably not the correct authority cache to consult...
        final AzureActiveDirectoryCloud cloudEnv = AzureActiveDirectory.getAzureActiveDirectoryCloud(authority);
        // This map can only be consulted if authority validation is on.
        // If the host has a hardcoded trust, we can just use the hostname.
        if (null != cloudEnv) {
            return cloudEnv.getPreferredNetworkHostName();
        }
        return authority.getHost();
    }

    @Override
    public MicrosoftStsAccessToken getAccessTokenFromResponse(MicrosoftStsTokenResponse response) {
        return new MicrosoftStsAccessToken(response);
    }

    @Override
    public MicrosoftStsRefreshToken getRefreshTokenFromResponse(MicrosoftStsTokenResponse response) {
        return new MicrosoftStsRefreshToken(response);
    }

    @Override
    public MicrosoftStsAccount createAccount(MicrosoftStsTokenResponse response) {
        IDToken idToken = null;
        ClientInfo clientInfo = null;
        try {
            idToken = new IDToken(response.getIdToken());
            clientInfo = new ClientInfo(response.getClientInfo());
        } catch (ServiceException ccse) {
            // TODO: Add a log here
            // TODO: Should we bail?
        }

        return MicrosoftStsAccount.create(idToken, clientInfo);
    }

    @Override
    protected void validateAuthorizationRequest(MicrosoftStsAuthorizationRequest request) {
        // TODO implement

    }

    @Override
    protected void validateTokenRequest(TokenRequest request) {
        // TODO implement
    }

    @Override
    protected TokenResult getTokenResultFromHttpResponse(HttpResponse response) {
        TokenResponse tokenResponse = null;
        TokenErrorResponse tokenErrorResponse = null;

        if (response.getStatusCode() >= 400) {
            //An error occurred
            tokenErrorResponse = ObjectMapper.deserializeJsonStringToObject(response.getBody(), MicrosoftTokenErrorResponse.class);
        } else {
            tokenResponse = ObjectMapper.deserializeJsonStringToObject(response.getBody(), MicrosoftTokenResponse.class);
        }

        return new TokenResult(tokenResponse, tokenErrorResponse);
    }
}
