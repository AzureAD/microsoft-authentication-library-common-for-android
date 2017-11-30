package com.microsoft.identity.common.internal.cache;

/**
 * Cache items that know how to produce their own keys and can serialize themselves.
 */
interface ISelfSerializingCacheItem {

    /**
     * The cache key for this object.
     *
     * @return The cache key to get.
     */
    String getCacheKey();

    /**
     * The cache value for this object.
     *
     * @return The cache value to get.
     */
    String getCacheValue();
}
