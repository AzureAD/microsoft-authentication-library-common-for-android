package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.internal.dto.AccountCredentialBase;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.IAccount;

public interface IAccountCredentialCacheKeyValueDelegate {

    String generateCacheKey(final IAccount account);

    String generateCacheValue(final IAccount account);

    String generateCacheKey(final Credential credential);

    String generateCacheValue(final Credential credential);

    <T extends AccountCredentialBase> T fromCacheValue(final String string, Class<? extends AccountCredentialBase> t); // TODO consider throwing an Exception if parsing fails

}
