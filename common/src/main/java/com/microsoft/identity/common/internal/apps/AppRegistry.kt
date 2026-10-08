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
        signingCertificateThumbprint = "JlOLOTFn6OFBFWuWQJYJ8h/aozEN7/zLFTfioXiXTrU6Yaft4cdEbdpkoJIvmB7Gv2HpHu6QOz+XIaXybtzL7A=="
    )

    val CHROME_CANARY = App(
        nickName = "Google Chrome Canary",
        packageName = "com.chrome.canary",
        signingCertificateThumbprint = "QfTWFoLyXuOCZ7bMYlMN+la3J3rau5x8p+w2v7vf1gOPiTyIMgdbNDzLaLWhgiC2ioj/hFqk8oZyqdJbFG6G4g=="
    )

    /** Release signing identity for Island. */
    val ISLAND = App(
        nickName = "Island",
        packageName = "io.island.Island",
        signingCertificateThumbprint = "9A0EWFnAmSLLMpJP9YP/zZL2PUxLL+eTfnTgI4wsCylaHdd5o4lwjZLjNkPZ7cu5tPYZHLCrEPJVJJNcgoRrAw=="
    )

    /** Release signing identity for Island Canary. */
    val ISLAND_CANARY = App(
        nickName = "Island Canary",
        packageName = "io.island.IslandCanary",
        signingCertificateThumbprint = "TwrYxhsZaPoxnXrFnGt5gzMg9WsV2V0LTHFd0KvqACW9cKfCQc9yPkV+e+f7lAfalMs3zdMfIzUpJZIT7tbOfQ=="
    )

    /** Release signing identity for Island Beta. */
    val ISLAND_BETA = App(
        nickName = "Island Beta",
        packageName = "io.island.IslandBeta",
        signingCertificateThumbprint = "KcVfTD7f9blSNklGx92FzIx1qwo1l7Vtp+sOUq+P2RNb7sSFZNLIBU6JdftdcNmmmv7rpMVcHTs7H1MJMfRkjA=="
    )

    /** Release signing identity for Island with Intune integration. */
    val ISLAND_INTUNE = App(
        nickName = "Island Intune",
        packageName = "io.island.island.intune",
        signingCertificateThumbprint = "q4Ycw2UxQJfVXEcREJwIeszP88D8QKdQw82y3m9Zmg8Sg+2YYP+wq9TATX9PgGjpgd2YfgcrXgsbuubbzdRqtA=="
    )

    /** Release signing identity for Island Canary with Intune integration. */
    val ISLAND_CANARY_INTUNE = App(
        nickName = "Island Canary Intune",
        packageName = "io.island.island.canary.intune",
        signingCertificateThumbprint = "XVdkJe6bsmBS3/a2utbFAfXVTg0IaXJS6hvzjx0Uyg+7q7187oXRAAxROMj8NkTiRlI43SCippiDTtRam5asww=="
    )

    /** Release signing identity for Island Beta with Intune integration. */
    val ISLAND_BETA_INTUNE = App(
        nickName = "Island Beta Intune",
        packageName = "io.island.island.beta.intune",
        signingCertificateThumbprint = "djswQiqC4rjGg2rNmkm+Y090echgP5JGoVesHJ2NnC1q7lQWJ/2a70HSOzMdXbYGtl0aZydlkR2sbMqRbeb4Lg=="
    )

    private const val ISLAND_USERDEBUG_SIGNATURE =
        "+P6Af2Jk8nb0tvnmyhZ6d6mrsJ5znPI597Vq7t5EvgcgsM3LYBmwnBnrI5z/RGBZTzwHVG3+nz8Ostf13u/4YQ=="

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
     * Contains Chrome and Island release identities. Broker must additionally enforce the
     * Island flight; Island userdebug identities require runtime debug trust.
     */
    @JvmField
    val BROWSER_SSO_AUTHORIZED_APPS = buildSet {
        add(CHROME)
        add(CHROME_BETA)
        add(CHROME_DEV)
        add(CHROME_CANARY)
        add(ISLAND)
        add(ISLAND_CANARY)
        add(ISLAND_BETA)
        add(ISLAND_INTUNE)
        add(ISLAND_CANARY_INTUNE)
        add(ISLAND_BETA_INTUNE)
        if (BrokerData.getShouldTrustDebugBrokers()) {
            add(MSAL_TEST_APP)
        }
    }

    /**
     * Island userdebug identities eligible only when the Island flight AND
     * [BrokerData.getShouldTrustDebugBrokers] are enabled at the time of the request.
     * The shared userdebug certificate does not authorize any other package.
     */
    @JvmField
    val ISLAND_BROWSER_SSO_DEBUG_AUTHORIZED_APPS = buildSet {
        listOf(
            ISLAND, ISLAND_CANARY, ISLAND_BETA, ISLAND_INTUNE,
            ISLAND_CANARY_INTUNE, ISLAND_BETA_INTUNE
        ).forEach { releaseApp ->
            add(
                App(
                    nickName = "${releaseApp.nickName} (userdebug)",
                    packageName = releaseApp.packageName,
                    signingCertificateThumbprint = ISLAND_USERDEBUG_SIGNATURE
                )
            )
        }
        add(
            App(
                nickName = "Island Dev (userdebug)",
                packageName = "io.island.IslandDev",
                signingCertificateThumbprint = ISLAND_USERDEBUG_SIGNATURE
            )
        )
        add(
            App(
                nickName = "Island Dev Intune (userdebug)",
                packageName = "io.island.island.dev.intune",
                signingCertificateThumbprint = ISLAND_USERDEBUG_SIGNATURE
            )
        )
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
