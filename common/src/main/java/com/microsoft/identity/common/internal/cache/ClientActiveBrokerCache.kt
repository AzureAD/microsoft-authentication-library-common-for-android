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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ClientActiveBrokerCache
internal constructor(private val storage: INameValueStorage<String>,
                     private val lock: Mutex): BaseActiveBrokerCache(storage, lock), IClientActiveBrokerCache {

    companion object {
        /**
         * File name of [ClientActiveBrokerCache] used by the SDK code.
         **/
        private const val BROKER_METADATA_CACHE_STORE_ON_SDK_SIDE_STORAGE_NAME = "BROKER_METADATA_CACHE_STORE_ON_SDK_SIDE"

        /**
         * The Mutex for all [ClientActiveBrokerCache] instances used by the SDK code.
         * (As of May 24, 2023... Kotlin has yet to officially support ReadWriteMutex.
         *  I don't think it's worth implementing our own (for now).
         *  If we eventually are seeing a perf hit, sure...)
         **/
        private val sSdkSideLock = Mutex()

        /**
         * If the caller is an SDK, invoke this function.
         *
         * @param storageSupplier an [IStorageSupplier] component.
         * @return a thread-safe [IClientActiveBrokerCache].
         */
        fun getCache(storageSupplier: IStorageSupplier)
                : IClientActiveBrokerCache {
            return ClientActiveBrokerCache(
                storage = storageSupplier.getEncryptedNameValueStore(
                    BROKER_METADATA_CACHE_STORE_ON_SDK_SIDE_STORAGE_NAME, String::class.java),
                lock = sSdkSideLock
            )
        }

        /**
         * Returns true if the time has NOT passed the given expiry date.
         */
        fun isNotExpired(expiryDate: Long?): Boolean{
            if (expiryDate == null) {
                return false
            }
            return System.currentTimeMillis() < expiryDate
        }

        /**
         * Key for storing time which the client discovery should use AccountManager.
         **/
        const val SHOULD_USE_ACCOUNT_MANAGER_UNTIL_EPOCH_MILLISECONDS_KEY =
            "SHOULD_USE_ACCOUNT_MANAGER_UNTIL_EPOCH_MILLISECONDS_KEY"
    }

    /**
     * Cached value of [SHOULD_USE_ACCOUNT_MANAGER_UNTIL_EPOCH_MILLISECONDS_KEY]
     **/
    var cachedTimeStamp: Long? = null

    override fun shouldUseAccountManager(): Boolean {
        return runBlocking {
            lock.withLock {
                if (cachedTimeStamp == null){
                    storage.get(SHOULD_USE_ACCOUNT_MANAGER_UNTIL_EPOCH_MILLISECONDS_KEY)?.let { rawValue ->
                        rawValue.toLongOrNull()?.let { expiryDate ->
                            cachedTimeStamp = expiryDate
                        }
                    }
                }

                if (isNotExpired(cachedTimeStamp)){
                    return@runBlocking true
                }

                cachedTimeStamp = null
                return@runBlocking false
            }
        }
    }

    override fun setShouldUseAccountManagerForTheNextMilliseconds(timeInMillis: Long) {
        return runBlocking {
            lock.withLock {
                val timeStamp = System.currentTimeMillis() + timeInMillis
                storage.put(SHOULD_USE_ACCOUNT_MANAGER_UNTIL_EPOCH_MILLISECONDS_KEY, timeStamp.toString())
                cachedTimeStamp = timeStamp
            }
        }
    }

    override fun clearCachedActiveBroker() {
        return runBlocking {
            lock.withLock {
                clearCachedActiveBrokerWithoutLock()
                storage.remove(SHOULD_USE_ACCOUNT_MANAGER_UNTIL_EPOCH_MILLISECONDS_KEY)
                cachedTimeStamp = null
            }
        }
    }
}