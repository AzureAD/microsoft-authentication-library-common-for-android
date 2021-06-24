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
import androidx.annotation.VisibleForTesting;

import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Deprecated
 * Currently serving as an adapter of {@link com.microsoft.identity.common.java.util.ClockSkewManager}
 */
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
        justification = "This class kept its original name to avoid breaking change during the refactoring process." +
                "Once the process is done, this class will be removed entirely. ")
public class ClockSkewManager extends com.microsoft.identity.common.java.util.ClockSkewManager {

    private static final class PreferencesMetadata {
        private static final String SKEW_PREFERENCES_FILENAME =
                "com.microsoft.identity.client.clock_correction";
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public ClockSkewManager(@NonNull final ISharedPreferencesFileManager manager) {
        super(new SharedPrefLongNameValueStorage(manager));
    }

    public ClockSkewManager(@NonNull final Context context) {
        super(new SharedPrefLongNameValueStorage(
                SharedPreferencesFileManager.getSharedPreferences(
                        context,
                        PreferencesMetadata.SKEW_PREFERENCES_FILENAME,
                        -1,
                        null
                )
        ));
    }
}
