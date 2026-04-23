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

/**
 * Unit tests for [EventCollector].
 */
class EventCollectorTest {

    private val testCorrelationId = "test-correlation-id"

    @Test
    fun addEvent_whenTagProvided_eventIsPresentInSchema() {
        val collector = EventCollector(testCorrelationId)

        collector.addEvent(EventTag.BrokerRequestReceived)

        val schema = collector.toTelemetrySchema()
        val events = schema.performanceRecord?.executionFlow
        assertNotNull(events)
        assertEquals(1, events!!.size)
        assertEquals(EventTag.BrokerRequestReceived, events[0].tag)
    }

    @Test
    fun addEvent_withDiagnosticAndErrorCode_fieldsArePresentInEvent() {
        val collector = EventCollector(testCorrelationId)

        collector.addEvent(EventTag.BrokerRequestFailed, diagnosticCode = 42, errorCode = 100)

        val events = collector.toTelemetrySchema().performanceRecord?.executionFlow
        assertNotNull(events)
        val event = events!![0]
        assertEquals(42, event.diagnosticCode)
        assertEquals(100, event.errorCode)
    }

    @Test
    fun addEvent_whenNoOptionalCodes_diagnosticAndErrorCodeAreNull() {
        val collector = EventCollector(testCorrelationId)

        collector.addEvent(EventTag.BrokerCacheHit)

        val event = collector.toTelemetrySchema().performanceRecord?.executionFlow?.get(0)
        assertNotNull(event)
        assertNull(event!!.diagnosticCode)
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

        val events = collector.toTelemetrySchema().performanceRecord?.executionFlow
        assertNotNull(events)
        assertEquals(tags.size, events!!.size)
        tags.forEachIndexed { index, tag -> assertEquals(tag, events[index].tag) }
    }

    @Test
    fun toTelemetrySchema_correlationIdMatchesConstructorArg() {
        val collector = EventCollector(testCorrelationId)

        val schema = collector.toTelemetrySchema()

        assertEquals(testCorrelationId, schema.correlationId)
    }

    @Test
    fun toTelemetrySchema_performanceRecordHasNonNegativeDuration() {
        val collector = EventCollector(testCorrelationId)
        collector.addEvent(EventTag.BrokerRequestReceived)

        val schema = collector.toTelemetrySchema()

        assertNotNull(schema.performanceRecord)
        assertTrue(schema.performanceRecord!!.duration >= 0)
    }

    @Test
    fun toTelemetrySchema_startTimeIsIso8601Format() {
        val collector = EventCollector(testCorrelationId)

        val schema = collector.toTelemetrySchema()

        val startTime = schema.performanceRecord?.startTime
        assertNotNull(startTime)
        // ISO 8601 instant strings end with 'Z'
        assertTrue(startTime!!.endsWith("Z"))
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

        val schema = collector.toTelemetrySchema()
        val events = schema.performanceRecord?.executionFlow
        assertNotNull(events)
        assertEquals(threadCount * eventsPerThread, events!!.size)

        // Verify duration invariant: duration >= max event timestamp (race condition regression test)
        val maxEventTs = events.maxOf { it.timestampMs }
        val duration = schema.performanceRecord!!.duration
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

        TelemetryHelper.addEventSafely(collector, EventTag.BrokerTokenAcquired, diagnosticCode = 1)

        val events = collector.toTelemetrySchema().performanceRecord?.executionFlow
        assertNotNull(events)
        assertEquals(1, events!!.size)
        assertEquals(EventTag.BrokerTokenAcquired, events[0].tag)
        assertEquals(1, events[0].diagnosticCode)
    }
}
