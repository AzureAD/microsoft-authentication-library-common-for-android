package com.microsoft.identity.common.internal.cache;

import java.util.List;

/**
 * A generic caching interface.
 */
public interface ISimpleCache<T> {

    /**
     * Inserts a new T into the cache.
     *
     * @param t The item to insert.
     * @return True, if inserted. False otherwise.
     */
    boolean insert(T t);

    /**
     * Removes an existing T from the cache.
     *
     * @param t The item to remove.
     * @return True if removed or does not exist. False otherwise.
     */
    boolean remove(T t);

    /**
     * Gets all entries in the cache.
     *
     * @return The List of cache entries. May be empty, never null.
     */
    List<T> getAll();

    /**
     * Removes all entries in the cache.
     *
     * @return True if the cache has been successfully cleared. False otherwise.
     */
    boolean clear();
}
