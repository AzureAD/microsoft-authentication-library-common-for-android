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

import android.net.Uri
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.SWITCH_BROWSER
import com.microsoft.identity.common.java.exception.ClientException
import io.mockk.every
import io.mockk.mockkObject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SwitchBrowserUriHelperTest {

    companion object {
        private const val CODE = "your-switch-browser-code"
        private const val ACTION_URI = "https://login.microsoftonline.com/switchbrowser/process"
        private const val STATE = "123"
    }

    @Test
    fun `test constructFromRedirectUri with valid redirect uri (StateRequired)`() {
        isStateRequired(true)
        val redirectString = "${Broker.NEW_BROKER_REDIRECT_URI}?" +
                "${SWITCH_BROWSER.CODE}=$CODE&" +
                "${SWITCH_BROWSER.ACTION_URI}=$ACTION_URI&" +
                "${SWITCH_BROWSER.STATE}=$STATE"
        val redirectUri = Uri.parse(redirectString)

        val switchBrowserProcessUri = SwitchBrowserUriHelper.buildProcessUri(redirectUri)
        Assert.assertNotNull(switchBrowserProcessUri)
        Assert.assertEquals(
            CODE,
            switchBrowserProcessUri.getQueryParameter(SWITCH_BROWSER.CODE)
        )
        val actionUri = Uri.parse(ACTION_URI)
        Assert.assertEquals(actionUri.scheme, switchBrowserProcessUri.scheme)
        Assert.assertEquals(actionUri.host, switchBrowserProcessUri.host)
        Assert.assertEquals(actionUri.path, switchBrowserProcessUri.path)
        Assert.assertEquals(
            STATE,
            switchBrowserProcessUri.getQueryParameter(SWITCH_BROWSER.STATE)
        )
    }

    @Test
    fun `test constructFromRedirectUri with valid redirect uri (StateNotRequired)`() {
        isStateRequired(false)
        val redirectString = "${Broker.NEW_BROKER_REDIRECT_URI}?" +
                "${SWITCH_BROWSER.CODE}=$CODE&" +
                "${SWITCH_BROWSER.ACTION_URI}=$ACTION_URI"
        val redirectUri = Uri.parse(redirectString)

        val switchBrowserProcessUri = SwitchBrowserUriHelper.buildProcessUri(redirectUri)
        Assert.assertNotNull(switchBrowserProcessUri)
        Assert.assertEquals(
            CODE,
            switchBrowserProcessUri.getQueryParameter(SWITCH_BROWSER.CODE)
        )
        val actionUri = Uri.parse(ACTION_URI)
        Assert.assertEquals(actionUri.scheme, switchBrowserProcessUri.scheme)
        Assert.assertEquals(actionUri.host, switchBrowserProcessUri.host)
        Assert.assertEquals(actionUri.path, switchBrowserProcessUri.path)
    }

    @Test
    fun `test buildProcessUri with missing code`() {
        val redirectString = "${Broker.NEW_BROKER_REDIRECT_URI}?" +
                "${SWITCH_BROWSER.ACTION_URI}=$ACTION_URI"
        val redirectUri = Uri.parse(redirectString)

        val exception = Assert.assertThrows(ClientException::class.java) {
            SwitchBrowserUriHelper.buildProcessUri(redirectUri)
        }
        Assert.assertEquals(ClientException.MALFORMED_URL, exception.errorCode)
        Assert.assertEquals("switch browser code is null or empty", exception.message)
    }

    @Test
    fun `test buildProcessUri with missing sate (StateRequired)`() {
        isStateRequired(true)
        val redirectString = "${Broker.NEW_BROKER_REDIRECT_URI}?" +
                "${SWITCH_BROWSER.CODE}=$CODE&" +
                "${SWITCH_BROWSER.ACTION_URI}=$ACTION_URI"
        val redirectUri = Uri.parse(redirectString)

        val exception = Assert.assertThrows(ClientException::class.java) {
            SwitchBrowserUriHelper.buildProcessUri(redirectUri)
        }
        Assert.assertEquals(ClientException.MISSING_PARAMETER, exception.errorCode)
        Assert.assertEquals("State is null or empty", exception.message)
    }

    @Test
    fun `test isSwitchBrowserRedirectUrl incorrect url`() {
        val url = "https://login.microsoftonline.com/"
        val redirectUrl = Broker.NEW_BROKER_REDIRECT_URI
        val path = "path"
        Assert.assertFalse(
            SwitchBrowserUriHelper.isSwitchBrowserRedirectUrl(url, redirectUrl, path)
        )
    }

    @Test
    fun `test isSwitchBrowserRedirectUrl correct url`() {
        val url = "${Broker.NEW_BROKER_REDIRECT_URI}/path"
        val redirectUrl = Broker.NEW_BROKER_REDIRECT_URI
        val path = "path"
        Assert.assertTrue(
            SwitchBrowserUriHelper.isSwitchBrowserRedirectUrl(url, redirectUrl, path)
        )
    }

    @Test
    fun `test buildResumeUri valid params (stateNotRequired)`() {
        isStateRequired(false)
        val uri = SwitchBrowserUriHelper.buildResumeUri(
            ACTION_URI, null
        )
        Assert.assertNotNull(uri)
        val actionUri = Uri.parse(ACTION_URI)
        Assert.assertEquals(actionUri.scheme, uri.scheme)
        Assert.assertEquals(actionUri.host, uri.host)
        Assert.assertEquals(actionUri.path, uri.path)
        Assert.assertNull(uri.getQueryParameter(SWITCH_BROWSER.STATE))
    }

    @Test
    fun `test buildResumeUri valid params (stateRequired)`() {
        val uri = SwitchBrowserUriHelper.buildResumeUri(
            ACTION_URI, STATE
        )
        Assert.assertNotNull(uri)
        val actionUri = Uri.parse(ACTION_URI)
        Assert.assertEquals(actionUri.scheme, uri.scheme)
        Assert.assertEquals(actionUri.host, uri.host)
        Assert.assertEquals(actionUri.path, uri.path)
        Assert.assertEquals(
            STATE,
            uri.getQueryParameter(SWITCH_BROWSER.STATE)
        )
    }

    @Test
    fun `test buildResumeUri missing state (stateRequired)`() {
        isStateRequired(true)
        val exception = Assert.assertThrows(ClientException::class.java) {
            SwitchBrowserUriHelper.buildResumeUri(ACTION_URI, null)
        }
        Assert.assertEquals(ClientException.MISSING_PARAMETER, exception.errorCode)
        Assert.assertEquals("State is null or empty", exception.message)
    }

    @Test
    fun `test states match (statesNotRequired)`() {
        isStateRequired(false)
        SwitchBrowserUriHelper.statesMatch(
            "", null
        )
    }

    @Test
    fun `test states match`() {
        isStateRequired(true)
        SwitchBrowserUriHelper.statesMatch(
            "https://example.auth.com/path?${SWITCH_BROWSER.STATE}=$STATE", STATE
        )
    }

    @Test
    fun `test states don't match`() {
        isStateRequired(true)
        val exception = Assert.assertThrows(
            ClientException::class.java) {
            SwitchBrowserUriHelper.statesMatch(
                "https://example.auth.com/path?${SWITCH_BROWSER.STATE}=$STATE", "error"
            )
        }
        Assert.assertEquals(
            ClientException.STATE_MISMATCH,
            exception.errorCode
        )
        Assert.assertEquals(
            "State does not match with the auth request state.",
            exception.message
        )
    }

    private fun isStateRequired(isStateRequired: Boolean) {
        mockkObject(SwitchBrowserUriHelper)
        every { SwitchBrowserUriHelper.STATE_VALIDATION_REQUIRED } returns isStateRequired
    }
}
