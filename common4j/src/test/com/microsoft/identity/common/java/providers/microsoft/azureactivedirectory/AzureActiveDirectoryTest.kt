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
import com.microsoft.identity.common.java.authorities.Environment
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import com.microsoft.identity.common.java.flighting.MockFlightsManager
import com.microsoft.identity.common.java.flighting.MockFlightsProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.net.URL

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
    fun testIsKnownCloudDiscoveryHost_sovsg() {
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
}
