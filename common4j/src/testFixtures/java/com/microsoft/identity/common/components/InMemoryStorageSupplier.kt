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
package com.microsoft.identity.common.components

import com.microsoft.identity.common.java.cache.IMultiTypeNameValueStorage
import com.microsoft.identity.common.java.cache.MapBackedPreferencesManager
import com.microsoft.identity.common.java.interfaces.INameValueStorage
import com.microsoft.identity.common.java.interfaces.IStorageSupplier
import com.microsoft.identity.common.java.util.ported.InMemoryStorage
import java.util.concurrent.ConcurrentHashMap

/**
 * Storage provided by this class are stored in-memory.
 * Used in tests only.
 */
class InMemoryStorageSupplier : IStorageSupplier {

    private val mNameValueStores: MutableMap<String, INameValueStorage<Any>> =
        ConcurrentHashMap()


    private val mFileStores: MutableMap<String, IMultiTypeNameValueStorage> =
        ConcurrentHashMap()

    @Suppress("UNCHECKED_CAST")
    override fun <T> getUnencryptedNameValueStore(storeName: String, clazz: Class<T>): INameValueStorage<T> {
        val existingStorage = mNameValueStores[storeName]
        if (existingStorage != null) {
            return existingStorage as INameValueStorage<T>
        }

        val nameValueStore = InMemoryStorage<Any>()
        mNameValueStores[storeName] = nameValueStore
        return nameValueStore as INameValueStorage<T>
    }

    override fun getUnencryptedFileStore(storeName: String): IMultiTypeNameValueStorage {
        val existingStorage = mFileStores[storeName]
        if (existingStorage != null) {
            return existingStorage
        }

        val fileStore = MapBackedPreferencesManager()
        mFileStores[storeName] = fileStore
        return fileStore
    }

    override fun <T> getEncryptedNameValueStore(
        storeName: String,
        clazz: Class<T>
    ): INameValueStorage<T> {
        return getUnencryptedNameValueStore(storeName, clazz)
    }

    override fun getEncryptedFileStore(storeName: String): IMultiTypeNameValueStorage {
        return getUnencryptedFileStore(storeName)
    }
}