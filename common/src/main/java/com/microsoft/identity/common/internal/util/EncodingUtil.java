package com.microsoft.identity.common.internal.util;


import android.util.Base64;

import java.nio.charset.Charset;

import static com.microsoft.identity.common.adal.internal.util.StringExtensions.ENCODING_UTF8;

public class EncodingUtil {

    public static String base64UrlEncodeToString(final String message) {
        return Base64.encodeToString(message.getBytes(Charset.forName(ENCODING_UTF8)), Base64.URL_SAFE | Base64.NO_WRAP);
    }

}
