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

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.SWITCH_BROWSER
import com.microsoft.identity.common.java.AuthenticationConstants.AAD.AUTHORIZATION
import com.microsoft.identity.common.java.browser.Browser
import com.microsoft.identity.common.java.browser.IBrowserSelector
import com.microsoft.identity.common.java.exception.ClientException
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doAnswer
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [SwitchBrowserProtocolCoordinator] covering both the outbound challenge side
 * ([SwitchBrowserProtocolCoordinator.processSwitchBrowserRedirectAsync]) and the inbound resume side
 * ([SwitchBrowserProtocolCoordinator.processSwitchBrowserResume] / `processSwitchBrowserResumeAsync`).
 *
 * Tests that stub [SwitchBrowserUriHelper.buildProcessUri] avoid the real AAD cloud-discovery
 * HTTPS call. The "real validation" tests deliberately do not stub it, so they exercise the
 * full validation path against a hand-crafted redirect URL.
 */
@RunWith(RobolectricTestRunner::class)
class SwitchBrowserProtocolCoordinatorTest {

    private companion object {
        private const val REDIRECT_URL =
            "msauth://com.microsoft.identity.client/switch_browser" +
                "?code=abc123&action_uri=https%3A%2F%2Flogin.microsoft.com%2Fpath"
        private const val AUTH_URL = "https://auth.com?state=123"
        private const val BASE_REDIRECT = "msauth://com.microsoft.identity.client"
    }

    /**
     * Per-test supervisor + scope. The handler's async methods are fire-and-forget (void),
     * so tests can't `.join()` a returned Job. Instead, [awaitWork] waits on all children
     * launched on this scope, giving us deterministic synchronisation.
     *
     * Uses [Dispatchers.Unconfined] so we don't need [Dispatchers.Main] installed (no
     * kotlinx-coroutines-test dep). After the inner `withContext(Dispatchers.IO)` returns,
     * the coroutine resumes inline on the IO completion thread; that's fine for tests since
     * the work we assert on (mocked Activity, span bookkeeping) is not main-thread-restricted.
     */
    private val supervisorJob: CompletableJob = SupervisorJob()
    private val testAsyncScope = CoroutineScope(Dispatchers.Unconfined + supervisorJob)

    /** Records the status the coordinator reports so tests can assert on it. */
    private class RecordingStatusListener : SwitchBrowserStatusCallback {
        var startedCount = 0
        var resumedCount = 0
        var completedUri: Uri? = null
        var completedHeaders: HashMap<String, String>? = null
        var failure: Throwable? = null

        override fun onSwitchBrowserStarted() { startedCount++ }
        override fun onSwitchBrowserResumed() { resumedCount++ }
        override fun onSwitchBrowserCompleted(uri: Uri, headers: HashMap<String, String>) {
            completedUri = uri
            completedHeaders = headers
        }
        override fun onSwitchBrowserFailed(error: Throwable) { failure = error }
    }

    private val statusListener = RecordingStatusListener()

    private fun newHandler(
        mockActivity: Activity = mock<Activity>(),
        browserSelector: IBrowserSelector = IBrowserSelector { _, _ ->
            Browser("fakeBrowser", emptySet(), "browser", false)
        },
        listener: SwitchBrowserStatusCallback = statusListener
    ): SwitchBrowserProtocolCoordinator {
        return SwitchBrowserProtocolCoordinator.forTesting(
            mockActivity,
            browserSelector,
            listener,
            /* spanContext = */ null,
            testAsyncScope
        )
    }

    private suspend fun awaitWork() {
        supervisorJob.children.toList().joinAll()
    }

    private fun isStateRequired(isStateRequired: Boolean) {
        mockkObject(SwitchBrowserUriHelper)
        every { SwitchBrowserUriHelper.STATE_VALIDATION_REQUIRED } returns isStateRequired
    }

