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
package com.microsoft.identity.common.internal.cache

import com.microsoft.identity.common.java.interfaces.INameValueStorage
import com.microsoft.identity.common.java.interfaces.IStorageSupplier
import kotlinx.coroutines.sync.Mutex

class ClientActiveBrokerCache
internal constructor(private val storage: INameValueStorage<String>,
                     private val lock: Mutex): BaseActiveBrokerCache(storage, lock), IClientActiveBrokerCache {

    companion object {
        /**
         * File name of [ClientActiveBrokerCache] used by the broker SDK (WPJ API, Broker API) code.
         * We need a separate storage as SDK might have their own encryption mechanism.
         **/
        private const val BROKER_METADATA_CACHE_STORE_ON_BROKER_SDK_SIDE_STORAGE_NAME = "BROKER_METADATA_CACHE_STORE_ON_BROKER_SDK_SIDE"

        /**
         * File name of [ClientActiveBrokerCache] used by the client SDK (MSAL, OneAuth) code.
         **/
        private const val BROKER_METADATA_CACHE_STORE_ON_CLIENT_SDK_SIDE_STORAGE_NAME = "BROKER_METADATA_CACHE_STORE_ON_CLIENT_SDK_SIDE"

        /**
         * The Mutex for all [ClientActiveBrokerCache] instances used by the SDK code.
         * (As of May 24, 2023... Kotlin has yet to officially support ReadWriteMutex.
         *  I don't think it's worth implementing our own (for now).
         *  If we eventually are seeing a perf hit, sure...)
         **/
        private val sBrokerSdkSideLock = Mutex()
        private val sClientSdkSideLock = Mutex()

        /**
         * If the caller is a broker SDK (WPJ API, Broker API), invoke this function.
         *
         * @param storageSupplier an [IStorageSupplier] component.
         * @return a thread-safe [IClientActiveBrokerCache].
         */
        @JvmStatic
        fun getBrokerSdkCache(storageSupplier: IStorageSupplier)
                : IClientActiveBrokerCache {
            return ClientActiveBrokerCache(
                storage = storageSupplier.getEncryptedNameValueStore(
                    BROKER_METADATA_CACHE_STORE_ON_BROKER_SDK_SIDE_STORAGE_NAME, String::class.java),
                lock = sBrokerSdkSideLock
            )
        }

        /**
         * If the caller is a client SDK (MSAL, OneAuth), invoke this function.
         *
         * @param storageSupplier an [IStorageSupplier] component.
         * @return a thread-safe [IClientActiveBrokerCache].
         */
        @JvmStatic
        fun getClientSdkCache(storageSupplier: IStorageSupplier)
                : IClientActiveBrokerCache {
            return ClientActiveBrokerCache(
                storage = storageSupplier.getEncryptedNameValueStore(
                    BROKER_METADATA_CACHE_STORE_ON_CLIENT_SDK_SIDE_STORAGE_NAME, String::class.java),
                lock = sClientSdkSideLock
            )
        }
    }
}