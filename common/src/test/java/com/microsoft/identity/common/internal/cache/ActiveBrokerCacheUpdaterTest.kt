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
import com.microsoft.identity.common.internal.cache.ActiveBrokerCacheUpdater.Companion.ACTIVE_BROKER_PACKAGE_NAME_KEY
import com.microsoft.identity.common.internal.cache.ActiveBrokerCacheUpdater.Companion.ACTIVE_BROKER_SIGNING_CERTIFICATE_THUMBPRINT_KEY
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ActiveBrokerCacheUpdaterTest {

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
        ActiveBrokerCacheUpdater.appendActiveBrokerToResultBundle(bundle, newActiveBroker)

        Assert.assertEquals(newActiveBroker.packageName,
            bundle.getString(ACTIVE_BROKER_PACKAGE_NAME_KEY))

        Assert.assertEquals(newActiveBroker.signingCertificateThumbprint,
            bundle.getString(ACTIVE_BROKER_SIGNING_CERTIFICATE_THUMBPRINT_KEY))
    }

    @Test
    fun testTryUpdateWithNullBundle(){
        val cache = InMemoryActiveBrokerCache()
        val cacheUpdater = ActiveBrokerCacheUpdater (
            // Bypass the Validation check.
            { _: BrokerData -> true },
            cache)

        cacheUpdater.updateCachedActiveBrokerFromResultBundle(null)
        Assert.assertNull(cache.getCachedActiveBroker())
    }

    @Test
    fun testTryUpdateWithIncompleteBundle(){
        val cache = InMemoryActiveBrokerCache()
        val cacheUpdater = ActiveBrokerCacheUpdater (
            // Bypass the Validation check.
            { _: BrokerData -> true },
            cache)

        val bundle1 = Bundle();
        bundle1.putString(ACTIVE_BROKER_PACKAGE_NAME_KEY, "")
        bundle1.putString(ACTIVE_BROKER_SIGNING_CERTIFICATE_THUMBPRINT_KEY, "someThumbPrint")
        cacheUpdater.updateCachedActiveBrokerFromResultBundle(bundle1)
        Assert.assertNull(cache.getCachedActiveBroker())

        val bundle2 = Bundle();
        bundle2.putString(ACTIVE_BROKER_PACKAGE_NAME_KEY, "somePackageName")
        bundle2.putString(ACTIVE_BROKER_SIGNING_CERTIFICATE_THUMBPRINT_KEY, "")
        cacheUpdater.updateCachedActiveBrokerFromResultBundle(bundle2)
        Assert.assertNull(cache.getCachedActiveBroker())

        val bundle3 = Bundle()
        bundle3.putString(ACTIVE_BROKER_PACKAGE_NAME_KEY, "somePackageName")
        cacheUpdater.updateCachedActiveBrokerFromResultBundle(bundle3)
        Assert.assertNull(cache.getCachedActiveBroker())

        val bundle4 = Bundle()
        bundle4.putString(ACTIVE_BROKER_SIGNING_CERTIFICATE_THUMBPRINT_KEY, "SomeThumbPrint")
        cacheUpdater.updateCachedActiveBrokerFromResultBundle(bundle4)
        Assert.assertNull(cache.getCachedActiveBroker())
    }

    @Test
    fun testTryUpdateWithUnknownApps(){
        val cache = InMemoryActiveBrokerCache()
        val cacheUpdater = ActiveBrokerCacheUpdater (
            { brokerData: BrokerData -> brokerData == newActiveBroker },
            cache)

        val bundle = Bundle()
        ActiveBrokerCacheUpdater.appendActiveBrokerToResultBundle(bundle, anotherBrokerApp)
        cacheUpdater.updateCachedActiveBrokerFromResultBundle(bundle)
        Assert.assertNull(cache.getCachedActiveBroker())
    }

    @Test
    fun testTryUpdateWithKnownBrokerAppsInBundle(){
        val cache = InMemoryActiveBrokerCache()
        val cacheUpdater = ActiveBrokerCacheUpdater (
            { brokerData: BrokerData -> brokerData == newActiveBroker },
            cache)

        val bundle = Bundle()
        ActiveBrokerCacheUpdater.appendActiveBrokerToResultBundle(bundle, newActiveBroker)
        cacheUpdater.updateCachedActiveBrokerFromResultBundle(bundle)
        Assert.assertEquals(newActiveBroker, cache.getCachedActiveBroker())
    }

    @Test
    fun testWipeCachedActiveBroker(){
        val cache = InMemoryActiveBrokerCache()
        cache.setCachedActiveBroker(newActiveBroker)

        val cacheUpdater = ActiveBrokerCacheUpdater ({ false }, cache)

        val bundle = Bundle()
        ActiveBrokerCacheUpdater.appendBrokerDiscoveryDisabledToResultBundle(bundle)

        Assert.assertEquals(newActiveBroker, cache.getCachedActiveBroker())
        Assert.assertFalse(cache.shouldUseAccountManager())

        cacheUpdater.updateCachedActiveBrokerFromResultBundle(bundle)

        Assert.assertNull(cache.getCachedActiveBroker())
        Assert.assertTrue(cache.shouldUseAccountManager())
    }
}