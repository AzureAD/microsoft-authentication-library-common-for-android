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
package com.microsoft.identity.common.components;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.internal.util.SharedPrefStringNameValueStorage;
import com.microsoft.identity.common.internal.util.SharedPreferenceLongStorage;
import com.microsoft.identity.common.java.cache.IMultiTypeNameValueStorage;
import com.microsoft.identity.common.java.crypto.IKeyAccessor;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.java.interfaces.IStorageSupplier;
import com.microsoft.identity.common.java.util.ported.Predicate;

import java.util.Iterator;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public class AndroidStorageSupplier implements IStorageSupplier {
    private final Context mContext;

    @SuppressWarnings("unchecked")
    @Override
    @NonNull
    public <T> INameValueStorage<T> getEncryptedNameValueStore(@NonNull final String storeName,
                                                               @Nullable final IKeyAccessor helper,
                                                               @NonNull final Class<T> clazz) {
        final IMultiTypeNameValueStorage mgr = SharedPreferencesFileManager.getSharedPreferences(mContext, storeName, helper);
        if (Long.class.isAssignableFrom(clazz)) {
            return (INameValueStorage<T>) new SharedPreferenceLongStorage(mgr);
        } else if (String.class.isAssignableFrom(clazz)) {
            return (INameValueStorage<T>) new SharedPrefStringNameValueStorage(mgr);
        }
        throw new UnsupportedOperationException("Only Long and String are natively supported as types");
    }

    @Override
    @NonNull
    public IMultiTypeNameValueStorage getEncryptedFileStore(@NonNull final String storeName,
                                                            @NonNull final IKeyAccessor helper) {
        return SharedPreferencesFileManager.getSharedPreferences(mContext, storeName, helper);
    }

    @Override
    @NonNull
    public IMultiTypeNameValueStorage getFileStore(@NonNull final String storeName) {
        return SharedPreferencesFileManager.getSharedPreferences(mContext, storeName, null);
    }

    @Override
    @NonNull
    public INameValueStorage<String> getMultiProcessStringStore(@NonNull final String storeName) {
        final SharedPreferences sharedPreferences = mContext.getSharedPreferences(storeName, Context.MODE_MULTI_PROCESS);
        return new SharedPrefStringNameValueStorage(new IMultiTypeNameValueStorage() {
            @Override
            public void putString(String key, String value) {
                sharedPreferences.edit().putString(key, value).apply();
            }

            @Override
            public String getString(String key) {
                return sharedPreferences.getString(key, null);
            }

            @Override
            public void putLong(String key, long value) {
                sharedPreferences.edit().putString(key, Long.toString(value)).apply();
            }

            @Override
            public long getLong(String key) {
                try {
                    if (!sharedPreferences.contains(key)) {
                        return 0;
                    }
                    return Long.parseLong(sharedPreferences.getString(key, "0"));
                } catch (final NumberFormatException nfe) {
                    return 0;
                }
            }

            @Override
            public Map<String, String> getAll() {
                return (Map<String, String>) sharedPreferences.getAll();
            }

            @Override
            public Iterator<Map.Entry<String, String>> getAllFilteredByKey(Predicate<String> keyFilter) {
                return null;
            }

            @Override
            public boolean contains(String key) {
                return sharedPreferences.contains(key);
            }

            @Override
            public void clear() {
                sharedPreferences.edit().clear().commit();
            }

            @Override
            public void remove(String key) {
                sharedPreferences.edit().remove(key).commit();
            }
        });
    }
}
