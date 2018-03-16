package com.microsoft.identity.common.internal.cache;

import com.google.gson.Gson;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;

import java.util.List;
import java.util.Locale;

/**
 * Uses Gson to serialize instances of <T> into {@link String}s.
 *
 * @param <T> The type to serialize.
 */
public abstract class AbstractCacheHelper<T> implements ICacheHelper<T> {

    public static final String CACHE_VALUE_SEPARATOR = "-";

    private final Gson mGson;

    AbstractCacheHelper() {
        mGson = new Gson();
    }

    /**
     * Turns the supplied T instance into JSON.
     *
     * @param t The type to transform.
     * @return T as a String of JSON.
     */
    protected final String toJson(T t) {
        return mGson.toJson(t);
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
    public String getCacheValue(T t) {
        return toJson(t);
    }
}
