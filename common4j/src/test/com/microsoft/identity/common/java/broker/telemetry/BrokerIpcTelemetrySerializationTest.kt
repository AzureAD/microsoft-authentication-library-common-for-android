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
 * Unit tests for Gson round-trip serialization/deserialization of [BrokerIpcTelemetry] and
 * its nested types.
 */
class BrokerIpcTelemetrySerializationTest {

    private val gson: Gson = GsonBuilder().create()

    // ------------------------------------------------------------------
    // BrokerIpcTelemetry round-trip
    // ------------------------------------------------------------------

    @Test
    fun brokerIpcTelemetry_roundTrip_preservesCorrelationId() {
        val original = buildFullSchema()

        val json = gson.toJson(original)
        val restored = gson.fromJson(json, BrokerIpcTelemetry::class.java)

        assertEquals(original.correlationId, restored.correlationId)
    }

    @Test
    fun brokerIpcTelemetry_roundTrip_preservesSchemaVersion() {
        val original = buildFullSchema()

        val json = gson.toJson(original)
        val restored = gson.fromJson(json, BrokerIpcTelemetry::class.java)

        assertEquals(BrokerIpcTelemetry.CURRENT_VERSION, restored.schemaVersion)
    }

    @Test
    fun brokerIpcTelemetry_roundTrip_preservesOptionalFields() {
        val original = buildFullSchema()

        val json = gson.toJson(original)
        val restored = gson.fromJson(json, BrokerIpcTelemetry::class.java)

        assertEquals(original.authOutcome, restored.authOutcome)
        assertEquals(original.errorCode, restored.errorCode)
        assertEquals(original.responseStarvationDuration, restored.responseStarvationDuration)
        assertEquals(original.powerPolicy, restored.powerPolicy)
        assertEquals(original.deviceIdle, restored.deviceIdle)
    }

    @Test
    fun brokerIpcTelemetry_roundTrip_withNullOptionalFields_remainsNull() {
        val original = BrokerIpcTelemetry(
            correlationId = "corr-null-opts",
            name = "authenticator",
            version = "test-broker-1.0",
            authOutcome = "success",
            performanceRecord = PerformanceRecord(
                startTime = "2024-01-01T00:00:00.000Z",
                duration = 0L,
                executionFlow = emptyList()
            )
        )

        val json = gson.toJson(original)
        val restored = gson.fromJson(json, BrokerIpcTelemetry::class.java)

        // Contract-required fields survive; genuinely optional ones stay absent.
        assertEquals("success", restored.authOutcome)
        assertNull(restored.errorCode)
        assertNull(restored.responseStarvationDuration)
        assertNull(restored.powerPolicy)
        assertNull(restored.deviceIdle)
    }

    // ------------------------------------------------------------------
    // PerformanceRecord round-trip
    // ------------------------------------------------------------------

    @Test
    fun performanceRecord_roundTrip_preservesDuration() {
        val original = buildFullSchema()

        val json = gson.toJson(original)
        val restored = gson.fromJson(json, BrokerIpcTelemetry::class.java)

        assertEquals(original.performanceRecord.duration, restored.performanceRecord.duration)
    }

    @Test
    fun performanceRecord_roundTrip_preservesStartTime() {
        val original = buildFullSchema()

        val json = gson.toJson(original)
        val restored = gson.fromJson(json, BrokerIpcTelemetry::class.java)

        assertEquals(original.performanceRecord.startTime, restored.performanceRecord.startTime)
    }

    // ------------------------------------------------------------------
    // ExecutionEvent round-trip
    // ------------------------------------------------------------------

    @Test
    fun executionEvent_roundTrip_preservesTag() {
        val original = buildFullSchema()

        val json = gson.toJson(original)
        val restored = gson.fromJson(json, BrokerIpcTelemetry::class.java)

        val originalEvents = original.performanceRecord.executionFlow
        val restoredEvents = restored.performanceRecord.executionFlow
        assertEquals(originalEvents.size, restoredEvents.size)
        originalEvents.forEachIndexed { i, event ->
            assertEquals(event.tag, restoredEvents[i].tag)
        }
    }

