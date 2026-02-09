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


import com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.INTUNE_AOSP_AGENT_DEBUG_SIGNATURE
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.INTUNE_AOSP_AGENT_RELEASE_SIGNATURE
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
        add(BrokerData.prodIntuneCE)
        add(INTUNE_AOSP_AGENT_PROD)
        if (BrokerData.getShouldTrustDebugBrokers()) {
            add(INTUNE_AOSP_AGENT_DEBUG)
            add(BrokerData.debugBrokerHost)
            add(BrokerData.debugMicrosoftAuthenticator)
            add(BrokerData.debugCompanyPortal)
            add(BrokerData.debugIntuneCE)
        }
    }
}
