/**
 * // Copyright (c) Microsoft Corporation.
 * // All rights reserved.
 * //
 * // This code is licensed under the MIT License.
 * //
 * // Permission is hereby granted, free of charge, to any person obtaining a copy
 * // of this software and associated documentation files(the "Software"), to deal
 * // in the Software without restriction, including without limitation the rights
 * // to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
 * // copies of the Software, and to permit persons to whom the Software is
 * // furnished to do so, subject to the following conditions :
 * //
 * // The above copyright notice and this permission notice shall be included in
 * // all copies or substantial portions of the Software.
 * //
 * // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * // IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * // FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * // AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * // LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * // OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * // THE SOFTWARE.
 * */

//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.java.util;

import java.util.Date;

/**
 * Provides functionality related to calculating and storing server clock skews for client
 * synchronization.
 */
public interface IClockSkewManager {

    /**
     * Called when a server response is received. Calculates the skew and stores the value.
     *
     * @param referenceTime The reference time against which to compute a skew.
     */
    void onTimestampReceived(long referenceTime);

    /**
     * Returns the current clock skew. The skew is positive if the client is ahead of the reference
     * time. Negative if the reference time is ahead.
     *
     * @return THe current clock skew in milliseconds.
     */
    long getSkewMillis();

    /**
     * Given a reference value (UTC), adjust the date with the known clock skew and return a Date
     * representing that point in time from the perspective of the client.
     *
     * @param referenceTime The reference time (UTC) in millis.
     * @return The "client time" with the applied skew.
     */
    Date toClientTime(long referenceTime);

    /**
     * Given a long value representing a point in "client time", adjust the date with the known
     * clock skew and return a Date representing that point in time from the perspective of the
     * reference value.
     *
     * @param clientTime A {@link Date} representing a point in time on the client.
     * @return The "reference time" with the applied skew.
     */
    Date toReferenceTime(long clientTime);

    /**
     * The current time according to the client. No skew is applied.
     *
     * @return A {@link Date} representing the current time as understood by the client.
     */
    Date getCurrentClientTime();

    /**
     * Return a {@link Date} representing the current point in time from the perspective of the
     * reference.
     *
     * @return An adjusted {@link Date} object.
     */
    Date getAdjustedReferenceTime();
}
