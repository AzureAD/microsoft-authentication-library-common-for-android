package com.microsoft.identity.common.internal.cache;

/**
 * This is an analoge of java.util.Function.Predicate, and exposes a single method evaluating a
 * candidate of the appropriate type and turning true if this candidate satisfies the predicate
 * and false otherwise.
 * @param <T> the type of value being evaluated.
 */
public interface Predicate<T> {
    /**
     * Evaluate the candidate value.
     * @param value the value to examine.
     * @return true if this candidate is acceptable according to the predicate, otherwise false.
     */
    boolean test(T value);
}
