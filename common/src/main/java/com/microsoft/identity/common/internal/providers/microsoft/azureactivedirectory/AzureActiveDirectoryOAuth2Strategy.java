package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory;

import android.net.Uri;

import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.providers.oauth2.AccessToken;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

/**
 * The Azure Active Directory oAuth2 Strategy
 */
public class AzureActiveDirectoryOAuth2Strategy extends OAuth2Strategy {

    private AzureActiveDirectoryOAuth2Configuration mConfig = null;

    public AzureActiveDirectoryOAuth2Strategy(AzureActiveDirectoryOAuth2Configuration config) {
        super(config);

        mConfig = config;
    }

    @Override
    protected void validateAuthorizationRequest(AuthorizationRequest request) {

    }

    /**
     * validate the contents of the token request... all the base class is currently abstract
     * some of the validation for requried parameters for the protocol could be there...
     *
     * @param request
     */
    @Override
    protected void validateTokenRequest(TokenRequest request) {

    }

    /**
     * Stubbed out for now, but should create a new AzureActiveDirectory account
     * Should accept a parameter (TokenResponse) for producing that user
     *
     * @return
     */
    @Override
    public Account createAccount(TokenResponse response) {
        IDToken idToken = null;
        ClientInfo clientInfo = null;
        try {
            idToken = new IDToken(response.getIdToken());
            clientInfo = new ClientInfo(((AzureActiveDirectoryTokenResponse) response).getClientInfo());
        } catch (ServiceException ccse) {
            // TODO: Add a log here
            // TODO: Should we bail?
        }
        return AzureActiveDirectoryAccount.create(idToken, clientInfo);
    }

    @Override
    public String getIssuerCacheIdentifier(AuthorizationRequest request) {
        if (!(request instanceof AzureActiveDirectoryAuthorizationRequest)) {
            throw new IllegalArgumentException("Request provided is not of type AzureActiveDirectoryAuthorizationRequest");
        }

        AzureActiveDirectoryAuthorizationRequest authRequest;
        authRequest = (AzureActiveDirectoryAuthorizationRequest) request;
        AzureActiveDirectoryCloud cloud = AzureActiveDirectory.getAzureActiveDirectoryCloud(authRequest.getAuthority());

        if (!cloud.isValidated() && this.mConfig.isAuthorityHostValdiationEnabled()) {
            //We have invalid cloud data... and authority host validation is enabled....
            //TODO: Throw an exception in this case... need to see what ADAL does in this case.
        }

        if (!cloud.isValidated() && !this.mConfig.isAuthorityHostValdiationEnabled()) {
            //Authority host validation not specified... but there is no cloud....
            //Hence just return the passed in Authority
            return authRequest.getAuthority().toString();
        }

        Uri authorityUri = Uri.parse(authRequest.getAuthority().toString())
                .buildUpon()
                .authority(cloud.getPreferredCacheHostName())
                .build();

        return authorityUri.toString();

    }

    @Override
    public AccessToken getAccessTokenFromResponse(TokenResponse response) {
        if (!(response instanceof AzureActiveDirectoryTokenResponse)) {
            throw new IllegalArgumentException(
                    "Expected AzureActiveDirectoryTokenResponse in AzureActiveDirectoryOAuth2Strategy.getAccessTokenFromResponse");
        }
        return new AzureActiveDirectoryAccessToken(response);
    }

    @Override
    public RefreshToken getRefreshTokenFromResponse(TokenResponse response) {
        if (!(response instanceof AzureActiveDirectoryTokenResponse)) {
            throw new IllegalArgumentException(
                    "Expected AzureActiveDirectoryTokenResponse in AzureActiveDirectoryOAuth2Strategy.getRefreshTokenFromResponse");
        }
        return new AzureActiveDirectoryRefreshToken((AzureActiveDirectoryTokenResponse) response);
    }

}
