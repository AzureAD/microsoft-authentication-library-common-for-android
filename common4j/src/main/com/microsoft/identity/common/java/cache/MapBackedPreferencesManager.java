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
package com.microsoft.identity.common.java.cache;

import com.microsoft.identity.common.java.util.ported.Predicate;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import lombok.experimental.Accessors;

@Accessors(prefix = "m")
/**
 * A SharedPreferencesFileManager backed by a HashMap.  This is mainly for testing purposes,
 * where it doesn't make sense to instantiate shared preferences files.
 */
public class MapBackedPreferencesManager implements IMultiTypeNameValueStorage {

    private final Map<String, String> mBackingStore = new HashMap<>();

    @Override
    public void putString(String key, String value) {
        mBackingStore.put(key, value);
    }

    @Override
    public String getString(String key) {
        return mBackingStore.get(key);
    }

    @Override
    public void putLong(String key, long value) {
        mBackingStore.put(key, Long.toString(value));
    }

    @Override
    public long getLong(String key) {
        String s = mBackingStore.get(key);
        return s == null ? 0 : Long.parseLong(s);
    }

    @Override
    public Map<String, String> getAll() {
        return new HashMap<>(mBackingStore);
    }

    @Override
    public Iterator<Map.Entry<String, String>> getAllFilteredByKey(Predicate<String> keyFilter) {
        Map<String, String> newMap = new HashMap<>();
        for (Map.Entry<String, String> entry: mBackingStore.entrySet()) {
            if (keyFilter.test(entry.getKey())) {
                newMap.put(entry.getKey(), entry.getValue());
            }
        }
        return newMap.entrySet().iterator();
    }

    @Override
    public boolean contains(String key) {
        return mBackingStore.containsKey(key);
    }

    @Override
    public void clear() {
        mBackingStore.clear();
    }

    @Override
    public void remove(String key) {
        mBackingStore.remove(key);
    }
}
