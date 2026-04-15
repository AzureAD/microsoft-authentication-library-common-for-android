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
package com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory

import com.microsoft.identity.common.java.authorities.Authority
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAuthority
import com.microsoft.identity.common.java.authorities.Environment
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import com.microsoft.identity.common.java.flighting.MockFlightsManager
import com.microsoft.identity.common.java.flighting.MockFlightsProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Tests for sovereign cloud discovery support in [AzureActiveDirectory].
 * Focuses on pre-seeding, isKnownCloudDiscoveryHost, cache-based discovery gating,
 * and environment changes.
 */
class AzureActiveDirectoryTest {

    @Before
    fun setup() {
        AzureActiveDirectory.setEnvironment(Environment.Production)
    }

    @After
    fun tearDown() {
        AzureActiveDirectory.setEnvironment(Environment.Production)
        CommonFlightsManager.resetFlightsManager()
    }

    // --- isKnownCloudDiscoveryHost tests ---

    @Test
    fun testIsKnownCloudDiscoveryHost_bleu() {
        assertTrue(AzureActiveDirectory.isKnownCloudDiscoveryHost("login.sovcloud-identity.fr"))
    }

    @Test
    fun testIsKnownCloudDiscoveryHost_delos() {
        assertTrue(AzureActiveDirectory.isKnownCloudDiscoveryHost("login.sovcloud-identity.de"))
    }

    @Test
    fun testIsKnownCloudDiscoveryHost_govsg() {
        assertTrue(AzureActiveDirectory.isKnownCloudDiscoveryHost("login.sovcloud-identity.sg"))
    }

    @Test
    fun testIsKnownCloudDiscoveryHost_publicCloud() {
        assertTrue(AzureActiveDirectory.isKnownCloudDiscoveryHost("login.microsoftonline.com"))
    }

    @Test
    fun testIsKnownCloudDiscoveryHost_nationalClouds() {
        assertTrue(AzureActiveDirectory.isKnownCloudDiscoveryHost("login.partner.microsoftonline.cn"))
        assertTrue(AzureActiveDirectory.isKnownCloudDiscoveryHost("login.microsoftonline.us"))
    }

    @Test
    fun testIsKnownCloudDiscoveryHost_caseInsensitive() {
        assertTrue(AzureActiveDirectory.isKnownCloudDiscoveryHost("LOGIN.SOVCLOUD-IDENTITY.FR"))
        assertTrue(AzureActiveDirectory.isKnownCloudDiscoveryHost("Login.Microsoftonline.Com"))
    }

    @Test
    fun testIsKnownCloudDiscoveryHost_unknownHost() {
        assertFalse(AzureActiveDirectory.isKnownCloudDiscoveryHost("example.com"))
        assertFalse(AzureActiveDirectory.isKnownCloudDiscoveryHost("login.unknown.com"))
    }

    // --- Pre-seeding tests ---

    @Test
    fun testSovereignCloudsPreSeededInCache() {
        assertTrue(AzureActiveDirectory.hasCloudHost(URL("https://login.sovcloud-identity.fr/common")))
        assertTrue(AzureActiveDirectory.hasCloudHost(URL("https://login.sovcloud-identity.de/common")))
        assertTrue(AzureActiveDirectory.hasCloudHost(URL("https://login.sovcloud-identity.sg/common")))
    }

    @Test
    fun testPreSeededCloudMetadataIsCorrect() {
        val bleuCloud = AzureActiveDirectory.getAzureActiveDirectoryCloud(
            URL("https://login.sovcloud-identity.fr/common")
        )
        assertNotNull(bleuCloud)
        assertEquals("login.sovcloud-identity.fr", bleuCloud.preferredNetworkHostName)
        assertEquals("login.sovcloud-identity.fr", bleuCloud.preferredCacheHostName)
    }

    @Test
    fun testPreSeededCloudsAreValidated() {
        assertTrue(AzureActiveDirectory.isValidCloudHost(URL("https://login.sovcloud-identity.fr/common")))
        assertTrue(AzureActiveDirectory.isValidCloudHost(URL("https://login.sovcloud-identity.de/common")))
        assertTrue(AzureActiveDirectory.isValidCloudHost(URL("https://login.sovcloud-identity.sg/common")))
    }

