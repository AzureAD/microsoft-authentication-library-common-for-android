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
package com.microsoft.identity.common.internal.providers.oauth2

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Tests for [AuthTabStrategyProvider].
 */
@RunWith(RobolectricTestRunner::class)
class AuthTabStrategyProviderTest {

    @After
    fun tearDown() {
        AuthTabStrategyProvider.resetForTest()
    }

    @Test
    fun defaults_returnUnavailableAndNoStrategy() {
        val context = RuntimeEnvironment.getApplication()
        val activity = mock<FragmentActivity>()

        assertFalse(AuthTabStrategyProvider.isAvailable())
        assertFalse(AuthTabStrategyProvider.isAuthTabSupported(context, "com.test.browser"))
        assertNull(AuthTabStrategyProvider.createStrategy(activity) { _: Bundle -> })
    }

    @Test
    fun register_delegatesSupportAndFactory() {
        val activity = mock<FragmentActivity>()
        val strategy = mock<BrowserLaunchStrategy>()
        var supportCheckCalled = false
        var factoryCalled = false

        AuthTabStrategyProvider.register(
            factory = { _, _ ->
                factoryCalled = true
                strategy
            },
            isSupported = { _, packageName ->
                supportCheckCalled = true
                packageName == "com.test.browser"
            }
        )

        assertTrue(AuthTabStrategyProvider.isAvailable())
        assertTrue(AuthTabStrategyProvider.isAuthTabSupported(RuntimeEnvironment.getApplication(), "com.test.browser"))
        assertTrue(supportCheckCalled)

        val createdStrategy = AuthTabStrategyProvider.createStrategy(activity) { _: Bundle -> }
        assertNotNull(createdStrategy)
        assertTrue(factoryCalled)
        assertSame(strategy, createdStrategy)
    }
}
