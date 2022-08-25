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

import com.microsoft.identity.common.java.util.ported.Predicate;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

/**
 * This is a storage that allows to store name-value pairs associated to a particular "Separator".
 * This separator can be anything of type T as defined by the implementations of this interface.
 * <p>
 * Functionally, this interface should be considered similar to Account Manager in Android that
 * allows to store name-value pairs separated per Account. This interface does the same thing,
 * however, instead of separating per account, it allows to separate per anything of any type.
 * Furthermore, internal storage mechanism here can also be backed by anything i.e. this interface
 * can be implemented by SharedPreferences on Android or any other mechanism on any platform.
 *
 * @param <T> the type of separator to use
 */
public interface IPerSeparatorMultiTypeNameValueStorage<T> {
    /**
     * Gets a {@link String} value from the storage.
     * A "null" will be returned in the following cases:
     * - separator not found
     * - name not found
     *
     * @param separator the key to use for storage separation
     * @param name      A name associated to the value
     */
    @Nullable
    String getString(@NonNull T separator, @NonNull String name);

    /**
     * Puts a value into the storage.
     *
     * @param separator the key to use for storage separation
     * @param name      A name associated to the value
     * @param value     value to be persisted
     */
    void putString(@NonNull T separator, @NonNull String name, @Nullable String value);

    /**
     * Persists a long value to the named resource.
     *
     * @param separator the key to use for storage separation
     * @param name      The name (key) of the long to save
     * @param value     The actual value to persist
     */
    void putLong(@NonNull T separator, @NonNull String name, long value);

    /**
     * Retrieves a long value previously stored.
     *
     * @param separator the key to use for storage separation
     * @param name      The name (key) of the long to retrieve
     * @return The persisted value or 0 if one cannot be found
     */
    long getLong(@NonNull T separator, @NonNull String name);

    /**
     * Returns all entries in the named resource.
     *
     * @param separator the key to use for storage separation
     * @return a map containing all the values associated to the supplied separator in this structure.
     */
    @NonNull
    Map<String, String> getAll(@NonNull T separator);

    /**
     * Removes a value from the storage.
     *
     * @param separator the key to use for storage separation
     * @param name      A name associated to the value
     */
    void remove(@NonNull T separator, @NonNull String name);

    /**
     * Clear all data from the storage associated to the specified separator.
     */
    void clear(@NonNull T separator);

    /**
     * Get all keys in this storage associated to the specified separator.
     *
     * @param separator the key to use for storage separation
     */
    @NonNull
    Set<String> keySet(@NonNull T separator);

    /**
     * Returns an iterator on the shared preferences entries that views only those entries that
     * the predicate evaluates to true on the key.
     *
     * @param separator the key to use for storage separation
     * @param keyFilter A predicate to use to evaluate the key, return true to include key value pair.
     * @return an iterator as a view on the shared preferences file.
     */
    Iterator<Map.Entry<String, String>> getAllFilteredByKey(@NonNull T separator, @NonNull Predicate<String> keyFilter);
}
