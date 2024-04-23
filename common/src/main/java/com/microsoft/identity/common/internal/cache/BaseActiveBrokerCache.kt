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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.jcip.annotations.ThreadSafe

/**
 * A cache for storing the active broker as known by the caller.
 **/
@ThreadSafe
open class BaseActiveBrokerCache
    (private val storage: INameValueStorage<String>,
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

    override fun getCachedActiveBroker(): BrokerData? {
        return runBlocking {
            lock.withLock {
                val packageName = storage.get(ACTIVE_BROKER_CACHE_PACKAGE_NAME_KEY)
                val signatureHash = storage.get(ACTIVE_BROKER_CACHE_SIGHASH_KEY)

                if (packageName.isNullOrEmpty() || signatureHash.isNullOrEmpty())
                    return@runBlocking null

                return@runBlocking BrokerData(packageName, signatureHash)
            }
        }
    }

    override fun shouldUseAccountManager(): Boolean {
        return runBlocking {
            lock.withLock {
                storage.get(SHOULD_USE_ACCOUNT_MANAGER_UNTIL_EPOCH_MILLISECONDS_KEY)?.let { rawValue ->
                    rawValue.toLongOrNull()?.let { expiryDate ->
                        return@runBlocking isNotExpired(expiryDate)
                    }
                }

                return@runBlocking false
            }
        }
    }

    override fun setShouldUseAccountManagerForTheNextMilliseconds(timeInMillis: Long) {
        return runBlocking {
            lock.withLock {
                val timeStamp = System.currentTimeMillis() + timeInMillis
                storage.put(SHOULD_USE_ACCOUNT_MANAGER_UNTIL_EPOCH_MILLISECONDS_KEY, timeStamp.toString())
            }
        }
    }

    override fun setCachedActiveBroker(brokerData: BrokerData) {
        return runBlocking {
            lock.withLock {
                setCachedActiveBrokerWithoutLock(brokerData)
            }
        }
    }

    override fun clearCachedActiveBroker() {
        return runBlocking {
            lock.withLock {
                storage.remove(ACTIVE_BROKER_CACHE_PACKAGE_NAME_KEY)
                storage.remove(ACTIVE_BROKER_CACHE_SIGHASH_KEY)
                storage.remove(SHOULD_USE_ACCOUNT_MANAGER_UNTIL_EPOCH_MILLISECONDS_KEY)
            }
        }
    }

    protected open fun setCachedActiveBrokerWithoutLock(brokerData: BrokerData){
        storage.put(ACTIVE_BROKER_CACHE_PACKAGE_NAME_KEY, brokerData.packageName)
        storage.put(ACTIVE_BROKER_CACHE_SIGHASH_KEY, brokerData.signingCertificateThumbprint)
        storage.remove(SHOULD_USE_ACCOUNT_MANAGER_UNTIL_EPOCH_MILLISECONDS_KEY)
    }
}