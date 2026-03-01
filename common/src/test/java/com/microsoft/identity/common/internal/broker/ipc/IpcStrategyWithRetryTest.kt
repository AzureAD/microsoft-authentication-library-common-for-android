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
import com.microsoft.identity.common.exception.BrokerCommunicationException
import com.microsoft.identity.common.exception.BrokerCommunicationException.Category
import com.microsoft.identity.common.internal.broker.BrokerData
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IpcStrategyWithRetryTest {

    companion object {
        val mockRequest = BrokerOperationBundle(
            BrokerOperationBundle.Operation.BROKER_DISCOVERY_METADATA_RETRIEVAL,
            BrokerData.prodMicrosoftAuthenticator.packageName,
            Bundle().apply { putBoolean("REQUEST", true) }
        )

        val mockResultBundle = Bundle().apply { putBoolean("RESULT", true) }
    }

    /**
     * A configurable mock [IIpcStrategy] that throws [BrokerCommunicationException] for the
     * first [failCount] calls, then returns [resultBundle] on subsequent calls.
     */
    private class MockIpc(
        private val type: IIpcStrategy.Type = IIpcStrategy.Type.CONTENT_PROVIDER,
        private val failCount: Int = 0,
        private val failCategory: Category = Category.CONNECTION_ERROR,
        private val resultBundle: Bundle = mockResultBundle
    ) : IIpcStrategy {
        var callCount = 0
        val loggedMessages = mutableListOf<String>()

        override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle? {
            callCount++
            if (callCount <= failCount) {
                throw BrokerCommunicationException(failCategory, type, "Simulated failure #$callCount", null)
            }
            return resultBundle
        }

        override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String): Boolean = true

        override fun getType(): IIpcStrategy.Type = type
    }

    // Success on first attempt – no retry, no telemetry attributes set.
    @Test
    fun testSuccessOnFirstAttempt_noRetry() {
        val mockIpc = MockIpc(failCount = 0)
        val strategy = IpcStrategyWithRetry(delegate = mockIpc, maxRetries = 3, baseDelayMs = 0L)

        val result = strategy.communicateToBroker(mockRequest)

        Assert.assertEquals(mockResultBundle, result)
        Assert.assertEquals(1, mockIpc.callCount)
    }

    // Success after 1 retry – delegate throws CONNECTION_ERROR first, succeeds second.
    @Test
    fun testSuccessAfter1Retry_connectionError() {
        val mockIpc = MockIpc(failCount = 1, failCategory = Category.CONNECTION_ERROR)
        val strategy = IpcStrategyWithRetry(delegate = mockIpc, maxRetries = 3, baseDelayMs = 0L)

        val result = strategy.communicateToBroker(mockRequest)

        Assert.assertEquals(mockResultBundle, result)
        Assert.assertEquals(2, mockIpc.callCount)
    }

    // Success after 2 retries – delegate throws NULL_CURSOR twice, succeeds third time.
    @Test
    fun testSuccessAfter2Retries_nullCursor() {
        val mockIpc = MockIpc(failCount = 2, failCategory = Category.NULL_CURSOR)
        val strategy = IpcStrategyWithRetry(delegate = mockIpc, maxRetries = 3, baseDelayMs = 0L)

        val result = strategy.communicateToBroker(mockRequest)

        Assert.assertEquals(mockResultBundle, result)
        Assert.assertEquals(3, mockIpc.callCount)
    }

    // Max retries exhausted – delegate always throws CONNECTION_ERROR, verify exception rethrown.
    @Test
    fun testMaxRetriesExhausted_exceptionRethrown() {
        val mockIpc = MockIpc(failCount = 4, failCategory = Category.CONNECTION_ERROR)
        val strategy = IpcStrategyWithRetry(delegate = mockIpc, maxRetries = 3, baseDelayMs = 0L)

        try {
            strategy.communicateToBroker(mockRequest)
            Assert.fail("Expected BrokerCommunicationException to be thrown")
        } catch (e: BrokerCommunicationException) {
            Assert.assertEquals(Category.CONNECTION_ERROR, e.category)
            // Should have tried 4 times total (attempt 0 + 3 retries).
            Assert.assertEquals(4, mockIpc.callCount)
        }
    }

    // Non-retryable error – OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE should not trigger any retry.
    @Test
    fun testNonRetryableError_operationNotSupportedOnServerSide() {
        val mockIpc = MockIpc(failCount = 4, failCategory = Category.OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE)
        val strategy = IpcStrategyWithRetry(delegate = mockIpc, maxRetries = 3, baseDelayMs = 0L)

        try {
            strategy.communicateToBroker(mockRequest)
            Assert.fail("Expected BrokerCommunicationException to be thrown")
        } catch (e: BrokerCommunicationException) {
            Assert.assertEquals(Category.OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE, e.category)
            // Should have tried only once – no retries for non-retryable errors.
            Assert.assertEquals(1, mockIpc.callCount)
        }
    }

    // Non-retryable VALIDATION_ERROR – no retry.
    @Test
    fun testNonRetryableError_validationError() {
        val mockIpc = MockIpc(failCount = 4, failCategory = Category.VALIDATION_ERROR)
        val strategy = IpcStrategyWithRetry(delegate = mockIpc, maxRetries = 3, baseDelayMs = 0L)

        try {
            strategy.communicateToBroker(mockRequest)
            Assert.fail("Expected BrokerCommunicationException to be thrown")
        } catch (e: BrokerCommunicationException) {
            Assert.assertEquals(Category.VALIDATION_ERROR, e.category)
            // Should have tried only once – no retries for VALIDATION_ERROR.
            Assert.assertEquals(1, mockIpc.callCount)
        }
    }

    // Verify getType() delegates to wrapped strategy.
    @Test
    fun testGetType_delegatesToWrappedStrategy() {
        val mockIpc = MockIpc(type = IIpcStrategy.Type.BOUND_SERVICE)
        val strategy = IpcStrategyWithRetry(delegate = mockIpc)

        Assert.assertEquals(IIpcStrategy.Type.BOUND_SERVICE, strategy.getType())
    }

    // Verify isSupportedByTargetedBroker() delegates to wrapped strategy.
    @Test
    fun testIsSupportedByTargetedBroker_delegatesToWrappedStrategy() {
        val mockIpc = MockIpc()
        val strategy = IpcStrategyWithRetry(delegate = mockIpc)

        Assert.assertTrue(strategy.isSupportedByTargetedBroker("com.azure.authenticator"))
    }
}
