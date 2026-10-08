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

import android.util.Base64
import com.microsoft.identity.common.BuildConfig
import com.microsoft.identity.common.adal.internal.AuthenticationConstants
import com.microsoft.identity.common.internal.broker.BrokerData
import com.microsoft.identity.common.java.broker.App
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [AppRegistry].
 */
@RunWith(RobolectricTestRunner::class)
class AppRegistryTest {

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
        assertEquals(6, AppRegistry.GET_DEVICE_TOKEN_AUTHORIZED_APPS.size)
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
        assertEquals(13, AppRegistry.DEVICE_REGISTRATION_AUTHORIZED_APPS.size)
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
    fun browserSsoAuthorizedApps_whenDebugBrokersTrusted_containsChromeIslandAndMsalTestApp() {
        assertEquals(11, AppRegistry.BROWSER_SSO_AUTHORIZED_APPS.size)
        assertTrue(AppRegistry.BROWSER_SSO_AUTHORIZED_APPS.contains(AppRegistry.CHROME))
        assertTrue(AppRegistry.BROWSER_SSO_AUTHORIZED_APPS.contains(AppRegistry.CHROME_BETA))
        assertTrue(AppRegistry.BROWSER_SSO_AUTHORIZED_APPS.contains(AppRegistry.CHROME_DEV))
        assertTrue(AppRegistry.BROWSER_SSO_AUTHORIZED_APPS.contains(AppRegistry.CHROME_CANARY))
        assertTrue(AppRegistry.BROWSER_SSO_AUTHORIZED_APPS.contains(AppRegistry.MSAL_TEST_APP))
        assertFalse(AppRegistry.BROWSER_SSO_AUTHORIZED_APPS.contains(AppRegistry.EDGE))
    }

    @Test
    fun islandBrowserSsoReleaseApps_haveExactlyTheProvidedPackageAndCertificatePairs() {
        val expectedIdentities = mapOf(
            "io.island.Island" to "9A0EWFnAmSLLMpJP9YP/zZL2PUxLL+eTfnTgI4wsCylaHdd5o4lwjZLjNkPZ7cu5tPYZHLCrEPJVJJNcgoRrAw==",
            "io.island.IslandCanary" to "TwrYxhsZaPoxnXrFnGt5gzMg9WsV2V0LTHFd0KvqACW9cKfCQc9yPkV+e+f7lAfalMs3zdMfIzUpJZIT7tbOfQ==",
            "io.island.IslandBeta" to "KcVfTD7f9blSNklGx92FzIx1qwo1l7Vtp+sOUq+P2RNb7sSFZNLIBU6JdftdcNmmmv7rpMVcHTs7H1MJMfRkjA==",
            "io.island.island.intune" to "q4Ycw2UxQJfVXEcREJwIeszP88D8QKdQw82y3m9Zmg8Sg+2YYP+wq9TATX9PgGjpgd2YfgcrXgsbuubbzdRqtA==",
            "io.island.island.canary.intune" to "XVdkJe6bsmBS3/a2utbFAfXVTg0IaXJS6hvzjx0Uyg+7q7187oXRAAxROMj8NkTiRlI43SCippiDTtRam5asww==",
            "io.island.island.beta.intune" to "djswQiqC4rjGg2rNmkm+Y090echgP5JGoVesHJ2NnC1q7lQWJ/2a70HSOzMdXbYGtl0aZydlkR2sbMqRbeb4Lg=="
        )

        val islandReleaseApps = AppRegistry.BROWSER_SSO_AUTHORIZED_APPS.filter {
            it.packageName in expectedIdentities
        }
        assertEquals(6, islandReleaseApps.size)
        assertEquals(
            expectedIdentities,
            islandReleaseApps.associate {
                it.packageName to it.signingCertificateThumbprint
            }
        )
    }

