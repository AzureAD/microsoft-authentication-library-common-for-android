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

import android.os.Bundle
import com.microsoft.identity.common.internal.broker.BrokerData
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IpcStrategyWithRetryTests {

    companion object {
        val mockRequest = BrokerOperationBundle(
            BrokerOperationBundle.Operation.BROKER_DISCOVERY_METADATA_RETRIEVAL,
            BrokerData.prodMicrosoftAuthenticator.packageName,
            Bundle().apply { putBoolean("REQUEST", true) }
        )

        val mockResultBundle = Bundle().apply { putBoolean("RESULT", true) }
    }

    /** Mock IPC strategy that fails a given number of times before succeeding. */
    private class MockIpc(
        private val type: IIpcStrategy.Type,
        private val failTimes: Int = 0,
        private val resultBundle: Bundle = mockResultBundle
    ) : IIpcStrategy {
        var callCount = 0
            private set

        override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle? {
            callCount++
            if (callCount <= failTimes) {
                throw RuntimeException("${type.name} attempt $callCount failed.")
            }
            return resultBundle
        }

        override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String) = true

        override fun getType() = type
    }

    @Test
    fun testGetType_delegatesToWrapped() {
        val wrapped = MockIpc(IIpcStrategy.Type.CONTENT_PROVIDER)
        val strategy = IpcStrategyWithRetry(wrapped, maxRetries = 2, baseDelayMs = 0)
        Assert.assertEquals(IIpcStrategy.Type.CONTENT_PROVIDER, strategy.getType())
    }

    @Test
    fun testIsSupportedByTargetedBroker_delegatesToWrapped() {
        val wrapped = MockIpc(IIpcStrategy.Type.BOUND_SERVICE)
        val strategy = IpcStrategyWithRetry(wrapped, maxRetries = 2, baseDelayMs = 0)
        Assert.assertTrue(strategy.isSupportedByTargetedBroker("com.some.broker"))
    }

    @Test
    fun testSucceedsOnFirstAttempt() {
        val wrapped = MockIpc(IIpcStrategy.Type.CONTENT_PROVIDER)
        val strategy = IpcStrategyWithRetry(wrapped, maxRetries = 3, baseDelayMs = 0)
        val result = strategy.communicateToBroker(mockRequest)
        Assert.assertEquals(mockResultBundle, result)
        Assert.assertEquals(1, wrapped.callCount)
    }

    @Test
    fun testRetriesAndSucceeds() {
        val wrapped = MockIpc(IIpcStrategy.Type.CONTENT_PROVIDER, failTimes = 2)
        val strategy = IpcStrategyWithRetry(wrapped, maxRetries = 3, baseDelayMs = 0)
        val result = strategy.communicateToBroker(mockRequest)
        Assert.assertEquals(mockResultBundle, result)
        Assert.assertEquals(3, wrapped.callCount)
    }

    @Test
    fun testExhaustsRetriesAndThrows() {
        val wrapped = MockIpc(IIpcStrategy.Type.CONTENT_PROVIDER, failTimes = Int.MAX_VALUE)
        val strategy = IpcStrategyWithRetry(wrapped, maxRetries = 2, baseDelayMs = 0)
        try {
            strategy.communicateToBroker(mockRequest)
            Assert.fail("Expected exception was not thrown")
        } catch (t: Throwable) {
            // 1 initial attempt + 2 retries = 3 total calls
            Assert.assertEquals(3, wrapped.callCount)
        }
    }

    @Test
    fun testZeroRetriesDoesNotRetry() {
        val wrapped = MockIpc(IIpcStrategy.Type.BOUND_SERVICE, failTimes = Int.MAX_VALUE)
        val strategy = IpcStrategyWithRetry(wrapped, maxRetries = 0, baseDelayMs = 0)
        try {
            strategy.communicateToBroker(mockRequest)
            Assert.fail("Expected exception was not thrown")
        } catch (t: Throwable) {
            // Only 1 attempt (the initial one, no retries)
            Assert.assertEquals(1, wrapped.callCount)
        }
    }
}
