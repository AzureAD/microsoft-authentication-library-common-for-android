package com.microsoft.identity.common.internal.cache;

interface ICacheValueDelegate<T> {

    String generateCacheKey(final T t);

    String generateCacheValue(final T t);

    T fromCacheValue(final String string);

}
