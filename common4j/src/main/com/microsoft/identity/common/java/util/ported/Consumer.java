package com.microsoft.identity.common.java.util.ported;

/**
 * Ported from {@link java.util.function.Consumer}
 */
public interface Consumer<T> {

    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     */
    void accept(T t);
}
