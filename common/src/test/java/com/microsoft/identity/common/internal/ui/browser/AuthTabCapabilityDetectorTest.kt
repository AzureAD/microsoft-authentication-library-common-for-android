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
import androidx.browser.customtabs.CustomTabsClient
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Unit tests for [AuthTabCapabilityDetector].
 */
@RunWith(RobolectricTestRunner::class)
class AuthTabCapabilityDetectorTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val packageName = "com.android.chrome"

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `isAuthTabSupported_nullPackageName_returnsFalse`() {
        val result = AuthTabCapabilityDetector.isAuthTabSupported(context, null)
        assertFalse(result)
    }

    @Test
    fun `isAuthTabSupported_emptyPackageName_returnsFalse`() {
        val result = AuthTabCapabilityDetector.isAuthTabSupported(context, "")
        assertFalse(result)
    }

    @Test
    fun `isAuthTabSupported_supported_returnsTrue`() {
        mockkStatic(CustomTabsClient::class)
        every { CustomTabsClient.isAuthTabSupported(context, packageName) } returns true

        val result = AuthTabCapabilityDetector.isAuthTabSupported(context, packageName)
        assertTrue(result)
    }

    @Test
    fun `isAuthTabSupported_notSupported_returnsFalse`() {
        mockkStatic(CustomTabsClient::class)
        every { CustomTabsClient.isAuthTabSupported(context, packageName) } returns false

        val result = AuthTabCapabilityDetector.isAuthTabSupported(context, packageName)
        assertFalse(result)
    }

    @Test
    fun `isAuthTabSupported_exceptionThrown_returnsFalse`() {
        mockkStatic(CustomTabsClient::class)
        every {
            CustomTabsClient.isAuthTabSupported(context, packageName)
        } throws RuntimeException("Test exception")

        val result = AuthTabCapabilityDetector.isAuthTabSupported(context, packageName)
        assertFalse(result)
    }
}
