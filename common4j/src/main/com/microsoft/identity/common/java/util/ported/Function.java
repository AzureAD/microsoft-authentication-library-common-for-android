package com.microsoft.identity.common.java.util.ported;

/**
 * Ported from {@link java.util.function.Function}
 */
public interface Function<T, R> {
    /**
     * Applies this function to the given argument.
     * @param t the function argument
     * @return the function result
     */
    R apply(T t);
}
