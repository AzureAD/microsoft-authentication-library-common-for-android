package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.internal.dto.AccessToken;
import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.RefreshToken;

/**
 * Provides helpers and factories to the AccountCredentialCache.
 */
public interface ICacheConfiguration {

    /**
     * Gets the {@link Account} cache helper;
     *
     * @return The cache helper to get.
     */
    ICacheHelper<Account> getAccountCacheHelper();

    /**
     * Gets the {@link AccessToken} cache helper.
     *
     * @return The cache helper to get.
     */
    ICacheHelper<AccessToken> getAccessTokenCacheHelper();

    /**
     * Gets the {@link RefreshToken} cache helper.
     *
     * @return The cache helper to get.
     */
    ICacheHelper<RefreshToken> getRefreshTokenCacheHelper();

    /**
     * Gets the {@link Account} factory.
     *
     * @return The Account factory to get.
     */
    IAccountFactory getAccountFactory();

    /**
     * Gets the {@link Credential} factory.
     *
     * @return The Credential factory to get.
     */
    ICredentialFactory getCredentialFactory();

}
