// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.common.internal.ui.webview.authux

import android.net.Uri
import android.webkit.WebView
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.ScriptHandler
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import com.microsoft.identity.common.internal.broker.AuthUxJavaScriptInterface
import com.microsoft.identity.common.internal.broker.AuthUxRequestCapabilities
import com.microsoft.identity.common.internal.broker.AuthUxTelemetrySink
import com.microsoft.identity.common.internal.ui.webview.authux.AuthUxMeasurementObserver.BridgeAttribute
import com.microsoft.identity.common.internal.ui.webview.authux.AuthUxMeasurementObserver.BridgeCounter
import com.microsoft.identity.common.internal.ui.webview.authux.AuthUxMeasurementObserver.Value
import com.microsoft.identity.common.java.opentelemetry.OTelUtility
import com.microsoft.identity.common.java.logging.Logger
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class AuthUxBridgeMeasurementsTest {
    private val payload = """{"correlationID":"cid","action_name":"log_telemetry",
        "action_component":"host","params":{"errorCode":"53003"}}"""
    private val url = "https://login.microsoftonline.com"

    @Test fun `request context uses caller aware factory and retains metadata until normal completion`() {
        val parent = mock(SpanContext::class.java)
        val span = mock(Span::class.java)
        mockStatic(OTelUtility::class.java).use { factory ->
            factory.`when`<Span> {
                OTelUtility.createSpanFromParent(AuthUxBridgeMeasurements.SPAN_NAME, parent, "com.example.caller")
            }.thenReturn(span)
            val observation = AuthUxBridgeMeasurements.start(parent, "tenant", "request-cid", "com.example.caller")
            factory.verify {
                OTelUtility.createSpanFromParent(AuthUxBridgeMeasurements.SPAN_NAME, parent, "com.example.caller")
            }
            factory.verifyNoMoreInteractions()
            verify(span).setAttribute("tenant_id", "tenant")
            verify(span).setAttribute("correlation_id", "request-cid")
            observation.setupFinished(true)
            verify(span, never()).end()
            observation.finish()
            observation.finish()
            verify(span).end()
        }
    }

    @Test fun `missing caller and request metadata use parent only factory without invented attributes`() {
        for (missing in listOf(null, "")) {
            val span = mock(Span::class.java)
            mockStatic(OTelUtility::class.java).use { factory ->
                factory.`when`<Span> {
                    OTelUtility.createSpanFromParent(AuthUxBridgeMeasurements.SPAN_NAME, null)
                }.thenReturn(span)
                val observation = AuthUxBridgeMeasurements.start(null, missing, missing, missing)
                factory.verify { OTelUtility.createSpanFromParent(AuthUxBridgeMeasurements.SPAN_NAME, null) }
                factory.verifyNoMoreInteractions()
                observation.setupFinished(false)
                verify(span, never()).setAttribute(eq("tenant_id"), anyString())
                verify(span, never()).setAttribute(eq("correlation_id"), anyString())
                verify(span).end()
            }
        }
    }

    @Test fun `request factory metadata and logging failures do not escape or lose span ownership`() {
        mockStatic(OTelUtility::class.java).use { factory ->
            factory.`when`<Span> {
                OTelUtility.createSpanFromParent(AuthUxBridgeMeasurements.SPAN_NAME, null, "caller")
            }.thenThrow(AssertionError("factory"))
            assertSame(AuthUxBridgeTelemetry.NONE, AuthUxBridgeMeasurements.start(null, "tenant", "cid", "caller"))

            val span = mock(Span::class.java)
            factory.`when`<Span> {
                OTelUtility.createSpanFromParent(AuthUxBridgeMeasurements.SPAN_NAME, null, "caller")
            }.thenReturn(span)
            `when`(span.setAttribute("tenant_id", "tenant")).thenThrow(AssertionError("metadata"))
            mockStatic(Logger::class.java).use { logger ->
                logger.`when`<Unit> {
                    Logger.warn("AuthUxBridgeMeasurements", "Unable to attach bridge request metadata.")
                }.thenThrow(AssertionError("logger"))
                val observation = AuthUxBridgeMeasurements.start(null, "tenant", "cid", "caller")
                assertNotSame(AuthUxBridgeTelemetry.NONE, observation)
                verify(span).setAttribute("correlation_id", "cid")
                observation.setupFinished(true)
                observation.finish()
                observation.finish()
                verify(span).end()
            }
        }
    }

    @Test fun `started span is ended if measurement ownership cannot be established`() {
        for (endFails in listOf(false, true)) {
            val span = mock(Span::class.java)
            if (endFails) doThrow(AssertionError("end")).`when`(span).end()
            mockStatic(OTelUtility::class.java).use { factory ->
                factory.`when`<Span> {
                    OTelUtility.createSpanFromParent(AuthUxBridgeMeasurements.SPAN_NAME, null)
                }.thenReturn(span)
                mockConstruction(AuthUxBridgeMeasurements::class.java) { _, _ ->
                    throw IllegalStateException("ownership")
                }.use {
                    assertSame(AuthUxBridgeTelemetry.NONE, AuthUxBridgeMeasurements.start(null, null, null, null))
                    verify(span).end()
                }
            }
        }
    }

    /**
     * Pins all 23 wire keys independently of production registry constants and enum-name derivation.
     * Zero and distinct nonzero long counts detect missing, duplicated or swapped counter mappings.
     * Verifies the helper's Span calls, not Broker filtering or downstream ingestion.
     */
    @Test fun `all observer fields retain their independent wire names and counter types`() {
        val counterKeys = linkedMapOf(
            BridgeCounter.SETUP_FAILED to "authux_setup_failed",
            BridgeCounter.INSTALL_ATTEMPTS to "authux_install_attempts",
            BridgeCounter.INSTALL_FAILED to "authux_install_failed",
            BridgeCounter.ROLLBACK_COMPLETE to "authux_rollback_complete",
            BridgeCounter.LEGACY_INSTALLED to "authux_legacy_installed",
            BridgeCounter.RECEIVED to "authux_received",
            BridgeCounter.REJECTED_CLOSED to "authux_rejected_closed",
            BridgeCounter.REJECTED_ORIGIN to "authux_rejected_origin",
            BridgeCounter.REJECTED_TYPE to "authux_rejected_type",
            BridgeCounter.REJECTED_MALFORMED to "authux_rejected_malformed",
            BridgeCounter.REJECTED_UNSUPPORTED to "authux_rejected_unsupported",
            BridgeCounter.REJECTED_CAPABILITY to "authux_rejected_capability",
            BridgeCounter.REJECTED_PROCESSING to "authux_rejected_processing",
            BridgeCounter.NUMBER_MATCH_IDENTIFIED to "authux_number_match_identified",
            BridgeCounter.RECORDER_WRITTEN to "authux_recorder_written",
            BridgeCounter.RECORDER_EXCLUDED_CID to "authux_recorder_excluded_cid",
            BridgeCounter.RECORDER_EXCLUDED_POLICY to "authux_recorder_excluded_policy",
            BridgeCounter.RECORDER_FAILED to "authux_recorder_failed",
            BridgeCounter.RECORDER_PARTIAL to "authux_recorder_partial"
        )
        assertEquals(BridgeCounter.values().toSet(), counterKeys.keys)
        assertEquals(19, counterKeys.values.toSet().size)
        for (nonzero in listOf(false, true)) withSpan { span, observation ->
            observation.state(BridgeAttribute.AUTH_UX_BRIDGE_FLIGHT, Value.ON)
            observation.state(BridgeAttribute.WEB_MESSAGE_LISTENER_FLIGHT, Value.OFF)
            observation.state(BridgeAttribute.TRANSPORT, Value.JAVASCRIPT_INTERFACE)
            observation.state(BridgeAttribute.TRANSPORT_SELECTION_REASON, Value.JAVASCRIPT_INTERFACE_REQUESTED)
            if (nonzero) {
                counterKeys.keys.forEachIndexed { index, counter ->
                    repeat(index + 1) { observation.count(counter) }
                }
            }
            observation.setupFinished(false)

            verify(span).setAttribute("authux_auth_ux_bridge_flight", "on")
            verify(span).setAttribute("authux_web_message_listener_flight", "off")
            verify(span).setAttribute("authux_transport", "javascript_interface")
            verify(span).setAttribute("authux_transport_selection_reason", "javascript_interface_requested")
            counterKeys.values.forEachIndexed { index, key ->
                verify(span).setAttribute(key, if (nonzero) (index + 1).toLong() else 0L)
            }
            verify(span).end()
            verifyNoMoreInteractions(span)
        }
    }

    @Test fun `finish waits for setup accounting and admitted callbacks without blocking`() = withSpan { span, observation ->
        val callback = observation.admitCallback()
        observation.finish()
        observation.finish()
        observation.admitCallback().count(BridgeCounter.REJECTED_CLOSED)
        observation.setupFinished(true)
        verify(span, never()).end()
        callback.count(BridgeCounter.REJECTED_MALFORMED)
        callback.completeCallback()
        observation.finish()
        verify(span).setAttribute("authux_received", 1L)
        verify(span).setAttribute("authux_rejected_malformed", 1L)
        verify(span).setAttribute("authux_rejected_closed", 0L)
        verify(span).setAttribute("authux_setup_failed", 0L)
        verify(span).end()
    }

    @Test fun `finish with no callbacks waits for final setup attributes`() = withSpan { span, observation ->
        observation.finish()
        verifyNoInteractions(span)
        observation.state(BridgeAttribute.AUTH_UX_BRIDGE_FLIGHT, Value.ON)
        observation.state(BridgeAttribute.WEB_MESSAGE_LISTENER_FLIGHT, Value.ON)
        observation.state(BridgeAttribute.TRANSPORT, Value.WEB_MESSAGE_LISTENER)
        observation.state(BridgeAttribute.TRANSPORT_SELECTION_REASON, Value.WEB_MESSAGE_LISTENER_INSTALLED)
        verifyNoInteractions(span)

        observation.setupFinished(true)
        observation.finish()
        observation.setupFinished(true)

        verify(span).setAttribute("authux_auth_ux_bridge_flight", "on")
        verify(span).setAttribute("authux_web_message_listener_flight", "on")
        verify(span).setAttribute("authux_transport", "web_message_listener")
        verify(span).setAttribute("authux_transport_selection_reason", "web_message_listener_installed")
        verify(span).setAttribute("authux_received", 0L)
        verify(span).end()
    }

    @Test fun `disabled and failed initializations end locally once with truthful defaults`() {
        for (failed in listOf(false, true)) {
            withSpan { span, observation ->
                if (failed) observation.count(BridgeCounter.SETUP_FAILED)
                observation.setupFinished(false)
                observation.setupFinished(false)
                observation.finish()
                verify(span).setAttribute("authux_auth_ux_bridge_flight", "not_evaluated")
                verify(span).setAttribute("authux_web_message_listener_flight", "not_evaluated")
                verify(span).setAttribute("authux_transport", "not_selected")
                verify(span).setAttribute("authux_setup_failed", if (failed) 1L else 0L)
                verify(span).end()
            }
        }
    }

    @Test fun `factory and export failures are isolated`() {
        mockStatic(OTelUtility::class.java).use { factory ->
            factory.`when`<Span> { OTelUtility.createSpanFromParent(AuthUxBridgeMeasurements.SPAN_NAME, null) }
                .thenThrow(AssertionError("factory"))
            assertSame(AuthUxBridgeTelemetry.NONE, AuthUxBridgeMeasurements.start(null, null, null, null))
        }
        withSpan { span, observation ->
            `when`(span.setAttribute("authux_setup_failed", 0L)).thenThrow(AssertionError("export"))
            doThrow(AssertionError("end")).`when`(span).end()
            observation.setupFinished(false)
            observation.finish()
            verify(span).setAttribute("authux_received", 0L)
            verify(span).end()
        }
    }

    @Test fun `both transports retain handle and reentrant sink close exports after cleanup outside locks`() {
        for (modern in listOf(false, true)) withSpan { span, observation ->
            val view = mock(WebView::class.java)
            val script = mock(ScriptHandler::class.java)
            lateinit var listener: WebViewCompat.WebMessageListener
            var listenerRemoved = false
            val adapter = object : AuthUxWebKitAdapter {
                override fun isWebMessageListenerSupported() = true
                override fun isDocumentStartScriptSupported() = true
                override fun addDocumentStartScript(
                    webView: WebView, script: String, allowedOriginRules: Set<String>
                ): ScriptHandler = scriptHandler
                private val scriptHandler = script
                override fun addWebMessageListener(
                    webView: WebView, objectName: String, allowedOriginRules: Set<String>,
                    callback: WebViewCompat.WebMessageListener
                ) { listener = callback }
                override fun removeWebMessageListener(webView: WebView, objectName: String) {
                    listenerRemoved = true
                }
            }
            lateinit var session: AuthUxBridgeSession
            var cleanupComplete = false
            var exportedOutsideLocks = false
            var exportedAfterCleanup = false
            var endedInsideSink = false
            val sink = AuthUxTelemetrySink {
                session.close()
                cleanupComplete = true
                endedInsideSink = mockingDetails(span).invocations.any { it.method.name == "end" }
                true
            }
            session = AuthUxBridgeSession.createWithTelemetry(view, url, true, sink, true,
                modern, observation, adapter)
            val processor: AuthUxJavaScriptInterface = ReflectionHelpers.getField(session, "processor")
            val owner: Any = ReflectionHelpers.getField(observation, "observer")
            val observationLock: Any = ReflectionHelpers.getField(owner, "lock")
            val processorLock: Any = ReflectionHelpers.getField(processor, "lifecycleLock")
            doAnswer {
                exportedAfterCleanup = cleanupComplete
                exportedOutsideLocks = !Thread.holdsLock(observationLock) && !Thread.holdsLock(processorLock)
                null
            }.`when`(span).end()
            observation.setupFinished(true)
            if (modern) {
                listener.onPostMessage(view, WebMessageCompat(payload), Uri.parse(url),
                    true, mock(JavaScriptReplyProxy::class.java))
                listener.onPostMessage(view, WebMessageCompat(payload), Uri.parse("https://evil.example"),
                    true, mock(JavaScriptReplyProxy::class.java))
            } else {
                processor.receiveAuthUxMessage(payload)
                processor.receiveAuthUxMessage(payload)
            }
            session.close()
            verify(span).setAttribute("authux_received", 1L)
            listOf(
                "authux_rejected_closed", "authux_rejected_origin", "authux_rejected_type",
                "authux_rejected_malformed", "authux_rejected_unsupported",
                "authux_rejected_capability", "authux_rejected_processing"
            ).forEach {
                verify(span).setAttribute(it, 0L)
            }
            verify(span).end()
            assertFalse(endedInsideSink)
            assertTrue(exportedAfterCleanup)
            assertTrue(exportedOutsideLocks)
            if (modern) {
                verify(script).remove()
                assertTrue(listenerRemoved)
            } else verify(view).removeJavascriptInterface("broker")
        }
    }

    @Test fun `modern validation admitted before close drains after cleanup and late rejection is unobserved`() =
        withSpan { span, observation ->
            val entered = CountDownLatch(1)
            val resume = CountDownLatch(1)
            val policy = mock(AuthUxOriginPolicy::class.java)
            val origin = Uri.parse(url)
            `when`(policy.isAllowedCallback(origin, true)).thenAnswer {
                entered.countDown()
                check(resume.await(5, TimeUnit.SECONDS))
                false
            }
            val processor = AuthUxJavaScriptInterface.createWithTelemetry(null, AuthUxRequestCapabilities.BROKER, observation)
            val listener = AuthUxWebMessageListener(processor, policy, observation)
            val view = mock(WebView::class.java)
            val reply = mock(JavaScriptReplyProxy::class.java)
            val executor = Executors.newSingleThreadExecutor()
            observation.setupFinished(true)
            try {
                val callback = executor.submit {
                    listener.onPostMessage(view, WebMessageCompat(payload), origin, true, reply)
                }
                assertTrue(entered.await(5, TimeUnit.SECONDS))
                processor.close()
                observation.finish()
                verify(span, never()).end()
                resume.countDown()
                callback.get(5, TimeUnit.SECONDS)
                listener.onPostMessage(view, WebMessageCompat(payload), origin, true, reply)
                verify(span).setAttribute("authux_received", 1L)
                verify(span).setAttribute("authux_rejected_origin", 1L)
                verify(span).end()
            } finally {
                resume.countDown()
                executor.shutdownNow()
                assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
            }
        }

    @Test fun `escaping error still drains callback and preserves identity`() = withSpan { span, observation ->
        val failure = AssertionError("validation")
        val policy = mock(AuthUxOriginPolicy::class.java)
        `when`(policy.isAllowedCallback(Uri.parse(url), true)).thenThrow(failure)
        val processor = AuthUxJavaScriptInterface()
        val listener = AuthUxWebMessageListener(processor, policy, observation)
        observation.setupFinished(true)
        try {
            listener.onPostMessage(mock(WebView::class.java), WebMessageCompat(payload),
                Uri.parse(url), true, mock(JavaScriptReplyProxy::class.java))
            fail("Must preserve validation error")
        } catch (actual: AssertionError) { assertSame(failure, actual) }
        observation.finish()
        verify(span).setAttribute("authux_received", 1L)
        verify(span).setAttribute("authux_rejected_processing", 1L)
        verify(span).end()
    }

    private fun withSpan(block: (Span, AuthUxBridgeTelemetry) -> Unit) {
        val span = mock(Span::class.java)
        mockStatic(OTelUtility::class.java).use { factory ->
            factory.`when`<Span> { OTelUtility.createSpanFromParent(AuthUxBridgeMeasurements.SPAN_NAME, null) }.thenReturn(span)
            block(span, AuthUxBridgeMeasurements.start(null, null, null, null))
            for (removed in listOf("setup_completed", "processed", "store_attempts")) {
                verify(span, never()).setAttribute(eq("authux_$removed"), anyLong())
            }
        }
    }
}
