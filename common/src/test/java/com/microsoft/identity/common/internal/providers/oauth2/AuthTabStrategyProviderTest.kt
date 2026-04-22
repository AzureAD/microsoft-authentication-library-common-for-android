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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [AuthTabStrategyProvider].
 */
@RunWith(RobolectricTestRunner::class)
class AuthTabStrategyProviderTest {

    private class FakeAuthTabLaunchStrategy : BrowserLaunchStrategy {
        override fun launch(processUri: android.net.Uri, browserPackageName: String) = Unit
        override fun handlesCancellationOnResume(): Boolean = false
        override fun cleanup() = Unit
    }

    @After
    fun tearDown() {
        AuthTabStrategyProvider.resetForTest()
    }

    @Test
    fun `isAuthTabSupported returns false when provider is not registered`() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()

        assertFalse(AuthTabStrategyProvider.isAvailable())
        assertFalse(AuthTabStrategyProvider.isAuthTabSupported(activity, "com.android.chrome"))
        assertFalse(AuthTabStrategyProvider.createStrategy(activity) { _: Bundle? -> } != null)
    }

    @Test
    fun `register enables support checks and strategy creation`() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()

        AuthTabStrategyProvider.register(
            factory = { _, _ -> FakeAuthTabLaunchStrategy() },
            isSupported = { _, browserPackage -> browserPackage == "com.android.chrome" }
        )

        assertTrue(AuthTabStrategyProvider.isAvailable())
        assertTrue(AuthTabStrategyProvider.isAuthTabSupported(activity, "com.android.chrome"))
        assertFalse(AuthTabStrategyProvider.isAuthTabSupported(activity, "com.example.browser"))
        assertNotNull(AuthTabStrategyProvider.createStrategy(activity) { _: Bundle? -> })
    }
}