    // --- getDefaultCloudUrl tests ---

    @Test
    fun testGetDefaultCloudUrl_production() {
        AzureActiveDirectory.setEnvironment(Environment.Production)
        assertEquals(AzureActiveDirectoryEnvironment.PRODUCTION_CLOUD_URL, AzureActiveDirectory.getDefaultCloudUrl())
    }

    @Test
    fun testGetDefaultCloudUrl_preProduction() {
        AzureActiveDirectory.setEnvironment(Environment.PreProduction)
        assertEquals(AzureActiveDirectoryEnvironment.PREPRODUCTION_CLOUD_URL, AzureActiveDirectory.getDefaultCloudUrl())
    }

    // --- hasCloudHost / isValidCloudHost tests ---

    @Test
    fun testHasCloudHost_unknownHost() {
        assertFalse(AzureActiveDirectory.hasCloudHost(URL("https://login.example.com/common")))
    }

    @Test
    fun testPutCloud_makesHostAvailable() {
        val host = "login.testcloud.com"
        val cloud = AzureActiveDirectoryCloud(host, host)
        AzureActiveDirectory.putCloud(host, cloud)

        assertTrue(AzureActiveDirectory.hasCloudHost(URL("https://$host/common")))
        assertTrue(AzureActiveDirectory.isValidCloudHost(URL("https://$host/common")))
    }

    @Test
    fun testIsValidCloudHost_unvalidatedCloud() {
        val host = "login.unvalidated.com"
        AzureActiveDirectory.putCloud(host, AzureActiveDirectoryCloud(false))

        assertTrue(AzureActiveDirectory.hasCloudHost(URL("https://$host/common")))
        assertFalse(AzureActiveDirectory.isValidCloudHost(URL("https://$host/common")))
    }

    // --- ensureCloudDiscoveryForAuthority caching behavior ---

    @Test
    fun testEnsureCloudDiscoveryForAuthority_sovCloudAlreadyCached_noOp() {
        val sovUrl = URL("https://login.sovcloud-identity.fr/common")
        AzureActiveDirectory.ensureCloudDiscoveryForAuthority(sovUrl)

        assertTrue(AzureActiveDirectory.hasCloudHost(sovUrl))
        assertTrue(AzureActiveDirectory.isValidCloudHost(sovUrl))
    }

    @Test
    fun testEnsureCloudDiscoveryForAuthority_nullUrl_doesNotThrow() {
        try {
            AzureActiveDirectory.ensureCloudDiscoveryForAuthority(null as URL?)
        } catch (e: Exception) {
            assertFalse("Should not be NPE", e is NullPointerException)
        }
    }

    @Test
    fun testEnsureCloudDiscoveryForAuthority_unknownHostAfterGlobalCached_noOp() {
        val defaultHost = "login.microsoftonline.com"
        AzureActiveDirectory.putCloud(defaultHost, AzureActiveDirectoryCloud(defaultHost, defaultHost))

        val unknownUrl = URL("https://login.example.com/common")
        AzureActiveDirectory.ensureCloudDiscoveryForAuthority(unknownUrl)

        assertFalse(AzureActiveDirectory.hasCloudHost(unknownUrl))
        assertTrue(AzureActiveDirectory.hasCloudHost(URL("https://$defaultHost/common")))
    }

    // --- getAzureActiveDirectoryCloudFromHostName tests ---

    @Test
    fun testGetAzureActiveDirectoryCloudFromHostName_sovCloud() {
        val cloud = AzureActiveDirectory.getAzureActiveDirectoryCloudFromHostName("login.sovcloud-identity.fr")
        assertNotNull(cloud)
        assertEquals("login.sovcloud-identity.fr", cloud.preferredNetworkHostName)
    }

    @Test
    fun testGetAzureActiveDirectoryCloudFromHostName_caseInsensitive() {
        val cloud = AzureActiveDirectory.getAzureActiveDirectoryCloudFromHostName("LOGIN.SOVCLOUD-IDENTITY.DE")
        assertNotNull(cloud)
        assertEquals("login.sovcloud-identity.de", cloud.preferredNetworkHostName)
    }

