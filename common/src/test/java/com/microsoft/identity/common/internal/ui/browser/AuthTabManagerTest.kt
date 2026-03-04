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
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.browser.customtabs.AuthTabIntent
import androidx.browser.customtabs.CustomTabsClient
import com.microsoft.identity.common.java.providers.RawAuthorizationResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Unit tests for [AuthTabManager].
 */
@RunWith(RobolectricTestRunner::class)
class AuthTabManagerTest {

    private lateinit var context: Context
    private lateinit var authTabManager: AuthTabManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        authTabManager = AuthTabManager()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // region isAuthTabSupported

    @Test
    fun test_isAuthTabSupported_returnsTrue_whenClientReturnsTrue() {
        mockkStatic(CustomTabsClient::class)
        every { CustomTabsClient.isAuthTabSupported(any()) } returns true

        assertTrue(AuthTabManager.isAuthTabSupported(context))
    }

    @Test
    fun test_isAuthTabSupported_returnsFalse_whenClientReturnsFalse() {
        mockkStatic(CustomTabsClient::class)
        every { CustomTabsClient.isAuthTabSupported(any()) } returns false

        assertFalse(AuthTabManager.isAuthTabSupported(context))
    }

    @Test
    fun test_isAuthTabSupported_returnsFalse_onException() {
        mockkStatic(CustomTabsClient::class)
        every { CustomTabsClient.isAuthTabSupported(any()) } throws RuntimeException("Browser not available")

        assertFalse(AuthTabManager.isAuthTabSupported(context))
    }

    // endregion

    // region mapAuthResultToRawResult

    @Test
    fun test_mapAuthResultToRawResult_RESULT_OK_withUri() {
        val redirectUri = "msauth://com.example.app/callback?code=auth_code"
        val mockAuthResult = mockk<AuthTabIntent.AuthResult>()
        every { mockAuthResult.resultCode } returns AuthTabIntent.RESULT_OK
        every { mockAuthResult.resultUri } returns Uri.parse(redirectUri)

        val result = authTabManager.mapAuthResultToRawResult(mockAuthResult)

        assertNotNull(result)
        assertEquals(RawAuthorizationResult.ResultCode.COMPLETED, result.resultCode)
    }

    @Test
    fun test_mapAuthResultToRawResult_RESULT_OK_withNullUri() {
        val mockAuthResult = mockk<AuthTabIntent.AuthResult>()
        every { mockAuthResult.resultCode } returns AuthTabIntent.RESULT_OK
        every { mockAuthResult.resultUri } returns null

        val result = authTabManager.mapAuthResultToRawResult(mockAuthResult)

        assertNotNull(result)
        assertEquals(RawAuthorizationResult.ResultCode.NON_OAUTH_ERROR, result.resultCode)
        assertNotNull(result.exception)
        assertEquals("authorization_result_not_found", result.exception!!.errorCode)
    }

    @Test
    fun test_mapAuthResultToRawResult_RESULT_CANCELED() {
        val mockAuthResult = mockk<AuthTabIntent.AuthResult>()
        every { mockAuthResult.resultCode } returns AuthTabIntent.RESULT_CANCELED

        val result = authTabManager.mapAuthResultToRawResult(mockAuthResult)

        assertNotNull(result)
        assertEquals(RawAuthorizationResult.ResultCode.CANCELLED, result.resultCode)
    }

    @Test
    fun test_mapAuthResultToRawResult_RESULT_VERIFICATION_FAILED() {
        val mockAuthResult = mockk<AuthTabIntent.AuthResult>()
        every { mockAuthResult.resultCode } returns AuthTabIntent.RESULT_VERIFICATION_FAILED

        val result = authTabManager.mapAuthResultToRawResult(mockAuthResult)

        assertNotNull(result)
        assertEquals(RawAuthorizationResult.ResultCode.NON_OAUTH_ERROR, result.resultCode)
        assertNotNull(result.exception)
        assertEquals("auth_tab_verification_failed", result.exception!!.errorCode)
    }

