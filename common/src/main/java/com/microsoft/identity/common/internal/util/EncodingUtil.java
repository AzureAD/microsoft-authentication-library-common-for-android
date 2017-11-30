package com.microsoft.identity.common.internal.util;

import android.util.Base64;

import java.nio.charset.Charset;

import static com.microsoft.identity.common.adal.internal.util.StringExtensions.ENCODING_UTF8;

/**
 * Util class for encoding related tasks.
 */
public class EncodingUtil {

    private EncodingUtil() {
        // Utility class.
    }

    /**
     * Base64 encodes the supplied String.
     *
     * @param message The String to encode.
     * @return The encoded String.
     */
    public static String base64UrlEncodeToString(final String message) {
        return Base64.encodeToString(message.getBytes(Charset.forName(ENCODING_UTF8)), Base64.URL_SAFE | Base64.NO_WRAP);
    }

}
