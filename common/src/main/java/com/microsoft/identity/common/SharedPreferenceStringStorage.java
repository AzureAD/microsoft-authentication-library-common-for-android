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

import android.content.Context;

import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.internal.util.AbstractSharedPrefNameValueStorage;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.java.util.ported.Predicate;

import java.util.Iterator;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NonNull;

/**
 * An implementation of {@link INameValueStorage} for storing String.
 * Implemented with SharedPreference.
 */
public class SharedPreferenceStringStorage extends AbstractSharedPrefNameValueStorage<String> {

    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", justification = "Lombok inserts nullchecks")
    public SharedPreferenceStringStorage(@NonNull final Context context,
                                         @NonNull final String sharedPrefFileName) {
        super(SharedPreferencesFileManager.getSharedPreferences(
                context,
                sharedPrefFileName,
                null
        ));
    }

    @Override
    public String get(@NonNull String name) {
        return mManager.getString(name);
    }

    public Iterator<Map.Entry<String, String>> getAllFilteredByKey(Predicate<String> keyPredicate) {
        return mManager.getAllFilteredByKey(keyPredicate);
    }

    @Override
    public @NonNull Map<String, String> getAll() {
        return mManager.getAll();
    }

    @Override
    public void put(@NonNull String name, @Nullable String value) {
        mManager.putString(name, value);
    }
}
