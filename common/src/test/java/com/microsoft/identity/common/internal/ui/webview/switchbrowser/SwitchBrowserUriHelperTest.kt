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
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
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
    fun `test isSwitchBrowserRedirectUrl accepts legit url with query params`() {
        // Query params (code/action_uri) are ignored — only scheme + authority + path are matched.
        val url = "${Broker.NEW_BROKER_REDIRECT_URI}/${SWITCH_BROWSER.REQUEST_PATH}" +
                "?${SWITCH_BROWSER.CODE}=$CODE&${SWITCH_BROWSER.ACTION_URI}=$ACTION_URI"
        Assert.assertTrue(
            SwitchBrowserUriHelper.isSwitchBrowserRedirectUrl(
                url, Broker.NEW_BROKER_REDIRECT_URI, SWITCH_BROWSER.REQUEST_PATH
            )
        )
    }

    @Test
    fun `test isSwitchBrowserRedirectUrl rejects suffixed host spoof`() {
        // Prefix-confusion vector: startsWith("{redirect}/switch_browser") accepts this, but the
        // structured match must reject the attacker-controlled path suffix.
        val url = "${Broker.NEW_BROKER_REDIRECT_URI}/${SWITCH_BROWSER.REQUEST_PATH}.evil.com/x" +
                "?${SWITCH_BROWSER.CODE}=STOLEN&${SWITCH_BROWSER.ACTION_URI}=$ACTION_URI"
        Assert.assertFalse(
            SwitchBrowserUriHelper.isSwitchBrowserRedirectUrl(
                url, Broker.NEW_BROKER_REDIRECT_URI, SWITCH_BROWSER.REQUEST_PATH
            )
        )
    }

    @Test
    fun `test isSwitchBrowserRedirectUrl rejects path suffix spoof`() {
        val url = "${Broker.NEW_BROKER_REDIRECT_URI}/${SWITCH_BROWSER.REQUEST_PATH}stolen" +
                "?${SWITCH_BROWSER.CODE}=STOLEN"
        Assert.assertFalse(
            SwitchBrowserUriHelper.isSwitchBrowserRedirectUrl(
                url, Broker.NEW_BROKER_REDIRECT_URI, SWITCH_BROWSER.REQUEST_PATH
            )
        )
    }

    @Test
    fun `test isSwitchBrowserRedirectUrl killSwitch reverts to prefix match`() {
        mockkObject(CommonFlightsManager)
        every {
            CommonFlightsManager.getFlightsProvider()
                .isFlightEnabled(CommonFlight.ENABLE_STRICT_REDIRECT_URI_MATCHING)
        } returns false
        // With strict matching disabled, the historical prefix match accepts the suffixed URL again.
        val url = "${Broker.NEW_BROKER_REDIRECT_URI}/${SWITCH_BROWSER.REQUEST_PATH}.evil.com/x" +
                "?${SWITCH_BROWSER.CODE}=STOLEN"
        Assert.assertTrue(
            SwitchBrowserUriHelper.isSwitchBrowserRedirectUrl(
                url, Broker.NEW_BROKER_REDIRECT_URI, SWITCH_BROWSER.REQUEST_PATH
            )
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
        isStateRequired(true)
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

    // region buildResumeBrowserUri

    @Test
    fun `test buildResumeBrowserUri appends RESUME_PATH to msauth redirect`() {
        val redirectUri = "msauth://com.microsoft.identity.client"
        val resumeUri = SwitchBrowserUriHelper.buildResumeBrowserUri(redirectUri)

        Assert.assertEquals(
            "msauth://com.microsoft.identity.client/${SWITCH_BROWSER.RESUME_PATH}",
            resumeUri.toString()
        )
        Assert.assertEquals("msauth", resumeUri.scheme)
        Assert.assertEquals("com.microsoft.identity.client", resumeUri.authority)
        Assert.assertEquals("/${SWITCH_BROWSER.RESUME_PATH}", resumeUri.path)
    }

    @Test
    fun `test buildResumeBrowserUri appends RESUME_PATH to https redirect with paths`() {
        val redirectUri = "https://login.microsoftonline.com/androidbroker/com.microsoft.identity.testuserapp"
        val resumeUri = SwitchBrowserUriHelper.buildResumeBrowserUri(redirectUri)

        Assert.assertEquals(
            "https://login.microsoftonline.com/androidbroker/com.microsoft.identity.testuserapp/${SWITCH_BROWSER.RESUME_PATH}",
            resumeUri.toString()
        )
        Assert.assertEquals("https", resumeUri.scheme)
        Assert.assertEquals("login.microsoftonline.com", resumeUri.authority)
    }

    @Test
    fun `test buildResumeBrowserUri uses NEW_BROKER_REDIRECT_URI`() {
        val resumeUri = SwitchBrowserUriHelper.buildResumeBrowserUri(Broker.NEW_BROKER_REDIRECT_URI)

        Assert.assertEquals(
            "${Broker.NEW_BROKER_REDIRECT_URI}/${SWITCH_BROWSER.RESUME_PATH}",
            resumeUri.toString()
        )
    }

    // endregion

    // region extractBaseRedirectUri

    @Test
    fun `test extractBaseRedirectUri removes last path segment from msauth uri`() {
        val uri = Uri.parse("msauth://com.microsoft.identity.client/switch_browser?code=code&action_uri=action-uri")
        val result = SwitchBrowserUriHelper.extractBaseRedirectUri(uri)

        Assert.assertEquals("msauth://com.microsoft.identity.client", result)
    }

    @Test
    fun `test extractBaseRedirectUri removes last path segment from https uri with multiple paths`() {
        val uri = Uri.parse(
            "https://login.microsoftonline.com/androidbroker/com.microsoft.identity.testuserapp/switch_browser?action=1"
        )
        val result = SwitchBrowserUriHelper.extractBaseRedirectUri(uri)

        Assert.assertEquals(
            "https://login.microsoftonline.com/androidbroker/com.microsoft.identity.testuserapp",
            result
        )
    }

    @Test
    fun `test extractBaseRedirectUri removes single path segment returning scheme and authority only`() {
        val uri = Uri.parse("msauth://Microsoft.AAD.BrokerPlugin/switch_browser?action=1")
        val result = SwitchBrowserUriHelper.extractBaseRedirectUri(uri)

        Assert.assertEquals("msauth://Microsoft.AAD.BrokerPlugin", result)
    }

    @Test
    fun `test extractBaseRedirectUri returns scheme and authority when no path`() {
        val uri = Uri.parse("msauth://com.microsoft.identity.client")
        val result = SwitchBrowserUriHelper.extractBaseRedirectUri(uri)

        Assert.assertEquals("msauth://com.microsoft.identity.client", result)
    }

    @Test
    fun `test extractBaseRedirectUri returns scheme and authority when path is only a slash`() {
        val uri = Uri.parse("https://login.microsoftonline.com/")
        val result = SwitchBrowserUriHelper.extractBaseRedirectUri(uri)

        Assert.assertEquals("https://login.microsoftonline.com", result)
    }

    @Test
    fun `test extractBaseRedirectUri preserves multiple intermediate path segments`() {
        val uri = Uri.parse("https://example.com/a/b/c/d/last_segment?x=1")
        val result = SwitchBrowserUriHelper.extractBaseRedirectUri(uri)

        Assert.assertEquals("https://example.com/a/b/c/d", result)
    }

    @Test
    fun `test extractBaseRedirectUri throws when scheme is missing`() {
        val uri = Uri.parse("//com.microsoft.identity.client/switch_browser")

        val exception = Assert.assertThrows(ClientException::class.java) {
            SwitchBrowserUriHelper.extractBaseRedirectUri(uri)
        }
        Assert.assertEquals(ClientException.MALFORMED_URL, exception.errorCode)
    }

    @Test
    fun `test extractBaseRedirectUri throws when authority is missing`() {
        val uri = Uri.parse("msauth:/switch_browser")

        val exception = Assert.assertThrows(ClientException::class.java) {
            SwitchBrowserUriHelper.extractBaseRedirectUri(uri)
        }
        Assert.assertEquals(ClientException.MALFORMED_URL, exception.errorCode)
    }

    @Test
    fun `test extractBaseRedirectUri then buildResumeBrowserUri produces valid resume uri`() {
        val originalUri = Uri.parse(
            "msauth://com.microsoft.identity.client/switch_browser?code=code&action_uri=action-uri"
        )
        val baseRedirect = SwitchBrowserUriHelper.extractBaseRedirectUri(originalUri)
        val resumeUri = SwitchBrowserUriHelper.buildResumeBrowserUri(baseRedirect)

        Assert.assertEquals(
            "msauth://com.microsoft.identity.client/${SWITCH_BROWSER.RESUME_PATH}",
            resumeUri.toString()
        )
    }

    // endregion

    private fun isStateRequired(isStateRequired: Boolean) {
        mockkObject(SwitchBrowserUriHelper)
        every { SwitchBrowserUriHelper.STATE_VALIDATION_REQUIRED } returns isStateRequired
    }

    @After
    fun tearDown() {
        // Clean up mocks after each test
        unmockkAll()
    }
}
