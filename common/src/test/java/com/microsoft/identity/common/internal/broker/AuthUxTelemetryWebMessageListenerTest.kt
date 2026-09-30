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
package com.microsoft.identity.common.internal.broker

import android.net.Uri
import android.webkit.WebView
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.microsoft.identity.common.internal.numberMatch.NumberMatchHelper
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthUxTelemetryWebMessageListenerTest {
    private val webView = mockk<WebView>(relaxed = true)
    private val message = mockk<WebMessageCompat>()
    private val replyProxy = mockk<JavaScriptReplyProxy>(relaxed = true)
    private val received = mutableListOf<AuthUxTelemetryEvent>()
    private val listener = AuthUxTelemetryWebMessageListener {
        received.add(it)
        true
    }

    @After
    fun tearDown() {
        NumberMatchHelper.numberMatchMap.clear()
        unmockkStatic(WebViewFeature::class)
        unmockkStatic(WebViewCompat::class)
    }

    @Test
    fun `allowed main-frame telemetry reaches sink`() {
        every { message.data } returns TELEMETRY_PAYLOAD

        listener.onPostMessage(
            webView,
            message,
            Uri.parse("https://login.microsoftonline.com"),
            true,
            replyProxy
        )

        assertEquals(listOf("530003"), received.map { it.errorCode })
    }

    @Test
    fun `subframe and disallowed origin messages are rejected`() {
        every { message.data } returns TELEMETRY_PAYLOAD

        listener.onPostMessage(
            webView,
            message,
            Uri.parse("https://login.microsoftonline.com"),
            false,
            replyProxy
        )
        listener.onPostMessage(
            webView,
            message,
            Uri.parse("https://example.test"),
            true,
            replyProxy
        )

        assertTrue(received.isEmpty())
    }

    @Test
    fun `number matching cannot execute through telemetry listener`() {
        every { message.data } returns NUMBER_MATCH_PAYLOAD

        listener.onPostMessage(
            webView,
            message,
            Uri.parse("https://login.microsoftonline.com"),
            true,
            replyProxy
        )

        assertTrue(received.isEmpty())
        assertTrue(NumberMatchHelper.numberMatchMap.isEmpty())
    }

    @Test
    fun `hook is feature gated and cleanup removes registered listener`() {
        mockkStatic(WebViewFeature::class)
        mockkStatic(WebViewCompat::class)
        every {
            WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)
        } returnsMany listOf(false, true, true)
        every {
            WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
        } returns true
        every {
            WebViewCompat.addWebMessageListener(any(), any(), any(), any())
        } just Runs
        every { WebViewCompat.removeWebMessageListener(any(), any()) } just Runs

        assertFalse(AuthUxTelemetryWebMessageListener.hook(webView) { true })
        assertTrue(AuthUxTelemetryWebMessageListener.hook(webView) { true })
        AuthUxTelemetryWebMessageListener.unhook(webView)

        verify(exactly = 1) {
            WebViewCompat.addWebMessageListener(
                webView,
                AuthUxTelemetryWebMessageListener.INTERFACE_NAME,
                AuthUxTelemetryWebMessageListener.getAllowedOriginRules(),
                any()
            )
        }
        verify(exactly = 1) {
            WebViewCompat.removeWebMessageListener(
                webView,
                AuthUxTelemetryWebMessageListener.INTERFACE_NAME
            )
        }
    }

    @Test
    fun `missing document start support does not install listener`() {
        mockkStatic(WebViewFeature::class)
        mockkStatic(WebViewCompat::class)
        every { WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) } returns true
        every { WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT) } returns false

        assertFalse(AuthUxTelemetryWebMessageListener.hook(webView) { true })

        verify(exactly = 0) { WebViewCompat.addWebMessageListener(any(), any(), any(), any()) }
    }

    @Test
    fun `feature lookup failure disables telemetry`() {
        mockkStatic(WebViewFeature::class)
        mockkStatic(WebViewCompat::class)
        every { WebViewFeature.isFeatureSupported(any()) } throws IllegalStateException("Lookup failed")

        assertFalse(AuthUxTelemetryWebMessageListener.hook(webView) { true })

        verify(exactly = 0) { WebViewCompat.addWebMessageListener(any(), any(), any(), any()) }
    }

    @Test
    fun `partial registration and cleanup failures do not escape`() {
        mockkStatic(WebViewFeature::class)
        mockkStatic(WebViewCompat::class)
        every { WebViewFeature.isFeatureSupported(any()) } returns true
        every {
            WebViewCompat.addWebMessageListener(any(), any(), any(), any())
        } throws IllegalStateException("Add failed")
        every {
            WebViewCompat.removeWebMessageListener(any(), any())
        } throws IllegalStateException("Remove failed")

        assertFalse(AuthUxTelemetryWebMessageListener.hook(webView) { true })

        verify(exactly = 1) {
            WebViewCompat.removeWebMessageListener(
                webView,
                AuthUxTelemetryWebMessageListener.INTERFACE_NAME
            )
        }
    }

    companion object {
        private const val TELEMETRY_PAYLOAD =
            """{"correlationID":"corr","action_name":"log_telemetry","action_component":"broker","params":{"errorCode":"530003"}}"""
        private const val NUMBER_MATCH_PAYLOAD =
            """{"correlationID":"corr","action_name":"write_data","action_component":"broker","params":{"operation":"number_matching","sessionID":"session","code_match":"42"}}"""
    }
}