    @After
    fun tearDown() {
        // mockkObject installs persistent stubs on the singleton SwitchBrowserUriHelper that
        // would otherwise leak into subsequent tests (e.g. a happy-path test would stub
        // buildProcessUri, and a later "real validation" test would receive the stub instead
        // of calling the actual implementation).
        unmockkAll()
    }

    // region processSwitchBrowserRedirectAsync — happy paths

    @Test
    fun `processSwitchBrowserRedirectAsync starts SwitchBrowserActivity (stateRequired)`() {
        isStateRequired(true)
        every { SwitchBrowserUriHelper.buildProcessUri(any()) } returns
            Uri.parse("https://login.microsoft.com/processuri?state=123")

        val mockActivity = mock<Activity>()
        var activityExecuted = false
        doAnswer {
            activityExecuted = true
            null
        }.whenever(mockActivity).startActivity(any())
        val handler = newHandler(mockActivity)

        handler.processSwitchBrowserRedirectAsync(
            switchBrowserRedirectUrl = REDIRECT_URL,
            authorizationUrl = AUTH_URL,
            baseRedirectUri = BASE_REDIRECT
        )
        runBlocking { awaitWork() }

        Assert.assertNull("onSwitchBrowserFailed must not fire on the happy path", statusListener.failure)
        Assert.assertEquals(1, statusListener.startedCount)
        Assert.assertTrue("startActivity must be invoked on success", activityExecuted)
        Assert.assertTrue(handler.wasSwitchBrowserFlowInitiated)
        Assert.assertTrue(handler.isSwitchBrowserChallengeActive)
    }

    @Test
    fun `processSwitchBrowserRedirectAsync starts SwitchBrowserActivity (StateNotRequired)`() {
        isStateRequired(false)
        every { SwitchBrowserUriHelper.buildProcessUri(any()) } returns
            Uri.parse("https://login.microsoft.com")

        val mockActivity = mock<Activity>()
        var activityExecuted = false
        doAnswer {
            activityExecuted = true
            null
        }.whenever(mockActivity).startActivity(any())
        val handler = newHandler(mockActivity)

        handler.processSwitchBrowserRedirectAsync(
            switchBrowserRedirectUrl = REDIRECT_URL,
            authorizationUrl = "https://auth.com",
            baseRedirectUri = BASE_REDIRECT
        )
        runBlocking { awaitWork() }

        Assert.assertTrue(activityExecuted)
        Assert.assertTrue(handler.wasSwitchBrowserFlowInitiated)
    }

    // endregion

    // region processSwitchBrowserRedirectAsync — error routing

    @Test
    fun `processSwitchBrowserRedirectAsync with no available browser routes NO_BROWSERS_AVAILABLE to onError`() {
        isStateRequired(true)
        every { SwitchBrowserUriHelper.buildProcessUri(any()) } returns
            Uri.parse("https://login.microsoft.com/processuri?state=123")

        val handler = newHandler(
            browserSelector = IBrowserSelector { _, _ -> null } // No browser available.
        )

        handler.processSwitchBrowserRedirectAsync(
            switchBrowserRedirectUrl = REDIRECT_URL,
            authorizationUrl = AUTH_URL,
            baseRedirectUri = BASE_REDIRECT
        )
        runBlocking { awaitWork() }

        Assert.assertTrue(statusListener.failure is ClientException)
        Assert.assertEquals(
            ClientException.NO_BROWSERS_AVAILABLE,
            (statusListener.failure as ClientException).errorCode
        )
        Assert.assertFalse(handler.wasSwitchBrowserFlowInitiated)
    }

