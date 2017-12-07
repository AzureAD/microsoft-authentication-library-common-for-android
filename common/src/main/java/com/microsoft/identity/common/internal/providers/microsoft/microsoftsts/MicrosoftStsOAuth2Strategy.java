package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;

import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.ClientInfo;
import com.microsoft.identity.common.internal.providers.oauth2.AccessToken;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

public class MicrosoftStsOAuth2Strategy extends OAuth2Strategy {

    private MicrosoftStsOAuth2Configuration mConfig;

    public MicrosoftStsOAuth2Strategy(MicrosoftStsOAuth2Configuration config) {
        super(config);
        mConfig = config;
    }

    @Override
    public String getIssuerCacheIdentifier(AuthorizationRequest request) {
        return null;
    }

    @Override
    public AccessToken getAccessTokenFromResponse(TokenResponse response) {
        MicrosoftStsAccessToken accessToken;

        if (response instanceof MicrosoftStsTokenResponse) {
            accessToken = new MicrosoftStsAccessToken(response);
        } else {
            throw new RuntimeException("Expected MicrosoftStsTokenResponse in MicrosoftStsOAuth2Strategy.getAccessTokenFromResponse");
        }

        return accessToken;
    }

    @Override
    public RefreshToken getRefreshTokenFromResponse(TokenResponse response) {
        MicrosoftStsRefreshToken refreshToken;

        if (response instanceof MicrosoftStsTokenResponse) {
            refreshToken = new MicrosoftStsRefreshToken((MicrosoftStsTokenResponse) response);
        } else {
            throw new RuntimeException("Expected AzureActiveDirectoryTokenResponse in AzureActiveDirectoryOAuth2Strategy.getRefreshTokenFromResponse");
        }
        return refreshToken;
    }

    @Override
    public Account createAccount(TokenResponse response) {
        IDToken idToken = new IDToken(response.getIdToken());
        ClientInfo clientInfo = new ClientInfo(((MicrosoftStsTokenResponse) response).getClientInfo());
        return MicrosoftStsAccount.create(idToken, clientInfo);
    }

    @Override
    protected void validateAuthorizationRequest(AuthorizationRequest request) {

    }

    @Override
    protected void validateTokenRequest(TokenRequest request) {

    }
}
