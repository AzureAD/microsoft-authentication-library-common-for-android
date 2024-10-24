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
package com.microsoft.identity.common.java.util.ported;

import com.microsoft.identity.common.java.logging.Logger;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

/**
 * A set of utility classes for handling objects to avoid repetition of common patterns.  The idea here is that the barrier
 * to entry to this class should be high, and the pattern should be in use in multiple locations.  Some
 * of these may duplicate functionality present in the JVM in various iterations - once we get support
 * for that we should remove them.
 */
public final class ObjectUtils {
    private static String TAG = ObjectUtils.class.getSimpleName();
    //Private constructor to discourage instantiation.
    private ObjectUtils() {
    }

    /**
     * This is a local implementation of Objects.equals.  It is a null-safe equals execution.
     * This should be removed if we get to an API version that has Objects.equals.
     *
     * @param o1 the first object
     * @param o2 the second object
     * @return true if the objects are both null or if they are both non-null and o1.equals(o2).
     */
    public static boolean equals(@Nullable final Object o1, @Nullable final Object o2) {
        return (o1 == o2) || (o1 != null && o1.equals(o2));
    }

    /**
     * A helper function for validating if the given object is null or empty.
     */
    public static void throwIfArgumentIsNull(final @Nullable Object argument,
                                             final @NonNull String argumentName,
                                             final @NonNull String methodTag) throws NullPointerException {
        if (argument == null) {
            Logger.error(methodTag, argumentName + " is null.", null);
            throw new NullPointerException(argumentName + " is null.");
        }
    }
}
