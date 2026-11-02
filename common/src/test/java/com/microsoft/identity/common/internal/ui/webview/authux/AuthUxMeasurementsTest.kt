// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.common.internal.ui.webview.authux

import android.net.Uri
import android.webkit.WebView
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import com.microsoft.identity.common.internal.broker.AuthUxJavaScriptInterface
import com.microsoft.identity.common.internal.broker.AuthUxMessageProcessor
import com.microsoft.identity.common.internal.broker.AuthUxRequestCapabilities
import com.microsoft.identity.common.internal.broker.AuthUxTelemetrySink
import com.microsoft.identity.common.internal.numberMatch.NumberMatchHelper
import com.microsoft.identity.common.java.opentelemetry.SpanExtension
import com.microsoft.identity.common.internal.ui.webview.authux.AuthUxMeasurementObserver.BridgeAttribute
import com.microsoft.identity.common.internal.ui.webview.authux.AuthUxMeasurementObserver.Event
import com.microsoft.identity.common.internal.ui.webview.authux.AuthUxMeasurementObserver.Value
import io.opentelemetry.api.trace.Span
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthUxMeasurementsTest {
    private val owner = RecordingObserver()
    private val observation = AuthUxBridgeTelemetry.wrap(owner)
    private val payload = """{"correlationID":"cid","action_name":"write_data",
        "action_component":"broker","params":{"operation":"number_matching",
        "sessionID":"abcd1234","code_match":"42"}}"""

    @After fun cleanup() { NumberMatchHelper.clearNumberMatchMap() }

    @Test fun `throwing observer never prevents functional processing`() {
        val broken = object : AuthUxMeasurementObserver {
            override fun admitCallback(): Boolean = error("observer")
            override fun completeCallback() = error("observer")
            override fun finish() = error("observer")
            override fun setupFinished(active: Boolean) = error("observer")
            override fun state(attribute: BridgeAttribute, value: Value) = error("observer")
            override fun count(event: Event) = error("observer")
        }
        val guarded = AuthUxBridgeTelemetry.wrap(broken)
        guarded.admitCallback().count(Event.RECEIVED)
        guarded.completeCallback()
        guarded.finish()
        guarded.setupFinished(false)
        guarded.state(BridgeAttribute.AUTH_UX_BRIDGE_FLIGHT, Value.ON)
        AuthUxJavaScriptInterface.createWithTelemetry(null, AuthUxRequestCapabilities.BROKER, guarded).process(payload)
        assertEquals("42", NumberMatchHelper.numberMatchMap["abcd1234"])
    }

    @Test fun `throwing counter after successful admission preserves functional processing`() {
        var admissions = 0
        var completions = 0
        val attemptedCounts = mutableListOf<Event>()
        val broken = object : AuthUxMeasurementObserver {
            override fun admitCallback(): Boolean {
                admissions++
                return true
            }
            override fun count(event: Event) {
                attemptedCounts.add(event)
                error("counter")
            }
            override fun state(attribute: BridgeAttribute, value: Value) = Unit
            override fun completeCallback() { completions++ }
        }
        val guarded = AuthUxBridgeTelemetry.wrap(broken)

        AuthUxJavaScriptInterface.createWithTelemetry(null, AuthUxRequestCapabilities.BROKER, guarded)
            .process(payload)

        assertEquals(1, admissions)
        assertEquals(listOf(Event.NUMBER_MATCH_IDENTIFIED), attemptedCounts)
        assertEquals(1, completions)
        assertEquals("42", NumberMatchHelper.numberMatchMap["abcd1234"])
    }

    @Test fun `modern dispatch is counted once and legacy process contract is preserved`() {
        val processor = AuthUxJavaScriptInterface.createWithTelemetry(null, AuthUxRequestCapabilities.BROKER, observation)
        val policy = mock(AuthUxOriginPolicy::class.java)
        val origin = Uri.parse("https://login.microsoftonline.com")
        `when`(policy.isAllowedCallback(origin, true)).thenReturn(true)
        val listener = AuthUxWebMessageListener(processor, policy, observation)
        listener.onPostMessage(mock(WebView::class.java), WebMessageCompat(payload), origin,
            true, mock(JavaScriptReplyProxy::class.java))
        processor.receiveAuthUxMessage(payload)
        assertEquals(2, owner.counts[Event.RECEIVED])
        assertEquals(2, owner.processed())
        assertEquals(2, owner.counts[Event.NUMBER_MATCH_IDENTIFIED])
        assertEquals("42", NumberMatchHelper.numberMatchMap["abcd1234"])
        AuthUxWebMessageListener::class.java.getConstructor(
            AuthUxMessageProcessor::class.java, AuthUxOriginPolicy::class.java)
        AuthUxJavaScriptInterface::class.java.getConstructor()
        AuthUxJavaScriptInterface::class.java.getConstructor(AuthUxTelemetrySink::class.java)
        AuthUxJavaScriptInterface::class.java.getConstructor(
            AuthUxTelemetrySink::class.java, AuthUxRequestCapabilities::class.java)
        assertEquals(Void.TYPE, AuthUxMessageProcessor::class.java
            .getMethod("process", String::class.java).returnType)
    }

    @Test fun `mixed callbacks derive processed totals without classifying store outcomes`() {
        val processor = AuthUxJavaScriptInterface.createWithTelemetry(null, AuthUxRequestCapabilities.BROKER, observation)
        processor.process(payload)
        processor.process(payload.replace("\"42\"", "\"invalid\""))
        processor.process("{")
        processor.close()
        processor.process(payload)
        assertEquals(4, owner.counts[Event.RECEIVED])
        assertEquals(2, owner.processed())
        assertEquals(2, owner.counts[Event.NUMBER_MATCH_IDENTIFIED])
        assertEquals("42", NumberMatchHelper.numberMatchMap["abcd1234"])
    }

    @Test fun `unobserved processor success adds no rejection`() {
        val processor = mock(AuthUxMessageProcessor::class.java)
        val listener = AuthUxWebMessageListener(processor, AuthUxOriginPolicy(), observation)
        listener.onPostMessage(mock(WebView::class.java), WebMessageCompat(payload),
            Uri.parse("https://login.microsoftonline.com"), true,
            mock(JavaScriptReplyProxy::class.java))
        verify(processor).process(payload)
        assertEquals(1, owner.counts[Event.RECEIVED])
        assertEquals(1, owner.processed())
    }

    @Test fun `native rejection is one received and no processing`() {
        val processor = mock(AuthUxMessageProcessor::class.java)
        val listener = AuthUxWebMessageListener(processor, AuthUxOriginPolicy(), observation)
        listener.onPostMessage(mock(WebView::class.java), WebMessageCompat(payload),
            Uri.parse("https://evil.example"), false, mock(JavaScriptReplyProxy::class.java))
        assertEquals(1, owner.counts[Event.RECEIVED])
        assertEquals(1, owner.counts[Event.REJECTED_ORIGIN])
        assertEquals(0, owner.processed())
        verifyNoInteractions(processor)
    }

    @Test fun `closed malformed unsupported and capability rejection are bounded`() {
        val processor = AuthUxJavaScriptInterface.createWithTelemetry(null, AuthUxRequestCapabilities(false, false), observation)
        processor.process("{")
        processor.process(payload)
        processor.process(payload.replace("number_matching", "unknown"))
        processor.close()
        processor.process(payload)
        assertEquals(4, owner.counts[Event.RECEIVED])
        assertEquals(1, owner.counts[Event.REJECTED_MALFORMED])
        assertEquals(1, owner.counts[Event.REJECTED_CAPABILITY])
        assertEquals(1, owner.counts[Event.REJECTED_UNSUPPORTED])
        assertEquals(1, owner.counts[Event.REJECTED_CLOSED])
        assertEquals(0, owner.processed())
        assertTrue(NumberMatchHelper.numberMatchMap.isEmpty())
    }

    @Test fun `identification precedes legacy span writes but respects telemetry precedence`() {
        val span = mock(Span::class.java)
        `when`(span.setAttribute("authux_js_action_name", "write_data"))
            .thenThrow(IllegalStateException("old span"))
        mockStatic(SpanExtension::class.java).use { current ->
            current.`when`<Span> { SpanExtension.current() }.thenReturn(span)
            val processor = AuthUxJavaScriptInterface.createWithTelemetry(
                null, AuthUxRequestCapabilities.BROKER, observation)
            processor.process(payload)
            processor.process(payload.replace("write_data", "log_telemetry"))
        }
        assertEquals(1, owner.counts[Event.NUMBER_MATCH_IDENTIFIED])
        assertTrue(NumberMatchHelper.numberMatchMap.isEmpty())
        assertEquals(1, owner.counts[Event.REJECTED_PROCESSING])
        assertEquals(1, owner.counts[Event.REJECTED_MALFORMED])
    }

    @Test fun `binary and null native callbacks each have exactly one terminal outcome`() {
        val processor = mock(AuthUxMessageProcessor::class.java)
        val listener = AuthUxWebMessageListener(processor, AuthUxOriginPolicy(), observation)
        val binary = mock(WebMessageCompat::class.java)
        `when`(binary.type).thenReturn(WebMessageCompat.TYPE_ARRAY_BUFFER)
        val absent = mock(WebMessageCompat::class.java)
        `when`(absent.type).thenReturn(WebMessageCompat.TYPE_STRING)
        for (message in listOf(binary, absent)) {
            listener.onPostMessage(mock(WebView::class.java), message,
                Uri.parse("https://login.microsoftonline.com"), true,
                mock(JavaScriptReplyProxy::class.java))
        }
        assertEquals(2, owner.counts[Event.RECEIVED])
        assertEquals(1, owner.counts[Event.REJECTED_TYPE])
        assertEquals(1, owner.counts[Event.REJECTED_MALFORMED])
        verifyNoInteractions(processor)
        assertEquals(0, owner.processed())
    }

    @Test fun `original Kotlin default entry points remain callable`() {
        val constructor = AuthUxJavaScriptInterface::class.java.getConstructor(
            AuthUxTelemetrySink::class.java, AuthUxRequestCapabilities::class.java,
            Int::class.javaPrimitiveType, Class.forName("kotlin.jvm.internal.DefaultConstructorMarker"))
        val processor = constructor.newInstance(null, null, 3, null)
        processor.process(payload)
        assertEquals("42", NumberMatchHelper.numberMatchMap["abcd1234"])

        val companion = AuthUxBridgeSession.Companion
        val factory = companion.javaClass.getDeclaredMethod(
            "create\$default", companion.javaClass, WebView::class.java, String::class.java,
            Boolean::class.javaPrimitiveType, AuthUxTelemetrySink::class.java,
            Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType,
            AuthUxWebKitAdapter::class.java, AuthUxOriginPolicy::class.java,
            Int::class.javaPrimitiveType, Any::class.java)
        val session = factory.invoke(null, companion, mock(WebView::class.java), null,
            true, null, false, false, null, null, 192, null) as AuthUxBridgeSession
        assertEquals(AuthUxTransportMode.DISABLED, session.mode)
        session.close()
    }

    @Test fun `post mutation failure counts one rejected callback without undoing mutation`() {
        val span = mock(Span::class.java)
        `when`(span.setAttribute("stored_number_match_entry", true)).thenThrow(IllegalStateException())
        mockStatic(SpanExtension::class.java).use { current ->
            current.`when`<Span> { SpanExtension.current() }.thenReturn(span)
            AuthUxJavaScriptInterface.createWithTelemetry(null, AuthUxRequestCapabilities.BROKER, observation)
                .process(payload)
        }
        assertEquals("42", NumberMatchHelper.numberMatchMap["abcd1234"])
        assertEquals(1, owner.counts[Event.RECEIVED])
        assertEquals(1, owner.counts[Event.NUMBER_MATCH_IDENTIFIED])
        assertEquals(1, owner.counts[Event.REJECTED_PROCESSING])
        assertEquals(0, owner.processed())
    }

    @Test fun `escaping functional error is preserved with one terminal observation`() {
        val failure = AssertionError("functional failure")
        mockStatic(SpanExtension::class.java).use { current ->
            current.`when`<Span> { SpanExtension.current() }.thenThrow(failure)
            try {
                AuthUxJavaScriptInterface.createWithTelemetry(null, AuthUxRequestCapabilities.BROKER, observation)
                    .process(payload)
                fail("Functional error must escape")
            } catch (actual: AssertionError) {
                assertSame(failure, actual)
            }
        }
        assertEquals(1, owner.counts[Event.RECEIVED])
        assertEquals(1, owner.counts[Event.REJECTED_PROCESSING])
        assertEquals(1, owner.counts[Event.NUMBER_MATCH_IDENTIFIED])
        assertEquals(0, owner.processed())
        assertTrue(NumberMatchHelper.numberMatchMap.isEmpty())
    }

    private class RecordingObserver : AuthUxMeasurementObserver {
        val counts = mutableMapOf<Event, Int>()
        fun processed() = (counts[Event.RECEIVED] ?: 0) -
            counts.filterKeys { it.name.startsWith("REJECTED_") }.values.sum()
        override fun admitCallback(): Boolean {
            count(Event.RECEIVED)
            return true
        }
        override fun state(attribute: BridgeAttribute, value: Value) = Unit
        override fun count(event: Event) { counts[event] = (counts[event] ?: 0) + 1 }
    }
}
