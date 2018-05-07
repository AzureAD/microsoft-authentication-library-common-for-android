package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;

/**
 * Hooks to allow cache implementors to validate inputs contain requisite information.
 */
public interface ISsoValidator {

    /**
     * Method to verify {@link Account} instances are valid prior to writing to the cache.
     *
     * @param account The Account to verify.
     * @return True, if the Account is valid. False otherwise.
     */
    boolean isAccountValid(final Account account);

    /**
     * Method to verify {@link RefreshToken} instances are valid prior to writing to the cache.
     *
     * @param refreshToken The RefreshToken to inspect.
     * @return True, if the RefreshToken is valid. False otherwise.
     */
    boolean isRefreshTokenValid(final RefreshToken refreshToken);
}
