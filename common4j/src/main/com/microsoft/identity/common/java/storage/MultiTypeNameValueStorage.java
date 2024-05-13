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
package com.microsoft.identity.common.java.storage;

import com.microsoft.identity.common.java.cache.IMultiTypeNameValueStorage;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.ported.Predicate;

import java.util.Iterator;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.AllArgsConstructor;
import lombok.NonNull;

/**
 * An implementation of {@link IMultiTypeNameValueStorage}.
 * <p>
 * The internal implementation here is backed by an implementation of a {@link INameValueStorage} with String.
 * Strings are dropped directly into the storage, whereas Long values are adapted into a String
 * using the {@link IGenericTypeStringAdapter#LongStringAdapter}.
 * <p>
 * This entire class should be functionally equivalent to the SharedPreferencesFileManager in the
 * Android Broker which does the exact things.
 * <p>
 * Any data stored in this {@link MultiTypeNameValueStorage} can also be encrypted by simply
 * supplying an encrypted variant of the {@link INameValueStorage} as the raw storage here. For
 * instance, you may choose to supply an {@link EncryptedNameValueStorage} to the constructor here
 * if you wish to store encrypted data in this {@link MultiTypeNameValueStorage}.
 */
@AllArgsConstructor
public class MultiTypeNameValueStorage implements IMultiTypeNameValueStorage {

    private final INameValueStorage<String> mNameValueStringStorage;

    @Override
    public void putString(@NonNull final String key, @Nullable final String value) {
        mNameValueStringStorage.put(key, value);
    }

    @Override
    public String getString(@NonNull final String key) {
        return mNameValueStringStorage.get(key);
    }

    @Override
    public void putLong(@NonNull final String key, final long value) {
        mNameValueStringStorage.put(key, IGenericTypeStringAdapter.LongStringAdapter.adapt(value));
    }

    @Override
    public long getLong(@NonNull final String key) {
        final String val = mNameValueStringStorage.get(key);

        if (StringUtil.isNullOrEmpty(val)) {
            return 0;
        }

        return IGenericTypeStringAdapter.LongStringAdapter.adapt(val);
    }

    @Override
    public Map<String, String> getAll() {
        return mNameValueStringStorage.getAll();
    }

    @Override
    public Iterator<Map.Entry<String, String>> getAllFilteredByKey(@NonNull final Predicate<String> keyFilter) {
        return mNameValueStringStorage.getAllFilteredByKey(keyFilter);
    }

    @Override
    public boolean contains(@NonNull final String key) {
        return !StringUtil.isNullOrEmpty(getString(key));
    }

    @Override
    public void clear() {
        mNameValueStringStorage.clear();
    }

    @Override
    public void remove(@NonNull final String key) {
        mNameValueStringStorage.remove(key);
    }
}
