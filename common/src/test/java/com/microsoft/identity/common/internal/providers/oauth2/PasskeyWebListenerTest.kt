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

package com.microsoft.identity.common.internal.providers.oauth2

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.webkit.WebView
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.test.core.app.ApplicationProvider
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.microsoft.identity.common.internal.ui.webview.AzureActiveDirectoryWebViewClient
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [PasskeyWebListener].
 *
 * Tests WebAuthn message handling, credential creation/retrieval flows, and error handling.
 * Uses real objects where possible, mocking only external dependencies like CredentialManager.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P]) // API 28+ required for passkey support
class PasskeyWebListenerTest {

    // Real objects
    private lateinit var testScope: CoroutineScope
    private lateinit var activity: Activity
    private lateinit var webView: WebView
    private lateinit var sourceOrigin: Uri

    // Mocked objects (only what's necessary)
    private lateinit var mockCredentialManagerHandler: CredentialManagerHandler
    private lateinit var mockJavaScriptReplyProxy: JavaScriptReplyProxy
    private lateinit var mockWebMessageCompat: WebMessageCompat
    private lateinit var mockWebViewClient: AzureActiveDirectoryWebViewClient

    // System under test
    private lateinit var passkeyWebListener: PasskeyWebListener

    @Before
    fun setUp() {
        testScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

        // Initialize real Activity using Robolectric
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        webView = WebView(ApplicationProvider.getApplicationContext())
        sourceOrigin = Uri.parse("https://login.microsoft.com")

        // Mock only external dependencies
        mockCredentialManagerHandler = mockk(relaxed = true)
        mockJavaScriptReplyProxy = mockk(relaxed = true)
        mockWebMessageCompat = mockk()
        mockWebViewClient = mockk(relaxed = true)

        // Create listener with test coroutine scope
        passkeyWebListener = PasskeyWebListener(
            coroutineScope = testScope,
            credentialManagerHandler = mockCredentialManagerHandler
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    // ========== Message Parsing Tests ==========

    @Test
    fun `onPostMessage with valid create request processes successfully`() = runBlocking {
        // Given
        val createRequest = """{"publicKey": {"challenge": "test"}}"""
        val message = createValidMessage(PasskeyWebListener.CREATE_UNIQUE_KEY, createRequest)
        every { mockWebMessageCompat.data } returns message

        val mockResponse = mockk<CreatePublicKeyCredentialResponse>()
        val responseJson = """{"type":"public-key","rawId":"dGVzdA=="}"""
        every { mockResponse.registrationResponseJson } returns responseJson
        coEvery { mockCredentialManagerHandler.createPasskey(createRequest) } returns mockResponse

        val messageSlot = slot<String>()

        // When
        passkeyWebListener.onPostMessage(
            webView,
            mockWebMessageCompat,
            sourceOrigin,
            isMainFrame = true,
            mockJavaScriptReplyProxy
        )

        // Then
        verify(timeout = 1000) { mockJavaScriptReplyProxy.postMessage(capture(messageSlot)) }
        val responseObject = JSONObject(messageSlot.captured)
        assertEquals(PasskeyReplyChannel.SUCCESS_STATUS, responseObject.getString(PasskeyReplyChannel.STATUS_KEY))
        assertEquals(PasskeyWebListener.CREATE_UNIQUE_KEY, responseObject.getString(PasskeyReplyChannel.TYPE_KEY))
    }

    @Test
    fun `onPostMessage with valid get request processes successfully`() = runBlocking {
        // Given
        val getRequest = """{"publicKey": {"challenge": "test"}}"""
        val message = createValidMessage(PasskeyWebListener.GET_UNIQUE_KEY, getRequest)
        every { mockWebMessageCompat.data } returns message

        val mockCredential = mockk<PublicKeyCredential>()
        val authResponseJson = """{"type":"public-key","rawId":"dGVzdA=="}"""
        every { mockCredential.authenticationResponseJson } returns authResponseJson

        val mockResponse = mockk<GetCredentialResponse>()
        every { mockResponse.credential } returns mockCredential
        coEvery { mockCredentialManagerHandler.getPasskey(getRequest) } returns mockResponse

        val messageSlot = slot<String>()

        // When
        passkeyWebListener.onPostMessage(
            webView,
            mockWebMessageCompat,
            sourceOrigin,
            isMainFrame = true,
            mockJavaScriptReplyProxy
        )

        // Then
        verify(timeout = 1000) { mockJavaScriptReplyProxy.postMessage(capture(messageSlot)) }
        val responseObject = JSONObject(messageSlot.captured)
        assertEquals(PasskeyReplyChannel.SUCCESS_STATUS, responseObject.getString(PasskeyReplyChannel.STATUS_KEY))
        assertEquals(PasskeyWebListener.GET_UNIQUE_KEY, responseObject.getString(PasskeyReplyChannel.TYPE_KEY))
    }

    @Test
    fun `onPostMessage with empty message data sends error`() {
        // Given
        every { mockWebMessageCompat.data } returns ""
        val messageSlot = slot<String>()

        // When
        passkeyWebListener.onPostMessage(
            webView,
            mockWebMessageCompat,
            sourceOrigin,
            isMainFrame = true,
            mockJavaScriptReplyProxy
        )

        // Then
        verify { mockJavaScriptReplyProxy.postMessage(capture(messageSlot)) }
        val responseObject = JSONObject(messageSlot.captured)
        assertEquals(PasskeyReplyChannel.ERROR_STATUS, responseObject.getString(PasskeyReplyChannel.STATUS_KEY))
        val dataObject = responseObject.getJSONObject(PasskeyReplyChannel.DATA_KEY)
        assertTrue(dataObject.getString(PasskeyReplyChannel.DOM_EXCEPTION_MESSAGE_KEY).contains("Message data is null or blank"))
    }

    @Test
    fun `onPostMessage with null message data sends error`() {
        // Given
        every { mockWebMessageCompat.data } returns null
        val messageSlot = slot<String>()

        // When
        passkeyWebListener.onPostMessage(
            webView,
            mockWebMessageCompat,
            sourceOrigin,
            isMainFrame = true,
            mockJavaScriptReplyProxy
        )

        // Then
        verify { mockJavaScriptReplyProxy.postMessage(capture(messageSlot)) }
        val responseObject = JSONObject(messageSlot.captured)
        assertEquals(PasskeyReplyChannel.ERROR_STATUS, responseObject.getString(PasskeyReplyChannel.STATUS_KEY))
    }

    @Test
    fun `onPostMessage with missing type key sends error`() {
        // Given
        val invalidMessage = """{"request": "test"}"""
        every { mockWebMessageCompat.data } returns invalidMessage
        val messageSlot = slot<String>()

        // When
        passkeyWebListener.onPostMessage(
            webView,
            mockWebMessageCompat,
            sourceOrigin,
            isMainFrame = true,
            mockJavaScriptReplyProxy
        )

        // Then
        verify { mockJavaScriptReplyProxy.postMessage(capture(messageSlot)) }
        val responseObject = JSONObject(messageSlot.captured)
        assertEquals(PasskeyReplyChannel.ERROR_STATUS, responseObject.getString(PasskeyReplyChannel.STATUS_KEY))
        val dataObject = responseObject.getJSONObject(PasskeyReplyChannel.DATA_KEY)
        assertTrue(dataObject.getString(PasskeyReplyChannel.DOM_EXCEPTION_MESSAGE_KEY).contains("type"))
    }

    @Test
    fun `onPostMessage with missing request key sends error`() {
        // Given
        val invalidMessage = """{"type": "create"}"""
        every { mockWebMessageCompat.data } returns invalidMessage
        val messageSlot = slot<String>()

        // When
        passkeyWebListener.onPostMessage(
            webView,
            mockWebMessageCompat,
            sourceOrigin,
            isMainFrame = true,
            mockJavaScriptReplyProxy
        )

        // Then
        verify { mockJavaScriptReplyProxy.postMessage(capture(messageSlot)) }
        val responseObject = JSONObject(messageSlot.captured)
        assertEquals(PasskeyReplyChannel.ERROR_STATUS, responseObject.getString(PasskeyReplyChannel.STATUS_KEY))
        val dataObject = responseObject.getJSONObject(PasskeyReplyChannel.DATA_KEY)
        assertTrue(dataObject.getString(PasskeyReplyChannel.DOM_EXCEPTION_MESSAGE_KEY).contains("request"))
    }

    @Test
    fun `onPostMessage with invalid JSON sends error`() {
        // Given
        every { mockWebMessageCompat.data } returns "not valid json"
        val messageSlot = slot<String>()

        // When
        passkeyWebListener.onPostMessage(
            webView,
            mockWebMessageCompat,
            sourceOrigin,
            isMainFrame = true,
            mockJavaScriptReplyProxy
        )

        // Then
        verify { mockJavaScriptReplyProxy.postMessage(capture(messageSlot)) }
        val responseObject = JSONObject(messageSlot.captured)
        assertEquals(PasskeyReplyChannel.ERROR_STATUS, responseObject.getString(PasskeyReplyChannel.STATUS_KEY))
    }

    @Test
    fun `onPostMessage with unknown request type sends error`() {
        // Given
        val message = createValidMessage("unknown_type", """{"test": "data"}""")
        every { mockWebMessageCompat.data } returns message
        val messageSlot = slot<String>()

        // When
        passkeyWebListener.onPostMessage(
            webView,
            mockWebMessageCompat,
            sourceOrigin,
            isMainFrame = true,
            mockJavaScriptReplyProxy
        )

        // Then
        verify { mockJavaScriptReplyProxy.postMessage(capture(messageSlot)) }
        val responseObject = JSONObject(messageSlot.captured)
        assertEquals(PasskeyReplyChannel.ERROR_STATUS, responseObject.getString(PasskeyReplyChannel.STATUS_KEY))
        val dataObject = responseObject.getJSONObject(PasskeyReplyChannel.DATA_KEY)
        assertTrue(dataObject.getString(PasskeyReplyChannel.DOM_EXCEPTION_MESSAGE_KEY).contains("Unsupported WebAuthN request type: unknown_type"))
    }

    // ========== Frame Origin Tests ==========

    @Test
    fun `onPostMessage from iframe sends error`() {
        // Given
        val message = createValidMessage(PasskeyWebListener.CREATE_UNIQUE_KEY, """{"test": "data"}""")
        every { mockWebMessageCompat.data } returns message
        val messageSlot = slot<String>()

        // When
        passkeyWebListener.onPostMessage(
            webView,
            mockWebMessageCompat,
            sourceOrigin,
            isMainFrame = false, // Not main frame
            mockJavaScriptReplyProxy
        )

        // Then
        verify { mockJavaScriptReplyProxy.postMessage(capture(messageSlot)) }
        val responseObject = JSONObject(messageSlot.captured)
        assertEquals(PasskeyReplyChannel.ERROR_STATUS, responseObject.getString(PasskeyReplyChannel.STATUS_KEY))
        val dataObject = responseObject.getJSONObject(PasskeyReplyChannel.DATA_KEY)
        assertTrue(dataObject.getString(PasskeyReplyChannel.DOM_EXCEPTION_MESSAGE_KEY).contains("iframe"))
    }

    // ========== Concurrent Request Tests ==========

    @Test
    fun `onPostMessage rejects concurrent requests`() = runBlocking {
        // Given - First request that will take time
        val firstRequest = createValidMessage(PasskeyWebListener.CREATE_UNIQUE_KEY, """{"test": "data1"}""")
        every { mockWebMessageCompat.data } returns firstRequest

        val mockResponse = mockk<CreatePublicKeyCredentialResponse>()
        every { mockResponse.registrationResponseJson } returns """{"id":"test"}"""
        coEvery { mockCredentialManagerHandler.createPasskey(any()) } coAnswers {
            kotlinx.coroutines.delay(100) // Simulate long operation
            mockResponse
        }

        // When - Send first request
        passkeyWebListener.onPostMessage(
            webView,
            mockWebMessageCompat,
            sourceOrigin,
            isMainFrame = true,
            mockJavaScriptReplyProxy
        )

        // Send second request immediately (before first completes)
        val secondReplyProxy = mockk<JavaScriptReplyProxy>(relaxed = true)
        val secondMessage = createValidMessage(PasskeyWebListener.GET_UNIQUE_KEY, """{"test": "data2"}""")
        val secondWebMessage = mockk<WebMessageCompat>()
        every { secondWebMessage.data } returns secondMessage

        val messageSlot = slot<String>()
        passkeyWebListener.onPostMessage(
            webView,
            secondWebMessage,
            sourceOrigin,
            isMainFrame = true,
            secondReplyProxy
        )

        // Then - Second request should be rejected immediately
        verify { secondReplyProxy.postMessage(capture(messageSlot)) }
        val responseObject = JSONObject(messageSlot.captured)
        assertEquals(PasskeyReplyChannel.ERROR_STATUS, responseObject.getString(PasskeyReplyChannel.STATUS_KEY))
        val dataObject = responseObject.getJSONObject(PasskeyReplyChannel.DATA_KEY)
        assertTrue(dataObject.getString(PasskeyReplyChannel.DOM_EXCEPTION_MESSAGE_KEY).contains("already in progress"))
    }

    // ========== Error Handling Tests ==========

    @Test
    fun `create request handles cancellation exception`() = runBlocking {
        // Given
        val createRequest = """{"publicKey": {"challenge": "test"}}"""
        val message = createValidMessage(PasskeyWebListener.CREATE_UNIQUE_KEY, createRequest)
        every { mockWebMessageCompat.data } returns message

        coEvery { mockCredentialManagerHandler.createPasskey(createRequest) } throws
            CreateCredentialCancellationException("User cancelled")

        val messageSlot = slot<String>()

        // When
        passkeyWebListener.onPostMessage(
            webView,
            mockWebMessageCompat,
            sourceOrigin,
            isMainFrame = true,
            mockJavaScriptReplyProxy
        )

        // Then
        verify(timeout = 1000) { mockJavaScriptReplyProxy.postMessage(capture(messageSlot)) }
        val responseObject = JSONObject(messageSlot.captured)
        assertEquals(PasskeyReplyChannel.ERROR_STATUS, responseObject.getString(PasskeyReplyChannel.STATUS_KEY))
        val dataObject = responseObject.getJSONObject(PasskeyReplyChannel.DATA_KEY)
        assertEquals(
            PasskeyReplyChannel.DOM_EXCEPTION_NOT_ALLOWED_ERROR,
            dataObject.getString(PasskeyReplyChannel.DOM_EXCEPTION_NAME_KEY)
        )
    }

    @Test
    fun `get request handles cancellation exception`() = runBlocking {
        // Given
        val getRequest = """{"publicKey": {"challenge": "test"}}"""
        val message = createValidMessage(PasskeyWebListener.GET_UNIQUE_KEY, getRequest)
        every { mockWebMessageCompat.data } returns message

        coEvery { mockCredentialManagerHandler.getPasskey(getRequest) } throws
            GetCredentialCancellationException("User cancelled")

        val messageSlot = slot<String>()

        // When
        passkeyWebListener.onPostMessage(
            webView,
            mockWebMessageCompat,
            sourceOrigin,
            isMainFrame = true,
            mockJavaScriptReplyProxy
        )

        // Then
        verify(timeout = 1000) { mockJavaScriptReplyProxy.postMessage(capture(messageSlot)) }
        val responseObject = JSONObject(messageSlot.captured)
        assertEquals(PasskeyReplyChannel.ERROR_STATUS, responseObject.getString(PasskeyReplyChannel.STATUS_KEY))
        val dataObject = responseObject.getJSONObject(PasskeyReplyChannel.DATA_KEY)
        assertEquals(
            PasskeyReplyChannel.DOM_EXCEPTION_NOT_ALLOWED_ERROR,
            dataObject.getString(PasskeyReplyChannel.DOM_EXCEPTION_NAME_KEY)
        )
    }

    @Test
    fun `create request handles generic exception`() = runBlocking {
        // Given
        val createRequest = """{"publicKey": {"challenge": "test"}}"""
        val message = createValidMessage(PasskeyWebListener.CREATE_UNIQUE_KEY, createRequest)
        every { mockWebMessageCompat.data } returns message

        coEvery { mockCredentialManagerHandler.createPasskey(createRequest) } throws
            RuntimeException("Unexpected error")

        val messageSlot = slot<String>()

        // When
        passkeyWebListener.onPostMessage(
            webView,
            mockWebMessageCompat,
            sourceOrigin,
            isMainFrame = true,
            mockJavaScriptReplyProxy
        )

        // Then
        verify(timeout = 1000) { mockJavaScriptReplyProxy.postMessage(capture(messageSlot)) }
        val responseObject = JSONObject(messageSlot.captured)
        assertEquals(PasskeyReplyChannel.ERROR_STATUS, responseObject.getString(PasskeyReplyChannel.STATUS_KEY))
    }

    // ========== Hook Method Tests ==========

    @Test
    @Config(sdk = [Build.VERSION_CODES.O_MR1]) // API 27 - below minimum
    fun `hook returns false on API below 28`() {
        // When
        val result = PasskeyWebListener.hook(webView, activity, mockWebViewClient)

        // Then
        assertFalse(result)
        verify(exactly = 0) { mockWebViewClient.addPasskeyRegistrationJsScript(any()) }
    }

    @Test
    fun `hook returns true and sets up listener on supported devices`() {
        // Given
        mockkStatic(WebViewFeature::class)
        every { WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) } returns true

        mockkStatic(WebViewCompat::class)
        every {
            WebViewCompat.addWebMessageListener(
                any(),
                any(),
                any(),
                any()
            )
        } just Runs

        // When
        val result = PasskeyWebListener.hook(webView, activity, mockWebViewClient)

        // Then
        assertTrue(result)
        verify {
            WebViewCompat.addWebMessageListener(
                webView,
                any(),
                any(),
                any()
            )
        }
        verify {
            mockWebViewClient.addPasskeyRegistrationJsScript(any())
        }

        unmockkStatic(WebViewFeature::class)
        unmockkStatic(WebViewCompat::class)
    }

    @Test
    fun `hook returns false when WEB_MESSAGE_LISTENER not supported`() {
        // Given
        mockkStatic(WebViewFeature::class)
        every { WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) } returns false

        // When
        val result = PasskeyWebListener.hook(webView, activity, mockWebViewClient)

        // Then
        assertFalse(result)
        verify(exactly = 0) { mockWebViewClient.addPasskeyRegistrationJsScript(any()) }

        unmockkStatic(WebViewFeature::class)
    }

    // ========== Helper Methods ==========

    /**
     * Creates a valid WebAuthn message JSON string.
     */
    private fun createValidMessage(type: String, request: String): String {
        return JSONObject().apply {
            put(PasskeyWebListener.TYPE_KEY, type)
            put(PasskeyWebListener.REQUEST_KEY, request)
        }.toString()
    }
}

