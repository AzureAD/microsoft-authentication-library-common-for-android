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

import com.microsoft.identity.common.java.interfaces.INameValueStorage;

import java.util.Calendar;
import java.util.Date;

import lombok.NonNull;

public class ClockSkewManager implements IClockSkewManager {

    private static final class PreferencesMetadata {
        private static final String KEY_SKEW = "skew";
    }

    private final INameValueStorage<Long> mClockSkewStorage;

    public ClockSkewManager(@NonNull final INameValueStorage<Long> storage) {
        mClockSkewStorage = storage;
    }

    @Override
    public void onTimestampReceived(long referenceTime) {
        final long clientTime = getCurrentClientTime().getTime();
        final long skewMillis = clientTime - referenceTime;
        mClockSkewStorage.put(PreferencesMetadata.KEY_SKEW, skewMillis);
    }

    @Override
    public long getSkewMillis() {
        return mClockSkewStorage.get(PreferencesMetadata.KEY_SKEW);
    }

    @Override
    public Date toClientTime(long referenceTime) {
        return new Date(referenceTime + getSkewMillis());
    }

    @Override
    public Date toReferenceTime(long clientTime) {
        return new Date(clientTime - getSkewMillis());
    }

    @Override
    public Date getCurrentClientTime() {
        return Calendar.getInstance().getTime();
    }

    @Override
    public Date getAdjustedReferenceTime() {
        return toReferenceTime(getCurrentClientTime().getTime());
    }
}