    @Test
    fun executionEvent_roundTrip_preservesTimestamp() {
        val event = ExecutionEvent(tag = EventTag.BrokerCacheHit, timestampMs = 12345L)

        val json = gson.toJson(event)
        val restored = gson.fromJson(json, ExecutionEvent::class.java)

        assertEquals(12345L, restored.timestampMs)
    }

    @Test
    fun executionEvent_roundTrip_preservesOptionalStatusCodeAndErrorCode() {
        val event = ExecutionEvent(
            tag = EventTag.BrokerNetworkCallFailed,
            timestampMs = 5000L,
            statusCode = 7,
            errorCode = 503
        )

        val json = gson.toJson(event)
        val restored = gson.fromJson(json, ExecutionEvent::class.java)

        assertEquals(7, restored.statusCode)
        assertEquals(503, restored.errorCode)
    }

    @Test
    fun executionEvent_roundTrip_whenOptionalCodesAbsent_remainsNull() {
        val event = ExecutionEvent(tag = EventTag.BrokerRequestReceived, timestampMs = 0L)

        val json = gson.toJson(event)
        val restored = gson.fromJson(json, ExecutionEvent::class.java)

        assertNull(restored.statusCode)
        assertNull(restored.errorCode)
    }

    // ------------------------------------------------------------------
    // EventTag serialized name verification
    // ------------------------------------------------------------------

    @Test
    fun eventTag_serializedAsEnumName() {
        val event = ExecutionEvent(tag = EventTag.BrokerRequestReceived, timestampMs = 0L)

        val json = gson.toJson(event)

        // The tag should be serialized as the human-readable enum name
        assertNotNull(json)
        val tagJson = gson.toJsonTree(event).asJsonObject.get("t")
        assertNotNull(tagJson)
        // EventTag is a plain enum — Gson serializes it as its .name()
        assertEquals("BrokerRequestReceived", tagJson.asString)
    }

    // ------------------------------------------------------------------
    // EventCollector → BrokerIpcTelemetry integration
    // ------------------------------------------------------------------

    @Test
    fun eventCollector_toBrokerIpcTelemetry_roundTripsCorrectly() {
        val collector = EventCollector("round-trip-corr-id")
        collector.addEvent(EventTag.BrokerRequestReceived)
        collector.addEvent(EventTag.BrokerCacheCheckStart)
        collector.addEvent(EventTag.BrokerResponseSent)

        val original = collector.toTestTelemetry()
        val json = gson.toJson(original)
        val restored = gson.fromJson(json, BrokerIpcTelemetry::class.java)

        assertEquals(original.correlationId, restored.correlationId)
        assertEquals(3, restored.performanceRecord.executionFlow.size)
        assertEquals(EventTag.BrokerResponseSent, restored.performanceRecord.executionFlow.last().tag)
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Supplies the contract-required broker identity and outcome fields for tests. */
    private fun EventCollector.toTestTelemetry(): BrokerIpcTelemetry =
        toBrokerIpcTelemetry(name = "authenticator", version = "test-broker-1.0", authOutcome = "success")

    private fun buildFullSchema(): BrokerIpcTelemetry {
        val events = listOf(
            ExecutionEvent(tag = EventTag.BrokerRequestReceived, timestampMs = 0L),
            ExecutionEvent(tag = EventTag.BrokerCacheHit, timestampMs = 10L),
            ExecutionEvent(tag = EventTag.BrokerResponseSent, timestampMs = 50L)
        )
        val perfRecord = PerformanceRecord(
            startTime = "2024-01-01T00:00:00.000Z",
            duration = 50L,
            executionFlow = events
        )
        return BrokerIpcTelemetry(
            correlationId = "test-corr-id",
            name = "authenticator",
            version = "test-broker-1.0",
            authOutcome = "success",
            errorCode = null,
            responseStarvationDuration = 5,
            powerPolicy = false,
            deviceIdle = true,
            performanceRecord = perfRecord
        )
    }
}