    @Test
    fun `processSwitchBrowserRedirectAsync with state mismatch routes STATE_MISMATCH to onError`() {
        isStateRequired(true)
        every { SwitchBrowserUriHelper.buildProcessUri(any()) } returns
            Uri.parse("https://login.microsoft.com/processuri?state=123")

        val handler = newHandler()

        handler.processSwitchBrowserRedirectAsync(
            switchBrowserRedirectUrl = REDIRECT_URL,
            // authorizationUrl carries state=456 — does not match processUri's state=123.
            authorizationUrl = "https://auth.com?state=456",
            baseRedirectUri = BASE_REDIRECT
        )
        runBlocking { awaitWork() }

        Assert.assertTrue(statusListener.failure is ClientException)
        Assert.assertEquals(
            ClientException.STATE_MISMATCH,
            (statusListener.failure as ClientException).errorCode
        )
        Assert.assertFalse(handler.wasSwitchBrowserFlowInitiated)
    }

    @Test
    fun `processSwitchBrowserRedirectAsync with invalid action URI authority routes UNKNOWN_AUTHORITY to onError`() {
        // No mock on buildProcessUri — let the real validation path run against an unknown
        // AAD authority. The handler must surface the error on onError rather than throw on
        // the WebView thread.
        val redirectUrl = "msauth://com.microsoft.identity.client/switch_browser" +
            "?code=abc123&action_uri=https%3A%2F%2Finvalid.authority.com%2Fpath"
        val handler = newHandler()

        handler.processSwitchBrowserRedirectAsync(
            switchBrowserRedirectUrl = redirectUrl,
            authorizationUrl = "https://auth.com",
            baseRedirectUri = BASE_REDIRECT
        )
        runBlocking { awaitWork() }

        Assert.assertTrue(
            "Expected ClientException, got ${statusListener.failure?.javaClass?.name}",
            statusListener.failure is ClientException
        )
        Assert.assertEquals(
            ClientException.UNKNOWN_AUTHORITY,
            (statusListener.failure as ClientException).errorCode
        )
        Assert.assertFalse(handler.wasSwitchBrowserFlowInitiated)
    }

    @Test
    fun `processSwitchBrowserRedirectAsync with malformed redirect URL routes MALFORMED_URL to onError`() {
        // No action_uri parameter — buildProcessUri throws MALFORMED_URL.
        val redirectUrl = "msauth://com.microsoft.identity.client/switch_browser?code=abc123"
        val handler = newHandler()

        handler.processSwitchBrowserRedirectAsync(
            switchBrowserRedirectUrl = redirectUrl,
            authorizationUrl = "https://auth.com",
            baseRedirectUri = BASE_REDIRECT
        )
        runBlocking { awaitWork() }

        Assert.assertTrue(statusListener.failure is ClientException)
        Assert.assertEquals(
            ClientException.MALFORMED_URL,
            (statusListener.failure as ClientException).errorCode
        )
    }

    // endregion

    // region processSwitchBrowserRedirectAsync — state lifecycle

    @Test
    fun `wasSwitchBrowserFlowInitiated survives resetChallengeState`() {
        isStateRequired(true)
        every { SwitchBrowserUriHelper.buildProcessUri(any()) } returns
            Uri.parse("https://login.microsoft.com/processuri?state=123")

        val mockActivity = mock<Activity>()
        doAnswer { null }.whenever(mockActivity).startActivity(any())
        val handler = newHandler(mockActivity)

        handler.processSwitchBrowserRedirectAsync(
            switchBrowserRedirectUrl = REDIRECT_URL,
            authorizationUrl = AUTH_URL,
            baseRedirectUri = BASE_REDIRECT
        )
        runBlocking { awaitWork() }

        Assert.assertTrue(handler.wasSwitchBrowserFlowInitiated)
        handler.resetChallengeState()
        Assert.assertFalse(handler.isSwitchBrowserChallengeActive)
        Assert.assertTrue(handler.wasSwitchBrowserFlowInitiated)
    }

    // endregion

    // region processSwitchBrowserResume (synchronous)

