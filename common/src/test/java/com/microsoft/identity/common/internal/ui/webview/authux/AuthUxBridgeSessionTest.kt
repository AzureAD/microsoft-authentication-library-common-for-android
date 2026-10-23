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

import android.webkit.WebView
import androidx.webkit.ScriptHandler
import androidx.webkit.WebViewCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthUxBridgeSessionTest {
    private val webView = mock(WebView::class.java)

    @Test
    fun `eligible broker selects WebMessageListener when both features are supported`() {
        val adapter = FakeWebKitAdapter()

        val session = AuthUxBridgeSession.create(
            webView = webView,
            initialUrl = LOGIN_URL,
            isBrokerAuthProcess = true,
            hasOnboardingTelemetryRecorder = false,
            brokerMasterFlightEnabled = true,
            preferWebMessageListener = true,
            webKitAdapter = adapter
        )

        assertEquals(AuthUxTransportMode.WEB_MESSAGE_LISTENER, session.mode)
        assertTrue(adapter.listenerAdded)
        assertTrue(adapter.scriptAdded)
        assertEquals(adapter.listenerAllowedOrigins, adapter.scriptAllowedOrigins)
        assertEquals(8, adapter.listenerAllowedOrigins?.size)
        verify(webView, never()).addJavascriptInterface(any(), eq("broker"))
    }

    @Test
    fun `broker preference off selects managed legacy without consulting modern prerequisites`() {
        val adapter = FakeWebKitAdapter()
        val originPolicy = mock(AuthUxOriginPolicy::class.java)
        val session = AuthUxBridgeSession.create(
            webView = webView,
            initialUrl = LOGIN_URL,
            isBrokerAuthProcess = true,
            hasOnboardingTelemetryRecorder = false,
            brokerMasterFlightEnabled = true,
            preferWebMessageListener = false,
            webKitAdapter = adapter,
            originPolicy = originPolicy
        )

        assertEquals(AuthUxTransportMode.JAVASCRIPT_INTERFACE, session.mode)
        assertEquals(0, adapter.listenerSupportCheckCount)
        assertEquals(0, adapter.scriptSupportCheckCount)
        assertFalse(adapter.listenerAdded)
        assertFalse(adapter.scriptAdded)
        org.mockito.Mockito.verifyNoInteractions(originPolicy)
        verify(webView).addJavascriptInterface(any(), eq("broker"))

        session.onPageFinished(webView)
        verify(webView).evaluateJavascript(any(), any())
        session.onPageStarted(webView, FEDERATED_URL)
        verify(webView).removeJavascriptInterface("broker")
        session.onPageStarted(webView, LOGIN_URL)
        verify(webView, times(2)).addJavascriptInterface(any(), eq("broker"))
        session.close()
        verify(webView, times(2)).removeJavascriptInterface("broker")
    }

    @Test
    fun `broker request is disabled when master flight is off`() {
        assertBrokerMasterFlightDisabled(hasOnboardingTelemetryRecorder = false)
    }

    @Test
    fun `broker recorder does not bypass disabled master flight`() {
        assertBrokerMasterFlightDisabled(hasOnboardingTelemetryRecorder = true)
    }

    @Test
    fun `listener-only support selects JavaScript interface`() {
        val adapter = FakeWebKitAdapter(documentStartSupported = false)

        val session = AuthUxBridgeSession.create(
            webView,
            LOGIN_URL,
            true,
            false,
            true,
            true,
            adapter
        )

        assertEquals(AuthUxTransportMode.JAVASCRIPT_INTERFACE, session.mode)
        assertFalse(adapter.listenerAdded)
        verify(webView).addJavascriptInterface(any(), eq("broker"))
    }

    @Test
    fun `rejected URLs expose neither transport until approved legacy navigation`() {
        listOf(
            "$LOGIN_URL?claims={\"access_token\":{}}",
            "$LOGIN_URL#claims={\"access_token\":{}}",
            "https://login.microsoftonline.com/%",
            "https://LOGIN.MICROSOFTONLINE.COM/path",
            "https://login.MicrosoftOnline.com/path",
            "https://.microsoftonline.com/path",
            "https://login.microsoftonline.com:invalid/path",
            "https://evil.example\\@login.microsoftonline.com/path"
        ).forEach { url ->
            val view = mock(WebView::class.java)
            val adapter = FakeWebKitAdapter()
            val session = AuthUxBridgeSession.create(
                view,
                url,
                true,
                false,
                true,
                true,
                adapter
            )

            assertEquals(url, AuthUxTransportMode.JAVASCRIPT_INTERFACE, session.mode)
            assertFalse(url, adapter.listenerAdded)
            assertFalse(url, adapter.scriptAdded)
            session.onPageStarted(view, url)
            session.onPageFinished(view)
            verify(view, never()).addJavascriptInterface(any(), eq("broker"))
            verify(view, never()).evaluateJavascript(any(), any())

            session.onPageStarted(view, LOGIN_URL)
            session.onPageFinished(view)
            verify(view).addJavascriptInterface(any(), eq("broker"))
            verify(view).evaluateJavascript(any(), any())

            session.onPageStarted(view, url)
            session.onPageFinished(view)
            verify(view).removeJavascriptInterface("broker")
            verify(view, times(1)).evaluateJavascript(any(), any())

            session.onPageStarted(view, ENCODED_CLAIMS_URL)
            session.onPageFinished(view)
            verify(view, times(2)).addJavascriptInterface(any(), eq("broker"))
            verify(view, times(2)).evaluateJavascript(any(), any())

            session.close()
            verify(view, times(2)).removeJavascriptInterface("broker")
        }
    }

    @Test
    fun `encoded claims select WebMessageListener when both features are supported`() {
        val adapter = FakeWebKitAdapter()
        val session = AuthUxBridgeSession.create(
            webView,
            ENCODED_CLAIMS_URL,
            true,
            false,
            true,
            true,
            adapter
        )

        assertEquals(AuthUxTransportMode.WEB_MESSAGE_LISTENER, session.mode)
        assertTrue(adapter.listenerAdded)
        assertTrue(adapter.scriptAdded)
        assertEquals(adapter.listenerAllowedOrigins, adapter.scriptAllowedOrigins)
        verify(webView, never()).addJavascriptInterface(any(), eq("broker"))
        session.close()
    }

    @Test
    fun `encoded claims use legacy when either required feature is unavailable`() {
        listOf(
            FakeWebKitAdapter(listenerSupported = false),
            FakeWebKitAdapter(documentStartSupported = false)
        ).forEach { adapter ->
            val view = mock(WebView::class.java)
            val session = AuthUxBridgeSession.create(
                view,
                ENCODED_CLAIMS_URL,
                true,
                false,
                true,
                true,
                adapter
            )

            assertEquals(AuthUxTransportMode.JAVASCRIPT_INTERFACE, session.mode)
            assertFalse(adapter.listenerAdded)
            assertFalse(adapter.scriptAdded)
            verify(view).addJavascriptInterface(any(), eq("broker"))
            session.onPageFinished(view)
            verify(view).evaluateJavascript(any(), any())
            session.close()
            verify(view).removeJavascriptInterface("broker")
        }
    }

    @Test
    fun `non-default port selects JavaScript interface`() {
        val session = AuthUxBridgeSession.create(
            webView,
            "https://login.microsoftonline.com:8443/common/oauth2/authorize",
            true,
            false,
            true,
            true,
            FakeWebKitAdapter()
        )

        assertEquals(AuthUxTransportMode.JAVASCRIPT_INTERFACE, session.mode)
        verify(webView).addJavascriptInterface(any(), eq("broker"))
    }

    @Test
    fun `custom scheme selects JavaScript interface`() {
        val session = AuthUxBridgeSession.create(
            webView,
            "custom://login.microsoftonline.com/common/oauth2/authorize",
            true,
            false,
            true,
            true,
            FakeWebKitAdapter()
        )

        assertEquals(AuthUxTransportMode.JAVASCRIPT_INTERFACE, session.mode)
        verify(webView).addJavascriptInterface(any(), eq("broker"))
    }

    @Test
    fun `legacy navigation manages interface and facade until session is closed`() {
        val session = AuthUxBridgeSession.create(
            webView,
            LOGIN_URL,
            true,
            false,
            true,
            true,
            FakeWebKitAdapter(documentStartSupported = false)
        )

        assertEquals(AuthUxTransportMode.JAVASCRIPT_INTERFACE, session.mode)
        verify(webView).addJavascriptInterface(any(), eq("broker"))

        session.onPageFinished(webView)
        verify(webView).evaluateJavascript(any(), any())

        session.onPageStarted(webView, FEDERATED_URL)
        verify(webView).removeJavascriptInterface("broker")
        session.onPageFinished(webView)
        verify(webView, times(1)).evaluateJavascript(any(), any())

        session.onPageStarted(webView, LOGIN_URL)
        verify(webView, times(2)).addJavascriptInterface(any(), eq("broker"))
        session.onPageFinished(webView)
        verify(webView, times(2)).evaluateJavascript(any(), any())

        session.close()
        verify(webView, times(2)).removeJavascriptInterface("broker")

        session.onPageStarted(webView, LOGIN_URL)
        session.onPageFinished(webView)

        verify(webView, times(2)).addJavascriptInterface(any(), eq("broker"))
        verify(webView, times(2)).evaluateJavascript(any(), any())
        verify(webView, times(2)).removeJavascriptInterface("broker")
    }

    @Test
    fun `http default port selects WebMessageListener`() {
        val adapter = FakeWebKitAdapter()
        val session = AuthUxBridgeSession.create(
            webView,
            "http://login.microsoftonline.com/common/oauth2/authorize",
            true,
            false,
            true,
            true,
            adapter
        )

        assertEquals(AuthUxTransportMode.WEB_MESSAGE_LISTENER, session.mode)
        assertTrue(adapter.listenerAdded)
    }

    @Test
    fun `document script failure without handle disables request`() {
        val adapter = FakeWebKitAdapter(failScriptRegistration = true)

        val session = AuthUxBridgeSession.create(
            webView,
            LOGIN_URL,
            true,
            false,
            true,
            true,
            adapter
        )

        assertEquals(AuthUxTransportMode.DISABLED, session.mode)
        assertTrue(adapter.listenerRemoved)
        verify(webView, never()).addJavascriptInterface(any(), eq("broker"))
    }

    @Test
    fun `document script post registration failure disables request`() {
        val adapter = FakeWebKitAdapter(failAfterScriptRegistration = true)

        val session = AuthUxBridgeSession.create(
            webView,
            LOGIN_URL,
            true,
            false,
            true,
            true,
            adapter
        )

        assertEquals(AuthUxTransportMode.DISABLED, session.mode)
        assertTrue(adapter.scriptAdded)
        assertTrue(adapter.listenerRemoved)
        verify(webView, never()).addJavascriptInterface(any(), eq("broker"))
    }

    @Test
    fun `listener registration failure rolls back before JavaScript interface`() {
        val operations = mutableListOf<String>()
        org.mockito.Mockito.doAnswer {
            operations.add("legacy-interface-added")
            null
        }.`when`(webView).addJavascriptInterface(any(), eq("broker"))
        val adapter = FakeWebKitAdapter(
            failListenerRegistration = true,
            operationLog = operations
        )

        val session = AuthUxBridgeSession.create(
            webView,
            LOGIN_URL,
            true,
            false,
            true,
            true,
            adapter
        )

        assertEquals(AuthUxTransportMode.JAVASCRIPT_INTERFACE, session.mode)
        assertFalse(adapter.scriptAdded)
        assertTrue(adapter.listenerRemoved)
        assertEquals(
            listOf(
                "listener-add-attempt",
                "listener-remove-attempt",
                "listener-removed",
                "legacy-interface-added"
            ),
            operations
        )
    }

    @Test
    fun `listener post registration failure rolls back before JavaScript interface`() {
        val operations = mutableListOf<String>()
        org.mockito.Mockito.doAnswer {
            operations.add("legacy-interface-added")
            null
        }.`when`(webView).addJavascriptInterface(any(), eq("broker"))
        val adapter = FakeWebKitAdapter(
            failAfterListenerRegistration = true,
            operationLog = operations
        )

        val session = AuthUxBridgeSession.create(
            webView,
            LOGIN_URL,
            true,
            false,
            true,
            true,
            adapter
        )

        assertEquals(AuthUxTransportMode.JAVASCRIPT_INTERFACE, session.mode)
        assertTrue(adapter.listenerAdded)
        assertFalse(adapter.scriptAdded)
        assertTrue(adapter.listenerRemoved)
        assertEquals(
            listOf(
                "listener-add-attempt",
                "listener-added",
                "listener-remove-attempt",
                "listener-removed",
                "legacy-interface-added"
            ),
            operations
        )
    }

    @Test
    fun `uncertain listener rollback disables request`() {
        val adapter = FakeWebKitAdapter(
            failAfterListenerRegistration = true,
            failListenerRemoval = true
        )

        val session = AuthUxBridgeSession.create(
            webView,
            LOGIN_URL,
            true,
            false,
            true,
            true,
            adapter
        )

        assertEquals(AuthUxTransportMode.DISABLED, session.mode)
        assertTrue(adapter.listenerAdded)
        assertFalse(adapter.scriptAdded)
        verify(webView, never()).addJavascriptInterface(any(), eq("broker"))
    }

    @Test
    fun `non-broker request without recorder is disabled`() {
        listOf(false, true).forEach { preferWebMessageListener ->
            val adapter = FakeWebKitAdapter()
            val session = AuthUxBridgeSession.create(
                webView = webView,
                initialUrl = LOGIN_URL,
                isBrokerAuthProcess = false,
                hasOnboardingTelemetryRecorder = false,
                brokerMasterFlightEnabled = true,
                preferWebMessageListener = preferWebMessageListener,
                webKitAdapter = adapter
            )

            assertEquals(AuthUxTransportMode.DISABLED, session.mode)
            assertEquals(0, adapter.listenerSupportCheckCount)
            assertEquals(0, adapter.scriptSupportCheckCount)
            assertFalse(adapter.listenerAdded)
            assertFalse(adapter.scriptAdded)
            session.onPageStarted(webView, LOGIN_URL)
            session.onPageFinished(webView)
            verify(webView, never()).addJavascriptInterface(any(), eq("broker"))
            verify(webView, never()).evaluateJavascript(any(), any())
            session.close()
        }
    }

    @Test
    fun `non-broker request with recorder is eligible for WebMessageListener telemetry`() {
        val adapter = FakeWebKitAdapter()

        val session = AuthUxBridgeSession.create(
            webView,
            LOGIN_URL,
            false,
            true,
            true,
            true,
            adapter
        )

        assertEquals(AuthUxTransportMode.WEB_MESSAGE_LISTENER, session.mode)
        assertTrue(adapter.listenerAdded)
    }

    @Test
    fun `close is idempotent and removes WebMessageListener components`() {
        val adapter = FakeWebKitAdapter()
        val session = AuthUxBridgeSession.create(
            webView,
            LOGIN_URL,
            true,
            false,
            true,
            true,
            adapter
        )

        session.close()
        session.close()

        assertEquals(1, adapter.scriptRemoveCount)
        assertEquals(1, adapter.listenerRemoveCount)
    }

    @Test
    fun `WebMessageListener facade keeps public contract and top frame guard`() {
        val script = AuthUxBridgeSession.getWebMessageListenerFacadeScript()

        assertTrue(script.contains("window.top !== window.self"))
        assertTrue(script.contains("window.broker"))
        assertTrue(script.contains("broker.postMessageToBroker"))
        assertTrue(script.contains("window.__authUxNativeBridge.postMessage"))
        assertTrue(script.contains("JSON.stringify(message)"))
    }

    private fun assertBrokerMasterFlightDisabled(
        hasOnboardingTelemetryRecorder: Boolean
    ) {
        listOf(false, true).forEach { preferWebMessageListener ->
            val adapter = FakeWebKitAdapter()
            val session = AuthUxBridgeSession.create(
                webView = webView,
                initialUrl = LOGIN_URL,
                isBrokerAuthProcess = true,
                hasOnboardingTelemetryRecorder = hasOnboardingTelemetryRecorder,
                brokerMasterFlightEnabled = false,
                preferWebMessageListener = preferWebMessageListener,
                webKitAdapter = adapter
            )

            assertEquals(AuthUxTransportMode.DISABLED, session.mode)
            assertEquals(0, adapter.listenerSupportCheckCount)
            assertEquals(0, adapter.scriptSupportCheckCount)
            assertFalse(adapter.listenerAdded)
            assertFalse(adapter.scriptAdded)

            session.onPageStarted(webView, LOGIN_URL)
            session.onPageFinished(webView)

            verify(webView, never()).addJavascriptInterface(any(), eq("broker"))
            verify(webView, never()).evaluateJavascript(any(), any())
            verify(webView, never()).removeJavascriptInterface("broker")
            session.close()
        }
    }

    private class FakeWebKitAdapter(
        private val listenerSupported: Boolean = true,
        private val documentStartSupported: Boolean = true,
        private val failListenerRegistration: Boolean = false,
        private val failAfterListenerRegistration: Boolean = false,
        private val failScriptRegistration: Boolean = false,
        private val failAfterScriptRegistration: Boolean = false,
        private val failListenerRemoval: Boolean = false,
        private val operationLog: MutableList<String>? = null
    ) : AuthUxWebKitAdapter {
        private val scriptHandler = mock(ScriptHandler::class.java)
        var listenerAdded = false
        var scriptAdded = false
        var listenerRemoved = false
        var listenerRemoveCount = 0
        var scriptRemoveCount = 0
        var listenerSupportCheckCount = 0
        var scriptSupportCheckCount = 0
        var listenerAllowedOrigins: Set<String>? = null
        var scriptAllowedOrigins: Set<String>? = null

        init {
            org.mockito.Mockito.doAnswer {
                scriptRemoveCount++
                null
            }.`when`(scriptHandler).remove()
        }

        override fun isWebMessageListenerSupported(): Boolean {
            listenerSupportCheckCount++
            return listenerSupported
        }

        override fun isDocumentStartScriptSupported(): Boolean {
            scriptSupportCheckCount++
            return documentStartSupported
        }

        override fun addWebMessageListener(
            webView: WebView,
            objectName: String,
            allowedOriginRules: Set<String>,
            listener: WebViewCompat.WebMessageListener
        ) {
            operationLog?.add("listener-add-attempt")
            if (failListenerRegistration) {
                throw IllegalStateException("listener registration failed")
            }
            listenerAdded = true
            listenerAllowedOrigins = allowedOriginRules
            operationLog?.add("listener-added")
            if (failAfterListenerRegistration) {
                throw IllegalStateException("listener registration failed after installation")
            }
        }

        override fun removeWebMessageListener(webView: WebView, objectName: String) {
            listenerRemoveCount++
            operationLog?.add("listener-remove-attempt")
            if (failListenerRemoval) {
                throw IllegalStateException("listener removal failed")
            }
            listenerRemoved = true
            operationLog?.add("listener-removed")
        }

        override fun addDocumentStartScript(
            webView: WebView,
            script: String,
            allowedOriginRules: Set<String>
        ): ScriptHandler {
            if (failScriptRegistration) {
                throw IllegalStateException("script registration failed")
            }
            scriptAdded = true
            scriptAllowedOrigins = allowedOriginRules
            if (failAfterScriptRegistration) {
                throw IllegalStateException("script registration failed after installation")
            }
            return scriptHandler
        }
    }

    companion object {
        private const val LOGIN_URL = "https://login.microsoftonline.com/common/oauth2/authorize"
        private const val ENCODED_CLAIMS_URL =
            "$LOGIN_URL?claims=%7B%22access_token%22%3A%7B%7D%7D"
        private const val FEDERATED_URL = "https://federation.example.com/login"
    }
}
