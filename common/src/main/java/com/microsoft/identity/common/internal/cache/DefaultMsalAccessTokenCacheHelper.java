package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.internal.dto.AccessToken;
import com.microsoft.identity.common.internal.dto.Credential;

import java.util.ArrayList;
import java.util.List;

/**
 * Default class for creating cache keys and values for {@link Credential} objects.
 */
public class DefaultMsalAccessTokenCacheHelper extends AbstractCacheHelper<AccessToken> {

    @Override
    public String createCacheKey(final AccessToken accessToken) {
        final List<String> keyComponents = new ArrayList<>();
        keyComponents.add(accessToken.getUniqueId());
        keyComponents.add(accessToken.getEnvironment());
        keyComponents.add(accessToken.getCredentialType());
        keyComponents.add(accessToken.getClientId());
        keyComponents.add(accessToken.getRealm());
        keyComponents.add(accessToken.getTarget());

        return collapseKeyComponents(keyComponents);
    }

}
