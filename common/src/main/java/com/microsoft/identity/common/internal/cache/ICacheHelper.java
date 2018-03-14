package com.microsoft.identity.common.internal.cache;

/**
 * Interface for convenience classes to make cache keys/value from T types.
 *
 * @param <T> The target object to cache.
 */
public interface ICacheHelper<T> {

    /**
     * Creates a cache key for the supplied object.
     *
     * @param t The object from which the cache key should derive.
     * @return The newly created cache key.
     */
    String createCacheKey(final T t);

    /**
     * Gets the serialized form of the supplied object for caching.
     *
     * @param t The object to cache.
     * @return The cache value equivalent of the supplied object.
     */
    String getCacheValue(final T t);
}
