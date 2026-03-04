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

import android.os.Build
import com.microsoft.identity.common.exception.BrokerCommunicationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.N])
class IpcRetryPolicyTest {

    private fun makeConnectionError(): BrokerCommunicationException =
        BrokerCommunicationException(
            BrokerCommunicationException.Category.CONNECTION_ERROR,
            IIpcStrategy.Type.BOUND_SERVICE,
            "connection error",
            null
        )

    private fun makeOperationNotSupportedOnServerSide(): BrokerCommunicationException =
        BrokerCommunicationException(
            BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE,
            IIpcStrategy.Type.BOUND_SERVICE,
            "not supported on server",
            null
        )

    private fun makeOperationNotSupportedOnClientSide(): BrokerCommunicationException =
        BrokerCommunicationException(
            BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_CLIENT_SIDE,
            IIpcStrategy.Type.BOUND_SERVICE,
            "not supported on client",
            null
        )

    // shouldRetry returns true for CONNECTION_ERROR
    @Test
    fun shouldRetry_connectionError_returnsTrue() {
        val policy = IpcRetryPolicy()
        assertTrue(policy.shouldRetry(makeConnectionError()))
    }

    // shouldRetry returns false for OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE
    @Test
    fun shouldRetry_operationNotSupportedOnServerSide_returnsFalse() {
        val policy = IpcRetryPolicy()
        assertFalse(policy.shouldRetry(makeOperationNotSupportedOnServerSide()))
    }

    // shouldRetry returns false for OPERATION_NOT_SUPPORTED_ON_CLIENT_SIDE
    @Test
    fun shouldRetry_operationNotSupportedOnClientSide_returnsFalse() {
        val policy = IpcRetryPolicy()
        assertFalse(policy.shouldRetry(makeOperationNotSupportedOnClientSide()))
    }

    // getDelayMs returns increasing delays for successive attempts
    @Test
    fun getDelayMs_returnsIncreasingDelays() {
        val policy = IpcRetryPolicy(baseDelayMs = 500L)
        val delay0 = policy.getDelayMs(0)
        val delay1 = policy.getDelayMs(1)
        val delay2 = policy.getDelayMs(2)

        // Each delay should be at least the exponential base (jitter only adds to it)
        assertTrue("delay0 should be >= 500", delay0 >= 500L)
        assertTrue("delay1 should be >= 1000", delay1 >= 1000L)
        assertTrue("delay2 should be >= 2000", delay2 >= 2000L)

        // Each tier's minimum is greater than the previous tier's maximum (jitter <= 10%)
        // delay0 max = 500 + 10% = 550; delay1 min = 1000 => delay0 < delay1
        // delay1 max = 1000 + 10% = 1100; delay2 min = 2000 => delay1 < delay2
        assertTrue("delay0 should be < delay1", delay0 < delay1)
        assertTrue("delay1 should be < delay2", delay1 < delay2)
    }

    // default maxRetries is 3
    @Test
    fun defaultMaxRetries_isThree() {
        val policy = IpcRetryPolicy()
        assertEquals(IpcRetryPolicy.DEFAULT_MAX_RETRIES, policy.maxRetries)
        assertEquals(3, policy.maxRetries)
    }

    // default baseDelayMs is 500
    @Test
    fun defaultBaseDelayMs_is500() {
        val policy = IpcRetryPolicy()
        assertEquals(IpcRetryPolicy.DEFAULT_BASE_DELAY_MS, policy.baseDelayMs)
        assertEquals(500L, policy.baseDelayMs)
    }

    // custom maxRetries and baseDelayMs are respected
    @Test
    fun customParameters_areRespected() {
        val policy = IpcRetryPolicy(maxRetries = 5, baseDelayMs = 100L)
        assertEquals(5, policy.maxRetries)
        assertEquals(100L, policy.baseDelayMs)
        assertTrue(policy.getDelayMs(0) >= 100L)
        assertTrue(policy.getDelayMs(1) >= 200L)
    }
}
