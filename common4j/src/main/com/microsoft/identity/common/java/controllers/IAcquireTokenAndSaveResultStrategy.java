package com.microsoft.identity.common.java.controllers;

import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.TokenCommandParameters;
import com.microsoft.identity.common.java.dto.RefreshTokenRecord;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.java.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;

import java.util.List;


import lombok.NonNull;

public interface IAcquireTokenAndSaveResultStrategy {


    List<ICacheRecord> getCacheRecord(@NonNull final MicrosoftStsTokenResponse microsoftStsTokenResponse,
                                      @NonNull final MicrosoftStsOAuth2Strategy oAuth2Strategy,
                                      @NonNull final MicrosoftStsAuthorizationRequest authorizationRequest);


    List<ICacheRecord> constructRenewATResultFromTokenResult(@NonNull MicrosoftStsOAuth2Strategy oAuth2Strategy,
                                                             @NonNull MicrosoftStsAuthorizationRequest authorizationRequest,
                                                             @NonNull TokenResult tokenResult, @SuppressWarnings(WarningType.rawtype_warning)
                                                             @NonNull OAuth2TokenCache tokenCache
    ) throws ClientException;

    void setRefreshTokenParameters(@NonNull final TokenRequest refreshTokenRequest,
                                   @NonNull final RefreshTokenRecord refreshToken,
                                   @NonNull final SilentTokenCommandParameters parameters);

    void initializeAuthorizationRequestBuilder(@SuppressWarnings(WarningType.rawtype_warning) @NonNull final AuthorizationRequest.Builder builder,
                                               @NonNull final TokenCommandParameters parameters);

}
