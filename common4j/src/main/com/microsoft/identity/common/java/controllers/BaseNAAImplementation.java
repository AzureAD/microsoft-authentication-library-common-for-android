package com.microsoft.identity.common.java.controllers;

import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.TokenCommandParameters;
import com.microsoft.identity.common.java.dto.RefreshTokenRecord;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.java.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;

import java.util.List;

import lombok.NonNull;

public class BaseNAAImplementation extends AcquireTokenAndSaveResultStrategy {

    @Override
    public List<ICacheRecord> constructRenewATResultFromTokenResult(@NonNull MicrosoftStsOAuth2Strategy oAuth2Strategy, @NonNull MicrosoftStsAuthorizationRequest authorizationRequest, @NonNull TokenResult tokenResult, @SuppressWarnings(WarningType.rawtype_warning) @NonNull OAuth2TokenCache tokenCache) {
        return getCacheRecord((MicrosoftStsTokenResponse) tokenResult.getTokenResponse(), oAuth2Strategy, authorizationRequest);
    }

    @Override
    public void setRefreshTokenParameters(@NonNull final TokenRequest refreshTokenRequest,
                                   @NonNull final RefreshTokenRecord refreshToken,
                                   @NonNull final SilentTokenCommandParameters parameters) {
        // isNAA request, set hub/brk and nested app parameters
        super.setRefreshTokenParameters(refreshTokenRequest, refreshToken, parameters);
        refreshTokenRequest.setClientId(parameters.getChildClientId());
        refreshTokenRequest.setBrkClientId(parameters.getClientId());
        refreshTokenRequest.setRedirectUri(parameters.getChildRedirectUri());
        refreshTokenRequest.setBrkRedirectUri(parameters.getRedirectUri());

    }

    @Override
    public void initializeAuthorizationRequestBuilder( @SuppressWarnings(WarningType.rawtype_warning) @NonNull final AuthorizationRequest.Builder builder,
                                                                                       @NonNull final TokenCommandParameters parameters) {
        super.initializeAuthorizationRequestBuilder(builder, parameters);
        builder.setBrkRedirectUri(parameters.getRedirectUri())
                .setBrkClientId(parameters.getClientId())
                .setClientId(parameters.getChildClientId())
                .setRedirectUri(parameters.getChildRedirectUri());

    }
}
