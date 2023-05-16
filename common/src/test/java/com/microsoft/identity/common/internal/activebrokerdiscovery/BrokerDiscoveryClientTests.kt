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
import com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle
import com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.locks.ReentrantLock

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
                            prodMicrosoftAuthenticator.signatureHash
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
            }
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
            }
        )

        Assert.assertEquals(prodCompanyPortal, client.getActiveBroker())
    }

    /**
     * The brokers returns an error.
     * Account Manager is used instead.
     **/
    @Test
    fun testQuery_ErrorReturned(){
        val client = BrokerDiscoveryClient(
            brokerCandidates = setOf(
                prodMicrosoftAuthenticator, prodCompanyPortal
            ),
            getActiveBrokerFromAccountManager = {
                return@BrokerDiscoveryClient prodCompanyPortal
            },
            ipcStrategy = object : IIpcStrategy {
                override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle {
                    throw UnsupportedOperationException()
                }
                override fun getType(): IIpcStrategy.Type {
                    return IIpcStrategy.Type.CONTENT_PROVIDER
                }
            },
            cache = InMemoryActiveBrokerCache(),
            isPackageInstalled =  {
                it == prodMicrosoftAuthenticator || it == prodCompanyPortal
            }
        )

        Assert.assertEquals(prodCompanyPortal, client.getActiveBroker())
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
            }
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
            }
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
            }
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
                            prodCompanyPortal.signatureHash
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
            }
        )

        Assert.assertEquals(prodCompanyPortal, client.getActiveBroker(shouldSkipCache = true))
        Assert.assertEquals(prodCompanyPortal, cache.getCachedActiveBroker())
    }

    /**
     * Create 3 clients. Try to make concurrent request.
     * Only 1 IPC call should be made.
     **/
    @Test
    fun testRaceCondition(){
        val cache = InMemoryActiveBrokerCache()
        val lock = ReentrantLock()
        var count = 0

        fun getClient(): IBrokerDiscoveryClient {
            return BrokerDiscoveryClient(
                brokerCandidates = setOf(
                    prodMicrosoftAuthenticator, prodCompanyPortal
                ),
                getActiveBrokerFromAccountManager = {
                    throw IllegalStateException()
                },
                ipcStrategy = object : IIpcStrategy {
                    override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle {
                        lock.lock()
                        count++
                        if (count == 2) {
                            // Throw an exception if this is being invoked twice.
                            // The value should be read from cache.
                            throw IllegalStateException()
                        }
                        lock.unlock()

                        if (bundle.targetBrokerAppPackageName == prodMicrosoftAuthenticator.packageName ||
                            bundle.targetBrokerAppPackageName == prodCompanyPortal.packageName) {
                            val returnBundle = Bundle()
                            returnBundle.putString(
                                BrokerDiscoveryClient.ACTIVE_BROKER_PACKAGE_NAME_BUNDLE_KEY,
                                prodCompanyPortal.packageName
                            )
                            returnBundle.putString(
                                BrokerDiscoveryClient.ACTIVE_BROKER_SIGNATURE_HASH_BUNDLE_KEY,
                                prodCompanyPortal.signatureHash
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
                lock = lock
            )
        }

        val client1 = getClient()
        val client2 = getClient()
        val client3 = getClient()

        runBlocking {
            launch {
                Assert.assertEquals(prodCompanyPortal, client1.getActiveBroker(shouldSkipCache = true))
                Assert.assertEquals(prodCompanyPortal, client2.getActiveBroker(shouldSkipCache = true))
                Assert.assertEquals(prodCompanyPortal, client3.getActiveBroker(shouldSkipCache = true))
            }
            launch {
                Assert.assertEquals(prodCompanyPortal, client2.getActiveBroker(shouldSkipCache = true))
                Assert.assertEquals(prodCompanyPortal, client1.getActiveBroker(shouldSkipCache = true))
                Assert.assertEquals(prodCompanyPortal, client3.getActiveBroker(shouldSkipCache = true))
            }
            launch {
                Assert.assertEquals(prodCompanyPortal, client3.getActiveBroker(shouldSkipCache = true))
                Assert.assertEquals(prodCompanyPortal, client1.getActiveBroker(shouldSkipCache = true))
                Assert.assertEquals(prodCompanyPortal, client2.getActiveBroker(shouldSkipCache = true))
            }
        }
    }
}