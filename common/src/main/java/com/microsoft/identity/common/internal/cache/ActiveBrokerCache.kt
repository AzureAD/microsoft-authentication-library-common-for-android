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
package com.microsoft.identity.common.internal.cache

import com.microsoft.identity.common.internal.broker.BrokerData
import com.microsoft.identity.common.java.interfaces.INameValueStorage
import com.microsoft.identity.common.java.interfaces.IStorageSupplier
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.jcip.annotations.ThreadSafe

/**
 * A cache for storing the active broker as known by the caller.
 **/
@ThreadSafe
open class ActiveBrokerCache
    internal constructor(private val storage: INameValueStorage<String>,
                         private val lock: Mutex) : IActiveBrokerCache {

    companion object {
        /**
         * Key of the broker package name in the cache.
         **/
        const val ACTIVE_BROKER_CACHE_PACKAGE_NAME_KEY = "ACTIVE_BROKER_CACHE_PACKAGE_NAME_KEY"

        /**
         * Key of the broker signature hash in the cache.
         **/
        const val ACTIVE_BROKER_CACHE_SIGHASH_KEY = "ACTIVE_BROKER_CACHE_SIGHASH_KEY"

        /**
         * File name of [ActiveBrokerCache] used by the broker code.
         **/
        private const val BROKER_METADATA_CACHE_STORE_ON_BROKER_SIDE_STORAGE_NAME = "BROKER_METADATA_CACHE_STORE_ON_BROKER_SIDE"

        /**
         * The Mutex for all [ActiveBrokerCache] instances used by the broker code.
         * (As of May 24, 2023... Kotlin has yet to officially support ReadWriteMutex.
         *  I don't think it's worth implementing our own (for now).
         *  If we eventually are seeing a perf hit, sure...)
         **/
        private val sBrokerSideLock = Mutex()

        /**
         * If the caller is the broker, invoke this function.
         *
         * @param storageSupplier an [IStorageSupplier] component.
         * @return a thread-safe [IActiveBrokerCache].
         * */
        fun getBrokerMetadataStoreOnBrokerSide(storageSupplier: IStorageSupplier)
            : IActiveBrokerCache {
            return ActiveBrokerCache(
                storage = storageSupplier.getEncryptedNameValueStore(
                    BROKER_METADATA_CACHE_STORE_ON_BROKER_SIDE_STORAGE_NAME, String::class.java),
                lock = sBrokerSideLock
            )
        }
    }

    /**
     * In-memory cached value.
     */
    internal var inMemoryCachedValue: BrokerData? = null

    override fun getCachedActiveBroker(): BrokerData? {
        return runBlocking {
            lock.withLock {
                if (inMemoryCachedValue != null) {
                    return@runBlocking inMemoryCachedValue
                }

                val packageName = storage.get(ACTIVE_BROKER_CACHE_PACKAGE_NAME_KEY)
                val signatureHash = storage.get(ACTIVE_BROKER_CACHE_SIGHASH_KEY)

                if (packageName.isNullOrEmpty() || signatureHash.isNullOrEmpty())
                    return@runBlocking null

                inMemoryCachedValue = BrokerData(packageName, signatureHash)
                return@runBlocking inMemoryCachedValue
            }
        }
    }

    override fun setCachedActiveBroker(brokerData: BrokerData) {
        return runBlocking {
            lock.withLock {
                storage.put(ACTIVE_BROKER_CACHE_PACKAGE_NAME_KEY, brokerData.packageName)
                storage.put(ACTIVE_BROKER_CACHE_SIGHASH_KEY, brokerData.signingCertificateThumbprint)
                inMemoryCachedValue = brokerData.copy()
            }
        }
    }

    override fun clearCachedActiveBroker() {
        return runBlocking {
            lock.withLock {
                clearCachedActiveBrokerWithoutLock()
            }
        }
    }

    protected fun clearCachedActiveBrokerWithoutLock(){
        storage.remove(ACTIVE_BROKER_CACHE_PACKAGE_NAME_KEY)
        storage.remove(ACTIVE_BROKER_CACHE_SIGHASH_KEY)
        inMemoryCachedValue = null
    }
}