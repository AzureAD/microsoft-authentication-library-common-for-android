package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.internal.dto.Credential;

/**
 * Default class for creating cache keys and values for {@link Credential} objects.
 */
public class DefaultCredentialCacheHelper implements ICacheHelper<Credential> {

    @Override
    public String createCacheKey(Credential credential) {
        // TODO
        return null;
    }

    @Override
    public String getCacheValue(Credential credential) {
        // TODO
        return null;
    }
}
