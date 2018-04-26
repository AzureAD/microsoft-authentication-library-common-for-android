package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;

interface ISsoValidator {

    boolean isAccountValid(final Account account);

    boolean isRefreshTokenValid(final RefreshToken refreshToken);
}
