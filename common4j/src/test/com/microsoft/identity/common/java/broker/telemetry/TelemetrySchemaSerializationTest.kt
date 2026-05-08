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

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests verifying that [TelemetrySchema] and its nested types round-trip through
 * Gson serialization/deserialization correctly, including [EventTag] compact values.
 */
class TelemetrySchemaSerializationTest {

    private val gson: Gson = GsonBuilder().create()

    @Test
    fun telemetrySchema_roundTrip_preservesCorrelationId() {
        val original = TelemetrySchema(correlationId = "abc-123")

        val json = gson.toJson(original)
        val restored = gson.fromJson(json, TelemetrySchema::class.java)

        assertEquals("abc-123", restored.correlationId)
    }

    @Test
    fun telemetrySchema_roundTrip_preservesSchemaVersion() {
        val original = TelemetrySchema(correlationId = "abc-123")

        val json = gson.toJson(original)
        val restored = gson.fromJson(json, TelemetrySchema::class.java)

        assertEquals(TelemetrySchema.CURRENT_VERSION, restored.schemaVersion)
    }

    @Test
    fun telemetrySchema_roundTrip_preservesOptionalFields() {
        val original = TelemetrySchema(
            correlationId = "cid",
            name = "token.acquire",
            version = "2",
            authOutcome = "success",
            errorCode = null,
            responseStarvationDuration = 100,
            powerPolicy = true,
            deviceIdle = false
        )

        val json = gson.toJson(original)
        val restored = gson.fromJson(json, TelemetrySchema::class.java)

        assertEquals("token.acquire", restored.name)
        assertEquals("2", restored.version)
        assertEquals("success", restored.authOutcome)
        assertNull(restored.errorCode)
        assertEquals(100, restored.responseStarvationDuration)
        assertEquals(true, restored.powerPolicy)
        assertEquals(false, restored.deviceIdle)
    }

    @Test
    fun performanceRecord_roundTrip_preservesFields() {
        val events = listOf(
            ExecutionEvent(tag = EventTag.BrokerRequestReceived, timestampMs = 0L),
            ExecutionEvent(tag = EventTag.BrokerCacheHit, timestampMs = 10L)
        )
        val perf = PerformanceRecord(
            startTime = "2024-01-01T00:00:00Z",
            duration = 150L,
            executionFlow = events
        )
        val schema = TelemetrySchema(correlationId = "cid", performanceRecord = perf)

        val json = gson.toJson(schema)
        val restored = gson.fromJson(json, TelemetrySchema::class.java)

        val restoredPerf = restored.performanceRecord
        assertNotNull(restoredPerf)
        assertEquals("1.0.0", restoredPerf!!.version)
        assertEquals("2024-01-01T00:00:00Z", restoredPerf.startTime)
        assertEquals(150L, restoredPerf.duration)
        assertEquals(2, restoredPerf.executionFlow.size)
    }

    @Test
    fun eventTag_serialized_usesCompactValue() {
        val event = ExecutionEvent(tag = EventTag.BrokerRequestReceived, timestampMs = 0L)
        val json = gson.toJson(event)
        // The tag field should serialize using compact "v" value, not the enum name
        assert(json.contains("bre.recv")) { "Expected compact tag value 'bre.recv' in: $json" }
    }

    @Test
    fun executionEvent_roundTrip_preservesTagEnum() {
        val original = ExecutionEvent(
            tag = EventTag.BrokerNetworkCallFailed,
            timestampMs = 42L,
            diagnosticCode = 5,
            errorCode = 99
        )

        val json = gson.toJson(original)
        val restored = gson.fromJson(json, ExecutionEvent::class.java)

        assertEquals(EventTag.BrokerNetworkCallFailed, restored.tag)
        assertEquals(42L, restored.timestampMs)
        assertEquals(5, restored.diagnosticCode)
        assertEquals(99, restored.errorCode)
    }

    @Test
    fun executionEvent_roundTrip_withNullOptionalFields() {
        val original = ExecutionEvent(tag = EventTag.BrokerCacheCheckStart, timestampMs = 7L)

        val json = gson.toJson(original)
        val restored = gson.fromJson(json, ExecutionEvent::class.java)

        assertNull(restored.diagnosticCode)
        assertNull(restored.errorCode)
    }

    @Test
    fun eventTag_allValues_roundTripThroughGson() {
        for (tag in EventTag.values()) {
            val event = ExecutionEvent(tag = tag, timestampMs = 0L)
            val json = gson.toJson(event)
            val restored = gson.fromJson(json, ExecutionEvent::class.java)
            assertEquals("Round-trip failed for tag: $tag", tag, restored.tag)
        }
    }

    @Test
    fun eventCollector_toTelemetrySchema_roundTripsThroughGson() {
        val collector = EventCollector("test-cid")
        collector.addEvent(EventTag.BrokerRequestReceived)
        collector.addEvent(EventTag.BrokerCacheCheckStart)
        collector.addEvent(EventTag.BrokerResponseSent)

        val schema = collector.toTelemetrySchema()
        val json = gson.toJson(schema)
        val restored = gson.fromJson(json, TelemetrySchema::class.java)

        assertEquals("test-cid", restored.correlationId)
        assertEquals(3, restored.performanceRecord?.executionFlow?.size)
        assertEquals(EventTag.BrokerRequestReceived, restored.performanceRecord?.executionFlow?.get(0)?.tag)
        assertEquals(EventTag.BrokerCacheCheckStart, restored.performanceRecord?.executionFlow?.get(1)?.tag)
        assertEquals(EventTag.BrokerResponseSent, restored.performanceRecord?.executionFlow?.get(2)?.tag)
    }

    @Test
    fun telemetrySchema_serializedJson_usesExpectedFieldNames() {
        val schema = TelemetrySchema(correlationId = "cid")

        val json = gson.toJson(schema)

        assert(json.contains("\"schema_version\"")) { "Expected 'schema_version' in: $json" }
        assert(json.contains("\"correlation_id\"")) { "Expected 'correlation_id' in: $json" }
    }
}
