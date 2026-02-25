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
import com.microsoft.identity.common.java.logging.Logger
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

/**
 * Retry policy for IPC strategy calls with exponential backoff.
 *
 * Retries are performed only when a [BrokerCommunicationException] with category
 * [BrokerCommunicationException.Category.CONNECTION_ERROR] is thrown.
 * All other exceptions (including [BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE]
 * and non-[BrokerCommunicationException] errors) propagate immediately without retry.
 *
 * Retry behavior is gated by the [CommonFlight.ENABLE_IPC_RETRY_WITH_EXPONENTIAL_BACKOFF] feature flag.
 * When the flag is disabled, the callable is executed exactly once with no retry.
 *
 * @param maxRetries Maximum number of retry attempts after the initial try (default: [DEFAULT_MAX_RETRIES]).
 * @param baseDelayMs Base delay in milliseconds for exponential backoff (default: [DEFAULT_BASE_DELAY_MS]).
 * @param sleepFunction Injectable sleep function to allow unit tests to avoid real delays.
 */
class IpcRetryPolicy @JvmOverloads constructor(
    val maxRetries: Int = DEFAULT_MAX_RETRIES,
    val baseDelayMs: Long = DEFAULT_BASE_DELAY_MS,
    private val sleepFunction: SleepFunction = SleepFunction { millis ->
        try {
            Thread.sleep(millis)
        } catch (e: InterruptedException) {
            // Restore the interrupt flag so callers can detect cancellation.
            Thread.currentThread().interrupt()
        }
    }
) {
    /**
     * Functional interface for injectable sleep, enabling unit testing without real delays.
     */
    fun interface SleepFunction {
        fun sleep(millis: Long)
    }

    /**
     * Functional interface for the IPC operation to be executed (Java-lambda-compatible).
     */
    fun interface IpcCallable<T> {
        @Throws(BrokerCommunicationException::class)
        fun call(): T
    }

    companion object {
        val TAG: String = IpcRetryPolicy::class.simpleName ?: "IpcRetryPolicy"
        const val DEFAULT_MAX_RETRIES = 3
        const val DEFAULT_BASE_DELAY_MS = 500L
    }

    /**
     * Returns true if the [CommonFlight.ENABLE_IPC_RETRY_WITH_EXPONENTIAL_BACKOFF] feature flag is enabled.
     */
    fun isEnabled(): Boolean =
        CommonFlightsManager.getFlightsProvider()
            .isFlightEnabled(CommonFlight.ENABLE_IPC_RETRY_WITH_EXPONENTIAL_BACKOFF)

    /**
     * Returns true if the given [throwable] is a transient IPC connection error that can be retried.
     * Only [BrokerCommunicationException] with [BrokerCommunicationException.Category.CONNECTION_ERROR] is retryable.
     */
    fun isRetryable(throwable: Throwable): Boolean =
        throwable is BrokerCommunicationException &&
            throwable.category == BrokerCommunicationException.Category.CONNECTION_ERROR

    /**
     * Calculates the exponential backoff delay in milliseconds for a given retry attempt, with jitter.
     *
     * Produces delays of approximately 500ms, 1000ms, 2000ms for attempts 0, 1, 2 respectively,
     * plus up to 10% random jitter.
     *
     * @param retryAttempt 0-indexed retry attempt number (0 = delay before the first retry).
     */
    fun calculateDelayMs(retryAttempt: Int): Long {
        val exponentialDelay = baseDelayMs * (1L shl retryAttempt)
        val jitter = (Random.nextDouble() * 0.1 * exponentialDelay).toLong()
        return exponentialDelay + jitter
    }

    /**
     * Executes the given [callable] with automatic retry on transient IPC connection errors.
     *
     * When the feature flag is disabled, the callable is invoked exactly once.
     * When enabled, on [BrokerCommunicationException] with [BrokerCommunicationException.Category.CONNECTION_ERROR],
     * the call is retried up to [maxRetries] times with exponential backoff and jitter.
     *
     * @param strategyName Name of the IPC strategy (used in log messages).
     * @param correlationId Request correlation ID (used in log messages), or null if not available.
     * @param retryCountRef Receives the number of retries that were performed (0 if succeeded first try).
     * @param callable The IPC operation to execute.
     * @return The result of the callable.
     * @throws BrokerCommunicationException if the callable fails after all retries are exhausted,
     *   or immediately if a non-retryable exception is thrown.
     */
    @Throws(BrokerCommunicationException::class)
    fun <T> execute(
        strategyName: String,
        correlationId: String?,
        retryCountRef: AtomicInteger,
        callable: IpcCallable<T>
    ): T {
        val methodTag = "$TAG:execute"
        val effectiveMaxRetries = if (isEnabled()) maxRetries else 0
        var retryCount = 0

        while (true) {
            try {
                val result = callable.call()
                retryCountRef.set(retryCount)
                return result
            } catch (e: BrokerCommunicationException) {
                if (retryCount < effectiveMaxRetries && isRetryable(e)) {
                    val delay = calculateDelayMs(retryCount)
                    Logger.info(
                        methodTag,
                        "[$correlationId] IPC strategy=$strategyName attempt=${retryCount + 1} " +
                            "failed with ${e.category}. Retrying in ${delay}ms."
                    )
                    sleepFunction.sleep(delay)
                    retryCount++
                } else {
                    retryCountRef.set(retryCount)
                    throw e
                }
            }
        }
    }
}
