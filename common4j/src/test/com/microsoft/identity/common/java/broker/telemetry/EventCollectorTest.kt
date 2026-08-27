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
package com.microsoft.identity.common.java.broker.telemetry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/**
 * Unit tests for [EventCollector].
 */
class EventCollectorTest {

    private val testCorrelationId = "test-correlation-id"

    @Test
    fun addEvent_whenTagProvided_eventIsPresentInSchema() {
        val collector = EventCollector(testCorrelationId)

        collector.addEvent(EventTag.BrokerRequestReceived)

        val schema = collector.toTestTelemetry()
        val events = schema.performanceRecord.executionFlow
        assertNotNull(events)
        assertEquals(1, events.size)
        assertEquals(EventTag.BrokerRequestReceived, events[0].tag)
    }

    @Test
    fun addEvent_withStatusCodeAndErrorCode_fieldsArePresentInEvent() {
        val collector = EventCollector(testCorrelationId)

        collector.addEvent(EventTag.BrokerRequestFailed, statusCode = 42, errorCode = 100)

        val events = collector.toTestTelemetry().performanceRecord.executionFlow
        assertNotNull(events)
        val event = events[0]
        assertEquals(42, event.statusCode)
        assertEquals(100, event.errorCode)
    }

    @Test
    fun addEvent_whenNoOptionalCodes_statusCodeAndErrorCodeAreNull() {
        val collector = EventCollector(testCorrelationId)

        collector.addEvent(EventTag.BrokerCacheHit)

        val event = collector.toTestTelemetry().performanceRecord.executionFlow.get(0)
        assertNotNull(event)
        assertNull(event.statusCode)
        assertNull(event.errorCode)
    }

    @Test
    fun addEvent_multipleEvents_allEventsRetainedInOrder() {
        val collector = EventCollector(testCorrelationId)
        val tags = listOf(
            EventTag.BrokerRequestReceived,
            EventTag.BrokerCacheCheckStart,
            EventTag.BrokerCacheHit,
            EventTag.BrokerResponseSent
        )

        tags.forEach { collector.addEvent(it) }

        val events = collector.toTestTelemetry().performanceRecord.executionFlow
        assertNotNull(events)
        assertEquals(tags.size, events.size)
        tags.forEachIndexed { index, tag -> assertEquals(tag, events[index].tag) }
    }

    /**
     * Guards the broker/client stitching contract by verifying broker events offset thread IDs.
     */
    @Test
    fun addEvent_threadIdIncludesBrokerOffset() {
        val collector = EventCollector(testCorrelationId)
        val expectedThreadId = Thread.currentThread().id + EventCollector.BROKER_THREAD_ID_OFFSET

        collector.addEvent(EventTag.BrokerRequestReceived)

        val event = collector.toTestTelemetry().performanceRecord.executionFlow.get(0)
        assertNotNull(event)
        assertEquals(expectedThreadId, event.threadId)
    }

    /**
     * Guards the monotonic elapsed-time contract by ensuring sequential event timestamps never go backwards.
     */
    @Test
    fun addEvent_multipleEvents_timestampsAreNonDecreasing() {
        val collector = EventCollector(testCorrelationId)
        val tags = listOf(
            EventTag.BrokerRequestReceived,
            EventTag.BrokerCacheCheckStart,
            EventTag.BrokerCacheHit,
            EventTag.BrokerResponseSent
        )

        tags.forEach { collector.addEvent(it) }

        val events = collector.toTestTelemetry().performanceRecord.executionFlow
        assertNotNull(events)
        events.zipWithNext().forEach { (previousEvent, nextEvent) ->
            assertTrue(
                "Event timestamp ${nextEvent.timestampMs} must be >= previous timestamp ${previousEvent.timestampMs}",
                nextEvent.timestampMs >= previousEvent.timestampMs
            )
        }
    }

    @Test
    fun toBrokerIpcTelemetry_correlationIdMatchesConstructorArg() {
        val collector = EventCollector(testCorrelationId)

        val schema = collector.toTestTelemetry()

        assertEquals(testCorrelationId, schema.correlationId)
    }

    @Test
    fun toBrokerIpcTelemetry_performanceRecordHasNonNegativeDuration() {
        val collector = EventCollector(testCorrelationId)
        collector.addEvent(EventTag.BrokerRequestReceived)

        val schema = collector.toTestTelemetry()

        assertNotNull(schema.performanceRecord)
        assertTrue(schema.performanceRecord.duration >= 0)
    }

