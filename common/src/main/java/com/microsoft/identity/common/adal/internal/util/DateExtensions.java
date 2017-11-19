package com.microsoft.identity.common.adal.internal.util;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;

import android.net.Uri;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

public final class DateExtensions {
    /**
     * Private constructor to prevent the class from being initiated.
     */
    private DateExtensions() { }

    /**
     * Create an immutable object for the input Date object
     * to avoid exposing the internal references.
     */
    public static Date createCopy(final Date date) {
        if (date != null) {
            return new Date(date.getTime());
        }

        return date;
    }


}

