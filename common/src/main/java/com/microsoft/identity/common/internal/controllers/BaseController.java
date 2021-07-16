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

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.net.HttpWebRequest;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.internal.authscheme.ITokenAuthenticationSchemeInternal;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.internal.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.java.util.SchemaUtil;
import com.microsoft.identity.common.internal.commands.parameters.BrokerSilentTokenCommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.CommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.DeviceCodeFlowCommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.GenerateShrCommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.RemoveAccountCommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.TokenCommandParameters;
import com.microsoft.identity.common.java.dto.AccessTokenRecord;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.dto.IdTokenRecord;
import com.microsoft.identity.common.java.dto.RefreshTokenRecord;
import com.microsoft.identity.common.internal.migration.TokenCacheItemMigrationAdapter;
import com.microsoft.identity.common.internal.providers.oauth2.AndroidTaskStateGenerator;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.OpenIdConnectPromptParameter;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;
import com.microsoft.identity.common.internal.request.SdkType;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;
import com.microsoft.identity.common.internal.result.GenerateShrResult;
import com.microsoft.identity.common.internal.result.LocalAuthenticationResult;
import com.microsoft.identity.common.java.telemetry.CliTelemInfo;
import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.telemetry.events.CacheEndEvent;
import com.microsoft.identity.common.java.commands.parameters.IHasExtraParameters;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAuthorizationRequest;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftTokenRequest;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftTokenResponse;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.java.providers.oauth2.IResult;
import com.microsoft.identity.common.java.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.java.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.java.util.ObjectMapper;
import com.microsoft.identity.common.logging.DiagnosticContext;
import com.microsoft.identity.common.logging.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import lombok.EqualsAndHashCode;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2ErrorCode.INVALID_GRANT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2SubErrorCode.BAD_TOKEN;
import static com.microsoft.identity.common.internal.authorities.Authority.B2C;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class BaseController {

    private static final String TAG = BaseController.class.getSimpleName();

    public static final Set<String> DEFAULT_SCOPES = new HashSet<>();

    static {
        DEFAULT_SCOPES.add(AuthenticationConstants.OAuth2Scopes.OPEN_ID_SCOPE);
        DEFAULT_SCOPES.add(AuthenticationConstants.OAuth2Scopes.OFFLINE_ACCESS_SCOPE);
        DEFAULT_SCOPES.add(AuthenticationConstants.OAuth2Scopes.PROFILE_SCOPE);
    }

    public static String getDelimitedDefaultScopeString() {
        // using StringBuilder as String.join() requires at least API level 26
        StringBuilder stringBuilder = new StringBuilder();

        for (String scope : DEFAULT_SCOPES) {
            stringBuilder.append(scope);
            stringBuilder.append(' ');
        }

        return stringBuilder.toString().trim();
    }

    public abstract AcquireTokenResult acquireToken(final InteractiveTokenCommandParameters request)
            throws Exception;

    public abstract void completeAcquireToken(
            final int requestCode,
            final int resultCode,
            final Intent data
    );

    public abstract AcquireTokenResult acquireTokenSilent(
            final SilentTokenCommandParameters parameters)
            throws Exception;

    public abstract List<ICacheRecord> getAccounts(
            final CommandParameters parameters)
            throws Exception;

    public abstract boolean removeAccount(
            final RemoveAccountCommandParameters parameters)
            throws Exception;

    public abstract boolean getDeviceMode(final CommandParameters parameters)
            throws Exception;

    public abstract List<ICacheRecord> getCurrentAccount(final CommandParameters parameters)
            throws Exception;

    public abstract boolean removeCurrentAccount(final RemoveAccountCommandParameters parameters)
            throws Exception;

    // Suppressing rawtype warnings due to the generic type AuthorizationResult
    @SuppressWarnings(WarningType.rawtype_warning)
    public abstract AuthorizationResult deviceCodeFlowAuthRequest(final DeviceCodeFlowCommandParameters parameters)
            throws Exception;

    public abstract AcquireTokenResult acquireDeviceCodeFlowToken(@SuppressWarnings(WarningType.rawtype_warning) final AuthorizationResult authorizationResult, final DeviceCodeFlowCommandParameters parameters)
            throws Exception;

    /**
     * Pre-filled ALL the fields in AuthorizationRequest.Builder
     */
    //Suppressing rawtype warnings due to the generic type Builder
    @SuppressWarnings(WarningType.rawtype_warning)
    protected final AuthorizationRequest.Builder initializeAuthorizationRequestBuilder(@NonNull final AuthorizationRequest.Builder builder,
                                                                                       @NonNull final TokenCommandParameters parameters) {
        UUID correlationId = null;

        try {
            correlationId = UUID.fromString(DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID));
        } catch (IllegalArgumentException ex) {
            Logger.error(TAG, "correlation id from diagnostic context is not a UUID", ex);
        }

        builder.setClientId(parameters.getClientId())
                .setRedirectUri(parameters.getRedirectUri());

        if (builder instanceof MicrosoftAuthorizationRequest.Builder) {
            ((MicrosoftAuthorizationRequest.Builder) builder).setCorrelationId(correlationId);
        }

        final Set<String> scopes = parameters.getScopes();

        if (parameters instanceof InteractiveTokenCommandParameters) {
            final InteractiveTokenCommandParameters interactiveTokenCommandParameters = (InteractiveTokenCommandParameters) parameters;
            // Set the multipleCloudAware and slice fields.
            if (builder instanceof MicrosoftAuthorizationRequest.Builder) {
                ((MicrosoftStsAuthorizationRequest.Builder) builder).setTokenScope(TextUtils.join(" ", parameters.getScopes()));
                if (interactiveTokenCommandParameters.getAuthority() instanceof AzureActiveDirectoryAuthority) {
                    final AzureActiveDirectoryAuthority requestAuthority = (AzureActiveDirectoryAuthority) interactiveTokenCommandParameters.getAuthority();
                    ((MicrosoftStsAuthorizationRequest.Builder) builder)
                            .setAuthority(requestAuthority.getAuthorityURL())
                            .setMultipleCloudAware(requestAuthority.mMultipleCloudsSupported)
                            .setState(new AndroidTaskStateGenerator(interactiveTokenCommandParameters.getActivity().getTaskId()).generate())
                            .setSlice(requestAuthority.mSlice);
                }
            }

            // Adding getExtraScopesToConsent to "Auth" request only.
            // https://docs.microsoft.com/bs-latn-ba/azure/active-directory/develop/msal-net-user-gets-consent-for-multiple-resources
            if (interactiveTokenCommandParameters.getExtraScopesToConsent() != null) {
                scopes.addAll(interactiveTokenCommandParameters.getExtraScopesToConsent());
            }

            final HashMap<String, String> completeRequestHeaders = new HashMap<>();

            if (interactiveTokenCommandParameters.getRequestHeaders() != null) {
                completeRequestHeaders.putAll(interactiveTokenCommandParameters.getRequestHeaders());
            }

            completeRequestHeaders.put(
                    AuthenticationConstants.AAD.APP_PACKAGE_NAME,
                    parameters.getApplicationName()
            );
            completeRequestHeaders.put(AuthenticationConstants.AAD.APP_VERSION,
                    parameters.getApplicationVersion()
            );

            // Add additional fields to the AuthorizationRequest.Builder to support interactive
            setBuilderProperties(builder, parameters, interactiveTokenCommandParameters, completeRequestHeaders);

            // We don't want to show the SELECT_ACCOUNT page if login_hint is set.
            if (!StringExtensions.isNullOrBlank(interactiveTokenCommandParameters.getLoginHint()) &&
                    interactiveTokenCommandParameters.getPrompt() == OpenIdConnectPromptParameter.SELECT_ACCOUNT &&
                    builder instanceof MicrosoftStsAuthorizationRequest.Builder) {
                ((MicrosoftStsAuthorizationRequest.Builder) builder).setPrompt(null);
            }
        }

        builder.setScope(TextUtils.join(" ", scopes));

        return builder;
    }

    // Suppressing unchecked warning as the generic type was not provided during constructing builder object.
    @SuppressWarnings(WarningType.unchecked_warning)
    private void setBuilderProperties(@SuppressWarnings(WarningType.rawtype_warning) @NonNull AuthorizationRequest.Builder builder, @NonNull TokenCommandParameters parameters, InteractiveTokenCommandParameters interactiveTokenCommandParameters, HashMap<String, String> completeRequestHeaders) {
        builder.setExtraQueryParams(
                interactiveTokenCommandParameters.getExtraQueryStringParameters()
        ).setClaims(
                parameters.getClaimsRequestJson()
        ).setRequestHeaders(
                completeRequestHeaders
        ).setWebViewZoomEnabled(
                interactiveTokenCommandParameters.isWebViewZoomEnabled()
        ).setWebViewZoomControlsEnabled(
                interactiveTokenCommandParameters.isWebViewZoomControlsEnabled()
        );

        if (builder instanceof MicrosoftStsAuthorizationRequest.Builder) {
            final MicrosoftStsAuthorizationRequest.Builder msBuilder = (MicrosoftStsAuthorizationRequest.Builder) builder;
            msBuilder.setLoginHint(
                    interactiveTokenCommandParameters.getLoginHint()
            ).setPrompt(
                    interactiveTokenCommandParameters.getPrompt().toString()
            );

            try {
                final PackageInfo packageInfo =
                        parameters.getAndroidApplicationContext().getPackageManager().getPackageInfo(COMPANY_PORTAL_APP_PACKAGE_NAME, 0);
                msBuilder.setInstalledCompanyPortalVersion(packageInfo.versionName);
            } catch (final PackageManager.NameNotFoundException e) {
                // CP is not installed. No need to do anything.
            }
        }
    }

    // Suppressing rawtype warnings due to the generic type AuthorizationRequest, OAuth2Strategy and Builder
    @SuppressWarnings(WarningType.rawtype_warning)
    protected AuthorizationRequest getAuthorizationRequest(@NonNull final OAuth2Strategy strategy,
                                                           @NonNull final TokenCommandParameters parameters) {
        final AuthorizationRequest.Builder builder = strategy.createAuthorizationRequestBuilder(parameters.getAccount());
        initializeAuthorizationRequestBuilder(builder, parameters);
        return builder.build();
    }

    protected TokenResult performTokenRequest(@SuppressWarnings(WarningType.rawtype_warning) @NonNull final OAuth2Strategy strategy,
                                              @SuppressWarnings(WarningType.rawtype_warning) @NonNull final AuthorizationRequest request,
                                              @NonNull final AuthorizationResponse response,
                                              @NonNull final InteractiveTokenCommandParameters parameters)
            throws IOException, ClientException {
        final String methodName = ":performTokenRequest";
        HttpWebRequest.throwIfNetworkNotAvailable(
                parameters.getAndroidApplicationContext(),
                parameters.isPowerOptCheckEnabled()
        );

        // Suppressing unchecked warnings due to casting of type AuthorizationRequest to GenericAuthorizationRequest and AuthorizationResponse to GenericAuthorizationResponse in arguments of method call to createTokenRequest
        @SuppressWarnings(WarningType.unchecked_warning) final TokenRequest tokenRequest = strategy.createTokenRequest(
                request,
                response,
                parameters.getAuthenticationScheme()
        );

        if (tokenRequest instanceof MicrosoftTokenRequest) {
            ((MicrosoftTokenRequest) tokenRequest).setClientAppName(parameters.getApplicationName());
            ((MicrosoftTokenRequest) tokenRequest).setClientAppVersion(parameters.getApplicationVersion());
        }

        if (parameters instanceof IHasExtraParameters) {
            ((IHasExtraParameters) tokenRequest).setExtraParameters(((IHasExtraParameters) parameters).getExtraParameters());
        }

        logExposedFieldsOfObject(TAG + methodName, tokenRequest);

        // Suppressing unchecked warnings due to casting of type TokenRequest to GenericTokenRequest in argument of method call to requestToken
        @SuppressWarnings(WarningType.unchecked_warning) final TokenResult tokenResult = strategy.requestToken(tokenRequest);

        logResult(TAG, tokenResult);

        return tokenResult;
    }

    protected void renewAccessToken(@NonNull final SilentTokenCommandParameters parameters,
                                    @NonNull final AcquireTokenResult acquireTokenSilentResult,
                                    @SuppressWarnings(WarningType.rawtype_warning) @NonNull final OAuth2TokenCache tokenCache,
                                    @SuppressWarnings(WarningType.rawtype_warning) @NonNull final OAuth2Strategy strategy,
                                    @NonNull final ICacheRecord cacheRecord)
            throws IOException, ClientException {
        final String methodName = ":renewAccessToken";
        Logger.info(
                TAG + methodName,
                "Renewing access token..."
        );

        RefreshTokenRecord refreshTokenRecord = cacheRecord.getRefreshToken();

        logParameters(TAG, parameters);

        final TokenResult tokenResult = performSilentTokenRequest(
                strategy,
                refreshTokenRecord,
                parameters
        );

        acquireTokenSilentResult.setTokenResult(tokenResult);

        logResult(TAG + methodName, tokenResult);

        if (tokenResult.getSuccess()) {
            Logger.info(
                    TAG + methodName,
                    "Token request was successful"
            );

            // Suppressing unchecked warnings due to casting of rawtypes to generic types of OAuth2TokenCache's instance tokenCache while calling method saveAndLoadAggregatedAccountData
            @SuppressWarnings(WarningType.unchecked_warning) final List<ICacheRecord> savedRecords = tokenCache.saveAndLoadAggregatedAccountData(
                    strategy,
                    getAuthorizationRequest(strategy, parameters),
                    tokenResult.getTokenResponse()
            );

            final ICacheRecord savedRecord = savedRecords.get(0);

            // Create a new AuthenticationResult to hold the saved record
            final LocalAuthenticationResult authenticationResult = new LocalAuthenticationResult(
                    finalizeCacheRecordForResult(savedRecord, parameters.getAuthenticationScheme()),
                    savedRecords,
                    parameters.getSdkType(),
                    false
            );

            // Set the client telemetry...
            if (null != tokenResult.getCliTelemInfo()) {
                final CliTelemInfo cliTelemInfo = tokenResult.getCliTelemInfo();
                authenticationResult.setSpeRing(cliTelemInfo.getSpeRing());
                authenticationResult.setRefreshTokenAge(cliTelemInfo.getRefreshTokenAge());
                Telemetry.emit(new CacheEndEvent().putSpeInfo(tokenResult.getCliTelemInfo().getSpeRing()));
            } else {
                // we can't put SpeInfo as the CliTelemInfo is null
                Telemetry.emit(new CacheEndEvent());
            }

            // Set the AuthenticationResult on the final result object
            acquireTokenSilentResult.setLocalAuthenticationResult(authenticationResult);
        } else {
            if (tokenResult.getErrorResponse() != null) {
                final String errorCode = tokenResult.getErrorResponse().getError();
                final String subErrorCode = tokenResult.getErrorResponse().getSubError();
                Logger.info(TAG, "Error: " + errorCode + " Suberror: " + subErrorCode);

                if (INVALID_GRANT.equals(errorCode) && BAD_TOKEN.equals(subErrorCode)) {
                    boolean isRemoved = tokenCache.removeCredential(cacheRecord.getRefreshToken());
                    Logger.info(
                            TAG,
                            "Refresh token is invalid, "
                                    + "attempting to delete the RT from cache, result:"
                                    + isRemoved
                    );
                }
            } else {
                Logger.warn(TAG, "Invalid state, No token success or error response on the token result");
            }
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
            Logger.info(
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
            @SuppressWarnings(WarningType.rawtype_warning)
            AuthorizationResult authResult = (AuthorizationResult) result;

            if (authResult.getAuthorizationStatus() != null) {
                Logger.info(
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
            Logger.infoPII(TAG, ObjectMapper.serializeObjectToJsonString(parameters));
        } else {
            Logger.info(TAG, ObjectMapper.serializeExposedFieldsOfObjectToJsonString(parameters));
        }
    }

    protected static void logExposedFieldsOfObject(@NonNull final String tag,
                                                   @NonNull final Object object) {
        final String TAG = tag + ":" + object.getClass().getSimpleName();
        Logger.info(TAG, ObjectMapper.serializeExposedFieldsOfObjectToJsonString(object));
    }

    protected TokenResult performSilentTokenRequest(
            @SuppressWarnings(WarningType.rawtype_warning) @NonNull final OAuth2Strategy strategy,
            @NonNull final RefreshTokenRecord refreshToken,
            @NonNull final SilentTokenCommandParameters parameters)
            throws ClientException, IOException {
        final String methodName = ":performSilentTokenRequest";

        Logger.info(
                TAG + methodName,
                "Requesting tokens..."
        );

        HttpWebRequest.throwIfNetworkNotAvailable(
                parameters.getAndroidApplicationContext(),
                parameters.isPowerOptCheckEnabled()
        );

        // Check that the authority is known
        final Authority.KnownAuthorityResult authorityResult =
                Authority.getKnownAuthorityResult(parameters.getAuthority());

        if (!authorityResult.getKnown()) {
            throw authorityResult.getClientException();
        }

        final TokenRequest refreshTokenRequest = strategy.createRefreshTokenRequest(parameters.getAuthenticationScheme());
        refreshTokenRequest.setClientId(parameters.getClientId());
        refreshTokenRequest.setScope(TextUtils.join(" ", parameters.getScopes()));
        refreshTokenRequest.setRefreshToken(refreshToken.getSecret());

        if (refreshTokenRequest instanceof MicrosoftTokenRequest) {
            ((MicrosoftTokenRequest) refreshTokenRequest).setClaims(parameters.getClaimsRequestJson());
            ((MicrosoftTokenRequest) refreshTokenRequest).setClientAppName(parameters.getApplicationName());
            ((MicrosoftTokenRequest) refreshTokenRequest).setClientAppVersion(parameters.getApplicationVersion());
        }

        //NOTE: this should be moved to the strategy; however requires a larger refactor
        if (parameters.getSdkType() == SdkType.ADAL) {
            ((MicrosoftTokenRequest) refreshTokenRequest).setIdTokenVersion("1");
        }

        // Set Broker version to Token Request if it's a brokered request.
        if (parameters instanceof BrokerSilentTokenCommandParameters) {
            ((MicrosoftTokenRequest) refreshTokenRequest).setBrokerVersion(
                    ((BrokerSilentTokenCommandParameters) parameters).getBrokerVersion()
            );
        }

        if (!StringExtensions.isNullOrBlank(refreshTokenRequest.getScope())) {
            Logger.infoPII(
                    TAG + methodName,
                    "Scopes: [" + refreshTokenRequest.getScope() + "]"
            );
        }

        return strategyRequestToken(strategy, refreshTokenRequest);
    }

    // Suppressing unchecked warnings due to casting of TokenRequest to GenericTokenRequest in the call to requestToken method
    @SuppressWarnings(WarningType.unchecked_warning)
    private TokenResult strategyRequestToken(@SuppressWarnings(WarningType.rawtype_warning) @NonNull OAuth2Strategy strategy, TokenRequest refreshTokenRequest) throws IOException, ClientException {
        return strategy.requestToken(refreshTokenRequest);
    }

    protected List<ICacheRecord> saveTokens(@SuppressWarnings(WarningType.rawtype_warning) @NonNull final OAuth2Strategy strategy,
                                            @SuppressWarnings(WarningType.rawtype_warning) @NonNull final AuthorizationRequest request,
                                            @NonNull final TokenResponse tokenResponse,
                                            @SuppressWarnings(WarningType.rawtype_warning) @NonNull final OAuth2TokenCache tokenCache) throws ClientException {
        final String methodName = ":saveTokens";

        Logger.info(
                TAG + methodName,
                "Saving tokens..."
        );

        // Suppressing unchecked warnings due to casting of rawtypes to generic types of OAuth2TokenCache's instance tokenCache while calling method saveAndLoadAggregatedAccountData
        @SuppressWarnings(WarningType.unchecked_warning)
        List<ICacheRecord> cacheRecords = tokenCache.saveAndLoadAggregatedAccountData(
                strategy,
                request,
                tokenResponse
        );

        return cacheRecords;
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

    protected Set<String> addDefaultScopes(@NonNull final TokenCommandParameters commandParameters) {
        final Set<String> requestScopes = commandParameters.getScopes();
        requestScopes.addAll(DEFAULT_SCOPES);
        // sanitize empty and null scopes
        requestScopes.removeAll(Arrays.asList("", null));
        return requestScopes;
    }

    /**
     * Helper method to get a cached account
     *
     * @param parameters
     * @return
     */
    protected AccountRecord getCachedAccountRecord(
            @NonNull final SilentTokenCommandParameters parameters) throws ClientException {
        if (parameters.getAccount() == null) {
            throw new ClientException(
                    ErrorStrings.NO_ACCOUNT_FOUND,
                    "No cached accounts found for the supplied homeAccountId and clientId"
            );
        }

        final boolean isB2CAuthority = B2C.equalsIgnoreCase(
                parameters
                        .getAuthority()
                        .getAuthorityTypeString()
        );

        final String clientId = parameters.getClientId();
        final String homeAccountId = parameters.getAccount().getHomeAccountId();
        final String localAccountId = parameters.getAccount().getLocalAccountId();
        final String environment = parameters.getAccount().getEnvironment();

        AccountRecord targetAccount;

        if (isB2CAuthority) {
            // Due to differences in the B2C service API relative to AAD, all IAccounts returned by
            // the B2C-STS have the same local_account_id irrespective of the policy used to load it.
            //
            // Because the home_account_id is unique to policy and there is no concept of
            // multi-realm accounts relative to B2C, we'll conditionally use the home_account_id
            // in these cases
            targetAccount = parameters
                    .getOAuth2TokenCache()
                    .getAccountByHomeAccountId(
                            null,
                            clientId,
                            homeAccountId
                    );
        } else {
            targetAccount = parameters
                    .getOAuth2TokenCache()
                    .getAccountByLocalAccountId(
                            environment,
                            clientId,
                            localAccountId
                    );
        }

        if (null == targetAccount && parameters.getOAuth2TokenCache() instanceof MsalOAuth2TokenCache) {
            targetAccount = getAccountWithFRTIfAvailable(
                    parameters,
                    (MsalOAuth2TokenCache) parameters.getOAuth2TokenCache()
            );
        }

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

    @Nullable
    private AccountRecord getAccountWithFRTIfAvailable(@NonNull final SilentTokenCommandParameters parameters,
                                                       @SuppressWarnings(WarningType.rawtype_warning) @NonNull final MsalOAuth2TokenCache msalOAuth2TokenCache) {

        final String homeAccountId = parameters.getAccount().getHomeAccountId();
        final String clientId = parameters.getClientId();

        // check for FOCI tokens for the homeAccountId
        final RefreshTokenRecord refreshTokenRecord = msalOAuth2TokenCache
                .getFamilyRefreshTokenForHomeAccountId(homeAccountId);

        if (refreshTokenRecord != null) {
            try {
                // foci token is available, make a request to service to see if the client id is FOCI and save the tokens
                TokenCacheItemMigrationAdapter.tryFociTokenWithGivenClientId(
                        parameters.getOAuth2TokenCache(),
                        clientId,
                        parameters.getRedirectUri(),
                        refreshTokenRecord,
                        parameters.getAccount()
                );

                // Try to look for account again in the cache
                return parameters
                        .getOAuth2TokenCache()
                        .getAccountByLocalAccountId(
                                null,
                                clientId,
                                parameters.getAccount().getLocalAccountId()
                        );
            } catch (IOException | ClientException e) {
                Logger.warn(TAG,
                        "Error while attempting to validate client: "
                                + clientId + " is part of family " + e.getMessage()
                );
            }
        } else {
            Logger.info(TAG, "No Foci tokens found for homeAccountId " + homeAccountId);
        }
        return null;
    }

    /**
     * Helper method which returns false if the tenant id of the authority
     * doesn't match with the tenant of the Access token for AADAuthority.
     * <p>
     * Returns true otherwise.
     */
    protected boolean isRequestAuthorityRealmSameAsATRealm(@NonNull final Authority requestAuthority,
                                                           @NonNull final AccessTokenRecord accessTokenRecord)
            throws ServiceException, ClientException {
        if (requestAuthority instanceof AzureActiveDirectoryAuthority) {

            String tenantId = ((AzureActiveDirectoryAuthority) requestAuthority).getAudience().getTenantId();

            if (AzureActiveDirectoryAudience.isHomeTenantAlias(tenantId)) {
                // if realm on AT and home account's tenant id do not match, we have a token for guest and
                // requested authority here is for home, so return false we need to refresh the token
                final String utidFromHomeAccountId = accessTokenRecord
                        .getHomeAccountId()
                        .split(Pattern.quote("."))[1];

                return utidFromHomeAccountId.equalsIgnoreCase(accessTokenRecord.getRealm());

            } else {
                tenantId = ((AzureActiveDirectoryAuthority) requestAuthority)
                        .getAudience()
                        .getTenantUuidForAlias(requestAuthority.getAuthorityURL().toString());
                return tenantId.equalsIgnoreCase(accessTokenRecord.getRealm());
            }
        }
        return true;
    }

    protected boolean isMsaAccount(final MicrosoftTokenResponse microsoftTokenResponse) {
        final String tenantId = SchemaUtil.getTenantId(
                microsoftTokenResponse.getClientInfo(),
                microsoftTokenResponse.getIdToken()
        );
        return AzureActiveDirectoryAudience.MSA_MEGA_TENANT_ID.equalsIgnoreCase(tenantId);
    }

    public ICacheRecord finalizeCacheRecordForResult(@NonNull final ICacheRecord cacheRecord,
                                                     @NonNull final AbstractAuthenticationScheme scheme) throws ClientException {
        if (scheme instanceof ITokenAuthenticationSchemeInternal) {
            final ITokenAuthenticationSchemeInternal tokenAuthScheme = (ITokenAuthenticationSchemeInternal) scheme;
            cacheRecord
                    .getAccessToken()
                    .setSecret(
                            tokenAuthScheme
                                    .getAccessTokenForScheme(
                                            cacheRecord.getAccessToken().getSecret()
                                    )
                    );
        }

        return cacheRecord;
    }

    /**
     * Generates a SHR sans AT.
     *
     * @param parameters The input command params.
     * @return The {@link GenerateShrResult} containing the resulting SHR.
     * @throws Exception If an error is encountered during SHR generation.
     */
    public abstract GenerateShrResult generateSignedHttpRequest(GenerateShrCommandParameters parameters) throws Exception;
}
