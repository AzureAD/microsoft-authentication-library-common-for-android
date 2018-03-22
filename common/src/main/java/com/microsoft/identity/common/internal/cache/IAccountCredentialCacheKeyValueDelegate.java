package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.Credential;

public interface IAccountCredentialCacheKeyValueDelegate {

    String generateCacheKey(final Account account);

    String generateCacheValue(final Account account);

    String generateCacheKey(final Credential credential);

    String generateCacheValue(final Credential credential);

    <T> T fromCacheValue(final String string, Class<T> t);

}
