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

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.browser.auth.AuthTabIntent
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [AuthTabManager].
 */
@RunWith(RobolectricTestRunner::class)
class AuthTabManagerTest {

    private val mockActivity: ComponentActivity = mockk(relaxed = true)
    private val mockUri: Uri = mockk()
    @Suppress("UNCHECKED_CAST")
    private val mockLauncher: ActivityResultLauncher<Uri> =
        mockk<ActivityResultLauncher<Uri>>(relaxed = true)
    @Suppress("UNCHECKED_CAST")
    private val mockCallback: ActivityResultCallback<Uri> = mockk()

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test(expected = IllegalStateException::class)
    fun `launch_beforeRegisterLauncher_throwsIllegalStateException`() {
        val manager = AuthTabManager()
        manager.launch(mockUri)
    }

    @Test
    fun `registerLauncher_callsAuthTabIntentRegisterActivityResultLauncher`() {
        mockkStatic(AuthTabIntent::class)
        every {
            AuthTabIntent.registerActivityResultLauncher(mockActivity, mockCallback)
        } returns mockLauncher

        val manager = AuthTabManager()
        manager.registerLauncher(mockActivity, mockCallback)

        verify(exactly = 1) {
            AuthTabIntent.registerActivityResultLauncher(mockActivity, mockCallback)
        }
    }

    @Test
    fun `launch_afterRegisterLauncher_buildsAndLaunchesAuthTabIntent`() {
        mockkStatic(AuthTabIntent::class)
        every {
            AuthTabIntent.registerActivityResultLauncher(mockActivity, mockCallback)
        } returns mockLauncher

        val mockAuthTabIntent: AuthTabIntent = mockk(relaxed = true)
        mockkConstructor(AuthTabIntent.Builder::class)
        every { anyConstructed<AuthTabIntent.Builder>().build() } returns mockAuthTabIntent

        val manager = AuthTabManager()
        manager.registerLauncher(mockActivity, mockCallback)
        manager.launch(mockUri)

        verify(exactly = 1) { mockAuthTabIntent.launch(mockLauncher, mockUri) }
    }
}
