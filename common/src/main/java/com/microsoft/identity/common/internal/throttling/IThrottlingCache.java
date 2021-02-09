package com.microsoft.identity.common.internal.throttling;

import androidx.annotation.Nullable;

import com.microsoft.identity.common.internal.providers.oauth2.TokenErrorResponse;

public interface IThrottlingCache {

    void saveThrottlingInfoForRequest(final Request request, final ThrottlingInfo throttlingInfo);

    void saveErrorResponseForRequest(final Request request, final TokenErrorResponse tokenErrorResponse);

    @Nullable
    ThrottlingInfo loadThrottlingForRequest(final Request request);

    @Nullable
    TokenErrorResponse loadErrorResponseForRequest(final Request request);
}
