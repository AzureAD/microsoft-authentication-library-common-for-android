package com.microsoft.identity.common.java.controllers;


import static com.microsoft.identity.common.java.AuthenticationConstants.Broker.PKEYAUTH_HEADER;
import static com.microsoft.identity.common.java.AuthenticationConstants.Broker.PKEYAUTH_VERSION;

import com.microsoft.identity.common.java.AuthenticationConstants;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.java.cache.CacheRecord;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.cache.MicrosoftStsAccountCredentialAdapter;
import com.microsoft.identity.common.java.commands.parameters.BrokerSilentTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.TokenCommandParameters;
import com.microsoft.identity.common.java.dto.AccessTokenRecord;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.dto.IdTokenRecord;
import com.microsoft.identity.common.java.dto.RefreshTokenRecord;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.logging.DiagnosticContext;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAuthorizationRequest;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftTokenRequest;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.java.providers.oauth2.OpenIdConnectPromptParameter;
import com.microsoft.identity.common.java.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;
import com.microsoft.identity.common.java.request.SdkType;
import com.microsoft.identity.common.java.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.NonNull;

public class AcquireTokenAndSaveResultStrategy implements IAcquireTokenAndSaveResultStrategy {

    private static final String TAG = AcquireTokenAndSaveResultStrategy.class.getSimpleName();

    @Override
    public List<ICacheRecord> constructRenewATResultFromTokenResult(@NonNull MicrosoftStsOAuth2Strategy oAuth2Strategy, @NonNull MicrosoftStsAuthorizationRequest authorizationRequest, @NonNull TokenResult tokenResult, @SuppressWarnings(WarningType.rawtype_warning) @NonNull OAuth2TokenCache tokenCache) throws ClientException {
        return saveRenewAccessTokenResultRecords(authorizationRequest, oAuth2Strategy, tokenResult, tokenCache);
    }

    private List<ICacheRecord> saveRenewAccessTokenResultRecords(@NonNull final MicrosoftStsAuthorizationRequest authorizationRequest,
                                                                @NonNull final MicrosoftStsOAuth2Strategy strategy,
                                                                @NonNull final TokenResult tokenResult,
                                                                 @SuppressWarnings(WarningType.rawtype_warning) @NonNull final OAuth2TokenCache tokenCache
    ) throws ClientException {
        return tokenCache.saveAndLoadAggregatedAccountData(
                strategy,
                authorizationRequest,
                tokenResult.getTokenResponse()
        );
    }

    @Override
    public List<ICacheRecord> getCacheRecord(@NonNull final MicrosoftStsTokenResponse microsoftStsTokenResponse,
                                             @NonNull final MicrosoftStsOAuth2Strategy oAuth2Strategy,
                                             @NonNull final MicrosoftStsAuthorizationRequest authorizationRequest) {
        final MicrosoftStsAccountCredentialAdapter credentialAdapter = new MicrosoftStsAccountCredentialAdapter();
        final AccountRecord accountRecord = credentialAdapter.createAccount(
                oAuth2Strategy,
                authorizationRequest,
                microsoftStsTokenResponse
        );

        final AccessTokenRecord accessTokenRecord = credentialAdapter.createAccessToken(
                oAuth2Strategy,
                authorizationRequest,
                microsoftStsTokenResponse
        );

        final IdTokenRecord idTokenRecord = credentialAdapter.createIdToken(
                oAuth2Strategy,
                authorizationRequest,
                microsoftStsTokenResponse
        );


        // Create the cache record from the token response
        final CacheRecord cacheRecord = CacheRecord.builder()
                .idToken(idTokenRecord)
                .accessToken(accessTokenRecord)
                .account(accountRecord)
                .refreshToken(credentialAdapter.createRefreshToken(
                        oAuth2Strategy,
                        authorizationRequest,
                        microsoftStsTokenResponse
                )).build();

        // Create the CacheRecord list
        final List<ICacheRecord> cacheRecordList = new ArrayList<>();
        cacheRecordList.add(cacheRecord);

        return cacheRecordList;
    }

    @Override
    public void setRefreshTokenParameters(@NonNull final TokenRequest refreshTokenRequest,
                                   @NonNull final RefreshTokenRecord refreshToken,
                                   @NonNull final SilentTokenCommandParameters parameters) {
        refreshTokenRequest.setScope(StringUtil.join(" ", parameters.getScopes()));
        refreshTokenRequest.setRefreshToken(refreshToken.getSecret());
        refreshTokenRequest.setClientId(parameters.getClientId());

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

    }

    @Override
    public  void initializeAuthorizationRequestBuilder(@SuppressWarnings(WarningType.rawtype_warning) @NonNull final AuthorizationRequest.Builder builder,
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
    }

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
}
