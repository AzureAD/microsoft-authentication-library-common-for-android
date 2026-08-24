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

import com.microsoft.identity.common.BuildConfig
import com.microsoft.identity.common.adal.internal.AuthenticationConstants
import com.microsoft.identity.common.internal.broker.BrokerData
import com.microsoft.identity.common.java.broker.App
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun browserSsoAuthorizedApps_whenDebugBrokersTrusted_containsChromeVariantsAndMsalTestApp() {
        assertEquals(5, AppRegistry.BROWSER_SSO_AUTHORIZED_APPS.size)
        assertTrue(AppRegistry.BROWSER_SSO_AUTHORIZED_APPS.contains(AppRegistry.CHROME))
        assertTrue(AppRegistry.BROWSER_SSO_AUTHORIZED_APPS.contains(AppRegistry.CHROME_BETA))
        assertTrue(AppRegistry.BROWSER_SSO_AUTHORIZED_APPS.contains(AppRegistry.CHROME_DEV))
        assertTrue(AppRegistry.BROWSER_SSO_AUTHORIZED_APPS.contains(AppRegistry.CHROME_CANARY))
        assertTrue(AppRegistry.BROWSER_SSO_AUTHORIZED_APPS.contains(AppRegistry.MSAL_TEST_APP))
        assertFalse(AppRegistry.BROWSER_SSO_AUTHORIZED_APPS.contains(AppRegistry.EDGE))
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
