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
import com.microsoft.identity.common.java.util.ported.InMemoryStorage
import com.microsoft.identity.common.java.util.ported.Predicate
import kotlinx.coroutines.sync.Mutex
import org.junit.Assert
import org.junit.Test

class ActiveBrokerCacheTest {

    companion object {
        private const val MOCK_HASH = "MOCK_HASH"
        private const val MOCK_PACKAGE_NAME = "MOCK_PACKAGE_NAME"
    }

    /**
     * If there are 2 instances of [BaseActiveBrokerCache] that points to the same storage.
     * A value written by the first one should be readable by the 2nd one.
     **/
    @Test
    fun testReadWriteAcrossInstances() {
        val lock = Mutex()
        val storage = InMemoryStorage<String>()

        val cache1 = BaseActiveBrokerCache(storage, lock)
        val cache2 = BaseActiveBrokerCache(storage, lock)

        Assert.assertNull(cache1.getCachedActiveBroker())
        Assert.assertNull(cache1.getCachedActiveBroker())

        val mockData = BrokerData(MOCK_PACKAGE_NAME, MOCK_HASH)
        cache1.setCachedActiveBroker(mockData)

        // Value should be readable by both caches.
        Assert.assertEquals(mockData, cache1.getCachedActiveBroker())
        Assert.assertEquals(mockData, cache2.getCachedActiveBroker())
    }

    @Test
    fun testRead() {
        val readOnlyStorage = object : INameValueStorage<String> {
            override fun get(name: String): String? {
                if (name == BaseActiveBrokerCache.ACTIVE_BROKER_CACHE_PACKAGE_NAME_KEY) {
                    return MOCK_PACKAGE_NAME
                }

                if (name == BaseActiveBrokerCache.ACTIVE_BROKER_CACHE_SIGHASH_KEY) {
                    return MOCK_HASH
                }

                throw IllegalStateException()
            }

            override fun getAll(): MutableMap<String, String> {
                throw UnsupportedOperationException()
            }

            override fun remove(name: String) {
                throw UnsupportedOperationException()
            }

            override fun clear() {
                throw UnsupportedOperationException()
            }

            override fun keySet(): MutableSet<String> {
                throw UnsupportedOperationException()
            }

            override fun getAllFilteredByKey(keyFilter: Predicate<String>?): MutableIterator<MutableMap.MutableEntry<String, String>> {
                throw UnsupportedOperationException()
            }

            override fun put(name: String, value: String?) {
                throw UnsupportedOperationException()
            }
        }

        val cache = BaseActiveBrokerCache(readOnlyStorage, Mutex())
        Assert.assertEquals(BrokerData(MOCK_PACKAGE_NAME, MOCK_HASH), cache.getCachedActiveBroker())
    }

    @Test
    fun testWrite() {
        val readOnlyStorage = object : INameValueStorage<String> {
            var brokerPkgName: String? = null
            var brokerSigHash: String? = null

            override fun get(name: String): String? {
                throw UnsupportedOperationException()
            }

            override fun getAll(): MutableMap<String, String> {
                throw UnsupportedOperationException()
            }

            override fun remove(name: String) {
                throw UnsupportedOperationException()
            }

            override fun clear() {
                throw UnsupportedOperationException()
            }

            override fun keySet(): MutableSet<String> {
                throw UnsupportedOperationException()
            }

            override fun getAllFilteredByKey(keyFilter: Predicate<String>?): MutableIterator<MutableMap.MutableEntry<String, String>> {
                throw UnsupportedOperationException()
            }

            override fun put(name: String, value: String?) {
                if (name == BaseActiveBrokerCache.ACTIVE_BROKER_CACHE_PACKAGE_NAME_KEY) {
                    brokerPkgName = value
                    return
                }

                if (name == BaseActiveBrokerCache.ACTIVE_BROKER_CACHE_SIGHASH_KEY) {
                    brokerSigHash = value
                    return
                }

                throw IllegalStateException()
            }
        }

        val cache = BaseActiveBrokerCache(readOnlyStorage, Mutex())
        val mockData = BrokerData(MOCK_PACKAGE_NAME, MOCK_HASH)
        cache.setCachedActiveBroker(mockData)

        Assert.assertEquals(MOCK_PACKAGE_NAME, readOnlyStorage.brokerPkgName)
        Assert.assertEquals(MOCK_HASH, readOnlyStorage.brokerSigHash)
    }

    @Test
    fun testClear() {
        val clearOnlyStorage = object : INameValueStorage<String> {
            var isCleared = false

            override fun get(name: String): String? {
                throw UnsupportedOperationException()
            }

            override fun getAll(): MutableMap<String, String> {
                throw UnsupportedOperationException()
            }

            override fun remove(name: String) {
                isCleared = true
            }

            override fun clear() {
                throw UnsupportedOperationException()
            }

            override fun keySet(): MutableSet<String> {
                throw UnsupportedOperationException()
            }

            override fun getAllFilteredByKey(keyFilter: Predicate<String>?): MutableIterator<MutableMap.MutableEntry<String, String>> {
                throw UnsupportedOperationException()
            }

            override fun put(name: String, value: String?) {
                throw UnsupportedOperationException()
            }
        }

        val cache = BaseActiveBrokerCache(clearOnlyStorage, Mutex())

        cache.clearCachedActiveBroker()
        Assert.assertTrue(clearOnlyStorage.isCleared)
    }

    @Test
    fun testE2EWriteReadClear() {
        val cache = BaseActiveBrokerCache(InMemoryStorage(), Mutex())
        Assert.assertNull(cache.getCachedActiveBroker())

        val mockData = BrokerData(MOCK_PACKAGE_NAME, MOCK_HASH)
        cache.setCachedActiveBroker(mockData)

        Assert.assertEquals(BrokerData(MOCK_PACKAGE_NAME, MOCK_HASH), cache.getCachedActiveBroker())

        cache.clearCachedActiveBroker()
        Assert.assertNull(cache.getCachedActiveBroker())
    }
}
