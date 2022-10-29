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
import com.microsoft.identity.common.java.crypto.IKeyAccessor;

import javax.annotation.Nullable;

import lombok.NonNull;

/**
 * An interface for loading {@link INameValueStorage}
 */
public interface IStorageSupplier {
    /**
     * Retrieve a name-value store with a given identifier.
     *
     * @param storeName The name of a new KeyValue store.
     * @param clazz     The class of values in the name value store.
     * @return a INameValueStorage instance based around data stored with the same storeName.
     */
    @NonNull
    default <T> INameValueStorage<T> getNameValueStore(@NonNull final String storeName,
                                                       @NonNull final Class<T> clazz) {
        return getEncryptedNameValueStore(storeName, null, clazz);
    }

    /**
     * Retrieve a name-value store with a given identifier.
     *
     * @param storeName The name of a new KeyValue store. May not be null.
     * @param helper    The key manager for the encryption.  May be null.
     * @param clazz     The class of values in the name value store. May not be null.
     * @return a INameValueStorage instance based around data stored with the same storeName.
     */
    @NonNull
    <T> INameValueStorage<T> getEncryptedNameValueStore(@NonNull final String storeName,
                                                        @Nullable final IKeyAccessor helper,
                                                        @NonNull final Class<T> clazz);

    /**
     * Get a generic encrypted IMultiTypeNameValueStorage with a given identifier.
     *
     * @param storeName The name of a new KeyValue store. May not be null.
     * @param helper    The key manager for the encryption.  May not be null.
     */
    @NonNull
    IMultiTypeNameValueStorage getEncryptedFileStore(@NonNull final String storeName,
                                                     @NonNull final IKeyAccessor helper);

    /**
     * Get a generic IMultiTypeNameValueStorage with a given identifier.
     *
     * @param storeName The name of a new KeyValue store. May not be null.
     */
    @NonNull
    IMultiTypeNameValueStorage getFileStore(@NonNull final String storeName);

    /**
     * Retrieve a multi-process safe name-value store with a given identifier.
     *
     * @param storeName The name of a new KeyValue store. May not be null.
     * @return a INameValueStorage instance based around data stored with the same storeName.
     */
    @NonNull
    INameValueStorage<String> getMultiProcessStringStore(@NonNull final String storeName);
}
