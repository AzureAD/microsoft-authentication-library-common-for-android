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
package com.microsoft.identity.common.internal.util;

import android.content.Context;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;

import java.util.Calendar;
import java.util.Date;

public class ClockSkewManager implements IClockSkewManager {

    private static final class PreferencesMetadata {

        private static final String SKEW_PREFERENCES_FILENAME =
                "com.microsoft.identity.client.clock_correction";

        private static final String KEY_SKEW = "skew";
    }

    private SharedPreferencesFileManager mClockSkewPreferences;

    public ClockSkewManager(@NonNull final Context context) {
        mClockSkewPreferences = SharedPreferencesFileManager.getSharedPreferences(
                context,
                PreferencesMetadata.SKEW_PREFERENCES_FILENAME,
                -1,
                null
        );
    }

    @Override
    public void onTimestampReceived(long referenceTime) {
        final long clientTime = getCurrentClientTime().getTime();
        final long skewMillis = clientTime - referenceTime;
        mClockSkewPreferences.putLong(PreferencesMetadata.KEY_SKEW, skewMillis);
    }

    @Override
    public long getSkewMillis() {
        return mClockSkewPreferences.getLong(PreferencesMetadata.KEY_SKEW);
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
