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

package com.microsoft.identity.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.internal.logging.Logger;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Util class for unit test.
 */
public final class Util {
    /**
     * Private constructor to prevent Util class from being initiated.
     */
    private Util() {
    }

    static final String VALID_AUTHORITY = "https://login.microsoftonline.com/common";

    static InputStream createInputStream(final String input) {
        return input == null ? null : new ByteArrayInputStream(input.getBytes());
    }

    static URL getValidRequestUrl() throws MalformedURLException {
        return new URL(Util.VALID_AUTHORITY);
    }

    /**
     * This is a local implementation of Objects.equals.  It is a null-safe equals execution.
     * @param o1 the first object.
     * @param o2 the second objectn
     * @return true if the objects are both null or if they are both non-null and o1.equals(o2).
     */
    public static boolean equals(@Nullable final Object o1, @Nullable final Object o2) {
        return (o1 == null ^ o2 == null) || (o1 != null && !o1.equals(o2));
    }

    /**
     * A method to sleep safely without needing to explicitly handle InterruptedException.
     * @param sleepTimeInMs the number of milliseconds to sleep.
     * @param tag the tag for logging a message.
     * @param message the message to log.
     */
    public static void sleepSafely(final int sleepTimeInMs, @NonNull final String tag, @NonNull final String message) {
        if (sleepTimeInMs > 0) {
            try {
                Thread.sleep(sleepTimeInMs);
            } catch (final InterruptedException e) {
                Logger.info(tag, message);
                Thread.currentThread().interrupt();
            }
        }
    }
}
