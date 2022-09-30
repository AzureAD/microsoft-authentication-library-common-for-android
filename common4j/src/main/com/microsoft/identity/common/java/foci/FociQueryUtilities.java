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
package com.microsoft.identity.common.java.foci;

import static com.microsoft.identity.common.java.authorities.AllAccounts.ALL_ACCOUNTS_TENANT_ID;

import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.authscheme.BearerAuthenticationSchemeInternal;
import com.microsoft.identity.common.java.cache.BrokerOAuth2TokenCache;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.controllers.BaseController;
import com.microsoft.identity.common.java.dto.IAccountRecord;
import com.microsoft.identity.common.java.dto.RefreshTokenRecord;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftTokenResponse;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenRequest;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.CommonURIBuilder;
import com.microsoft.identity.common.java.util.ported.ObjectUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.NonNull;

public class FociQueryUtilities {

    private static final String TAG = FociQueryUtilities.class.getSimpleName();

    /**
     * Testing whether the given client ID can use the cached foci to refresh token.
     *
     * @param clientId    String of the given client id.
     * @param redirectUri redirect url string of the given client id.
     * @param cacheRecord Foci cache record.
     * @return true if the given client id can use the cached foci token. False, otherwise.
     * @throws ClientException
     * @throws IOException
     */
    public static boolean tryFociTokenWithGivenClientId(@SuppressWarnings(WarningType.rawtype_warning) @NonNull final BrokerOAuth2TokenCache brokerOAuth2TokenCache,
                                                        @NonNull final String clientId,
                                                        @NonNull final String redirectUri,
                                                        @NonNull final ICacheRecord cacheRecord) throws IOException, ClientException {
        return tryFociTokenWithGivenClientId(
                brokerOAuth2TokenCache,
                clientId, redirectUri,
                cacheRecord.getRefreshToken(),
                cacheRecord.getAccount()
        );
    }

    /**
     * Testing whether the given client ID can use the cached foci to refresh token.
     *
     * @param clientId           String of the given client id.
     * @param redirectUri        redirect url string of the given client id.
     * @param accountRecord      account record of request
     * @param refreshTokenRecord refresh token record of FOCI account
     * @return true if the given client id can use the cached foci token. False, otherwise.
     * @throws ClientException
     * @throws IOException
     */
    public static boolean tryFociTokenWithGivenClientId(@SuppressWarnings(WarningType.rawtype_warning) @NonNull final OAuth2TokenCache brokerOAuth2TokenCache,
                                                        @NonNull final String clientId,
                                                        @NonNull final String redirectUri,
                                                        @NonNull final RefreshTokenRecord refreshTokenRecord,
                                                        @NonNull final IAccountRecord accountRecord)
            throws ClientException, IOException {
        final String methodName = ":tryFociTokenWithGivenClientId";
        final MicrosoftStsOAuth2Configuration config = new MicrosoftStsOAuth2Configuration();

        //Get authority url
        final CommonURIBuilder requestUrlBuilder = new CommonURIBuilder();
        requestUrlBuilder.setScheme("https")
                .setHost(refreshTokenRecord.getEnvironment())
                .setPath(StringUtil.isNullOrEmpty(accountRecord.getRealm()) ? ALL_ACCOUNTS_TENANT_ID : accountRecord.getRealm());
        final URL authorityUrl;
        try {
            authorityUrl = new URL(requestUrlBuilder.build().toString());
        } catch (URISyntaxException e) {
            throw new ClientException(ErrorStrings.MALFORMED_URL, e.getMessage(), e);
        }

        //set the token endpoint for the configuration
        config.setAuthorityUrl(authorityUrl);

        // Create the strategy
        final OAuth2StrategyParameters strategyParameters = OAuth2StrategyParameters.builder().build();
        final MicrosoftStsOAuth2Strategy strategy = new MicrosoftStsOAuth2Strategy(config, strategyParameters);

        final String refreshToken = refreshTokenRecord.getSecret();
        final String scopes;
        // Hardcoding Teams Agent's client ID with the scope it's pre-authorized for.
        // This is because if only the default scope is passed, eSTS will set the resource ID (on its side)
        // based on the RT (Which the given clientId might not be pre-authorized for).
        // TODO: make pre-authorization of MSGraph User.read (and the default scopes) a requirement
        //       for every FoCI apps (and hardcode it here).
        //       https://identitydivision.visualstudio.com/Engineering/_workitems/edit/1222002
        if (ObjectUtils.equals(clientId, "87749df4-7ccf-48f8-aa87-704bad0e0e16")) {
            final Span span = GlobalOpenTelemetry.getTracer(TAG).spanBuilder("setScopeForDMAgentForFoci").startSpan();
            try (final Scope scope = span.makeCurrent()) {
                scopes = "https://devicemgmt.teams.microsoft.com/.default " + BaseController.getDelimitedDefaultScopeString();
                Logger.info(TAG + methodName,
                        "Teams agent client ID - making a test request with teams agent resource.");
            }
            finally {
                span.end();
            }
        } else {
            scopes = BaseController.getDelimitedDefaultScopeString();
        }

        // Create a correlation_id for the request
        final UUID correlationId = UUID.randomUUID();

        Logger.verbose(TAG + methodName,
                "Create the token request with correlationId ["
                        + correlationId
                        + "]");

        final MicrosoftStsTokenRequest tokenRequest = createTokenRequest(
                clientId,
                scopes,
                refreshToken,
                redirectUri,
                strategy,
                correlationId,
                "2"
        );

        Logger.verbose(TAG + methodName,
                "Start refreshing token (to verify foci) with correlationId ["
                        + correlationId
                        + "]");
        final TokenResult tokenResult = strategy.requestToken(tokenRequest);

        Logger.verbose(TAG + methodName,
                "Is the client ID able to use the foci? ["
                        + tokenResult.getSuccess()
                        + "] with correlationId ["
                        + correlationId
                        + "]");

        if (tokenResult.getSuccess()) {
            // Save the token record in tha cache so that we have an entry in BrokerApplicationMetadata for this client id.
            final MicrosoftStsAuthorizationRequest authorizationRequest = createAuthRequest(
                    strategy,
                    clientId,
                    redirectUri,
                    scopes,
                    accountRecord,
                    correlationId
            );
            Logger.verbose(TAG + methodName,
                    "Saving records to cache with client id" + clientId
            );
            brokerOAuth2TokenCacheSave(brokerOAuth2TokenCache, strategy, tokenResult, authorizationRequest);
        }
        return tokenResult.getSuccess();
    }

