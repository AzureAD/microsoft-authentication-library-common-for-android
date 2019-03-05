//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.internal.controllers;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ArgumentException;
import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.exception.UiRequiredException;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.cache.SchemaUtil;
import com.microsoft.identity.common.internal.dto.AccessTokenRecord;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.logging.DiagnosticContext;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.net.ObjectMapper;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.ClientInfo;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.IResult;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.common.internal.request.AcquireTokenOperationParameters;
import com.microsoft.identity.common.internal.request.AcquireTokenSilentOperationParameters;
import com.microsoft.identity.common.internal.request.OperationParameters;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;
import com.microsoft.identity.common.internal.result.LocalAuthenticationResult;
import com.microsoft.identity.common.internal.telemetry.CliTelemInfo;
import com.microsoft.identity.common.internal.util.DateUtilities;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public abstract class BaseController {

    private static final String TAG = BaseController.class.getSimpleName();

    public abstract AcquireTokenResult acquireToken(final AcquireTokenOperationParameters request)
            throws ExecutionException, InterruptedException, ClientException, IOException, ArgumentException, ServiceException;

    public abstract void completeAcquireToken(final int requestCode, final int resultCode, final Intent data);

    public abstract AcquireTokenResult acquireTokenSilent(final AcquireTokenSilentOperationParameters request)
            throws IOException, ClientException, UiRequiredException, ArgumentException, ServiceException;

    protected void throwIfNetworkNotAvailable(final Context context) throws ClientException {
        final String methodName = ":throwIfNetworkNotAvailable";
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (networkInfo == null || !networkInfo.isConnected()) {
            throw new ClientException(
                    ClientException.DEVICE_NETWORK_NOT_AVAILABLE,
                    "Device network connection is not available."
            );
        }

        Logger.info(
                TAG + methodName,
                "Network status: connected"
        );
    }

    /**
     * Pre-filled ALL the fields in AuthorizationRequest.Builder
     */
    protected final AuthorizationRequest.Builder initializeAuthorizationRequestBuilder(@NonNull final AuthorizationRequest.Builder builder,
                                                                                        @NonNull final OperationParameters parameters){
        UUID correlationId = null;

        try {
            correlationId = UUID.fromString(DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID));
        } catch (IllegalArgumentException ex) {
            Logger.error(TAG, "correlation id from diagnostic context is not a UUID", ex);
        }

        builder.setClientId(parameters.getClientId())
                .setRedirectUri(parameters.getRedirectUri())
                .setCorrelationId(correlationId);

        if (parameters instanceof AcquireTokenOperationParameters) {
            AcquireTokenOperationParameters acquireTokenOperationParameters = (AcquireTokenOperationParameters) parameters;
            if (acquireTokenOperationParameters.getExtraScopesToConsent() != null) {
                parameters.getScopes().addAll(acquireTokenOperationParameters.getExtraScopesToConsent());
            }

            // Add additional fields to the AuthorizationRequest.Builder to support interactive
            builder.setLoginHint(
                    acquireTokenOperationParameters.getLoginHint()
            ).setExtraQueryParams(
                    acquireTokenOperationParameters.getExtraQueryStringParameters()
            ).setPrompt(
                    acquireTokenOperationParameters.getOpenIdConnectPromptParameter().toString()
            ).setClaims(
                    parameters.getClaimsRequestJson()
            ).setRequestHeaders(
                    acquireTokenOperationParameters.getRequestHeaders()
            );

            // Set the multipleCloudAware and slice fields.
            if (acquireTokenOperationParameters.getAuthority() instanceof AzureActiveDirectoryAuthority) {
                final AzureActiveDirectoryAuthority requestAuthority = (AzureActiveDirectoryAuthority)acquireTokenOperationParameters.getAuthority();
                ((MicrosoftAuthorizationRequest.Builder) builder)
                    .setAuthority(requestAuthority.getAuthorityURL())
                    .setMultipleCloudAware(requestAuthority.mMultipleCloudsSupported)
                    .setSlice(requestAuthority.mSlice);
            }
        }

        builder.setScope(TextUtils.join(" ", parameters.getScopes()));

        return builder;
    }

    protected AuthorizationRequest getAuthorizationRequest(@NonNull final OAuth2Strategy strategy,
                                                           @NonNull final OperationParameters parameters) {
        AuthorizationRequest.Builder builder = strategy.createAuthorizationRequestBuilder(parameters.getAccount());
        initializeAuthorizationRequestBuilder(builder, parameters);
        return builder.build();
    }

    protected TokenResult performTokenRequest(final OAuth2Strategy strategy,
                                              final AuthorizationRequest request,
                                              final AuthorizationResponse response,
                                              final AcquireTokenOperationParameters parameters)
            throws IOException, ClientException {
        throwIfNetworkNotAvailable(parameters.getAppContext());

        TokenRequest tokenRequest = strategy.createTokenRequest(request, response);
        tokenRequest.setGrantType(TokenRequest.GrantTypes.AUTHORIZATION_CODE);

        TokenResult tokenResult = null;

        tokenResult = strategy.requestToken(tokenRequest);


        return tokenResult;
    }

    protected void renewAccessToken(@NonNull final AcquireTokenSilentOperationParameters parameters,
                                    @NonNull final AcquireTokenResult acquireTokenSilentResult,
                                    @NonNull final OAuth2TokenCache tokenCache,
                                    @NonNull final OAuth2Strategy strategy,
                                    @NonNull final ICacheRecord cacheRecord)
            throws IOException, ClientException {
        final String methodName = ":renewAccessToken";
        Logger.verbose(
                TAG + methodName,
                "Renewing access token..."
        );
        parameters.setRefreshToken(cacheRecord.getRefreshToken());

        final TokenResult tokenResult = performSilentTokenRequest(strategy, parameters);
        acquireTokenSilentResult.setTokenResult(tokenResult);

        logResult(TAG + methodName, tokenResult);

        if (tokenResult.getSuccess()) {
            Logger.verbose(
                    TAG + methodName,
                    "Token request was successful"
            );
            final ICacheRecord savedRecord = tokenCache.save(
                    strategy,
                    getAuthorizationRequest(strategy, parameters),
                    tokenResult.getTokenResponse()
            );

            // Create a new AuthenticationResult to hold the saved record
            final LocalAuthenticationResult authenticationResult = new LocalAuthenticationResult(savedRecord);

            // Set the client telemetry...
            if (null != tokenResult.getCliTelemInfo()) {
                final CliTelemInfo cliTelemInfo = tokenResult.getCliTelemInfo();
                authenticationResult.setSpeRing(cliTelemInfo.getSpeRing());
                authenticationResult.setRefreshTokenAge(cliTelemInfo.getRefreshTokenAge());
            }

            // Set the AuthenticationResult on the final result object
            acquireTokenSilentResult.setLocalAuthenticationResult(authenticationResult);
        }
    }

    /**
     * Log IResult objects.  IResult objects are returned from Authorization and Token Requests
     * @param tag
     * @param result
     */
    protected void logResult(String tag, IResult result){

        final String TAG = tag + ":" + result.getClass().getSimpleName();

        if(result.getSuccess()) {
            Logger.verbose(
                    TAG,
                    "Success Result"
            );
        }else{
            Logger.warn(
                    TAG,
                    "Failure Result"
            );
            if(result.getErrorResponse() != null) {
                if(result.getErrorResponse().getError() != null) {
                    Logger.warn(
                            TAG,
                            "Error: " + result.getErrorResponse().getError()
                    );
                }
                if(result.getErrorResponse().getErrorDescription() != null) {
                    Logger.warnPII(
                            TAG,
                            "Description: " + result.getErrorResponse().getErrorDescription()
                    );
                }
            }
        }

        if(result instanceof AuthorizationResult){
            AuthorizationResult authResult = (AuthorizationResult)result;
            if(authResult.getAuthorizationStatus() != null) {
                Logger.verbose(
                        TAG,
                        "Authorization Status: " + authResult.getAuthorizationStatus().toString()
                );
            }
        }
    }

    /**
     * Log parameters objects passed to controllers
     * @param tag
     * @param parameters
     */
    protected void logParameters(String tag, Object parameters){
        final String TAG = tag + ":" + parameters.getClass().getSimpleName();
        Logger.verbosePII(TAG, ObjectMapper.serializeObjectToJsonString(parameters));
    }

    protected TokenResult performSilentTokenRequest(
            final OAuth2Strategy strategy,
            final AcquireTokenSilentOperationParameters parameters)
            throws ClientException, IOException {

        final String methodName = ":performSilentTokenRequest";

        Logger.verbose(
                TAG + methodName,
                "Requesting tokens..."
        );

        throwIfNetworkNotAvailable(parameters.getAppContext());

        // Check that the authority is known
        Authority.KnownAuthorityResult authorityResult = Authority.getKnownAuthorityResult(parameters.getAuthority());

        if (!authorityResult.getKnown()) {
            throw authorityResult.getClientException();
        }

        final TokenRequest refreshTokenRequest = strategy.createRefreshTokenRequest();
        refreshTokenRequest.setClientId(parameters.getClientId());
        refreshTokenRequest.setScope(TextUtils.join(" ", parameters.getScopes()));
        refreshTokenRequest.setRefreshToken(parameters.getRefreshToken().getSecret());
        refreshTokenRequest.setRedirectUri(parameters.getRedirectUri());

        if (!StringExtensions.isNullOrBlank(refreshTokenRequest.getScope())) {
            Logger.verbosePII(
                    TAG + methodName,
                    "Scopes: [" + refreshTokenRequest.getScope() + "]"
            );
        }

        return strategy.requestToken(refreshTokenRequest);
    }

    protected ICacheRecord saveTokens(final OAuth2Strategy strategy,
                                      final AuthorizationRequest request,
                                      final TokenResponse tokenResponse,
                                      final OAuth2TokenCache tokenCache) throws ClientException {
        final String methodName = ":saveTokens";
        Logger.verbose(
                TAG + methodName,
                "Saving tokens..."
        );
        return tokenCache.save(strategy, request, tokenResponse);
    }

    protected boolean refreshTokenIsNull(ICacheRecord cacheRecord) {
        return null == cacheRecord.getRefreshToken();
    }

    protected boolean accessTokenIsNull(ICacheRecord cacheRecord) {
        return null == cacheRecord.getAccessToken();
    }

    protected void addDefaultScopes(final OperationParameters operationParameters){
        final Set<String> requestScopes = operationParameters.getScopes();
        requestScopes.add(AuthenticationConstants.OAuth2Scopes.OPEN_ID_SCOPE);
        requestScopes.add(AuthenticationConstants.OAuth2Scopes.OFFLINE_ACCESS_SCOPE);
        requestScopes.add(AuthenticationConstants.OAuth2Scopes.PROFILE_SCOPE);
        // sanitize empty and null scopes
        requestScopes.removeAll(Arrays.asList("", null));
        operationParameters.setScopes(requestScopes);
    }

    public static AccessTokenRecord getAccessTokenRecord(@NonNull final MicrosoftStsTokenResponse tokenResponse,
                                                         @NonNull final OperationParameters requestParameters) {
        final String methodName = ":getAccessTokenRecord";

        final AccessTokenRecord accessTokenRecord = new AccessTokenRecord();

        try {
            final ClientInfo clientInfo = new ClientInfo(tokenResponse.getClientInfo());
            accessTokenRecord.setHomeAccountId(SchemaUtil.getHomeAccountId(clientInfo));
            accessTokenRecord.setRealm(clientInfo.getUtid());
            final AzureActiveDirectoryCloud cloudEnv = AzureActiveDirectory.
                    getAzureActiveDirectoryCloud(
                            requestParameters.getAuthority().getAuthorityURL()
                    );
            if (cloudEnv != null) {
                Logger.info(TAG, "Using preferred cache host name...");
                accessTokenRecord.setEnvironment(cloudEnv.getPreferredCacheHostName());
            } else {
                accessTokenRecord.setEnvironment(
                        requestParameters.getAuthority().getAuthorityURL().getHost()
                );
            }
        } catch (final ServiceException e) {
            Logger.error(TAG + methodName, "ClientInfo construction failed ", e);
        }
        accessTokenRecord.setClientId(requestParameters.getClientId());
        accessTokenRecord.setSecret(tokenResponse.getAccessToken());
        accessTokenRecord.setAccessTokenType(tokenResponse.getTokenType());
        accessTokenRecord.setAuthority(
                requestParameters.getAuthority().getAuthorityURL().toString()
        );
        accessTokenRecord.setTarget(TextUtils.join(" ", requestParameters.getScopes()));
        accessTokenRecord.setCredentialType(CredentialType.AccessToken.name());
        accessTokenRecord.setExpiresOn(
                String.valueOf(DateUtilities.getExpiresOn(tokenResponse.getExpiresIn()))
        );
        accessTokenRecord.setExtendedExpiresOn(
                String.valueOf(DateUtilities.getExpiresOn(tokenResponse.getExtExpiresIn()))
        );
        accessTokenRecord.setCachedAt(
                String.valueOf(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()))
        );
        return accessTokenRecord;
    }

}