    @Test
    fun toBrokerIpcTelemetry_startTimeIsIso8601Format() {
        val collector = EventCollector(testCorrelationId)

        val schema = collector.toTestTelemetry()

        val startTime = schema.performanceRecord.startTime
        assertNotNull(startTime)
        // Fixed-width ISO 8601 / RFC 3339 UTC with millisecond precision, e.g. 2026-01-13T10:30:45.123Z
        val iso8601UtcMillis = Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$""")
        assertTrue(
            "startTime '$startTime' is not ISO 8601 UTC with millisecond precision",
            iso8601UtcMillis.matches(startTime)
        )

        // Parse back in UTC: guards against the formatter silently using the default time zone,
        // which would still satisfy the pattern above but encode the wrong instant.
        val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .parse(startTime)
        assertTrue(
            "startTime '$startTime' should decode to approximately the current UTC instant",
            abs(parsed.time - System.currentTimeMillis()) < 60_000L
        )
    }

    @Test
    fun addEvent_isThreadSafe_allEventsRecordedUnderConcurrentAccess() {
        val collector = EventCollector(testCorrelationId)
        val threadCount = 10
        val eventsPerThread = 20
        val threads = (1..threadCount).map {
            Thread {
                repeat(eventsPerThread) {
                    collector.addEvent(EventTag.BrokerNetworkCallStart)
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        val schema = collector.toTestTelemetry()
        val events = schema.performanceRecord.executionFlow
        assertNotNull(events)
        assertEquals(threadCount * eventsPerThread, events.size)

        // Verify duration invariant: duration >= max event timestamp (race condition regression test)
        val maxEventTs = events.maxOf { it.timestampMs }
        val duration = schema.performanceRecord.duration
        assertTrue(
            "Duration ($duration) must be >= max event timestamp ($maxEventTs)",
            duration >= maxEventTs
        )
    }

    @Test
    fun addEventSafely_withNullCollector_doesNotThrow() {
        // Should be a no-op and not throw NullPointerException
        TelemetryHelper.addEventSafely(null, EventTag.BrokerRequestReceived)
    }

    @Test
    fun addEventSafely_withNonNullCollector_addsEvent() {
        val collector = EventCollector(testCorrelationId)

        TelemetryHelper.addEventSafely(collector, EventTag.BrokerTokenAcquired, statusCode = 1)

        val events = collector.toTestTelemetry().performanceRecord.executionFlow
        assertNotNull(events)
        assertEquals(1, events.size)
        assertEquals(EventTag.BrokerTokenAcquired, events[0].tag)
        assertEquals(1, events[0].statusCode)
    }

    @Test
    fun adoptCorrelationId_whenConstructedBlank_adoptsResolvedValue() {
        val collector = EventCollector("")

        collector.adoptCorrelationId("resolved-correlation-id")

        assertEquals("resolved-correlation-id", collector.correlationId)
        assertEquals("resolved-correlation-id", collector.toTestTelemetry().correlationId)
    }

    @Test
    fun adoptCorrelationId_whenConstructedWithValue_doesNotOverwrite() {
        // A caller-supplied ID is what the caller will use to look the request up, so it wins.
        val collector = EventCollector(testCorrelationId)

        collector.adoptCorrelationId("some-other-correlation-id")

        assertEquals(testCorrelationId, collector.correlationId)
    }

    @Test
    fun adoptCorrelationId_withNullOrBlankResolved_leavesCollectorUnchanged() {
        // A caller with nothing better to offer must not degrade what is already recorded.
        val collector = EventCollector("")

        collector.adoptCorrelationId(null)
        assertEquals("", collector.correlationId)

        collector.adoptCorrelationId("   ")
        assertEquals("", collector.correlationId)
    }

    @Test
    fun adoptCorrelationId_calledTwice_isIdempotent() {
        // Callers invoke this unconditionally, so repeated calls must be harmless.
        val collector = EventCollector("")

        collector.adoptCorrelationId("first-resolved-id")
        collector.adoptCorrelationId("second-resolved-id")

        assertEquals("first-resolved-id", collector.correlationId)
    }

    /** Supplies the contract-required broker identity and outcome fields for tests. */
    private fun EventCollector.toTestTelemetry(): BrokerIpcTelemetry =
        toBrokerIpcTelemetry(name = "authenticator", version = "test-broker-1.0", authOutcome = "success")
}
