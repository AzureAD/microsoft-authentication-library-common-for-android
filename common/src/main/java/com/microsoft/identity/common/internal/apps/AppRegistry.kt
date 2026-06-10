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


import com.microsoft.identity.common.adal.internal.AuthenticationConstants
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.INTUNE_AOSP_AGENT_DEBUG_SIGNATURE
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.INTUNE_AOSP_AGENT_RELEASE_SIGNATURE
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.INTUNE_APP_PACKAGE_NAME
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.INTUNE_APP_SHA512_DEBUG_SIGNATURE
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.INTUNE_APP_SHA512_RELEASE_SIGNATURE
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.IPPHONE_APP_PACKAGE_NAME
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.IPPHONE_APP_SHA512_DEBUG_SIGNATURE
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.IPPHONE_APP_SHA512_RELEASE_SIGNATURE
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ONE_AUTH_TEST_APP_SIGNATURE
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.SHARED_EDGE_SIGNATURE
import com.microsoft.identity.common.internal.broker.BrokerData
import com.microsoft.identity.common.java.broker.App

/**
 * Registry of known apps and their signing certificate thumbprints.
 * For broker-related apps, see [com.microsoft.identity.common.internal.broker.BrokerData]
 */
object AppRegistry {

    val EDGE = App(
        nickName = "Microsoft Edge",
        packageName = "com.microsoft.emmx",
        signingCertificateThumbprint = SHARED_EDGE_SIGNATURE
    )

    val EDGE_BETA = App(
        nickName = "Microsoft Edge Beta",
        packageName = "com.microsoft.emmx.beta",
        signingCertificateThumbprint = SHARED_EDGE_SIGNATURE
    )

    val EDGE_CANARY = App(
        nickName = "Microsoft Edge Canary",
        packageName = "com.microsoft.emmx.canary",
        signingCertificateThumbprint = SHARED_EDGE_SIGNATURE
    )

    val ONE_AUTH_TEST_APP = App(
        nickName = "OneAuth Test App",
        packageName = "com.msft.oneauth.testapp",
        signingCertificateThumbprint = ONE_AUTH_TEST_APP_SIGNATURE
    )

    val INTUNE_AOSP_AGENT_PROD = App(
        nickName = "Intune AOSP Agent Prod",
        packageName =  "com.microsoft.intune.aospagent",
        signingCertificateThumbprint = INTUNE_AOSP_AGENT_RELEASE_SIGNATURE
    )

    val INTUNE_AOSP_AGENT_DEBUG = App(
        nickName = "Intune AOSP Agent Debug",
        packageName =  "com.microsoft.intune.aospagent",
        signingCertificateThumbprint = INTUNE_AOSP_AGENT_DEBUG_SIGNATURE
    )

    val CHROME = App(
        nickName = "Google Chrome",
        packageName = "com.android.chrome",
        signingCertificateThumbprint = "7fmduHKTdHHrlMvldlEqAIlSfii1tl35bxj1OXN5Ve8c4lU6URVu4xtSHc3BVZxS6WWJnxMDhIfQN0N0K2NDJg=="
    )

    val CHROME_BETA = App(
        nickName = "Google Chrome Beta",
        packageName = "com.chrome.beta",
        signingCertificateThumbprint = "ZZTQrvpldI8bmSdc8TKK3KISErF8zy+nMp269KAuPxyvVz7BqgczKtS90pKGEPV8eVOIRqFDaRe4aDie4lCTpw=="
    )

    val CHROME_DEV = App(
        nickName = "Google Chrome Dev",
        packageName = "com.chrome.dev",
        signingCertificateThumbprint = "JlOLOTFn6OFBFWuWQJYJ8h/aozEN7/zLFTfioXiXTrU6Yaft4cdEbdpkoJIvmB7GvHpHu6QOz+XIaXybtzL7A=="
    )

    val CHROME_CANARY = App(
        nickName = "Google Chrome Canary",
        packageName = "com.chrome.canary",
        signingCertificateThumbprint = "QfTWFoLyXuOCZ7bMYlMN+la3J3rau5x8p+w2v7vf1gOPiTyIMgdbNDzLaLWhgiC2ioj/hFqk8oZyqdJbFG6G4g=="
    )

    val INTUNE_CE_PROD = App(
        nickName = "Intune Company Portal (prod)",
        packageName = INTUNE_APP_PACKAGE_NAME,
        signingCertificateThumbprint = INTUNE_APP_SHA512_RELEASE_SIGNATURE
    )

    val INTUNE_CE_DEBUG = App(
        nickName = "Intune Company Portal (debug)",
        packageName = INTUNE_APP_PACKAGE_NAME,
        signingCertificateThumbprint = INTUNE_APP_SHA512_DEBUG_SIGNATURE
    )

