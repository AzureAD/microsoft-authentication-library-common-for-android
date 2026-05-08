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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

/**
 * Unit tests for [EventCollector].
 */
class EventCollectorTest {

    private val correlationId = "test-correlation-id"

    @Test
    fun addEvent_whenCalled_addsEventToFlow() {
        val collector = EventCollector(correlationId)

        collector.addEvent(EventTag.BrokerRequestReceived)

        val schema = collector.toTelemetrySchema()
        assertEquals(1, schema.performanceRecord?.executionFlow?.size)
        assertEquals(EventTag.BrokerRequestReceived, schema.performanceRecord?.executionFlow?.first()?.tag)
    }

    @Test
    fun addEvent_withDiagnosticCode_setsCodeOnEvent() {
        val collector = EventCollector(correlationId)

        collector.addEvent(EventTag.BrokerCacheMiss, diagnosticCode = 42)

        val event = collector.toTelemetrySchema().performanceRecord?.executionFlow?.first()
        assertEquals(42, event?.diagnosticCode)
        assertNull(event?.errorCode)
    }

    @Test
    fun addEvent_withErrorCode_setsErrorCodeOnEvent() {
        val collector = EventCollector(correlationId)

        collector.addEvent(EventTag.BrokerRequestFailed, errorCode = 999)

        val event = collector.toTelemetrySchema().performanceRecord?.executionFlow?.first()
        assertEquals(999, event?.errorCode)
        assertNull(event?.diagnosticCode)
    }

    @Test
    fun toTelemetrySchema_whenNoEvents_returnsEmptyExecutionFlow() {
        val collector = EventCollector(correlationId)

        val schema = collector.toTelemetrySchema()

        assertNotNull(schema.performanceRecord)
        assertTrue(schema.performanceRecord!!.executionFlow.isEmpty())
    }

    @Test
    fun toTelemetrySchema_setsCorrelationId() {
        val collector = EventCollector(correlationId)

        val schema = collector.toTelemetrySchema()

        assertEquals(correlationId, schema.correlationId)
    }

    @Test
    fun toTelemetrySchema_setsNonNegativeDuration() {
        val collector = EventCollector(correlationId)
        collector.addEvent(EventTag.BrokerRequestReceived)

        val schema = collector.toTelemetrySchema()

        assertTrue(schema.performanceRecord!!.duration >= 0)
    }

    @Test
    fun toTelemetrySchema_setsStartTimeAsIso8601() {
        val collector = EventCollector(correlationId)

        val schema = collector.toTelemetrySchema()

        // Instant.toString() produces a valid ISO-8601 string containing 'T'
        val startTime = schema.performanceRecord?.startTime
        assertNotNull(startTime)
        assertTrue(startTime!!.contains("T"))
    }

    @Test
    fun addEvent_multipleEvents_preservesInsertionOrder() {
        val collector = EventCollector(correlationId)
        val tags = listOf(
            EventTag.BrokerRequestReceived,
            EventTag.BrokerCacheCheckStart,
            EventTag.BrokerCacheHit,
            EventTag.BrokerResponseSent
        )

        tags.forEach { collector.addEvent(it) }

        val flow = collector.toTelemetrySchema().performanceRecord!!.executionFlow
        assertEquals(tags.size, flow.size)
        tags.forEachIndexed { index, tag -> assertEquals(tag, flow[index].tag) }
    }

    @Test
    fun addEvent_fromMultipleThreads_isThreadSafe() {
        val collector = EventCollector(correlationId)
        val threadCount = 50
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)

        repeat(threadCount) {
            executor.submit {
                collector.addEvent(EventTag.BrokerNetworkCallStart)
                latch.countDown()
            }
        }

        latch.await()
        executor.shutdown()

        val flow = collector.toTelemetrySchema().performanceRecord!!.executionFlow
        assertEquals(threadCount, flow.size)
    }

    @Test
    fun addEventSafely_whenCollectorIsNull_doesNotThrow() {
        // Should be a no-op and not throw NullPointerException
        TelemetryHelper.addEventSafely(null, EventTag.BrokerRequestReceived)
    }

    @Test
    fun addEventSafely_whenCollectorIsNonNull_addsEvent() {
        val collector = EventCollector(correlationId)

        TelemetryHelper.addEventSafely(collector, EventTag.BrokerCacheHit, diagnosticCode = 1, errorCode = 2)

        val flow = collector.toTelemetrySchema().performanceRecord!!.executionFlow
        assertEquals(1, flow.size)
        assertEquals(EventTag.BrokerCacheHit, flow.first().tag)
        assertEquals(1, flow.first().diagnosticCode)
        assertEquals(2, flow.first().errorCode)
    }
}
