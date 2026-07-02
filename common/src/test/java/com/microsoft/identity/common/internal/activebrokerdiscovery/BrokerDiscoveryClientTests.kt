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

import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.microsoft.identity.common.adal.internal.AuthenticationSettings
import com.microsoft.identity.common.components.AndroidStorageSupplier
import com.microsoft.identity.common.crypto.AndroidAuthSdkStorageEncryptionManager
import com.microsoft.identity.common.crypto.MockData
import com.microsoft.identity.common.exception.BrokerCommunicationException
import com.microsoft.identity.common.internal.broker.BrokerData
import com.microsoft.identity.common.internal.broker.BrokerData.Companion.prodCompanyPortal
import com.microsoft.identity.common.internal.broker.BrokerData.Companion.prodMicrosoftAuthenticator
import com.microsoft.identity.common.internal.broker.ipc.AbstractIpcStrategyWithServiceValidation
import com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle
import com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy
import com.microsoft.identity.common.internal.cache.ClientActiveBrokerCache
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager
import com.microsoft.identity.common.java.crypto.StorageEncryptionManager
import com.microsoft.identity.common.java.crypto.key.AES256SecretKeyGenerator
import com.microsoft.identity.common.java.crypto.key.ISecretKeyProvider
import com.microsoft.identity.common.java.crypto.key.PredefinedKeyProvider
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.exception.ClientException.ONLY_SUPPORTS_ACCOUNT_MANAGER_ERROR_CODE
import com.microsoft.identity.common.java.exception.ErrorStrings
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
class BrokerDiscoveryClientTests {