    @Test
    fun `test processSwitchBrowserResume with valid extras (stateRequired)`() {
        isStateRequired(true)
        val code = "switch_browser_code"
        val actionUrl = "https://login.microsoft.com/switchbrowser/path"
        val state = "123"
        val extras = Bundle().apply {
            putString(SWITCH_BROWSER.CODE, code)
            putString(SWITCH_BROWSER.ACTION_URI, actionUrl)
            putString(SWITCH_BROWSER.STATE, state)
        }
        val handler = newHandler()
        // Simulate a prior challenge that left the flow active; verify resume clears it.
        handler.isSwitchBrowserChallengeActive = true

        val (uri, headers) = handler.processSwitchBrowserResume("https://auth.com?state=$state", extras)
        val actionUri = Uri.parse(actionUrl)
        Assert.assertEquals(actionUri.scheme, uri.scheme)
        Assert.assertEquals(actionUri.host, uri.host)
        Assert.assertEquals(actionUri.path, uri.path)
        Assert.assertEquals("Bearer $code", headers[AUTHORIZATION])
        Assert.assertFalse(
            "Challenge state must be reset after a successful resume",
            handler.isSwitchBrowserChallengeActive
        )
    }

    @Test
    fun `test processSwitchBrowserResume with valid extras (StateNotRequired)`() {
        isStateRequired(false)
        val code = "switch_browser_code"
        val actionUrl = "https://login.microsoft.com/switchbrowser/path"
        val extras = Bundle().apply {
            putString(SWITCH_BROWSER.CODE, code)
            putString(SWITCH_BROWSER.ACTION_URI, actionUrl)
        }
        val handler = newHandler()

        val (uri, headers) = handler.processSwitchBrowserResume("https://auth.com", extras)
        val actionUri = Uri.parse(actionUrl)
        Assert.assertEquals(actionUri.scheme, uri.scheme)
        Assert.assertEquals(actionUri.host, uri.host)
        Assert.assertEquals(actionUri.path, uri.path)
        Assert.assertEquals("Bearer $code", headers[AUTHORIZATION])
    }

    @Test
    fun `test processSwitchBrowserResume with missing state (stateRequired)`() {
        isStateRequired(true)
        val code = "switch_browser_code"
        val actionUrl = "login.microsoft.com/switchbrowser/path"
        val extras = Bundle().apply {
            putString(SWITCH_BROWSER.CODE, code)
            putString(SWITCH_BROWSER.ACTION_URI, actionUrl)
        }
        val handler = newHandler()

        val exception = Assert.assertThrows(ClientException::class.java) {
            handler.processSwitchBrowserResume("", extras)
        }
        Assert.assertEquals(ClientException.STATE_MISMATCH, exception.errorCode)
        Assert.assertEquals("State is null.", exception.message)
    }

    @Test
    fun `test processSwitchBrowserResume with missing extras`() {
        isStateRequired(false)
        val extras = Bundle()
        val handler = newHandler()

        val exception = Assert.assertThrows(ClientException::class.java) {
            handler.processSwitchBrowserResume("", extras)
        }
        Assert.assertEquals(ClientException.MISSING_PARAMETER, exception.errorCode)
        Assert.assertEquals("Action URI is null/empty: true, code is null/empty: true.", exception.message)
    }

    @Test
    fun `test processSwitchBrowserResume with null action URI`() {
        val code = "switch_browser_code"
        val state = "123"
        val extras = Bundle().apply {
            putString(SWITCH_BROWSER.CODE, code)
            putString(SWITCH_BROWSER.ACTION_URI, null)
            putString(SWITCH_BROWSER.STATE, state)
        }
        val handler = newHandler()

        val exception = Assert.assertThrows(ClientException::class.java) {
            handler.processSwitchBrowserResume("https://auth.com?state=$state", extras)
        }
        Assert.assertEquals(ClientException.MISSING_PARAMETER, exception.errorCode)
        Assert.assertTrue(exception.message!!.contains("Action URI is null/empty: true"))
    }

