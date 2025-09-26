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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.AUTHORIZATION_AGENT
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.SWITCH_BROWSER
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.SwitchBrowserRequestHandler
import com.microsoft.identity.common.java.AuthenticationConstants.AAD.AUTHORIZATION
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.ui.AuthorizationAgent
import io.mockk.every
import io.mockk.mockkObject
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
    fun `test processSwitchBrowserResume with valid extras (stateRequired)`() {
        isStateRequired(true)
        // Mock parameters
        val mockSwitchBrowserRequestHandler = mock(SwitchBrowserRequestHandler::class.java)
        doNothing().`when`(mockSwitchBrowserRequestHandler).resetChallengeState()
        val code = "switch_browser_code"
        val actionUrl = "https://login.microsoft.com/switchbrowser/path"
        val state = "123"
        val extras = Bundle().apply {
            putString(SWITCH_BROWSER.CODE, code)
            putString(SWITCH_BROWSER.ACTION_URI, actionUrl)
            putString(SWITCH_BROWSER.STATE, state)
        }
        // Create an instance of SwitchBrowserProtocolCoordinator
        val coordinator = SwitchBrowserProtocolCoordinator(mockSwitchBrowserRequestHandler)

        // Call the method to be tested
        coordinator.processSwitchBrowserResume("https://auth.com?state=$state",extras) { uri, headers ->
            // Verify the resume URI
            val actionUri = Uri.parse(actionUrl)
            Assert.assertEquals(actionUri.scheme, uri.scheme)
            Assert.assertEquals(actionUri.host, uri.host)
            Assert.assertEquals(actionUri.path, uri.path)
            Assert.assertEquals("Bearer $code", headers[AUTHORIZATION])
        }
    }

    @Test
    fun `test processSwitchBrowserResume with valid extras (StateNotRequired)`() {
        isStateRequired(false)
        // Mock parameters
        val mockSwitchBrowserRequestHandler = mock(SwitchBrowserRequestHandler::class.java)
        doNothing().`when`(mockSwitchBrowserRequestHandler).resetChallengeState()
        val code = "switch_browser_code"
        val actionUrl = "https://login.microsoft.com/switchbrowser/path"
        val extras = Bundle().apply {
            putString(SWITCH_BROWSER.CODE, code)
            putString(SWITCH_BROWSER.ACTION_URI, actionUrl)
        }
        // Create an instance of SwitchBrowserProtocolCoordinator
        val coordinator = SwitchBrowserProtocolCoordinator(mockSwitchBrowserRequestHandler)

        // Call the method to be tested
        coordinator.processSwitchBrowserResume("https://auth.com",extras) { uri, headers ->
            // Verify the resume URI
            val actionUri = Uri.parse(actionUrl)
            Assert.assertEquals(actionUri.scheme, uri.scheme)
            Assert.assertEquals(actionUri.host, uri.host)
            Assert.assertEquals(actionUri.path, uri.path)
            Assert.assertEquals("Bearer $code", headers[AUTHORIZATION])
        }
    }

    @Test
    fun `test processSwitchBrowserResume with missing state (stateRequired)`() {
        isStateRequired(true)
        // Mock parameters
        val mockSwitchBrowserRequestHandler = mock(SwitchBrowserRequestHandler::class.java)
        val code = "switch_browser_code"
        val actionUrl = "login.microsoft.com/switchbrowser/path"
        val extras = Bundle().apply {
            putString(SWITCH_BROWSER.CODE, code)
            putString(SWITCH_BROWSER.ACTION_URI, actionUrl)
        }
        // Create an instance of SwitchBrowserProtocolCoordinator
        val coordinator = SwitchBrowserProtocolCoordinator(mockSwitchBrowserRequestHandler)

        val exception = Assert.assertThrows(ClientException::class.java) {
            // Call the method to be tested
            coordinator.processSwitchBrowserResume("",extras) { _, _ ->
                // This block should not be executed
                Assert.fail()
            }
        }
        Assert.assertEquals(ClientException.STATE_MISMATCH, exception.errorCode)
        Assert.assertEquals("State is null.", exception.message)
    }

    @Test
    fun `test processSwitchBrowserResume with missing extras`() {
        isStateRequired(false)
        // Mock parameters
        val mockSwitchBrowserRequestHandler = mock(SwitchBrowserRequestHandler::class.java)
        val extras = Bundle().apply {
            // Missing code
            // Missing ACTION_URI
        }
        // Create an instance of SwitchBrowserProtocolCoordinator
        val coordinator = SwitchBrowserProtocolCoordinator(mockSwitchBrowserRequestHandler)

        val exception = Assert.assertThrows(ClientException::class.java) {
            // Call the method to be tested
            coordinator.processSwitchBrowserResume("",extras) { _, _ ->
                // This block should not be executed
                Assert.fail()
            }
        }
        Assert.assertEquals(ClientException.MISSING_PARAMETER, exception.errorCode)
        Assert.assertEquals("Action URI is null/empty: true, code is null/empty: true.", exception.message)
    }

    @Test
    fun `test isExpectingSwitchBrowserResume with handler true`() {
        // Mock parameters
        val mockSwitchBrowserRequestHandler = mock(SwitchBrowserRequestHandler::class.java)
        `when`(mockSwitchBrowserRequestHandler.isSwitchBrowserChallengeActive).then { true }
        // Create an instance of SwitchBrowserProtocolCoordinator
        val coordinator = SwitchBrowserProtocolCoordinator(mockSwitchBrowserRequestHandler)

        // Call the method to be tested
        val result = coordinator.isExpectingSwitchBrowserResume()

        // Verify the result
        Assert.assertTrue(result)
    }

    @Test
    fun `test isExpectingSwitchBrowserResume with handler false`() {
        // Mock parameters
        val mockSwitchBrowserRequestHandler = mock(SwitchBrowserRequestHandler::class.java)
        `when`(mockSwitchBrowserRequestHandler.isSwitchBrowserChallengeActive).then { false }
        // Create an instance of SwitchBrowserProtocolCoordinator
        val coordinator = SwitchBrowserProtocolCoordinator(mockSwitchBrowserRequestHandler)

        // Call the method to be tested
        val result = coordinator.isExpectingSwitchBrowserResume()

        // Verify the result
        Assert.assertFalse(result)
    }

    @Test
    fun `test isSwitchBrowserResume for valid url`() {
        // Mock parameters
        val url = "${Broker.NEW_BROKER_REDIRECT_URI}/${SWITCH_BROWSER.RESUME_PATH}"
        val redirectUrl = Broker.NEW_BROKER_REDIRECT_URI

        // Call the method to be tested
        val result = SwitchBrowserProtocolCoordinator.isSwitchBrowserResume(url, redirectUrl)

        // Verify the result
        Assert.assertTrue(result)
    }

    @Test
    fun `test isSwitchBrowserResume for invalid url`() {
        // Mock parameters
        val url = "${Broker.NEW_BROKER_REDIRECT_URI}/invalid_path"
        val redirectUrl = Broker.NEW_BROKER_REDIRECT_URI

        // Call the method to be tested
        val result = SwitchBrowserProtocolCoordinator.isSwitchBrowserResume(url, redirectUrl)

        // Verify the result
        Assert.assertFalse(result)
    }

    @Test
    fun `test getIntentToResumeWebViewAuth`() {
        // Mock parameters
        val mockContext = mock(Context::class.java)
        val actionUri = "mock-action-uri"
        val code = "mock-code"
        val state = "mock-state"
        val intentDataString =
            "${Broker.NEW_BROKER_REDIRECT_URI}/${SWITCH_BROWSER.RESUME_PATH}?" +
                    "${SWITCH_BROWSER.ACTION_URI}=$actionUri&" +
                    "${SWITCH_BROWSER.CODE}=$code&" +
                    "${SWITCH_BROWSER.STATE}=$state"

        // Call the method to be tested
        val intent = SwitchBrowserProtocolCoordinator
            .getIntentToResumeWebViewAuth(mockContext, intentDataString)

        // Verify the result
        Assert.assertEquals(
            0,
            intent.flags
        )
        Assert.assertEquals(actionUri, intent.getStringExtra(SWITCH_BROWSER.ACTION_URI))
        Assert.assertEquals(code, intent.getStringExtra(SWITCH_BROWSER.CODE))
        Assert.assertEquals(state, intent.getStringExtra(SWITCH_BROWSER.STATE))
    }

    @Test
    fun `test processSwitchBrowserResume with null action URI`() {
        // Mock parameters
        val mockSwitchBrowserRequestHandler = mock(SwitchBrowserRequestHandler::class.java)
        val code = "switch_browser_code"
        val state = "123"
        val extras = Bundle().apply {
            putString(SWITCH_BROWSER.CODE, code)
            putString(SWITCH_BROWSER.ACTION_URI, null) // Null action URI
            putString(SWITCH_BROWSER.STATE, state)
        }
        // Create an instance of SwitchBrowserProtocolCoordinator
        val coordinator = SwitchBrowserProtocolCoordinator(mockSwitchBrowserRequestHandler)

        val exception = Assert.assertThrows(ClientException::class.java) {
            coordinator.processSwitchBrowserResume("https://auth.com?state=$state", extras) { _, _ ->
                // This block should not be executed
                Assert.fail("Should not reach success callback with null action URI")
            }
        }

        Assert.assertEquals(ClientException.MISSING_PARAMETER, exception.errorCode)
        Assert.assertTrue(exception.message!!.contains("Action URI is null/empty: true"))
    }

    @Test
    fun `test processSwitchBrowserResume with empty action URI`() {
        // Mock parameters
        val mockSwitchBrowserRequestHandler = mock(SwitchBrowserRequestHandler::class.java)
        val code = "switch_browser_code"
        val state = "123"
        val extras = Bundle().apply {
            putString(SWITCH_BROWSER.CODE, code)
            putString(SWITCH_BROWSER.ACTION_URI, "") // Empty action URI
            putString(SWITCH_BROWSER.STATE, state)
        }
        // Create an instance of SwitchBrowserProtocolCoordinator
        val coordinator = SwitchBrowserProtocolCoordinator(mockSwitchBrowserRequestHandler)

        val exception = Assert.assertThrows(ClientException::class.java) {
            coordinator.processSwitchBrowserResume("https://auth.com?state=$state", extras) { _, _ ->
                // This block should not be executed
                Assert.fail("Should not reach success callback with empty action URI")
            }
        }

        Assert.assertEquals(ClientException.MISSING_PARAMETER, exception.errorCode)
        Assert.assertTrue(exception.message!!.contains("Action URI is null/empty: true"))
    }

    @Test
    fun `test processSwitchBrowserResume with invalid action URI authority`() {
        isStateRequired(true)
        // Mock parameters
        val mockSwitchBrowserRequestHandler = mock(SwitchBrowserRequestHandler::class.java)
        val code = "switch_browser_code"
        val invalidActionUrl = "https://invalid.authority.com/switchbrowser/path" // Invalid AAD authority
        val state = "123"
        val extras = Bundle().apply {
            putString(SWITCH_BROWSER.CODE, code)
            putString(SWITCH_BROWSER.ACTION_URI, invalidActionUrl)
            putString(SWITCH_BROWSER.STATE, state)
        }
        // Create an instance of SwitchBrowserProtocolCoordinator
        val coordinator = SwitchBrowserProtocolCoordinator(mockSwitchBrowserRequestHandler)

        val exception = Assert.assertThrows(ClientException::class.java) {
            coordinator.processSwitchBrowserResume("https://auth.com?state=$state", extras) { _, _ ->
                // This block should not be executed
                Assert.fail("Should not reach success callback with invalid action URI authority")
            }
        }

        Assert.assertEquals(ClientException.UNKNOWN_AUTHORITY, exception.errorCode)
        Assert.assertTrue(exception.message!!.contains("Authority 'invalid.authority.com' is not a valid AAD authority"))
    }

    private fun isStateRequired(isStateRequired: Boolean) {
        mockkObject(SwitchBrowserUriHelper)
        every { SwitchBrowserUriHelper.STATE_VALIDATION_REQUIRED } returns isStateRequired
    }
}