    @Test
    fun testGetAzureActiveDirectoryCloudFromHostName_unknown() {
        val cloud = AzureActiveDirectory.getAzureActiveDirectoryCloudFromHostName("login.unknown.com")
        assertNull(cloud)
    }

    // --- ensureCloudDiscoveryForAuthority(Authority) overload tests ---

    @Test
    fun testEnsureCloudDiscoveryForAuthority_nullAuthority_doesNotThrow() {
        try {
            AzureActiveDirectory.ensureCloudDiscoveryForAuthority(null as Authority?)
        } catch (e: Exception) {
            assertFalse("Should not be NPE", e is NullPointerException)
        }
    }

    @Test
    fun testEnsureCloudDiscoveryForAuthority_sovCloudAuthority_isNoOp() {
        val authority = Authority.getAuthorityFromAuthorityUrl(
            "https://${AzureActiveDirectoryCloud.BLEU_CLOUD_HOST}/common"
        )
        AzureActiveDirectory.ensureCloudDiscoveryForAuthority(authority)
        assertTrue(
            AzureActiveDirectory.hasCloudHost(
                URL("https://${AzureActiveDirectoryCloud.BLEU_CLOUD_HOST}/common")
            )
        )
    }

    // --- Environment switching with sovereign clouds ---

    @Test
    fun testSovCloudsRemainAfterEnvironmentSwitch() {
        AzureActiveDirectory.setEnvironment(Environment.PreProduction)
        assertTrue(AzureActiveDirectory.hasCloudHost(URL("https://login.sovcloud-identity.fr/common")))
        assertTrue(AzureActiveDirectory.hasCloudHost(URL("https://login.sovcloud-identity.de/common")))
        assertTrue(AzureActiveDirectory.hasCloudHost(URL("https://login.sovcloud-identity.sg/common")))
    }

    // --- Flight-gated sovereign discovery routing tests ---

    private fun setSovereignCloudInstanceDiscoveryFlightEnabled(enabled: Boolean) {
        val provider = MockFlightsProvider()
        provider.addFlight(
            CommonFlight.ENABLE_SOVEREIGN_CLOUD_INSTANCE_DISCOVERY.key,
            enabled.toString()
        )
        val manager = MockFlightsManager()
        manager.setMockBrokerFlightsProvider(provider)
        CommonFlightsManager.initializeCommonFlightsManager(manager)
    }

    @Test
    fun testFlightOn_sovCloudPreSeeded_andIsKnownHost() {
        setSovereignCloudInstanceDiscoveryFlightEnabled(true)
        // Sovereign clouds are always pre-seeded (not gated by flight)
        assertTrue(AzureActiveDirectory.hasCloudHost(URL("https://login.sovcloud-identity.fr/common")))
        // Known host check is not flight-gated
        assertTrue(AzureActiveDirectory.isKnownCloudDiscoveryHost("login.sovcloud-identity.fr"))
    }

    @Test
    fun testFlightOff_sovCloudsStillPreSeeded() {
        setSovereignCloudInstanceDiscoveryFlightEnabled(false)
        // Pre-seeding is NOT gated by the flight — sovereign hosts should still be in cache
        assertTrue(AzureActiveDirectory.hasCloudHost(URL("https://login.sovcloud-identity.fr/common")))
        assertTrue(AzureActiveDirectory.hasCloudHost(URL("https://login.sovcloud-identity.de/common")))
        assertTrue(AzureActiveDirectory.hasCloudHost(URL("https://login.sovcloud-identity.sg/common")))
    }

    @Test
    fun testFlightOn_ensureDiscoveryForSovAuthority_sovCloudCached_isNoOp() {
        setSovereignCloudInstanceDiscoveryFlightEnabled(true)
        val bleuUrl = URL("https://login.sovcloud-identity.fr/common")
        // Sovereign cloud is pre-seeded, so this should be a no-op (no network call)
        AzureActiveDirectory.ensureCloudDiscoveryForAuthority(bleuUrl)
        assertTrue(AzureActiveDirectory.hasCloudHost(bleuUrl))
        assertTrue(AzureActiveDirectory.isValidCloudHost(bleuUrl))
    }

