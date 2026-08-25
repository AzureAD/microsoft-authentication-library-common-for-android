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

    @Test
    fun getAcquireTokenResultFromResultBundle_validTelemetry_setsTelemetryOnResult() {
        val cacheRecord = newCacheRecord()
        val records: MutableList<ICacheRecord> = arrayListOf(cacheRecord)
        val authResult = LocalAuthenticationResult(cacheRecord, records, SdkType.MSAL, false)
        val bundle = adapter.bundleFromAuthenticationResult(authResult, "16.0").apply {
            putString(
                AuthenticationConstants.BrokerContentProvider.BROKER_IPC_TELEMETRY,
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
                AuthenticationConstants.BrokerContentProvider.BROKER_IPC_TELEMETRY,
                gson.toJson(telemetry)
            )
        }

        val exception = adapter.getBaseExceptionFromBundle(bundle)

        assertEquals(telemetry, exception.brokerIpcTelemetry)
    }

    private fun assertMissingRootFieldRejected(field: String) {
        val json = gson.toJsonTree(telemetry).asJsonObject
        json.remove(field)

        assertNull(adapter.getBrokerIpcTelemetryFromBundle(bundleWith(json.toString())))
    }

    private fun bundleWith(json: String) = Bundle().apply {
        putString(AuthenticationConstants.BrokerContentProvider.BROKER_IPC_TELEMETRY, json)
    }

    private fun newCacheRecord() = CacheRecord.builder()
        .account(MockRecords.getMockAccountRecord_AAD())
        .idToken(MockRecords.getMockIdTokenRecord_AAD())
        .accessToken(MockRecords.getMockAccessTokenRecord_AAD())
        .refreshToken(MockRecords.getMockRefreshTokenRecord_AAD())
        .build()
}
