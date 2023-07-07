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
package com.microsoft.identity.common.internal.activebrokerdiscovery

import android.os.Bundle
import com.microsoft.identity.common.exception.BrokerCommunicationException
import com.microsoft.identity.common.internal.broker.BrokerData.Companion.prodCompanyPortal
import com.microsoft.identity.common.internal.broker.BrokerData.Companion.prodMicrosoftAuthenticator
import com.microsoft.identity.common.internal.broker.ipc.AbstractIpcStrategyWithServiceValidation
import com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle
import com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.exception.ClientException.ONLY_SUPPORTS_ACCOUNT_MANAGER_ERROR_CODE
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
class BrokerDiscoveryClientTests {

    /**
     * Happy scenario.
     * - First time querying (nothing in the cache).
     * - AuthApp and CP are installed. Both app supports the new election mechanism
     *      (Account Manager shall not be used).
     * - AuthApp is the active broker.
     **/
    @Test
    fun testQueryFromBroker(){
        val client = BrokerDiscoveryClient(
            brokerCandidates = setOf(
                prodMicrosoftAuthenticator, prodCompanyPortal
            ),
            getActiveBrokerFromAccountManager = {
                throw IllegalStateException()
            },
            ipcStrategy = object : IIpcStrategy {
                override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle {
                    if (bundle.targetBrokerAppPackageName == prodMicrosoftAuthenticator.packageName ||
                            bundle.targetBrokerAppPackageName == prodCompanyPortal.packageName) {
                        val returnBundle = Bundle()
                        returnBundle.putString(
                            BrokerDiscoveryClient.ACTIVE_BROKER_PACKAGE_NAME_BUNDLE_KEY,
                            prodMicrosoftAuthenticator.packageName
                        )
                        returnBundle.putString(
                            BrokerDiscoveryClient.ACTIVE_BROKER_SIGNATURE_HASH_BUNDLE_KEY,
                            prodMicrosoftAuthenticator.signingCertificateHash
                        )
                        return returnBundle
                    }

                    throw IllegalStateException()
                }
                override fun getType(): IIpcStrategy.Type {
                    return IIpcStrategy.Type.CONTENT_PROVIDER
                }
            },
            cache = InMemoryActiveBrokerCache(),
            isPackageInstalled =  {
                it == prodMicrosoftAuthenticator || it == prodCompanyPortal
            },
            isValidBroker = { true }
        )

        Assert.assertEquals(prodMicrosoftAuthenticator, client.getActiveBroker())
    }

    /**
     * The brokers don't support the new election mechanism.
     * Account Manager is used instead.
     * */
    @Test
    fun testQueryFromLegacyBroker(){
        val client = BrokerDiscoveryClient(
            brokerCandidates = setOf(
                prodMicrosoftAuthenticator, prodCompanyPortal
            ),
            getActiveBrokerFromAccountManager = {
                return@BrokerDiscoveryClient prodCompanyPortal
            },
            ipcStrategy = object : IIpcStrategy {
                override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle {
                    throw BrokerCommunicationException(
                        BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE,
                        IIpcStrategy.Type.CONTENT_PROVIDER,
                        null,
                        null
                    )
                }
                override fun getType(): IIpcStrategy.Type {
                    return IIpcStrategy.Type.CONTENT_PROVIDER
                }
            },
            cache = InMemoryActiveBrokerCache(),
            isPackageInstalled =  {
                it == prodMicrosoftAuthenticator || it == prodCompanyPortal
            },
            isValidBroker = { true }
        )

        Assert.assertEquals(prodCompanyPortal, client.getActiveBroker())
    }

    /**
     * If we get an ONLY_SUPPORTS_ACCOUNT_MANAGER_ERROR_CODE error.
     * AccountManager shall be used (but not cached).
     **/
    @Test
    fun testQuery_V0ProtocolErrorReturned(){
        val cache = InMemoryActiveBrokerCache()
        val client = BrokerDiscoveryClient(
            brokerCandidates = setOf(
                prodMicrosoftAuthenticator, prodCompanyPortal
            ),
            getActiveBrokerFromAccountManager = {
                return@BrokerDiscoveryClient prodCompanyPortal
            },
            ipcStrategy = object : IIpcStrategy {
                override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle {
                    throw ClientException(ONLY_SUPPORTS_ACCOUNT_MANAGER_ERROR_CODE)
                }
                override fun getType(): IIpcStrategy.Type {
                    return IIpcStrategy.Type.CONTENT_PROVIDER
                }
            },
            cache = cache,
            isPackageInstalled =  {
                it == prodMicrosoftAuthenticator || it == prodCompanyPortal
            },
            isValidBroker = { true }
        )
        Assert.assertEquals(prodCompanyPortal, client.getActiveBroker())
        Assert.assertTrue(cache.shouldUseAccountManager())
        Assert.assertNull(cache.getCachedActiveBroker())
    }

