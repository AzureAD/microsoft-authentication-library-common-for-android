package com.microsoft.identity.common.internal.providers.azureactivedirectoryb2c;

import android.net.Uri;

import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.internal.providers.oauth2.AccessToken;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Configuration;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;


/**
 * Azure Active Directory B2C OAuth Strategy
 * See the following for more information on the B2C OAuth Implementation:
 * see <a href='https://docs.microsoft.com/en-us/azure/active-directory-b2c/active-directory-b2c-reference-oauth-code'>
 *     https://docs.microsoft.com/en-us/azure/active-directory-b2c/active-directory-b2c-reference-oauth-code</a>
 */
public class AzureActiveDirectoryB2COAuth2Strategy extends OAuth2Strategy {
    public AzureActiveDirectoryB2COAuth2Strategy(OAuth2Configuration config) {
        super(config);
    }

    @Override
    public AuthorizationResponse requestAuthorization(AuthorizationRequest request, AuthorizationStrategy authorizationStrategy) {
        return super.requestAuthorization(request, authorizationStrategy);
    }

    @Override
    protected Uri createAuthorizationUri() {
        return super.createAuthorizationUri();
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
    protected void validateAuthoriztionRequest(AuthorizationRequest request) {

    }

    @Override
    protected void validateTokenRequest(TokenRequest request) {

    }
}
