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
import java.util.concurrent.ThreadLocalRandom

/**
 * Retry policy for IPC strategy calls with exponential back-off.
 *
 * Retries are gated by [CommonFlight.ENABLE_IPC_RETRY_WITH_EXPONENTIAL_BACKOFF].
 * Only [BrokerCommunicationException.Category.CONNECTION_ERROR] is retryable;
 * all other error categories and non-[BrokerCommunicationException] throwables
 * propagate immediately without retrying.
 *
 * Delay values describe pauses that must be observed on a background/worker thread;
 * this policy must never be used on the main thread.
 *
 * @param sleepFunction injectable sleep implementation for test isolation; defaults to [Thread.sleep].
 */
class IpcRetryPolicy @JvmOverloads constructor(
    private val sleepFunction: (Long) -> Unit = { Thread.sleep(it) }
) {
    companion object {
        private const val MAX_RETRIES = 3
        private const val BASE_DELAY_MS = 500L
        private const val JITTER_MS = 100L
    }

    /**
     * Returns true if IPC retry with exponential back-off is enabled via
     * [CommonFlight.ENABLE_IPC_RETRY_WITH_EXPONENTIAL_BACKOFF].
     */
    fun isEnabled(): Boolean =
        CommonFlightsManager.getFlightsProvider()
            .getBooleanValue(CommonFlight.ENABLE_IPC_RETRY_WITH_EXPONENTIAL_BACKOFF)

    /**
     * Returns true if [throwable] represents a transient IPC failure that warrants a retry.
     * Only [BrokerCommunicationException.Category.CONNECTION_ERROR] is retryable.
     */
    fun isRetryable(throwable: Throwable): Boolean =
        throwable is BrokerCommunicationException &&
            throwable.category == BrokerCommunicationException.Category.CONNECTION_ERROR

    /**
     * Returns the maximum number of retry attempts (attempts after the initial call).
     */
    fun getMaxRetries(): Int = MAX_RETRIES

    /**
     * Returns the delay in milliseconds to wait before retry [attempt] (0-based).
     *
     * Delays follow exponential back-off with ±[JITTER_MS] ms jitter:
     * - attempt 0 → ~500 ms
     * - attempt 1 → ~1 000 ms
     * - attempt 2 → ~2 000 ms
     *
     * @param attempt 0-based retry attempt index.
     * @return non-negative delay in milliseconds.
     */
    fun getDelayMs(attempt: Int): Long {
        val exponentialDelay = BASE_DELAY_MS shl attempt  // 500, 1000, 2000
        val jitter = ThreadLocalRandom.current().nextLong(-JITTER_MS, JITTER_MS + 1)
        return maxOf(0L, exponentialDelay + jitter)
    }

    /**
     * Sleeps the calling thread for [delayMs] milliseconds.
     * If the thread is interrupted, the interrupt flag is restored and the method returns.
     *
     * @param delayMs duration to sleep in milliseconds.
     */
    fun sleepBeforeRetry(delayMs: Long) {
        try {
            sleepFunction(delayMs)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}
