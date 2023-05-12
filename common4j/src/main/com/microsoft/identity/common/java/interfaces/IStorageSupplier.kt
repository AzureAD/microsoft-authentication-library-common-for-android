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
package com.microsoft.identity.common.java.interfaces

import com.microsoft.identity.common.java.cache.IMultiTypeNameValueStorage

/**
 * An interface for loading [INameValueStorage]
 */
interface IStorageSupplier {
    /**
     * Retrieve an unencrypted name-value store with a given identifier.
     *
     * @param storeName The name of a new KeyValue store.
     * @param clazz     The class of values in the name value store.
     * @return a INameValueStorage instance based around data stored with the same storeName.
     */
    fun <T> getUnencryptedNameValueStore(storeName: String, clazz: Class<T>): INameValueStorage<T>

    /**
     * Retrieve an encrypted name-value store with a given identifier.
     *
     * @param storeName The name of a new KeyValue store.
     * @param clazz     The class of values in the name value store.
     * @return a INameValueStorage instance based around data stored with the same storeName.
     */
    fun <T> getEncryptedNameValueStore(storeName: String, clazz: Class<T>): INameValueStorage<T>

    /**
     * Get a generic unencrypted IMultiTypeNameValueStorage with a given identifier.
     *
     * @param storeName The name of a new KeyValue store.
     */
    fun getUnencryptedFileStore(storeName: String): IMultiTypeNameValueStorage

    /**
     * Get a generic encrypted IMultiTypeNameValueStorage with a given identifier.
     *
     * @param storeName The name of a new KeyValue store.
     */
    fun getEncryptedFileStore(storeName: String): IMultiTypeNameValueStorage
}