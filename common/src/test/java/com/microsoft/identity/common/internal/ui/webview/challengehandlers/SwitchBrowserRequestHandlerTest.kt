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
package com.microsoft.identity.common.internal.ui.webview.challengehandlers

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.microsoft.identity.common.internal.ui.webview.switchbrowser.SwitchBrowserUriHelper
import com.microsoft.identity.common.java.browser.Browser
import com.microsoft.identity.common.java.browser.IBrowserSelector
import com.microsoft.identity.common.java.exception.ClientException
import io.mockk.every
import io.mockk.mockkObject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito.`when`
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SwitchBrowserRequestHandlerTest {

    @Test
    fun `test processChallenge success (stateRequired)`() {
        isStateRequired(true)
        // Mock parameters
        val mockActivity = mock<Activity>()
        var activityExecuted = false
        doAnswer {
            activityExecuted = true
            null
        }.whenever(mockActivity).startActivity(any())
        val challenge = mock(SwitchBrowserChallenge::class.java)
        `when`(challenge.processUri).thenReturn(Uri.parse("https://login.microsoft.com?state=123"))
        `when`(challenge.authorizationUrl).thenReturn("https://auth.com?state=123")
        `when`(challenge.redirectUri).thenReturn("https://myapp.example.com/callback")
        val browserSelector = // Browser available
            IBrowserSelector { _, _ -> Browser("fakeBrowser", emptySet(), "browser", false) }
        val handler = SwitchBrowserRequestHandler(mockActivity, browserSelector, null)
        handler.processChallenge(challenge)
        Assert.assertTrue(activityExecuted)
    }

    @Test
    fun `test processChallenge success (StateNotRequired)`() {
        isStateRequired(false)
        // Mock parameters
        val mockActivity = mock<Activity>()
        var activityExecuted = false
        doAnswer {
            activityExecuted = true
            null
        }.whenever(mockActivity).startActivity(any())
        val challenge = mock(SwitchBrowserChallenge::class.java)
        `when`(challenge.processUri).thenReturn(Uri.parse("https://login.microsoft.com"))
        `when`(challenge.authorizationUrl).thenReturn("https://auth.com")
        `when`(challenge.redirectUri).thenReturn("https://myapp.example.com/callback")
        val browserSelector = // Browser available
            IBrowserSelector { _, _ -> Browser("fakeBrowser", emptySet(), "browser", false) }
        val handler = SwitchBrowserRequestHandler(mockActivity, browserSelector, null)
        handler.processChallenge(challenge)
        Assert.assertTrue(activityExecuted)
    }

    @Test
    fun `test processChallenge no browser available`() {
        // Mock parameters
        val activity = mock(Activity::class.java)
        doNothing().`when`(activity).startActivity(Intent())
        val challenge = mock(SwitchBrowserChallenge::class.java)
        `when`(challenge.processUri).thenReturn(Uri.parse("https://login.microsoft.com?state=123"))
        `when`(challenge.authorizationUrl).thenReturn("https://auth.com?state=123")
        `when`(challenge.redirectUri).thenReturn("https://myapp.example.com/callback")
        val browserSelector = IBrowserSelector { _, _ -> null } // No browser available
        val handler = SwitchBrowserRequestHandler(activity, browserSelector, null)
        val exception = Assert.assertThrows(ClientException::class.java) {
            handler.processChallenge(challenge)
        }
        Assert.assertEquals(ClientException.NO_BROWSERS_AVAILABLE, exception.errorCode)
        Assert.assertEquals("No browser found for SwitchBrowserChallenge.", exception.message)
    }

    @Test
    fun `test processChallenge states mismatch`() {
        isStateRequired(true)
        // Mock parameters
        val mockActivity = mock<Activity>()
        val challenge = mock(SwitchBrowserChallenge::class.java)
        `when`(challenge.processUri).thenReturn(Uri.parse("https://login.microsoft.com?state=123"))
        `when`(challenge.authorizationUrl).thenReturn("https://auth.com?state=456")
        `when`(challenge.redirectUri).thenReturn("https://myapp.example.com/callback")
        val browserSelector = // Browser available
            IBrowserSelector { _, _ -> Browser("fakeBrowser", emptySet(), "browser", false) }
        val handler = SwitchBrowserRequestHandler(mockActivity, browserSelector, null)
        val exception = Assert.assertThrows(ClientException::class.java) {
            handler.processChallenge(challenge)
        }
        Assert.assertEquals(ClientException.STATE_MISMATCH, exception.errorCode)
        Assert.assertEquals("State does not match with the auth request state.", exception.message)
    }

    private fun isStateRequired(isStateRequired: Boolean) {
        mockkObject(SwitchBrowserUriHelper)
        every { SwitchBrowserUriHelper.STATE_VALIDATION_REQUIRED } returns isStateRequired
    }
}
