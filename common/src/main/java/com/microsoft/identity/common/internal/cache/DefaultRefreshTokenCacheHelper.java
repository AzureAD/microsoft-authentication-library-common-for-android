package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.internal.dto.RefreshToken;

import java.util.ArrayList;
import java.util.List;

public class DefaultRefreshTokenCacheHelper extends AbstractCacheHelper<RefreshToken> {

    @Override
    public String createCacheKey(RefreshToken refreshToken) {
        final List<String> keyComponents = new ArrayList<>();
        keyComponents.add(refreshToken.getUniqueId());
        keyComponents.add(refreshToken.getEnvironment());
        keyComponents.add(refreshToken.getCredentialType());
        keyComponents.add(refreshToken.getClientId());
        keyComponents.add(refreshToken.getTarget());

        return collapseKeyComponents(keyComponents);
    }
}
