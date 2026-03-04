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
import com.microsoft.identity.common.java.logging.Logger
import kotlin.random.Random

/**
 * Policy for retrying IPC strategy calls with exponential backoff.
 *
 * Retries are performed only on [BrokerCommunicationException] with category
 * [BrokerCommunicationException.Category.CONNECTION_ERROR]. All other exceptions
 * (including [BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE]
 * and any [com.microsoft.identity.common.java.exception.BaseException]) propagate immediately.
 *
 * @param maxRetries      Maximum number of retry attempts (default: 3).
 * @param baseDelayMs     Base delay in milliseconds before the first retry (default: 500 ms).
 * @param jitterRangeMs   Maximum additional jitter in milliseconds added to each delay (default: 100 ms).
 */
class IpcRetryPolicy(
    val maxRetries: Int = DEFAULT_MAX_RETRIES,
    val baseDelayMs: Long = DEFAULT_BASE_DELAY_MS,
    val jitterRangeMs: Long = DEFAULT_JITTER_RANGE_MS
) {
    companion object {
        private val TAG = IpcRetryPolicy::class.simpleName
        const val DEFAULT_MAX_RETRIES = 3
        const val DEFAULT_BASE_DELAY_MS = 500L
        const val DEFAULT_JITTER_RANGE_MS = 100L

        /**
         * Returns true if the given [BrokerCommunicationException] is retryable.
         * Only [BrokerCommunicationException.Category.CONNECTION_ERROR] is retryable.
         */
        @JvmStatic
        fun isRetryable(exception: BrokerCommunicationException): Boolean {
            return exception.category == BrokerCommunicationException.Category.CONNECTION_ERROR
        }
    }

    /**
     * Executes [block] with retry on transient [BrokerCommunicationException.Category.CONNECTION_ERROR].
     *
     * @param strategyName  Name of the IPC strategy, used for logging.
     * @param correlationId Correlation ID of the current request, used for logging.
     * @param block         The operation to execute (and retry on failure).
     * @return A pair of (result, retry count) where retry count is the number of retries performed.
     * @throws BrokerCommunicationException if all retries are exhausted or the exception is non-retryable.
     */
    fun <T> executeWithRetry(
        strategyName: String,
        correlationId: String?,
        block: () -> T
    ): Pair<T, Int> {
        val methodTag = "$TAG:executeWithRetry"
        var attempt = 0
        while (true) {
            try {
                val result = block()
                return Pair(result, attempt)
            } catch (e: BrokerCommunicationException) {
                if (!isRetryable(e) || attempt >= maxRetries) {
                    throw e
                }
                val delay = computeBackoffDelayMs(attempt)
                attempt++
                Logger.warn(
                    methodTag,
                    "[$correlationId] IPC strategy [$strategyName] failed with CONNECTION_ERROR " +
                        "(attempt $attempt/$maxRetries). Retrying in ${delay}ms. Cause: ${e.message}"
                )
                Thread.sleep(delay)
            }
        }
    }

    /**
     * Computes the exponential backoff delay for a given attempt index (0-based).
     * delay = baseDelayMs * 2^attempt + jitter
     */
    fun computeBackoffDelayMs(attempt: Int): Long {
        val exponential = baseDelayMs * (1L shl attempt)
        val jitter = if (jitterRangeMs > 0) Random.nextLong(jitterRangeMs) else 0L
        return exponential + jitter
    }
}
