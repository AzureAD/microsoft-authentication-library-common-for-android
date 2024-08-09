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
package com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.java.commands.parameters.RopcTokenCommandParameters;
import com.microsoft.identity.common.java.dto.IAccountRecord;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftTokenErrorResponse;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResultFactory;
import com.microsoft.identity.common.java.providers.oauth2.IAuthorizationStrategy;
import com.microsoft.identity.common.java.providers.oauth2.IDToken;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters;
import com.microsoft.identity.common.java.providers.oauth2.TokenErrorResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.java.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;
import com.microsoft.identity.common.java.util.ObjectMapper;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.util.CommonURIBuilder;

import java.net.HttpURLConnection;
import java.net.URISyntaxException;

import static com.microsoft.identity.common.java.exception.ErrorStrings.AUTHORITY_URL_NOT_VALID;

/**
 * The Azure Active Directory OAuth 2.0 Strategy.
 */
// Suppressing rawtype warnings due to the generic types AuthorizationStrategy, AuthorizationResult and AuthorizationResultFactory
@SuppressWarnings(WarningType.rawtype_warning)
public class AzureActiveDirectoryOAuth2Strategy
        extends OAuth2Strategy<
        AzureActiveDirectoryAccessToken,
        AzureActiveDirectoryAccount,
        AzureActiveDirectoryAuthorizationRequest,
        AzureActiveDirectoryAuthorizationRequest.Builder,
        IAuthorizationStrategy,
        AzureActiveDirectoryOAuth2Configuration,
        OAuth2StrategyParameters,
        AzureActiveDirectoryAuthorizationResponse,
        AzureActiveDirectoryRefreshToken,
        AzureActiveDirectoryTokenRequest,
        AzureActiveDirectoryTokenResponse,
        TokenResult,
        AuthorizationResult> {

    private static final String TAG = AzureActiveDirectoryOAuth2Strategy.class.getSimpleName();

    /**
     * Constructor of AzureActiveDirectoryOAuth2Strategy.
     *
     * @param config Azure Active Directory OAuth2 configuration
     */
    public AzureActiveDirectoryOAuth2Strategy(final AzureActiveDirectoryOAuth2Configuration config,
                                              final OAuth2StrategyParameters options) throws ClientException {
        super(config, options);
        Logger.verbose(TAG, "Init: " + TAG);
        if (null != config.getAuthorityUrl()) {
            setTokenEndpoint(config.getAuthorityUrl().toString() + "/oauth2/token");
        } else {
            setTokenEndpoint("https://login.microsoftonline.com/microsoft.com/oauth2/token");
        }
    }

    @Override
    public AuthorizationResultFactory getAuthorizationResultFactory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getIssuerCacheIdentifier(final AzureActiveDirectoryAuthorizationRequest authRequest) throws ClientException {
        final String methodTag = TAG + ":getIssuerCacheIdentifier";

        final AzureActiveDirectoryCloud cloud = AzureActiveDirectory.getAzureActiveDirectoryCloud(authRequest.getAuthority());
        if (cloud == null) {
            if (!getOAuth2Configuration().isAuthorityHostValidationEnabled()) {
                Logger.warn(methodTag, "Discovery data does not include cloud authority and validation is off."
                        + " Returning passed in Authority: "
                        + authRequest.getAuthority().toString());
                return authRequest.getAuthority().toString();
            }

            throw new ClientException(AUTHORITY_URL_NOT_VALID,
                    "Discovery data does not include cloud authority and validation is on.");
        }

        if (!cloud.isValidated() && getOAuth2Configuration().isAuthorityHostValidationEnabled()) {
            Logger.warn(methodTag, "Authority host validation has been enabled. This data hasn't been validated, though.");
            // We have invalid cloud data... and authority host validation is enabled....
            // TODO: Throw an exception in this case... need to see what ADAL does in this case.
            // If Cloud is null, e.g. Authority is PPE and AAD PE doesn't include it in the discovery data, this will similarly throw:
            // java.lang.NullPointerException: Attempt to invoke virtual method
            // 'boolean com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud.isValidated()'
            // on a null object reference
        }

        if (!cloud.isValidated() && !getOAuth2Configuration().isAuthorityHostValidationEnabled()) {
            Logger.warn(
                    methodTag,
                    "Authority host validation not specified..."
                            + "but there is no cloud..."
                            + "Hence just return the passed in Authority"
            );

            return authRequest.getAuthority().toString();
        }

        Logger.info(methodTag, "Building authority URI");

        try {
            final String issuerCacheIdentifier = new CommonURIBuilder(authRequest.getAuthority().toString())
                    .setHost(cloud.getPreferredCacheHostName())
                    .build().toString();

            Logger.infoPII(methodTag, "Issuer cache identifier created: " + issuerCacheIdentifier);
            return issuerCacheIdentifier;
        } catch (final URISyntaxException e) {
            throw new ClientException(ClientException.MALFORMED_URL, e.getMessage(), e);
        }
    }

    @Override
    public AzureActiveDirectoryAccessToken getAccessTokenFromResponse(
            @NonNull final AzureActiveDirectoryTokenResponse response) {
        final AzureActiveDirectoryAccessToken at = new AzureActiveDirectoryAccessToken(response);

        return at;
    }

    @Override
    public AzureActiveDirectoryRefreshToken getRefreshTokenFromResponse(
            @NonNull final AzureActiveDirectoryTokenResponse response) {
        final AzureActiveDirectoryRefreshToken rt = new AzureActiveDirectoryRefreshToken(response);

        return rt;
    }

    /**
     * Stubbed out for now, but should create a new AzureActiveDirectory account.
     * Should accept a parameter (TokenResponse) for producing that user
     *
     * @return
     */
    @Override
    public AzureActiveDirectoryAccount createAccount(
            @NonNull final AzureActiveDirectoryTokenResponse response) {
        final String methodTag = TAG + ":createAccount";

        IDToken idToken = null;
        ClientInfo clientInfo = null;

        try {
            Logger.info(methodTag, "Constructing IDToken from response");
            idToken = new IDToken(response.getIdToken());

            Logger.info(methodTag, "Constructing ClientInfo from response");
            clientInfo = new ClientInfo(response.getClientInfo());
        } catch (ServiceException ccse) {
            Logger.error(methodTag, "Failed to construct IDToken or ClientInfo", null);
            Logger.errorPII(methodTag, "Failed with Exception", ccse);
            throw new RuntimeException();
        }

        final AzureActiveDirectoryAccount account = new AzureActiveDirectoryAccount(idToken, clientInfo);

        Logger.info(methodTag, "Account created");
        Logger.infoPII(methodTag, account.toString());

        return account;
    }

    @Override
    public AzureActiveDirectoryAuthorizationRequest.Builder createAuthorizationRequestBuilder() {
        return new AzureActiveDirectoryAuthorizationRequest.Builder();
    }

    @Override
    public AzureActiveDirectoryAuthorizationRequest.Builder createAuthorizationRequestBuilder(IAccountRecord account) {
        return createAuthorizationRequestBuilder();
    }

    @Override
    public AzureActiveDirectoryTokenRequest createTokenRequest(
            AzureActiveDirectoryAuthorizationRequest request,
            AzureActiveDirectoryAuthorizationResponse response,
            AbstractAuthenticationScheme scheme) {
        return null;
    }

    @Override
    public AzureActiveDirectoryTokenRequest createRefreshTokenRequest(AbstractAuthenticationScheme scheme) {
        return null;
    }

    @Override
    public AzureActiveDirectoryTokenRequest createRopcTokenRequest(RopcTokenCommandParameters tokenCommandParameters) throws ClientException {
        return null;
    }

    @Override
    protected void validateAuthorizationRequest(final AzureActiveDirectoryAuthorizationRequest request) {
        // TODO
    }

    /**
     * validate the contents of the token request... all the base class is currently abstract
     * some of the validation for required parameters for the protocol could be there...
     *
     * @param request
     */
    @Override
    protected void validateTokenRequest(final AzureActiveDirectoryTokenRequest request) {
        // TODO
    }

    @Override
    protected TokenResult getTokenResultFromHttpResponse(
            final HttpResponse response,
            @Nullable final AzureActiveDirectoryTokenRequest request
    ) {
        final String methodName = "getTokenResultFromHttpResponse";

        TokenResponse tokenResponse = null;
        TokenErrorResponse tokenErrorResponse = null;

        if (response.getStatusCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
            //An error occurred
            Logger.warn(TAG + ":" + methodName, "Status code was: " + response.getStatusCode());
            tokenErrorResponse = ObjectMapper.deserializeJsonStringToObject(response.getBody(), MicrosoftTokenErrorResponse.class);
        } else {
            tokenResponse = ObjectMapper.deserializeJsonStringToObject(response.getBody(), AzureActiveDirectoryTokenResponse.class);
        }

        final TokenResult result = new TokenResult(tokenResponse, tokenErrorResponse);

        return result;
    }

    @Override
    protected void validateTokenResponse(AzureActiveDirectoryTokenRequest request,
                                         AzureActiveDirectoryTokenResponse response) {
        // TODO
    }

}
