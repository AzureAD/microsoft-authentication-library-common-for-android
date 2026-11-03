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
import androidx.webkit.ScriptHandler
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import com.microsoft.identity.common.internal.broker.AuthUxJavaScriptInterface
import com.microsoft.identity.common.internal.broker.AuthUxTelemetryEvent
import com.microsoft.identity.common.internal.broker.AuthUxTelemetrySink
import com.microsoft.identity.common.internal.numberMatch.NumberMatchHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import java.util.UUID

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
            telemetrySink = null,
            brokerAuthUxBridgeEnabled = true,
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
            telemetrySink = null,
            brokerAuthUxBridgeEnabled = true,
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
    fun `broker request is disabled when bridge enablement is off`() {
        assertBrokerAuthUxBridgeDisabled(hasOnboardingTelemetryRecorder = false)
    }

    @Test
    fun `broker recorder does not bypass disabled bridge`() {
        assertBrokerAuthUxBridgeDisabled(hasOnboardingTelemetryRecorder = true)
    }

    @Test
    fun `listener-only support selects JavaScript interface`() {
        val adapter = FakeWebKitAdapter(documentStartSupported = false)

        val session = AuthUxBridgeSession.create(
            webView,
            LOGIN_URL,
            true,
            null,
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
                null,
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
            null,
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
                null,
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
            null,
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
            null,
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
            null,
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
            null,
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
            null,
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
            null,
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
            null,
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
            null,
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
            null,
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
                telemetrySink = null,
                brokerAuthUxBridgeEnabled = true,
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
            RecordingTelemetrySink(),
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
            null,
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

    private fun assertBrokerAuthUxBridgeDisabled(
        hasOnboardingTelemetryRecorder: Boolean
    ) {
        listOf(false, true).forEach { preferWebMessageListener ->
            val adapter = FakeWebKitAdapter()
            val session = AuthUxBridgeSession.create(
                webView = webView,
                initialUrl = LOGIN_URL,
                isBrokerAuthProcess = true,
                telemetrySink = if (hasOnboardingTelemetryRecorder) {
                    RecordingTelemetrySink()
                } else {
                    null
                },
                brokerAuthUxBridgeEnabled = false,
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

    @Test
    fun `JavaScript interface forwards every accepted telemetry occurrence to supplied sink`() {
        val sink = RecordingTelemetrySink()
        val session = AuthUxBridgeSession.create(
            webView,
            LOGIN_URL,
            false,
            sink,
            true,
            true,
            FakeWebKitAdapter(listenerSupported = false)
        )
        val processorCaptor = ArgumentCaptor.forClass(Any::class.java)
        verify(webView).addJavascriptInterface(processorCaptor.capture(), eq("broker"))
        val processor = processorCaptor.value as com.microsoft.identity.common.internal.broker.AuthUxJavaScriptInterface
        val payload = """
            {
              "correlationID":"correlation-id",
              "action_name":"log_telemetry",
              "action_component":"host",
              "params":{"errorCode":"530003"}
            }
        """.trimIndent()

        processor.process(payload)
        processor.process(payload)

        assertEquals(AuthUxTransportMode.JAVASCRIPT_INTERFACE, session.mode)
        assertEquals(listOf("530003", "530003"), sink.events.map { it.errorCode })
    }

    /**
     * Covers the preferred listener transport with both required AndroidX features supported.
     * Messages enter the session-installed callback from an allowed origin in the main frame.
     */
    @Test
    fun `broker page delivers telemetry and number matching through one WebMessageListener session`() {
        assertBrokerPageSharesTransport(AuthUxTransportMode.WEB_MESSAGE_LISTENER)
    }

    /**
     * Covers explicit JavaScript-interface selection despite both AndroidX features being supported.
     * Messages enter through the annotated method on the object registered with the WebView.
     */
    @Test
    fun `broker page delivers telemetry and number matching through one JavaScript interface session`() {
        assertBrokerPageSharesTransport(AuthUxTransportMode.JAVASCRIPT_INTERFACE)
    }

    /**
     * Sends one telemetry message followed by one Number Matching message on one Broker session,
     * verifying isolated side effects and unchanged transport installation after each delivery.
     *
     * One mocked WebView and approved initial URL simulate the same page without navigation.
     * This exercises installed native entry points, not webpage JavaScript or facade execution.
     * Correlation assertions verify payload preservation, not rejection of mismatched request IDs.
     */
    private fun assertBrokerPageSharesTransport(mode: AuthUxTransportMode) {
        val adapter = FakeWebKitAdapter()
        val sink = RecordingTelemetrySink()
        val sessionId = UUID.randomUUID().toString()
        val correlationId = UUID.randomUUID().toString()
        val initialStore = NumberMatchHelper.numberMatchMap.toMap()
        val useListener = mode == AuthUxTransportMode.WEB_MESSAGE_LISTENER
        val session = AuthUxBridgeSession.create(
            webView = webView,
            initialUrl = LOGIN_URL,
            isBrokerAuthProcess = true,
            telemetrySink = sink,
            brokerAuthUxBridgeEnabled = true,
            preferWebMessageListener = useListener,
            webKitAdapter = adapter
        )

        try {
            val deliver: (String) -> Unit = if (useListener) {
                val listener = requireNotNull(adapter.installedListener)
                val replyProxy = mock(JavaScriptReplyProxy::class.java)
                val sendMessage: (String) -> Unit = { payload ->
                    listener.onPostMessage(
                        webView,
                        WebMessageCompat(payload),
                        Uri.parse("https://login.microsoftonline.com"),
                        true,
                        replyProxy
                    )
                }
                sendMessage
            } else {
                val captor = ArgumentCaptor.forClass(AuthUxJavaScriptInterface::class.java)
                verify(webView).addJavascriptInterface(captor.capture(), eq("broker"))
                captor.value::receiveAuthUxMessage
            }

            fun assertTransportUnchanged(phase: String) {
                assertEquals("$phase: selected transport", mode, session.mode)
                assertEquals(
                    "$phase: listener installation count",
                    if (useListener) 1 else 0,
                    adapter.listenerAddCount
                )
                assertEquals(
                    "$phase: document-start script installation count",
                    if (useListener) 1 else 0,
                    adapter.scriptAddCount
                )
                assertEquals("$phase: unexpected listener removal", 0, adapter.listenerRemoveCount)
                assertEquals("$phase: unexpected script removal", 0, adapter.scriptRemoveCount)
                verify(webView, times(if (useListener) 0 else 1))
                    .addJavascriptInterface(any(), any())
                verify(webView, never()).removeJavascriptInterface(any())
            }

            val telemetryPayload = """
                {
                  "correlationID":"$correlationId",
                  "action_name":"${AuthUxJavaScriptInterface.ActionNames.LOG_TELEMETRY}",
                  "action_component":"host",
                  "params":{
                    "v":1,
                    "sessionID":"$sessionId",
                    "errorCode":"530003",
                    "pageId":"ConvergedTFA",
                    "trackingId":"tracking-id"
                  }
                }
            """.trimIndent()
            val numberMatchingPayload = """
                {
                  "correlationID":"$correlationId",
                  "action_name":"write_data",
                  "action_component":"broker",
                  "params":{
                    "operation":"${AuthUxJavaScriptInterface.OperationNames.NUMBER_MATCHING}",
                    "sessionID":"$sessionId",
                    "code_match":"42"
                  }
                }
            """.trimIndent()
            val expectedEvents = listOf(
                AuthUxTelemetryEvent(
                    correlationId = correlationId,
                    errorCode = "530003",
                    sessionId = sessionId,
                    pageId = "ConvergedTFA",
                    trackingId = "tracking-id",
                    version = "1"
                )
            )

            assertTransportUnchanged("After session creation")
            deliver(telemetryPayload)
            assertEquals(
                "Telemetry must deliver exactly one event with the expected context",
                expectedEvents,
                sink.events
            )
            assertEquals(
                "Telemetry must not modify the Number Matching store",
                initialStore,
                NumberMatchHelper.numberMatchMap
            )
            assertTransportUnchanged("After telemetry")

            deliver(numberMatchingPayload)
            assertEquals(
                "Number Matching must update only the expected session entry",
                initialStore + (sessionId to "42"),
                NumberMatchHelper.numberMatchMap
            )
            assertEquals(
                "Number Matching must not invoke the telemetry sink",
                expectedEvents,
                sink.events
            )
            assertTransportUnchanged("After Number Matching")
        } finally {
            try {
                session.close()
            } finally {
                val previousValue = initialStore[sessionId]
                if (previousValue == null) {
                    NumberMatchHelper.numberMatchMap.remove(sessionId)
                } else {
                    NumberMatchHelper.numberMatchMap[sessionId] = previousValue
                }
            }
        }
    }

    private class RecordingTelemetrySink : AuthUxTelemetrySink {
        val events = mutableListOf<AuthUxTelemetryEvent>()

        override fun tryConsumeAuthUxTelemetry(event: AuthUxTelemetryEvent): Boolean {
            events.add(event)
            return true
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
        var installedListener: WebViewCompat.WebMessageListener? = null
        var listenerAddCount = 0
        var scriptAddCount = 0
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
            installedListener = listener
            listenerAddCount++
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
            scriptAddCount++
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
