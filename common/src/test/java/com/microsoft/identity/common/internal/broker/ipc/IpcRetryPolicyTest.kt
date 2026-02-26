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
import com.microsoft.identity.common.internal.mocks.MockCommonFlightsManager
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import com.microsoft.identity.common.java.flighting.IFlightsProvider
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IpcRetryPolicyTest {

    @After
    fun tearDown() {
        CommonFlightsManager.resetFlightsManager()
    }

    // region isRetryable

    @Test
    fun isRetryable_connectionError_returnsTrue() {
        val policy = IpcRetryPolicy()
        val exception = BrokerCommunicationException(
            BrokerCommunicationException.Category.CONNECTION_ERROR,
            IIpcStrategy.Type.BOUND_SERVICE,
            "connection error",
            null
        )
        assertTrue(policy.isRetryable(exception))
    }

    @Test
    fun isRetryable_operationNotSupportedOnServerSide_returnsFalse() {
        val policy = IpcRetryPolicy()
        val exception = BrokerCommunicationException(
            BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE,
            IIpcStrategy.Type.BOUND_SERVICE,
            "not supported on server",
            null
        )
        assertFalse(policy.isRetryable(exception))
    }

    @Test
    fun isRetryable_operationNotSupportedOnClientSide_returnsFalse() {
        val policy = IpcRetryPolicy()
        val exception = BrokerCommunicationException(
            BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_CLIENT_SIDE,
            IIpcStrategy.Type.BOUND_SERVICE,
            "not supported on client",
            null
        )
        assertFalse(policy.isRetryable(exception))
    }

    @Test
    fun isRetryable_validationError_returnsFalse() {
        val policy = IpcRetryPolicy()
        val exception = BrokerCommunicationException(
            BrokerCommunicationException.Category.VALIDATION_ERROR,
            IIpcStrategy.Type.BOUND_SERVICE,
            "validation error",
            null
        )
        assertFalse(policy.isRetryable(exception))
    }

    @Test
    fun isRetryable_nullCursor_returnsFalse() {
        val policy = IpcRetryPolicy()
        val exception = BrokerCommunicationException(
            BrokerCommunicationException.Category.NULL_CURSOR,
            IIpcStrategy.Type.BOUND_SERVICE,
            "null cursor",
            null
        )
        assertFalse(policy.isRetryable(exception))
    }

    @Test
    fun isRetryable_nonBrokerException_returnsFalse() {
        val policy = IpcRetryPolicy()
        assertFalse(policy.isRetryable(RuntimeException("not a broker exception")))
    }

    // endregion

    // region getDelayMs

    @Test
    fun getDelayMs_attempt0_returnsAroundBaseDelay() {
        val policy = IpcRetryPolicy(baseDelayMs = 500L, jitterMs = 100L)
        // Run multiple times to account for random jitter
        repeat(20) {
            val delay = policy.getDelayMs(0)
            assertTrue("Delay $delay should be between 400 and 600ms", delay in 400L..600L)
        }
    }

    @Test
    fun getDelayMs_attempt1_returnsAroundDoubledBaseDelay() {
        val policy = IpcRetryPolicy(baseDelayMs = 500L, jitterMs = 100L)
        repeat(20) {
            val delay = policy.getDelayMs(1)
            assertTrue("Delay $delay should be between 900 and 1100ms", delay in 900L..1100L)
        }
    }

    @Test
    fun getDelayMs_attempt2_returnsAroundQuadrupledBaseDelay() {
        val policy = IpcRetryPolicy(baseDelayMs = 500L, jitterMs = 100L)
        repeat(20) {
            val delay = policy.getDelayMs(2)
            assertTrue("Delay $delay should be between 1900 and 2100ms", delay in 1900L..2100L)
        }
    }

    @Test
    fun getDelayMs_neverNegative() {
        val policy = IpcRetryPolicy(baseDelayMs = 0L, jitterMs = 100L)
        repeat(20) {
            val delay = policy.getDelayMs(0)
            assertTrue("Delay $delay should be >= 0", delay >= 0L)
        }
    }

    // endregion

    // region isEnabled

    @Test
    fun isEnabled_flagOffByDefault_returnsFalse() {
        val policy = IpcRetryPolicy()
        assertFalse(policy.isEnabled())
    }

    @Test
    fun isEnabled_flagOn_returnsTrue() {
        val mockFlightsProvider = Mockito.mock(IFlightsProvider::class.java)
        Mockito.`when`(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_IPC_RETRY_WITH_EXPONENTIAL_BACKOFF))
            .thenReturn(true)
        val mockManager = MockCommonFlightsManager()
        mockManager.mockCommonFlightsProvider = mockFlightsProvider
        CommonFlightsManager.initializeCommonFlightsManager(mockManager)

        val policy = IpcRetryPolicy()
        assertTrue(policy.isEnabled())
    }

    // endregion
}
