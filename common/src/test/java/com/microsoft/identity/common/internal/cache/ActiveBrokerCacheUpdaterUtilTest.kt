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
package com.microsoft.identity.common.internal.cache

import android.os.Bundle
import com.microsoft.identity.common.internal.activebrokerdiscovery.InMemoryActiveBrokerCache
import com.microsoft.identity.common.internal.broker.BrokerData
import com.microsoft.identity.common.internal.cache.ActiveBrokerCacheUpdaterUtil.Companion.ACTIVE_BROKER_PACKAGE_NAME_KEY
import com.microsoft.identity.common.internal.cache.ActiveBrokerCacheUpdaterUtil.Companion.ACTIVE_BROKER_SIGNING_CERTIFICATE_THUMBPRINT_KEY
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ActiveBrokerCacheUpdaterUtilTest {

    private val newActiveBroker = BrokerData(
        "com.microsoft.newActiveBroker",
        "SOME_SIG_HASH"
    )

    private val anotherBrokerApp = BrokerData(
        "com.microsoft.someotherapp",
        "SOME_SIG_HASH"
    )

    @Test
    fun testAppendResultToBundle(){
        val bundle = Bundle()
        ActiveBrokerCacheUpdaterUtil.appendActiveBrokerToResultBundle(bundle, newActiveBroker)

        Assert.assertEquals(newActiveBroker.packageName,
            bundle.getString(ACTIVE_BROKER_PACKAGE_NAME_KEY))

        Assert.assertEquals(newActiveBroker.signingCertificateThumbprint,
            bundle.getString(ACTIVE_BROKER_SIGNING_CERTIFICATE_THUMBPRINT_KEY))
    }

    @Test
    fun testTryUpdateWithNullBundle(){
        val util = ActiveBrokerCacheUpdaterUtil { _: BrokerData ->
            // Bypass the Validation check.
            true
        }
        val cache = InMemoryActiveBrokerCache()

        util.updateCachedActiveBrokerFromResultBundle(cache, null)
        Assert.assertNull(cache.getCachedActiveBroker())
    }

    @Test
    fun testTryUpdateWithIncompleteBundle(){
        val util = ActiveBrokerCacheUpdaterUtil { _: BrokerData ->
            // Bypass the Validation check.
            true
        }
        val cache = InMemoryActiveBrokerCache()

        val bundle1 = Bundle();
        bundle1.putString(ACTIVE_BROKER_PACKAGE_NAME_KEY, "")
        bundle1.putString(ACTIVE_BROKER_SIGNING_CERTIFICATE_THUMBPRINT_KEY, "someThumbPrint")
        util.updateCachedActiveBrokerFromResultBundle(cache, bundle1)
        Assert.assertNull(cache.getCachedActiveBroker())

        val bundle2 = Bundle();
        bundle2.putString(ACTIVE_BROKER_PACKAGE_NAME_KEY, "somePackageName")
        bundle2.putString(ACTIVE_BROKER_SIGNING_CERTIFICATE_THUMBPRINT_KEY, "")
        util.updateCachedActiveBrokerFromResultBundle(cache, bundle2)
        Assert.assertNull(cache.getCachedActiveBroker())

        val bundle3 = Bundle()
        bundle3.putString(ACTIVE_BROKER_PACKAGE_NAME_KEY, "somePackageName")
        util.updateCachedActiveBrokerFromResultBundle(cache, bundle3)
        Assert.assertNull(cache.getCachedActiveBroker())

        val bundle4 = Bundle()
        bundle4.putString(ACTIVE_BROKER_SIGNING_CERTIFICATE_THUMBPRINT_KEY, "SomeThumbPrint")
        util.updateCachedActiveBrokerFromResultBundle(cache, bundle4)
        Assert.assertNull(cache.getCachedActiveBroker())
    }

    @Test
    fun testTryUpdateWithUnknownApps(){
        val util = ActiveBrokerCacheUpdaterUtil { brokerData: BrokerData ->
            brokerData == newActiveBroker
        }
        val cache = InMemoryActiveBrokerCache()

        val bundle = Bundle()
        ActiveBrokerCacheUpdaterUtil.appendActiveBrokerToResultBundle(bundle, anotherBrokerApp)
        util.updateCachedActiveBrokerFromResultBundle(cache, bundle)
        Assert.assertNull(cache.getCachedActiveBroker())
    }

    @Test
    fun testTryUpdateWithKnownBrokerAppsInBundle(){
        val util = ActiveBrokerCacheUpdaterUtil { brokerData: BrokerData ->
            brokerData == newActiveBroker
        }
        val cache = InMemoryActiveBrokerCache()

        val bundle = Bundle()
        ActiveBrokerCacheUpdaterUtil.appendActiveBrokerToResultBundle(bundle, newActiveBroker)
        util.updateCachedActiveBrokerFromResultBundle(cache, bundle)
        Assert.assertEquals(newActiveBroker, cache.getCachedActiveBroker())
    }
}