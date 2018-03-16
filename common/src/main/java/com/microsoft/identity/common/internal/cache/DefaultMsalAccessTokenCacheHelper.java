package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.dto.AccessToken;
import com.microsoft.identity.common.internal.dto.Credential;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Default class for creating cache keys and values for {@link Credential} objects.
 */
public class DefaultMsalAccessTokenCacheHelper extends AbstractCacheHelper<AccessToken> {

    public static final String CACHE_VALUE_SEPARATOR = "-";

    @Override
    public String createCacheKey(final AccessToken accessToken) {
        final List<String> keyComponents = new ArrayList<>();
        keyComponents.add(accessToken.getUniqueId());
        keyComponents.add(accessToken.getEnvironment());
        keyComponents.add(accessToken.getCredentialType());
        keyComponents.add(accessToken.getClientId());
        keyComponents.add(accessToken.getRealm());
        keyComponents.add(accessToken.getTarget());

        String cacheKey = "";

        for (String keyComponent : keyComponents) {
            if (!StringExtensions.isNullOrBlank(keyComponent)) {
                // TODO are cache keys supposed to be lowercased?
                keyComponent = keyComponent.toLowerCase(Locale.US);
                // Append the CACHE_VALUE_SEPARATOR unless this is the last element
                cacheKey += keyComponent + CACHE_VALUE_SEPARATOR;
            }
        }

        if (cacheKey.endsWith(CACHE_VALUE_SEPARATOR)) {
            cacheKey = cacheKey.substring(0, cacheKey.length() - 1);
        }

        return cacheKey;
    }

}
