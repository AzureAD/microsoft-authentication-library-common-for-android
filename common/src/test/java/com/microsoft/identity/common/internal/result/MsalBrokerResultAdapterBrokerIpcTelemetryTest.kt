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
package com.microsoft.identity.common.internal.result

import android.os.Bundle
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.common.java.broker.telemetry.BrokerIpcTelemetry
import com.microsoft.identity.common.java.broker.telemetry.ExecutionEvent
import com.microsoft.identity.common.java.broker.telemetry.EventTag
import com.microsoft.identity.common.java.broker.telemetry.PerformanceRecord
import com.microsoft.identity.common.java.cache.CacheRecord
import com.microsoft.identity.common.java.cache.ICacheRecord
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.request.SdkType
import com.microsoft.identity.common.java.result.LocalAuthenticationResult
import com.microsoft.identity.internal.testutils.MockRecords
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MsalBrokerResultAdapterBrokerIpcTelemetryTest {
    private val adapter = MsalBrokerResultAdapter()
    private val gson = Gson()
    private val telemetry = BrokerIpcTelemetry(
        correlationId = "correlation-id",
        name = "authenticator",
        version = "1.0",
        authOutcome = "success",
        performanceRecord = PerformanceRecord(
            startTime = "2026-08-25T00:00:00.000Z",
            duration = 10,
            executionFlow = listOf(
                ExecutionEvent(EventTag.BrokerTokenAcquired, 10)
            )
        )
    )

    @Test
    fun getBrokerIpcTelemetryFromBundle_validJson_deserializesPayload() {
        val restored = adapter.getBrokerIpcTelemetryFromBundle(bundleWith(gson.toJson(telemetry)))

        assertEquals(telemetry, restored)
    }

    @Test
    fun getBrokerIpcTelemetryFromBundle_nullJson_returnsNull() {
        assertNull(adapter.getBrokerIpcTelemetryFromBundle(Bundle()))
    }

    @Test
    fun getBrokerIpcTelemetryFromBundle_emptyJson_returnsNull() {
        assertNull(adapter.getBrokerIpcTelemetryFromBundle(bundleWith("")))
    }

    @Test
    fun getBrokerIpcTelemetryFromBundle_malformedJson_returnsNull() {
        assertNull(adapter.getBrokerIpcTelemetryFromBundle(bundleWith("{")))
    }

    /**
     * A JSON array root is well-formed JSON, so it reaches the type adapter factory rather
     * than failing in the tokenizer. The factory rejects any non-object root.
     */
    @Test
    fun getBrokerIpcTelemetryFromBundle_arrayRoot_returnsNull() {
        assertNull(adapter.getBrokerIpcTelemetryFromBundle(bundleWith("[]")))
    }

    /** As above, for a scalar root. */
    @Test
    fun getBrokerIpcTelemetryFromBundle_scalarRoot_returnsNull() {
        assertNull(adapter.getBrokerIpcTelemetryFromBundle(bundleWith("\"not-an-object\"")))
    }

    /**
     * An explicit JSON null is a valid document that deserializes to null rather than
     * throwing, so it must degrade to "no telemetry" like every other unusable payload.
     */
    @Test
    fun getBrokerIpcTelemetryFromBundle_jsonNull_returnsNull() {
        assertNull(adapter.getBrokerIpcTelemetryFromBundle(bundleWith("null")))
    }

    /**
     * Guards the method's never-throws contract against a payload that fails inside the
     * delegate adapter rather than in the tokenizer or the required-field check: every
     * required field is present, but `duration` holds a string where a number is expected.
     */
    @Test
    fun getBrokerIpcTelemetryFromBundle_wrongFieldType_returnsNull() {
        val json = gson.toJsonTree(telemetry).asJsonObject
        json.getAsJsonObject("perf").addProperty("duration", "not-a-number")

        assertNull(adapter.getBrokerIpcTelemetryFromBundle(bundleWith(json.toString())))
    }

    @Test
    fun getBrokerIpcTelemetryFromBundle_missingSchemaVersion_returnsNull() {
        assertMissingRootFieldRejected("schema_version")
    }

    @Test
    fun getBrokerIpcTelemetryFromBundle_missingCorrelationId_returnsNull() {
        assertMissingRootFieldRejected("correlation_id")
    }

    @Test
    fun getBrokerIpcTelemetryFromBundle_missingName_returnsNull() {
        assertMissingRootFieldRejected("name")
    }

    @Test
    fun getBrokerIpcTelemetryFromBundle_missingVersion_returnsNull() {
        assertMissingRootFieldRejected("version")
    }

    @Test
    fun getBrokerIpcTelemetryFromBundle_missingAuthOutcome_returnsNull() {
        assertMissingRootFieldRejected("auth_outcome")
    }

    @Test
    fun getBrokerIpcTelemetryFromBundle_missingPerformanceRecord_returnsNull() {
        assertMissingRootFieldRejected("perf")
    }

    @Test
    fun getBrokerIpcTelemetryFromBundle_missingPerformanceFields_returnsNull() {
        listOf("version", "start_time", "duration", "execution_flow").forEach { field ->
            val json = gson.toJsonTree(telemetry).asJsonObject
            json.getAsJsonObject("perf").remove(field)

            assertNull(adapter.getBrokerIpcTelemetryFromBundle(bundleWith(json.toString())))
        }
    }

    /**
     * Broker and client ship independently, so a client can receive a tag added after it was
     * built. Gson's default enum adapter would yield null for that name and — because Gson
     * bypasses the Kotlin constructor — leave it in the non-nullable [ExecutionEvent.tag],
     * surfacing later as an NPE. It must degrade to [EventTag.Unknown] instead, and must not
     * discard the rest of the payload.
     */
    @Test
    fun getBrokerIpcTelemetryFromBundle_unrecognizedEventTag_degradesToUnknown() {
        val json = gson.toJsonTree(telemetry).asJsonObject
        firstEvent(json).addProperty("t", "SomeTagFromANewerBroker")

        val restored = adapter.getBrokerIpcTelemetryFromBundle(bundleWith(json.toString()))

        val original = telemetry.performanceRecord.executionFlow.first()
        val degraded = restored!!.performanceRecord.executionFlow.first()
        assertEquals(EventTag.Unknown, degraded.tag)
        // "Keeps the timeline intact" is the actual claim: only the label is lost, so every
        // other field on the degraded event must survive untouched.
        assertEquals(original.timestampMs, degraded.timestampMs)
        assertEquals(original.threadId, degraded.threadId)
        assertEquals(original.statusCode, degraded.statusCode)
        assertEquals(original.errorCode, degraded.errorCode)
        assertEquals("correlation-id", restored.correlationId)
    }

    /** The surrounding events keep their identity; only the unrecognized one degrades. */
    @Test
    fun getBrokerIpcTelemetryFromBundle_unrecognizedEventTag_leavesKnownTagsIntact() {
        val multiEvent = telemetry.copy(
            performanceRecord = telemetry.performanceRecord.copy(
                executionFlow = listOf(
                    ExecutionEvent(EventTag.BrokerNetworkCallStart, 1),
                    ExecutionEvent(EventTag.BrokerTokenAcquired, 2),
                    ExecutionEvent(EventTag.BrokerResponseSent, 3)
                )
            )
        )
        val json = gson.toJsonTree(multiEvent).asJsonObject
        eventAt(json, 1).addProperty("t", "SomeTagFromANewerBroker")

        val restored = adapter.getBrokerIpcTelemetryFromBundle(bundleWith(json.toString()))

        assertEquals(
            listOf(EventTag.BrokerNetworkCallStart, EventTag.Unknown, EventTag.BrokerResponseSent),
            restored!!.performanceRecord.executionFlow.map { it.tag }
        )
    }

    /**
     * An absent or null `t` is malformed rather than merely unrecognized — the sender broke the
     * contract — so it is rejected instead of degrading, in line with every other required field.
     */
    @Test
    fun getBrokerIpcTelemetryFromBundle_missingEventTag_returnsNull() {
        val json = gson.toJsonTree(telemetry).asJsonObject
        firstEvent(json).remove("t")

        assertNull(adapter.getBrokerIpcTelemetryFromBundle(bundleWith(json.toString())))
    }

    @Test
    fun getBrokerIpcTelemetryFromBundle_nullEventTag_returnsNull() {
        val json = gson.toJsonTree(telemetry).asJsonObject
        firstEvent(json).add("t", JsonNull.INSTANCE)

        assertNull(adapter.getBrokerIpcTelemetryFromBundle(bundleWith(json.toString())))
    }

    /**
     * Only a JSON string can be *unrecognized*; any other token is malformed and must reject the
     * payload rather than degrade. A number matters most here: [com.google.gson.stream.JsonReader]
     * would happily coerce `123` to the string `"123"`, which would otherwise slip through as an
     * unrecognized name and degrade to [EventTag.Unknown] while an object in the same position
     * dropped the whole payload.
     */
    @Test
    fun getBrokerIpcTelemetryFromBundle_nonStringEventTag_returnsNull() {
        val nonStringTags = listOf(
            JsonPrimitive(123),
            JsonPrimitive(true),
            JsonObject(),
            JsonArray()
        )

        nonStringTags.forEach { tagValue ->
            val json = gson.toJsonTree(telemetry).asJsonObject
            firstEvent(json).add("t", tagValue)

            assertNull(
                "Expected payload to be rejected for tag value: $tagValue",
                adapter.getBrokerIpcTelemetryFromBundle(bundleWith(json.toString()))
            )
        }
    }

    @Test
    fun getBrokerIpcTelemetryFromBundle_missingEventTimestamp_returnsNull() {
        val json = gson.toJsonTree(telemetry).asJsonObject
        firstEvent(json).remove("ts")

        assertNull(adapter.getBrokerIpcTelemetryFromBundle(bundleWith(json.toString())))
    }

    /**
     * [EventTag.Unknown] is a client-side sentinel, never a legitimate wire value, so it is
     * excluded from the name lookup. Receiving it resolves to the same constant but takes the
     * unrecognized branch, so a sentinel echoed back is logged rather than accepted silently.
     */
    @Test
    fun getBrokerIpcTelemetryFromBundle_sentinelEventTagOnWire_treatedAsUnrecognized() {
        val json = gson.toJsonTree(telemetry).asJsonObject
        firstEvent(json).addProperty("t", EventTag.Unknown.name)

        val restored = adapter.getBrokerIpcTelemetryFromBundle(bundleWith(json.toString()))

        assertEquals(
            EventTag.Unknown,
            restored!!.performanceRecord.executionFlow.first().tag
        )
    }

    @Test
    fun getAcquireTokenResultFromResultBundle_validTelemetry_setsTelemetryOnResult() {
        val cacheRecord = newCacheRecord()
        val records: MutableList<ICacheRecord> = arrayListOf(cacheRecord)
        val authResult = LocalAuthenticationResult(cacheRecord, records, SdkType.MSAL, false)
        val bundle = adapter.bundleFromAuthenticationResult(authResult, "16.0").apply {
            putString(
                AuthenticationConstants.Broker.BROKER_IPC_TELEMETRY,
                gson.toJson(telemetry)
            )
        }

        val result = adapter.getAcquireTokenResultFromResultBundle(bundle)

        assertEquals(telemetry, result.brokerIpcTelemetry)
    }

    @Test
    fun getBaseExceptionFromBundle_validTelemetry_setsTelemetryOnException() {
        val bundle = adapter.bundleFromBaseException(
            ClientException("test_error", "test message"),
            null
        ).apply {
            putString(
                AuthenticationConstants.Broker.BROKER_IPC_TELEMETRY,
                gson.toJson(telemetry)
            )
        }

        val exception = adapter.getBaseExceptionFromBundle(bundle)

        assertEquals(telemetry, exception.brokerIpcTelemetry)
    }

    private fun eventAt(root: JsonObject, index: Int) =
        root.getAsJsonObject("perf").getAsJsonArray("execution_flow")[index].asJsonObject

    private fun firstEvent(root: JsonObject) = eventAt(root, 0)

    private fun assertMissingRootFieldRejected(field: String) {
        val json = gson.toJsonTree(telemetry).asJsonObject
        json.remove(field)

        assertNull(adapter.getBrokerIpcTelemetryFromBundle(bundleWith(json.toString())))
    }

    private fun bundleWith(json: String) = Bundle().apply {
        putString(AuthenticationConstants.Broker.BROKER_IPC_TELEMETRY, json)
    }

    private fun newCacheRecord() = CacheRecord.builder()
        .account(MockRecords.getMockAccountRecord_AAD())
        .idToken(MockRecords.getMockIdTokenRecord_AAD())
        .accessToken(MockRecords.getMockAccessTokenRecord_AAD())
        .refreshToken(MockRecords.getMockRefreshTokenRecord_AAD())
        .build()
}
