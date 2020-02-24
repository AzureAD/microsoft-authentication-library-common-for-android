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
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.net.HttpWebRequest;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.internal.authscheme.ITokenAuthenticationSchemeInternal;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.cache.SchemaUtil;
import com.microsoft.identity.common.internal.dto.AccessTokenRecord;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.dto.IdTokenRecord;
import com.microsoft.identity.common.internal.logging.DiagnosticContext;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.net.ObjectMapper;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftTokenRequest;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftTokenResponse;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
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
import com.microsoft.identity.common.internal.request.BrokerAcquireTokenSilentOperationParameters;
import com.microsoft.identity.common.internal.request.OperationParameters;
import com.microsoft.identity.common.internal.request.SdkType;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;
import com.microsoft.identity.common.internal.result.LocalAuthenticationResult;
import com.microsoft.identity.common.internal.telemetry.CliTelemInfo;
import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.telemetry.events.CacheEndEvent;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.microsoft.identity.common.internal.authorities.Authority.B2C;

public abstract class BaseController {

    private static final String TAG = BaseController.class.getSimpleName();

    public static final Set<String> DEFAULT_SCOPES = new HashSet<>();

    static {
        DEFAULT_SCOPES.add(AuthenticationConstants.OAuth2Scopes.OPEN_ID_SCOPE);
        DEFAULT_SCOPES.add(AuthenticationConstants.OAuth2Scopes.OFFLINE_ACCESS_SCOPE);
        DEFAULT_SCOPES.add(AuthenticationConstants.OAuth2Scopes.PROFILE_SCOPE);
    }

    public abstract AcquireTokenResult acquireToken(final AcquireTokenOperationParameters request)
            throws Exception;

    public abstract void completeAcquireToken(
            final int requestCode,
            final int resultCode,
            final Intent data
    );

    public abstract AcquireTokenResult acquireTokenSilent(final AcquireTokenSilentOperationParameters request)
            throws Exception;

    public abstract List<ICacheRecord> getAccounts(final OperationParameters parameters)
            throws Exception;

    public abstract boolean removeAccount(final OperationParameters parameters)
            throws Exception;

    public abstract boolean getDeviceMode(final OperationParameters parameters)
            throws Exception;

    public abstract List<ICacheRecord> getCurrentAccount(final OperationParameters parameters)
            throws Exception;

    public abstract boolean removeCurrentAccount(final OperationParameters parameters)
            throws Exception;

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
            // Set the multipleCloudAware and slice fields.
            if (acquireTokenOperationParameters.getAuthority() instanceof AzureActiveDirectoryAuthority) {
                final AzureActiveDirectoryAuthority requestAuthority = (AzureActiveDirectoryAuthority) acquireTokenOperationParameters.getAuthority();
                ((MicrosoftAuthorizationRequest.Builder) builder)
                        .setAuthority(requestAuthority.getAuthorityURL())
                        .setMultipleCloudAware(requestAuthority.mMultipleCloudsSupported)
                        .setSlice(requestAuthority.mSlice);
            }

