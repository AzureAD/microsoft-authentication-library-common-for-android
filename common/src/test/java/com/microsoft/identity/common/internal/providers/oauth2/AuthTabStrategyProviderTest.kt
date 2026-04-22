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

import android.content.Context
import androidx.fragment.app.FragmentActivity
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthTabStrategyProviderTest {

    @After
    fun tearDown() {
        AuthTabStrategyProvider.unregister()
    }

    @Test
    fun isAuthTabSupported_returnsFalse_whenNotRegistered() {
        val supported = AuthTabStrategyProvider.isAuthTabSupported(
            mockk<Context>(relaxed = true),
            "com.browser"
        )
        assertFalse(supported)
    }

    @Test
    fun createStrategy_returnsNull_whenNotRegistered() {
        val strategy = AuthTabStrategyProvider.createStrategy(
            mockk<FragmentActivity>(relaxed = true)
        ) { _ -> }
        assertNull(strategy)
    }

    @Test
    fun register_makesProviderAvailable_andDelegatesToRegisteredImplementations() {
        val expectedStrategy = mockk<BrowserLaunchStrategy>()
        AuthTabStrategyProvider.register(
            factory = { _, _ -> expectedStrategy },
            isSupported = { _, browserPackage -> browserPackage == "com.contoso.browser" }
        )

        assertTrue(AuthTabStrategyProvider.isAvailable())
        assertTrue(
            AuthTabStrategyProvider.isAuthTabSupported(
                mockk<Context>(relaxed = true),
                "com.contoso.browser"
            )
        )
        assertSame(
            expectedStrategy,
            AuthTabStrategyProvider.createStrategy(
                mockk<FragmentActivity>(relaxed = true)
            ) { _ -> }
        )
    }
}
