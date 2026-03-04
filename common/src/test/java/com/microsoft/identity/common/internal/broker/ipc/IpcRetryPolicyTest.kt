//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.internal.broker.ipc

import com.microsoft.identity.common.exception.BrokerCommunicationException
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
     * When no [IFlightsManager] is configured, [IpcRetryPolicy.isEnabled] should return false
     * (the default value for [CommonFlight.ENABLE_IPC_RETRY_WITH_EXPONENTIAL_BACKOFF]).
     */
    @Test
    fun isEnabled_whenNoFlightsManager_returnsFalse() {
        val policy = IpcRetryPolicy()
        assertFalse("isEnabled should default to false", policy.isEnabled())
    }

    /**
     * When the flight flag is enabled via a custom [IFlightsManager], [IpcRetryPolicy.isEnabled]
     * should return true.
     */
    @Test
    fun isEnabled_whenFlightEnabled_returnsTrue() {
        registerFlightsManager(enableIpcRetry = true)
        val policy = IpcRetryPolicy()
        assertTrue("isEnabled should be true when flight is on", policy.isEnabled())
    }

    /**
     * [IpcRetryPolicy.isRetryable] should return true only for
     * [BrokerCommunicationException.Category.CONNECTION_ERROR].
     */
    @Test
    fun isRetryable_connectionError_returnsTrue() {
        val policy = IpcRetryPolicy()
        val exception = BrokerCommunicationException(
            BrokerCommunicationException.Category.CONNECTION_ERROR,
            IIpcStrategy.Type.BOUND_SERVICE,
            "connection failed",
            null
        )
        assertTrue("CONNECTION_ERROR should be retryable", policy.isRetryable(exception))
    }

    /**
     * [IpcRetryPolicy.isRetryable] should return false for
     * [BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE].
     */
    @Test
    fun isRetryable_operationNotSupportedOnServerSide_returnsFalse() {
        val policy = IpcRetryPolicy()
        val exception = BrokerCommunicationException(
            BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE,
            IIpcStrategy.Type.BOUND_SERVICE,
            "not supported",
            null
        )
        assertFalse("OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE should not be retryable", policy.isRetryable(exception))
    }

    /**
     * [IpcRetryPolicy.isRetryable] should return false for
     * [BrokerCommunicationException.Category.VALIDATION_ERROR].
     */
    @Test
    fun isRetryable_validationError_returnsFalse() {
        val policy = IpcRetryPolicy()
        val exception = BrokerCommunicationException(
            BrokerCommunicationException.Category.VALIDATION_ERROR,
            IIpcStrategy.Type.BOUND_SERVICE,
            "validation failed",
            null
        )
        assertFalse("VALIDATION_ERROR should not be retryable", policy.isRetryable(exception))
    }

    /**
     * [IpcRetryPolicy.isRetryable] should return false for arbitrary non-[BrokerCommunicationException]
     * throwables (e.g. [RuntimeException]).
     */
    @Test
    fun isRetryable_nonBrokerCommunicationException_returnsFalse() {
        val policy = IpcRetryPolicy()
        assertFalse("RuntimeException should not be retryable", policy.isRetryable(RuntimeException("boom")))
    }

    /**
     * [IpcRetryPolicy.getMaxRetries] should return 3.
     */
    @Test
    fun getMaxRetries_returnsThree() {
        val policy = IpcRetryPolicy()
        assertEquals("maxRetries should be 3", 3, policy.getMaxRetries())
    }

    /**
     * [IpcRetryPolicy.getDelayMs] should return values within the expected range for each attempt.
     * - attempt 0 → [400, 600] ms (500 ± 100)
     * - attempt 1 → [900, 1100] ms (1000 ± 100)
     * - attempt 2 → [1900, 2100] ms (2000 ± 100)
     */
    @Test
    fun getDelayMs_returnsValuesInExpectedRange() {
        val policy = IpcRetryPolicy()
        val attempt0 = policy.getDelayMs(0)
        assertTrue("attempt 0 delay should be >= 400ms", attempt0 >= 400L)
        assertTrue("attempt 0 delay should be <= 600ms", attempt0 <= 600L)

        val attempt1 = policy.getDelayMs(1)
        assertTrue("attempt 1 delay should be >= 900ms", attempt1 >= 900L)
        assertTrue("attempt 1 delay should be <= 1100ms", attempt1 <= 1100L)

        val attempt2 = policy.getDelayMs(2)
        assertTrue("attempt 2 delay should be >= 1900ms", attempt2 >= 1900L)
        assertTrue("attempt 2 delay should be <= 2100ms", attempt2 <= 2100L)
    }

    /**
     * [IpcRetryPolicy.sleepBeforeRetry] should invoke the injected sleep function with the
     * exact delay passed in.
     */
    @Test
    fun sleepBeforeRetry_callsSleepFunctionWithCorrectDelay() {
        var sleptMs = -1L
        val policy = IpcRetryPolicy(sleepFunction = { ms -> sleptMs = ms })

        policy.sleepBeforeRetry(300L)

        assertEquals("sleepBeforeRetry should call sleep with the given delay", 300L, sleptMs)
    }

    /**
     * [IpcRetryPolicy.sleepBeforeRetry] should restore the interrupt flag and return normally
     * when the sleep function throws [InterruptedException].
     */
    @Test
    fun sleepBeforeRetry_interruptedException_restoresInterruptFlag() {
        val policy = IpcRetryPolicy(sleepFunction = { throw InterruptedException("interrupted") })

        Thread.interrupted() // clear any pre-existing interrupt before testing
        policy.sleepBeforeRetry(500L)

        assertTrue("interrupt flag should be restored after InterruptedException",
            Thread.currentThread().isInterrupted)
        // Clean up interrupt flag so subsequent tests are not affected
        Thread.interrupted()
    }

    // ─── helpers ───────────────────────────────────────────────────────────────

    private fun registerFlightsManager(enableIpcRetry: Boolean) {
        val provider = object : IFlightsProvider {
            override fun isFlightEnabled(flightConfig: IFlightConfig) = getBooleanValue(flightConfig)
            override fun getBooleanValue(flightConfig: IFlightConfig) =
                flightConfig == CommonFlight.ENABLE_IPC_RETRY_WITH_EXPONENTIAL_BACKOFF && enableIpcRetry
            override fun getIntValue(flightConfig: IFlightConfig) = 0
            override fun getDoubleValue(flightConfig: IFlightConfig) = 0.0
            override fun getStringValue(flightConfig: IFlightConfig) = ""
            override fun getJsonValue(flightConfig: IFlightConfig) = JSONObject()
        }
        val manager = object : IFlightsManager {
            override fun getFlightsProvider(waitForConfigsWithTimeoutInMs: Long) = provider
            override fun getFlightsProviderForTenant(
                tenantId: String,
                waitForConfigsWithTimeoutInMs: Long
            ) = provider
        }
        CommonFlightsManager.initializeCommonFlightsManager(manager)
    }
}
