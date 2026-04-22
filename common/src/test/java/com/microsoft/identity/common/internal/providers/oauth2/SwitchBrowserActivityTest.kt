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

import android.content.Intent
import io.mockk.anyConstructed
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [SwitchBrowserActivity].
 */
@RunWith(RobolectricTestRunner::class)
class SwitchBrowserActivityTest {

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun onCreate_usesAuthTabStrategy_whenProviderReturnsOne() {
        mockkObject(AuthTabStrategyProvider)
        val authTabStrategy = mockk<BrowserLaunchStrategy>(relaxed = true)
        every { AuthTabStrategyProvider.isAuthTabSupported(any(), any()) } returns true
        every { AuthTabStrategyProvider.createStrategy(any(), any()) } returns authTabStrategy

        Robolectric.buildActivity(SwitchBrowserActivity::class.java, getIntent()).create()

        verify(exactly = 1) { authTabStrategy.launch() }
    }

    @Test
    fun onCreate_fallsBackToCustomTabsStrategy_whenAuthTabUnsupported() {
        mockkObject(AuthTabStrategyProvider)
        every { AuthTabStrategyProvider.isAuthTabSupported(any(), any()) } returns false
        mockkConstructor(CustomTabsLaunchStrategy::class)
        every { anyConstructed<CustomTabsLaunchStrategy>().launch() } just runs

        Robolectric.buildActivity(SwitchBrowserActivity::class.java, getIntent()).create()

        verify(exactly = 1) { anyConstructed<CustomTabsLaunchStrategy>().launch() }
    }

    private fun getIntent(): Intent {
        return Intent().apply {
            putExtra(SwitchBrowserActivity.BROWSER_PACKAGE_NAME, "com.test.browser")
            putExtra(SwitchBrowserActivity.BROWSER_SUPPORTS_CUSTOM_TABS, true)
            putExtra(SwitchBrowserActivity.PROCESS_URI, "https://login.microsoftonline.com/switchbrowser/process")
        }
    }
}
