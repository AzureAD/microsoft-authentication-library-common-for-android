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

import com.microsoft.identity.common.java.interfaces.INameValueStorage;

import java.util.HashMap;
import java.util.Set;

import lombok.NonNull;

public class InMemoryStorage<T> implements INameValueStorage<T> {

    final HashMap<String, T> mMap = new HashMap<>();

    public int size(){
        return mMap.size();
    }

    @Override
    public T get(final String name) {
        return mMap.get(name);
    }

    @Override
    public @NonNull T getOrDefault(@NonNull String name, @NonNull T defaultValue) {
        return mMap.getOrDefault(name, defaultValue);
    }

    @Override
    public void put(final String name, final T value) {
        mMap.put(name, value);
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
    public @NonNull Set<String> keySet() {
        return mMap.keySet();
    }
}
