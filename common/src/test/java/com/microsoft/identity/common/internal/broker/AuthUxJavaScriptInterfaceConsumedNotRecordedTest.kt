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

import com.microsoft.identity.common.java.opentelemetry.AttributeName
import org.junit.Assert
import org.junit.Test

/**
 * Pins what the bridge does with a code a sink **consumed but did not record** (AB#3688632).
 *
 * A sink returns `true` for three different downstream fates — recorded, dropped by policy, refused
 * outright — and the bridge cannot tell them apart. These tests state the resulting span/log
 * behaviour explicitly so it is a decision rather than an accident, and so a future change that
 * makes the span mean "was recorded" fails loudly here.
 */
class AuthUxJavaScriptInterfaceConsumedNotRecordedTest {

    private val errorCode = "530003"

    private fun payload(code: String) = """
        {
            "correlationID": "corr-1",
            "action_name": "log_telemetry",
            "action_component": "host",
            "params": { "v": "1", "sessionID": "sess-1", "errorCode": "$code" }
        }
    """.trimIndent()

    /** A sink that consumes everything but records nothing — a pure policy-drop sink. */
    private class DroppingSink : AuthUxTelemetrySink {
        var calls = 0
        override fun tryConsumeAuthUxTelemetry(event: AuthUxTelemetryEvent): Boolean {
            calls++
            return true
        }
    }

    @Test
    fun `test a consumed-but-dropped code still lands on the span`() {
        val span = RecordingSpan()
        val sink = DroppingSink()

        span.makeCurrent().use {
            AuthUxJavaScriptInterface(sink).receiveAuthUxMessage(payload(errorCode))
        }

        Assert.assertEquals("the sink must have been offered the code", 1, sink.calls)
        // Deliberate: authux_js_error_code records WHAT THE PAGE REPORTED and a sink accepted, not
        // what downstream telemetry stored. It is namespaced to the bridge, so every value on it is
        // page-supplied by construction and no provenance is lost — unlike the onboarding blob's
        // blocking_errors list, which is SHARED with broker4j's own symbolic constants.
        Assert.assertEquals(
            "a consumed code is recorded on the bridge's own span attribute even when the sink "
                    + "dropped it; this attribute means 'page reported and a sink took it'",
            errorCode,
            span.getAttribute(AttributeName.authux_js_error_code.name)
        )
    }

    @Test
    fun `test a declined code does NOT land on the span`() {
        // The contrast that gives the attribute its meaning: NOT_CONSUMED never sets it, so the
        // attribute can never carry a code no sink took responsibility for.
        val span = RecordingSpan()
        val sink = AuthUxTelemetrySink { false }

        span.makeCurrent().use {
            AuthUxJavaScriptInterface(sink).receiveAuthUxMessage(payload(errorCode))
        }

        Assert.assertFalse(
            "a code the sink declined must not appear on the span",
            span.hasAttribute(AttributeName.authux_js_error_code.name)
        )
    }

    @Test
    fun `test an over-cap code does not reach the sink or the span`() {
        // The bridge's own drops are different from the sink's: they never reach a sink at all, so
        // they must not set the attribute either.
        val span = RecordingSpan()
        val sink = DroppingSink()
        val bridge = AuthUxJavaScriptInterface(sink)

        // Fill the distinct-code cap (10) with codes that are not the one under test.
        repeat(10) { bridge.receiveAuthUxMessage(payload("10000$it")) }
        val callsBeforeCap = sink.calls

        span.makeCurrent().use { bridge.receiveAuthUxMessage(payload("999999")) }

        Assert.assertEquals(
            "an over-cap code must never be offered to the sink",
            callsBeforeCap,
            sink.calls
        )
        Assert.assertFalse(
            "an over-cap code must not appear on the span",
            span.hasAttribute(AttributeName.authux_js_error_code.name)
        )
    }
}
