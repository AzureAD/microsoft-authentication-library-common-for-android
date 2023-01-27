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
package com.microsoft.identity.common.components;

import com.microsoft.identity.common.java.cache.IMultiTypeNameValueStorage;
import com.microsoft.identity.common.java.cache.MapBackedPreferencesManager;
import com.microsoft.identity.common.java.crypto.IKeyAccessor;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.java.interfaces.IStorageSupplier;
import com.microsoft.identity.common.java.util.ported.InMemoryStorage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

public class InMemoryStorageSupplier implements IStorageSupplier{
    private final Map<String, INameValueStorage<?>> mStores = new ConcurrentHashMap<>();

    @Override
    @NonNull
    public <T> INameValueStorage<T> getEncryptedNameValueStore(@NonNull final String storeName,
                                                               @Nullable final IKeyAccessor helper,
                                                               @NonNull final Class<T> clazz) {
        @SuppressWarnings("unchecked")
        INameValueStorage<T> ret = (INameValueStorage<T>) mStores.get(storeName);
        if (ret == null) {
            mStores.put(storeName, new InMemoryStorage<>());
            ret = (INameValueStorage<T>) mStores.get(storeName);
        }
        return ret;
    }

    private final Map<String, IMultiTypeNameValueStorage> mEncryptedFileStores = new ConcurrentHashMap<>();

    @Override
    public synchronized IMultiTypeNameValueStorage getEncryptedFileStore(@NonNull final String storeName,
                                                                         @NonNull final IKeyAccessor helper) {
        IMultiTypeNameValueStorage ret = mEncryptedFileStores.get(storeName);
        if (ret == null) {
            mEncryptedFileStores.put(storeName, MapBackedPreferencesManager.builder().name(storeName).build());
            ret = (IMultiTypeNameValueStorage) mEncryptedFileStores.get(storeName);
        }
        return ret;
    }

    private final Map<String, IMultiTypeNameValueStorage> mFileStores = new ConcurrentHashMap<>();

    @Override
    public IMultiTypeNameValueStorage getFileStore(@NonNull final String storeName) {
        IMultiTypeNameValueStorage ret = mFileStores.get(storeName);
        if (ret == null) {
            mFileStores.put(storeName, MapBackedPreferencesManager.builder().name(storeName).build());
            ret = (IMultiTypeNameValueStorage) mFileStores.get(storeName);
        }
        return ret;
    }

    @Override
    public INameValueStorage<String> getMultiProcessStringStore(@NonNull final String storeName) {
        @SuppressWarnings("unchecked")
        INameValueStorage<String> ret = (INameValueStorage<String>) mStores.get(storeName);
        if (ret == null) {
            mStores.put(storeName, new InMemoryStorage<String>());
            ret = (INameValueStorage<String>) mStores.get(storeName);
        }
        return ret;
    }
}
