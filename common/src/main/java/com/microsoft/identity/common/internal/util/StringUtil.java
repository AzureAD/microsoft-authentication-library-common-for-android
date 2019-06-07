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

import android.support.annotation.NonNull;
import android.util.Pair;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


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

    public static String join(char delimiter, List<String> toJoin) {
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
     *         1 if thisVersion is larger than thatVersion,
     *         0 if thisVersion is equal to thatVersion.
     */
    public static int compareSemanticVersion(final String thisVersion, final String thatVersion) {
        if(thatVersion == null) {
            return 1;
        }

        final String[] thisParts = thisVersion.split("\\.");
        final String[] thatParts = thatVersion.split("\\.");
        final int length = Math.max(thisParts.length, thatParts.length);
        for(int i = 0; i < length; i++) {
            int thisPart = i < thisParts.length ?
                    Integer.parseInt(thisParts[i]) : 0;
            int thatPart = i < thatParts.length ?
                    Integer.parseInt(thatParts[i]) : 0;

            if(thisPart < thatPart) {
                return -1;
            }

            if(thisPart > thatPart) {
                return 1;
            }
        }

        return 0;
    }

    /**
     * Return a copy of the contents of the given map as a {@link JSONObject}. Instead of failing on
     * {@code null} values like the {@link JSONObject} map constructor, it cleans them up and
     * correctly converts them to {@link JSONObject#NULL}.
     */
    public static JSONObject toJsonObject(Map<String, ?> map) {
        JSONObject jsonObject = new JSONObject();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            Object value = wrap(entry.getValue());
            try {
                jsonObject.put(entry.getKey(), value);
            } catch (JSONException ignored) {
                // Ignore values that JSONObject doesn't accept.
            }
        }
        return jsonObject;
    }

    /**
     * Wraps the given object if necessary. {@link JSONObject#wrap(Object)} is only available on API
     * 19+, so we've copied the implementation. Deviates from the original implementation in that it
     * always returns {@link JSONObject#NULL} instead of {@code null} in case of a failure, and
     * returns the {@link Object#toString} of any object that is of a custom (non-primitive or
     * non-collection/map) type.
     *
     * <p>If the object is null returns {@link JSONObject#NULL}. If the object is a {@link JSONArray}
     * or {@link JSONObject}, no wrapping is necessary. If the object is {@link JSONObject#NULL}, no
     * wrapping is necessary. If the object is an array or {@link Collection}, returns an equivalent
     * {@link JSONArray}. If the object is a {@link Map}, returns an equivalent {@link JSONObject}. If
     * the object is a primitive wrapper type or {@link String}, returns the object. Otherwise returns
     * the result of {@link Object#toString}. If wrapping fails, returns JSONObject.NULL.
     */
    private static Object wrap(Object o) {
        //TODO can be removed after change the minSdk to 19.
        if (o == null) {
            return JSONObject.NULL;
        }
        if (o instanceof JSONArray || o instanceof JSONObject) {
            return o;
        }
        if (o.equals(JSONObject.NULL)) {
            return o;
        }
        try {
            if (o instanceof Collection) {
                return new JSONArray((Collection) o);
            } else if (o.getClass().isArray()) {
                final int length = Array.getLength(o);
                JSONArray array = new JSONArray();
                for (int i = 0; i < length; ++i) {
                    array.put(wrap(Array.get(array, i)));
                }
                return array;
            }
            if (o instanceof Map) {
                //noinspection unchecked
                return toJsonObject((Map) o);
            }
            if (o instanceof Boolean
                    || o instanceof Byte
                    || o instanceof Character
                    || o instanceof Double
                    || o instanceof Float
                    || o instanceof Integer
                    || o instanceof Long
                    || o instanceof Short
                    || o instanceof String) {
                return o;
            }
            // Deviate from original implementation and return the String representation of the object
            // regardless of package.
            return o.toString();
        } catch (Exception ignored) {
        }
        // Deviate from original and return JSONObject.NULL instead of null.
        return JSONObject.NULL;
    }
}
