//   Copyright (c) Microsoft Corporation.
//   All rights reserved.
//
//   This code is licensed under the MIT License.
//
//   Permission is hereby granted, free of charge, to any person obtaining a copy
//   of this software and associated documentation files(the "Software"), to deal
//   in the Software without restriction, including without limitation the rights
//   to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//   copies of the Software, and to permit persons to whom the Software is
//   furnished to do so, subject to the following conditions :
//
//   The above copyright notice and this permission notice shall be included in
//   all copies or substantial portions of the Software.
//
//   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//   THE SOFTWARE.

package com.microsoft.identity.common.internal.util;

import com.microsoft.identity.common.internal.cache.IKeyBasedStorage;
import com.microsoft.identity.common.java.util.ported.Predicate;

import java.util.Iterator;
import java.util.Map;

import lombok.NonNull;

/**
 * An adapter for SharedPreferences that allows it to appear as a nameValue store with String
 * value types.
 */
public class SharedPrefStringNameValueStorage extends AbstractSharedPrefNameValueStorage<String> {
    public SharedPrefStringNameValueStorage(IKeyBasedStorage mManager) {
        super(mManager);
    }

    @Override
    public String get(@NonNull String name) { return mManager.getString(name); }

    @Override
    public @NonNull Map<String, String> getAll() {
        return mManager.getAll();
    }

    @Override
    public void put(@NonNull String name, String value) {
        mManager.putString(name, value);
    }

    @Override
    public Iterator<Map.Entry<String, String>> getAllFilteredByKey(Predicate<String> keyFilter) {
        return mManager.getAllFilteredByKey(keyFilter);
    }
}