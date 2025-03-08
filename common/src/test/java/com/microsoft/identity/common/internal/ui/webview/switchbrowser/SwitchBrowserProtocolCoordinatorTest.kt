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
package com.microsoft.identity.common.internal.ui.webview.switchbrowser

import android.os.Bundle
import com.microsoft.identity.common.adal.internal.AuthenticationConstants
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.CLIENT_ID
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.REDIRECT_URI
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.SwitchBrowserRequestHandler
import com.microsoft.identity.common.java.AuthenticationConstants.AAD.AUTHORIZATION
import com.microsoft.identity.common.java.exception.ClientException
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SwitchBrowserProtocolCoordinatorTest {

    @Test
    fun `test processSwitchBrowserResume with valid extras`() {
        // Mock parameters
        val mockSwitchBrowserRequestHandler = mock(SwitchBrowserRequestHandler::class.java)
        doNothing().`when`(mockSwitchBrowserRequestHandler).resetChallengeState()
        val redirectUrl = "AAD://example.com/redirect"
        val clientId = "client_id"
        val code = "switch_browser_code"
        val actionUrl = "test.example.com/switchbrowser/path"
        val extras = Bundle().apply {
            putString(AuthenticationConstants.SWITCH_BROWSER.CODE, code)
            putString(AuthenticationConstants.SWITCH_BROWSER.ACTION_URI, actionUrl)
        }
        // Create an instance of SwitchBrowserProtocolCoordinator
        val coordinator = SwitchBrowserProtocolCoordinator(mockSwitchBrowserRequestHandler)

        // Call the method to be tested
        coordinator.processSwitchBrowserResume(extras, redirectUrl, clientId) { uri, headers ->
            // Verify the resume URI
            Assert.assertEquals(clientId, uri.getQueryParameter(CLIENT_ID))
            Assert.assertEquals(redirectUrl, uri.getQueryParameter(REDIRECT_URI))
            Assert.assertEquals(actionUrl, uri.host + uri.path)
            Assert.assertEquals(code, headers[AUTHORIZATION])
        }
    }

    @Test
    fun `test processSwitchBrowserResume with missing extras`() {
        // Mock parameters
        val mockSwitchBrowserRequestHandler = mock(SwitchBrowserRequestHandler::class.java)
        val redirectUrl = "AAD://example.com/redirect"
        val clientId = "client_id"
        val extras = Bundle().apply {
            // Missing code
            // Missing ACTION_URI
        }
        // Create an instance of SwitchBrowserProtocolCoordinator
        val coordinator = SwitchBrowserProtocolCoordinator(mockSwitchBrowserRequestHandler)

        val exception = Assert.assertThrows(ClientException::class.java) {
            // Call the method to be tested
            coordinator.processSwitchBrowserResume(extras, redirectUrl, clientId) { _, _ ->
                // This block should not be executed
                Assert.fail()
            }
        }
        Assert.assertEquals(ClientException.MISSING_PARAMETER, exception.errorCode)
        Assert.assertEquals("Action URI is null/empty: true, code is null/empty: true", exception.message)
    }

    @Test
    fun `test isSwitchBrowserResume with extras and handler true`() {
        // Mock parameters
        val mockSwitchBrowserRequestHandler = mock(SwitchBrowserRequestHandler::class.java)
        `when`(mockSwitchBrowserRequestHandler.isChallengeHandled).then { true }
        // Create an instance of SwitchBrowserProtocolCoordinator
        val coordinator = SwitchBrowserProtocolCoordinator(mockSwitchBrowserRequestHandler)

        // Call the method to be tested
        val result = coordinator.isExpectingSwitchBrowserResume()

        // Verify the result
        Assert.assertTrue(result)
    }

    @Test
    fun `test isSwitchBrowserResume with extras handler false`() {
        // Mock parameters
        val mockSwitchBrowserRequestHandler = mock(SwitchBrowserRequestHandler::class.java)
        `when`(mockSwitchBrowserRequestHandler.isChallengeHandled).then { false }
        // Create an instance of SwitchBrowserProtocolCoordinator
        val coordinator = SwitchBrowserProtocolCoordinator(mockSwitchBrowserRequestHandler)

        // Call the method to be tested
        val result = coordinator.isExpectingSwitchBrowserResume()

        // Verify the result
        Assert.assertFalse(result)
    }
}
