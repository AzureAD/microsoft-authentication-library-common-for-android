package com.microsoft.identity.common.internal.cache;

import com.google.gson.Gson;

/**
 * Uses Gson to serialize instances of <T> into {@link String}s.
 *
 * @param <T> The type to serialize.
 */
abstract class AbstractCacheHelper<T> implements ICacheHelper<T> {

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

    @Override
    public String getCacheValue(T t) {
        return toJson(t);
    }
}
