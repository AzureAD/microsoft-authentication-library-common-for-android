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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test

class IpcRetryPolicyTest {

    private val connectionError = BrokerCommunicationException(
        BrokerCommunicationException.Category.CONNECTION_ERROR,
        IIpcStrategy.Type.BOUND_SERVICE,
        "Connection failed",
        null
    )

    private val nonSupportedError = BrokerCommunicationException(
        BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE,
        IIpcStrategy.Type.BOUND_SERVICE,
        "Operation not supported",
        null
    )

    /**
     * Policy with zero jitter and minimal delay for fast tests.
     */
    private fun fastPolicy(maxRetries: Int = 3) = IpcRetryPolicy(
        maxRetries = maxRetries,
        baseDelayMs = 0L,
        jitterRangeMs = 0L
    )

    @Test
    fun isRetryable_connectionError_returnsTrue() {
        assert(IpcRetryPolicy.isRetryable(connectionError))
    }

    @Test
    fun isRetryable_operationNotSupported_returnsFalse() {
        assert(!IpcRetryPolicy.isRetryable(nonSupportedError))
    }

    @Test
    fun isRetryable_validationError_returnsFalse() {
        val validationError = BrokerCommunicationException(
            BrokerCommunicationException.Category.VALIDATION_ERROR,
            IIpcStrategy.Type.BOUND_SERVICE,
            "Validation failed",
            null
        )
        assert(!IpcRetryPolicy.isRetryable(validationError))
    }

    @Test
    fun executeWithRetry_successOnFirstAttempt_returnsResultWithZeroRetries() {
        val policy = fastPolicy()
        val (result, retryCount) = policy.executeWithRetry("TestStrategy", "test-correlation") {
            "success"
        }
        assertEquals("success", result)
        assertEquals(0, retryCount)
    }

    @Test
    fun executeWithRetry_successAfterOneConnectionError_returnsResultWithOneRetry() {
        val policy = fastPolicy()
        var attempt = 0
        val (result, retryCount) = policy.executeWithRetry("TestStrategy", "test-correlation") {
            attempt++
            if (attempt == 1) throw connectionError
            "success"
        }
        assertEquals("success", result)
        assertEquals(1, retryCount)
    }

    @Test
    fun executeWithRetry_successAfterMaxRetries_returnsResultWithMaxRetries() {
        val maxRetries = 3
        val policy = fastPolicy(maxRetries = maxRetries)
        var attempt = 0
        val (result, retryCount) = policy.executeWithRetry("TestStrategy", "test-correlation") {
            attempt++
            if (attempt <= maxRetries) throw connectionError
            "success"
        }
        assertEquals("success", result)
        assertEquals(maxRetries, retryCount)
    }

    @Test
    fun executeWithRetry_allAttemptsFailWithConnectionError_throwsAfterMaxRetries() {
        val maxRetries = 3
        val policy = fastPolicy(maxRetries = maxRetries)
        var callCount = 0
        try {
            policy.executeWithRetry("TestStrategy", "test-correlation") {
                callCount++
                throw connectionError
            }
            fail("Expected BrokerCommunicationException to be thrown")
        } catch (e: BrokerCommunicationException) {
            // Expected: exhausted all retries
            assertEquals(BrokerCommunicationException.Category.CONNECTION_ERROR, e.category)
            // Expect: 1 initial attempt + maxRetries retries
            assertEquals(maxRetries + 1, callCount)
        }
    }

    @Test
    fun executeWithRetry_nonRetryableException_propagatesImmediatelyWithoutRetry() {
        val policy = fastPolicy()
        var callCount = 0
        try {
            policy.executeWithRetry("TestStrategy", "test-correlation") {
                callCount++
                throw nonSupportedError
            }
            fail("Expected BrokerCommunicationException to be thrown")
        } catch (e: BrokerCommunicationException) {
            assertEquals(BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE, e.category)
            assertEquals(1, callCount)
        }
    }

    @Test
    fun executeWithRetry_nullCorrelationId_doesNotThrow() {
        val policy = fastPolicy()
        val (result, _) = policy.executeWithRetry("TestStrategy", null) { "ok" }
        assertEquals("ok", result)
    }

    @Test
    fun defaultPolicy_hasExpectedDefaults() {
        val policy = IpcRetryPolicy()
        assertEquals(IpcRetryPolicy.DEFAULT_MAX_RETRIES, policy.maxRetries)
        assertEquals(IpcRetryPolicy.DEFAULT_BASE_DELAY_MS, policy.baseDelayMs)
        assertEquals(IpcRetryPolicy.DEFAULT_JITTER_RANGE_MS, policy.jitterRangeMs)
    }

    @Test
    fun executeWithRetry_connectionErrorThenNonRetryableError_propagatesNonRetryable() {
        val policy = fastPolicy()
        var attempt = 0
        try {
            policy.executeWithRetry("TestStrategy", "test-correlation") {
                attempt++
                if (attempt == 1) throw connectionError
                throw nonSupportedError
            }
            fail("Expected BrokerCommunicationException to be thrown")
        } catch (e: BrokerCommunicationException) {
            assertEquals(BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE, e.category)
            assertEquals(2, attempt)
        }
    }

    @Test
    fun executeWithRetry_result_isNotNull() {
        val policy = fastPolicy()
        val (result, _) = policy.executeWithRetry("TestStrategy", "test-correlation") {
            "non-null-result"
        }
        assertNotNull(result)
    }
}
