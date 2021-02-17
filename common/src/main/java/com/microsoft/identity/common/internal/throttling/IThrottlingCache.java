package com.microsoft.identity.common.internal.throttling;

import androidx.annotation.Nullable;

import com.microsoft.identity.common.internal.providers.oauth2.TokenErrorResponse;

public interface IThrottlingCache {

    void saveThrottlingInfoForRequest(final Request request, final ThrottlingInfo throttlingInfo);

    @Nullable
    ThrottlingInfo loadThrottlingInfoForRequest(final Request request);
}
