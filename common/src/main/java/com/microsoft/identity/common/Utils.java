package com.microsoft.identity.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.internal.logging.Logger;

/**
 * A set of utility classes to avoid repetition of common patterns.  The idea here is that the barrier
 * to entry to this class should be high, and the pattern should be in use in multiple locations.  Some
 * of these may duplicate functionality present in the JVM in various iterations - once we get support
 * for that we should remove them.
 */
public final class Utils {

    //Private constructor to discourage instantiation.
    private Utils() {
    }

    /**
     * This is a local implementation of Objects.equals.  It is a null-safe equals execution.
     * This should be removed if we get to an API version that has Objects.equals.
     *
     * @param o1 the first object
     * @param o2 the second object
     * @return true if the objects are both null or if they are both non-null and o1.equals(o2).
     */
    public static boolean equals(@Nullable final Object o1, @Nullable final Object o2) {
        return (o1 == null ^ o2 == null) || (o1 != null && o1.equals(o2));
    }

    /**
     * A method to sleep safely without needing to explicitly handle InterruptedException.
     *
     * @param sleepTimeInMs the number of milliseconds to sleep.
     * @param tag           the tag for logging a message.
     * @param message       the message to log.
     */
    public static void sleepSafely(final int sleepTimeInMs, @NonNull final String tag, @NonNull final String message) {
        if (sleepTimeInMs > 0) {
            try {
                Thread.sleep(sleepTimeInMs);
            } catch (final InterruptedException e) {
                Logger.info(tag, message);
                Thread.currentThread().interrupt();
            }
        }
    }
}