    /**
     * If we ping the broker that doesn't support the new broker election logic,
     * an error shall be returned. AccountManager shall be used (but not cached).
     **/
    @Test
    fun testQuery_UnsupportedBrokerErrorReturned(){
        val cache = InMemoryActiveBrokerCache()
        val client = BrokerDiscoveryClient(
            brokerCandidates = setOf(
                prodMicrosoftAuthenticator, prodCompanyPortal
            ),
            getActiveBrokerFromAccountManager = {
                return@BrokerDiscoveryClient prodMicrosoftAuthenticator
            },
            ipcStrategy = object : AbstractIpcStrategyWithServiceValidation() {
                override fun communicateToBrokerAfterValidation(bundle: BrokerOperationBundle): Bundle? {
                    throw IllegalStateException()
                }

                override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String): Boolean {
                    return false
                }

                override fun getType(): IIpcStrategy.Type {
                    return IIpcStrategy.Type.CONTENT_PROVIDER
                }
            },
            cache = cache,
            isPackageInstalled =  {
                it == prodMicrosoftAuthenticator || it == prodCompanyPortal
            },
            isValidBroker = { true }
        )

        Assert.assertEquals(prodMicrosoftAuthenticator, client.getActiveBroker())
        Assert.assertTrue(cache.shouldUseAccountManager())
        Assert.assertNull(cache.getCachedActiveBroker())
    }


    /**
     * No Broker is installed.
     **/
    @Test
    fun testQuery_NoBrokerInstalled(){
        val client = BrokerDiscoveryClient(
            brokerCandidates = setOf(
                prodMicrosoftAuthenticator, prodCompanyPortal
            ),
            getActiveBrokerFromAccountManager = {
                return@BrokerDiscoveryClient null
            },
            ipcStrategy = object : IIpcStrategy {
                override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle {
                    throw IllegalStateException()
                }
                override fun getType(): IIpcStrategy.Type {
                    return IIpcStrategy.Type.CONTENT_PROVIDER
                }
            },
            cache = InMemoryActiveBrokerCache(),
            isPackageInstalled =  {
                return@BrokerDiscoveryClient false
            },
            isValidBroker = { true }
        )

        Assert.assertNull(client.getActiveBroker())
    }

    /**
     * Test relying on cached value
     * */
    @Test
    fun testCache() {
        val cache = InMemoryActiveBrokerCache()
        cache.setCachedActiveBroker(prodMicrosoftAuthenticator)

        val client = BrokerDiscoveryClient(
            brokerCandidates = setOf(
                prodMicrosoftAuthenticator, prodCompanyPortal
            ),
            getActiveBrokerFromAccountManager = {
                throw IllegalStateException()
            },
            ipcStrategy = object : IIpcStrategy {
                override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle {
                    throw IllegalStateException()
                }
                override fun getType(): IIpcStrategy.Type {
                    return IIpcStrategy.Type.CONTENT_PROVIDER
                }
            },
            cache = cache,
            isPackageInstalled =  {
                it == prodMicrosoftAuthenticator || it == prodCompanyPortal
            },
            isValidBroker = { true }
        )

        Assert.assertEquals(prodMicrosoftAuthenticator, client.getActiveBroker())
    }

    /**
     * There is already a cached active broker, but all the apps have been uninstalled.
     **/
    @Test
    fun testCache_AppUninstalled() {
        val cache = InMemoryActiveBrokerCache()
        cache.setCachedActiveBroker(prodMicrosoftAuthenticator)

        val client = BrokerDiscoveryClient(
            brokerCandidates = setOf(
                prodMicrosoftAuthenticator, prodCompanyPortal
            ),
            getActiveBrokerFromAccountManager = {
                return@BrokerDiscoveryClient null
            },
            ipcStrategy = object : IIpcStrategy {
                override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle {
                    throw IllegalStateException()
                }
                override fun getType(): IIpcStrategy.Type {
                    return IIpcStrategy.Type.CONTENT_PROVIDER
                }
            },
            cache = cache,
            isPackageInstalled =  {
                return@BrokerDiscoveryClient false
            },
            isValidBroker = { true }
        )

        Assert.assertNull(client.getActiveBroker())
        Assert.assertNull(cache.getCachedActiveBroker())
    }

    /**
     * There is no a cached active broker, but the installed app is a malicious app (signed by unknown key)
     **/
    @Test
    fun test_ReplacedByMaliciousApp() {
        val cache = InMemoryActiveBrokerCache()

        val client = BrokerDiscoveryClient(
            brokerCandidates = setOf(
                prodMicrosoftAuthenticator, prodCompanyPortal
            ),
            getActiveBrokerFromAccountManager = {
                return@BrokerDiscoveryClient null
            },
            ipcStrategy = object : IIpcStrategy {
                override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle {
                    throw IllegalStateException()
                }
                override fun getType(): IIpcStrategy.Type {
                    return IIpcStrategy.Type.CONTENT_PROVIDER
                }
            },
            cache = InMemoryActiveBrokerCache(),
            isPackageInstalled =  { it == prodMicrosoftAuthenticator },
            isValidBroker = { false }
        )

        Assert.assertNull(client.getActiveBroker())
        Assert.assertNull(cache.getCachedActiveBroker())
    }

    /**
     * There is already a cached active broker, but the installed app is a malicious app (signed by unknown key)
     **/
    @Test
    fun testCache_ReplacedByMaliciousApp() {
        val cache = InMemoryActiveBrokerCache()
        cache.setCachedActiveBroker(prodMicrosoftAuthenticator)

        val client = BrokerDiscoveryClient(
            brokerCandidates = setOf(
                prodMicrosoftAuthenticator, prodCompanyPortal
            ),
            getActiveBrokerFromAccountManager = {
                return@BrokerDiscoveryClient null
            },
            ipcStrategy = object : IIpcStrategy {
                override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle {
                    throw IllegalStateException()
                }
                override fun getType(): IIpcStrategy.Type {
                    return IIpcStrategy.Type.CONTENT_PROVIDER
                }
            },
            cache = cache,
            isPackageInstalled =  { it == prodMicrosoftAuthenticator },
            isValidBroker = { false }
        )

        Assert.assertNull(client.getActiveBroker())
        Assert.assertNull(cache.getCachedActiveBroker())
    }

    /**
     * Authenticator is cached as active broker.
     * If queried, the actual active broker is Company Portal.
     * We're forcing to skip cache in this test.
     **/
    @Test
    fun testSkipCache() {
        val cache = InMemoryActiveBrokerCache()
        cache.setCachedActiveBroker(prodMicrosoftAuthenticator)

        val client = BrokerDiscoveryClient(
            brokerCandidates = setOf(
                prodMicrosoftAuthenticator, prodCompanyPortal
            ),
            getActiveBrokerFromAccountManager = {
                throw IllegalStateException()
            },
            ipcStrategy = object : IIpcStrategy {
                override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle {
                    if (bundle.targetBrokerAppPackageName == prodMicrosoftAuthenticator.packageName ||
                        bundle.targetBrokerAppPackageName == prodCompanyPortal.packageName) {
                        val returnBundle = Bundle()
                        returnBundle.putString(
                            BrokerDiscoveryClient.ACTIVE_BROKER_PACKAGE_NAME_BUNDLE_KEY,
                            prodCompanyPortal.packageName
                        )
                        returnBundle.putString(
                            BrokerDiscoveryClient.ACTIVE_BROKER_SIGNATURE_HASH_BUNDLE_KEY,
                            prodCompanyPortal.signingCertificateHash
                        )
                        return returnBundle
                    }

                    throw IllegalStateException()
                }
                override fun getType(): IIpcStrategy.Type {
                    return IIpcStrategy.Type.CONTENT_PROVIDER
                }
            },
            cache = cache,
            isPackageInstalled =  {
                it == prodMicrosoftAuthenticator || it == prodCompanyPortal
            },
            isValidBroker = { true }
        )

        Assert.assertEquals(prodCompanyPortal, client.getActiveBroker(shouldSkipCache = true))
        Assert.assertEquals(prodCompanyPortal, cache.getCachedActiveBroker())
    }

    /**
     * Create 3 clients. Try to make requests from multiple coroutines (same thread).
     * Only 1 IPC call should be made. The rest should read from cache.
     **/
    @Test
    fun testRaceCondition_MultiCoroutines(){
        val queriedAuthenticator = AtomicBoolean(false)
        val queriedCompanyPortal = AtomicBoolean(false)
        val cache = InMemoryActiveBrokerCache()

        val countDownLatch = CountDownLatch(3)

        val client1 = getClientForConcurrencyTest(queriedAuthenticator, queriedCompanyPortal, cache)
        val client2 = getClientForConcurrencyTest(queriedAuthenticator, queriedCompanyPortal, cache)
        val client3 = getClientForConcurrencyTest(queriedAuthenticator, queriedCompanyPortal, cache)


        // Coroutine (Same thread, multiple coroutines)
        runBlocking {
            launch {
                Assert.assertEquals(prodCompanyPortal, client1.getActiveBroker())
                Assert.assertEquals(prodCompanyPortal, client2.getActiveBroker())
                Assert.assertEquals(prodCompanyPortal, client3.getActiveBroker())
                countDownLatch.countDown()
            }
            launch {
                Assert.assertEquals(prodCompanyPortal, client2.getActiveBroker())
                Assert.assertEquals(prodCompanyPortal, client3.getActiveBroker())
                Assert.assertEquals(prodCompanyPortal, client1.getActiveBroker())
                countDownLatch.countDown()
            }
            launch {
                Assert.assertEquals(prodCompanyPortal, client3.getActiveBroker())
                Assert.assertEquals(prodCompanyPortal, client1.getActiveBroker())
                Assert.assertEquals(prodCompanyPortal, client2.getActiveBroker())
                countDownLatch.countDown()
            }
        }

        countDownLatch.await()
    }

    /**
     * Create 3 clients. Try to make requests from multiple threads.
     * Only 1 IPC call should be made. The rest should read from cache.
     **/
    @Test
    fun testRaceCondition_MultiThread(){
        val queriedAuthenticator = AtomicBoolean(false)
        val queriedCompanyPortal = AtomicBoolean(false)
        val cache = InMemoryActiveBrokerCache()

        val countDownLatch = CountDownLatch(3)

        val client1 = getClientForConcurrencyTest(queriedAuthenticator, queriedCompanyPortal, cache)
        val client2 = getClientForConcurrencyTest(queriedAuthenticator, queriedCompanyPortal, cache)
        val client3 = getClientForConcurrencyTest(queriedAuthenticator, queriedCompanyPortal, cache)

        Thread().run {
            Assert.assertEquals(prodCompanyPortal, client1.getActiveBroker())
            Assert.assertEquals(prodCompanyPortal, client2.getActiveBroker())
            Assert.assertEquals(prodCompanyPortal, client3.getActiveBroker())
            countDownLatch.countDown()
        }
        Thread().run {
            Assert.assertEquals(prodCompanyPortal, client2.getActiveBroker())
            Assert.assertEquals(prodCompanyPortal, client3.getActiveBroker())
            Assert.assertEquals(prodCompanyPortal, client1.getActiveBroker())
            countDownLatch.countDown()
        }
        Thread().run {
            Assert.assertEquals(prodCompanyPortal, client3.getActiveBroker())
            Assert.assertEquals(prodCompanyPortal, client1.getActiveBroker())
            Assert.assertEquals(prodCompanyPortal, client2.getActiveBroker())
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    // Returns a client which will throw an error if
    // AuthApp/CP is queried via IPC more than once - each.
    private fun getClientForConcurrencyTest(queriedAuthenticator: AtomicBoolean,
                                            queriedCompanyPortal: AtomicBoolean,
                                            cache: InMemoryActiveBrokerCache) : BrokerDiscoveryClient {
        return BrokerDiscoveryClient(
            brokerCandidates = setOf(
                prodMicrosoftAuthenticator, prodCompanyPortal
            ),
            getActiveBrokerFromAccountManager = {
                throw IllegalStateException("Result shouldn't be obtained from AccountManager")
            },
            ipcStrategy = object : IIpcStrategy {
                override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle {
                    if (bundle.targetBrokerAppPackageName == prodMicrosoftAuthenticator.packageName) {
                        if (!queriedAuthenticator.compareAndSet(false,true)) {
                            throw IllegalStateException("AuthApp shouldn't be invoked more than once.")
                        }

                        val returnBundle = Bundle()
                        returnBundle.putString(
                            BrokerDiscoveryClient.ACTIVE_BROKER_PACKAGE_NAME_BUNDLE_KEY,
                            prodCompanyPortal.packageName
                        )
                        returnBundle.putString(
                            BrokerDiscoveryClient.ACTIVE_BROKER_SIGNATURE_HASH_BUNDLE_KEY,
                            prodCompanyPortal.signingCertificateHash
                        )
                        return returnBundle
                    }

                    if (bundle.targetBrokerAppPackageName == prodCompanyPortal.packageName) {
                        if (!queriedCompanyPortal.compareAndSet(false, true)) {
                            throw IllegalStateException("CP shouldn't be invoked more than once.")
                        }

                        // Let's say if this guy throws an error.
                        throw UnsupportedOperationException("CP error.")
                    }

                    throw UnsupportedOperationException("Unknown broker app.")
                }
                override fun getType(): IIpcStrategy.Type {
                    return IIpcStrategy.Type.CONTENT_PROVIDER
                }
            },
            cache = cache,
            isPackageInstalled =  {
                it == prodMicrosoftAuthenticator || it == prodCompanyPortal
            },
            isValidBroker = { true }
        )
    }
}