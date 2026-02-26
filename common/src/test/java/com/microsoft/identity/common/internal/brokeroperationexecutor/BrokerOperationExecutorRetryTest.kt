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
package com.microsoft.identity.common.internal.brokeroperationexecutor

import android.os.Build
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory
import com.microsoft.identity.common.exception.BrokerCommunicationException
import com.microsoft.identity.common.internal.activebrokerdiscovery.InMemoryActiveBrokerCache
import com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle
import com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy
import com.microsoft.identity.common.internal.broker.ipc.IpcRetryPolicy
import com.microsoft.identity.common.internal.cache.ActiveBrokerCacheUpdater
import com.microsoft.identity.common.internal.controllers.BrokerOperationExecutor
import com.microsoft.identity.common.internal.mocks.MockCommonFlightsManager
import com.microsoft.identity.common.internal.telemetry.events.ApiEndEvent
import com.microsoft.identity.common.java.commands.parameters.CommandParameters
import com.microsoft.identity.common.java.exception.BaseException
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.exception.ErrorStrings
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import com.microsoft.identity.common.java.flighting.IFlightsProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.N])
class BrokerOperationExecutorRetryTest {

    companion object {
        private const val SUCCESS_KEY = "success_key"
        private val MOCK_TYPE = IIpcStrategy.Type.BOUND_SERVICE
    }

    private lateinit var cacheUpdater: ActiveBrokerCacheUpdater

    @Before
    fun setUp() {
        cacheUpdater = ActiveBrokerCacheUpdater({ true }, InMemoryActiveBrokerCache())
    }

    @After
    fun tearDown() {
        CommonFlightsManager.resetFlightsManager()
    }

    /** Enables the retry flight flag via a mock flights provider. */
    private fun enableRetryFlight() {
        val mockProvider = Mockito.mock(IFlightsProvider::class.java)
        Mockito.`when`(mockProvider.isFlightEnabled(CommonFlight.ENABLE_IPC_RETRY_WITH_EXPONENTIAL_BACKOFF))
            .thenReturn(true)
        val mockManager = MockCommonFlightsManager()
        mockManager.mockCommonFlightsProvider = mockProvider
        CommonFlightsManager.initializeCommonFlightsManager(mockManager)
    }

    /**
     * Returns a fast IpcRetryPolicy (0ms delay, no jitter) backed by the current flight flag state.
     * Using 0ms delays avoids slowing down unit tests.
     */
    private fun fastRetryPolicy() = IpcRetryPolicy(maxRetries = 3, baseDelayMs = 0L, jitterMs = 0L)

    private fun makeExecutor(
        strategy: IIpcStrategy,
        retryPolicy: IpcRetryPolicy = IpcRetryPolicy()
    ): BrokerOperationExecutor {
        return BrokerOperationExecutor(listOf(strategy), cacheUpdater, retryPolicy)
    }