            if (builder instanceof MicrosoftStsAuthorizationRequest.Builder) {
                ((MicrosoftStsAuthorizationRequest.Builder) builder).setTokenScope(TextUtils.join(" ", parameters.getScopes()));
            }

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
            ).setWebViewZoomEnabled(
                    acquireTokenOperationParameters.isWebViewZoomEnabled()
            ).setWebViewZoomControlsEnabled(
                    acquireTokenOperationParameters.isWebViewZoomControlsEnabled()
            );

            // We don't want to show the SELECT_ACCOUNT page if login_hint is set.
            if (!StringExtensions.isNullOrBlank(acquireTokenOperationParameters.getLoginHint()) &&
                    acquireTokenOperationParameters.getOpenIdConnectPromptParameter() == OpenIdConnectPromptParameter.SELECT_ACCOUNT) {
                builder.setPrompt(null);
            }


        }

        builder.setScope(TextUtils.join(" ", parameters.getScopes()));

        return builder;
    }

    protected AuthorizationRequest getAuthorizationRequest(@NonNull final OAuth2Strategy strategy,
                                                           @NonNull final OperationParameters parameters) {
        final AuthorizationRequest.Builder builder = strategy.createAuthorizationRequestBuilder(parameters.getAccount());
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

        final TokenRequest tokenRequest = strategy.createTokenRequest(
                request,
                response,
                parameters.getAuthenticationScheme()
        );
        logExposedFieldsOfObject(TAG + methodName, tokenRequest);

        final TokenResult tokenResult = strategy.requestToken(tokenRequest);

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
        Logger.info(
                TAG + methodName,
                "Renewing access token..."
        );
        parameters.setRefreshToken(cacheRecord.getRefreshToken());

        logParameters(TAG, parameters);

        final TokenResult tokenResult = performSilentTokenRequest(strategy, parameters);
        acquireTokenSilentResult.setTokenResult(tokenResult);

        logResult(TAG + methodName, tokenResult);

        if (tokenResult.getSuccess()) {
            Logger.info(
                    TAG + methodName,
                    "Token request was successful"
            );

            final List<ICacheRecord> savedRecords = tokenCache.saveAndLoadAggregatedAccountData(
                    strategy,
                    getAuthorizationRequest(strategy, parameters),
                    tokenResult.getTokenResponse()
            );
            final ICacheRecord savedRecord = savedRecords.get(0);

            // Create a new AuthenticationResult to hold the saved record
            final LocalAuthenticationResult authenticationResult = new LocalAuthenticationResult(
                    finalizeCacheRecordForResult(savedRecord, parameters.getAuthenticationScheme()),
                    savedRecords,
                    SdkType.MSAL
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
            @NonNull final OAuth2Strategy strategy,
            @NonNull final AcquireTokenSilentOperationParameters parameters)
            throws ClientException, IOException {
        final String methodName = ":performSilentTokenRequest";

        Logger.info(
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

        final TokenRequest refreshTokenRequest = strategy.createRefreshTokenRequest(parameters.getAuthenticationScheme());
        refreshTokenRequest.setClientId(parameters.getClientId());
        refreshTokenRequest.setScope(TextUtils.join(" ", parameters.getScopes()));
        refreshTokenRequest.setRefreshToken(parameters.getRefreshToken().getSecret());

        if (refreshTokenRequest instanceof MicrosoftTokenRequest) {
            ((MicrosoftTokenRequest) refreshTokenRequest).setClaims(parameters.getClaimsRequestJson());
        }

        //NOTE: this should be moved to the strategy; however requires a larger refactor
        if (parameters.getSdkType() == SdkType.ADAL) {
            ((MicrosoftTokenRequest) refreshTokenRequest).setIdTokenVersion("1");
        }

        // Set Broker version to Token Request if it's a brokered request.
        if (parameters instanceof BrokerAcquireTokenSilentOperationParameters) {
            ((MicrosoftTokenRequest) refreshTokenRequest).setBrokerVersion(
                    ((BrokerAcquireTokenSilentOperationParameters) parameters).getBrokerVersion()
            );
        }

        if (!StringExtensions.isNullOrBlank(refreshTokenRequest.getScope())) {
            Logger.infoPII(
                    TAG + methodName,
                    "Scopes: [" + refreshTokenRequest.getScope() + "]"
            );
        }

        return strategy.requestToken(refreshTokenRequest);
    }

    protected List<ICacheRecord> saveTokens(@NonNull final OAuth2Strategy strategy,
                                            @NonNull final AuthorizationRequest request,
                                            @NonNull final TokenResponse tokenResponse,
                                            @NonNull final OAuth2TokenCache tokenCache) throws ClientException {
        final String methodName = ":saveTokens";

        Logger.info(
                TAG + methodName,
                "Saving tokens..."
        );

        return tokenCache.saveAndLoadAggregatedAccountData(
                strategy,
                request,
                tokenResponse
        );
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
        requestScopes.addAll(DEFAULT_SCOPES);
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

        final boolean isB2CAuthority = B2C.equalsIgnoreCase(
                parameters
                        .getAuthority()
                        .getAuthorityTypeString()
        );

        final String clientId = parameters.getClientId();
        final String homeAccountId = parameters.getAccount().getHomeAccountId();
        final String localAccountId = parameters.getAccount().getLocalAccountId();

        final AccountRecord targetAccount;

        if (isB2CAuthority) {
            // Due to differences in the B2C service API relative to AAD, all IAccounts returned by
            // the B2C-STS have the same local_account_id irrespective of the policy used to load it.
            //
            // Because the home_account_id is unique to policy and there is no concept of
            // multi-realm accounts relative to B2C, we'll conditionally use the home_account_id
            // in these cases
            targetAccount = parameters
                    .getTokenCache()
                    .getAccountByHomeAccountId(
                            null,
                            clientId,
                            homeAccountId
                    );
        } else {
            targetAccount = parameters
                    .getTokenCache()
                    .getAccountByLocalAccountId(
                            null,
                            clientId,
                            localAccountId
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
}
