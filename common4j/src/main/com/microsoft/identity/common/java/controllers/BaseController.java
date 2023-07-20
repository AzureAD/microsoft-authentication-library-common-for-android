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
package com.microsoft.identity.common.java.controllers;

import static com.microsoft.identity.common.java.AuthenticationConstants.Broker.PKEYAUTH_HEADER;
import static com.microsoft.identity.common.java.AuthenticationConstants.Broker.PKEYAUTH_VERSION;
import static com.microsoft.identity.common.java.authorities.Authority.B2C;
import static com.microsoft.identity.common.java.exception.ServiceException.SERVICE_NOT_AVAILABLE;
import static com.microsoft.identity.common.java.util.ResultUtil.logExposedFieldsOfObject;

import com.microsoft.identity.common.java.AuthenticationConstants;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.java.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.java.authscheme.ITokenAuthenticationSchemeInternal;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.java.commands.parameters.BrokerSilentTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.CommandParameters;
import com.microsoft.identity.common.java.commands.parameters.DeviceCodeFlowCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.GenerateShrCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.IHasExtraParameters;
import com.microsoft.identity.common.java.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.RemoveAccountCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.RopcTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.TokenCommandParameters;
import com.microsoft.identity.common.java.constants.OAuth2ErrorCode;
import com.microsoft.identity.common.java.constants.OAuth2SubErrorCode;
import com.microsoft.identity.common.java.dto.AccessTokenRecord;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.dto.IdTokenRecord;
import com.microsoft.identity.common.java.dto.RefreshTokenRecord;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.foci.FociQueryUtilities;
import com.microsoft.identity.common.java.logging.DiagnosticContext;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAuthorizationRequest;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftTokenRequest;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftTokenResponse;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.java.providers.oauth2.IResult;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.java.providers.oauth2.OpenIdConnectPromptParameter;
import com.microsoft.identity.common.java.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.java.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;
import com.microsoft.identity.common.java.request.SdkType;
import com.microsoft.identity.common.java.result.AcquireTokenResult;
import com.microsoft.identity.common.java.result.GenerateShrResult;
import com.microsoft.identity.common.java.result.LocalAuthenticationResult;
import com.microsoft.identity.common.java.telemetry.CliTelemInfo;
import com.microsoft.identity.common.java.telemetry.Telemetry;
import com.microsoft.identity.common.java.telemetry.events.CacheEndEvent;
import com.microsoft.identity.common.java.util.ObjectMapper;
import com.microsoft.identity.common.java.util.ResultUtil;
import com.microsoft.identity.common.java.util.SchemaUtil;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.ported.PropertyBag;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class BaseController {

    private static final String TAG = BaseController.class.getSimpleName();

    public static String getDelimitedDefaultScopeString() {
        // using StringBuilder as String.join() requires at least API level 26
        StringBuilder stringBuilder = new StringBuilder();

        for (String scope : AuthenticationConstants.DEFAULT_SCOPES) {
            stringBuilder.append(scope);
            stringBuilder.append(' ');
        }

        return stringBuilder.toString().trim();
    }

    public abstract AcquireTokenResult acquireToken(final InteractiveTokenCommandParameters request)
            throws Exception;

    public abstract void onFinishAuthorizationSession(
            final int requestCode,
            final int resultCode,
            @NonNull final PropertyBag data);

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

    public AcquireTokenResult acquireTokenWithPassword(@NonNull final RopcTokenCommandParameters parameters) throws Exception {
        final String methodName = ":acquireToken";

        Logger.verbose(
                TAG + methodName,
                "Acquiring token..."
        );

        final AcquireTokenResult acquireTokenResult = new AcquireTokenResult();

        //00) Validate MSAL Parameters
        parameters.validate();

        // Add default scopes
        final Set<String> mergedScopes = addDefaultScopes(parameters);

        final RopcTokenCommandParameters parametersWithScopes = parameters
                .toBuilder()
                .scopes(mergedScopes)
                .build();

        logParameters(TAG, parametersWithScopes);

        //0) Get known authority result
        parametersWithScopes.getPlatformComponents()
                .getPlatformUtil()
                .throwIfNetworkNotAvailable(parametersWithScopes.isPowerOptCheckEnabled());

//        final Authority.KnownAuthorityResult authorityResult = Authority.getKnownAuthorityResult(parametersWithScopes.getAuthority());
//
//        //0.1 If not known throw resulting exception
//        if (!authorityResult.getKnown()) {
//            Logger.error(TAG + methodName, "Authority is not known.", authorityResult.getClientException());
//            throw authorityResult.getClientException();
//        }

        // Build up params for Strategy construction
        final OAuth2StrategyParameters strategyParameters = OAuth2StrategyParameters.builder()
                .platformComponents(parameters.getPlatformComponents())
                .authenticationScheme(parameters.getAuthenticationScheme())
                .build();

        //1) Get oAuth2Strategy for Authority Type
        @SuppressWarnings(WarningType.rawtype_warning) final OAuth2Strategy oAuth2Strategy = parametersWithScopes
                .getAuthority()
                .createOAuth2Strategy(strategyParameters);

        //2) Get the token by exchanging password
        final TokenRequest ropcTokenRequest = oAuth2Strategy.createRopcTokenRequest(
                parametersWithScopes
        );

        final TokenResult tokenResult = oAuth2Strategy.requestToken(ropcTokenRequest);

        acquireTokenResult.setTokenResult(tokenResult);

        @SuppressWarnings(WarningType.rawtype_warning) final OAuth2TokenCache tokenCache = parameters.getOAuth2TokenCache();

        if (tokenResult != null && tokenResult.getSuccess()) {
            // Suppressing unchecked warnings due to casting of rawtypes to generic types of OAuth2TokenCache's instance tokenCache while calling method saveAndLoadAggregatedAccountData
            @SuppressWarnings(WarningType.unchecked_warning) final List<ICacheRecord> savedRecords = tokenCache.saveAndLoadAggregatedAccountData(
                    oAuth2Strategy,
                    getAuthorizationRequest(oAuth2Strategy, parameters),
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
            acquireTokenResult.setLocalAuthenticationResult(authenticationResult);
        }

        return acquireTokenResult;
    }

    /**
     * Pre-filled ALL the fields in AuthorizationRequest.Builder
     */
    //Suppressing rawtype warnings due to the generic type Builder
    @SuppressWarnings(WarningType.rawtype_warning)
    protected final AuthorizationRequest.Builder initializeAuthorizationRequestBuilder(@NonNull final AuthorizationRequest.Builder builder,
                                                                                       @NonNull final TokenCommandParameters parameters) {
        UUID correlationId = null;

        try {
            correlationId = UUID.fromString(DiagnosticContext.INSTANCE.getRequestContext().get(DiagnosticContext.CORRELATION_ID));
        } catch (IllegalArgumentException ex) {
            Logger.error(TAG, "correlation id from diagnostic context is not a UUID", ex);
        }

        builder.setClientId(parameters.getClientId())
                .setRedirectUri(parameters.getRedirectUri());

        if (builder instanceof MicrosoftAuthorizationRequest.Builder) {
            ((MicrosoftAuthorizationRequest.Builder) builder).setCorrelationId(correlationId);
        }

        if (builder instanceof MicrosoftStsAuthorizationRequest.Builder) {
            ((MicrosoftStsAuthorizationRequest.Builder) builder).setApplicationIdentifier(parameters.getApplicationIdentifier());
        }

        final Set<String> scopes = parameters.getScopes();

        if (parameters instanceof InteractiveTokenCommandParameters) {
            final InteractiveTokenCommandParameters interactiveTokenCommandParameters = (InteractiveTokenCommandParameters) parameters;
            // Set the multipleCloudAware and slice fields.
            if (builder instanceof MicrosoftAuthorizationRequest.Builder) {
                ((MicrosoftStsAuthorizationRequest.Builder) builder).setTokenScope(StringUtil.join(" ", parameters.getScopes()));
                if (interactiveTokenCommandParameters.getAuthority() instanceof AzureActiveDirectoryAuthority) {
                    final AzureActiveDirectoryAuthority requestAuthority = (AzureActiveDirectoryAuthority) interactiveTokenCommandParameters.getAuthority();
                    ((MicrosoftStsAuthorizationRequest.Builder) builder)
                            .setAuthority(requestAuthority.getAuthorityURL())
                            .setMultipleCloudAware(requestAuthority.isMultipleCloudsSupported())
                            .setState(interactiveTokenCommandParameters.getPlatformComponents().getStateGenerator().generate())
                            .setSlice(requestAuthority.mSlice)
                            .setApplicationIdentifier(parameters.getApplicationIdentifier());
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
            completeRequestHeaders.put(PKEYAUTH_HEADER, PKEYAUTH_VERSION);

            // Add additional fields to the AuthorizationRequest.Builder to support interactive
            setBuilderProperties(builder, parameters, interactiveTokenCommandParameters, completeRequestHeaders);

            // We don't want to show the SELECT_ACCOUNT page if login_hint is set.
            if (!StringUtil.isNullOrEmpty(interactiveTokenCommandParameters.getLoginHint()) &&
                    interactiveTokenCommandParameters.getPrompt() == OpenIdConnectPromptParameter.SELECT_ACCOUNT &&
                    builder instanceof MicrosoftStsAuthorizationRequest.Builder) {
                ((MicrosoftStsAuthorizationRequest.Builder) builder).setPrompt(null);
            }
        }

        builder.setScope(StringUtil.join(" ", scopes));

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

            final String installedCompanyPortalVersion =
                    parameters.getPlatformComponents().getPlatformUtil().getInstalledCompanyPortalVersion();

            if (!StringUtil.isNullOrEmpty(installedCompanyPortalVersion)) {
                msBuilder.setInstalledCompanyPortalVersion(installedCompanyPortalVersion);
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

        parameters.getPlatformComponents()
                .getPlatformUtil()
                .throwIfNetworkNotAvailable(parameters.isPowerOptCheckEnabled());

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

        ResultUtil.logExposedFieldsOfObject(TAG + methodName, tokenRequest);

        // Suppressing unchecked warnings due to casting of type TokenRequest to GenericTokenRequest in argument of method call to requestToken
        @SuppressWarnings(WarningType.unchecked_warning) final TokenResult tokenResult = strategy.requestToken(tokenRequest);

        ResultUtil.logResult(TAG, tokenResult);

        return tokenResult;
    }

    protected void renewAccessToken(@NonNull final SilentTokenCommandParameters parameters,
                                    @NonNull final AcquireTokenResult acquireTokenSilentResult,
                                    @SuppressWarnings(WarningType.rawtype_warning) @NonNull final OAuth2TokenCache tokenCache,
                                    @SuppressWarnings(WarningType.rawtype_warning) @NonNull final OAuth2Strategy strategy,
                                    @NonNull final ICacheRecord cacheRecord)
            throws IOException, ClientException, ServiceException {
        final String methodTag = TAG + ":renewAccessToken";
        Logger.info(
                methodTag,
                "Renewing access token..."
        );

        RefreshTokenRecord refreshTokenRecord = cacheRecord.getRefreshToken();

        logParameters(methodTag, parameters);

        final TokenResult tokenResult = performSilentTokenRequest(
                strategy,
                refreshTokenRecord,
                parameters
        );

        acquireTokenSilentResult.setTokenResult(tokenResult);

        ResultUtil.logResult(methodTag, tokenResult);

        if (tokenResult.getSuccess()) {
            Logger.info(
                    methodTag,
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
                Logger.info(methodTag, "Error: " + errorCode + " Suberror: " + subErrorCode);

                if (OAuth2ErrorCode.INVALID_GRANT.equals(errorCode) &&
                        OAuth2SubErrorCode.BAD_TOKEN.equals(subErrorCode)) {
                    boolean isRemoved = tokenCache.removeCredential(cacheRecord.getRefreshToken());
                    Logger.info(
                            methodTag,
                            "Refresh token is invalid, "
                                    + "attempting to delete the RT from cache, result:"
                                    + isRemoved
                    );
                }

                /*
                    Intended to cover the AAD outage scenario for the refresh_in logic.
                    Should return existing AT without refreshing it.
                    This way caller will know whether to refresh based on this exception.
                 */
                if (SERVICE_NOT_AVAILABLE.equals(errorCode)) {
                    throw new ServiceException(SERVICE_NOT_AVAILABLE, "AAD is not available.", tokenResult.getErrorResponse().getStatusCode(), null);
                }

            } else {
                Logger.warn(methodTag, "Invalid state, No token success or error response on the token result");
            }
        }
    }

    /*
        Light version of:
        protected renewAccessToken(@NonNull final SilentTokenCommandParameters parameters,
                                   @NonNull final AcquireTokenResult acquireTokenSilentResult,
                                   @SuppressWarnings(WarningType.rawtype_warning) @NonNull final OAuth2TokenCache tokenCache,
                                   @SuppressWarnings(WarningType.rawtype_warning) @NonNull final OAuth2Strategy strategy,
                                   @NonNull final ICacheRecord cacheRecord)

        Diffs
        1) acquireTokenSilentResult updates omitted.
        2) All arguments derived from SilentTokenCommandParameters
        3) New logic replacing old Access Token
     */
    public TokenResult renewAccessToken(@NonNull final SilentTokenCommandParameters parameters)
            throws IOException, ClientException, ServiceException {
        final String methodTag = TAG + ":renewAccessToken";
        Logger.info(
                methodTag,
                "Renewing access token..."
        );

        OAuth2Strategy strategy = getStrategy(parameters);
        OAuth2TokenCache cache = getTokenCache(parameters);
        ICacheRecord cacheRecord = getCacheRecord(parameters);

        Logger.info(
                methodTag,
                "Attempting renewal of Access Token because it's refresh-expired. RefreshIn was expired at " + cacheRecord.getAccessToken().getRefreshOn() + ". Regular expiry is at " + cacheRecord.getAccessToken().getExpiresOn() + "."
                        + "Currently executing acquireTokenSilent(..), SilentTokenCommand with CorrelationId: " + parameters.getCorrelationId()
        );
        //Get tokenResult
        RefreshTokenRecord refreshTokenRecord = cacheRecord.getRefreshToken();
        logParameters(TAG, parameters);
        final TokenResult tokenResult = performSilentTokenRequest(
                strategy,
                refreshTokenRecord,
                parameters
        );

        logResult(methodTag, tokenResult);
        if (tokenResult.getSuccess()) {
            Logger.info(
                    methodTag,
                    "Token request was successful"
            );

            // Remove old Access Token
            Logger.info(
                    methodTag,
                    "Access token is refresh-expired. Removing from cache..."
            );
            final AccessTokenRecord accessTokenRecord = cacheRecord.getAccessToken();
            cache.removeCredential(accessTokenRecord);

            // Suppressing unchecked warnings due to casting of rawtypes to generic types of OAuth2TokenCache's instance tokenCache while calling method saveAndLoadAggregatedAccountData
            @SuppressWarnings(WarningType.unchecked_warning) final List<ICacheRecord> savedRecords = cache.saveAndLoadAggregatedAccountData(
                    strategy,
                    getAuthorizationRequest(strategy, parameters),
                    tokenResult.getTokenResponse()
            );

            final ICacheRecord savedRecord = savedRecords.get(0);
            finalizeCacheRecordForResult(savedRecord, parameters.getAuthenticationScheme());
            // Set the client telemetry...
            if (null != tokenResult.getCliTelemInfo()) {
                Telemetry.emit(new CacheEndEvent().putSpeInfo(tokenResult.getCliTelemInfo().getSpeRing()));
            } else {
                // we can't put SpeInfo as the CliTelemInfo is null
                Telemetry.emit(new CacheEndEvent());
            }

        } else {
            if (tokenResult.getErrorResponse() != null) {
                final String errorCode = tokenResult.getErrorResponse().getError();
                final String subErrorCode = tokenResult.getErrorResponse().getSubError();
                Logger.warn(methodTag, "Error: " + errorCode + " Suberror: " + subErrorCode);

                if (OAuth2ErrorCode.INVALID_GRANT.equals(errorCode) && OAuth2SubErrorCode.BAD_TOKEN.equals(subErrorCode)) {
                    boolean isRemoved = cache.removeCredential(cacheRecord.getRefreshToken());
                    Logger.info(
                            methodTag,
                            "Refresh token is invalid, "
                                    + "attempting to delete the RT from cache, result:"
                                    + isRemoved
                    );
                }

                /*
                    Intended to cover the AAD outage scenario for the refresh_in logic.
                    Should return existing AT without refreshing it.
                    This way caller will know whether to refresh based on this exception.
                 */
                if (SERVICE_NOT_AVAILABLE.equals(errorCode)) {
                    throw new ServiceException(SERVICE_NOT_AVAILABLE, "AAD is not available.", tokenResult.getErrorResponse().getStatusCode(), null);
                }

            } else {
                Logger.warn(methodTag, "Invalid state, No token success or error response on the token result");
            }
        }

        return tokenResult;
    }

    public OAuth2Strategy getStrategy(@NonNull final SilentTokenCommandParameters parameters) throws ClientException {
        final OAuth2StrategyParameters strategyParameters = OAuth2StrategyParameters.builder()
                .platformComponents(parameters.getPlatformComponents())
                .authenticationScheme(parameters.getAuthenticationScheme())
                .build();

        return parameters.getAuthority().createOAuth2Strategy(strategyParameters);
    }

    public ICacheRecord getCacheRecord(@NonNull final SilentTokenCommandParameters parameters) throws ClientException {
        //Extract cache from parameters
        final AccountRecord targetAccount = getCachedAccountRecord(parameters);
        final AbstractAuthenticationScheme authScheme = parameters.getAuthenticationScheme();
        final OAuth2TokenCache cache = parameters.getOAuth2TokenCache();

        //Get cacheRecord from cache
        @SuppressWarnings("unchecked")
        final List<ICacheRecord> cacheRecords = cache.loadWithAggregatedAccountData(
                parameters.getClientId(),
                parameters.getApplicationIdentifier(),
                parameters.getMamEnrollmentId(),
                StringUtil.join(" ", parameters.getScopes()),
                targetAccount,
                authScheme
        );
        return cacheRecords.get(0);
    }

    public OAuth2TokenCache getTokenCache(@NonNull final SilentTokenCommandParameters parameters) {
        //Extract cache from parameters
        return parameters.getOAuth2TokenCache();
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

        if (Logger.isAllowPii()) {
            Logger.infoPII(TAG, ObjectMapper.serializeObjectToJsonString(parameters));
        } else {
            Logger.info(TAG, ObjectMapper.serializeExposedFieldsOfObjectToJsonString(parameters));
        }
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

        parameters.getPlatformComponents()
                .getPlatformUtil()
                .throwIfNetworkNotAvailable(parameters.isPowerOptCheckEnabled());

        // Check that the authority is known
//        final Authority.KnownAuthorityResult authorityResult =
//                Authority.getKnownAuthorityResult(parameters.getAuthority());
//
//        if (!authorityResult.getKnown()) {
//            throw authorityResult.getClientException();
//        }

        final TokenRequest refreshTokenRequest = strategy.createRefreshTokenRequest(parameters.getAuthenticationScheme());
        refreshTokenRequest.setClientId(parameters.getClientId());
        refreshTokenRequest.setScope(StringUtil.join(" ", parameters.getScopes()));
        refreshTokenRequest.setRefreshToken(refreshToken.getSecret());

        if (refreshTokenRequest instanceof MicrosoftTokenRequest) {
            ((MicrosoftTokenRequest) refreshTokenRequest).setClaims(parameters.getClaimsRequestJson());
            ((MicrosoftTokenRequest) refreshTokenRequest).setClientAppName(parameters.getApplicationName());
            ((MicrosoftTokenRequest) refreshTokenRequest).setClientAppVersion(parameters.getApplicationVersion());

            //NOTE: this should be moved to the strategy; however requires a larger refactor
            if (parameters.getSdkType() == SdkType.ADAL) {
                ((MicrosoftTokenRequest) refreshTokenRequest).setIdTokenVersion("1");
            }

            if (parameters instanceof BrokerSilentTokenCommandParameters) {
                // Set Broker version to Token Request if it's a brokered request.
                ((MicrosoftTokenRequest) refreshTokenRequest).setBrokerVersion(
                        ((BrokerSilentTokenCommandParameters) parameters).getBrokerVersion()
                );
                // Set PKeyAuth Header for token endpoint.
                ((MicrosoftTokenRequest) refreshTokenRequest).setPKeyAuthHeaderAllowed(
                        ((BrokerSilentTokenCommandParameters) parameters).isPKeyAuthHeaderAllowed()
                );
            }
        }

        if (!StringUtil.isNullOrEmpty(refreshTokenRequest.getScope())) {
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
        requestScopes.addAll(AuthenticationConstants.DEFAULT_SCOPES);
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
        final String methodTag = TAG + ":getCachedAccountRecord";
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

        AccountRecord targetAccount = getCachedAccountRecordFromCallingAppCache(parameters);
        if (targetAccount != null) {
            return targetAccount;
        } else {
            Logger.info(methodTag, "Account not found in app cache..");
            targetAccount = getCachedAccountRecordFromAllCaches(parameters);
        }

        if (null == targetAccount) {
            final String clientId = parameters.getClientId();
            final String homeAccountId = parameters.getAccount().getHomeAccountId();
            if (Logger.isAllowPii()) {
                Logger.errorPII(
                        methodTag,
                        "No accounts found for clientId [" + clientId + "], homeAccountId [" + homeAccountId + "]",
                        null
                );
            } else {
                Logger.error(
                        methodTag,
                        "No accounts found for clientId [" + clientId + "]",
                        null
                );
            }

            throw new ClientException(
                    ErrorStrings.NO_ACCOUNT_FOUND,
                    "No cached accounts found for the supplied "
                            + (isB2CAuthority ? "homeAccountId" : "localAccountId")
            );
        }

        return targetAccount;
    }

    /**
     * Lookup in app-specific cache.
     */
    @Nullable
    private AccountRecord getCachedAccountRecordFromCallingAppCache(
            @NonNull final SilentTokenCommandParameters parameters) {
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
        return targetAccount;
    }

    /**
     * Lookup in ALL the caches including the foci cache.
     */
    @Nullable
    protected AccountRecord getCachedAccountRecordFromAllCaches(
            @NonNull final SilentTokenCommandParameters parameters) throws ClientException {
        // TO-DO https://identitydivision.visualstudio.com/Engineering/_workitems/edit/1999531/
        if (parameters.getOAuth2TokenCache() instanceof MsalOAuth2TokenCache) {
            return getAccountWithFRTIfAvailable(
                    parameters,
                    (MsalOAuth2TokenCache) parameters.getOAuth2TokenCache()
            );
        }
        return null;
    }

    @Nullable
    private AccountRecord getAccountWithFRTIfAvailable(@NonNull final SilentTokenCommandParameters parameters,
                                                       @SuppressWarnings(WarningType.rawtype_warning) @NonNull final MsalOAuth2TokenCache msalOAuth2TokenCache) {

        final String methodTag = TAG + ":getAccountWithFRTIfAvailable";
        final String homeAccountId = parameters.getAccount().getHomeAccountId();
        final String clientId = parameters.getClientId();

        // check for FOCI tokens for the homeAccountId
        final RefreshTokenRecord refreshTokenRecord = msalOAuth2TokenCache
                .getFamilyRefreshTokenForHomeAccountId(homeAccountId);

        if (refreshTokenRecord != null) {
            try {
                // foci token is available, make a request to service to see if the client id is FOCI and save the tokens
                FociQueryUtilities.tryFociTokenWithGivenClientId(
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
                Logger.warn(methodTag,
                        "Error while attempting to validate client: "
                                + clientId + " is part of family " + e.getMessage()
                );
            }
        } else {
            Logger.info(methodTag, "No Foci tokens found for homeAccountId " + homeAccountId);
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

    public ICacheRecord finalizeCacheRecordForResult(@NonNull final ICacheRecord cacheRecord,
                                                     @NonNull final AbstractAuthenticationScheme scheme) throws ClientException {
        if (scheme instanceof ITokenAuthenticationSchemeInternal &&
                !StringUtil.isNullOrEmpty(cacheRecord.getAccessToken().getSecret())) {
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

    /**
     * Helper method for Device Code Flow (DCF) to check if a result object is valid (was a success). If not, an exception will be generated and thrown.
     * This method is called in both parts of the DCF protocol.
     *
     * @param result result object to be checked
     * @throws ServiceException MsalServiceException object reflecting error code returned by the result
     */
    protected void validateDeviceCodeFlowServiceResult(@NonNull final IResult result) throws ServiceException {
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
