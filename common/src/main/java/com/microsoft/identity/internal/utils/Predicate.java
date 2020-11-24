package com.microsoft.identity.internal.utils;

/**
 * Generic functional interface to give caller flexibility of code to evaluate on the given
 * arguments.
 *
 * @param <T> input arguments to evaluate.
 */
public interface Predicate<T> {
    boolean test(T t);

    /**
     * Utility methods on predicate combinations.
     */
    class Ops {
        /**
         * Construct a new Predicate that is the combination of AND--
         * @param others
         * @param <T>
         * @return
         */
        public static <T> Predicate<T> AND(final Predicate <T>... others) {
            return new Predicate<T>() {
                public boolean test(T t) {
                    for (Predicate<T> predicate : others) {
                        if (!predicate.test(t)) {
                            return false;
                        }
                    }
                    return true;
                }
            };
        }
        public static <T> Predicate<T> OR(final Predicate <T>... others) {
            return new Predicate<T>() {
                public boolean test(T t) {
                    for (Predicate<T> predicate : others) {
                        if (predicate.test(t)) {
                            return true;
                        }
                    }
                    return false;
                }
            };
        }
    }
}