    companion object {
        /**
         * Mirrors ClientActiveBrokerCache.BROKER_METADATA_CACHE_STORE_ON_BROKER_SDK_SIDE_STORAGE_NAME
         * (which is private). Centralized here to reduce drift if the production name changes.
         */
        private const val BROKER_SDK_CACHE_FILE_NAME = "BROKER_METADATA_CACHE_STORE_ON_BROKER_SDK_SIDE"
    }

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
                            BrokerDiscoveryClient.ACTIVE_BROKER_SIGNING_CERTIFICATE_THUMBPRINT_BUNDLE_KEY,
                            prodMicrosoftAuthenticator.signingCertificateThumbprint
                        )
                        return returnBundle
                    }

                    throw IllegalStateException()
                }
                override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String): Boolean {
                    return true
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
                override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String): Boolean {
                    return true
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
                override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String): Boolean {
                    return true
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
                override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String): Boolean {
                    return true
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
                override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String): Boolean {
                    return true
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

    @Test
    fun testCache_BrokerDoesNotSupportCurrentIpc() {
        val cache = InMemoryActiveBrokerCache()
        cache.setCachedActiveBroker(prodMicrosoftAuthenticator)

        val client = BrokerDiscoveryClient(
            brokerCandidates = setOf(
                prodMicrosoftAuthenticator, prodCompanyPortal
            ),
            getActiveBrokerFromAccountManager = {
                throw IllegalStateException("AccountManager should not be used when another broker supports IPC.")
            },
            ipcStrategy = object : AbstractIpcStrategyWithServiceValidation() {
                override fun communicateToBrokerAfterValidation(bundle: BrokerOperationBundle): Bundle {
                    if (bundle.targetBrokerAppPackageName == prodCompanyPortal.packageName) {
                        val returnBundle = Bundle()
                        returnBundle.putString(
                            BrokerDiscoveryClient.ACTIVE_BROKER_PACKAGE_NAME_BUNDLE_KEY,
                            prodCompanyPortal.packageName
                        )
                        returnBundle.putString(
                            BrokerDiscoveryClient.ACTIVE_BROKER_SIGNING_CERTIFICATE_THUMBPRINT_BUNDLE_KEY,
                            prodCompanyPortal.signingCertificateThumbprint
                        )
                        return returnBundle
                    }
                    throw IllegalStateException("Unexpected package: ${bundle.targetBrokerAppPackageName}")
                }

                override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String): Boolean {
                    return targetedBrokerPackageName == prodCompanyPortal.packageName
                }

                override fun getType(): IIpcStrategy.Type {
                    return IIpcStrategy.Type.CONTENT_PROVIDER
                }
            },
            cache = cache,
            isPackageInstalled = {
                it == prodMicrosoftAuthenticator || it == prodCompanyPortal
            },
            isValidBroker = { true }
        )

        Assert.assertEquals(prodCompanyPortal, client.getActiveBroker())
        Assert.assertEquals(prodCompanyPortal, cache.getCachedActiveBroker())
    }

    @Test
    fun testInMemoryCache_BrokerDoesNotSupportCurrentIpc() {
        val cache = InMemoryActiveBrokerCache()
        cache.setCachedActiveBroker(prodMicrosoftAuthenticator)

        val client = BrokerDiscoveryClient(
            brokerCandidates = setOf(
                prodMicrosoftAuthenticator, prodCompanyPortal
            ),
            getActiveBrokerFromAccountManager = {
                throw IllegalStateException("AccountManager should not be used when another broker supports IPC.")
            },
            ipcStrategy = object : AbstractIpcStrategyWithServiceValidation() {
                override fun communicateToBrokerAfterValidation(bundle: BrokerOperationBundle): Bundle {
                    if (bundle.targetBrokerAppPackageName == prodCompanyPortal.packageName) {
                        val returnBundle = Bundle()
                        returnBundle.putString(
                            BrokerDiscoveryClient.ACTIVE_BROKER_PACKAGE_NAME_BUNDLE_KEY,
                            prodCompanyPortal.packageName
                        )
                        returnBundle.putString(
                            BrokerDiscoveryClient.ACTIVE_BROKER_SIGNING_CERTIFICATE_THUMBPRINT_BUNDLE_KEY,
                            prodCompanyPortal.signingCertificateThumbprint
                        )
                        return returnBundle
                    }
                    throw IllegalStateException("Unexpected package: ${bundle.targetBrokerAppPackageName}")
                }

                override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String): Boolean {
                    return targetedBrokerPackageName == prodCompanyPortal.packageName
                }

                override fun getType(): IIpcStrategy.Type {
                    return IIpcStrategy.Type.CONTENT_PROVIDER
                }
            },
            cache = cache,
            isPackageInstalled = {
                it == prodMicrosoftAuthenticator || it == prodCompanyPortal
            },
            isValidBroker = { true }
        )
        client.cachedData = BrokerDiscoveryClient.CachedBrokerData(prodMicrosoftAuthenticator)

        Assert.assertEquals(prodCompanyPortal, client.getActiveBrokerWithInMemoryCache(null))
        Assert.assertEquals(prodCompanyPortal, cache.getCachedActiveBroker())
        Assert.assertEquals(prodCompanyPortal, client.cachedData?.brokerData)
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
                override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String): Boolean {
                    return true
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
                override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String): Boolean {
                    return true
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
                override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String): Boolean {
                    return true
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
                            BrokerDiscoveryClient.ACTIVE_BROKER_SIGNING_CERTIFICATE_THUMBPRINT_BUNDLE_KEY,
                            prodCompanyPortal.signingCertificateThumbprint
                        )
                        return returnBundle
                    }

                    throw IllegalStateException()
                }
                override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String): Boolean {
                    return true
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
                            BrokerDiscoveryClient.ACTIVE_BROKER_SIGNING_CERTIFICATE_THUMBPRINT_BUNDLE_KEY,
                            prodCompanyPortal.signingCertificateThumbprint
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
                override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String): Boolean {
                    return true
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

    /**
     * Test if authapp does not support broker discovery, but cp does.
     * */
    @Test
    fun testOneAppDoesNotSupportNewBrokerDiscovery() {
        val cache = InMemoryActiveBrokerCache()

        val client = BrokerDiscoveryClient(
            brokerCandidates = setOf(
                prodMicrosoftAuthenticator, prodCompanyPortal
            ),
            getActiveBrokerFromAccountManager = {
                throw IllegalStateException()
            },
            ipcStrategy = object : IIpcStrategy {
                override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle {
                    if (bundle.targetBrokerAppPackageName == prodCompanyPortal.packageName) {
                        val returnBundle = Bundle()
                        returnBundle.putString(
                            BrokerDiscoveryClient.ACTIVE_BROKER_PACKAGE_NAME_BUNDLE_KEY,
                            prodCompanyPortal.packageName
                        )
                        returnBundle.putString(
                            BrokerDiscoveryClient.ACTIVE_BROKER_SIGNING_CERTIFICATE_THUMBPRINT_BUNDLE_KEY,
                            prodCompanyPortal.signingCertificateThumbprint
                        )
                        return returnBundle
                    }

                    throw BrokerCommunicationException(
                        BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE,
                        IIpcStrategy.Type.CONTENT_PROVIDER,
                        null,
                        null
                    )
                }
                override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String): Boolean {
                    return prodMicrosoftAuthenticator.packageName == targetedBrokerPackageName
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
        Assert.assertFalse(cache.shouldUseAccountManager())
    }

    /**
     * Making 2 calls, the first one when the broker is not installed.
     * Then make a request after the broker is installed.
     * */
    @Test
    fun testBrokerAppRecentlyInstalled(){
        val cache = InMemoryActiveBrokerCache()
        var installedBrokerApp: BrokerData? = null

        val client = BrokerDiscoveryClient(
            brokerCandidates = setOf(
                prodMicrosoftAuthenticator, prodCompanyPortal
            ),
            getActiveBrokerFromAccountManager = {
                return@BrokerDiscoveryClient installedBrokerApp
            },
            ipcStrategy = object : IIpcStrategy {
                override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle {
                    installedBrokerApp?.let {
                        if (bundle.targetBrokerAppPackageName == it.packageName) {
                            val returnBundle = Bundle()
                            returnBundle.putString(
                                BrokerDiscoveryClient.ACTIVE_BROKER_PACKAGE_NAME_BUNDLE_KEY,
                                it.packageName
                            )
                            returnBundle.putString(
                                BrokerDiscoveryClient.ACTIVE_BROKER_SIGNING_CERTIFICATE_THUMBPRINT_BUNDLE_KEY,
                                it.signingCertificateThumbprint
                            )
                            return returnBundle
                        }
                    }

                    throw IllegalStateException()
                }
                override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String): Boolean {
                    return true
                }
                override fun getType(): IIpcStrategy.Type {
                    return IIpcStrategy.Type.CONTENT_PROVIDER
                }
            },
            cache = cache,
            isPackageInstalled =  {
                return@BrokerDiscoveryClient installedBrokerApp != null
            },
            isValidBroker = { true }
        )

        // First call, no broker installed.
        Assert.assertNull(client.getActiveBroker())

        // Then install Authenticator!
        installedBrokerApp = prodMicrosoftAuthenticator

        // Second call, Authenticator is installed.
        Assert.assertEquals(prodMicrosoftAuthenticator, client.getActiveBroker())
    }

    /**
     * Test that the value is read from in memory cache, not going through the discovery flow or even the storage.
     * For this test, we'll persist a value in storage. After the first read, the storage should never be accessed again.
     **/
    @Test
    fun testReadFromInMemoryCache_WithValueInStorage() {
        val cache = object : InMemoryActiveBrokerCache() {
            var readCount = 0
            override fun getCachedActiveBroker(): BrokerData? {
                readCount++
                return super.getCachedActiveBroker()
            }
        }
        cache.setCachedActiveBroker(prodMicrosoftAuthenticator)

        val client = BrokerDiscoveryClient(
            brokerCandidates = setOf(
                prodMicrosoftAuthenticator, prodCompanyPortal
            ),
            getActiveBrokerFromAccountManager = {
                throw IllegalStateException("getActiveBrokerFromAccountManager should not be called when reading from cache")
            },
            ipcStrategy = object : IIpcStrategy {
                override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle {
                    throw IllegalStateException("communicateToBroker should not be called when reading from cache")
                }
                override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String): Boolean {
                    throw IllegalStateException("isSupportedByTargetedBroker should not be called when reading from cache")
                }
                override fun getType(): IIpcStrategy.Type {
                    return IIpcStrategy.Type.CONTENT_PROVIDER
                }
            },
            cache = cache,
            isPackageInstalled = { packageName ->
                packageName == prodMicrosoftAuthenticator
            },
            isValidBroker = { brokerData ->
                brokerData == prodMicrosoftAuthenticator
            }
        )

        Assert.assertNull(client.cachedData)
        Assert.assertEquals(0, cache.readCount)

        // Trigger "reading from storage"
        val result = client.getActiveBrokerWithInMemoryCache(null)

        Assert.assertEquals(prodMicrosoftAuthenticator, result)
        Assert.assertEquals(prodMicrosoftAuthenticator, client.cachedData!!.brokerData)
        Assert.assertEquals(1, cache.readCount)

        // If we invoke the API again, the read count should not increase, as the value is read from in-memory cache.
        client.getActiveBrokerWithInMemoryCache(null)
        Assert.assertEquals(prodMicrosoftAuthenticator, client.cachedData!!.brokerData)
        Assert.assertEquals(1, cache.readCount)
    }

    /**
     * Test that the value is read from in memory cache, not going through the discovery flow or even the storage.
     **/
    @Test
    fun testReadFromInMemoryCache_WithEmptyStorage() {
        val cache = object : InMemoryActiveBrokerCache() {
            var readCount = 0
            override fun getCachedActiveBroker(): BrokerData? {
                readCount++
                return super.getCachedActiveBroker()
            }
        }

        val client = BrokerDiscoveryClient(
            brokerCandidates = setOf(
                prodMicrosoftAuthenticator, prodCompanyPortal
            ),
            getActiveBrokerFromAccountManager = {
                throw IllegalStateException("getActiveBrokerFromAccountManager should not be called when the ipc operation succeeded.")
            },
            ipcStrategy = object : IIpcStrategy {
                override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle {
                    if (bundle.targetBrokerAppPackageName == prodMicrosoftAuthenticator.packageName) {
                        val returnBundle = Bundle()
                        returnBundle.putString(
                            BrokerDiscoveryClient.ACTIVE_BROKER_PACKAGE_NAME_BUNDLE_KEY,
                            prodMicrosoftAuthenticator.packageName
                        )
                        returnBundle.putString(
                            BrokerDiscoveryClient.ACTIVE_BROKER_SIGNING_CERTIFICATE_THUMBPRINT_BUNDLE_KEY,
                            prodMicrosoftAuthenticator.signingCertificateThumbprint
                        )
                        return returnBundle
                    }

                    throw IllegalStateException()
                }
                override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String): Boolean {
                    return true
                }
                override fun getType(): IIpcStrategy.Type {
                    return IIpcStrategy.Type.CONTENT_PROVIDER
                }
            },
            cache = cache,
            isPackageInstalled = { packageName ->
                packageName == prodMicrosoftAuthenticator
            },
            isValidBroker = { brokerData ->
                brokerData == prodMicrosoftAuthenticator
            }
        )

        Assert.assertNull(client.cachedData)
        Assert.assertEquals(0, cache.readCount)

        // Trigger "reading from storage"
        val result = client.getActiveBrokerWithInMemoryCache(null)

        Assert.assertEquals(prodMicrosoftAuthenticator, result)
        Assert.assertEquals(prodMicrosoftAuthenticator, cache.activeBroker)
        Assert.assertEquals(prodMicrosoftAuthenticator, client.cachedData!!.brokerData)
        Assert.assertEquals(1, cache.readCount)

        // If we invoke the API again, the read count should not increase, as the value is read from in-memory cache.
        client.getActiveBrokerWithInMemoryCache(null)
        Assert.assertEquals(prodMicrosoftAuthenticator, client.cachedData!!.brokerData)
        Assert.assertEquals(1, cache.readCount)
    }

    /**
     * Test that if the broker is not installed, we will never perform any IPC or storage read after the first attempt.
     **/
    @Test
    fun testReadFromInMemoryCache_BrokerNotInstalled() {
        val cache = object : InMemoryActiveBrokerCache() {
            var readCount = 0
            override fun getCachedActiveBroker(): BrokerData? {
                readCount++
                return super.getCachedActiveBroker()
            }
        }

        var accountManagerReadCount = 0
        val client = BrokerDiscoveryClient(
            brokerCandidates = setOf(
                prodMicrosoftAuthenticator, prodCompanyPortal
            ),
            getActiveBrokerFromAccountManager = {
                accountManagerReadCount++
                null
            },
            ipcStrategy = object : IIpcStrategy {
                override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle {
                    throw IllegalStateException()
                }
                override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String): Boolean {
                    return true
                }
                override fun getType(): IIpcStrategy.Type {
                    return IIpcStrategy.Type.CONTENT_PROVIDER
                }
            },
            cache = cache,
            isPackageInstalled = { false },
            isValidBroker = { false }
        )

        Assert.assertNull(client.cachedData)
        Assert.assertEquals(0, cache.readCount)

        // Trigger "reading from storage"
        val result = client.getActiveBrokerWithInMemoryCache(null)

        Assert.assertNull(result)
        Assert.assertNull(cache.activeBroker)
        Assert.assertNull(client.cachedData!!.brokerData)
        Assert.assertEquals(1, cache.readCount)
        Assert.assertEquals(1, accountManagerReadCount)

        // If we invoke the API again, the read counts should not increase, as the value is read from in-memory cache.
        client.getActiveBrokerWithInMemoryCache(null)
        Assert.assertNull(client.cachedData!!.brokerData)
        Assert.assertEquals(1, cache.readCount)
        Assert.assertEquals(1, accountManagerReadCount)
    }

    /**
     * Test concurrent access to in-memory cache from multiple coroutines.
     * All coroutines should read from cache without triggering discovery flow or storage operations.
     **/
    @Test
    fun testReadFromInMemoryCache_ConcurrentCoroutines() {
        val cache = object : InMemoryActiveBrokerCache() {
            val readCount = AtomicInteger(0)
            override fun getCachedActiveBroker(): BrokerData? {
                readCount.incrementAndGet()
                return super.getCachedActiveBroker()
            }
        }
        // Pre-populate the cache with Microsoft Authenticator as the active broker
        cache.setCachedActiveBroker(prodMicrosoftAuthenticator)

        // Create multiple clients that share the same cache
        val client = getClientForInMemoryCacheTest(cache)

        // Run multiple coroutines concurrently (same thread, multiple coroutines)
        runBlocking {
            launch {
                Assert.assertEquals(prodMicrosoftAuthenticator, client.getActiveBrokerWithInMemoryCache(null))
                Assert.assertEquals(prodMicrosoftAuthenticator, client.getActiveBrokerWithInMemoryCache(null))
                Assert.assertEquals(prodMicrosoftAuthenticator, client.getActiveBrokerWithInMemoryCache(null))
            }
            launch {
                Assert.assertEquals(prodMicrosoftAuthenticator, client.getActiveBrokerWithInMemoryCache(null))
                Assert.assertEquals(prodMicrosoftAuthenticator, client.getActiveBrokerWithInMemoryCache(null))
                Assert.assertEquals(prodMicrosoftAuthenticator, client.getActiveBrokerWithInMemoryCache(null))
            }
            launch {
                Assert.assertEquals(prodMicrosoftAuthenticator, client.getActiveBrokerWithInMemoryCache(null))
                Assert.assertEquals(prodMicrosoftAuthenticator, client.getActiveBrokerWithInMemoryCache(null))
                Assert.assertEquals(prodMicrosoftAuthenticator, client.getActiveBrokerWithInMemoryCache(null))
            }
        }

        // Verify the cache state after all concurrent operations
        Assert.assertEquals(prodMicrosoftAuthenticator, client.cachedData!!.brokerData)
        Assert.assertEquals(1, cache.readCount.get())
    }

    /**
     * Test concurrent access to in-memory cache from multiple threads.
     * All threads should read from cache without triggering discovery flow or storage operations.
     **/
    @Test
    fun testReadFromInMemoryCache_Concurrent() {
        val cache = object : InMemoryActiveBrokerCache() {
            val readCount = AtomicInteger(0)
            override fun getCachedActiveBroker(): BrokerData? {
                readCount.incrementAndGet()
                return super.getCachedActiveBroker()
            }
        }
        // Pre-populate the cache with Microsoft Authenticator as the active broker
        cache.setCachedActiveBroker(prodMicrosoftAuthenticator)

        val countDownLatch = CountDownLatch(3)
        val client = getClientForInMemoryCacheTest(cache)

        // Start multiple threads that all try to get the active broker
        Thread {
            try {
                Assert.assertEquals(prodMicrosoftAuthenticator, client.getActiveBrokerWithInMemoryCache(null))
                Assert.assertEquals(prodMicrosoftAuthenticator, client.getActiveBrokerWithInMemoryCache(null))
                Assert.assertEquals(prodMicrosoftAuthenticator, client.getActiveBrokerWithInMemoryCache(null))
            } finally {
                countDownLatch.countDown()
            }
        }.start()

        Thread {
            try {
                Assert.assertEquals(prodMicrosoftAuthenticator, client.getActiveBrokerWithInMemoryCache(null))
                Assert.assertEquals(prodMicrosoftAuthenticator, client.getActiveBrokerWithInMemoryCache(null))
                Assert.assertEquals(prodMicrosoftAuthenticator, client.getActiveBrokerWithInMemoryCache(null))
            } finally {
                countDownLatch.countDown()
            }
        }.start()

        Thread {
            try {
                Assert.assertEquals(prodMicrosoftAuthenticator, client.getActiveBrokerWithInMemoryCache(null))
                Assert.assertEquals(prodMicrosoftAuthenticator, client.getActiveBrokerWithInMemoryCache(null))
                Assert.assertEquals(prodMicrosoftAuthenticator, client.getActiveBrokerWithInMemoryCache(null))
            } finally {
                countDownLatch.countDown()
            }
        }.start()

        // Wait for all threads to complete
        countDownLatch.await()

        // Verify the cache state after all concurrent operations
        Assert.assertEquals(prodMicrosoftAuthenticator, client.cachedData!!.brokerData)
        Assert.assertEquals(1, cache.readCount.get())
    }

    // Helper method to create a client that will fail if any discovery operations are triggered
    private fun getClientForInMemoryCacheTest(cache: InMemoryActiveBrokerCache): BrokerDiscoveryClient {
        return BrokerDiscoveryClient(
            brokerCandidates = setOf(
                prodMicrosoftAuthenticator, prodCompanyPortal
            ),
            getActiveBrokerFromAccountManager = {
                throw IllegalStateException("getActiveBrokerFromAccountManager should not be called when reading from cache")
            },
            ipcStrategy = object : IIpcStrategy {
                override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle {
                    throw IllegalStateException("communicateToBroker should not be called when reading from cache")
                }
                override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String): Boolean {
                    throw IllegalStateException("isSupportedByTargetedBroker should not be called when reading from cache")
                }
                override fun getType(): IIpcStrategy.Type {
                    return IIpcStrategy.Type.CONTENT_PROVIDER
                }
            },
            cache = cache,
            isPackageInstalled = { brokerData ->
                brokerData == prodMicrosoftAuthenticator || brokerData == prodCompanyPortal
            },
            isValidBroker = { brokerData ->
                brokerData == prodMicrosoftAuthenticator || brokerData == prodCompanyPortal
            }
        )
    }

    /**
     * End-to-end test for the encryption key mismatch bug.
     *
     * Simulates Company Portal scenario:
     * 1. MSAL sets a predefined key → cache data is encrypted with it (U001 prefix).
     * 2. On next launch, WPJ API uses a different key for encryption (simulating keystore).
     * 3. Reading cache fails decryption (ClientException caught by EncryptedNameValueStorage) → null.
     * 4. BrokerDiscoveryClient falls back to IPC discovery — no crash.
     * 5. After IPC, cache is re-populated with the new key and is readable.
     *
     * Uses real StorageEncryptionManager subclasses and ClientActiveBrokerCache.
     * Uses a second PredefinedKeyProvider to stand in for the Android KeyStore key
     * (which is unavailable in Robolectric).
     */
    @Test
    fun testCacheDecryptionFailure_FallsBackToIpcDiscovery() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // Ensure test isolation: clear any leftover data in the broker SDK cache SharedPreferences
        // and the SharedPreferencesFileManager singleton cache.
        context.getSharedPreferences(BROKER_SDK_CACHE_FILE_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()
        SharedPreferencesFileManager.clearSingletonCache()
        AuthenticationSettings.INSTANCE.clearSecretKeysForTestCases()

        try {
            // A mock "keystore" key provider using a different key AND a different identifier than
            // the predefined one. PredefinedKeyProvider always uses "U001", so we need a custom
            // ISecretKeyProvider with a distinct identifier (e.g. "KS01") to simulate the real
            // KeyStoreBackedSecretKeyProvider behavior.
            val mockKeystoreKeyProvider = object : ISecretKeyProvider {
                override val alias: String = "MOCK_KEYSTORE_KEY"
                override val keyTypeIdentifier: String = "KS01"
                override val key: javax.crypto.SecretKey = AES256SecretKeyGenerator.generateKeyFromRawBytes(MockData.ANOTHER_PREDEFINED_KEY)
                override val cipherTransformation: String = "AES/CBC/PKCS5Padding"
            }

            // Step 1: Write cache data using the predefined key (simulates MSAL-initialized path).
            AuthenticationSettings.INSTANCE.setSecretKey(MockData.PREDEFINED_KEY)
            val writeEncryptionManager = AndroidAuthSdkStorageEncryptionManager(context)
            val writeSupplier = AndroidStorageSupplier(context, writeEncryptionManager)
            val writeCache = ClientActiveBrokerCache.getBrokerSdkCache(writeSupplier)
            writeCache.setCachedActiveBroker(prodMicrosoftAuthenticator)

            // Verify data was written successfully.
            Assert.assertEquals(prodMicrosoftAuthenticator, writeCache.getCachedActiveBroker())

            // Step 2: Clear the predefined key and SharedPreferencesFileManager cache
            //         (simulates app restart where MSAL hasn't initialized).
            AuthenticationSettings.INSTANCE.clearSecretKeysForTestCases()
            SharedPreferencesFileManager.clearSingletonCache()

            // Step 3: Create a "keystore-only" encryption manager that uses the mock keystore key
            //         and throws ClientException for U001-encrypted data (simulates the fix).
            val readEncryptionManager = object : StorageEncryptionManager() {
                override fun getKeyProviderForEncryption(): ISecretKeyProvider {
                    return mockKeystoreKeyProvider
                }

                override fun getKeyProviderForDecryption(cipherText: ByteArray): List<ISecretKeyProvider> {
                    val keyIdentifier = getKeyIdentifierFromCipherText(cipherText)
                    if (PredefinedKeyProvider.USER_PROVIDED_KEY_IDENTIFIER.equals(keyIdentifier, ignoreCase = true)) {
                        throw ClientException(
                            ErrorStrings.DECRYPTION_FAILED,
                            "Cipher Text is encrypted by USER_PROVIDED_KEY_IDENTIFIER, " +
                                    "but mPredefinedKeyProvider is null."
                        )
                    }
                    // For data encrypted with the mock keystore key ("KS01"), return it.
                    return listOf(mockKeystoreKeyProvider)
                }
            }
            val readSupplier = AndroidStorageSupplier(context, readEncryptionManager)
            val readCache = ClientActiveBrokerCache.getBrokerSdkCache(readSupplier)

            // Step 4: Use this cache in BrokerDiscoveryClient — should fall back to IPC, not crash.
            val client = BrokerDiscoveryClient(
                brokerCandidates = setOf(
                    prodMicrosoftAuthenticator, prodCompanyPortal
                ),
                getActiveBrokerFromAccountManager = {
                    throw IllegalStateException("Should not fall back to AccountManager when IPC succeeds")
                },
                ipcStrategy = object : IIpcStrategy {
                    override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle {
                        val returnBundle = Bundle()
                        returnBundle.putString(
                            BrokerDiscoveryClient.ACTIVE_BROKER_PACKAGE_NAME_BUNDLE_KEY,
                            prodCompanyPortal.packageName
                        )
                        returnBundle.putString(
                            BrokerDiscoveryClient.ACTIVE_BROKER_SIGNING_CERTIFICATE_THUMBPRINT_BUNDLE_KEY,
                            prodCompanyPortal.signingCertificateThumbprint
                        )
                        return returnBundle
                    }
                    override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String): Boolean {
                        return true
                    }
                    override fun getType(): IIpcStrategy.Type {
                        return IIpcStrategy.Type.CONTENT_PROVIDER
                    }
                },
                cache = readCache,
                isPackageInstalled = {
                    it == prodMicrosoftAuthenticator || it == prodCompanyPortal
                },
                isValidBroker = { true }
            )

            // Should NOT crash — should fall back to IPC and discover Company Portal.
            val result = client.getActiveBroker()
            Assert.assertEquals(prodCompanyPortal, result)

            // After IPC re-populates the cache, it should be readable with the mock keystore key.
            Assert.assertEquals(prodCompanyPortal, readCache.getCachedActiveBroker())
        } finally {
            // Restore global state to avoid leaking into other tests.
            context.getSharedPreferences(BROKER_SDK_CACHE_FILE_NAME, Context.MODE_PRIVATE)
                .edit().clear().commit()
            SharedPreferencesFileManager.clearSingletonCache()
            AuthenticationSettings.INSTANCE.clearSecretKeysForTestCases()
        }
    }
}