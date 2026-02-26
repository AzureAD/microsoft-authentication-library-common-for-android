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
import kotlin.random.Random

/**
 * Defines the retry policy for IPC operations with exponential backoff.
 *
 * Only [BrokerCommunicationException] with [BrokerCommunicationException.Category.CONNECTION_ERROR]
 * is eligible for retry. All other exception categories and [com.microsoft.identity.common.java.exception.BaseException]
 * subclasses propagate immediately without retry.
 *
 * All retry behaviour is gated behind [CommonFlight.ENABLE_IPC_RETRY_WITH_EXPONENTIAL_BACKOFF].
 * When the flag is off, zero retries are performed and the original behaviour is preserved.
 */
class IpcRetryPolicy @JvmOverloads constructor(
    val maxRetries: Int = DEFAULT_MAX_RETRIES,
    val baseDelayMs: Long = BASE_DELAY_MS,
    val jitterMs: Long = JITTER_MS
) {
    companion object {
        const val DEFAULT_MAX_RETRIES = 3
        const val BASE_DELAY_MS = 500L
        const val JITTER_MS = 100L
    }

    /**
     * Returns true if the given throwable is eligible for retry.
     *
     * Only [BrokerCommunicationException] with [BrokerCommunicationException.Category.CONNECTION_ERROR]
     * is retryable; other categories and non-[BrokerCommunicationException] throwables are not.
     */
    fun isRetryable(throwable: Throwable): Boolean {
        return throwable is BrokerCommunicationException &&
            throwable.category == BrokerCommunicationException.Category.CONNECTION_ERROR
    }

    /**
     * Returns the backoff delay in milliseconds for the given attempt (0-indexed).
     *
     * Uses exponential backoff: [baseDelayMs] * 2^attempt, with random jitter in [-jitterMs, +jitterMs].
     * The returned value is always >= 0.
     */
    fun getDelayMs(attempt: Int): Long {
        val exponentialDelay = baseDelayMs * (1L shl attempt)
        val jitter = Random.nextLong(-jitterMs, jitterMs + 1)
        return maxOf(0L, exponentialDelay + jitter)
    }

    /**
     * Returns true if IPC retry is enabled via the feature flag.
     */
    fun isEnabled(): Boolean {
        return CommonFlightsManager.getFlightsProvider()
            .isFlightEnabled(CommonFlight.ENABLE_IPC_RETRY_WITH_EXPONENTIAL_BACKOFF)
    }

    /**
     * Sleeps for the given duration, handling [InterruptedException] by re-interrupting the thread.
     */
    fun sleepBeforeRetry(delayMs: Long) {
        try {
            Thread.sleep(delayMs)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}
