package com.microsoft.identity.common.internal.providers.oauth2;

import com.microsoft.identity.common.Account;

import android.media.session.MediaSession;
import android.net.Uri;


/**
 * Serves as the abstract base class for an oAuth2 client implementation; The base class should be extended
 * by Identity Provider specific implementations; For example: Azure Active Directory, ADFS, Microsoft STS, Etc...
 */
public abstract class OAuth2Strategy {

    protected String mTokenEndpoint;
    protected String mAuthorizationEndpoint;
    protected Uri mIssuer;

    /**
     * Template method for executing an OAuth2 authorization request
     * @param request
     * @param authorizationStrategy
     * @return
     */
    public AuthorizationResponse requestAuthorization(AuthorizationRequest request, AuthorizationStrategy authorizationStrategy){
        validateAuthoriztionRequest(request);
        Uri authorizationUri = createAuthorizationUri();
        AuthorizationResult result = authorizationStrategy.requestAuthorization(request);
        //TODO: Reconcile authorization result and response
        AuthorizationResponse response = new AuthorizationResponse();
        return response;
    }

    /**
     * Construct the authorization endpoint URI based on issue and path to the authorization endpoint
     * NOTE: We could look at basing this on the contennts returned from the OpenID Configuration document
     * @return
     */
    protected Uri createAuthorizationUri(){
        //final Uri.Builder builder = new Uri.Builder().scheme(originalAuthority.getProtocol()).authority(host).appendPath(path);
        Uri authorizationUri = Uri.withAppendedPath(mIssuer, mAuthorizationEndpoint);
        return authorizationUri;
    }

    /**
     * An abstract method for returning the issuer identifier to be used when caching a token response
     * @return
     */
    public abstract String getIssuerCacheIdentifier(AuthorizationRequest request);

    public abstract AccessToken getAccessTokenFromResponse(TokenResponse response);

    public abstract RefreshToken getRefreshTokenFromResponse(TokenResponse response);

    /**
     * An abstract method for returning the user associated with a request;  This
     * could be based on the contents of the ID Token or it could be returned based on making a call
     * to the user_info or profile endpoint associated with a userr: For example: graph.microsoft.com/me
     * This allows IDPs that do not support OIDC to still be able to return a user to us
     * This method should take the TokenResponse as a parameter
     * @return
     */
    public abstract Account createAccount(TokenResponse response);

    /**
     * Abstract method for validating the authorization request.  In the case of AAD this is the method
     * from which the details of the authorization request including authority validation would occur (preferred network and preferred cache)
     *
     * @param request
     */
    protected abstract void validateAuthoriztionRequest(AuthorizationRequest request);

    /**
     * Abstract method for validating the token request.  Generally speaking I expect this just to be validating
     * that all of the information was provided in the Token Request in order to successfully complete it.
     * @param request
     */
    protected abstract void validateTokenRequest(TokenRequest request);
}
