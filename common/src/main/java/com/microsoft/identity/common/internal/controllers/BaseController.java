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

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Intent;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.net.HttpWebRequest;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.cache.SchemaUtil;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.dto.IdTokenRecord;
import com.microsoft.identity.common.internal.logging.DiagnosticContext;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.net.ObjectMapper;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftTokenRequest;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftTokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.IResult;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.OpenIdConnectPromptParameter;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.common.internal.request.AcquireTokenOperationParameters;
import com.microsoft.identity.common.internal.request.AcquireTokenSilentOperationParameters;
import com.microsoft.identity.common.internal.request.OperationParameters;
import com.microsoft.identity.common.internal.request.SdkType;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;
import com.microsoft.identity.common.internal.result.LocalAuthenticationResult;
import com.microsoft.identity.common.internal.telemetry.CliTelemInfo;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public abstract class BaseController {

    private static final String TAG = BaseController.class.getSimpleName();

    public abstract AcquireTokenResult acquireToken(final AcquireTokenOperationParameters request)
            throws ExecutionException, InterruptedException, BaseException, IOException;

    public abstract void completeAcquireToken(
            final int requestCode,
            final int resultCode,
            final Intent data
    );

    public abstract AcquireTokenResult acquireTokenSilent(final AcquireTokenSilentOperationParameters request)
            throws IOException, BaseException;

    public abstract List<AccountRecord> getAccounts(final OperationParameters parameters) throws ClientException, InterruptedException, ExecutionException, RemoteException, OperationCanceledException, IOException, AuthenticatorException;

    public abstract boolean removeAccount(final OperationParameters parameters) throws BaseException, InterruptedException, ExecutionException, RemoteException;

    /**
     * Pre-filled ALL the fields in AuthorizationRequest.Builder
     */
    protected final AuthorizationRequest.Builder initializeAuthorizationRequestBuilder(@NonNull final AuthorizationRequest.Builder builder,
                                                                                       @NonNull final OperationParameters parameters) {
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

            // We don't want to show the SELECT_ACCOUNT page if login_hint is set.
            if (!StringExtensions.isNullOrBlank(acquireTokenOperationParameters.getLoginHint()) &&
                    acquireTokenOperationParameters.getOpenIdConnectPromptParameter() == OpenIdConnectPromptParameter.SELECT_ACCOUNT) {
                builder.setPrompt(null);
            }

            // Set the multipleCloudAware and slice fields.
            if (acquireTokenOperationParameters.getAuthority() instanceof AzureActiveDirectoryAuthority) {
                final AzureActiveDirectoryAuthority requestAuthority = (AzureActiveDirectoryAuthority) acquireTokenOperationParameters.getAuthority();
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

    protected TokenResult performTokenRequest(@NonNull final OAuth2Strategy strategy,
                                              @NonNull final AuthorizationRequest request,
                                              @NonNull final AuthorizationResponse response,
                                              @NonNull final AcquireTokenOperationParameters parameters)
            throws IOException, ClientException {
        final String methodName = ":performTokenRequest";
        HttpWebRequest.throwIfNetworkNotAvailable(parameters.getAppContext());

        TokenRequest tokenRequest = strategy.createTokenRequest(request, response);
        logExposedFieldsOfObject(TAG + methodName, tokenRequest);
        tokenRequest.setGrantType(TokenRequest.GrantTypes.AUTHORIZATION_CODE);

        TokenResult tokenResult = strategy.requestToken(tokenRequest);

        logResult(TAG, tokenResult);

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

        logParameters(TAG, parameters);

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
            final LocalAuthenticationResult authenticationResult = new LocalAuthenticationResult(
                    savedRecord,
                    parameters.getSdkType()
            );

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
     *
     * @param tag    The log tag to use.
     * @param result The result object to log.
     */
    public static void logResult(@NonNull final String tag,
                                 @NonNull final IResult result) {
        final String TAG = tag + ":" + result.getClass().getSimpleName();

        if (result.getSuccess()) {
            Logger.verbose(
                    TAG,
                    "Success Result"
            );

            logExposedFieldsOfObject(TAG, result.getSuccessResponse());
        } else {
            Logger.warn(
                    TAG,
                    "Failure Result"
            );

            if (result.getErrorResponse() != null) {
                if (result.getErrorResponse().getError() != null) {
                    Logger.warn(
                            TAG,
                            "Error: " + result.getErrorResponse().getError()
                    );
                }

                if (result.getErrorResponse().getErrorDescription() != null) {
                    Logger.warnPII(
                            TAG,
                            "Description: " + result.getErrorResponse().getErrorDescription()
                    );
                }

                logExposedFieldsOfObject(TAG, result.getErrorResponse());
            }
        }

        if (result instanceof AuthorizationResult) {
            AuthorizationResult authResult = (AuthorizationResult) result;

            if (authResult.getAuthorizationStatus() != null) {
                Logger.verbose(
                        TAG,
                        "Authorization Status: " + authResult.getAuthorizationStatus().toString()
                );
            }
        }
    }

    /**
     * Log parameters objects passed to controllers
     *
     * @param tag        The log tag to use.
     * @param parameters The request parameters to log.
     */
    protected void logParameters(String tag, Object parameters) {
        final String TAG = tag + ":" + parameters.getClass().getSimpleName();

        if (Logger.getAllowPii()) {
            Logger.verbosePII(TAG, ObjectMapper.serializeObjectToJsonString(parameters));
        } else {
            Logger.verbose(TAG, ObjectMapper.serializeExposedFieldsOfObjectToJsonString(parameters));
        }
    }

    protected static void logExposedFieldsOfObject(@NonNull final String tag,
                                                   @NonNull final Object object) {
        final String TAG = tag + ":" + object.getClass().getSimpleName();
        Logger.verbose(TAG, ObjectMapper.serializeExposedFieldsOfObjectToJsonString(object));
    }

    protected TokenResult performSilentTokenRequest(
            @NonNull final OAuth2Strategy strategy,
            @NonNull final AcquireTokenSilentOperationParameters parameters)
            throws ClientException, IOException {
        final String methodName = ":performSilentTokenRequest";

        Logger.verbose(
                TAG + methodName,
                "Requesting tokens..."
        );

        HttpWebRequest.throwIfNetworkNotAvailable(parameters.getAppContext());

        // Check that the authority is known
        final Authority.KnownAuthorityResult authorityResult =
                Authority.getKnownAuthorityResult(parameters.getAuthority());

        if (!authorityResult.getKnown()) {
            throw authorityResult.getClientException();
        }

        final TokenRequest refreshTokenRequest = strategy.createRefreshTokenRequest();
        refreshTokenRequest.setClientId(parameters.getClientId());
        refreshTokenRequest.setScope(TextUtils.join(" ", parameters.getScopes()));
        refreshTokenRequest.setRefreshToken(parameters.getRefreshToken().getSecret());
        refreshTokenRequest.setRedirectUri(parameters.getRedirectUri());

        if (refreshTokenRequest instanceof MicrosoftTokenRequest) {
            ((MicrosoftTokenRequest)refreshTokenRequest).setClaims(parameters.getClaimsRequestJson());
        }

        //NOTE: this should be moved to the strategy; however requires a larger refactor
        if (parameters.getSdkType() == SdkType.ADAL) {
            ((MicrosoftTokenRequest) refreshTokenRequest).setIdTokenVersion("1");
        }

        if (!StringExtensions.isNullOrBlank(refreshTokenRequest.getScope())) {
            Logger.verbosePII(
                    TAG + methodName,
                    "Scopes: [" + refreshTokenRequest.getScope() + "]"
            );
        }

        return strategy.requestToken(refreshTokenRequest);
    }

    protected ICacheRecord saveTokens(@NonNull final OAuth2Strategy strategy,
                                      @NonNull final AuthorizationRequest request,
                                      @NonNull final TokenResponse tokenResponse,
                                      @NonNull final OAuth2TokenCache tokenCache) throws ClientException {
        final String methodName = ":saveTokens";

        Logger.verbose(
                TAG + methodName,
                "Saving tokens..."
        );

        return tokenCache.save(strategy, request, tokenResponse);
    }

    protected boolean refreshTokenIsNull(@NonNull final ICacheRecord cacheRecord) {
        return null == cacheRecord.getRefreshToken();
    }

    protected boolean accessTokenIsNull(@NonNull final ICacheRecord cacheRecord) {
        return null == cacheRecord.getAccessToken();
    }

    protected boolean idTokenIsNull(@NonNull final ICacheRecord cacheRecord,
                                    @NonNull final SdkType sdkType) {
        final IdTokenRecord idTokenRecord = (sdkType == SdkType.ADAL) ?
                cacheRecord.getV1IdToken() : cacheRecord.getIdToken();

        return null == idTokenRecord;
    }

    protected void addDefaultScopes(@NonNull final OperationParameters operationParameters) {
        final Set<String> requestScopes = operationParameters.getScopes();
        requestScopes.add(AuthenticationConstants.OAuth2Scopes.OPEN_ID_SCOPE);
        requestScopes.add(AuthenticationConstants.OAuth2Scopes.OFFLINE_ACCESS_SCOPE);
        requestScopes.add(AuthenticationConstants.OAuth2Scopes.PROFILE_SCOPE);
        // sanitize empty and null scopes
        requestScopes.removeAll(Arrays.asList("", null));
        operationParameters.setScopes(requestScopes);
    }

    /**
     * Helper method to get a cached account
     *
     * @param parameters
     * @return
     */
    protected AccountRecord getCachedAccountRecord(
            @NonNull final AcquireTokenSilentOperationParameters parameters) throws ClientException {
        if (parameters.getAccount() == null) {
            throw new ClientException(
                    ErrorStrings.NO_ACCOUNT_FOUND,
                    "No cached accounts found for the supplied homeAccountId and clientId"
            );
        }

        final String clientId = parameters.getClientId();
        final String homeAccountId = parameters.getAccount().getHomeAccountId();
        final String localAccountId = parameters.getAccount().getLocalAccountId();

        final AccountRecord targetAccount =
                parameters
                        .getTokenCache()
                        .getAccountWithLocalAccountId(
                                null,
                                clientId,
                                localAccountId
                        );

        if (null == targetAccount) {
            Logger.info(
                    TAG,
                    "No accounts found for clientId ["
                            + clientId
                            + ", "
                            + "]",
                    null
            );
            Logger.errorPII(
                    TAG,
                    "No accounts found for clientId, homeAccountId: ["
                            + clientId
                            + ", "
                            + homeAccountId
                            + "]",
                    null
            );
            throw new ClientException(
                    ErrorStrings.NO_ACCOUNT_FOUND,
                    "No cached accounts found for the supplied homeAccountId"
            );
        }

        return targetAccount;
    }

    protected boolean isMsaAccount(final MicrosoftTokenResponse microsoftTokenResponse){
        final String tenantId = SchemaUtil.getTenantId(
                microsoftTokenResponse.getClientInfo(),
                microsoftTokenResponse.getIdToken()
        );
        return AzureActiveDirectoryAudience.MSA_MEGA_TENANT_ID.equalsIgnoreCase(tenantId);
    }

}
