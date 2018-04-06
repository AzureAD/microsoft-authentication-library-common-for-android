package com.microsoft.identity.common.internal.providers.microsoft.activedirectoryfederationservices;

import android.media.session.MediaSession;
import android.net.Uri;

import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.internal.net.HttpResponse;
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
 * Azure Active Directory Federation Services 2012 R2 OAuth Strategy
 * ADFS 2012 R2 (v3) only support oAuth and did not yet support OIDC... as result there is
 * no opportunity to get an ID Token
 * Docs on this functionality are lacking....
 * see <a href='https://msdn.microsoft.com/en-us/library/dn633593.aspx'>https://msdn.microsoft.com/en-us/library/dn633593.aspx</a>
 * see <a href='https://blogs.technet.microsoft.com/maheshu/2015/04/28/oauth-2-0-support-in-adfs-on-windows-server-2012-r2/'>https://blogs.technet.microsoft.com/maheshu/2015/04/28/oauth-2-0-support-in-adfs-on-windows-server-2012-r2/</a>
 */
public class ActiveDirectoryFederationServices2012R2OAuth2Strategy extends OAuth2Strategy{
    public ActiveDirectoryFederationServices2012R2OAuth2Strategy(OAuth2Configuration config) {
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
    protected void validateAuthorizationRequest(AuthorizationRequest request) {
    }

    @Override
    protected void validateTokenRequest(TokenRequest request) {
    }

    @Override
    protected TokenResponse getTokenResponseFromHttpResponse(HttpResponse response){
        return null;
    }
}
