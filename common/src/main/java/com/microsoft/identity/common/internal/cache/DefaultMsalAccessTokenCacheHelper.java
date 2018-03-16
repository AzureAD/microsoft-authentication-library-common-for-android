package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.internal.dto.AccessToken;
import com.microsoft.identity.common.internal.dto.Credential;

/**
 * Default class for creating cache keys and values for {@link Credential} objects.
 */
public class DefaultMsalAccessTokenCacheHelper extends AbstractCacheHelper<AccessToken> {

    @Override
    public String createCacheKey(AccessToken accessToken) {
        // TODO
        return null;
    }

}
