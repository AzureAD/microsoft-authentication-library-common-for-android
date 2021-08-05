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

import com.microsoft.identity.common.internal.cache.IKeyBasedStorage;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.ported.Predicate;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import lombok.NonNull;

/**
 * Adapts {@link IKeyBasedStorage} to {@link INameValueStorage <Long>}
 * */
public class SharedPreferenceLongStorage extends AbstractSharedPrefNameValueStorage<Long> {
    public SharedPreferenceLongStorage(IKeyBasedStorage mManager) {
        super(mManager);
    }

    @Override
    public Long get(@NonNull String name) {
        return mManager.getLong(name);
    }

    @Override
    public @NonNull Map<String, Long> getAll() {
        Map<String, Long> allLongs = new HashMap<>();
        for (Map.Entry<String, String> e : mManager.getAll().entrySet()) {
            try {
                allLongs.put(e.getKey(), Long.parseLong(e.getValue()));
            } catch (final NumberFormatException nfe) {
                //nothing to do
            }
        }
        return allLongs;
    }

    @Override
    public void put(@NonNull String name, Long value) {
        mManager.putLong(name, value);
    }

    @Override
    public Iterator<Map.Entry<String, Long>> getAllFilteredByKey(final @NonNull Predicate<String> keyFilter) {
        return new Iterator<Map.Entry<String, Long>>() {

            final Iterator<Map.Entry<String, String>> iterator = mManager.getAllFilteredByKey(keyFilter);
            Map.Entry<String, Long> nextEntry = null;

            @Override
            public boolean hasNext() {
                if (nextEntry != null) {
                    return true;
                }
                if (!iterator.hasNext()) {
                    return false;
                }
                do {
                    Map.Entry<String, String> nextElement = iterator.next();
                    try {
                        long parsedValue = Long.parseLong(nextElement.getValue());
                        nextEntry = new AbstractMap.SimpleEntry<String, Long>(nextElement.getKey(), parsedValue);
                    } catch (NumberFormatException nfe) {
                        nextEntry = null;
                    }
                } while (nextEntry == null && iterator.hasNext());
                return nextEntry != null;
            }

            @Override
            public Map.Entry<String, Long> next() {
                if (nextEntry == null && !hasNext()) {
                    throw new NoSuchElementException();
                }
                final Map.Entry<String, Long> tmp = nextEntry;
                nextEntry = null;
                return tmp;
            }


            @Override
            public void remove() {
                throw new UnsupportedOperationException("Removal of elements is not supported");
            }
        };
    }
}
