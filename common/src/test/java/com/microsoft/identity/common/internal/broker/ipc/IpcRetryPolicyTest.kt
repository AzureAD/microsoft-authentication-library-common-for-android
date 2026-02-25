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
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import com.microsoft.identity.common.java.flighting.IFlightConfig
import com.microsoft.identity.common.java.flighting.IFlightsManager
import com.microsoft.identity.common.java.flighting.IFlightsProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
class IpcRetryPolicyTest {

    private val noopSleep = IpcRetryPolicy.SleepFunction { _ -> }

    private fun makePolicy(
        maxRetries: Int = IpcRetryPolicy.DEFAULT_MAX_RETRIES,
        baseDelayMs: Long = IpcRetryPolicy.DEFAULT_BASE_DELAY_MS
    ) = IpcRetryPolicy(maxRetries = maxRetries, baseDelayMs = baseDelayMs, sleepFunction = noopSleep)

    /** Sets up a FlightsManager that returns the given boolean for every flight. */
    private fun setFlightEnabled(enabled: Boolean) {
        CommonFlightsManager.initializeCommonFlightsManager(object : IFlightsManager {
            override fun getFlightsProvider(waitForConfigsWithTimeoutInMs: Long): IFlightsProvider =
                object : IFlightsProvider {
                    override fun isFlightEnabled(flightConfig: IFlightConfig): Boolean = enabled
                    override fun getBooleanValue(flightConfig: IFlightConfig): Boolean = enabled
                    override fun getIntValue(flightConfig: IFlightConfig): Int = flightConfig.defaultValue as Int
                    override fun getDoubleValue(flightConfig: IFlightConfig): Double = flightConfig.defaultValue as Double
                    override fun getStringValue(flightConfig: IFlightConfig): String = flightConfig.defaultValue as String
                    override fun getJsonValue(flightConfig: IFlightConfig): org.json.JSONObject =
                        flightConfig.defaultValue as org.json.JSONObject
                }

            override fun getFlightsProviderForTenant(
                tenantId: String,
                waitForConfigsWithTimeoutInMs: Long
            ): IFlightsProvider = getFlightsProvider(waitForConfigsWithTimeoutInMs)
        })
    }

    @Before
    fun setUp() {
        setFlightEnabled(true)
    }

    @After
    fun tearDown() {
        CommonFlightsManager.resetFlightsManager()
    }

    // ── isRetryable tests ────────────────────────────────────────────────────

    @Test
    fun isRetryable_connectionError_returnsTrue() {
        val policy = makePolicy()
        val exception = BrokerCommunicationException(
            BrokerCommunicationException.Category.CONNECTION_ERROR,
            IIpcStrategy.Type.BOUND_SERVICE,
            "connection failed",
            null
        )
        assertTrue(policy.isRetryable(exception))
    }

    @Test
    fun isRetryable_operationNotSupportedOnServerSide_returnsFalse() {
        val policy = makePolicy()
        val exception = BrokerCommunicationException(
            BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE,
            IIpcStrategy.Type.BOUND_SERVICE,
            "not supported",
            null
        )
        assertFalse(policy.isRetryable(exception))
    }

    @Test
    fun isRetryable_validationError_returnsFalse() {
        val policy = makePolicy()
        val exception = BrokerCommunicationException(
            BrokerCommunicationException.Category.VALIDATION_ERROR,
            IIpcStrategy.Type.BOUND_SERVICE,
            "validation failed",
            null
        )
        assertFalse(policy.isRetryable(exception))
    }

    @Test
    fun isRetryable_nonBrokerException_returnsFalse() {
        val policy = makePolicy()
        assertFalse(policy.isRetryable(RuntimeException("generic error")))
    }

    // ── calculateDelayMs tests ───────────────────────────────────────────────

    @Test
    fun calculateDelayMs_attempt0_isApproximately500ms() {
        val policy = IpcRetryPolicy(baseDelayMs = 500L, sleepFunction = noopSleep)
        val delay = policy.calculateDelayMs(0)
        assertTrue("Expected ~500ms, got $delay", delay in 500L..550L)
    }

    @Test
    fun calculateDelayMs_attempt1_isApproximately1000ms() {
        val policy = IpcRetryPolicy(baseDelayMs = 500L, sleepFunction = noopSleep)
        val delay = policy.calculateDelayMs(1)
        assertTrue("Expected ~1000ms, got $delay", delay in 1000L..1100L)
    }

    @Test
    fun calculateDelayMs_attempt2_isApproximately2000ms() {
        val policy = IpcRetryPolicy(baseDelayMs = 500L, sleepFunction = noopSleep)
        val delay = policy.calculateDelayMs(2)
        assertTrue("Expected ~2000ms, got $delay", delay in 2000L..2200L)
    }

    // ── execute tests ────────────────────────────────────────────────────────

    @Test
    fun execute_successOnFirstTry_returnsResultWithZeroRetries() {
        val policy = makePolicy()
        val retryCount = AtomicInteger(-1)
        val result = policy.execute("TestStrategy", "corr-id", retryCount) { "success" }
        assertEquals("success", result)
        assertEquals(0, retryCount.get())
    }

