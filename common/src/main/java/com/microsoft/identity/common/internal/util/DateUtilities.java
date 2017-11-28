package com.microsoft.identity.common.internal.util;

import java.util.Date;

public class DateUtilities {

    private DateUtilities() { }
    /**
     * Create a copy of a date
     * to avoid exposing the internal references.
     */
    public static Date createCopy(final Date date) {
        if (date != null) {
            return new Date(date.getTime());
        }

        return date;
    }
}

