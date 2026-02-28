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
package com.microsoft.identity.common.java.ipc

import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import com.microsoft.identity.common.java.flighting.IFlightConfig
import com.microsoft.identity.common.java.flighting.IFlightsManager
import com.microsoft.identity.common.java.flighting.IFlightsProvider
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [IpcRetryPolicy].
 */
class IpcRetryPolicyTest {

    @After
    fun tearDown() {
        CommonFlightsManager.resetFlightsManager()
    }

    /**
     * When no [IFlightsManager] is configured, [IpcRetryPolicy.fromFlights] should return
     * the default values defined by each [CommonFlight] entry.
     */
    @Test
    fun fromFlights_defaultValues_returnsFlightDefaults() {
        val policy = IpcRetryPolicy.fromFlights()

        assertFalse("enabled should default to false", policy.enabled)
        assertEquals("maxRetries should default to 3", 3, policy.maxRetries)
        assertEquals("initialDelayMs should default to 100", 100, policy.initialDelayMs)
        assertEquals("extensionFactor should default to 2", 2, policy.extensionFactor)
        assertEquals("maxDelayMs should default to 2000", 2000, policy.maxDelayMs)
    }

    /**
     * When a custom [IFlightsManager] is registered, [IpcRetryPolicy.fromFlights] should read
     * values from the configured provider rather than the defaults.
     */
    @Test
    fun fromFlights_customValues_readsFromFlightsProvider() {
        val customProvider = object : IFlightsProvider {
            override fun isFlightEnabled(flightConfig: IFlightConfig) =
                getBooleanValue(flightConfig)

            override fun getBooleanValue(flightConfig: IFlightConfig) =
                flightConfig == CommonFlight.ENABLE_IPC_RETRY

            override fun getIntValue(flightConfig: IFlightConfig) = when (flightConfig) {
                CommonFlight.IPC_RETRY_COUNT -> 5
                CommonFlight.IPC_RETRY_INITIAL_DELAY_MS -> 200
                CommonFlight.IPC_RETRY_EXTENSION_FACTOR -> 3
                CommonFlight.IPC_RETRY_MAX_DELAY_MS -> 5000
                else -> 0
            }

            override fun getDoubleValue(flightConfig: IFlightConfig) = 0.0
            override fun getStringValue(flightConfig: IFlightConfig) = ""
            override fun getJsonValue(flightConfig: IFlightConfig) = JSONObject()
        }

        val customManager = object : IFlightsManager {
            override fun getFlightsProvider(waitForConfigsWithTimeoutInMs: Long) = customProvider
            override fun getFlightsProviderForTenant(
                tenantId: String,
                waitForConfigsWithTimeoutInMs: Long
            ) = customProvider
        }

        CommonFlightsManager.initializeCommonFlightsManager(customManager)

        val policy = IpcRetryPolicy.fromFlights()

        assertTrue("enabled should be true from custom provider", policy.enabled)
        assertEquals("maxRetries should be 5 from custom provider", 5, policy.maxRetries)
        assertEquals("initialDelayMs should be 200 from custom provider", 200, policy.initialDelayMs)
        assertEquals("extensionFactor should be 3 from custom provider", 3, policy.extensionFactor)
        assertEquals("maxDelayMs should be 5000 from custom provider", 5000, policy.maxDelayMs)
    }

    /**
     * [IpcRetryPolicy] should accept maxRetries=0 (no retries after initial attempt).
     */
    @Test
    fun dataClass_zeroRetries_isAccepted() {
        val policy = IpcRetryPolicy(
            enabled = true,
            maxRetries = 0,
            initialDelayMs = 100,
            extensionFactor = 2,
            maxDelayMs = 2000
        )

        assertEquals("maxRetries=0 should be stored as-is", 0, policy.maxRetries)
        assertTrue("enabled=true should be stored as-is", policy.enabled)
    }

    /**
     * Two [IpcRetryPolicy] instances with identical field values must be equal.
     */
    @Test
    fun dataClass_sameValues_areEqual() {
        val policy1 = IpcRetryPolicy(
            enabled = false,
            maxRetries = 3,
            initialDelayMs = 100,
            extensionFactor = 2,
            maxDelayMs = 2000
        )
        val policy2 = IpcRetryPolicy(
            enabled = false,
            maxRetries = 3,
            initialDelayMs = 100,
            extensionFactor = 2,
            maxDelayMs = 2000
        )

        assertEquals("Two instances with identical values should be equal", policy1, policy2)
    }
}
