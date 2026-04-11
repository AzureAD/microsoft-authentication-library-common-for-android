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
package com.microsoft.identity.common.internal.ui.browser

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsClient
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [AuthTabManager].
 */
@RunWith(RobolectricTestRunner::class)
class AuthTabManagerTest {

    @Before
    fun setUp() {
        mockkStatic(CustomTabsClient::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(CustomTabsClient::class)
    }

    @Test
    fun `isSupported returns true when browser supports auth tab`() {
        val context = mock<Context>()
        every { CustomTabsClient.isAuthTabSupported(context, "com.android.chrome") } returns true

        assertTrue(AuthTabManager.isSupported(context, "com.android.chrome"))
    }

    @Test
    fun `isSupported returns false when browser does not support auth tab`() {
        val context = mock<Context>()
        every { CustomTabsClient.isAuthTabSupported(context, "com.android.chrome") } returns false

        assertFalse(AuthTabManager.isSupported(context, "com.android.chrome"))
    }

    @Test
    fun `isSupported returns false when exception is thrown`() {
        val context = mock<Context>()
        every {
            CustomTabsClient.isAuthTabSupported(context, "com.android.chrome")
        } throws RuntimeException("test exception")

        assertFalse(AuthTabManager.isSupported(context, "com.android.chrome"))
    }

    @Test
    fun `isSupported returns false when package name is empty`() {
        val context = mock<Context>()
        every { CustomTabsClient.isAuthTabSupported(context, "") } returns false

        assertFalse(AuthTabManager.isSupported(context, ""))
    }

    @Test
    fun `launch throws when registerLauncher has not been called`() {
        val manager = AuthTabManager()
        val uri = Uri.parse("https://login.microsoft.com/auth")
        try {
            manager.launch(uri, "msauth")
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // Expected: "AuthTabManager.launch() called before registerLauncher()"
            assertTrue(e.message?.contains("registerLauncher") == true)
        }
    }

    @Test
    fun `unregister clears the launcher without throwing`() {
        val manager = AuthTabManager()
        // Should not throw even when called before registerLauncher
        manager.unregister()
        // Verify that launch fails after unregister
        try {
            manager.launch(Uri.parse("https://login.microsoft.com"), "msauth")
            fail("Expected exception after unregister")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }
}
