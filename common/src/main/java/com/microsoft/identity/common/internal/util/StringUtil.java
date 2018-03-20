package com.microsoft.identity.common.internal.util;

/**
 * Util class for string related tasks.
 */
public class StringUtil {
    /**
     * checks if string is null or empty.
     *
     * @param string String to check for null or blank
     * @return boolean if the string was null or blank
     */
    public static boolean isEmpty(final String string) {
        return string == null || string.trim().length() == 0;
    }
}
