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

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.logging.Logger;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * String utilities.
 */
public final class StringUtil {
    private static final String TAG = StringUtil.class.getSimpleName();

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
     * Returns true if the string contains the given substring.
     *
     * @param str    the string to search for a substring in
     * @param substr the substring to search for
     * @return true if the string contains the given substring, false if it does not or if it is null
     */
    public static boolean containsSubString(final String str, final String substr) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }

        return str.contains(substr);
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

    public static String join(final char delimiter, @NonNull final Iterable<String> toJoin) {
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
    public static Map.Entry<String, String> getTenantInfo(@NonNull final String homeAccountId) {
        final String methodTag = TAG + ":getTenantInfo";
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
        } else {
            Logger.warn(methodTag, "We had a home account id that could not be split correctly, " +
                    "We expected it to split into " +
                    EXPECTED_LENGTH + " parts but instead we had " + uidUtidArray.length + " when " +
                    "splitting the string on dot ('.')");
            Logger.warnPII(methodTag, "We had a home account id that could not be split correctly, " +
                    "Its value was: '" + homeAccountId + "', and we expected it to split into " +
                    EXPECTED_LENGTH + " parts but instead we had " + uidUtidArray.length + " when " +
                    "splitting the string on dot ('.')");
        }

        return new AbstractMap.SimpleEntry<>(uid, utid);
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
     * Returns true if the first semantic version is smaller or equal to the second version.
     */
    public static boolean isFirstVersionSmallerOrEqual(@NonNull final String first,
                                                       @Nullable final String second) {
        return compareSemanticVersion(first, second) <= 0;
    }

    /**
     * Returns true if the first semantic version is larger or equal to the second version.
     */
    public static boolean isFirstVersionLargerOrEqual(@NonNull final String first,
                                                      @Nullable final String second) {
        return compareSemanticVersion(first, second) >= 0;
    }

    /**
     * Utility to null-safe-compare string in a case-insensitive manner.
     *
     * @param one The first string to compare.
     * @param two The second string to compare.
     */
    @SuppressFBWarnings(value = "ES_COMPARING_PARAMETER_STRING_WITH_EQ",
                        justification = "This is actually a reference comparison")
    @SuppressWarnings("PMD.UseEqualsToCompareStrings")
    public static boolean equalsIgnoreCase(@Nullable final String one, @Nullable final String two) {
        return one == two || (one != null && one.equalsIgnoreCase(two));
    }

}