    @Test
    fun `test processSwitchBrowserResume with empty action URI`() {
        val code = "switch_browser_code"
        val state = "123"
        val extras = Bundle().apply {
            putString(SWITCH_BROWSER.CODE, code)
            putString(SWITCH_BROWSER.ACTION_URI, "")
            putString(SWITCH_BROWSER.STATE, state)
        }
        val handler = newHandler()

        val exception = Assert.assertThrows(ClientException::class.java) {
            handler.processSwitchBrowserResume("https://auth.com?state=$state", extras)
        }
        Assert.assertEquals(ClientException.MISSING_PARAMETER, exception.errorCode)
        Assert.assertTrue(exception.message!!.contains("Action URI is null/empty: true"))
    }

    @Test
    fun `test processSwitchBrowserResume with invalid action URI authority`() {
        isStateRequired(true)
        val code = "switch_browser_code"
        val invalidActionUrl = "https://invalid.authority.com/switchbrowser/path"
        val state = "123"
        val extras = Bundle().apply {
            putString(SWITCH_BROWSER.CODE, code)
            putString(SWITCH_BROWSER.ACTION_URI, invalidActionUrl)
            putString(SWITCH_BROWSER.STATE, state)
        }
        val handler = newHandler()

        val exception = Assert.assertThrows(ClientException::class.java) {
            handler.processSwitchBrowserResume("https://auth.com?state=$state", extras)
        }
        Assert.assertEquals(ClientException.UNKNOWN_AUTHORITY, exception.errorCode)
        Assert.assertTrue(exception.message!!.contains("Authority 'invalid.authority.com' is not a valid AAD authority"))
    }

    @Test
    fun `test createErrorBundle round-trips errorCode and errorMessage via processSwitchBrowserResume`() {
        val errorCode = "switch_browser_failed"
        val errorMessage = "Simulated upstream failure in the switch browser flow."
        val extras = SwitchBrowserProtocolCoordinator.createErrorBundle(errorCode, errorMessage)
        val handler = newHandler()
        handler.isSwitchBrowserChallengeActive = true

        val exception = Assert.assertThrows(ClientException::class.java) {
            handler.processSwitchBrowserResume("https://auth.com", extras)
        }
        Assert.assertEquals(errorCode, exception.errorCode)
        Assert.assertEquals(errorMessage, exception.message)

        // Reset must run on the error path too — otherwise isSwitchBrowserChallengeActive stays
        // true and subsequent onResume() calls would re-enter the resume flow with an
        // already-consumed bundle.
        Assert.assertFalse(handler.isSwitchBrowserChallengeActive)
    }

    @Test
    fun `test createErrorBundle with only errorCode still throws with that code`() {
        val errorCode = "code_only_error"
        val extras = SwitchBrowserProtocolCoordinator.createErrorBundle(errorCode, "")
        val handler = newHandler()
        handler.isSwitchBrowserChallengeActive = true

        val exception = Assert.assertThrows(ClientException::class.java) {
            handler.processSwitchBrowserResume("https://auth.com", extras)
        }
        Assert.assertEquals(errorCode, exception.errorCode)
        Assert.assertFalse(handler.isSwitchBrowserChallengeActive)
    }

    // endregion

    // region processSwitchBrowserResumeAsync

    @Test
    fun `test processSwitchBrowserResumeAsync delivers resume URI to success callback`() {
        isStateRequired(true)
        val code = "switch_browser_code"
        val actionUrl = "https://login.microsoft.com/switchbrowser/path"
        val state = "123"
        val extras = Bundle().apply {
            putString(SWITCH_BROWSER.CODE, code)
            putString(SWITCH_BROWSER.ACTION_URI, actionUrl)
            putString(SWITCH_BROWSER.STATE, state)
        }
        val handler = newHandler()

        handler.processSwitchBrowserResumeAsync(
            authorizationRequest = "https://auth.com?state=$state",
            extras = extras
        )
        runBlocking { awaitWork() }

        Assert.assertNull("onSwitchBrowserFailed should not fire on the happy path", statusListener.failure)
        Assert.assertNotNull(statusListener.completedUri)
        val parsedActionUri = Uri.parse(actionUrl)
        Assert.assertEquals(parsedActionUri.scheme, statusListener.completedUri!!.scheme)
        Assert.assertEquals(parsedActionUri.host, statusListener.completedUri!!.host)
        Assert.assertEquals(parsedActionUri.path, statusListener.completedUri!!.path)
        Assert.assertEquals("Bearer $code", statusListener.completedHeaders!![AUTHORIZATION])
    }

