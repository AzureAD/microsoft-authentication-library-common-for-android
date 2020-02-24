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
package com.microsoft.identity.common.internal.util;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * String utilities.
 */
public final class StringUtil {

    private StringUtil() {
        // Utility class.
    }

    /**
     * Check if the string is null or empty.
     *
     * @param message String
     * @return true if the string object is null or the string is empty.
     */
    public static boolean isEmpty(final String message) {
        return message == null || message.trim().length() == 0; //NOPMD  Suppressing PMD warning for new String creation on trim()"
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

    public static String join(final char delimiter, @NonNull final List<String> toJoin) {
        StringBuilder builder = new StringBuilder();

        char tempDelimiter = Character.MIN_VALUE;

        for (String s : toJoin) {
            if (tempDelimiter != Character.MIN_VALUE) {
                builder.append(tempDelimiter);
            }

            tempDelimiter = delimiter;
            builder.append(s);
        }

        return builder.toString();
    }

    /**
     * Parses the supplied home_account_id to extract the uid, utid.
     *
     * @param homeAccountId The home_account_id to inspect.
     * @return A Pair of Strings representing the uid/utid of the supplied home_account_id.
     * Return value cannot be null, but its values (pair.first, pair.second) may be.
     */
    public static Pair<String, String> getTenantInfo(@NonNull final String homeAccountId) {
        // Split this value by its parts... <uid>.<utid>
        final int EXPECTED_LENGTH = 2;
        final int INDEX_UID = 0;
        final int INDEX_UTID = 1;

        final String[] uidUtidArray = homeAccountId.split("\\.");

        String uid = null;
        String utid = null;

        if (EXPECTED_LENGTH == uidUtidArray.length
                && !StringExtensions.isNullOrBlank(uidUtidArray[INDEX_UID])
                && !StringExtensions.isNullOrBlank(uidUtidArray[INDEX_UTID])) {
            uid = uidUtidArray[INDEX_UID];
            utid = uidUtidArray[INDEX_UTID];
        }

        return new Pair<>(uid, utid);
    }

    /**
     * The function to compare the two versions.
     *
     * @param thisVersion
     * @param thatVersion
     * @return int -1 if thisVersion is smaller than thatVersion,
     * 1 if thisVersion is larger than thatVersion,
     * 0 if thisVersion is equal to thatVersion.
     */
    public static int compareSemanticVersion(@NonNull final String thisVersion,
                                             @Nullable final String thatVersion) {
        if (thatVersion == null) {
            return 1;
        }

        final String[] thisParts = thisVersion.split("\\.");
        final String[] thatParts = thatVersion.split("\\.");

        final int length = Math.max(thisParts.length, thatParts.length);

        for (int i = 0; i < length; i++) {
            int thisPart = i < thisParts.length ? Integer.parseInt(thisParts[i]) : 0;
            int thatPart = i < thatParts.length ? Integer.parseInt(thatParts[i]) : 0;

            if (thisPart < thatPart) {
                return -1;
            }

            if (thisPart > thatPart) {
                return 1;
            }
        }

        return 0;
    }

    /**
     * Counts the number of occurrences of one String in another.
     *
     * @param str
     * @param subString
     * @return int
     */
    public static int countMatches(@NonNull final String str, @Nullable final String subString) {
        int count = 0;

        if (StringUtil.isEmpty(str) || StringUtil.isEmpty(subString)) {
            return count;
        }

        for (int i = 0; i <= (str.length() - subString.length()); i++) {
            if (str.substring(i, i + subString.length()).equalsIgnoreCase(subString)) {
                count++;
            }
        }

        return count;
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

}