    val TEAMS_IPPHONE_PROD = App(
        nickName = "Teams IP Phone - Teams Devices (prod)",
        packageName = IPPHONE_APP_PACKAGE_NAME,
        signingCertificateThumbprint = IPPHONE_APP_SHA512_RELEASE_SIGNATURE
    )

    val TEAMS_IPPHONE_DEBUG = App(
        nickName = "Teams IP Phone - Teams Devices (debug)",
        packageName = IPPHONE_APP_PACKAGE_NAME,
        signingCertificateThumbprint = IPPHONE_APP_SHA512_DEBUG_SIGNATURE
    )

    val MSAL_TEST_APP = App(
        nickName = "MSAL Test App",
        packageName = "com.msft.identity.client.sample.local",
        signingCertificateThumbprint = AuthenticationConstants.Broker.BROKER_HOST_APP_SIGNATURE_SHA512
    )

    val MDE_APP_PROD = App(
        nickName = "Microsoft Defender for Endpoint",
        packageName = "com.microsoft.scmx",
        signingCertificateThumbprint = "iPULpH0pq8ms1Qy7cOzGsVRQN7/zW4IbW+UKcajvtrTrzM5o5VcaghNEA1Ho4Wq7ay0efqqJcalxa8eHxVnHKA=="
    )

    val MDE_APP_DEBUG = App(
        nickName = "Microsoft Defender for Endpoint",
        packageName = "com.microsoft.scmx",
        signingCertificateThumbprint = "k0ZSm/+bEPZAq6mXujRXqP3B6+Zb2yXCiqwuvtCooLfKS91zvHCf+D9FFUYIkJyIKmn1onyWbwRXHEWfS5SaHQ=="
    )

    @JvmField
    val SSO_TOKEN_AUTHORIZED_APPS = buildSet {
        add(EDGE)
        add(EDGE_BETA)
        add(EDGE_CANARY)
        if (BrokerData.getShouldTrustDebugBrokers()) {
            add(BrokerData.debugBrokerHost)
            add(ONE_AUTH_TEST_APP)
        }
    }

    @JvmField
    val GET_DEVICE_TOKEN_AUTHORIZED_APPS = buildSet {
        add(INTUNE_AOSP_AGENT_PROD)
        add(MDE_APP_PROD)
        if (BrokerData.getShouldTrustDebugBrokers()) {
            add(INTUNE_AOSP_AGENT_DEBUG)
            add(BrokerData.debugBrokerHost)
            add(BrokerData.debugMicrosoftAuthenticator)
            add(MDE_APP_DEBUG)
        }
    }

    @JvmField
    val DEVICE_REGISTRATION_AUTHORIZED_APPS = buildSet {
        add(BrokerData.prodMicrosoftAuthenticator)
        add(BrokerData.prodCompanyPortal)
        add(INTUNE_CE_PROD)
        add(INTUNE_AOSP_AGENT_PROD)
        add(TEAMS_IPPHONE_PROD)
        add(MDE_APP_PROD)
        if (BrokerData.getShouldTrustDebugBrokers()) {
            add(INTUNE_AOSP_AGENT_DEBUG)
            add(BrokerData.debugBrokerHost)
            add(BrokerData.debugMicrosoftAuthenticator)
            add(BrokerData.debugCompanyPortal)
            add(INTUNE_CE_DEBUG)
            add(TEAMS_IPPHONE_DEBUG)
            add(MDE_APP_DEBUG)
        }
    }

    /**
     * Apps authorized to request Browser SSO headers (PRT credentials).
     * Currently limited to Chrome browser variants.
     */
    @JvmField
    val BROWSER_SSO_AUTHORIZED_APPS = buildSet {
        add(CHROME)
        add(CHROME_BETA)
        add(CHROME_DEV)
        add(CHROME_CANARY)
        if (BrokerData.getShouldTrustDebugBrokers()) {
            add(MSAL_TEST_APP)
        }
    }

    /**
     * Apps authorized to trigger force broker discovery.
     * Debug apps (mock brokers, broker host) are included when debug broker trust is enabled.
     */
    @JvmField
    val FORCE_BROKER_DISCOVERY_ALLOW_LIST = buildSet {
        add(INTUNE_CE_PROD)
        if (BrokerData.getShouldTrustDebugBrokers()) {
            add(INTUNE_CE_DEBUG)
            add(BrokerData.debugMockLtw)
            add(BrokerData.debugMockCp)
            add(BrokerData.debugMockAuthApp)
            add(BrokerData.debugBrokerHost)
        }
    }
}
