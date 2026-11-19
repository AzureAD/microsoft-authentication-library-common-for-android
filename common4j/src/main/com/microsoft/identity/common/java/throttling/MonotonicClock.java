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
package com.microsoft.identity.common.java.throttling;

/**
 * Clock abstraction separating monotonic elapsed deadlines ({@link #nanoTime()}) from one-shot
 * wall-clock conversion of RFC 9110 HTTP-date headers ({@link #currentTimeMillis()}).
 */
public interface MonotonicClock {

    /**
     * Returns monotonic nanoseconds suitable for deadline and LRU comparisons.
     *
     * @return monotonic time in nanoseconds.
     */
    long nanoTime();

    /**
     * Returns current UTC epoch milliseconds. Used only once at header parse time to convert
     * an RFC 9110 HTTP-date into a relative delta before anchoring to {@link #nanoTime()}.
     *
     * @return epoch milliseconds.
     */
    long currentTimeMillis();

    /**
     * Production system clock implementation.
     */
    MonotonicClock SYSTEM = new MonotonicClock() {
        @Override
        public long nanoTime() {
            return System.nanoTime();
        }

        @Override
        public long currentTimeMillis() {
            return System.currentTimeMillis();
        }
    };
}