    @Test
    fun islandBrowserSsoDebugApps_haveExactlyEightPackagesAndTheSharedUserdebugCertificate() {
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
        val expectedSignature =
            "+P6Af2Jk8nb0tvnmyhZ6d6mrsJ5znPI597Vq7t5EvgcgsM3LYBmwnBnrI5z/RGBZTzwHVG3+nz8Ostf13u/4YQ=="

        assertEquals(8, AppRegistry.ISLAND_FAMILY.debugApps.size)
        assertEquals(
            expectedPackages,
            AppRegistry.ISLAND_FAMILY.debugApps.map { it.packageName }.toSet()
        )
        assertTrue(
            AppRegistry.ISLAND_FAMILY.debugApps.all {
                it.signingCertificateThumbprint == expectedSignature
            }
        )
        assertTrue(
            AppRegistry.BROWSER_SSO_AUTHORIZED_APPS.intersect(
                AppRegistry.ISLAND_FAMILY.debugApps
            ).isEmpty()
        )
    }

    @Test
    fun islandBrowserSsoApps_haveSha512SizedCertificateThumbprints() {
        val identities = AppRegistry.BROWSER_SSO_AUTHORIZED_APPS +
                AppRegistry.ISLAND_FAMILY.debugApps

        identities.forEach {
            assertEquals(it.packageName, 64, Base64.decode(it.signingCertificateThumbprint, Base64.NO_WRAP).size)
        }
    }

    @Test
    fun islandBrowserSsoApps_areNotAddedToOtherPrivilegedAuthorizationLists() {
        val islandPackages = AppRegistry.ISLAND_FAMILY.debugApps.map { it.packageName }.toSet()
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
    fun browserFamilies_groupExactlyTheRegisteredVariants() {
        assertEquals(
            setOf(AppRegistry.CHROME, AppRegistry.CHROME_BETA,
                AppRegistry.CHROME_DEV, AppRegistry.CHROME_CANARY),
            AppRegistry.CHROME_FAMILY.releaseApps
        )
        assertTrue(AppRegistry.CHROME_FAMILY.debugApps.isEmpty())
        assertEquals(
            setOf(AppRegistry.ISLAND, AppRegistry.ISLAND_CANARY, AppRegistry.ISLAND_BETA,
                AppRegistry.ISLAND_INTUNE, AppRegistry.ISLAND_CANARY_INTUNE,
                AppRegistry.ISLAND_BETA_INTUNE),
            AppRegistry.ISLAND_FAMILY.releaseApps
        )
        assertEquals(
            setOf(AppRegistry.CHROME_FAMILY, AppRegistry.ISLAND_FAMILY),
            AppRegistry.BROWSER_SSO_APP_FAMILIES
        )
        AppRegistry.BROWSER_SSO_APP_FAMILIES.forEach { family ->
            (family.releaseApps + family.debugApps).forEach {
                assertSame(family, AppRegistry.getBrowserSsoAppFamily(it.packageName))
            }
        }
    }

    @Test
    fun browserFamilyLookup_doesNotTreatPackagePrefixOrTestAppAsFamilyMembership() {
        assertNull(AppRegistry.getBrowserSsoAppFamily(null))
        assertNull(AppRegistry.getBrowserSsoAppFamily("io.island.unlisted"))
        assertNull(AppRegistry.getBrowserSsoAppFamily("com.chrome.unlisted"))
        assertNull(AppRegistry.getBrowserSsoAppFamily(AppRegistry.MSAL_TEST_APP.packageName))
        assertSame(AppRegistry.ISLAND_FAMILY, AppRegistry.getBrowserSsoAppFamily("IO.ISLAND.ISLAND"))
    }

    @Test
    fun browserSsoAllowedApps_selectDebugIdentitiesUsingCurrentTrustPolicy() {
        val originalDebugTrust = BrokerData.getShouldTrustDebugBrokers()
        try {
            BrokerData.setShouldTrustDebugBrokers(false)
            assertEquals(AppRegistry.BROWSER_SSO_AUTHORIZED_APPS, AppRegistry.getBrowserSsoAuthorizedApps())
            BrokerData.setShouldTrustDebugBrokers(true)
            assertEquals(
                AppRegistry.BROWSER_SSO_AUTHORIZED_APPS + AppRegistry.ISLAND_FAMILY.debugApps,
                AppRegistry.getBrowserSsoAuthorizedApps()
            )
            BrokerData.setShouldTrustDebugBrokers(false)
            assertEquals(AppRegistry.BROWSER_SSO_AUTHORIZED_APPS, AppRegistry.getBrowserSsoAuthorizedApps())
        } finally {
            BrokerData.setShouldTrustDebugBrokers(originalDebugTrust)
        }
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
