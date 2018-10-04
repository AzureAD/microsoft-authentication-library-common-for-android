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

import java.util.Iterator;
import java.util.List;
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
            if(tempDelimiter != Character.MIN_VALUE) {
                builder.append(tempDelimiter);
            }
            tempDelimiter = delimiter;
            builder.append(s);
        }

        return builder.toString();
    }
}
