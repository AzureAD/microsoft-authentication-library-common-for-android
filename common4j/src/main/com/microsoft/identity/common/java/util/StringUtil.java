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
package com.microsoft.identity.common.java.util;

import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.logging.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

import static com.microsoft.identity.common.java.AuthenticationConstants.ENCODING_UTF8;
import static com.microsoft.identity.common.java.AuthenticationConstants.ENCODING_UTF8_STRING;

public class StringUtil {
    private static String TAG = StringUtil.class.getSimpleName();

    /**
     * Checks if string is null or empty.
     *
     * @param message String to check for null or blank.
     * @return true, if the string is null or blank.
     */
    public static boolean isNullOrEmpty(String message) {
        return message == null || message.trim().length() == 0;
    }

    /**
     * Returns true if the string contains the given substring.
     *
     * @param str    the string to search for a substring in
     * @param substr the substring to search for
     * @return true if the string contains the given substring, false if it does not or if it is null
     */
    public static boolean containsSubString(final String str, final String substr) {
        if (isNullOrEmpty(str)) {
            return false;
        }

        return str.contains(substr);
    }

    /**
     * Utility to null-safe-compare string in a case-insensitive manner.
     *
     * @param one The first string to compare.
     * @param two The second string to compare.
     */
    public static boolean equalsIgnoreCase(@Nullable final String one, @Nullable final String two) {
        return Objects.equals(one, two) || (one != null && one.equalsIgnoreCase(two));
    }

    /**
     * Parses the supplied home_account_id to extract the uid, utid.
     *
     * @param homeAccountId The home_account_id to inspect.
     * @return A Pair of Strings representing the uid/utid of the supplied home_account_id.
     * Return value cannot be null, but its values (pair.first, pair.second) may be.
     */
    public static Map.Entry<String, String> getTenantInfo(@NonNull final String homeAccountId) {
        // Split this value by its parts... <uid>.<utid>
        final int EXPECTED_LENGTH = 2;
        final int INDEX_UID = 0;
        final int INDEX_UTID = 1;

        final String[] uidUtidArray = homeAccountId.split("\\.");

        String uid = null;
        String utid = null;

        if (EXPECTED_LENGTH == uidUtidArray.length
                && !isNullOrEmpty(uidUtidArray[INDEX_UID])
                && !isNullOrEmpty(uidUtidArray[INDEX_UTID])) {
            uid = uidUtidArray[INDEX_UID];
            utid = uidUtidArray[INDEX_UTID];
        }

        return new AbstractMap.SimpleEntry<>(uid, utid);
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
            if (!isNullOrEmpty(name)) {
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
                if (!isNullOrEmpty(item.trim())) {
                    items.add(item);
                }

                startIndex = i + 1;
            } else if (input.charAt(i) == '"') {
                insideString = !insideString;
            }
        }

        item = input.substring(startIndex);
        if (!isNullOrEmpty(item.trim())) {
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
        if (!isNullOrEmpty(value)) {
            return value.replace("\"", "");
        }
        return null;
    }

    /**
     * Perform URL decode on the given source.
     *
     * @param source The String to decode for.
     * @return The decoded string.
     * @throws UnsupportedEncodingException If encoding is not supported.
     */
    public static String urlFormDecode(final String source) throws UnsupportedEncodingException {
        if (isNullOrEmpty(source)) {
            return "";
        }

        return URLDecoder.decode(source, ENCODING_UTF8_STRING);
    }

    /**
     * Get a string from the given exception.
     *
     * @param exception an exception object to extract a stack trace string from.
     * @return A stack trace string
     */
    public static String getStackTraceAsString(@NonNull final Exception exception) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        return pw.toString();
    }

    public static String fromByteArray(@NonNull final byte[] bytes){
        return new String(bytes, ENCODING_UTF8);
    }

    public static byte[] toByteArray(@NonNull final String string){
        return string.getBytes(ENCODING_UTF8);
    }
}
