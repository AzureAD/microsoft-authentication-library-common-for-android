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

import static com.microsoft.identity.common.java.AuthenticationConstants.ENCODING_UTF8;
import static com.microsoft.identity.common.java.AuthenticationConstants.ENCODING_UTF8_STRING;

import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.util.ported.ObjectUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Pattern;

import cz.msebera.android.httpclient.extras.Base64;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NonNull;

public class StringUtil {
    private static String TAG = StringUtil.class.getSimpleName();

    private static final String RFC3339_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private static final String TOKEN_HASH_ALGORITHM = "SHA256";

    /**
     * Checks if string is null or empty.
     *
     * @param message String to check for null or blank.
     * @return true, if the string is null or blank.
     */
    public static boolean isNullOrEmpty(final @Nullable String message) {
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
        return ObjectUtils.equals(one, two) || (one != null && one.equalsIgnoreCase(two));
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
        } else {
            Logger.warn(TAG, "We had a home account id that could not be split correctly, " +
                    "We expected it to split into " +
                    EXPECTED_LENGTH + " parts but instead we had " + uidUtidArray.length + " when " +
                    "splitting the string on dot ('.')");
            Logger.warnPII(TAG, "We had a home account id that could not be split correctly, " +
                    "Its value was: '" + homeAccountId + "', and we expected it to split into " +
                    EXPECTED_LENGTH + " parts but instead we had " + uidUtidArray.length + " when " +
                    "splitting the string on dot ('.')");
        }

