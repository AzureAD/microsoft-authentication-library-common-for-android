package com.microsoft.identity.common.internal.util;

/**
 * Represents an operation that returns a result, taking no arguments.
 *
 * @param <T> the type of argument returned.
 */
public interface Supplier<T> {
    /**
     * @return a instance of a T.
     */
    T get();
}
