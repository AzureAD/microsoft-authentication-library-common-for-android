/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 *
 * This code is licensed under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files(the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions :
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.microsoft.identity.common.internal.ui.webview.authux

import android.net.Uri
import android.webkit.WebView
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import com.microsoft.identity.common.internal.broker.AuthUxMessageProcessor
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthUxWebMessageListenerTest {
    private val webView = mock(WebView::class.java)
    private val replyProxy = mock(JavaScriptReplyProxy::class.java)

    @Test
    fun `allowed main-frame message is delivered exactly once`() {
        val processor = RecordingProcessor()
        val listener = AuthUxWebMessageListener(processor, AuthUxOriginPolicy())

        listener.onPostMessage(
            webView,
            message("payload"),
            Uri.parse("https://login.microsoftonline.com"),
            true,
            replyProxy
        )

        assertEquals(listOf("payload"), processor.payloads)
    }

    @Test
    fun `subframe and disallowed origin messages are rejected`() {
        val processor = RecordingProcessor()
        val listener = AuthUxWebMessageListener(processor, AuthUxOriginPolicy())

        listener.onPostMessage(
            webView,
            message("subframe"),
            Uri.parse("https://login.microsoftonline.com"),
            false,
            replyProxy
        )
        listener.onPostMessage(
            webView,
            message("wrong-origin"),
            Uri.parse("https://login.microsoftonline.com.evil.example"),
            true,
            replyProxy
        )

        assertEquals(emptyList<String>(), processor.payloads)
    }

    @Test
    fun `http default port message is allowed`() {
        val processor = RecordingProcessor()
        val listener = AuthUxWebMessageListener(processor, AuthUxOriginPolicy())

        listener.onPostMessage(
            webView,
            message("payload"),
            Uri.parse("http://login.microsoftonline.com:80"),
            true,
            replyProxy
        )

        assertEquals(listOf("payload"), processor.payloads)
    }

    @Test
    fun `non-default port and custom scheme messages are rejected`() {
        val processor = RecordingProcessor()
        val listener = AuthUxWebMessageListener(processor, AuthUxOriginPolicy())

        listener.onPostMessage(
            webView,
            message("non-default-port"),
            Uri.parse("https://login.microsoftonline.com:8443"),
            true,
            replyProxy
        )
        listener.onPostMessage(
            webView,
            message("custom-scheme"),
            Uri.parse("custom://login.microsoftonline.com"),
            true,
            replyProxy
        )

        assertEquals(emptyList<String>(), processor.payloads)
    }

    @Test
    fun `late message is ignored by closed request processor`() {
        val processor = RecordingProcessor()
        processor.close()
        val listener = AuthUxWebMessageListener(processor, AuthUxOriginPolicy())

        listener.onPostMessage(
            webView,
            message("late"),
            Uri.parse("https://login.microsoftonline.com"),
            true,
            replyProxy
        )

        assertEquals(emptyList<String>(), processor.payloads)
    }

    private fun message(data: String): WebMessageCompat =
        mock(WebMessageCompat::class.java).also {
            `when`(it.data).thenReturn(data)
        }

    private class RecordingProcessor : AuthUxMessageProcessor {
        val payloads = mutableListOf<String>()
        override var isClosed: Boolean = false
            private set

        override fun process(jsonPayload: String) {
            if (!isClosed) {
                payloads.add(jsonPayload)
            }
        }

        override fun close() {
            isClosed = true
        }
    }
}
