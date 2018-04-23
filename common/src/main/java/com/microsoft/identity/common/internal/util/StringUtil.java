package com.microsoft.identity.common.internal.util;

/**
 * String utilities
 */
public class StringUtil {
    public static boolean isEmpty(final String message) {
        return message == null || message.trim().length() == 0;
    }
}
