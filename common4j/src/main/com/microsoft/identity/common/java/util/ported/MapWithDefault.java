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
package com.microsoft.identity.common.java.util.ported;

import com.microsoft.identity.common.java.interfaces.INameValueStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

/**
 * A wrapper around Map
 * We have this class because Map's getOrDefault is not available pre Android API 24.
 * */
public class MapWithDefault<T> implements INameValueStorage<T> {
    private final Map<String, T> mMap = new ConcurrentHashMap<>();

    @Nullable
    public T get(@NonNull final String key) {
        return mMap.get(key);
    }

    @NonNull
    public T getOrDefault(@NonNull final String key, @NonNull final T defaultValue) {
        final T result = mMap.get(key);
        if (result == null) {
            return defaultValue;
        }
        return result;
    }

    public void put(@NonNull final String key,
                    @Nullable final T value) {
        if (value == null) {
            mMap.remove(key);
            return;
        }

        mMap.put(key, value);
    }

    @Override
    public void remove(@NonNull String name) {
        mMap.remove(name);
    }

    @Override
    public void clear() {
        mMap.clear();
    }

    @Override
    public Set<String> keySet() {
        return mMap.keySet();
    }
}
