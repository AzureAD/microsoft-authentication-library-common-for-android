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
import com.microsoft.identity.common.java.exception.ClientException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class IpcRetryPolicyTest {

    companion object {
        private const val STRATEGY_NAME = "TestStrategy"
        private const val CORRELATION_ID = "test-correlation-id"
        private val CONNECTION_ERROR_EXCEPTION = BrokerCommunicationException(
            BrokerCommunicationException.Category.CONNECTION_ERROR,
            IIpcStrategy.Type.BOUND_SERVICE,
            "Connection error",
            null
        )
        private val NOT_SUPPORTED_EXCEPTION = BrokerCommunicationException(
            BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE,
            IIpcStrategy.Type.BOUND_SERVICE,
            "Not supported",
            null
        )
    }

    /** No-op sleep function to keep tests fast. */
    private val noSleep: (Long) -> Unit = {}

    private fun makePolicy(maxRetries: Int = IpcRetryPolicy.DEFAULT_MAX_RETRIES) =
        IpcRetryPolicy(
            maxRetries = maxRetries,
            jitterFraction = 0.0,
            sleepFn = noSleep
        )

    // -------------------------------------------------------------------------
    // Success on first attempt – no retry should be needed
    // -------------------------------------------------------------------------

    @Test
    fun executeWithRetry_successOnFirstAttempt_returnsResultWithZeroRetries() {
        val policy = makePolicy()
        val result = policy.executeWithRetry(STRATEGY_NAME, CORRELATION_ID) { "success" }
        assertEquals("success", result.value)
        assertEquals(0, result.retryCount)
    }

    // -------------------------------------------------------------------------
    // CONNECTION_ERROR → retry → eventual success
    // -------------------------------------------------------------------------

    @Test
    fun executeWithRetry_connectionErrorThenSuccess_retriesAndReturnsResult() {
        val policy = makePolicy(maxRetries = 3)
        var attempt = 0
        val result = policy.executeWithRetry(STRATEGY_NAME, CORRELATION_ID) {
            attempt++
            if (attempt < 3) {
                throw CONNECTION_ERROR_EXCEPTION
            }
            "recovered"
        }
        assertEquals("recovered", result.value)
        assertEquals(2, result.retryCount) // 2 retries before succeeding on attempt 3
    }

    // -------------------------------------------------------------------------
    // Max retries exhausted – should throw CONNECTION_ERROR after all attempts
    // -------------------------------------------------------------------------

    @Test
    fun executeWithRetry_connectionErrorExhaustsRetries_throwsException() {
        val policy = makePolicy(maxRetries = 3)
        try {
            policy.executeWithRetry(STRATEGY_NAME, CORRELATION_ID) {
                throw CONNECTION_ERROR_EXCEPTION
            }
            fail("Expected BrokerCommunicationException to be thrown")
        } catch (e: BrokerCommunicationException) {
            assertEquals(BrokerCommunicationException.Category.CONNECTION_ERROR, e.category)
        }
    }

    @Test
    fun executeWithRetry_connectionError_performsCorrectNumberOfAttempts() {
        val policy = makePolicy(maxRetries = 3)
        var callCount = 0
        try {
            policy.executeWithRetry(STRATEGY_NAME, CORRELATION_ID) {
                callCount++
                throw CONNECTION_ERROR_EXCEPTION
            }
        } catch (e: BrokerCommunicationException) {
            // Expected
        }
        // Initial attempt + maxRetries = 1 + 3 = 4 total calls
        assertEquals(4, callCount)
    }

    // -------------------------------------------------------------------------
    // OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE – must NOT be retried
    // -------------------------------------------------------------------------

    @Test
    fun executeWithRetry_notSupportedError_propagatesImmediatelyWithoutRetry() {
        val policy = makePolicy(maxRetries = 3)
        var callCount = 0
        try {
            policy.executeWithRetry(STRATEGY_NAME, CORRELATION_ID) {
                callCount++
                throw NOT_SUPPORTED_EXCEPTION
            }
            fail("Expected BrokerCommunicationException to be thrown")
        } catch (e: BrokerCommunicationException) {
            assertEquals(BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE, e.category)
        }
        assertEquals(1, callCount) // Must not retry
    }

    // -------------------------------------------------------------------------
    // Non-BrokerCommunicationException (BaseException subclass) – must NOT retry
    // -------------------------------------------------------------------------

    @Test
    fun executeWithRetry_baseException_propagatesImmediatelyWithoutRetry() {
        val policy = makePolicy(maxRetries = 3)
        var callCount = 0
        val clientException = ClientException("error_code", "Auth error")
        try {
            policy.executeWithRetry(STRATEGY_NAME, CORRELATION_ID) {
                callCount++
                throw clientException
            }
            fail("Expected ClientException to be thrown")
        } catch (e: ClientException) {
            assertEquals("error_code", e.errorCode)
        }
        assertEquals(1, callCount) // Must not retry
    }

    // -------------------------------------------------------------------------
    // computeDelayMs – verify exponential back-off without jitter
    // -------------------------------------------------------------------------

    @Test
    fun computeDelayMs_withZeroJitter_returnsExponentialValues() {
        val policy = IpcRetryPolicy(
            baseDelayMs = 500L,
            jitterFraction = 0.0,
            sleepFn = noSleep
        )
        assertEquals(500L, policy.computeDelayMs(0))   // 500 * 2^0 = 500
        assertEquals(1000L, policy.computeDelayMs(1))  // 500 * 2^1 = 1000
        assertEquals(2000L, policy.computeDelayMs(2))  // 500 * 2^2 = 2000
    }

    @Test
    fun computeDelayMs_withJitter_returnsValueWithinExpectedRange() {
        val policy = IpcRetryPolicy(
            baseDelayMs = 500L,
            jitterFraction = 0.2,
            sleepFn = noSleep
        )
        val delay = policy.computeDelayMs(0) // base = 500, max jitter = 100
        assertTrue("Delay should be >= base delay", delay >= 500L)
        assertTrue("Delay should be < base + max jitter + 1", delay < 601L)
    }

    // -------------------------------------------------------------------------
    // Feature: sleep is invoked with computed back-off delay during retries
    // -------------------------------------------------------------------------

    @Test
    fun executeWithRetry_connectionErrors_sleepCalledWithExpectedDelays() {
        val sleepDelays = mutableListOf<Long>()
        val policy = IpcRetryPolicy(
            maxRetries = 3,
            baseDelayMs = 500L,
            jitterFraction = 0.0,
            sleepFn = { ms -> sleepDelays.add(ms) }
        )
        var attempt = 0
        try {
            policy.executeWithRetry(STRATEGY_NAME, CORRELATION_ID) {
                attempt++
                throw CONNECTION_ERROR_EXCEPTION
            }
        } catch (_: BrokerCommunicationException) { }

        assertEquals(3, sleepDelays.size) // 3 retries → 3 sleeps
        assertEquals(500L, sleepDelays[0])
        assertEquals(1000L, sleepDelays[1])
        assertEquals(2000L, sleepDelays[2])
    }
}