    /**
     * Create the token request used to refresh the cache RTs.
     *
     * @param clientId      The clientId of the app which "owns" this token.
     * @param scopes        The scopes to include in the request.
     * @param refreshToken  The token to refresh/
     * @param redirectUri   The redirect uri for this request.
     * @param strategy      The strategy to create the TokenRequest.
     * @param correlationId The correlation id to send in the request.
     * @return The fully-formed TokenRequest.
     */
    @NonNull
    public static MicrosoftStsTokenRequest createTokenRequest(@NonNull final String clientId,
                                                              @NonNull final String scopes,
                                                              @NonNull final String refreshToken,
                                                              @NonNull final String redirectUri,
                                                              @NonNull final MicrosoftStsOAuth2Strategy strategy,
                                                              @Nullable final UUID correlationId,
                                                              @NonNull final String idTokenVersion) throws ClientException {
        final MicrosoftStsTokenRequest tokenRequest =
                strategy.createRefreshTokenRequest(new BearerAuthenticationSchemeInternal());

        // Set the request properties
        tokenRequest.setClientId(clientId);
        tokenRequest.setScope(scopes);
        tokenRequest.setCorrelationId(correlationId);
        tokenRequest.setRefreshToken(refreshToken);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setIdTokenVersion(idTokenVersion);

        return tokenRequest;
    }

    private static MicrosoftStsAuthorizationRequest createAuthRequest(@NonNull final MicrosoftStsOAuth2Strategy strategy,
                                                                      @NonNull final String clientId,
                                                                      @NonNull final String redirectUri,
                                                                      @NonNull final String scope,
                                                                      @NonNull final IAccountRecord accountRecord,
                                                                      @Nullable final UUID correlationId) {
        final MicrosoftStsAuthorizationRequest.Builder builder = strategy.createAuthorizationRequestBuilder(
                accountRecord
        );
        return builder.setClientId(clientId)
                .setRedirectUri(redirectUri)
                .setCorrelationId(correlationId)
                .setScope(scope)
                .build();
    }

    // Suppressing unchecked warnings due to casting of rawtypes to generic types of OAuth2TokenCache's instance brokerOAuth2TokenCache while calling method save
    @SuppressWarnings(WarningType.unchecked_warning)
    private static void brokerOAuth2TokenCacheSave(@SuppressWarnings(WarningType.rawtype_warning) @NonNull OAuth2TokenCache brokerOAuth2TokenCache, MicrosoftStsOAuth2Strategy strategy, TokenResult tokenResult, MicrosoftStsAuthorizationRequest authorizationRequest) throws ClientException {
        brokerOAuth2TokenCache.save(
                strategy,
                authorizationRequest,
                (MicrosoftTokenResponse) tokenResult.getTokenResponse()
        );
    }
}