    @Test
    fun test_mapAuthResultToRawResult_RESULT_VERIFICATION_TIMED_OUT() {
        val mockAuthResult = mockk<AuthTabIntent.AuthResult>()
        every { mockAuthResult.resultCode } returns AuthTabIntent.RESULT_VERIFICATION_TIMED_OUT

        val result = authTabManager.mapAuthResultToRawResult(mockAuthResult)

        assertNotNull(result)
        assertEquals(RawAuthorizationResult.ResultCode.NON_OAUTH_ERROR, result.resultCode)
        assertNotNull(result.exception)
        assertEquals("auth_tab_verification_timed_out", result.exception!!.errorCode)
    }

    @Test
    fun test_mapAuthResultToRawResult_unknownResultCode() {
        val mockAuthResult = mockk<AuthTabIntent.AuthResult>()
        every { mockAuthResult.resultCode } returns Int.MAX_VALUE

        val result = authTabManager.mapAuthResultToRawResult(mockAuthResult)

        assertNotNull(result)
        assertEquals(RawAuthorizationResult.ResultCode.NON_OAUTH_ERROR, result.resultCode)
        assertNotNull(result.exception)
        assertEquals("auth_tab_unknown_result", result.exception!!.errorCode)
    }

    // endregion

    // region launch / registerLauncher

    @Test(expected = IllegalStateException::class)
    fun test_launch_throwsIfLauncherNotRegistered() {
        authTabManager.launch(Uri.parse("https://login.microsoftonline.com/common/oauth2/v2.0/authorize"), "msauth")
    }

    @Test
    fun test_registerLauncher_setsLauncherField() {
        mockkStatic(AuthTabIntent::class)
        val mockLauncher = mockk<ActivityResultLauncher<Intent>>()
        val mockCaller = mockk<ActivityResultCaller>()
        every { AuthTabIntent.registerActivityResultLauncher(any(), any()) } returns mockLauncher

        val returned = authTabManager.registerLauncher(mockCaller) { /* no-op */ }

        // Verify method chaining returns the same instance
        assertEquals(authTabManager, returned)
        // Verify mLauncher is set: calling launch should not throw the "not registered" error.
        // It may throw something else because the mocked launcher has no behaviour set up,
        // but the guard condition (mLauncher == null) must be satisfied.
        try {
            authTabManager.launch(Uri.parse("https://login.microsoftonline.com"), "msauth")
        } catch (e: IllegalStateException) {
            assertFalse(
                "Expected launcher to be set, but launch() still reported it as unregistered: ${e.message}",
                e.message?.contains("not registered") == true
            )
        } catch (_: Exception) {
            // Any other exception is acceptable: the launcher is set, just the mock doesn't process calls.
        }
    }

    @Test
    fun test_registerLauncher_callbackInvoked_withMappedResult() {
        mockkStatic(AuthTabIntent::class)
        val mockCaller = mockk<ActivityResultCaller>()
        val callbackSlot = slot<ActivityResultCallback<AuthTabIntent.AuthResult>>()
        every { AuthTabIntent.registerActivityResultLauncher(any(), capture(callbackSlot)) } returns mockk()

        var receivedResult: RawAuthorizationResult? = null
        authTabManager.registerLauncher(mockCaller) { result -> receivedResult = result }

        assertTrue("Callback should have been captured during registerLauncher", callbackSlot.isCaptured)

        // Simulate a RESULT_CANCELED AuthTab result being delivered via the launcher callback
        val mockAuthResult = mockk<AuthTabIntent.AuthResult>()
        every { mockAuthResult.resultCode } returns AuthTabIntent.RESULT_CANCELED
        callbackSlot.captured.onActivityResult(mockAuthResult)

        assertNotNull(receivedResult)
        assertEquals(RawAuthorizationResult.ResultCode.CANCELLED, receivedResult!!.resultCode)
    }

    // endregion
}
