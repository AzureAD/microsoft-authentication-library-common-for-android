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

import com.microsoft.identity.common.internal.numberMatch.NumberMatchHelper
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import io.opentelemetry.api.trace.Span
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class AuthUxJavaScriptInterfaceTest {

    private lateinit var authUxJavaScriptInterface: AuthUxJavaScriptInterface

    private val mockSessionId = "12345678"
    private val mockNumberMatchValue = "00"

    private val numberMatchTestPayload = """
        {
            correlationID: w.ServerData.correlationId,
            action_name: "write_data",
            action_component: "broker",
            params: {
                operation: "number_matching",
                sessionID: $mockSessionId,
                code_match: $mockNumberMatchValue
            }
        }
    """.trimIndent()

    private val mockErrorCode = "530003"

    /** Mirrors AuthUxJavaScriptInterface.MAX_TELEMETRY_ATTEMPTS, which is private. */
    private val MAX_ATTEMPTS_FOR_TEST = 25

    // Matches the Auth UX design-doc wire format: dispatched by action_name = "log_telemetry"
    // (no params.operation), errorCode sent as a JSON number, plus additional params context
    // fields. Written as strict JSON — quoted keys — so the fixture is exactly what a real JS
    // sender's JSON.stringify would emit rather than something only a lenient parser accepts.
    private val logTelemetryTestPayload = """
        {
            "correlationID": "corr-1",
            "action_name": "log_telemetry",
            "action_component": "host",
            "params": {
                "v": 1,
                "sessionID": "sess-1",
                "errorCode": $mockErrorCode,
                "pageId": "ConvergedTFA",
                "trackingId": "track-1"
            }
        }
    """.trimIndent()

    private val logTelemetryMissingErrorCodePayload = """
        {
            "correlationID": "corr-1",
            "action_name": "log_telemetry",
            "action_component": "host",
            "params": {
                "v": 1,
                "sessionID": "sess-1",
                "pageId": "ConvergedTFA",
                "trackingId": "track-1"
            }
        }
    """.trimIndent()

    // A future Auth UX version may add new key/value pairs at the top level and inside params;
    // dispatch must tolerate them and still forward errorCode.
    private val logTelemetryWithFutureFieldsPayload = """
        {
            "correlationID": "corr-1",
            "action_name": "log_telemetry",
            "action_component": "host",
            "futureTopLevel": "ignored",
            "params": {
                "v": 2,
                "sessionID": "sess-1",
                "errorCode": $mockErrorCode,
                "pageId": "ConvergedTFA",
                "trackingId": "track-1",
                "futureField": "ignored",
                "futureNumber": 42
            }
        }
    """.trimIndent()

    @Before
    fun setUp() {
        authUxJavaScriptInterface = AuthUxJavaScriptInterface()
    }

    @After
    fun tearDown() {
        // Clear the static map after each test
        NumberMatchHelper.numberMatchMap.clear()
    }

    @Test
    fun `test receiveAuthUxMessage with NUMBER_MATCH function`() {
        // Call the method
        authUxJavaScriptInterface.receiveAuthUxMessage(numberMatchTestPayload)

        // Verify that the data was stored in NumberMatchHelper
        val storedValue = NumberMatchHelper.numberMatchMap[mockSessionId]
        Assert.assertTrue(storedValue == mockNumberMatchValue)
    }

    @Test
    fun `test receiveAuthUxMessage with empty json`() {
        // Call the method
        authUxJavaScriptInterface.receiveAuthUxMessage("{}")

        // Should not get an exception
    }

    @Test
    fun `test receiveAuthUxMessage with non-json string`() {
        // Call the method
        authUxJavaScriptInterface.receiveAuthUxMessage("NotAJson")

        // Should not get an exception
    }

    @Test
    fun `test receiveAuthUxMessage with log_telemetry forwards errorCode to sink exactly once`() {
        val sink = RecordingTelemetrySink()
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        interfaceWithSink.receiveAuthUxMessage(logTelemetryTestPayload)

        // The opaque error code is forwarded to the sink exactly once...
        Assert.assertEquals(listOf(mockErrorCode), sink.received)
        // ...and H3 holds: the telemetry path never touches the number-match device store.
        Assert.assertTrue(NumberMatchHelper.numberMatchMap.isEmpty())
    }

    @Test
    fun `test receiveAuthUxMessage with log_telemetry and missing errorCode is a no-op`() {
        val sink = RecordingTelemetrySink()
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        interfaceWithSink.receiveAuthUxMessage(logTelemetryMissingErrorCodePayload)

        // Absent errorCode must not throw and must not emit telemetry.
        Assert.assertTrue(sink.received.isEmpty())
    }

    @Test
    fun `test receiveAuthUxMessage with log_telemetry tolerates unknown future fields`() {
        val sink = RecordingTelemetrySink()
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        interfaceWithSink.receiveAuthUxMessage(logTelemetryWithFutureFieldsPayload)

        // New top-level and params key/value pairs must not break dispatch; errorCode still forwarded.
        Assert.assertEquals(listOf(mockErrorCode), sink.received)
        Assert.assertTrue(NumberMatchHelper.numberMatchMap.isEmpty())
    }

    @Test
    fun `test receiveAuthUxMessage with log_telemetry does not throw when no sink attached`() {
        // Default no-arg construction (no sink): parses and validates without side effects or throwing.
        authUxJavaScriptInterface.receiveAuthUxMessage(logTelemetryTestPayload)

        Assert.assertTrue(NumberMatchHelper.numberMatchMap.isEmpty())
    }

    @Test
    fun `test receiveAuthUxMessage with number_matching does not invoke telemetry sink`() {
        val sink = RecordingTelemetrySink()
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        interfaceWithSink.receiveAuthUxMessage(numberMatchTestPayload)

        // Existing number-match behavior is unchanged (regression-safe)...
        Assert.assertEquals(mockNumberMatchValue, NumberMatchHelper.numberMatchMap[mockSessionId])
        // ...and the telemetry sink is never invoked for a non-telemetry action.
        Assert.assertTrue(sink.received.isEmpty())
    }

    /** Test double that records every telemetry event routed to the sink. */
    private class RecordingTelemetrySink(
        /** When false, simulates a host that is not ready to consume yet (e.g. no recorder). */
        var consume: Boolean = true
    ) : AuthUxTelemetrySink {
        val events = mutableListOf<AuthUxTelemetryEvent>()
        /** Every invocation, including ones this sink declines. */
        var calls = 0
        val received: List<String> get() = events.map { it.errorCode }
        override fun tryConsumeAuthUxTelemetry(event: AuthUxTelemetryEvent): Boolean {
            calls++
            if (!consume) {
                return false
            }
            events.add(event)
            return true
        }
    }

    /** Test double that always throws, to prove a bad sink cannot kill the bridge. */
    private class ThrowingTelemetrySink : AuthUxTelemetrySink {
        var calls = 0
        override fun tryConsumeAuthUxTelemetry(event: AuthUxTelemetryEvent): Boolean {
            calls++
            throw IllegalStateException("sink boom")
        }
    }

    /** Runs [block] with [span] installed as the current span. */
    private fun withCurrentSpan(span: Span, block: () -> Unit) {
        span.makeCurrent().use { block() }
    }

    private val authUxErrorCodeAttribute: String
        get() = AttributeName.authux_js_error_code.name

    @Test
    fun `test span error-code attribute is set when the sink consumes the code`() {
        val span = RecordingSpan()
        val interfaceWithSink = AuthUxJavaScriptInterface(RecordingTelemetrySink())

        withCurrentSpan(span) { interfaceWithSink.receiveAuthUxMessage(logTelemetryTestPayload) }

        Assert.assertEquals(mockErrorCode, span.getAttribute(authUxErrorCodeAttribute))
    }

    @Test
    fun `test span error-code attribute is unset when the sink declines the code`() {
        // NOT_CONSUMED means nothing was recorded downstream and the code stays eligible for retry,
        // so a span carrying it would advertise telemetry that does not exist.
        val span = RecordingSpan()
        val interfaceWithSink = AuthUxJavaScriptInterface(RecordingTelemetrySink(consume = false))

        withCurrentSpan(span) { interfaceWithSink.receiveAuthUxMessage(logTelemetryTestPayload) }

        Assert.assertFalse(
            "a declined code must not reach the span",
            span.hasAttribute(authUxErrorCodeAttribute)
        )
    }

    @Test
    fun `test span error-code attribute is unset when the sink throws`() {
        val span = RecordingSpan()
        val interfaceWithSink = AuthUxJavaScriptInterface(ThrowingTelemetrySink())

        withCurrentSpan(span) { interfaceWithSink.receiveAuthUxMessage(logTelemetryTestPayload) }

        Assert.assertFalse(
            "a throwing sink recorded nothing, so the span must stay silent",
            span.hasAttribute(authUxErrorCodeAttribute)
        )
    }

    @Test
    fun `test span error-code attribute holds the last consumed code`() {
        // setAttribute is single-valued, so a flow reporting several codes keeps only the last one.
        // Pinned because dashboards read this attribute and the loss is otherwise invisible.
        val span = RecordingSpan()
        val interfaceWithSink = AuthUxJavaScriptInterface(RecordingTelemetrySink())

        withCurrentSpan(span) {
            interfaceWithSink.receiveAuthUxMessage(logTelemetryPayloadWithErrorCode("\"530003\""))
            interfaceWithSink.receiveAuthUxMessage(logTelemetryPayloadWithErrorCode("\"53003\""))
        }

        Assert.assertEquals("53003", span.getAttribute(authUxErrorCodeAttribute))
    }

    @Test
    fun `test a code not consumed by the sink stays eligible for retry`() {
        // A sink may legitimately accept the call and record nothing — the first real implementation
        // returns false while its recorder is not yet attached. The bridge must NOT mark such a code
        // as forwarded, otherwise the retry is suppressed and the value is lost for good.
        val sink = RecordingTelemetrySink(consume = false)
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        interfaceWithSink.receiveAuthUxMessage(logTelemetryTestPayload)
        Assert.assertTrue("nothing consumed yet", sink.received.isEmpty())

        // Host becomes ready; the same code is reported again and must now get through.
        sink.consume = true
        interfaceWithSink.receiveAuthUxMessage(logTelemetryTestPayload)

        Assert.assertEquals(listOf(mockErrorCode), sink.received)
    }

    @Test
    fun `test an unconsumed code stays eligible but attempts are still bounded`() {
        // Two properties at once. A declined code must not consume the distinct-code cap (so it can
        // still get through later), but the work a page can cause must remain bounded — otherwise a
        // page looping against an unwired or perpetually-declining sink re-invokes it forever,
        // because a code that is never consumed is never recorded as handled.
        val sink = RecordingTelemetrySink(consume = false)
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        for (i in 1..40) {
            interfaceWithSink.receiveAuthUxMessage(logTelemetryPayloadWithErrorCode("\"5300$i\""))
        }
        Assert.assertTrue("nothing consumed", sink.received.isEmpty())
        Assert.assertEquals(
            "sink invocations must be bounded by the attempt cap",
            25,
            sink.calls
        )
    }

    @Test
    fun `test an unconsumed code does not consume the distinct-code cap`() {
        val sink = RecordingTelemetrySink(consume = false)
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        // Five declined codes: they must not count against the cap of 10 distinct consumed codes.
        for (i in 1..5) {
            interfaceWithSink.receiveAuthUxMessage(logTelemetryPayloadWithErrorCode("\"53000$i\""))
        }
        Assert.assertTrue(sink.received.isEmpty())

        sink.consume = true
        for (i in 1..10) {
            interfaceWithSink.receiveAuthUxMessage(logTelemetryPayloadWithErrorCode("\"53000$i\""))
        }

        Assert.assertEquals("cap must not have been consumed by declined codes", 10, sink.received.size)
    }

    @Test
    fun `test a throwing sink is treated as handled and is not retried`() {
        // A throwing sink is a host defect; retrying cannot fix it, and treating it as retryable
        // would let a looping page re-invoke a broken sink without bound. Both posts go through the
        // SAME instance — the dedupe/cap bookkeeping is per instance, so asserting across two
        // instances would prove nothing.
        val sink = ThrowingTelemetrySink()
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        interfaceWithSink.receiveAuthUxMessage(logTelemetryTestPayload)
        interfaceWithSink.receiveAuthUxMessage(logTelemetryTestPayload)

        Assert.assertEquals("second post must be suppressed as a duplicate", 1, sink.calls)
    }

    @Test
    fun `test dedupe does not survive bridge re-registration`() {
        // Documents the per-instance scope called out in handledErrorCodes' KDoc: the WebView host
        // rebuilds the bridge on every navigation, so the same code reported across two page loads
        // reaches the sink twice. Request-wide de-duplication is the consumer's job — see
        // AzureActiveDirectoryWebViewClient.tryConsumeAuthUxServerErrorCode, which de-duplicates for
        // exactly this reason. The onboarding recorder deliberately does not: its blocking-errors
        // list is append-only and chronological for its other callers.
        val sink = RecordingTelemetrySink()

        AuthUxJavaScriptInterface(sink).receiveAuthUxMessage(logTelemetryTestPayload)
        AuthUxJavaScriptInterface(sink).receiveAuthUxMessage(logTelemetryTestPayload)

        Assert.assertEquals(listOf(mockErrorCode, mockErrorCode), sink.received)
    }

    @Test
    fun `test a correlationId containing a newline is sanitized before logging`() {
        // Log-forging guard: correlationID is page-controlled and is passed as the correlationID
        // argument of every Logger call on this path, which common4j formats verbatim.
        val sink = RecordingTelemetrySink()
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        interfaceWithSink.receiveAuthUxMessage(
            """
            {
                "correlationID": "corr-1\nFATAL forged line",
                "action_name": "log_telemetry",
                "action_component": "host",
                "params": { "errorCode": $mockErrorCode }
            }
            """.trimIndent()
        )

        val seen = sink.events.single().correlationId
        Assert.assertFalse("CR/LF must not survive in correlationId", seen.contains("\n"))
        Assert.assertFalse(seen.contains("\r"))
        // Replaced with a space rather than removed, so the value keeps its shape in a log line.
        Assert.assertEquals("corr-1 FATAL forged line", seen)
    }

    @Test
    fun `test an oversized correlationId is bounded and control chars replaced`() {
        // The correlation ID is page-controlled. Sanitizing must bound its own work rather than
        // transforming the whole value and then cutting it, and must neutralize control characters
        // beyond just CR/LF (an ESC, for example, can also corrupt a log consumer). The ESC is
        // placed at the START so it falls inside the kept prefix and its replacement is observable
        // rather than being truncated away.
        val sink = RecordingTelemetrySink()
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        val huge = "corr\u001B[31m" + "A".repeat(200_000)
        interfaceWithSink.receiveAuthUxMessage(
            """
            {
                "correlationID": "$huge",
                "action_name": "log_telemetry",
                "action_component": "host",
                "params": { "errorCode": $mockErrorCode }
            }
            """.trimIndent()
        )

        val seen = sink.events.single().correlationId
        Assert.assertTrue(
            "oversized correlationId must be truncated, was ${seen.length} chars",
            seen.length < 200
        )
        Assert.assertTrue(seen.endsWith("...(truncated)"))
        Assert.assertFalse("ESC must not survive sanitization", seen.contains('\u001B'))
        Assert.assertTrue("ESC must be replaced with a space", seen.startsWith("corr [31m"))
    }

    @Test
    fun `test an errorCode containing a control character is rejected`() {
        // An ESC is not in ERROR_CODE_REGEX, so the code is dropped before it can reach the sink or
        // the onboarding blob. (The rejection warning renders it via sanitizeForLog, which is
        // covered directly by the correlationId test above.)
        val sink = RecordingTelemetrySink()
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        interfaceWithSink.receiveAuthUxMessage(
            logTelemetryPayloadWithErrorCode("\"530003\\u001B[31mred\"")
        )

        Assert.assertTrue(sink.received.isEmpty())
    }

    @Test
    fun `test an oversized action_component does not affect dispatch`() {
        // Span attributes are sanitized, but dispatch must keep comparing the RAW values. An
        // oversized/control-char action_component is page-controlled and gets bounded before it
        // reaches the span, yet the log_telemetry action must still be recognised and forwarded.
        val sink = RecordingTelemetrySink()
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        val hugeComponent = "host" + "X".repeat(5_000)
        interfaceWithSink.receiveAuthUxMessage(
            """
            {
                "correlationID": "corr-1",
                "action_name": "log_telemetry",
                "action_component": "$hugeComponent",
                "params": { "errorCode": $mockErrorCode }
            }
            """.trimIndent()
        )

        Assert.assertEquals(listOf(mockErrorCode), sink.received)
    }

    @Test
    fun `test malformed codes count toward the attempt cap`() {
        // Validation failures log a warning and do regex work, so the cap has to be applied on
        // entry to the handler rather than after validation — otherwise a page could spam malformed
        // codes forever without ever reaching it. Observable here because once the cap is exhausted
        // by malformed messages, a subsequent VALID code is refused.
        val sink = RecordingTelemetrySink()
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        repeat(MAX_ATTEMPTS_FOR_TEST) {
            interfaceWithSink.receiveAuthUxMessage(logTelemetryPayloadWithErrorCode("\"\""))
        }
        interfaceWithSink.receiveAuthUxMessage(logTelemetryTestPayload)

        Assert.assertTrue(
            "malformed messages must consume the attempt cap, so the later valid code is refused",
            sink.received.isEmpty()
        )
    }

    @Test
    fun `test a valid code still gets through below the attempt cap`() {
        // Guards the opposite direction of the test above: counting malformed messages must not be
        // so aggressive that ordinary noise blocks a legitimate code.
        val sink = RecordingTelemetrySink()
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        repeat(5) {
            interfaceWithSink.receiveAuthUxMessage(logTelemetryPayloadWithErrorCode("\"\""))
        }
        interfaceWithSink.receiveAuthUxMessage(logTelemetryTestPayload)

        Assert.assertEquals(listOf(mockErrorCode), sink.received)
    }

    @Test
    fun `test a realistic correlationId is preserved verbatim as the join key`() {
        // The correlation ID is the key used to join this event against eSTS / Kusto records, so
        // sanitizing must not shorten it. A GUID is 36 chars and must survive intact.
        val guid = "e4fd50ea-aa9c-41c9-8cc1-078ae50ddf0d"
        val sink = RecordingTelemetrySink()
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        interfaceWithSink.receiveAuthUxMessage(
            """
            {
                "correlationID": "$guid",
                "action_name": "log_telemetry",
                "action_component": "host",
                "params": { "errorCode": $mockErrorCode }
            }
            """.trimIndent()
        )

        Assert.assertEquals(guid, sink.events.single().correlationId)
    }

    @Test
    fun `test repeated duplicate codes do not spam past the attempt cap`() {
        // The duplicate and distinct-code checks log and return, so if the attempt counter were
        // checked after them a page looping ONE already-handled code would keep logging forever.
        // The counter is checked first, so processing stops once the cap is hit — observable here
        // because a code offered after the cap never reaches the sink even though it is new.
        val sink = RecordingTelemetrySink()
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        // One consumed code, then the same code 40 more times (all duplicates).
        for (i in 1..41) {
            interfaceWithSink.receiveAuthUxMessage(logTelemetryTestPayload)
        }
        Assert.assertEquals("only the first is forwarded", 1, sink.calls)

        // The attempt cap is now exhausted, so even a brand-new code is refused.
        interfaceWithSink.receiveAuthUxMessage(logTelemetryPayloadWithErrorCode("\"999999\""))
        Assert.assertEquals("attempt cap must stop further processing", 1, sink.calls)
    }

    @Test
    fun `test log_telemetry carrying a smuggled number_matching operation never reaches the store`() {
        // H3 invariant: dispatch is decided by action_name FIRST, so a log_telemetry message that
        // smuggles params.operation = number_matching must NOT mutate the number-match device store.
        val sink = RecordingTelemetrySink()
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        interfaceWithSink.receiveAuthUxMessage(
            """
            {
                "correlationID": "corr-1",
                "action_name": "log_telemetry",
                "action_component": "host",
                "params": {
                    "operation": "number_matching",
                    "sessionID": "$mockSessionId",
                    "code_match": "$mockNumberMatchValue",
                    "errorCode": $mockErrorCode
                }
            }
            """.trimIndent()
        )

        Assert.assertTrue(
            "log_telemetry must never write to the number-match store",
            NumberMatchHelper.numberMatchMap.isEmpty()
        )
        Assert.assertEquals(listOf(mockErrorCode), sink.received)
    }

    @Test
    fun `test unknown action_name carrying an errorCode does not invoke the sink`() {
        val sink = RecordingTelemetrySink()
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        interfaceWithSink.receiveAuthUxMessage(
            """
            {
                "correlationID": "corr-1",
                "action_name": "some_future_action",
                "action_component": "host",
                "params": { "errorCode": $mockErrorCode }
            }
            """.trimIndent()
        )

        Assert.assertTrue(sink.received.isEmpty())
    }

    @Test
    fun `test a throwing sink does not propagate out of receiveAuthUxMessage`() {
        // The whole point of this class is to never kill the broker.
        val interfaceWithSink = AuthUxJavaScriptInterface(ThrowingTelemetrySink())

        interfaceWithSink.receiveAuthUxMessage(logTelemetryTestPayload)

        // Reaching here without an exception is the assertion.
    }

    @Test
    fun `test empty and null errorCode are no-ops`() {
        val sink = RecordingTelemetrySink()
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        interfaceWithSink.receiveAuthUxMessage(logTelemetryPayloadWithErrorCode("\"\""))
        interfaceWithSink.receiveAuthUxMessage(logTelemetryPayloadWithErrorCode("null"))

        Assert.assertTrue(sink.received.isEmpty())
    }

    @Test
    fun `test errorCode sent as a quoted string is forwarded`() {
        // JSON.stringify emits a quoted string for a string-typed field, which is the likeliest
        // real-world shape alongside the bare number.
        val sink = RecordingTelemetrySink()
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        interfaceWithSink.receiveAuthUxMessage(logTelemetryPayloadWithErrorCode("\"$mockErrorCode\""))

        Assert.assertEquals(listOf(mockErrorCode), sink.received)
    }

    @Test
    fun `test over-long errorCode is rejected`() {
        val sink = RecordingTelemetrySink()
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        val tooLong = "5".repeat(33) // regex caps at 32
        interfaceWithSink.receiveAuthUxMessage(logTelemetryPayloadWithErrorCode("\"$tooLong\""))

        Assert.assertTrue(sink.received.isEmpty())
    }

    @Test
    fun `test errorCode containing a newline is rejected before reaching the sink`() {
        // Log-injection guard: a crafted code must never reach the sink or a log line intact.
        val sink = RecordingTelemetrySink()
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        interfaceWithSink.receiveAuthUxMessage(
            logTelemetryPayloadWithErrorCode("\"530003\\nFATAL fake log line\"")
        )

        Assert.assertTrue(sink.received.isEmpty())
    }

    @Test
    fun `test valid symbolic errorCode is accepted`() {
        // addBlockingError also accepts symbolic constants, so validation must stay permissive
        // enough for them rather than being numeric-only.
        val sink = RecordingTelemetrySink()
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        interfaceWithSink.receiveAuthUxMessage(logTelemetryPayloadWithErrorCode("\"BROKER_INSTALL\""))

        Assert.assertEquals(listOf("BROKER_INSTALL"), sink.received)
    }

    @Test
    fun `test boolean-sourced errorCode is coerced to a string and passes shape validation`() {
        // Documents a known, accepted limitation: Gson coerces the JSON literal true into "true",
        // which is alphanumeric and therefore indistinguishable from a symbolic code by shape alone.
        // Validation bounds charset and length, not semantics — deciding what is a meaningful error
        // code is the server contract's job, not this bridge's. Recorded here so the behavior is a
        // documented decision rather than an accident.
        val sink = RecordingTelemetrySink()
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        interfaceWithSink.receiveAuthUxMessage(logTelemetryPayloadWithErrorCode("true"))

        Assert.assertEquals(listOf("true"), sink.received)
    }

    @Test
    fun `test duplicate errorCodes are forwarded only once`() {
        val sink = RecordingTelemetrySink()
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        interfaceWithSink.receiveAuthUxMessage(logTelemetryTestPayload)
        interfaceWithSink.receiveAuthUxMessage(logTelemetryTestPayload)

        // Deduped, so a page reloading (or looping) cannot bloat the onboarding blob.
        Assert.assertEquals(listOf(mockErrorCode), sink.received)
    }

    @Test
    fun `test forwarding is capped per bridge instance`() {
        val sink = RecordingTelemetrySink()
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        // 15 distinct codes, cap is 10.
        for (i in 1..15) {
            interfaceWithSink.receiveAuthUxMessage(logTelemetryPayloadWithErrorCode("\"53000$i\""))
        }

        Assert.assertEquals(10, sink.received.size)
    }

    @Test
    fun `test non-integer schema version still parses and forwards errorCode`() {
        // params.v is the field designed to change over time; a non-integer value must not drop the
        // whole message (and with it the errorCode).
        val sink = RecordingTelemetrySink()
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        interfaceWithSink.receiveAuthUxMessage(
            """
            {
                "correlationID": "corr-1",
                "action_name": "log_telemetry",
                "action_component": "host",
                "params": { "v": "1.0", "errorCode": $mockErrorCode }
            }
            """.trimIndent()
        )

        Assert.assertEquals(listOf(mockErrorCode), sink.received)
        Assert.assertEquals("1.0", sink.events.single().version)
    }

    @Test
    fun `test sink receives full telemetry context including correlation id`() {
        val sink = RecordingTelemetrySink()
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        interfaceWithSink.receiveAuthUxMessage(logTelemetryTestPayload)

        val event = sink.events.single()
        Assert.assertEquals("corr-1", event.correlationId)
        Assert.assertEquals(mockErrorCode, event.errorCode)
        Assert.assertEquals("sess-1", event.sessionId)
        Assert.assertEquals("ConvergedTFA", event.pageId)
        Assert.assertEquals("track-1", event.trackingId)
        Assert.assertEquals("1", event.version)
    }

    private fun logTelemetryPayloadWithErrorCode(errorCodeLiteral: String): String = """
        {
            "correlationID": "corr-1",
            "action_name": "log_telemetry",
            "action_component": "host",
            "params": { "errorCode": $errorCodeLiteral }
        }
    """.trimIndent()

    @Test
    fun `test isValidUrlForInterface with valid AAD Global URL`() {
        val validUrl = "https://login.microsoftonline.com/common/oauth2/authorize"
        Assert.assertTrue(AuthUxJavaScriptInterface.isValidUriForInterface(validUrl))
    }

    @Test
    fun `test isValidUrlForInterface with valid AAD US URL`() {
        val validUrl = "https://login.microsoftonline.us/common/oauth2/authorize"
        Assert.assertTrue(AuthUxJavaScriptInterface.isValidUriForInterface(validUrl))
    }

    @Test
    fun `test isValidUrlForInterface with valid AAD China URL`() {
        val validUrl = "https://login.microsoftonline.cn/common/oauth2/authorize"
        Assert.assertTrue(AuthUxJavaScriptInterface.isValidUriForInterface(validUrl))
    }

    @Test
    fun `test isValidUrlForInterface with null URL`() {
        val nullUrl: String? = null
        Assert.assertFalse(AuthUxJavaScriptInterface.isValidUriForInterface(nullUrl))
    }

    @Test
    fun `test isValidUrlForInterface with invalid URL`() {
        val invalidUrl = "https://example.com"
        Assert.assertFalse(AuthUxJavaScriptInterface.isValidUriForInterface(invalidUrl))
    }

    @Test
    fun `test isValidUrlForInterface with scheme-only null-host URI`() {
        // A scheme-only URI (e.g. an openid-vc:// redirect) parses with a null host. It must be
        // rejected rather than throwing on the null-host AAD suffix check.
        val schemeOnlyUrl = "openid-vc://?request_uri=https://verifiedid.did.msidentity.com/x"
        Assert.assertFalse(AuthUxJavaScriptInterface.isValidUriForInterface(schemeOnlyUrl))
    }

    @Test
    fun `test isValidUrlForInterface with empty URL`() {
        val emptyUrl = ""
        Assert.assertFalse(AuthUxJavaScriptInterface.isValidUriForInterface(emptyUrl))
    }
}