        return new AbstractMap.SimpleEntry<>(uid, utid);
    }

    /**
     * Helper method to get uid from home account id
     * V2 home account format : <uid>.<utid>
     * V1 : it's stored as <uid>
     *
     * @param homeAccountId
     * @return valid uid or null if it's not in either of the format.
     */
    @Nullable
    public static String getUIdFromHomeAccountId(@Nullable String homeAccountId) {
        final String methodName = ":getUIdFromHomeAccountId";
        final String DELIMITER_TENANTED_USER_ID = ".";
        final int EXPECTED_ARGS_LEN = 2;
        final int INDEX_USER_ID = 0;

        if (!StringUtil.isNullOrEmpty(homeAccountId)) {
            final String[] homeAccountIdSplit = homeAccountId.split(
                    Pattern.quote(DELIMITER_TENANTED_USER_ID)
            );

            if (homeAccountIdSplit.length == EXPECTED_ARGS_LEN) {
                Logger.info(TAG + methodName,
                        "Home account id is tenanted, returning uid "
                );
                return homeAccountIdSplit[INDEX_USER_ID];
            } else if (homeAccountIdSplit.length == 1) {
                Logger.info(TAG + methodName,
                        "Home account id not tenanted, it's the uid added by v1 broker "
                );
                return homeAccountIdSplit[INDEX_USER_ID];
            }
        }

        Logger.warn(TAG + methodName,
                "Home Account id doesn't have uid or tenant id information, returning null "
        );

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
     * Split the input string into a list of string tokens.
     *
     * @param items     String
     * @param delimiter String
     * @return List<String>
     */
    public static List<String> getStringTokens(final String items, final String delimiter) {
        final StringTokenizer tokenizer = new StringTokenizer(items, delimiter);
        final List<String> itemList = new ArrayList<>();
        while (tokenizer.hasMoreTokens()) {
            final String name = tokenizer.nextToken();
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
     * @param throwable an throwable object to extract a stack trace string from.
     * @return A stack trace string
     */
    public static String getStackTraceAsString(@NonNull final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return pw.toString();
    }

    public static String fromByteArray(final byte[] bytes) {
        return new String(bytes, ENCODING_UTF8);
    }

    public static byte[] toByteArray(@NonNull final String string) {
        return string.getBytes(ENCODING_UTF8);
    }

    /**
     * Converts a Date object into a RFC3339 formatted date String
     *
     * @param date Date
     * @return String
     */
    @NonNull
    public static String RFC3339DateToString(@NonNull final Date date) {
        final String methodName = "RFC3339DateToString";
        Logger.verbose(TAG + methodName, "RFC3339DateToString is called.");
        final SimpleDateFormat RFC3339DateFormat = new SimpleDateFormat(RFC3339_DATE_FORMAT, Locale.US);
        RFC3339DateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return RFC3339DateFormat.format(date);
    }

    /**
     * Converts a RFC3339 formatted date String into a Date object
     *
     * @param dateStr String
     * @return Date
     * @throws ParseException if the dateStr is formatted incorrectly
     */
    @NonNull
    public static Date RFC3339StringToDate(@NonNull String dateStr) throws ParseException {
        final String methodName = "RFC3339StringToDate";
        Logger.verbose(TAG + methodName, "RFC3339StringToDate is called.");
        final SimpleDateFormat RFC3339DateFormat = new SimpleDateFormat(RFC3339_DATE_FORMAT, Locale.US);
        RFC3339DateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return RFC3339DateFormat.parse(dateStr);
    }

    /**
     * Util method to check if a string is a UUID or not
     *
     * @param inputString : inputString
     * @return true if the inputString is a UUID else false;
     */
    public static boolean isUuid(@NonNull final String inputString) {
        try {
            UUID.fromString(inputString);
            return true;
        } catch (final IllegalArgumentException e) {
            return false;
        }
    }

    public static String encodeUrlSafeString(@NonNull final byte[] bytesToEncode) {
        return Base64.encodeToString(bytesToEncode, Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE);
    }

    public static String encodeUrlSafeString(@NonNull final String stringToEncode) {
        return Base64.encodeToString(toByteArray(stringToEncode), Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE);
    }

    /**
     * Create the Hash string of the message.
     *
     * @param msg String
     * @return String in Hash
     * @throws NoSuchAlgorithmException throws if no such algorithm.
     */
    public static String createHash(final String msg) throws NoSuchAlgorithmException {
        if (!isNullOrEmpty(msg)) {
            final MessageDigest digester = MessageDigest.getInstance(TOKEN_HASH_ALGORITHM);
            final byte[] msgInBytes = msg.getBytes(ENCODING_UTF8);
            return new String(Base64.encode(digester.digest(msgInBytes), Base64.NO_WRAP),
                    ENCODING_UTF8);
        }
        return msg;
    }

    /**
     * Utility to null-safe-compare strings in a case-insensitive manner, trimming both inputs.
     *
     * @param one The first string to compare.
     * @param two The second string to compare.
     * @return true if the inputs are equal, false otherwise.
     */
    public static boolean equalsIgnoreCaseTrimBoth(@Nullable final String one,
                                                   @Nullable final String two) {
        return equalsIgnoreCaseTrim(one != null ? one.trim() : null, two);
    }

    /**
     * Utility to null-safe-compare strings in a case-insensitive manner, trimming the second input.
     *
     * @param one The first string to compare.
     * @param two The second string to compare.
     * @return true if the inputs are equal, false otherwise.
     */
    @SuppressFBWarnings(value = "ES_COMPARING_PARAMETER_STRING_WITH_EQ",
            justification = "This is an intentional reference comparison")
    public static boolean equalsIgnoreCaseTrim(@Nullable final String one, @Nullable final String two) {
        return one == two || (two != null && equalsIgnoreCase(one, two.trim()));
    }

    /**
     * Return an empty string if the input is null.
     *
     * @param input an input string to evaluate.
     * @return an empty string if the input is null, the input otherwise.
     */
    public static String sanitizeNull(final String input) {
        return null == input ? "" : input;
    }

    /**
     * If the input is null, return an empty string. Otherwise, return a trimmed, toLowerCase
     * version of the string in question.
     *
     * @param input a string to evaluate.
     * @return a sanitized version of that string.
     */
    public static String sanitizeNullAndLowercaseAndTrim(final String input) {
        String outValue = null == input ? "" : input.toLowerCase(Locale.US).trim();

        return outValue;
    }

    /**
     * encode string with url form encoding. Space will be +.
     *
     * @param source the string to encode.
     * @return the decoded
     * @throws UnsupportedEncodingException throws if encoding not supported.
     */
    public static String urlFormEncode(String source) throws UnsupportedEncodingException {
        return URLEncoder.encode(source, ENCODING_UTF8_STRING);
    }

    /**
     * Given a byte array, return a base64-encoded String representing that byte array.
     *
     * @param bytes the bytes to encode.
     * @return the Base64 representation of those bytes, without line-wrapping.
     */
    public static String base64Encode(final byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    /**
     * Converts the given String into a rawData byte array, and Base64-decode it.
     */
    public static byte[] base64Decode(@NonNull final String encodedString) {
        return Base64.decode(encodedString, Base64.NO_WRAP);
    }

    /**
     * Converts the given String into a rawData byte array, and Base64-decode it with the url-safe flag.
     */
    public static byte[] base64DecodeUrlSafeString(@NonNull final String encodedString) {
        return Base64.decode(encodedString, Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE);
    }

    /**
     * This is a reimplementation of String.join for the android platform.  Possibly this should
     * shift into PlatformUtils, which could rely on String.join dependent on the android API level.
     *
     * @param separator a separator for the joined strings.
     * @param segments  a set of segments to join.
     * @return a new char sequence constructed by joining the segments with the separator.
     */
    public static <T extends CharSequence> String join(final CharSequence separator, final @NonNull Iterable<T> segments) {
        final Iterator<T> itr = segments.iterator();
        // If the iterable is empty, return empty string.
        if (!itr.hasNext()) {
            return "";
        }
        final T first = itr.next();
        // If the iterable has but one value, return it directly.
        if (!itr.hasNext()) {
            if (first instanceof String) {
                return (String) first;
            }
            return first.toString();
        }
        final StringBuilder sb = new StringBuilder();
        // This iterator must have at least one value, since it isn't empty.
        sb.append(first);
        while (itr.hasNext()) {
            sb.append(separator);
            sb.append(itr.next());
        }
        return sb.toString();
    }

    /**
     * A helper function for validating if the given String is null or empty.
     */
    public static void throwIfArgumentIsNullOrEmpty(final @Nullable String argument,
                                                    final @NonNull String argumentName,
                                                    final @NonNull String methodName) throws NullPointerException {
        if (isNullOrEmpty(argument)) {
            Logger.error(TAG + methodName, argumentName + " is null or empty.", null);
            throw new NullPointerException(argumentName + " is null or empty.");
        }
    }

    /***
     * Helper to perform base64 decoding with logging.
     * @param input Input string
     * @param flags
     * @param failureMessage The message to log in case of failure.
     */
    public static byte[] base64Decode(@NonNull final String input, int flags, @NonNull final String failureMessage) {
        final String methodTag = TAG + ":base64Decode";
        try {
            return Base64.decode(input, flags);
        } catch (IllegalArgumentException e) {
            Logger.error(methodTag, failureMessage + " " + e.getMessage(), null);
            throw e;
        }
    }
}
