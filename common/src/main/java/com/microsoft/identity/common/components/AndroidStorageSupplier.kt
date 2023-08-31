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
package com.microsoft.identity.common.components

import android.content.Context
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager
import com.microsoft.identity.common.internal.util.SharedPrefStringNameValueStorage
import com.microsoft.identity.common.internal.util.SharedPreferenceLongStorage
import com.microsoft.identity.common.java.cache.IMultiTypeNameValueStorage
import com.microsoft.identity.common.java.crypto.StorageEncryptionManager
import com.microsoft.identity.common.java.interfaces.INameValueStorage
import com.microsoft.identity.common.java.interfaces.IStorageSupplier

class AndroidStorageSupplier(private val context: Context,
                             private val storageEncryptionManager: StorageEncryptionManager)
    : IStorageSupplier {

    companion object {
        @Suppress("UNCHECKED_CAST")
        private fun <T> getNameValueStore(context: Context,
                                          storeName: String,
                                          clazz: Class<T>,
                                          storageEncryptionManager: StorageEncryptionManager?): INameValueStorage<T> {
            val mgr: IMultiTypeNameValueStorage =
                SharedPreferencesFileManager.getSharedPreferences(context, storeName, storageEncryptionManager)
            if (Long::class.java.isAssignableFrom(clazz)|| java.lang.Long::class.java.isAssignableFrom(clazz)) {
                return (SharedPreferenceLongStorage(mgr) as INameValueStorage<T>)
            } else if (String::class.java.isAssignableFrom(clazz)|| java.lang.String::class.java.isAssignableFrom(clazz)) {
                return (SharedPrefStringNameValueStorage(mgr) as INameValueStorage<T>)
            }

            throw UnsupportedOperationException("Only Long and String are natively supported as types")
        }
    }

    override fun <T> getUnencryptedNameValueStore(storeName: String, clazz: Class<T>): INameValueStorage<T> {
        return getNameValueStore(context, storeName, clazz, null)
    }

    override fun <T> getEncryptedNameValueStore(
        storeName: String,
        clazz: Class<T>
    ): INameValueStorage<T> {
        return getNameValueStore(context, storeName, clazz, storageEncryptionManager)
    }

    override fun getUnencryptedFileStore(storeName: String): IMultiTypeNameValueStorage {
        return SharedPreferencesFileManager.getSharedPreferences(context, storeName, null)
    }

    override fun getEncryptedFileStore(storeName: String): IMultiTypeNameValueStorage {
        return SharedPreferencesFileManager.getSharedPreferences(context, storeName, storageEncryptionManager)
    }
}