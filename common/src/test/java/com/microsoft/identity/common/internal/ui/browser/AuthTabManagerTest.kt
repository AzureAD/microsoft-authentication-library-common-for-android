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
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.browser.customtabs.AuthTabIntent
import androidx.browser.customtabs.CustomTabsClient
import com.microsoft.identity.common.java.providers.RawAuthorizationResult
import com.microsoft.identity.common.java.providers.RawAuthorizationResult.ResultCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthTabManagerTest {

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun test_isAuthTabSupported_returnsTrue_whenClientReturnsTrue() {
        mockkStatic(CustomTabsClient::class)
        val context = mockk<Context>()
        every { CustomTabsClient.isAuthTabSupported(context) } returns true

        assertTrue(AuthTabManager.isAuthTabSupported(context))
    }

    @Test
    fun test_isAuthTabSupported_returnsFalse_whenClientReturnsFalse() {
        mockkStatic(CustomTabsClient::class)
        val context = mockk<Context>()
        every { CustomTabsClient.isAuthTabSupported(context) } returns false

        assertFalse(AuthTabManager.isAuthTabSupported(context))
    }

    @Test
    fun test_isAuthTabSupported_returnsFalse_onException() {
        mockkStatic(CustomTabsClient::class)
        val context = mockk<Context>()
        every { CustomTabsClient.isAuthTabSupported(context) } throws RuntimeException("test error")

        assertFalse(AuthTabManager.isAuthTabSupported(context))
    }

    @Test
    fun test_mapAuthResultToRawResult_RESULT_OK_withUri() {
        val manager = AuthTabManager()
        val authResult = mockk<AuthTabIntent.AuthResult>()
        val redirectUri = Uri.parse("myscheme://callback?code=abc123")
        every { authResult.resultCode } returns AuthTabIntent.RESULT_OK
        every { authResult.resultUri } returns redirectUri

        val result = manager.mapAuthResultToRawResult(authResult)

        assertEquals(ResultCode.COMPLETED, result.resultCode)
        assertNotNull(result.authorizationFinalUri)
        assertEquals(redirectUri.toString(), result.authorizationFinalUri.toString())
    }

    @Test
    fun test_mapAuthResultToRawResult_RESULT_OK_withNullUri() {
        val manager = AuthTabManager()
        val authResult = mockk<AuthTabIntent.AuthResult>()
        every { authResult.resultCode } returns AuthTabIntent.RESULT_OK
        every { authResult.resultUri } returns null

        val result = manager.mapAuthResultToRawResult(authResult)

        assertEquals(ResultCode.NON_OAUTH_ERROR, result.resultCode)
        assertNotNull(result.exception)
        assertEquals("authorization_result_not_found", result.exception?.errorCode)
    }

    @Test
    fun test_mapAuthResultToRawResult_RESULT_CANCELED() {
        val manager = AuthTabManager()
        val authResult = mockk<AuthTabIntent.AuthResult>()
        every { authResult.resultCode } returns AuthTabIntent.RESULT_CANCELED

        val result = manager.mapAuthResultToRawResult(authResult)

        assertEquals(ResultCode.CANCELLED, result.resultCode)
    }

    @Test
    fun test_mapAuthResultToRawResult_RESULT_VERIFICATION_FAILED() {
        val manager = AuthTabManager()
        val authResult = mockk<AuthTabIntent.AuthResult>()
        every { authResult.resultCode } returns AuthTabIntent.RESULT_VERIFICATION_FAILED

        val result = manager.mapAuthResultToRawResult(authResult)

        assertEquals(ResultCode.NON_OAUTH_ERROR, result.resultCode)
        assertNotNull(result.exception)
        assertEquals("auth_tab_verification_failed", result.exception?.errorCode)
    }

    @Test
    fun test_mapAuthResultToRawResult_RESULT_VERIFICATION_TIMED_OUT() {
        val manager = AuthTabManager()
        val authResult = mockk<AuthTabIntent.AuthResult>()
        every { authResult.resultCode } returns AuthTabIntent.RESULT_VERIFICATION_TIMED_OUT

        val result = manager.mapAuthResultToRawResult(authResult)

        assertEquals(ResultCode.NON_OAUTH_ERROR, result.resultCode)
        assertNotNull(result.exception)
        assertEquals("auth_tab_verification_timed_out", result.exception?.errorCode)
    }

    @Test
    fun test_launch_throwsIfLauncherNotRegistered() {
        val manager = AuthTabManager()
        val authUrl = Uri.parse("https://login.microsoftonline.com/authorize")

        assertThrows(IllegalStateException::class.java) {
            manager.launch(authUrl, "myscheme")
        }
    }

    @Test
    fun test_registerLauncher_setsLauncherField() {
        mockkStatic(AuthTabIntent::class)
        val mockLauncher = mockk<ActivityResultLauncher<Intent>>(relaxed = true)
        val mockCaller = mockk<ActivityResultCaller>()
        every { AuthTabIntent.registerActivityResultLauncher(mockCaller, any()) } returns mockLauncher

        val manager = AuthTabManager()
        val returned = manager.registerLauncher(mockCaller) { /* no-op */ }

        // Verify chaining returns the same manager instance
        assertSame(manager, returned)

        // Verify the launcher is set: launch() should no longer throw IllegalStateException.
        // Mock AuthTabIntent.Builder so launch() can proceed without a real browser.
        val mockAuthTabIntent = mockk<AuthTabIntent>(relaxed = true)
        every { AuthTabIntent.Builder().build() } returns mockAuthTabIntent

        val authUrl = Uri.parse("https://login.microsoftonline.com/authorize")
        // If mLauncher is null this would throw; passing means launcher was set correctly.
        manager.launch(authUrl, "myscheme")
    }
}
