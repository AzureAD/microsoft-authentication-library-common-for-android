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
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class SwitchBrowserActivityTest {

    @After
    fun tearDown() {
        unmockkObject(AuthTabStrategyProvider)
    }

    @Test
    fun onCreate_usesAuthTabStrategy_whenProviderReturnsStrategy() {
        mockkObject(AuthTabStrategyProvider)
        val authTabStrategy = mockk<BrowserLaunchStrategy>()

        every { AuthTabStrategyProvider.isAuthTabSupported(any(), any()) } returns true
        every { AuthTabStrategyProvider.createStrategy(any(), any()) } returns authTabStrategy
        every { authTabStrategy.launch(any(), any()) } just Runs
        every { authTabStrategy.handlesCancellationOnResume() } returns false
        every { authTabStrategy.cleanup() } just Runs

        val processUri = "https://login.microsoftonline.com/switchbrowser/process"
        val browserPackage = "com.contoso.browser"
        val intent = Intent().apply {
            putExtra(SwitchBrowserActivity.BROWSER_PACKAGE_NAME, browserPackage)
            putExtra(SwitchBrowserActivity.BROWSER_SUPPORTS_CUSTOM_TABS, true)
            putExtra(SwitchBrowserActivity.PROCESS_URI, processUri)
        }

        val controller = Robolectric.buildActivity(SwitchBrowserActivity::class.java, intent).create().start().resume()
        val activity = controller.get()

        verify(exactly = 1) { AuthTabStrategyProvider.isAuthTabSupported(any(), browserPackage) }
        verify(exactly = 1) { AuthTabStrategyProvider.createStrategy(any(), any()) }
        verify(exactly = 1) { authTabStrategy.launch(browserPackage, processUri) }
        assertFalse(activity.isFinishing)

        controller.pause().destroy()
        verify(exactly = 1) { authTabStrategy.cleanup() }
    }

    @Test
    fun onCreate_fallsBackToCustomTabsStrategy_whenProviderReturnsNull() {
        mockkObject(AuthTabStrategyProvider)
        every { AuthTabStrategyProvider.isAuthTabSupported(any(), any()) } returns true
        every { AuthTabStrategyProvider.createStrategy(any(), any()) } returns null

        val processUri = "https://login.microsoftonline.com/switchbrowser/process"
        val browserPackage = "com.contoso.browser"
        val intent = Intent().apply {
            putExtra(SwitchBrowserActivity.BROWSER_PACKAGE_NAME, browserPackage)
            putExtra(SwitchBrowserActivity.BROWSER_SUPPORTS_CUSTOM_TABS, false)
            putExtra(SwitchBrowserActivity.PROCESS_URI, processUri)
        }

        val activity = Robolectric.buildActivity(SwitchBrowserActivity::class.java, intent).create().get()
        val nextIntent = shadowOf(activity).nextStartedActivity

        verify(exactly = 1) { AuthTabStrategyProvider.createStrategy(any(), any()) }
        assertEquals(Intent.ACTION_VIEW, nextIntent.action)
        assertEquals(browserPackage, nextIntent.`package`)
        assertEquals(processUri, nextIntent.dataString)
        assertFalse(activity.isFinishing)
    }

    @Test
    fun onCreate_fallsBackToBrowserIntent_whenCustomTabsBindingFails() {
        mockkObject(AuthTabStrategyProvider)
        every { AuthTabStrategyProvider.isAuthTabSupported(any(), any()) } returns true
        every { AuthTabStrategyProvider.createStrategy(any(), any()) } returns null

        val processUri = "https://login.microsoftonline.com/switchbrowser/process"
        val browserPackage = "com.contoso.browser"
        val intent = Intent().apply {
            putExtra(SwitchBrowserActivity.BROWSER_PACKAGE_NAME, browserPackage)
            putExtra(SwitchBrowserActivity.BROWSER_SUPPORTS_CUSTOM_TABS, true)
            putExtra(SwitchBrowserActivity.PROCESS_URI, processUri)
        }

        val activity = Robolectric.buildActivity(SwitchBrowserActivity::class.java, intent).create().get()
        val nextIntent = shadowOf(activity).nextStartedActivity

        verify(exactly = 1) { AuthTabStrategyProvider.createStrategy(any(), any()) }
        assertEquals(Intent.ACTION_VIEW, nextIntent.action)
        assertEquals(browserPackage, nextIntent.`package`)
        assertEquals(processUri, nextIntent.dataString)
        assertFalse(activity.isFinishing)
    }

    @Test
    fun onCreate_skipsAuthTabProviderFactory_whenAuthTabNotSupported() {
        mockkObject(AuthTabStrategyProvider)
        every { AuthTabStrategyProvider.isAuthTabSupported(any(), any()) } returns false

        val processUri = "https://login.microsoftonline.com/switchbrowser/process"
        val browserPackage = "com.contoso.browser"
        val intent = Intent().apply {
            putExtra(SwitchBrowserActivity.BROWSER_PACKAGE_NAME, browserPackage)
            putExtra(SwitchBrowserActivity.BROWSER_SUPPORTS_CUSTOM_TABS, false)
            putExtra(SwitchBrowserActivity.PROCESS_URI, processUri)
        }

        val activity = Robolectric.buildActivity(SwitchBrowserActivity::class.java, intent).create().get()
        val nextIntent = shadowOf(activity).nextStartedActivity

        verify(exactly = 1) { AuthTabStrategyProvider.isAuthTabSupported(any(), browserPackage) }
        verify(exactly = 0) { AuthTabStrategyProvider.createStrategy(any(), any()) }
        assertEquals(Intent.ACTION_VIEW, nextIntent.action)
        assertEquals(browserPackage, nextIntent.`package`)
        assertEquals(processUri, nextIntent.dataString)
        assertFalse(activity.isFinishing)
    }
}