    @Test
    fun `test processSwitchBrowserResumeAsync routes state mismatch to error callback`() {
        isStateRequired(true)
        val code = "switch_browser_code"
        val actionUrl = "https://login.microsoft.com/switchbrowser/path"
        val extras = Bundle().apply {
            putString(SWITCH_BROWSER.CODE, code)
            putString(SWITCH_BROWSER.ACTION_URI, actionUrl)
            // STATE intentionally absent — state validation will fail.
        }
        val handler = newHandler()
        handler.isSwitchBrowserChallengeActive = true

        handler.processSwitchBrowserResumeAsync(
            authorizationRequest = "",
            extras = extras
        )
        runBlocking { awaitWork() }

        Assert.assertNull("Completed must not fire on state mismatch", statusListener.completedUri)
        Assert.assertTrue(statusListener.failure is ClientException)
        Assert.assertEquals(
            ClientException.STATE_MISMATCH,
            (statusListener.failure as ClientException).errorCode
        )
        // resetChallengeState must run on the error path so the handler does not get stuck
        // expecting a resume that already failed.
        Assert.assertFalse(handler.isSwitchBrowserChallengeActive)
    }

    @Test
    fun `test processSwitchBrowserResumeAsync routes invalid action URI to error callback`() {
        isStateRequired(true)
        val code = "switch_browser_code"
        val invalidActionUrl = "https://invalid.authority.com/switchbrowser/path"
        val state = "123"
        val extras = Bundle().apply {
            putString(SWITCH_BROWSER.CODE, code)
            putString(SWITCH_BROWSER.ACTION_URI, invalidActionUrl)
            putString(SWITCH_BROWSER.STATE, state)
        }
        val handler = newHandler()

        handler.processSwitchBrowserResumeAsync(
            authorizationRequest = "https://auth.com?state=$state",
            extras = extras
        )
        runBlocking { awaitWork() }

        Assert.assertNull(statusListener.completedUri)
        Assert.assertTrue(statusListener.failure is ClientException)
        Assert.assertEquals(
            ClientException.UNKNOWN_AUTHORITY,
            (statusListener.failure as ClientException).errorCode
        )
    }

    // endregion

    // region isExpectingSwitchBrowserResume / isSwitchBrowserResume

    @Test
    fun `test isExpectingSwitchBrowserResume with handler true`() {
        val handler = newHandler()
        handler.isSwitchBrowserChallengeActive = true
        Assert.assertTrue(handler.isExpectingSwitchBrowserResume())
    }

    @Test
    fun `test isExpectingSwitchBrowserResume with handler false`() {
        val handler = newHandler()
        handler.isSwitchBrowserChallengeActive = false
        Assert.assertFalse(handler.isExpectingSwitchBrowserResume())
    }

    @Test
    fun `test isSwitchBrowserResume for valid url`() {
        val url = "${Broker.NEW_BROKER_REDIRECT_URI}/${SWITCH_BROWSER.RESUME_PATH}"
        val redirectUrl = Broker.NEW_BROKER_REDIRECT_URI
        Assert.assertTrue(SwitchBrowserProtocolCoordinator.isSwitchBrowserResume(url, redirectUrl))
    }

    @Test
    fun `test isSwitchBrowserResume for invalid url`() {
        val url = "${Broker.NEW_BROKER_REDIRECT_URI}/invalid_path"
        val redirectUrl = Broker.NEW_BROKER_REDIRECT_URI
        Assert.assertFalse(SwitchBrowserProtocolCoordinator.isSwitchBrowserResume(url, redirectUrl))
    }

    // endregion
}
