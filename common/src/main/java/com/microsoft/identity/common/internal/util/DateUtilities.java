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

import java.util.Date;
import java.util.concurrent.TimeUnit;

public final class DateUtilities {

    private DateUtilities() {
    }

    /**
     * Create a copy of a date to avoid exposing the internal references.
     *
     * @param date represents a specific instant in time, with millisecond precision.
     * @return Date
     */
    public static Date createCopy(final Date date) {
        if (date != null) {
            return new Date(date.getTime());
        }

        return date;
    }

    public static long getExpiresOn(long expiresIn) {
        final long currentTimeMillis = System.currentTimeMillis();
        final long currentTimeSecs = TimeUnit.MILLISECONDS.toSeconds(currentTimeMillis);
        return currentTimeSecs + expiresIn;
    }
}

