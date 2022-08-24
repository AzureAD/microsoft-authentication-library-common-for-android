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
package com.microsoft.identity.common.java.interfaces;

import com.microsoft.identity.common.java.cache.IMultiTypeNameValueStorage;
import com.microsoft.identity.common.java.util.ported.Predicate;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

/**
 * An abstract implementation of {@link IPerSeparatorMultiTypeNameValueStorage}.
 *
 * @param <T> the type parameter indicating the type of separator
 */
public abstract class AbstractPerSeparatorMultiTypeNameValueStorage<T> implements IPerSeparatorMultiTypeNameValueStorage<T> {

    /**
     * Provides a storage instance for the specified separator.
     *
     * @param separator the separator to use for data separation
     * @return a {@link com.microsoft.identity.common.java.storage.MultiTypeNameValueStorage} to use
     * for storing data
     */
    @NonNull
    protected abstract IMultiTypeNameValueStorage getStoreForSeparator(@NonNull final T separator);

    @Nullable
    @Override
    public String getString(@NonNull final T separator, @NonNull final String name) {
        return getStoreForSeparator(separator).getString(name);
    }

    @Override
    public void putString(@NonNull final T separator,
                          @NonNull final String name,
                          @Nullable final String value) {
        getStoreForSeparator(separator).putString(name, value);
    }

    @Override
    public void putLong(@NonNull final T separator, @NonNull final String name, final long value) {
        getStoreForSeparator(separator).putLong(name, value);
    }

    @Override
    public long getLong(@NonNull final T separator, @NonNull final String name) {
        return getStoreForSeparator(separator).getLong(name);
    }

    @Override
    public @NonNull Map<String, String> getAll(@NonNull final T separator) {
        return getStoreForSeparator(separator).getAll();
    }

    @Override
    public void remove(@NonNull final T separator, @NonNull final String name) {
        getStoreForSeparator(separator).remove(name);
    }

    @Override
    public void clear(@NonNull final T separator) {
        getStoreForSeparator(separator).clear();
    }

    @Override
    public @NonNull Set<String> keySet(@NonNull final T separator) {
        return getStoreForSeparator(separator).getAll().keySet();
    }

    @Override
    public Iterator<Map.Entry<String, String>> getAllFilteredByKey(@NonNull final T separator,
                                                                   @NonNull final Predicate<String> keyFilter) {
        return getStoreForSeparator(separator).getAllFilteredByKey(keyFilter);
    }
}
