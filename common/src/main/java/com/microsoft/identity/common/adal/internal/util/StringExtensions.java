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

import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.logging.Logger;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public final class StringExtensions {
    /**
     * The constant ENCODING_UTF8.
     */
    public static final String ENCODING_UTF8 = "UTF-8";

    private static final String TAG = StringExtensions.class.getSimpleName();

    private static final String TOKEN_HASH_ALGORITHM = "SHA256";

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
            final byte[] msgInBytes = msg.getBytes(ENCODING_UTF8);
            return new String(Base64.encode(digester.digest(msgInBytes), Base64.NO_WRAP),
                    ENCODING_UTF8);
        }
        return msg;
    }

    /**
     * Encode string with url form encoding. Space will be replaced by '+'.
     *
     * @param source the string to encode.
     * @return the decoded string.
     * @throws UnsupportedEncodingException throws if encoding not supported.
     */
    public static String urlFormEncode(String source) throws UnsupportedEncodingException {
        return URLEncoder.encode(source, ENCODING_UTF8);
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
        return URLDecoder.decode(source, ENCODING_UTF8);
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
                ENCODING_UTF8);
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
     * @return HashMap
     */
    public static HashMap<String, String> getUrlParameters(String finalUrl) {
        final String methodTag = TAG + ":getUrlParameters";
        Uri response = Uri.parse(finalUrl);
        if (!HashMapExtensions.urlFormDecode(response.getFragment()).isEmpty()) {
            Logger.warn(methodTag, "Received url contains unexpected fragment parameters.");
            Logger.warnPII(methodTag, "Unexpected fragment: " + response.getFragment());
        }

        return HashMapExtensions.urlFormDecode(response.getEncodedQuery());
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
        return Base64.encodeToString(message.getBytes(Charset.forName(ENCODING_UTF8)), Base64.URL_SAFE | Base64.NO_WRAP);
    }

    /**
     * Append parameter to the url. If the no query parameters, return the url originally passed in.
     */
    public static String appendQueryParameterToUrl(final String url, final Map<String, String> requestParams)
            throws UnsupportedEncodingException {
        if (isNullOrBlank(url)) {
            throw new IllegalArgumentException("Empty authority endpoint parameter.");
        }

        if (requestParams.isEmpty()) {
            return url;
        }

        Uri.Builder builtUri = Uri.parse(url).buildUpon();
        for (Map.Entry<String, String> entry : requestParams.entrySet()) {
            builtUri.appendQueryParameter(entry.getKey(), entry.getValue());
        }

        return builtUri.build().toString();
    }

    /**
     * Remove query parameter from URL.
     */
    public static String removeQueryParameterFromUrl(final String url) throws URISyntaxException {
        final URI uri = new URI(url);
        return new URI(uri.getScheme(),
                uri.getAuthority(),
                uri.getPath(),
                null,
                uri.getFragment()).toString();
    }
}
