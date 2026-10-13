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
package com.microsoft.identity.common.internal.apps

import android.os.Build
import android.util.Base64
import com.microsoft.identity.common.BuildConfig
import com.microsoft.identity.common.adal.internal.AuthenticationConstants
import com.microsoft.identity.common.internal.broker.BrokerData
import com.microsoft.identity.common.java.broker.App
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import com.microsoft.identity.common.java.flighting.IFlightsManager
import com.microsoft.identity.common.java.flighting.IFlightsProvider
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.lang.reflect.Modifier

/**
 * Unit tests for [AppRegistry].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class AppRegistryTest {
    private var islandEnabled = false

    @Before
    fun setUpFlights() {
        val provider = mock<IFlightsProvider>()
        whenever(provider.isFlightEnabled(CommonFlight.ENABLE_ISLAND_BROWSER_SSO))
            .thenAnswer { islandEnabled }
        val manager = mock<IFlightsManager>()
        whenever(manager.getFlightsProvider(any<Long>())).thenReturn(provider)
        CommonFlightsManager.initializeCommonFlightsManager(manager)
    }

    @After
    fun resetFlights() {
        CommonFlightsManager.resetFlightsManager()
    }

    @Test
    fun edgeApps_haveSharedSignatureAndDistinctPackages() {
        assertApp(
            app = AppRegistry.EDGE,
            nickName = "Microsoft Edge",
            packageName = "com.microsoft.emmx",
            signingCertificateThumbprint = AuthenticationConstants.Broker.SHARED_EDGE_SIGNATURE
        )
        assertApp(
            app = AppRegistry.EDGE_BETA,
            nickName = "Microsoft Edge Beta",
            packageName = "com.microsoft.emmx.beta",
            signingCertificateThumbprint = AuthenticationConstants.Broker.SHARED_EDGE_SIGNATURE
        )
        assertApp(
            app = AppRegistry.EDGE_CANARY,
            nickName = "Microsoft Edge Canary",
            packageName = "com.microsoft.emmx.canary",
            signingCertificateThumbprint = AuthenticationConstants.Broker.SHARED_EDGE_SIGNATURE
        )
    }

    @Test
    fun chromeApps_haveExpectedPackageNamesAndSignatures() {
        assertApp(
            app = AppRegistry.CHROME,
            nickName = "Google Chrome",
            packageName = "com.android.chrome",
            signingCertificateThumbprint = "7fmduHKTdHHrlMvldlEqAIlSfii1tl35bxj1OXN5Ve8c4lU6URVu4xtSHc3BVZxS6WWJnxMDhIfQN0N0K2NDJg=="
        )
        assertApp(
            app = AppRegistry.CHROME_BETA,
            nickName = "Google Chrome Beta",
            packageName = "com.chrome.beta",
            signingCertificateThumbprint = "ZZTQrvpldI8bmSdc8TKK3KISErF8zy+nMp269KAuPxyvVz7BqgczKtS90pKGEPV8eVOIRqFDaRe4aDie4lCTpw=="
        )
        assertApp(
            app = AppRegistry.CHROME_DEV,
            nickName = "Google Chrome Dev",
            packageName = "com.chrome.dev",
            signingCertificateThumbprint = "JlOLOTFn6OFBFWuWQJYJ8h/aozEN7/zLFTfioXiXTrU6Yaft4cdEbdpkoJIvmB7Gv2HpHu6QOz+XIaXybtzL7A=="
        )
        assertApp(
            app = AppRegistry.CHROME_CANARY,
            nickName = "Google Chrome Canary",
            packageName = "com.chrome.canary",
            signingCertificateThumbprint = "QfTWFoLyXuOCZ7bMYlMN+la3J3rau5x8p+w2v7vf1gOPiTyIMgdbNDzLaLWhgiC2ioj/hFqk8oZyqdJbFG6G4g=="
        )
    }

    @Test
    fun intuneAndTeamsApps_haveProdAndDebugIdentities() {
        assertApp(
            app = AppRegistry.INTUNE_AOSP_AGENT_PROD,
            nickName = "Intune AOSP Agent Prod",
            packageName = "com.microsoft.intune.aospagent",
            signingCertificateThumbprint = AuthenticationConstants.Broker.INTUNE_AOSP_AGENT_RELEASE_SIGNATURE
        )
        assertApp(
            app = AppRegistry.INTUNE_AOSP_AGENT_DEBUG,
            nickName = "Intune AOSP Agent Debug",
            packageName = "com.microsoft.intune.aospagent",
            signingCertificateThumbprint = AuthenticationConstants.Broker.INTUNE_AOSP_AGENT_DEBUG_SIGNATURE
        )
        assertApp(
            app = AppRegistry.INTUNE_CE_PROD,
            nickName = "Intune Company Portal (prod)",
            packageName = AuthenticationConstants.Broker.INTUNE_APP_PACKAGE_NAME,
            signingCertificateThumbprint = AuthenticationConstants.Broker.INTUNE_APP_SHA512_RELEASE_SIGNATURE
        )
        assertApp(
            app = AppRegistry.INTUNE_CE_DEBUG,
            nickName = "Intune Company Portal (debug)",
            packageName = AuthenticationConstants.Broker.INTUNE_APP_PACKAGE_NAME,
            signingCertificateThumbprint = AuthenticationConstants.Broker.INTUNE_APP_SHA512_DEBUG_SIGNATURE
        )
        assertApp(
            app = AppRegistry.TEAMS_IPPHONE_PROD,
            nickName = "Teams IP Phone - Teams Devices (prod)",
            packageName = AuthenticationConstants.Broker.IPPHONE_APP_PACKAGE_NAME,
            signingCertificateThumbprint = AuthenticationConstants.Broker.IPPHONE_APP_SHA512_RELEASE_SIGNATURE
        )
        assertApp(
            app = AppRegistry.TEAMS_IPPHONE_DEBUG,
            nickName = "Teams IP Phone - Teams Devices (debug)",
            packageName = AuthenticationConstants.Broker.IPPHONE_APP_PACKAGE_NAME,
            signingCertificateThumbprint = AuthenticationConstants.Broker.IPPHONE_APP_SHA512_DEBUG_SIGNATURE
        )
    }

    @Test
    fun testAppsAndMdeApps_haveExpectedIdentities() {
        assertApp(
            app = AppRegistry.ONE_AUTH_TEST_APP,
            nickName = "OneAuth Test App",
            packageName = "com.msft.oneauth.testapp",
            signingCertificateThumbprint = AuthenticationConstants.Broker.ONE_AUTH_TEST_APP_SIGNATURE
        )
        assertApp(
            app = AppRegistry.MSAL_TEST_APP,
            nickName = "MSAL Test App",
            packageName = "com.msft.identity.client.sample.local",
            signingCertificateThumbprint = AuthenticationConstants.Broker.BROKER_HOST_APP_SIGNATURE_SHA512
        )
        assertApp(
            app = AppRegistry.MDE_APP_PROD,
            nickName = "Microsoft Defender for Endpoint",
            packageName = "com.microsoft.scmx",
            signingCertificateThumbprint = "iPULpH0pq8ms1Qy7cOzGsVRQN7/zW4IbW+UKcajvtrTrzM5o5VcaghNEA1Ho4Wq7ay0efqqJcalxa8eHxVnHKA=="
        )
        assertApp(
            app = AppRegistry.MDE_APP_DEBUG,
            nickName = "Microsoft Defender for Endpoint",
            packageName = "com.microsoft.scmx",
            signingCertificateThumbprint = "k0ZSm/+bEPZAq6mXujRXqP3B6+Zb2yXCiqwuvtCooLfKS91zvHCf+D9FFUYIkJyIKmn1onyWbwRXHEWfS5SaHQ=="
        )
    }

    @Test
    fun bootMenuApps_haveProdAndDebugIdentities() {
        assertApp(
            app = AppRegistry.BOOT_MENU_PROD,
            nickName = "Microsoft Boot Menu (prod)",
            packageName = "com.microsoft.apps.bootmenu",
            signingCertificateThumbprint = "5uz4ymmE6MKGJGZl9lZSwFVXEUe+v2JK5S7NalKLcw4EK645krf3c/GXt99A8sJLWedNHfQA+l+BZMJIj9Uy7A=="
        )
        assertApp(
            app = AppRegistry.BOOT_MENU_DEBUG,
            nickName = "Microsoft Boot Menu (debug)",
            packageName = "com.microsoft.apps.bootmenu",
            signingCertificateThumbprint = "tc2+6Lf/ODCxvB9e5RRIe2svcpSQ/avZwJgaNMwRUtm/HTFVFDA9q8ZDGOQgJmACJbzpwBiLxgTpSW7sRfkvPA=="
        )
    }

    @Test
    fun mdepZteServiceApps_haveProdAndDebugIdentities() {
        assertApp(
            app = AppRegistry.MDEP_ZTE_SERVICE_PROD,
            nickName = "Microsoft MDEP ZTE Service (prod)",
            packageName = "com.microsoft.mdep.zte.service",
            signingCertificateThumbprint = "5uz4ymmE6MKGJGZl9lZSwFVXEUe+v2JK5S7NalKLcw4EK645krf3c/GXt99A8sJLWedNHfQA+l+BZMJIj9Uy7A=="
        )
        assertApp(
            app = AppRegistry.MDEP_ZTE_SERVICE_DEBUG,
            nickName = "Microsoft MDEP ZTE Service (debug)",
            packageName = "com.microsoft.mdep.zte.service",
            signingCertificateThumbprint = "tc2+6Lf/ODCxvB9e5RRIe2svcpSQ/avZwJgaNMwRUtm/HTFVFDA9q8ZDGOQgJmACJbzpwBiLxgTpSW7sRfkvPA=="
        )
    }

    @Test
    fun ssoTokenAuthorizedApps_whenDebugBrokersTrusted_containsEdgeAndDebugAppsOnly() {
        assertEquals(5, AppRegistry.SSO_TOKEN_AUTHORIZED_APPS.size)
        assertTrue(AppRegistry.SSO_TOKEN_AUTHORIZED_APPS.contains(AppRegistry.EDGE))
        assertTrue(AppRegistry.SSO_TOKEN_AUTHORIZED_APPS.contains(AppRegistry.EDGE_BETA))
        assertTrue(AppRegistry.SSO_TOKEN_AUTHORIZED_APPS.contains(AppRegistry.EDGE_CANARY))
        assertTrue(AppRegistry.SSO_TOKEN_AUTHORIZED_APPS.contains(BrokerData.debugBrokerHost))
        assertTrue(AppRegistry.SSO_TOKEN_AUTHORIZED_APPS.contains(AppRegistry.ONE_AUTH_TEST_APP))
        assertFalse(AppRegistry.SSO_TOKEN_AUTHORIZED_APPS.contains(AppRegistry.CHROME))
    }

    @Test
    fun getDeviceTokenAuthorizedApps_whenDebugBrokersTrusted_containsDeviceTokenApps() {
        assertEquals(10, AppRegistry.GET_DEVICE_TOKEN_AUTHORIZED_APPS.size)
        assertTrue(AppRegistry.GET_DEVICE_TOKEN_AUTHORIZED_APPS.contains(AppRegistry.BOOT_MENU_PROD))
        assertTrue(AppRegistry.GET_DEVICE_TOKEN_AUTHORIZED_APPS.contains(AppRegistry.BOOT_MENU_DEBUG))
        assertTrue(AppRegistry.GET_DEVICE_TOKEN_AUTHORIZED_APPS.contains(AppRegistry.MDEP_ZTE_SERVICE_PROD))
        assertTrue(AppRegistry.GET_DEVICE_TOKEN_AUTHORIZED_APPS.contains(AppRegistry.MDEP_ZTE_SERVICE_DEBUG))
        assertTrue(AppRegistry.GET_DEVICE_TOKEN_AUTHORIZED_APPS.contains(AppRegistry.INTUNE_AOSP_AGENT_PROD))
        assertTrue(AppRegistry.GET_DEVICE_TOKEN_AUTHORIZED_APPS.contains(AppRegistry.MDE_APP_PROD))
        assertTrue(AppRegistry.GET_DEVICE_TOKEN_AUTHORIZED_APPS.contains(AppRegistry.INTUNE_AOSP_AGENT_DEBUG))
        assertTrue(AppRegistry.GET_DEVICE_TOKEN_AUTHORIZED_APPS.contains(BrokerData.debugBrokerHost))
        assertTrue(AppRegistry.GET_DEVICE_TOKEN_AUTHORIZED_APPS.contains(BrokerData.debugMicrosoftAuthenticator))
        assertTrue(AppRegistry.GET_DEVICE_TOKEN_AUTHORIZED_APPS.contains(AppRegistry.MDE_APP_DEBUG))
        assertFalse(AppRegistry.GET_DEVICE_TOKEN_AUTHORIZED_APPS.contains(AppRegistry.INTUNE_CE_PROD))
    }

    @Test
    fun deviceRegistrationAuthorizedApps_whenDebugBrokersTrusted_containsProdAndDebugRegistrationApps() {
        assertEquals(17, AppRegistry.DEVICE_REGISTRATION_AUTHORIZED_APPS.size)
        assertTrue(AppRegistry.DEVICE_REGISTRATION_AUTHORIZED_APPS.contains(AppRegistry.BOOT_MENU_PROD))
        assertTrue(AppRegistry.DEVICE_REGISTRATION_AUTHORIZED_APPS.contains(AppRegistry.BOOT_MENU_DEBUG))
        assertTrue(AppRegistry.DEVICE_REGISTRATION_AUTHORIZED_APPS.contains(AppRegistry.MDEP_ZTE_SERVICE_PROD))
        assertTrue(AppRegistry.DEVICE_REGISTRATION_AUTHORIZED_APPS.contains(AppRegistry.MDEP_ZTE_SERVICE_DEBUG))
        assertTrue(AppRegistry.DEVICE_REGISTRATION_AUTHORIZED_APPS.contains(BrokerData.prodMicrosoftAuthenticator))
        assertTrue(AppRegistry.DEVICE_REGISTRATION_AUTHORIZED_APPS.contains(BrokerData.prodCompanyPortal))
        assertTrue(AppRegistry.DEVICE_REGISTRATION_AUTHORIZED_APPS.contains(AppRegistry.INTUNE_CE_PROD))
        assertTrue(AppRegistry.DEVICE_REGISTRATION_AUTHORIZED_APPS.contains(AppRegistry.INTUNE_AOSP_AGENT_PROD))
        assertTrue(AppRegistry.DEVICE_REGISTRATION_AUTHORIZED_APPS.contains(AppRegistry.TEAMS_IPPHONE_PROD))
        assertTrue(AppRegistry.DEVICE_REGISTRATION_AUTHORIZED_APPS.contains(AppRegistry.MDE_APP_PROD))
        assertTrue(AppRegistry.DEVICE_REGISTRATION_AUTHORIZED_APPS.contains(AppRegistry.INTUNE_AOSP_AGENT_DEBUG))
        assertTrue(AppRegistry.DEVICE_REGISTRATION_AUTHORIZED_APPS.contains(BrokerData.debugBrokerHost))
        assertTrue(AppRegistry.DEVICE_REGISTRATION_AUTHORIZED_APPS.contains(BrokerData.debugMicrosoftAuthenticator))
        assertTrue(AppRegistry.DEVICE_REGISTRATION_AUTHORIZED_APPS.contains(BrokerData.debugCompanyPortal))
        assertTrue(AppRegistry.DEVICE_REGISTRATION_AUTHORIZED_APPS.contains(AppRegistry.INTUNE_CE_DEBUG))
        assertTrue(AppRegistry.DEVICE_REGISTRATION_AUTHORIZED_APPS.contains(AppRegistry.TEAMS_IPPHONE_DEBUG))
        assertTrue(AppRegistry.DEVICE_REGISTRATION_AUTHORIZED_APPS.contains(AppRegistry.MDE_APP_DEBUG))
        assertFalse(AppRegistry.DEVICE_REGISTRATION_AUTHORIZED_APPS.contains(AppRegistry.EDGE))
    }

    @Test
    fun browserSsoAuthorizedApps_whenDebugBrokersTrusted_containsChromeAndMsalTestAppOnly() {
        val authorizedApps = AppRegistry.getBrowserSsoAuthorizedApps()
        assertEquals(5, authorizedApps.size)
        assertTrue(authorizedApps.containsAll(AppRegistry.CHROME_BROWSER_SSO_APPS))
        assertTrue(authorizedApps.contains(AppRegistry.MSAL_TEST_APP))
        assertFalse(authorizedApps.contains(AppRegistry.EDGE))
        assertTrue(authorizedApps.intersect(AppRegistry.ISLAND_BROWSER_SSO_APPS).isEmpty())
    }

    @Test
    fun islandPlayStoreApps_haveExactlyTheProvidedPackageAndCertificatePairs() {
        val expectedIdentities = mapOf(
            "io.island.Island" to "9A0EWFnAmSLLMpJP9YP/zZL2PUxLL+eTfnTgI4wsCylaHdd5o4lwjZLjNkPZ7cu5tPYZHLCrEPJVJJNcgoRrAw==",
            "io.island.IslandCanary" to "TwrYxhsZaPoxnXrFnGt5gzMg9WsV2V0LTHFd0KvqACW9cKfCQc9yPkV+e+f7lAfalMs3zdMfIzUpJZIT7tbOfQ==",
            "io.island.IslandBeta" to "KcVfTD7f9blSNklGx92FzIx1qwo1l7Vtp+sOUq+P2RNb7sSFZNLIBU6JdftdcNmmmv7rpMVcHTs7H1MJMfRkjA==",
            "io.island.island.intune" to "q4Ycw2UxQJfVXEcREJwIeszP88D8QKdQw82y3m9Zmg8Sg+2YYP+wq9TATX9PgGjpgd2YfgcrXgsbuubbzdRqtA==",
            "io.island.island.canary.intune" to "XVdkJe6bsmBS3/a2utbFAfXVTg0IaXJS6hvzjx0Uyg+7q7187oXRAAxROMj8NkTiRlI43SCippiDTtRam5asww==",
            "io.island.island.beta.intune" to "djswQiqC4rjGg2rNmkm+Y090echgP5JGoVesHJ2NnC1q7lQWJ/2a70HSOzMdXbYGtl0aZydlkR2sbMqRbeb4Lg=="
        )

        val islandPlayStoreApps = AppRegistry.ISLAND_BROWSER_SSO_APPS.filter {
            it.signingCertificateThumbprint != DIRECT_DISTRIBUTION_SIGNATURE
        }
        assertEquals(6, islandPlayStoreApps.size)
        assertEquals(
            expectedIdentities,
            islandPlayStoreApps.associate {
                it.packageName to it.signingCertificateThumbprint
            }
        )
    }

    @Test
    fun islandDirectDistributionApps_haveExactlyEightPackagesAndTheSharedProductionCertificate() {
        val expectedPackages = setOf(
            "io.island.Island",
            "io.island.IslandCanary",
            "io.island.IslandBeta",
            "io.island.island.intune",
            "io.island.island.canary.intune",
            "io.island.island.beta.intune",
            "io.island.IslandDev",
            "io.island.island.dev.intune"
        )
        val directDistributionApps = AppRegistry.ISLAND_BROWSER_SSO_APPS.filter {
            it.signingCertificateThumbprint == DIRECT_DISTRIBUTION_SIGNATURE
        }
        assertEquals(8, directDistributionApps.size)
        assertEquals(
            expectedPackages,
            directDistributionApps.map { it.packageName }.toSet()
        )
        islandEnabled = true
        assertTrue(AppRegistry.getBrowserSsoAuthorizedApps().containsAll(directDistributionApps))
    }

    @Test
    fun islandBrowserSsoApps_haveSha512SizedCertificateThumbprints() {
        AppRegistry.ISLAND_BROWSER_SSO_APPS.forEach {
            assertEquals(it.packageName, 64, Base64.decode(it.signingCertificateThumbprint, Base64.NO_WRAP).size)
        }
    }

    @Test
    fun islandBrowserSsoApps_areNotAddedToOtherPrivilegedAuthorizationLists() {
        val islandPackages = AppRegistry.ISLAND_BROWSER_SSO_APPS.map { it.packageName }.toSet()
        val existingAuthorizationLists = listOf(
            AppRegistry.SSO_TOKEN_AUTHORIZED_APPS,
            AppRegistry.GET_DEVICE_TOKEN_AUTHORIZED_APPS,
            AppRegistry.DEVICE_REGISTRATION_AUTHORIZED_APPS,
            AppRegistry.FORCE_BROKER_DISCOVERY_ALLOW_LIST
        )

        existingAuthorizationLists.forEach { authorizedApps ->
            assertTrue(authorizedApps.none { it.packageName in islandPackages })
        }
    }

    @Test
    fun browserIdentitySets_haveExactlyTheRegisteredVariants() {
        assertEquals(
            setOf(AppRegistry.CHROME, AppRegistry.CHROME_BETA,
                AppRegistry.CHROME_DEV, AppRegistry.CHROME_CANARY),
            AppRegistry.CHROME_BROWSER_SSO_APPS
        )
        assertEquals(14, AppRegistry.ISLAND_BROWSER_SSO_APPS.size)
        assertEquals(8, AppRegistry.ISLAND_BROWSER_SSO_APPS.map { it.packageName }.toSet().size)
        assertEquals(
            setOf(AppRegistry.ISLAND, AppRegistry.ISLAND_CANARY, AppRegistry.ISLAND_BETA,
                AppRegistry.ISLAND_INTUNE, AppRegistry.ISLAND_CANARY_INTUNE,
                AppRegistry.ISLAND_BETA_INTUNE),
            AppRegistry.ISLAND_BROWSER_SSO_APPS.filter {
                it.signingCertificateThumbprint != DIRECT_DISTRIBUTION_SIGNATURE
            }.toSet()
        )
    }

    @Test
    fun browserSsoEligibility_usesTheCurrentFlightWithoutChangingTheBaseline() {
        val baseline = AppRegistry.getBrowserSsoAuthorizedApps()
        islandEnabled = true
        assertEquals(
            baseline + AppRegistry.ISLAND_BROWSER_SSO_APPS,
            AppRegistry.getBrowserSsoAuthorizedApps()
        )
        assertEquals(19, AppRegistry.getBrowserSsoAuthorizedApps().size)
        islandEnabled = false
        assertEquals(baseline, AppRegistry.getBrowserSsoAuthorizedApps())
        assertEquals(5, baseline.size)
        assertTrue(baseline.intersect(AppRegistry.ISLAND_BROWSER_SSO_APPS).isEmpty())
    }

    @Test
    fun browserSsoEligibility_exposesAJavaStaticEntryPoint() {
        val method = AppRegistry::class.java.getDeclaredMethod(
            "getBrowserSsoAuthorizedApps"
        )
        assertTrue(Modifier.isStatic(method.modifiers))
        assertEquals(AppRegistry.getBrowserSsoAuthorizedApps(), method.invoke(null))
        islandEnabled = true
        assertEquals(AppRegistry.getBrowserSsoAuthorizedApps(), method.invoke(null))
    }

    @Test
    fun browserSsoEligibility_preservesMsalTestAppUnderCurrentDebugTrust() {
        val originalDebugTrust = BrokerData.getShouldTrustDebugBrokers()
        try {
            listOf(false, true, false).forEach { trustDebug ->
                BrokerData.setShouldTrustDebugBrokers(trustDebug)
                listOf(false, true).forEach { enabled ->
                    islandEnabled = enabled
                    val authorizedApps = AppRegistry.getBrowserSsoAuthorizedApps()
                    assertEquals(trustDebug, authorizedApps.contains(AppRegistry.MSAL_TEST_APP))
                    assertTrue(authorizedApps.containsAll(AppRegistry.CHROME_BROWSER_SSO_APPS))
                    assertEquals(
                        enabled,
                        authorizedApps.containsAll(AppRegistry.ISLAND_BROWSER_SSO_APPS)
                    )
                }
            }
        } finally {
            BrokerData.setShouldTrustDebugBrokers(originalDebugTrust)
        }
    }

    @Test
    fun islandEligibility_withoutAHostFlightProvider_usesTheDisabledDefault() {
        CommonFlightsManager.resetFlightsManager()
        assertEquals("EnableIslandBrowserSso", CommonFlight.ENABLE_ISLAND_BROWSER_SSO.key)
        assertEquals(false, CommonFlight.ENABLE_ISLAND_BROWSER_SSO.defaultValue)
        val apps = AppRegistry.getBrowserSsoAuthorizedApps()
        assertTrue(apps.containsAll(AppRegistry.CHROME_BROWSER_SSO_APPS))
        assertTrue(apps.intersect(AppRegistry.ISLAND_BROWSER_SSO_APPS).isEmpty())
    }

    @Test
    fun forceBrokerDiscoveryAllowList_whenDebugBrokersTrusted_containsCompanyPortalAndMockApps() {
        assertEquals(6, AppRegistry.FORCE_BROKER_DISCOVERY_ALLOW_LIST.size)
        assertTrue(AppRegistry.FORCE_BROKER_DISCOVERY_ALLOW_LIST.contains(AppRegistry.INTUNE_CE_PROD))
        assertTrue(AppRegistry.FORCE_BROKER_DISCOVERY_ALLOW_LIST.contains(AppRegistry.INTUNE_CE_DEBUG))
        assertTrue(AppRegistry.FORCE_BROKER_DISCOVERY_ALLOW_LIST.contains(BrokerData.debugMockLtw))
        assertTrue(AppRegistry.FORCE_BROKER_DISCOVERY_ALLOW_LIST.contains(BrokerData.debugMockCp))
        assertTrue(AppRegistry.FORCE_BROKER_DISCOVERY_ALLOW_LIST.contains(BrokerData.debugMockAuthApp))
        assertTrue(AppRegistry.FORCE_BROKER_DISCOVERY_ALLOW_LIST.contains(BrokerData.debugBrokerHost))
        assertFalse(AppRegistry.FORCE_BROKER_DISCOVERY_ALLOW_LIST.contains(AppRegistry.MDE_APP_PROD))
    }

    private fun assertApp(
        app: App,
        nickName: String,
        packageName: String,
        signingCertificateThumbprint: String
    ) {
        assertEquals(nickName, app.nickName)
        assertEquals(packageName, app.packageName)
        assertEquals(signingCertificateThumbprint, app.signingCertificateThumbprint)
    }

    companion object {
        private const val DIRECT_DISTRIBUTION_SIGNATURE =
            "+P6Af2Jk8nb0tvnmyhZ6d6mrsJ5znPI597Vq7t5EvgcgsM3LYBmwnBnrI5z/RGBZTzwHVG3+nz8Ostf13u/4YQ=="

        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            BrokerData.setShouldTrustDebugBrokers(true)
        }

        @JvmStatic
        @AfterClass
        fun tearDownClass() {
            BrokerData.setShouldTrustDebugBrokers(BuildConfig.DEBUG)
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S])
class AppRegistryProductionTrustTest {
    private var islandEnabled = false

    @Before
    fun setUpFlights() {
        val provider = mock<IFlightsProvider>()
        whenever(provider.isFlightEnabled(CommonFlight.ENABLE_ISLAND_BROWSER_SSO))
            .thenAnswer { islandEnabled }
        val manager = mock<IFlightsManager>()
        whenever(manager.getFlightsProvider(any<Long>())).thenReturn(provider)
        CommonFlightsManager.initializeCommonFlightsManager(manager)
    }

    @After
    fun resetFlights() {
        CommonFlightsManager.resetFlightsManager()
    }

    @Test
    fun browserSsoEligibility_initializedWithoutDebugTrust_islandRequiresOnlyItsFlight() {
        assertFalse(BrokerData.getShouldTrustDebugBrokers())

        val baseline = AppRegistry.getBrowserSsoAuthorizedApps()
        assertEquals(4, baseline.size)
        assertTrue(baseline.intersect(AppRegistry.ISLAND_BROWSER_SSO_APPS).isEmpty())
        islandEnabled = true
        val authorizedApps = AppRegistry.getBrowserSsoAuthorizedApps()
        assertEquals(18, authorizedApps.size)
        assertEquals(14, AppRegistry.ISLAND_BROWSER_SSO_APPS.size)
        assertTrue(authorizedApps.containsAll(AppRegistry.ISLAND_BROWSER_SSO_APPS))
        assertTrue(authorizedApps.containsAll(AppRegistry.CHROME_BROWSER_SSO_APPS))
        assertFalse(authorizedApps.contains(AppRegistry.MSAL_TEST_APP))
        islandEnabled = false
        assertEquals(baseline, AppRegistry.getBrowserSsoAuthorizedApps())
    }

    @Test
    fun deviceAllowlists_whenDebugBrokersNotTrusted_containOnlyProductionMdepApps() {
        for (authorizedApps in listOf(
            AppRegistry.GET_DEVICE_TOKEN_AUTHORIZED_APPS,
            AppRegistry.DEVICE_REGISTRATION_AUTHORIZED_APPS
        )) {
            assertTrue(authorizedApps.contains(AppRegistry.BOOT_MENU_PROD))
            assertTrue(authorizedApps.contains(AppRegistry.MDEP_ZTE_SERVICE_PROD))
            assertFalse(authorizedApps.contains(AppRegistry.BOOT_MENU_DEBUG))
            assertFalse(authorizedApps.contains(AppRegistry.MDEP_ZTE_SERVICE_DEBUG))
        }
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            BrokerData.setShouldTrustDebugBrokers(false)
        }

        @JvmStatic
        @AfterClass
        fun tearDownClass() {
            BrokerData.setShouldTrustDebugBrokers(BuildConfig.DEBUG)
        }
    }
}