    @Test
    fun execute_connectionError_retriesAndSucceeds() {
        val policy = makePolicy(maxRetries = 3)
        val retryCount = AtomicInteger(-1)
        var callCount = 0

        val result = policy.execute("TestStrategy", "corr-id", retryCount) {
            callCount++
            if (callCount < 3) {
                throw BrokerCommunicationException(
                    BrokerCommunicationException.Category.CONNECTION_ERROR,
                    IIpcStrategy.Type.BOUND_SERVICE, "transient", null
                )
            }
            "ok"
        }

        assertEquals("ok", result)
        assertEquals(3, callCount)       // called 3 times
        assertEquals(2, retryCount.get()) // 2 retries performed
    }

    @Test
    fun execute_connectionError_maxRetriesExceeded_throwsLastException() {
        val policy = makePolicy(maxRetries = 3)
        val retryCount = AtomicInteger(-1)
        var callCount = 0

        try {
            policy.execute("TestStrategy", "corr-id", retryCount) {
                callCount++
                throw BrokerCommunicationException(
                    BrokerCommunicationException.Category.CONNECTION_ERROR,
                    IIpcStrategy.Type.BOUND_SERVICE, "transient", null
                )
            }
            fail("Expected BrokerCommunicationException")
        } catch (e: BrokerCommunicationException) {
            assertEquals(BrokerCommunicationException.Category.CONNECTION_ERROR, e.category)
        }

        assertEquals(4, callCount)        // 1 initial + 3 retries
        assertEquals(3, retryCount.get()) // 3 retries performed
    }

    @Test
    fun execute_operationNotSupportedOnServerSide_doesNotRetry() {
        val policy = makePolicy(maxRetries = 3)
        val retryCount = AtomicInteger(-1)
        var callCount = 0

        try {
            policy.execute("TestStrategy", "corr-id", retryCount) {
                callCount++
                throw BrokerCommunicationException(
                    BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE,
                    IIpcStrategy.Type.BOUND_SERVICE, "not supported", null
                )
            }
            fail("Expected BrokerCommunicationException")
        } catch (e: BrokerCommunicationException) {
            assertEquals(BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE, e.category)
        }

        assertEquals(1, callCount)        // called only once
        assertEquals(0, retryCount.get()) // no retries
    }

    @Test
    fun execute_featureFlagDisabled_doesNotRetry() {
        setFlightEnabled(false)
        val policy = makePolicy(maxRetries = 3)
        val retryCount = AtomicInteger(-1)
        var callCount = 0

        try {
            policy.execute("TestStrategy", "corr-id", retryCount) {
                callCount++
                throw BrokerCommunicationException(
                    BrokerCommunicationException.Category.CONNECTION_ERROR,
                    IIpcStrategy.Type.BOUND_SERVICE, "transient", null
                )
            }
            fail("Expected BrokerCommunicationException")
        } catch (e: BrokerCommunicationException) {
            assertEquals(BrokerCommunicationException.Category.CONNECTION_ERROR, e.category)
        }

        assertEquals(1, callCount)        // called only once despite CONNECTION_ERROR
        assertEquals(0, retryCount.get()) // no retries
    }

    @Test
    fun execute_retryThenSuccess_recordsCorrectRetryCount() {
        val policy = makePolicy(maxRetries = 3)
        val retryCount = AtomicInteger(-1)
        var callCount = 0

        val result = policy.execute("TestStrategy", "corr-id", retryCount) {
            callCount++
            if (callCount == 1) {
                throw BrokerCommunicationException(
                    BrokerCommunicationException.Category.CONNECTION_ERROR,
                    IIpcStrategy.Type.BOUND_SERVICE, "transient", null
                )
            }
            "recovered"
        }

        assertEquals("recovered", result)
        assertEquals(2, callCount)
        assertEquals(1, retryCount.get())
    }

    @Test
    fun execute_sleepIsCalledBetweenRetries() {
        val sleepDelays = mutableListOf<Long>()
        val trackingSleep = IpcRetryPolicy.SleepFunction { millis -> sleepDelays.add(millis) }
        val policy = IpcRetryPolicy(maxRetries = 3, baseDelayMs = 500L, sleepFunction = trackingSleep)
        val retryCount = AtomicInteger(-1)

        try {
            policy.execute("TestStrategy", "corr-id", retryCount) {
                throw BrokerCommunicationException(
                    BrokerCommunicationException.Category.CONNECTION_ERROR,
                    IIpcStrategy.Type.BOUND_SERVICE, "transient", null
                )
            }
        } catch (e: BrokerCommunicationException) {
            // Expected: the exception is thrown after all retries are exhausted.
            assertEquals(BrokerCommunicationException.Category.CONNECTION_ERROR, e.category)
        }

        assertEquals(3, sleepDelays.size)
        // First delay should be in the ~500ms range (with up to 10% jitter)
        assertTrue(sleepDelays[0] in 500L..550L)
        // Second delay should be in the ~1000ms range
        assertTrue(sleepDelays[1] in 1000L..1100L)
        // Third delay should be in the ~2000ms range
        assertTrue(sleepDelays[2] in 2000L..2200L)
    }
}
