// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.internal.providers.oauth2;

import android.net.Uri;
import android.text.TextUtils;

import com.microsoft.identity.common.BaseAccount;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.dto.IAccountRecord;
import com.microsoft.identity.common.internal.eststelemetry.EstsTelemetry;
import com.microsoft.identity.common.internal.logging.DiagnosticContext;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.net.HttpRequest;
import com.microsoft.identity.common.internal.net.HttpResponse;
import com.microsoft.identity.common.internal.net.ObjectMapper;
import com.microsoft.identity.common.internal.platform.Device;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftTokenRequest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Future;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AAD.CLIENT_REQUEST_ID;

/**
 * Serves as the abstract base class for an oAuth2 client implementation; The base class should be extended
 * by Identity Provider specific implementations; For example: Azure Active Directory, ADFS, Microsoft STS, Etc...
 */
public abstract class OAuth2Strategy
        <GenericAccessToken extends AccessToken,
                GenericAccount extends BaseAccount,
                GenericAuthorizationRequest extends AuthorizationRequest,
                GenericAuthorizationRequestBuilder extends AuthorizationRequest.Builder,
                GenericAuthorizationStrategy extends AuthorizationStrategy,
                GenericOAuth2Configuration extends OAuth2Configuration,
                GenericAuthorizationResponse extends AuthorizationResponse,
                GenericRefreshToken extends RefreshToken,
                GenericTokenRequest extends TokenRequest,
                GenericTokenResponse extends TokenResponse,
                GenericTokenResult extends TokenResult,
                GenericAuthorizationResult extends AuthorizationResult> {

    private static final String TAG = OAuth2Strategy.class.getSimpleName();

    protected static final String TOKEN_REQUEST_CONTENT_TYPE = "application/x-www-form-urlencoded";

    protected final GenericOAuth2Configuration mConfig;
    protected String mTokenEndpoint;
    protected String mAuthorizationEndpoint;
    private Uri mIssuer;

    /**
     * Constructor of OAuth2Strategy.
     *
     * @param config generic OAuth2 configuration
     */
    public OAuth2Strategy(GenericOAuth2Configuration config) {
        mConfig = config;
    }

    /**
     * Template method for executing an OAuth2 authorization request.
     *
     * @param request               generic authorization request.
     * @param authorizationStrategy generic authorization strategy.
     * @return GenericAuthorizationResponse
     */
    public Future<AuthorizationResult> requestAuthorization(
            final GenericAuthorizationRequest request,
            final GenericAuthorizationStrategy authorizationStrategy) {
        validateAuthorizationRequest(request);
        Future<AuthorizationResult> future = null;
        try {
            future = authorizationStrategy.requestAuthorization(request, this);
        } catch (final UnsupportedEncodingException | ClientException exc) {
            //TODO
        }

        return future;
    }

    public abstract AuthorizationResultFactory getAuthorizationResultFactory();

    /**
     * @param request generic token request.
     * @return GenericTokenResult
     * @throws IOException thrown when failed or interrupted I/O operations occur.
     */
    public GenericTokenResult requestToken(final GenericTokenRequest request) throws IOException, ClientException {
        final String methodName = ":requestToken";

        Logger.verbose(
                TAG + methodName,
                "Requesting token..."
        );

        validateTokenRequest(request);

        final HttpResponse response = performTokenRequest(request);
        return getTokenResultFromHttpResponse(response);
    }

    protected HttpResponse performTokenRequest(final GenericTokenRequest request) throws IOException, ClientException {
        final String methodName = ":performTokenRequest";

        Logger.verbose(
                TAG + methodName,
                "Performing token request..."
        );

        final String requestBody = ObjectMapper.serializeObjectToFormUrlEncoded(request);
        final Map<String, String> headers = new TreeMap<>();
        headers.put(CLIENT_REQUEST_ID, DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID));

        if (request instanceof MicrosoftTokenRequest &&
                !TextUtils.isEmpty(((MicrosoftTokenRequest) request).getBrokerVersion())) {
            headers.put(
                    Device.PlatformIdParameters.BROKER_VERSION,
                    ((MicrosoftTokenRequest) request).getBrokerVersion()
            );
        }
        headers.putAll(Device.getPlatformIdParameters());
        headers.putAll(EstsTelemetry.getInstance().getTelemetryHeaders());

        return HttpRequest.sendPost(
                new URL(mTokenEndpoint),
                headers,
                requestBody.getBytes(ObjectMapper.ENCODING_SCHEME),
                TOKEN_REQUEST_CONTENT_TYPE
        );
    }

    protected final void setTokenEndpoint(final String tokenEndpoint) {
        mTokenEndpoint = tokenEndpoint;
    }

    public String getAuthorityFromTokenEndpoint() {
        return mTokenEndpoint.toLowerCase().replace("oauth2/v2.0/token", "");
    }

    protected final void setAuthorizationEndpoint(final String authorizationEndpoint) {
        mAuthorizationEndpoint = authorizationEndpoint;
    }

    protected GenericOAuth2Configuration getOAuth2Configuration() {
        return mConfig;
    }

    protected Uri getIssuer() {
        return mIssuer;
    }

    protected final void setIssuer(final Uri issuer) {
        mIssuer = issuer;
    }

    /**
     * An abstract method for returning the issuer identifier to be used when caching a token response.
     *
     * @param request generic token request.
     * @return String of issuer cache identifier.
     */
    public abstract String getIssuerCacheIdentifier(GenericAuthorizationRequest request);

    /**
     * @param response generic token response.
     * @return generic access token.
     */
    public abstract GenericAccessToken getAccessTokenFromResponse(GenericTokenResponse response);

    /**
     * @param response generic token response.
     * @return generic refresh token.
     */
    public abstract GenericRefreshToken getRefreshTokenFromResponse(GenericTokenResponse response);

    /**
     * An abstract method for returning the user associated with a request.
     * This could be based on the contents of the ID Token or it could be returned based on making a call
     * to the user_info or profile endpoint associated with a userr: For example: graph.microsoft.com/me
     * This allows IDPs that do not support OIDC to still be able to return a user to us
     * This method should take the TokenResponse as a parameter
     *
     * @param response Generic token response.
     * @return GenericAccount
     */
    public abstract GenericAccount createAccount(GenericTokenResponse response);

    /**
     * Abstract method for creating the authorization request.
     *
     * @return AuthorizationRequest.
     */
    public abstract GenericAuthorizationRequestBuilder createAuthorizationRequestBuilder();

    /**
     * Abstract method for creating the authorization request.
     *
     * @param account The IAccount available to this strategy.
     * @return AuthorizationRequest.
     */
    public abstract GenericAuthorizationRequestBuilder createAuthorizationRequestBuilder(IAccountRecord account);

    /**
     * Abstract method for creating the token request.  In the case of AAD this is the method
     *
     * @return TokenRequest.
     */
    public abstract GenericTokenRequest createTokenRequest(GenericAuthorizationRequest request, GenericAuthorizationResponse response);

    /**
     * Abstract method for creating the refresh token request.
     *
     * @return TokenRequest.
     */
    public abstract GenericTokenRequest createRefreshTokenRequest();

    /**
     * Abstract method for validating the authorization request.  In the case of AAD this is the method
     * from which the details of the authorization request including authority validation would occur (preferred network and preferred cache)
     *
     * @param request generic authorization request.
     */
    protected abstract void validateAuthorizationRequest(GenericAuthorizationRequest request);

    /**
     * Abstract method for validating the token request.  Generally speaking I expect this just to be validating
     * that all of the information was provided in the Token Request in order to successfully complete it.
     *
     * @param request Generic token request.
     */
    protected abstract void validateTokenRequest(GenericTokenRequest request);

    /**
     * Abstract method for translating the HttpResponse to a TokenResponse.
     *
     * @param response Http response.
     */
    protected abstract GenericTokenResult getTokenResultFromHttpResponse(HttpResponse response);

    // TODO
//    protected abstract void validateAuthorizationResponse(GenericAuthorizationResponse response);

//    protected abstract void validateTokenResponse(GenericTokenResponse response);
}