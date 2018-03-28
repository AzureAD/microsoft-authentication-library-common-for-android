package com.microsoft.identity.common.adal.internal.util;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.CommonCoreException;
import com.microsoft.identity.common.exception.CommonCoreExceptionMessage;

import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

public final class StringExtensions {
    /** The Constant ENCODING_UTF8. */
    public static final String ENCODING_UTF8 = "UTF_8";

    private static final String TAG = StringExtensions.class.getSimpleName();

    private static final String TOKEN_HASH_ALGORITHM = "SHA256";

    private StringExtensions() {
        // Intentionally left blank
    }
    /**
     * checks if string is null or empty.
     *
     * @param param String to check for null or blank
     * @return boolean if the string was null or blank
     */
    public static boolean isNullOrBlank(String param) {
        return param == null || param.trim().length() == 0; //NOPMD
    }

    public static String createHash(String msg) throws NoSuchAlgorithmException,
            UnsupportedEncodingException {
        if (!isNullOrBlank(msg)) {
            MessageDigest digester = MessageDigest.getInstance(TOKEN_HASH_ALGORITHM);
            final byte[] msgInBytes = msg.getBytes(AuthenticationConstants.ENCODING_UTF8);
            return new String(Base64.encode(digester.digest(msgInBytes), Base64.NO_WRAP),
                    AuthenticationConstants.ENCODING_UTF8);
        }
        return msg;
    }

    /**
     * encode string with url form encoding. Space will be +
     *
     * @param source the string to encode
     * @return the decoded
     * @throws UnsupportedEncodingException
     */
    public static String urlFormEncode(String source) throws UnsupportedEncodingException {
        return URLEncoder.encode(source, ENCODING_UTF8);
    }

    /**
     * replace + to space and decode.
     *
     * @param source the string to decode
     * @return the encoded string
     * @throws UnsupportedEncodingException
     */
    public static String urlFormDecode(String source) throws UnsupportedEncodingException {

        // Decode everything else
        return URLDecoder.decode(source, ENCODING_UTF8);
    }

    public static String encodeBase64URLSafeString(final byte[] bytes)
            throws UnsupportedEncodingException {
        return new String(
                Base64.encode(bytes, Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE),
                AuthenticationConstants.ENCODING_UTF8);
    }

    /**
     * create url from given endpoint. return null if format is not right.
     *
     * @param endpoint url as a string
     * @return URL object for this string
     */
    public static URL getUrl(String endpoint) {
        URL authority = null;
        try {
            authority = new URL(endpoint);
        } catch (MalformedURLException e1) {
            //Log.e(TAG, e1.getMessage(), "", ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_URL.toString(), e1);
            Log.e(TAG, CommonCoreExceptionMessage.AUTHORITY_URL_NOT_VALID);
        }

        return authority;
    }

    public static HashMap<String, String> getUrlParameters(String finalUrl) {
        Uri response = Uri.parse(finalUrl);
        String fragment = response.getFragment();
        HashMap<String, String> parameters = HashMapExtensions.urlFormDecode(fragment);

        if (parameters == null || parameters.isEmpty()) {
            String queryParameters = response.getEncodedQuery();
            parameters = HashMapExtensions.urlFormDecode(queryParameters);
        }
        return parameters;
    }

    public static List<String> getStringTokens(final String items, final String delimiter) {
        final StringTokenizer st = new StringTokenizer(items, delimiter);
        final List<String> itemList = new ArrayList<>();
        while (st.hasMoreTokens()) {
            String name = st.nextToken();
            if (!isNullOrBlank(name)) {
                itemList.add(name);
            }
        }

        return itemList;
    }

    public static ArrayList<String> splitWithQuotes(String input, char delimiter) {
        final ArrayList<String> items = new ArrayList<>();

        int startIndex = 0;
        boolean insideString = false;
        String item;
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == delimiter && !insideString) {
                item = input.substring(startIndex, i);
                if (!isNullOrBlank(item.trim())) {
                    items.add(item);
                }

                startIndex = i + 1;
            } else if (input.charAt(i) == '"') {
                insideString = !insideString;
            }
        }

        item = input.substring(startIndex);
        if (!isNullOrBlank(item.trim())) {
            items.add(item);
        }

        return items;
    }

    public static String removeQuoteInHeaderValue(String value) {
        if (!isNullOrBlank(value)) {
            return value.replace("\"", "");
        }
        return null;
    }

    /**
     * Checks if header value has this prefix. Prefix + whitespace is
     * acceptable.
     *
     * @param value String to check
     * @param prefix prefix to check the above string
     * @return boolean true if the string starts with prefix and has some body after it.
     */
    public static boolean hasPrefixInHeader(final String value, final String prefix) {
        return value.startsWith(prefix) && value.length() > prefix.length() + 2
                && Character.isWhitespace(value.charAt(prefix.length()));
    }

    public static String base64UrlEncodeToString(final String message) {
        return Base64.encodeToString(message.getBytes(Charset.forName(ENCODING_UTF8)), Base64.URL_SAFE | Base64.NO_WRAP);
    }
}
