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
package com.microsoft.identity.common.internal.broker

import com.microsoft.identity.common.BuildConfig
import com.microsoft.identity.common.internal.broker.BrokerData.Companion.debugBrokerHost
import com.microsoft.identity.common.internal.broker.BrokerData.Companion.debugLTW
import com.microsoft.identity.common.internal.broker.BrokerData.Companion.debugMicrosoftAuthenticator
import com.microsoft.identity.common.internal.broker.BrokerData.Companion.debugMockAuthApp
import com.microsoft.identity.common.internal.broker.BrokerData.Companion.debugMockCp
import com.microsoft.identity.common.internal.broker.BrokerData.Companion.debugMockLtw
import com.microsoft.identity.common.internal.broker.BrokerData.Companion.prodCompanyPortal
import com.microsoft.identity.common.internal.broker.BrokerData.Companion.prodMicrosoftAuthenticator
import com.microsoft.identity.common.internal.broker.BrokerData.Companion.setShouldTrustDebugBrokers
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit Tests for [BrokerData].
 */
@RunWith(RobolectricTestRunner::class)
class BrokerDataTest {

    @After
    fun tearDown(){
        //Reset value.
        setShouldTrustDebugBrokers(BuildConfig.DEBUG)
    }

    @Test
    fun testGetValidBrokersInDebugMode() {
        setShouldTrustDebugBrokers(true)
        val brokerData: Set<BrokerData> = BrokerData.getKnownBrokerApps()
        Assert.assertEquals(8, brokerData.size.toLong())
        Assert.assertTrue(brokerData.contains(debugBrokerHost))
        Assert.assertTrue(brokerData.contains(prodCompanyPortal))
        Assert.assertTrue(brokerData.contains(debugMicrosoftAuthenticator))
        Assert.assertTrue(brokerData.contains(prodMicrosoftAuthenticator))
        Assert.assertTrue(brokerData.contains(debugLTW))
        Assert.assertTrue(brokerData.contains(debugMockLtw))
        Assert.assertTrue(brokerData.contains(debugMockCp))
        Assert.assertTrue(brokerData.contains(debugMockAuthApp))
    }

    @Test
    fun testGetValidBrokersInReleaseMode() {
        setShouldTrustDebugBrokers(false)
        val brokerData: Set<BrokerData> = BrokerData.getKnownBrokerApps()
        Assert.assertEquals(2, brokerData.size.toLong())
        Assert.assertTrue(brokerData.contains(prodCompanyPortal))
        Assert.assertTrue(brokerData.contains(prodMicrosoftAuthenticator))
    }

    @Test
    fun testEqual(){
        Assert.assertTrue(BrokerData("HelloPackage", "HelloCertHash")
                == BrokerData("HelloPackage", "HelloCertHash"))
    }
}
