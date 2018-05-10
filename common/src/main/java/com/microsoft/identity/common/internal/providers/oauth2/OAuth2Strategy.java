package com.microsoft.identity.common.internal.providers.oauth2;

import android.net.Uri;

import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.internal.net.HttpRequest;
import com.microsoft.identity.common.internal.net.HttpResponse;
import com.microsoft.identity.common.internal.net.ObjectMapper;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;


/**
 * Serves as the abstract base class for an oAuth2 client implementation; The base class should be extended
 * by Identity Provider specific implementations; For example: Azure Active Directory, ADFS, Microsoft STS, Etc...
 */
public abstract class OAuth2Strategy
        <G_AccessToken extends AccessToken,
                G_Account extends Account,
                G_AuthorizationRequest extends AuthorizationRequest,
                G_AuthorizationResponse extends AuthorizationResponse,
                G_AuthorizationStrategy extends AuthorizationStrategy,
                G_OAuth2Configuration extends OAuth2Configuration,
                G_RefreshToken extends RefreshToken,
                G_TokenRequest extends TokenRequest,
                G_TokenResponse extends TokenResponse,
                G_TokenResult extends TokenResult> {

    protected String mTokenEndpoint;
    protected String mAuthorizationEndpoint;
    protected Uri mIssuer;

    protected static final String TOKEN_REQUEST_CONTENT_TYPE = "application/x-www-form-urlencoded";

    public OAuth2Strategy(G_OAuth2Configuration config) {

    }

    /**
     * Template method for executing an OAuth2 authorization request
     *
     * @param request
     * @param authorizationStrategy
     * @return
     */
    public G_AuthorizationResponse requestAuthorization(
            final G_AuthorizationRequest request,
            final G_AuthorizationStrategy authorizationStrategy) {
        validateAuthorizationRequest(request);
        Uri authorizationUri = createAuthorizationUri();
        AuthorizationResult result = authorizationStrategy.requestAuthorization(request);
        //TODO: Reconcile authorization result and response
        AuthorizationResponse response = new AuthorizationResponse();
        return (G_AuthorizationResponse) response;
    }


    public G_TokenResult requestToken(final G_TokenRequest request) throws IOException {
        validateTokenRequest(request);
        HttpResponse response = performTokenRequest(request);
        return getTokenResultFromHttpResponse(response);
    }


    protected HttpResponse performTokenRequest(final G_TokenRequest request) throws IOException {

        String requestBody = ObjectMapper.serializeObjectToFormUrlEncoded(request);
        Map<String, String> headers = new TreeMap<>();
        String correlationId = UUID.randomUUID().toString();
        headers.put("client-request-id", correlationId);

        return HttpRequest.sendPost(
                new URL(mTokenEndpoint),
                headers,
                requestBody.getBytes(ObjectMapper.ENCODING_SCHEME),
                TOKEN_REQUEST_CONTENT_TYPE
        );
    }


    /**
     * Construct the authorization endpoint URI based on issuer and path to the authorization endpoint
     * NOTE: We could look at basing this on the contents returned from the OpenID Configuration document
     *
     * @return
     */
    protected Uri createAuthorizationUri() {
        //final Uri.Builder builder = new Uri.Builder().scheme(originalAuthority.getProtocol()).authority(host).appendPath(path);
        Uri authorizationUri = Uri.withAppendedPath(mIssuer, mAuthorizationEndpoint);
        return authorizationUri;
    }

    /**
     * An abstract method for returning the issuer identifier to be used when caching a token response
     *
     * @return
     */
    public abstract String getIssuerCacheIdentifier(G_AuthorizationRequest request);

    public abstract G_AccessToken getAccessTokenFromResponse(G_TokenResponse response);

    public abstract G_RefreshToken getRefreshTokenFromResponse(G_TokenResponse response);

    /**
     * An abstract method for returning the user associated with a request;  This
     * could be based on the contents of the ID Token or it could be returned based on making a call
     * to the user_info or profile endpoint associated with a userr: For example: graph.microsoft.com/me
     * This allows IDPs that do not support OIDC to still be able to return a user to us
     * This method should take the TokenResponse as a parameter
     *
     * @return
     */
    public abstract G_Account createAccount(G_TokenResponse response);

    /**
     * Abstract method for validating the authorization request.  In the case of AAD this is the method
     * from which the details of the authorization request including authority validation would occur (preferred network and preferred cache)
     *
     * @param request
     */
    protected abstract void validateAuthorizationRequest(G_AuthorizationRequest request);

    /**
     * Abstract method for validating the token request.  Generally speaking I expect this just to be validating
     * that all of the information was provided in the Token Request in order to successfully complete it.
     *
     * @param request
     */
    protected abstract void validateTokenRequest(G_TokenRequest request);

    /**
     * Abstract method for translating the HttpResponse to a TokenResponse.
     *
     * @param response
     */
    protected abstract G_TokenResult getTokenResultFromHttpResponse(HttpResponse response);

    // TODO
//    protected abstract void validateAuthorizationResponse(G_AuthorizationResponse response);

//    protected abstract void validateTokenResponse(G_TokenResponse response);
}
