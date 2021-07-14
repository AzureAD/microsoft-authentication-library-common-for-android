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
package com.microsoft.identity.common.internal.cache;

import java.util.Iterator;
import java.util.Map;

/**
 * Return a key-value store
 */
public interface ISharedPreferencesFileManager {
    /**
     * Associates a {@link String} value with a key in the named resource that this represents.
     *
     * @param key   The name to store a String under.
     * @param value A string value..
     */
    void putString(String key, String value);

    /**
     * Retrieves a given String from the resource with the given key.
     *
     * @param key The key being retrieved.
     * @return The string value associated with this key or null if no value could be found.
     */
    String getString(String key);

    /**
     * Persists a long value to the named resource.
     *
     * @param key   The name (key) of the long to save.
     * @param value The actual value to persist.
     */
    void putLong(String key, long value);

    /**
     * Retrieves a long value previously stored.
     *
     * @param key The name (key) of the long to retrieve.
     * @return The persisted value or 0 if one cannot be found.
     */
    long getLong(String key);

    /**
     * Returns the name of named resource that this data source is backed by.
     *
     * @return The name of the file.
     */
    String getSharedPreferencesFileName();

    /**
     * Returns all entries in the named resource.
     * <p>
     * Note that you must not modify the collection returned by this method, or alter any of its
     * contents. The consistency of your stored data is not guaranteed if you do.
     *
     * @return A Map of all entries.
     */
    Map<String, String> getAll();

    /**
     * Returns an iterator on the shared preferences entries that views only those entries that
     * the predicate evaluates to true on the key.
     * @param keyFilter A predicate to use to evaluate the key, return true to include key value pair.
     * @return an iterator as a view on the shared preferences file.
     */
    Iterator<Map.Entry<String, String>> getAllFilteredByKey(Predicate<String> keyFilter);

    /**
     * Tests if the store backed by the named resource contains an entry for the supplied key.
     *
     * @param key The key to consult.
     * @return True, if the key has an associate entry.
     */
    boolean contains(String key);

    /**
     * Clears the contents of the named resource.
     */
    void clear();

    /**
     * Removes any associated entry for the supplied key.
     *
     * @param key The key whose value should be cleared.
     */
    void remove(final String key);

}
