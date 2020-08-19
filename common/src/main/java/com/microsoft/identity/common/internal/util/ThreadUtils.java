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

import androidx.annotation.NonNull;

import com.microsoft.identity.common.internal.logging.Logger;

/**
 * A set of utility classes for thread operations to avoid repetition of common patterns.  The idea here is that the barrier
 * to entry to this class should be high, and the pattern should be in use in multiple locations.  Some
 * of these may duplicate functionality present in the JVM in various iterations - once we get support
 * for that we should remove them.
 */
public class ThreadUtils {
    /**
     * A method to sleep safely without needing to explicitly handle InterruptedException.
     *
     * @param sleepTimeInMs the number of milliseconds to sleep.
     * @param tag           the tag for logging a message.
     * @param message       the message to log.
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