    private fun makeParams(): CommandParameters {
        return CommandParameters.builder()
            .platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext()))
            .build()
    }

    /** A simple broker operation that expects a bundle with SUCCESS_KEY=true. */
    private fun makeOperation(): BrokerOperationExecutor.BrokerOperation<Boolean> {
        return object : BrokerOperationExecutor.BrokerOperation<Boolean> {
            override fun performPrerequisites(strategy: IIpcStrategy) {}

            override fun getBundle(): BrokerOperationBundle =
                BrokerOperationBundle(BrokerOperationBundle.Operation.BROKER_API_HELLO, "MOCK", Bundle())

            override fun extractResultBundle(resultBundle: Bundle?): Boolean {
                if (resultBundle != null && resultBundle.containsKey(SUCCESS_KEY))
                    return resultBundle.getBoolean(SUCCESS_KEY)
                throw ClientException("unexpected_bundle")
            }

            override fun getMethodName(): String = "testMethod"

            override fun getTelemetryApiId(): String? = null

            override fun putValueInSuccessEvent(event: ApiEndEvent, result: Boolean) {}
        }
    }

    // region flag-off tests (original behaviour preserved)

    @Test
    fun retryFlagOff_successFirstAttempt_succeeds() {
        val result = makeExecutor(strategySuccessOnFirstAttempt()).execute(makeParams(), makeOperation())
        assertTrue(result)
    }

    @Test
    fun retryFlagOff_connectionError_doesNotRetry() {
        // Flag is off (default). A strategy that always fails should be called exactly once.
        val callCount = AtomicInteger()
        val strategy = object : IIpcStrategy {
            override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle {
                callCount.incrementAndGet()
                throw BrokerCommunicationException(
                    BrokerCommunicationException.Category.CONNECTION_ERROR,
                    MOCK_TYPE, "error", null
                )
            }
            override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String) = true
            override fun getType() = MOCK_TYPE
        }

        try {
            makeExecutor(strategy).execute(makeParams(), makeOperation())
            fail("Expected exception")
        } catch (e: BaseException) {
            assertTrue(e is ClientException)
            assertEquals(ErrorStrings.BROKER_BIND_SERVICE_FAILED, e.errorCode)
        }
        // Only 1 call – no retries when flag is off
        assertEquals(1, callCount.get())
    }

    // endregion

    // region flag-on tests

    @Test
    fun retryFlagOn_successFirstAttempt_succeeds() {
        enableRetryFlight()
        val result = makeExecutor(strategySuccessOnFirstAttempt(), fastRetryPolicy())
            .execute(makeParams(), makeOperation())
        assertTrue(result)
    }

    @Test
    fun retryFlagOn_failOnceThenSucceed_retriesAndSucceeds() {
        enableRetryFlight()
        val callCount = AtomicInteger()
        val strategy = strategyFailThenSucceed(failCount = 1, callCounter = callCount)
        val result = makeExecutor(strategy, fastRetryPolicy()).execute(makeParams(), makeOperation())
        assertTrue(result)
        assertEquals(2, callCount.get()) // 1 failure + 1 success
    }

    @Test
    fun retryFlagOn_failTwiceThenSucceed_retriesAndSucceeds() {
        enableRetryFlight()
        val callCount = AtomicInteger()
        val strategy = strategyFailThenSucceed(failCount = 2, callCounter = callCount)
        val result = makeExecutor(strategy, fastRetryPolicy()).execute(makeParams(), makeOperation())
        assertTrue(result)
        assertEquals(3, callCount.get()) // 2 failures + 1 success
    }

    @Test
    fun retryFlagOn_alwaysFails_exhaustsMaxRetriesAndThrows() {
        enableRetryFlight()
        val callCount = AtomicInteger()
        val strategy = object : IIpcStrategy {
            override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle {
                callCount.incrementAndGet()
                throw BrokerCommunicationException(
                    BrokerCommunicationException.Category.CONNECTION_ERROR,
                    MOCK_TYPE, "persistent error", null
                )
            }
            override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String) = true
            override fun getType() = MOCK_TYPE
        }

        try {
            makeExecutor(strategy, fastRetryPolicy()).execute(makeParams(), makeOperation())
            fail("Expected exception")
        } catch (e: BaseException) {
            assertTrue(e is ClientException)
            assertEquals(ErrorStrings.BROKER_BIND_SERVICE_FAILED, e.errorCode)
        }
        // 1 initial attempt + 3 retries = 4 total calls
        assertEquals(4, callCount.get())
    }

    @Test
    fun retryFlagOn_operationNotSupportedOnServer_doesNotRetry() {
        enableRetryFlight()
        val callCount = AtomicInteger()
        val strategy = object : IIpcStrategy {
            override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle {
                callCount.incrementAndGet()
                throw BrokerCommunicationException(
                    BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE,
                    MOCK_TYPE, "not supported", null
                )
            }
            override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String) = true
            override fun getType() = MOCK_TYPE
        }

        try {
            makeExecutor(strategy, fastRetryPolicy()).execute(makeParams(), makeOperation())
            fail("Expected exception")
        } catch (e: BaseException) {
            assertTrue(e is ClientException)
            assertEquals(ErrorStrings.BROKER_BIND_SERVICE_FAILED, e.errorCode)
        }
        // Only 1 call – non-retryable exception
        assertEquals(1, callCount.get())
    }

    @Test
    fun retryFlagOn_validationError_doesNotRetry() {
        enableRetryFlight()
        val callCount = AtomicInteger()
        val strategy = object : IIpcStrategy {
            override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle {
                callCount.incrementAndGet()
                throw BrokerCommunicationException(
                    BrokerCommunicationException.Category.VALIDATION_ERROR,
                    MOCK_TYPE, "validation error", null
                )
            }
            override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String) = true
            override fun getType() = MOCK_TYPE
        }

        try {
            makeExecutor(strategy, fastRetryPolicy()).execute(makeParams(), makeOperation())
            fail("Expected exception")
        } catch (e: BaseException) {
            assertTrue(e is ClientException)
        }
        assertEquals(1, callCount.get())
    }

    @Test
    fun retryFlagOn_nullCursor_doesNotRetry() {
        enableRetryFlight()
        val callCount = AtomicInteger()
        val strategy = object : IIpcStrategy {
            override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle {
                callCount.incrementAndGet()
                throw BrokerCommunicationException(
                    BrokerCommunicationException.Category.NULL_CURSOR,
                    MOCK_TYPE, "null cursor", null
                )
            }
            override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String) = true
            override fun getType() = MOCK_TYPE
        }

        try {
            makeExecutor(strategy, fastRetryPolicy()).execute(makeParams(), makeOperation())
            fail("Expected exception")
        } catch (e: BaseException) {
            assertTrue(e is ClientException)
        }
        assertEquals(1, callCount.get())
    }

    @Test
    fun retryFlagOn_prerequisitesReExecutedOnEachRetry() {
        enableRetryFlight()
        val prerequisiteCount = AtomicInteger()
        val ipcCallCount = AtomicInteger()

        val operation = object : BrokerOperationExecutor.BrokerOperation<Boolean> {
            override fun performPrerequisites(strategy: IIpcStrategy) {
                prerequisiteCount.incrementAndGet()
            }
            override fun getBundle() = BrokerOperationBundle(
                BrokerOperationBundle.Operation.BROKER_API_HELLO, "MOCK", Bundle()
            )
            override fun extractResultBundle(resultBundle: Bundle?): Boolean {
                if (resultBundle != null && resultBundle.containsKey(SUCCESS_KEY))
                    return resultBundle.getBoolean(SUCCESS_KEY)
                throw ClientException("unexpected_bundle")
            }
            override fun getMethodName() = "testPrerequisites"
            override fun getTelemetryApiId(): String? = null
            override fun putValueInSuccessEvent(event: ApiEndEvent, result: Boolean) {}
        }

        // Fail twice, then succeed on 3rd attempt
        val strategy = strategyFailThenSucceed(failCount = 2, callCounter = ipcCallCount)

        val result = makeExecutor(strategy, fastRetryPolicy()).execute(makeParams(), operation)
        assertTrue(result)
        // performPrerequisites should be called once per attempt: initial + 2 retries = 3
        assertEquals(3, prerequisiteCount.get())
    }

    // endregion

    // region helpers

    private fun strategySuccessOnFirstAttempt(): IIpcStrategy {
        return object : IIpcStrategy {
            override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle =
                Bundle().apply { putBoolean(SUCCESS_KEY, true) }
            override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String) = true
            override fun getType() = MOCK_TYPE
        }
    }

    private fun strategyFailThenSucceed(
        failCount: Int,
        callCounter: AtomicInteger = AtomicInteger()
    ): IIpcStrategy {
        return object : IIpcStrategy {
            override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle {
                if (callCounter.incrementAndGet() <= failCount) {
                    throw BrokerCommunicationException(
                        BrokerCommunicationException.Category.CONNECTION_ERROR,
                        MOCK_TYPE, "transient error", null
                    )
                }
                return Bundle().apply { putBoolean(SUCCESS_KEY, true) }
            }
            override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String) = true
            override fun getType() = MOCK_TYPE
        }
    }

    // endregion
}
