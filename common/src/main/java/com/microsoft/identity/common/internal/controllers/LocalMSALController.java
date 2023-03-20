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

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.microsoft.identity.common.java.configuration.LibraryConfiguration;
import com.microsoft.identity.common.java.controllers.CommandDispatcher;
import com.microsoft.identity.common.java.eststelemetry.PublicApiId;
import com.microsoft.identity.common.java.exception.ArgumentException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.internal.commands.RefreshOnCommand;
import com.microsoft.identity.common.java.commands.parameters.DeviceCodeFlowCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.GenerateShrCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.RemoveAccountCommandParameters;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.exception.UiRequiredException;
import com.microsoft.identity.common.java.platform.DevicePoPUtils;
import com.microsoft.identity.common.java.constants.OAuth2ErrorCode;
import com.microsoft.identity.common.java.controllers.BaseController;
import com.microsoft.identity.common.java.result.AcquireTokenResult;
import com.microsoft.identity.common.java.result.GenerateShrResult;
import com.microsoft.identity.common.java.result.LocalAuthenticationResult;
import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.telemetry.events.ApiEndEvent;
import com.microsoft.identity.common.internal.telemetry.events.ApiStartEvent;
import com.microsoft.identity.common.java.util.ThreadUtils;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.java.authscheme.IPoPAuthenticationSchemeParams;
import com.microsoft.identity.common.java.commands.parameters.CommandParameters;
import com.microsoft.identity.common.java.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.java.providers.RawAuthorizationResult;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResponse;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenRequest;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationStatus;
import com.microsoft.identity.common.java.providers.oauth2.IAuthorizationStrategy;
import com.microsoft.identity.common.java.providers.oauth2.IResult;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;
import com.microsoft.identity.common.java.request.SdkType;
import com.microsoft.identity.common.java.telemetry.TelemetryEventStrings;
import com.microsoft.identity.common.java.util.ported.PropertyBag;
import com.microsoft.identity.common.java.util.ResultUtil;
import com.microsoft.identity.common.logging.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class LocalMSALController extends BaseController {

    private static final String TAG = LocalMSALController.class.getSimpleName();

    @SuppressWarnings(WarningType.rawtype_warning)
    private IAuthorizationStrategy mAuthorizationStrategy = null;

    @SuppressWarnings(WarningType.rawtype_warning)
    private AuthorizationRequest mAuthorizationRequest = null;

    @Override
    public AcquireTokenResult acquireToken(
            @NonNull final InteractiveTokenCommandParameters parameters)
            throws ExecutionException, InterruptedException, ClientException, IOException, ArgumentException {
        final String methodTag = TAG + ":acquireToken";

        Logger.info(
                methodTag,
                "Acquiring token..."
        );

        Telemetry.emit(
                new ApiStartEvent()
                        .putProperties(parameters)
                        .putApiId(TelemetryEventStrings.Api.LOCAL_ACQUIRE_TOKEN_INTERACTIVE)
        );

        final AcquireTokenResult acquireTokenResult = new AcquireTokenResult();

        //00) Validate MSAL Parameters
        parameters.validate();

        // Add default scopes
        final Set<String> mergedScopes = addDefaultScopes(parameters);

        final InteractiveTokenCommandParameters parametersWithScopes = parameters
                .toBuilder()
                .scopes(mergedScopes)
                .build();

        logParameters(TAG, parametersWithScopes);

        //0) Get known authority result
        parametersWithScopes.getPlatformComponents()
                .getPlatformUtil()
                .throwIfNetworkNotAvailable(parametersWithScopes.isPowerOptCheckEnabled());

        Authority.KnownAuthorityResult authorityResult = Authority.getKnownAuthorityResult(parametersWithScopes.getAuthority());

        //0.1 If not known throw resulting exception
        if (!authorityResult.getKnown()) {
            Telemetry.emit(
                    new ApiEndEvent()
                            .putException(authorityResult.getClientException())
                            .putApiId(TelemetryEventStrings.Api.LOCAL_ACQUIRE_TOKEN_INTERACTIVE)
            );

            throw authorityResult.getClientException();
        }

        // Build up params for Strategy construction
        final OAuth2StrategyParameters strategyParameters = OAuth2StrategyParameters.builder()
                .platformComponents(parameters.getPlatformComponents())
                .build();

        //1) Get oAuth2Strategy for Authority Type
        @SuppressWarnings(WarningType.rawtype_warning) final OAuth2Strategy oAuth2Strategy = parametersWithScopes
                .getAuthority()
                .createOAuth2Strategy(strategyParameters);


        //2) Request authorization interactively
        @SuppressWarnings(WarningType.rawtype_warning) final AuthorizationResult result = performAuthorizationRequest(
                oAuth2Strategy,
                parametersWithScopes
        );
        acquireTokenResult.setAuthorizationResult(result);

        ResultUtil.logResult(TAG, result);

        if (result.getAuthorizationStatus().equals(AuthorizationStatus.SUCCESS)) {
            //3) Exchange authorization code for token
            final TokenResult tokenResult = performTokenRequest(
                    oAuth2Strategy,
                    mAuthorizationRequest,
                    result.getAuthorizationResponse(),
                    parametersWithScopes
            );

            acquireTokenResult.setTokenResult(tokenResult);

            if (tokenResult != null && tokenResult.getSuccess()) {
                //4) Save tokens in token cache
                final List<ICacheRecord> records = saveTokens(
                        oAuth2Strategy,
                        mAuthorizationRequest,
                        tokenResult.getTokenResponse(),
                        parametersWithScopes.getOAuth2TokenCache()
                );

                // The first element in the returned list is the item we *just* saved, the rest of
                // the elements are necessary to construct the full IAccount + TenantProfile
                final ICacheRecord newestRecord = records.get(0);

                acquireTokenResult.setLocalAuthenticationResult(
                        new LocalAuthenticationResult(
                                finalizeCacheRecordForResult(
                                        newestRecord,
                                        parametersWithScopes.getAuthenticationScheme()
                                ),
                                records,
                                SdkType.MSAL,
                                false
                        )
                );
            }
        }

        Telemetry.emit(
                new ApiEndEvent()
                        .putResult(acquireTokenResult)
                        .putApiId(TelemetryEventStrings.Api.LOCAL_ACQUIRE_TOKEN_INTERACTIVE)
        );

        return acquireTokenResult;
    }

    // Suppressing rawtype warnings due to the generic types AuthorizationResult and OAuth2Strategy
    @SuppressWarnings(WarningType.rawtype_warning)
    private AuthorizationResult performAuthorizationRequest(@NonNull final OAuth2Strategy strategy,
                                                            @NonNull final InteractiveTokenCommandParameters parameters)
            throws ExecutionException, InterruptedException, ClientException {

        parameters.getPlatformComponents()
                .getPlatformUtil()
                .throwIfNetworkNotAvailable(parameters.isPowerOptCheckEnabled());

        parameters.getPlatformComponents().getAuthorizationStrategyFactory();

        mAuthorizationStrategy = parameters.getPlatformComponents().getAuthorizationStrategyFactory().getAuthorizationStrategy(parameters);
        mAuthorizationRequest = getAuthorizationRequest(strategy, parameters);

        // Suppressing unchecked warnings due to casting of AuthorizationRequest to GenericAuthorizationRequest and AuthorizationStrategy to GenericAuthorizationStrategy in the arguments of call to requestAuthorization method
        @SuppressWarnings(WarningType.unchecked_warning) final Future<AuthorizationResult> future = strategy.requestAuthorization(
                mAuthorizationRequest,
                mAuthorizationStrategy
        );

        final AuthorizationResult result = future.get();

        return result;
    }

    @Override
    public void onFinishAuthorizationSession(int requestCode,
                                             int resultCode,
                                             @NonNull final PropertyBag data) {
        final String methodTag = TAG + ":onFinishAuthorizationSession";
        Logger.verbose(
                methodTag,
                "Completing authorization..."
        );

        Telemetry.emit(
                new ApiStartEvent()
                        .putApiId(TelemetryEventStrings.Api.LOCAL_COMPLETE_ACQUIRE_TOKEN_INTERACTIVE)
                        .put(TelemetryEventStrings.Key.RESULT_CODE, String.valueOf(resultCode))
                        .put(TelemetryEventStrings.Key.REQUEST_CODE, String.valueOf(requestCode))
        );

        mAuthorizationStrategy.completeAuthorization(requestCode, RawAuthorizationResult.fromPropertyBag(data));

        Telemetry.emit(
                new ApiEndEvent()
                        .putApiId(TelemetryEventStrings.Api.LOCAL_COMPLETE_ACQUIRE_TOKEN_INTERACTIVE)
        );
    }

    @Override
    public AcquireTokenResult acquireTokenSilent(
            @NonNull final SilentTokenCommandParameters parameters)
            throws IOException, ClientException, ArgumentException, ServiceException {
        final String methodTag = TAG + ":acquireTokenSilent";
        Logger.info(
                methodTag,
                "Acquiring token silently..."
        );

        Telemetry.emit(
                new ApiStartEvent()
                        .putProperties(parameters)
                        .putApiId(TelemetryEventStrings.Api.LOCAL_ACQUIRE_TOKEN_SILENT)
        );

        final AcquireTokenResult acquireTokenSilentResult = new AcquireTokenResult();

        //Validate MSAL Parameters
        parameters.validate();

        // Add default scopes
        final Set<String> mergedScopes = addDefaultScopes(parameters);

        final SilentTokenCommandParameters parametersWithScopes = parameters
                .toBuilder()
                .scopes(mergedScopes)
                .build();

        @SuppressWarnings(WarningType.rawtype_warning) final OAuth2TokenCache tokenCache = parametersWithScopes.getOAuth2TokenCache();

        final AccountRecord targetAccount = getCachedAccountRecord(parametersWithScopes);

        // Build up params for Strategy construction
        final AbstractAuthenticationScheme authScheme = parametersWithScopes.getAuthenticationScheme();
        final OAuth2StrategyParameters strategyParameters = OAuth2StrategyParameters.builder()
                .platformComponents(parameters.getPlatformComponents())
                .build();

        @SuppressWarnings(WarningType.rawtype_warning) final OAuth2Strategy strategy = parametersWithScopes.getAuthority().createOAuth2Strategy(strategyParameters);

        // Suppressing unchecked warning of converting List<ICacheRecord> to List due to generic type not provided for tokenCache
        @SuppressWarnings(WarningType.unchecked_warning) final List<ICacheRecord> cacheRecords = tokenCache.loadWithAggregatedAccountData(
                parametersWithScopes.getClientId(),
                TextUtils.join(" ", parametersWithScopes.getScopes()),
                targetAccount,
                authScheme
        );

        // The first element is the 'fully-loaded' CacheRecord which may contain the AccountRecord,
        // AccessTokenRecord, RefreshTokenRecord, and IdTokenRecord... (if all of those artifacts exist)
        // subsequent CacheRecords represent other profiles (projections) of this principal in
        // other tenants. Those tokens will be 'sparse', meaning that their AT/RT will not be loaded
        final ICacheRecord fullCacheRecord = cacheRecords.get(0);
        if (LibraryConfiguration.getInstance().isRefreshInEnabled()
                && fullCacheRecord.getAccessToken() != null
                && fullCacheRecord.getAccessToken().refreshOnIsActive()) {
            Logger.info(
                    methodTag,
                    "RefreshOn is active. This will extend your token usage in the rare case servers are not available."
            );
        }
        if (LibraryConfiguration.getInstance().isRefreshInEnabled()
                && fullCacheRecord.getAccessToken() != null
                && fullCacheRecord.getAccessToken().shouldRefresh()) {
            if (!fullCacheRecord.getAccessToken().isExpired()) {
                setAcquireTokenResult(acquireTokenSilentResult, parametersWithScopes, cacheRecords);
                final RefreshOnCommand refreshOnCommand = new RefreshOnCommand(parameters, this, PublicApiId.MSAL_REFRESH_ON);
                CommandDispatcher.submitAndForget(refreshOnCommand);
            } else {
                Logger.warn(
                        methodTag,
                        "Access token is expired. Removing from cache..."
                );
                // Remove the expired token
                tokenCache.removeCredential(fullCacheRecord.getAccessToken());
                renewAT(
                        parametersWithScopes,
                        acquireTokenSilentResult,
                        tokenCache,
                        strategy,
                        fullCacheRecord,
                        methodTag
                );
            }
        } else
            if ((accessTokenIsNull(fullCacheRecord)
                || refreshTokenIsNull(fullCacheRecord)
                || parametersWithScopes.isForceRefresh()
                || !isRequestAuthorityRealmSameAsATRealm(parametersWithScopes.getAuthority(), fullCacheRecord.getAccessToken())
                || !strategy.validateCachedResult(authScheme, fullCacheRecord))) {
            if (!refreshTokenIsNull(fullCacheRecord)) {
                // No AT found, but the RT checks out, so we'll use it
                renewAT(
                        parametersWithScopes,
                        acquireTokenSilentResult,
                        tokenCache,
                        strategy,
                        fullCacheRecord,
                        methodTag
                );
            } else {
                final UiRequiredException exception = new UiRequiredException(
                        ErrorStrings.NO_TOKENS_FOUND,
                        "No refresh token was found. "
                );

                Telemetry.emit(
                        new ApiEndEvent()
                                .putException(exception)
                                .putApiId(TelemetryEventStrings.Api.LOCAL_ACQUIRE_TOKEN_SILENT)
                );

                throw exception;
            }
        } else if (fullCacheRecord.getAccessToken().isExpired()) {
            Logger.warn(
                    methodTag,
                    "Access token is expired. Removing from cache..."
            );
            // Remove the expired token
            tokenCache.removeCredential(fullCacheRecord.getAccessToken());
            renewAT(
                    parametersWithScopes,
                    acquireTokenSilentResult,
                    tokenCache,
                    strategy,
                    fullCacheRecord,
                    methodTag
            );

        } else {
            Logger.verbose(
                    methodTag,
                    "Returning silent result"
            );
            setAcquireTokenResult(acquireTokenSilentResult, parametersWithScopes, cacheRecords);
        }

        Telemetry.emit(
                new ApiEndEvent()
                        .putResult(acquireTokenSilentResult)
                        .putApiId(TelemetryEventStrings.Api.LOCAL_ACQUIRE_TOKEN_SILENT)
        );

        return acquireTokenSilentResult;
    }

    private void setAcquireTokenResult(final AcquireTokenResult acquireTokenSilentResult,
                                       final SilentTokenCommandParameters parametersWithScopes,
                                       final List<ICacheRecord> cacheRecords) throws ClientException {
        ICacheRecord fullCacheRecord = cacheRecords.get(0);
        acquireTokenSilentResult.setLocalAuthenticationResult(
                new LocalAuthenticationResult(
                        finalizeCacheRecordForResult(
                                fullCacheRecord,
                                parametersWithScopes.getAuthenticationScheme()
                        ),
                        cacheRecords,
                        SdkType.MSAL,
                        true
                )
        );
    }

    private void renewAT(@NonNull final SilentTokenCommandParameters parametersWithScopes,
                         @NonNull final AcquireTokenResult acquireTokenSilentResult,
                         @SuppressWarnings(WarningType.rawtype_warning) @NonNull final OAuth2TokenCache tokenCache,
                         @SuppressWarnings(WarningType.rawtype_warning) @NonNull final OAuth2Strategy strategy,
                         @NonNull final ICacheRecord cacheRecord,
                         @NonNull final String tag) throws IOException, ClientException, ServiceException {
        Logger.verbose(
                tag,
                "Renewing access token..."
        );
        renewAccessToken(
                parametersWithScopes,
                acquireTokenSilentResult,
                tokenCache,
                strategy,
                cacheRecord
        );
    }

    @Override
    @WorkerThread
    public List<ICacheRecord> getAccounts(@NonNull final CommandParameters parameters) {
        Telemetry.emit(
                new ApiStartEvent()
                        .putProperties(parameters)
                        .putApiId(TelemetryEventStrings.Api.LOCAL_GET_ACCOUNTS)
        );

        Logger.info(TAG, "querying in localMSALController for accounts in cache");
        @SuppressWarnings(WarningType.unchecked_warning) final List<ICacheRecord> accountsInCache =
                parameters
                        .getOAuth2TokenCache()
                        .getAccountsWithAggregatedAccountData(
                                null, // * wildcard
                                parameters.getClientId()
                        );
        Logger.info(TAG, "no. of accounts found in local msal cache "+ accountsInCache.size());
        for (ICacheRecord  cacheRecord : accountsInCache) {
            Logger.info(TAG, "account : "+ cacheRecord.getAccount().getUsername());
        }
        Telemetry.emit(
                new ApiEndEvent()
                        .putApiId(TelemetryEventStrings.Api.LOCAL_GET_ACCOUNTS)
                        .put(TelemetryEventStrings.Key.ACCOUNTS_NUMBER, Integer.toString(accountsInCache.size()))
                        .put(TelemetryEventStrings.Key.IS_SUCCESSFUL, TelemetryEventStrings.Value.TRUE)
        );

        return accountsInCache;
    }

    @Override
    @WorkerThread
    public boolean removeAccount(
            @NonNull final RemoveAccountCommandParameters parameters) {
        Telemetry.emit(
                new ApiStartEvent()
                        .putProperties(parameters)
                        .putApiId(TelemetryEventStrings.Api.LOCAL_REMOVE_ACCOUNT)
        );

        String realm = null;

        if (parameters.getAccount() != null) {
            realm = parameters.getAccount().getRealm();
        }
        Logger.info(TAG, "remove account from MSAL cache called with account==null? "+ parameters.getAccount());
        final boolean localRemoveAccountSuccess = !parameters
                .getOAuth2TokenCache()
                .removeAccount(
                        null, // remove account from all environment
                        parameters.getClientId(),
                        parameters.getAccount() == null ? null : parameters.getAccount().getHomeAccountId(),
                        realm
                ).isEmpty();

        Telemetry.emit(
                new ApiEndEvent()
                        .put(TelemetryEventStrings.Key.IS_SUCCESSFUL, String.valueOf(localRemoveAccountSuccess))
                        .putApiId(TelemetryEventStrings.Api.LOCAL_REMOVE_ACCOUNT)
        );
        Logger.info(TAG, "removed account from MSAL cache success? "+ localRemoveAccountSuccess);
        return localRemoveAccountSuccess;
    }

    @Override
    public boolean getDeviceMode(CommandParameters parameters) throws Exception {
        final String methodTag = TAG + ":getDeviceMode";

        final String errorMessage = "LocalMSALController is not eligible to use the broker. Do not check sharedDevice mode and return false immediately.";
        com.microsoft.identity.common.internal.logging.Logger.warn(methodTag, errorMessage);

        return false;
    }

    @Override
    public List<ICacheRecord> getCurrentAccount(CommandParameters parameters) throws Exception {
        return getAccounts(parameters);
    }

    @Override
    public boolean removeCurrentAccount(RemoveAccountCommandParameters parameters) throws Exception {
        return removeAccount(parameters);
    }

    // Suppressing rawtype warnings due to the generic types AuthorizationResult and OAuth2Strategy
    @SuppressWarnings(WarningType.rawtype_warning)
    @Override
    public AuthorizationResult deviceCodeFlowAuthRequest(final DeviceCodeFlowCommandParameters parameters)
            throws ServiceException, ClientException, IOException {
        // Logging start of method
        final String methodTag = TAG + ":deviceCodeFlowAuthRequest";
        Logger.verbose(
                methodTag,
                "Device Code Flow: Authorizing user code..."
        );

        // Default scopes here
        final Set<String> mergedScopes = addDefaultScopes(parameters);

        final DeviceCodeFlowCommandParameters parametersWithScopes = parameters
                .toBuilder()
                .scopes(mergedScopes)
                .build();

        logParameters(TAG, parametersWithScopes);

        // Start telemetry with LOCAL_DEVICE_CODE_FLOW_ACQUIRE_URL_AND_CODE
        Telemetry.emit(
                new ApiStartEvent()
                        .putProperties(parametersWithScopes)
                        .putApiId(TelemetryEventStrings.Api.LOCAL_DEVICE_CODE_FLOW_ACQUIRE_URL_AND_CODE)
        );

        final Authority.KnownAuthorityResult authorityResult = Authority.getKnownAuthorityResult(parametersWithScopes.getAuthority());

        // If not known throw resulting exception
        if (!authorityResult.getKnown()) {
            Telemetry.emit(
                    new ApiEndEvent()
                            .putException(authorityResult.getClientException())
                            .putApiId(TelemetryEventStrings.Api.LOCAL_DEVICE_CODE_FLOW_ACQUIRE_URL_AND_CODE)
            );

            throw authorityResult.getClientException();
        }

        final AuthorizationResult authorizationResult;

        try {
            // Create OAuth2Strategy using commandParameters and strategyParameters
            final OAuth2StrategyParameters strategyParameters = OAuth2StrategyParameters.builder()
                    .platformComponents(parameters.getPlatformComponents())
                    .build();

            final OAuth2Strategy oAuth2Strategy = parametersWithScopes
                    .getAuthority()
                    .createOAuth2Strategy(strategyParameters);

            // DCF protocol step 1: Get user code
            // Populate global authorization request
            mAuthorizationRequest = getAuthorizationRequest(oAuth2Strategy, parametersWithScopes);

            // Call method defined in oAuth2Strategy to request authorization
            authorizationResult = oAuth2Strategy.getDeviceCode((MicrosoftStsAuthorizationRequest) mAuthorizationRequest);

            validateServiceResult(authorizationResult);

        } catch (Exception error) {
            Telemetry.emit(
                    new ApiEndEvent()
                            .putException(error)
                            .putApiId(TelemetryEventStrings.Api.LOCAL_DEVICE_CODE_FLOW_ACQUIRE_URL_AND_CODE)
            );
            throw error;
        }

        Logger.verbose(
                methodTag,
                "Device Code Flow authorization step finished..."
        );

        ResultUtil.logResult(TAG, authorizationResult);

        // End telemetry with LOCAL_DEVICE_CODE_FLOW_ACQUIRE_URL_AND_CODE
        Telemetry.emit(
                new ApiEndEvent()
                        .putApiId(TelemetryEventStrings.Api.LOCAL_DEVICE_CODE_FLOW_ACQUIRE_URL_AND_CODE)
        );

        return authorizationResult;
    }

    @Override
    public AcquireTokenResult acquireDeviceCodeFlowToken(
            @SuppressWarnings(WarningType.rawtype_warning) final AuthorizationResult authorizationResult,
            final DeviceCodeFlowCommandParameters parameters)
            throws ServiceException, ClientException, IOException {

        // Logging start of method
        final String methodTag = TAG + ":acquireDeviceCodeFlowToken";
        Logger.verbose(
                methodTag,
                "Device Code Flow: Polling for token..."
        );

        // Start telemetry with LOCAL_DEVICE_CODE_FLOW_POLLING
        Telemetry.emit(
                new ApiStartEvent()
                        .putApiId(TelemetryEventStrings.Api.LOCAL_DEVICE_CODE_FLOW_POLLING)
        );

        // Create empty AcquireTokenResult object
        final AcquireTokenResult acquireTokenResult = new AcquireTokenResult();

        // Assign authorization result
        acquireTokenResult.setAuthorizationResult(authorizationResult);

        // Fetch the Authorization Response
        final MicrosoftStsAuthorizationResponse authorizationResponse = (MicrosoftStsAuthorizationResponse) authorizationResult.getAuthorizationResponse();

        // DCF protocol step 2: Poll for token
        TokenResult tokenResult = null;

        try {
            // Create OAuth2Strategy using commandParameters and strategyParameters
            final OAuth2StrategyParameters strategyParameters = OAuth2StrategyParameters.builder()
                    .platformComponents(parameters.getPlatformComponents())
                    .build();

            @SuppressWarnings(WarningType.rawtype_warning) final OAuth2Strategy oAuth2Strategy = parameters
                    .getAuthority()
                    .createOAuth2Strategy(strategyParameters);

            // Create token request outside of loop so it isn't re-created after every loop
            // Suppressing unchecked warnings due to casting of AuthorizationRequest to GenericAuthorizationRequest and MicrosoftStsAuthorizationResponse to GenericAuthorizationResponse in the arguments of call to createTokenRequest method
            @SuppressWarnings(WarningType.unchecked_warning) final MicrosoftStsTokenRequest tokenRequest = (MicrosoftStsTokenRequest) oAuth2Strategy.createTokenRequest(
                    mAuthorizationRequest,
                    authorizationResponse,
                    parameters.getAuthenticationScheme()
            );

            // Fetch wait interval
            final int intervalInMilliseconds = Integer.parseInt(authorizationResponse.getInterval()) * 1000;

            String errorCode = ErrorStrings.DEVICE_CODE_FLOW_AUTHORIZATION_PENDING_ERROR_CODE;

            // Loop to send multiple requests checking for token
            while (authorizationPending(errorCode)) {

                // Wait between polls
                ThreadUtils.sleepSafely(intervalInMilliseconds, TAG,
                        "Attempting to sleep thread during Device Code Flow token polling...");

                errorCode = ""; // Reset error code

                // Execute Token Request
                // Suppressing unchecked warnings due to casting of MicrosoftStsTokenRequest to GenericTokenRequest in the arguments of call to requestToken method
                @SuppressWarnings(WarningType.unchecked_warning)
                TokenResult tokenResultFromRequestToken = oAuth2Strategy.requestToken(tokenRequest);

                tokenResult = tokenResultFromRequestToken;

                // Fetch error if the request failed
                if (tokenResult.getErrorResponse() != null) {
                    errorCode = tokenResult.getErrorResponse().getError();
                }
            }

            // Validate request success, may throw MsalServiceException
            validateServiceResult(tokenResult);

            // Assign token result
            acquireTokenResult.setTokenResult(tokenResult);

            // If the token is valid, save it into token cache
            final List<ICacheRecord> records = saveTokens(
                    oAuth2Strategy,
                    mAuthorizationRequest,
                    acquireTokenResult.getTokenResult().getTokenResponse(),
                    parameters.getOAuth2TokenCache()
            );

            // Once the token is stored, fetch and assign the authentication result
            final ICacheRecord newestRecord = records.get(0);
            acquireTokenResult.setLocalAuthenticationResult(
                    new LocalAuthenticationResult(
                            finalizeCacheRecordForResult(
                                    newestRecord,
                                    parameters.getAuthenticationScheme()
                            ),
                            records,
                            SdkType.MSAL,
                            false
                    )
            );
        } catch (Exception error) {
            Telemetry.emit(
                    new ApiEndEvent()
                            .putException(error)
                            .putApiId(TelemetryEventStrings.Api.LOCAL_DEVICE_CODE_FLOW_POLLING)
            );
            throw error;
        }

        ResultUtil.logResult(TAG, tokenResult);

        // End telemetry with LOCAL_DEVICE_CODE_FLOW_POLLING
        Telemetry.emit(
                new ApiEndEvent()
                        .putResult(acquireTokenResult)
                        .putApiId(TelemetryEventStrings.Api.LOCAL_DEVICE_CODE_FLOW_POLLING)
        );

        return acquireTokenResult;
    }

    @Override
    public GenerateShrResult generateSignedHttpRequest(
            @NonNull final GenerateShrCommandParameters parameters) throws Exception {
        final OAuth2TokenCache cache = parameters.getOAuth2TokenCache();
        final String clientId = parameters.getClientId();
        final String homeAccountId = parameters.getHomeAccountId();
        final IPoPAuthenticationSchemeParams popSchemeParams = parameters.getPopParameters();

        final GenerateShrResult result;
        if (userHasLocalAccountRecord(cache, clientId, homeAccountId)) {
            // Perform the signing locally...
            result = DevicePoPUtils.generateSignedHttpRequest(parameters.getPlatformComponents(), popSchemeParams);
        } else {
            // Populate the error on the result and return...
            result = new GenerateShrResult();
            result.setErrorCode(GenerateShrResult.Errors.NO_ACCOUNT_FOUND);
            result.setErrorMessage("Account does not exist.");
        }

        return result;
    }

    /**
     * Checks if the local cache contains an {@link AccountRecord} for the supplied input.
     *
     * @param cache         The cache to consult.
     * @param clientId      The clientId of the app on behalf of whom we are querying the cache.
     * @param homeAccountId The home_account_id of the targeted user.
     * @return True, if an {@link AccountRecord} exists. False otherwise.
     */
    private boolean userHasLocalAccountRecord(@NonNull final OAuth2TokenCache cache,
                                              @NonNull final String clientId,
                                              @NonNull final String homeAccountId) {
        // If we have an account for this user, then we will service this request locally
        return null != cache.getAccountByHomeAccountId(null, clientId, homeAccountId);
    }

    /**
     * Returns true if the given error shows authorization is pending.
     *
     * @param errorCode error from response
     * @return true or false if error is pending
     */
    private boolean authorizationPending(@NonNull final String errorCode) {
        return errorCode.equals(ErrorStrings.DEVICE_CODE_FLOW_AUTHORIZATION_PENDING_ERROR_CODE);
    }

    /**
     * Helper method to check if a result object is valid (was a success). If not, an exception will be generated and thrown.
     * This method is called in both parts of the DCF protocol.
     *
     * @param result result object to be checked
     * @throws ServiceException MsalServiceException object reflecting error code returned by the result
     */
    private void validateServiceResult(@NonNull final IResult result) throws ServiceException {
        // If result was unsuccessful, create an exception
        if (!result.getSuccess()) {
            // Create ServiceException object
            // Based on error code, fetch the error message
            final String errorCode = result.getErrorResponse().getError();
            final String errorMessage;

            // Check response code against pre-defined error codes
            switch (errorCode) {
                case ErrorStrings.DEVICE_CODE_FLOW_AUTHORIZATION_DECLINED_ERROR_CODE:
                    errorMessage = ErrorStrings.DEVICE_CODE_FLOW_AUTHORIZATION_DECLINED_ERROR_MESSAGE;
                    break;
                case ErrorStrings.DEVICE_CODE_FLOW_EXPIRED_TOKEN_ERROR_CODE:
                    errorMessage = ErrorStrings.DEVICE_CODE_FLOW_EXPIRED_TOKEN_ERROR_MESSAGE;
                    break;
                case ErrorStrings.DEVICE_CODE_FLOW_BAD_VERIFICATION_ERROR_CODE:
                    errorMessage = ErrorStrings.DEVICE_CODE_FLOW_BAD_VERIFICATION_ERROR_MESSAGE;
                    break;
                case OAuth2ErrorCode.INVALID_GRANT:
                    errorMessage = ErrorStrings.DEVICE_CODE_FLOW_INVALID_GRANT_ERROR_MESSAGE;
                    break;
                case ErrorStrings.INVALID_SCOPE:
                    errorMessage = ErrorStrings.DEVICE_CODE_FLOW_INVALID_SCOPE_ERROR_MESSAGE;
                    break;
                default:
                    errorMessage = ErrorStrings.DEVICE_CODE_FLOW_DEFAULT_ERROR_MESSAGE;
            }

            // Create a ServiceException object and throw it
            throw new ServiceException(
                    errorCode,
                    errorMessage,
                    ServiceException.DEFAULT_STATUS_CODE,
                    null
            );
        }
    }
}