    // --- Concurrency tests ---
    // Verify the fix for the ABBA deadlock between AzureActiveDirectory.class and
    // AzureActiveDirectoryAuthority.class monitors, and the lock convoy where
    // synchronized read-only methods blocked behind network I/O.

    /**
     * Regression test for the ABBA deadlock between AzureActiveDirectory.class and
     * AzureActiveDirectoryAuthority.class monitors.
     *
     * Thread 1: isKnownAuthority → sLock → getAuthorityURL → AADAuthority.class → AAD.class
     * Thread 2: ensureCloudDiscoveryForAuthority → AAD.class → getAuthorityURL → AADAuthority.class
     *
     * If the deadlock still existed, this test would hang and be killed by the timeout.
     */
    @Test(timeout = 10_000)
    fun testNoDeadlock_isKnownAuthority_vs_ensureCloudDiscovery() {
        val authority = Authority.getAuthorityFromAuthorityUrl(
            "https://login.microsoftonline.com/common"
        ) as AzureActiveDirectoryAuthority

        val barrier = CyclicBarrier(2)
        val completedLatch = CountDownLatch(2)
        val thread1Error = AtomicReference<Throwable?>(null)
        val thread2Error = AtomicReference<Throwable?>(null)

        val t1 = Thread {
            try {
                repeat(500) {
                    if (it == 0) barrier.await(5, TimeUnit.SECONDS)
                    Authority.isKnownAuthority(authority)
                }
            } catch (e: Throwable) {
                thread1Error.set(e)
            } finally {
                completedLatch.countDown()
            }
        }

        val t2 = Thread {
            try {
                repeat(500) {
                    if (it == 0) barrier.await(5, TimeUnit.SECONDS)
                    try {
                        AzureActiveDirectory.ensureCloudDiscoveryForAuthority(authority)
                    } catch (_: Exception) { }
                }
            } catch (e: Throwable) {
                thread2Error.set(e)
            } finally {
                completedLatch.countDown()
            }
        }

        t1.start()
        t2.start()

        assertTrue(
            "Both threads should complete within timeout (deadlock detected if this fails)",
            completedLatch.await(9, TimeUnit.SECONDS)
        )
        assertNull("Thread 1 should not throw", thread1Error.get())
        assertNull("Thread 2 should not throw", thread2Error.get())
    }

    /**
     * Verifies that concurrent reads to hasCloudHost / getAzureActiveDirectoryCloud
     * do not block each other now that synchronized has been removed from read-only methods.
     */
    @Test(timeout = 10_000)
    fun testConcurrentReads_doNotBlock() {
        val threadCount = 8
        val barrier = CyclicBarrier(threadCount)
        val completedLatch = CountDownLatch(threadCount)
        val anyError = AtomicBoolean(false)
        val url = URL("https://login.microsoftonline.com/common")

        val threads = (1..threadCount).map { index ->
            Thread {
                try {
                    barrier.await(5, TimeUnit.SECONDS)
                    repeat(1_000) {
                        when (index % 4) {
                            0 -> AzureActiveDirectory.hasCloudHost(url)
                            1 -> AzureActiveDirectory.getAzureActiveDirectoryCloud(url)
                            2 -> AzureActiveDirectory.getAzureActiveDirectoryCloudFromHostName("login.microsoftonline.com")
                            3 -> AzureActiveDirectory.isValidCloudHost(url)
                        }
                    }
                } catch (_: Throwable) {
                    anyError.set(true)
                } finally {
                    completedLatch.countDown()
                }
            }
        }

        threads.forEach { it.start() }
        assertTrue(
            "All reader threads should complete quickly",
            completedLatch.await(9, TimeUnit.SECONDS)
        )
        assertFalse("No reader should throw an exception", anyError.get())
    }

