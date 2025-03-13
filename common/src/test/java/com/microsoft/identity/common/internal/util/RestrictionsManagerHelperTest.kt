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
package com.microsoft.identity.common.internal.util

import android.content.Context
import android.content.RestrictionsManager
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowRestrictionsManager

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowRestrictionsManager::class])
class RestrictionsManagerHelperTest {

    private lateinit var context: Context
    private lateinit var restrictionsManagerHelper: RestrictionsManagerHelper
    private fun setRestrictionsManager(bundle: Bundle) {
        Shadows.shadowOf(
            context.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
        ).setApplicationRestrictions(bundle)
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        restrictionsManagerHelper =
            RestrictionsManagerHelper(context)
    }
    @Test
    fun testGetRestrictionsManagerHelperDefaultBoolean() {
        // Set test value on the restrictions manager
        setRestrictionsManager(Bundle())
        val defaultValue = restrictionsManagerHelper.getBoolean(key = "test", default = false)
        Assert.assertFalse(defaultValue)
    }

    @Test
    fun testGetRestrictionsManagerHelperDefaultString() {
        // Set test value on the restrictions manager
        setRestrictionsManager(Bundle())
        val defaultValue = restrictionsManagerHelper.getString(key = "test", default = "default")
        Assert.assertEquals("default", defaultValue)
    }

    @Test
    fun testGetRestrictionsManagerHelperSetBooleanTrue() {
        // Set test value on the restrictions manager
        setRestrictionsManager(Bundle().apply { putBoolean("test", true) })
        // Create a helper with the mocked restrictions manager and get the value
        val setValue = restrictionsManagerHelper.getBoolean("test")
        Assert.assertTrue(setValue)
    }

    @Test
    fun testGetRestrictionsManagerHelperSetBooleanFalse() {
        // Set test value on the restrictions manager
        setRestrictionsManager(Bundle().apply { putBoolean("test", false) })

        // Create a helper with the mocked restrictions manager and get the value
        val setValue = restrictionsManagerHelper.getBoolean("test")
        Assert.assertFalse(setValue)
    }

    @Test
    fun testGetRestrictionsManagerHelperSetString() {
        // Set test value on the restrictions manager
        setRestrictionsManager(Bundle().apply { putString("test", "expected") })

        // Create a helper with the mocked restrictions manager and get the value
        val setValue = restrictionsManagerHelper.getString("test")
        Assert.assertEquals("expected", setValue)
    }

    @Test
    fun testGetRestrictionsManagerHelperPutBooleanKeyOnBundleRequest() {
        setRestrictionsManager(Bundle())

        val bundleRequest = restrictionsManagerHelper.createRequestBundle(
            booleanKeysToInclude = setOf("value0", "value1"),
        )
        val keys = bundleRequest.getStringArrayList(RestrictionsManagerHelper.BOOLEAN_VALUES_KEY)
        Assert.assertEquals(2, keys?.size)
        Assert.assertEquals("value0", keys?.get(0))
        Assert.assertEquals("value1", keys?.get(1))
    }

    @Test
    fun testGetRestrictionsManagerHelperPutStringKeyOnBundleRequest() {
        setRestrictionsManager(Bundle())

        val bundleRequest = restrictionsManagerHelper.createRequestBundle(
            stringKeysToInclude = setOf("value0", "value1"),
        )
        val keys = bundleRequest.getStringArrayList(RestrictionsManagerHelper.STRING_VALUES_KEY)
        Assert.assertEquals(2, keys?.size)
        Assert.assertEquals("value0", keys?.get(0))
        Assert.assertEquals("value1", keys?.get(1))
    }

    @Test
    fun testGetRestrictionsManagerHelperGetFilteredBundleFromLocalRestrictionManager() {
        // Set test value on the restrictions manager
        setRestrictionsManager(
            Bundle().apply {
                putBoolean("bkey0", true)
                putBoolean("bkey1", false)
                putString("skey0", "value0")
            }
        )

        val bundleRequest = restrictionsManagerHelper.createRequestBundle(
            stringKeysToInclude = setOf("skey0"),
            booleanKeysToInclude = setOf("bkey0", "bkey1"),
        )

        val result = restrictionsManagerHelper.getFilteredBundleFromLocalRestrictionManager(bundleRequest)
        Assert.assertEquals("value0", result.getString("skey0"))
        Assert.assertTrue(result.getBoolean("bkey0"))
        Assert.assertFalse(result.getBoolean("bkey1"))
    }
}
