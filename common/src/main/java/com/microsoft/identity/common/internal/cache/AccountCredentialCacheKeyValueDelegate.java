package com.microsoft.identity.common.internal.cache;

import com.google.gson.Gson;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.dto.AccessToken;
import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.RefreshToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Uses Gson to serialize instances of <T> into {@link String}s.
 */
public class AccountCredentialCacheKeyValueDelegate implements IAccountCredentialCacheKeyValueDelegate {

    public static final String CACHE_VALUE_SEPARATOR = "-";

    private final Gson mGson;

    public AccountCredentialCacheKeyValueDelegate() {
        mGson = new Gson();
    }


    protected final String collapseKeyComponents(final List<String> keyComponents) {
        String cacheKey = "";

        for (String keyComponent : keyComponents) {
            if (!StringExtensions.isNullOrBlank(keyComponent)) {
                keyComponent = keyComponent.toLowerCase(Locale.US);
                cacheKey += keyComponent + CACHE_VALUE_SEPARATOR;
            }
        }

        if (cacheKey.endsWith(CACHE_VALUE_SEPARATOR)) {
            cacheKey = cacheKey.substring(0, cacheKey.length() - 1);
        }

        return cacheKey;
    }

    @Override
    public String generateCacheKey(Account account) {
        final List<String> keyComponents = new ArrayList<>();
        keyComponents.add(account.getUniqueId());
        keyComponents.add(account.getEnvironment());
        keyComponents.add(account.getRealm());

        return collapseKeyComponents(keyComponents);
    }

    @Override
    public String generateCacheValue(Account account) {
        return mGson.toJson(account);
    }

    @Override
    public String generateCacheKey(Credential credential) {
        final List<String> keyComponents = new ArrayList<>();
        keyComponents.add(credential.getUniqueId());
        keyComponents.add(credential.getEnvironment());
        keyComponents.add(credential.getCredentialType());
        keyComponents.add(credential.getClientId());

        if (credential instanceof AccessToken) {
            keyComponents.add(((AccessToken) credential).getRealm());
            keyComponents.add(((AccessToken) credential).getTarget());
        } else if (credential instanceof RefreshToken) {
            keyComponents.add(((RefreshToken) credential).getTarget());
        }

        return collapseKeyComponents(keyComponents);
    }

    @Override
    public String generateCacheValue(Credential credential) {
        // TODO serialize extra fields?
        return mGson.toJson(credential);
    }

    @Override
    public <T> T fromCacheValue(String string, Class<T> t) {
        // TODO serialize extra fields?
        return mGson.fromJson(string, t);
    }
}
