// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.adal.internal.util;

import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.logging.Logger;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

public final class StringExtensions {
    private static final String TAG = StringExtensions.class.getSimpleName();

    private static final String TOKEN_HASH_ALGORITHM = "SHA256";

    private static final String QUERY_STRING_SYMBOL = "?";
    private static final String QUERY_STRING_DELIMITER = "&";

    private StringExtensions() {
        // Intentionally left blank
    }

    /**
     * checks if string is null or empty.
     *
     * @param param String to check for null or blank.
     * @return boolean if the string was null or blank.
     */
    public static boolean isNullOrBlank(String param) {
        return param == null || param.trim().length() == 0; //NOPMD
    }

    /**
     * Create the Hash string of the message.
     *
     * @param msg String
     * @return String in Hash
     * @throws NoSuchAlgorithmException     throws if no such algorithm.
     * @throws UnsupportedEncodingException throws if encoding not supported.
     */
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
     * encode string with url form encoding. Space will be +.
     *
     * @param source the string to encode.
     * @return the decoded
     * @throws UnsupportedEncodingException throws if encoding not supported.
     */
    public static String urlFormEncode(String source) throws UnsupportedEncodingException {
        return URLEncoder.encode(source, AuthenticationConstants.ENCODING_UTF8);
    }

    /**
     * replace + to space and decode.
     *
     * @param source the string to decode.
     * @return the encoded string.
     * @throws UnsupportedEncodingException throws if encoding not supported.
     */
    public static String urlFormDecode(String source) throws UnsupportedEncodingException {

        // Decode everything else
        return URLDecoder.decode(source, AuthenticationConstants.ENCODING_UTF8);
    }

    /**
     * Encode Base64 URL Safe String.
     *
     * @param bytes byte[]
     * @return String
     * @throws UnsupportedEncodingException throws if encoding not supported.
     */
    public static String encodeBase64URLSafeString(final byte[] bytes)
            throws UnsupportedEncodingException {
        return new String(
                Base64.encode(bytes, Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE),
                AuthenticationConstants.ENCODING_UTF8);
    }

    /**
     * create url from given endpoint. return null if format is not right.
     *
     * @param endpoint url as a string.
     * @return URL object for this string.
     */
    public static URL getUrl(String endpoint) {
        URL authority = null;
        try {
            authority = new URL(endpoint);
        } catch (MalformedURLException e1) {
            //Log.e(TAG, e1.getMessage(), "", ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_URL.toString(), e1);
            Log.e(TAG, ErrorStrings.AUTHORITY_URL_NOT_VALID);
        }

        return authority;
    }

    /**
     * Get URL parameters from final url.
     *
     * @param finalUrl String
     * @return HashMap<String ,   String>
     */
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

    /**
     * Split the input string into a list of string tokens.
     *
     * @param items     String
     * @param delimiter String
     * @return List<String>
     */
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

    /**
     * Split the input with delimiter.
     *
     * @param input     String
     * @param delimiter char
     * @return ArrayList<String>
     */
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

    /**
     * Remove quote in header value.
     *
     * @param value String
     * @return String
     */
    public static String removeQuoteInHeaderValue(String value) {
        if (!isNullOrBlank(value)) {
            return value.replace("\"", "");
        }
        return null;
    }

    /**
     * Checks if header value has this prefix. Prefix + whitespace is acceptable.
     *
     * @param value  String to check.
     * @param prefix prefix to check the above string.
     * @return boolean true if the string starts with prefix and has some body after it.
     */
    public static boolean hasPrefixInHeader(final String value, final String prefix) {
        return value.startsWith(prefix) && value.length() > prefix.length() + 2
                && Character.isWhitespace(value.charAt(prefix.length()));
    }

    /**
     * Based64URL encode the input string.
     *
     * @param message String
     * @return String
     */
    public static String base64UrlEncodeToString(final String message) {
        return Base64.encodeToString(message.getBytes(Charset.forName(AuthenticationConstants.ENCODING_UTF8)), Base64.URL_SAFE | Base64.NO_WRAP);
    }

    /**
     * Convert the given set of scopes into the string with the provided delimiter.
     *
     * @param inputSet  The Set of scopes to convert.
     * @param delimiter The delimiter used to construct the scopes in the format of String.
     * @return The converted scopes in the format of String.
     */
    public static String convertSetToString(final Set<String> inputSet, final String delimiter) {
        if (inputSet == null || inputSet.isEmpty() || delimiter == null) {
            return "";
        }

        final StringBuilder stringBuilder = new StringBuilder();
        final Iterator<String> iterator = inputSet.iterator();
        stringBuilder.append(iterator.next());

        while (iterator.hasNext()) {
            stringBuilder.append(delimiter);
            stringBuilder.append(iterator.next());
        }

        return stringBuilder.toString();
    }

    /**
     * Decode the given url, and convert it into map with the given delimiter.
     *
     * @param url       The url to decode for.
     * @param delimiter The delimiter used to parse the url string.
     * @return The Map of the items decoded with the given delimiter.
     */
    public static Map<String, String> decodeUrlToMap(final String url, final String delimiter) {
        final Map<String, String> decodedUrlMap = new HashMap<>();

        // delimiter can be " "
        if (isNullOrBlank(url) || delimiter == null) {
            return decodedUrlMap;
        }

        final StringTokenizer tokenizer = new StringTokenizer(url, delimiter);
        while (tokenizer.hasMoreTokens()) {
            final String pair = tokenizer.nextToken();
            final String[] elements = pair.split("=");

            if (elements.length != 2) {
                continue;
            }

            try {
                final String key = urlFormDecode(elements[0]);
                final String value = urlFormDecode(elements[1]);

                if (!isNullOrBlank(key) && !isNullOrBlank(value)) {
                    decodedUrlMap.put(key, value);
                }
            } catch (final UnsupportedEncodingException e) {
                Logger.error(TAG, null, "URL form decode failed.", e);
            }
        }

        return decodedUrlMap;
    }

    /**
     * Append parameter to the url. If the no query parameters, return the url originally passed in.
     */
    public static String appendQueryParameterToUrl(final String url, final Map<String, String> requestParams)
            throws UnsupportedEncodingException {
        if (isNullOrBlank(url)) {
            throw new IllegalArgumentException("Empty authority string");
        }

        if (requestParams.isEmpty()) {
            return url;
        }

        final Set<String> queryParamsSet = new HashSet<>();
        for (Map.Entry<String, String> entry : requestParams.entrySet()) {
            queryParamsSet.add(entry.getKey() + "=" + urlFormEncode(entry.getValue()));
        }

        final String queryString = convertSetToString(queryParamsSet, QUERY_STRING_DELIMITER);
        final String queryStringFormat;
        if (url.contains(QUERY_STRING_SYMBOL)) {
            queryStringFormat = url.endsWith(QUERY_STRING_DELIMITER) ? "%s%s" : "%s" + QUERY_STRING_DELIMITER + "%s";
        } else {
            queryStringFormat = "%s" + QUERY_STRING_SYMBOL + "%s";
        }

        return String.format(queryStringFormat, url, queryString);
    }
}
