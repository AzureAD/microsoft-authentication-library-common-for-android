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
package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.dto.IAccountRecord;
import com.microsoft.identity.common.internal.dto.RefreshTokenRecord;
import com.microsoft.identity.common.internal.logging.DiagnosticContext;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.net.HttpResponse;
import com.microsoft.identity.common.internal.net.ObjectMapper;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftTokenErrorResponse;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.ClientInfo;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResultFactory;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.TokenErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.UUID;

public class MicrosoftStsOAuth2Strategy
        extends OAuth2Strategy
        <MicrosoftStsAccessToken,
                MicrosoftStsAccount,
                MicrosoftStsAuthorizationRequest,
                MicrosoftStsAuthorizationRequest.Builder,
                AuthorizationStrategy,
                MicrosoftStsOAuth2Configuration,
                MicrosoftStsAuthorizationResponse,
                MicrosoftStsRefreshToken,
                MicrosoftStsTokenRequest,
                MicrosoftStsTokenResponse,
                TokenResult,
                AuthorizationResult> {

    private static final String TAG = MicrosoftStsOAuth2Strategy.class.getSimpleName();

    /**
     * Constructor of MicrosoftStsOAuth2Strategy.
     *
     * @param config MicrosoftStsOAuth2Configuration
     */
    public MicrosoftStsOAuth2Strategy(@NonNull final MicrosoftStsOAuth2Configuration config) {
        super(config);
        setTokenEndpoint(config.getTokenEndpoint().toString());
    }

    @Override
    public AuthorizationResultFactory getAuthorizationResultFactory() {
        return new MicrosoftStsAuthorizationResultFactory();
    }

    @Override
    public String getIssuerCacheIdentifier(MicrosoftStsAuthorizationRequest request) {
        final String methodName = ":getIssuerCacheIdentifier";

        final URL authority = request.getAuthority();
        // TODO I don't think this is right... This is probably not the correct authority cache to consult...
        final AzureActiveDirectoryCloud cloudEnv = AzureActiveDirectory.getAzureActiveDirectoryCloud(authority);
        // This map can only be consulted if authority validation is on.
        // If the host has a hardcoded trust, we can just use the hostname.
        if (null != cloudEnv) {
            final String preferredCacheHostName = cloudEnv.getPreferredCacheHostName();
            Logger.info(
                    TAG + methodName,
                    "Using preferred cache host name..."
            );
            Logger.infoPII(
                    TAG + methodName,
                    "Preferred cache hostname: [" + preferredCacheHostName + "]"
            );
            return preferredCacheHostName;
        }
        return authority.getHost();
    }

    @Override
    public MicrosoftStsAccessToken getAccessTokenFromResponse(
            @NonNull final MicrosoftStsTokenResponse response) {
        final String methodName = ":getAccessTokenFromResponse";
        Logger.verbose(
                TAG + methodName,
                "Getting AT from TokenResponse..."
        );
        return new MicrosoftStsAccessToken(response);
    }

    @Override
    public MicrosoftStsRefreshToken getRefreshTokenFromResponse(
            @NonNull final MicrosoftStsTokenResponse response) {
        final String methodName = ":getRefreshTokenFromResponse";
        Logger.verbose(
                TAG + methodName,
                "Getting RT from TokenResponse..."
        );
        return new MicrosoftStsRefreshToken(response);
    }

    @Override
    public MicrosoftStsAccount createAccount(
            @NonNull final MicrosoftStsTokenResponse response) {
        final String methodName = ":createAccount";
        Logger.verbose(
                TAG + methodName,
                "Creating account from TokenResponse..."
        );
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
    public MicrosoftStsAuthorizationRequest.Builder createAuthorizationRequestBuilder() {
        final String methodName = ":createAuthorizationRequestBuilder";
        Logger.verbose(
                TAG + methodName,
                "Creating AuthorizationRequestBuilder..."
        );
        MicrosoftStsAuthorizationRequest.Builder builder = new MicrosoftStsAuthorizationRequest.Builder();
        builder.setAuthority(mConfig.getAuthorityUrl());
        if (mConfig.getSlice() != null) {
            Logger.verbose(
                    TAG + methodName,
                    "Setting slice params..."
            );
            builder.setSlice(mConfig.getSlice());
        }
        builder.setFlightParameters(mConfig.getFlightParameters());
        return builder;
    }

    @Override
    public MicrosoftStsAuthorizationRequest.Builder createAuthorizationRequestBuilder(@Nullable final IAccountRecord account) {
        final String methodName = ":createAuthorizationRequestBuilder";
        Logger.verbose(
                TAG + methodName,
                "Creating AuthorizationRequestBuilder"
        );
        final MicrosoftStsAuthorizationRequest.Builder builder = createAuthorizationRequestBuilder();

        if (null != account) {
            final String homeAccountId = account.getHomeAccountId();

            // Split this value by its parts... <uid>.<utid>
            final int EXPECTED_LENGTH = 2;
            final int INDEX_UID = 0;
            final int INDEX_UTID = 1;

            final String[] uidUtidPair = homeAccountId.split("\\.");

            if (EXPECTED_LENGTH == uidUtidPair.length
                    && !StringExtensions.isNullOrBlank(uidUtidPair[INDEX_UID])
                    && !StringExtensions.isNullOrBlank(uidUtidPair[INDEX_UTID])) {
                builder.setUid(uidUtidPair[INDEX_UID]);
                builder.setUtid(uidUtidPair[INDEX_UTID]);

                Logger.verbosePII(
                        TAG + methodName,
                        "Builder w/ uid: [" + uidUtidPair[INDEX_UID] + "]"
                );

                Logger.verbosePII(
                        TAG + methodName,
                        "Builder w/ utid: [" + uidUtidPair[INDEX_UTID] + "]"
                );
            }
        }

        return builder;
    }

    @Override
    public MicrosoftStsTokenRequest createTokenRequest(@NonNull final MicrosoftStsAuthorizationRequest request,
                                                       @NonNull final MicrosoftStsAuthorizationResponse response) {
        final String methodName = ":createTokenRequest";
        Logger.verbose(
                TAG + methodName,
                "Creating TokenRequest..."
        );
        MicrosoftStsTokenRequest tokenRequest = new MicrosoftStsTokenRequest();
        tokenRequest.setCodeVerifier(request.getPkceChallenge().getCodeVerifier());
        tokenRequest.setCode(response.getCode());
        tokenRequest.setRedirectUri(request.getRedirectUri());
        tokenRequest.setClientId(request.getClientId());
        try {
            tokenRequest.setCorrelationId(UUID.fromString(DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID)));
        } catch (IllegalArgumentException ex) {
            //We're not setting the correlation id if we can't parse it from the diagnostic context
            Logger.error("MicrosoftSTSOAuth2Strategy", "Correlation id on diagnostic context is not a UUID.", ex);
        }
        return tokenRequest;
    }

    @Override
    public MicrosoftStsTokenRequest createRefreshTokenRequest(@NonNull final RefreshTokenRecord refreshToken,
                                                              @NonNull final List<String> scopes) {
        final String methodName = ":createRefreshTokenRequest";
        Logger.verbose(
                TAG + methodName,
                "Creating refresh token request"
        );

        final MicrosoftStsTokenRequest request = new MicrosoftStsTokenRequest();
        request.setRefreshToken(refreshToken.getSecret());
        request.setGrantType(TokenRequest.GrantTypes.REFRESH_TOKEN);
        request.setScope(StringUtil.join(' ', scopes));

        if (null != request.getScope()) {
            Logger.verbosePII(
                    TAG + methodName,
                    "Scopes: [" + request.getScope() + "]"
            );
        }

        return request;
    }

    @Override
    protected void validateAuthorizationRequest(final MicrosoftStsAuthorizationRequest request) {
        // TODO implement

    }

    @Override
    protected void validateTokenRequest(MicrosoftStsTokenRequest request) {

    }

    @Override
    protected TokenResult getTokenResultFromHttpResponse(final HttpResponse response) {
        final String methodName = ":getTokenResultFromHttpResponse";
        Logger.verbose(
                TAG + methodName,
                "Getting TokenResult from HttpResponse..."
        );
        TokenResponse tokenResponse = null;
        TokenErrorResponse tokenErrorResponse = null;

        if (response.getStatusCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
            //An error occurred
            tokenErrorResponse = ObjectMapper.deserializeJsonStringToObject(response.getBody(), MicrosoftTokenErrorResponse.class);
        } else {
            tokenResponse = ObjectMapper.deserializeJsonStringToObject(response.getBody(), MicrosoftStsTokenResponse.class);
        }

        return new TokenResult(tokenResponse, tokenErrorResponse);
    }

    @Override
    public boolean supportsOIDC() {
        return true;
    }

}