    /**
     * Verifies that concurrent getAuthorityURL() calls on AzureActiveDirectoryAuthority
     * do not deadlock or throw.
     */
    @Test(timeout = 10_000)
    fun testConcurrentGetAuthorityURL_doesNotDeadlock() {
        val authority = Authority.getAuthorityFromAuthorityUrl(
            "https://login.microsoftonline.com/common"
        ) as AzureActiveDirectoryAuthority

        val threadCount = 4
        val barrier = CyclicBarrier(threadCount)
        val completedLatch = CountDownLatch(threadCount)
        val anyError = AtomicBoolean(false)

        val threads = (1..threadCount).map {
            Thread {
                try {
                    barrier.await(5, TimeUnit.SECONDS)
                    repeat(1_000) {
                        authority.authorityURL
                    }
                } catch (_: Throwable) {
                    anyError.set(true)
                } finally {
                    completedLatch.countDown()
                }
            }
        }

        threads.forEach { it.start() }
        assertTrue(
            "All threads calling getAuthorityURL should complete",
            completedLatch.await(9, TimeUnit.SECONDS)
        )
        assertFalse("No thread should throw", anyError.get())
    }

    /**
     * Verifies that a writer (putCloud) and concurrent readers (hasCloudHost)
     * can run simultaneously without errors, validating ConcurrentHashMap safety.
     */
    @Test(timeout = 10_000)
    fun testConcurrentReadsAndWrites_noErrors() {
        val completedLatch = CountDownLatch(2)
        val anyError = AtomicBoolean(false)
        val barrier = CyclicBarrier(2)

        val writer = Thread {
            try {
                barrier.await(5, TimeUnit.SECONDS)
                repeat(500) { i ->
                    val host = "login.test$i.com"
                    AzureActiveDirectory.putCloud(host, AzureActiveDirectoryCloud(host, host))
                }
            } catch (_: Throwable) {
                anyError.set(true)
            } finally {
                completedLatch.countDown()
            }
        }

        val reader = Thread {
            try {
                barrier.await(5, TimeUnit.SECONDS)
                repeat(500) { i ->
                    val host = "login.test$i.com"
                    AzureActiveDirectory.hasCloudHost(URL("https://$host/common"))
                    AzureActiveDirectory.getAzureActiveDirectoryCloudFromHostName(host)
                }
            } catch (_: Throwable) {
                anyError.set(true)
            } finally {
                completedLatch.countDown()
            }
        }

        writer.start()
        reader.start()
        assertTrue(
            "Reader and writer should both complete",
            completedLatch.await(9, TimeUnit.SECONDS)
        )
        assertFalse("No exception should occur during concurrent read/write", anyError.get())
    }

    /**
     * Stress test: simulates the ANR scenario where multiple apps call
     * isKnownAuthority and ensureCloudDiscovery simultaneously on different authorities.
     */
    @Test(timeout = 15_000)
    fun testHighConcurrency_multipleAuthorities_noDeadlock() {
        val hosts = listOf(
            "login.microsoftonline.com",
            "login.sovcloud-identity.fr",
            "login.sovcloud-identity.de",
            "login.sovcloud-identity.sg"
        )
        val authorities = hosts.map { Authority.getAuthorityFromAuthorityUrl("https://$it/common") }
        val threadCount = authorities.size * 2
        val barrier = CyclicBarrier(threadCount)
        val completedLatch = CountDownLatch(threadCount)
        val anyError = AtomicBoolean(false)

        val threads = authorities.flatMap { authority ->
            listOf(
                Thread {
                    try {
                        barrier.await(5, TimeUnit.SECONDS)
                        repeat(200) { Authority.isKnownAuthority(authority) }
                    } catch (_: Throwable) { anyError.set(true) }
                    finally { completedLatch.countDown() }
                },
                Thread {
                    try {
                        barrier.await(5, TimeUnit.SECONDS)
                        repeat(200) {
                            try { AzureActiveDirectory.ensureCloudDiscoveryForAuthority(authority) }
                            catch (_: Exception) { }
                        }
                    } catch (_: Throwable) { anyError.set(true) }
                    finally { completedLatch.countDown() }
                }
            )
        }

        threads.forEach { it.start() }
        assertTrue(
            "All $threadCount threads should complete (deadlock detected if this fails)",
            completedLatch.await(14, TimeUnit.SECONDS)
        )
        assertFalse("No thread should throw an unexpected exception", anyError.get())
    }
}
