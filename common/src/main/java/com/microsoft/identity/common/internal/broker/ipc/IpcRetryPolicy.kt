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
import com.microsoft.identity.common.java.exception.BaseException
import com.microsoft.identity.common.java.logging.Logger

/**
 * Retry policy for transient IPC connection errors in [BrokerOperationExecutor].
 *
 * Retries are performed only on [BrokerCommunicationException] with category
 * [BrokerCommunicationException.Category.CONNECTION_ERROR]. All other exception types
 * (including [BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE]
 * and [BaseException]) propagate immediately without retrying.
 *
 * @param maxRetries      Maximum number of retry attempts (default: [DEFAULT_MAX_RETRIES]).
 * @param baseDelayMs     Base delay in milliseconds before the first retry (default: [DEFAULT_BASE_DELAY_MS]).
 * @param jitterFraction  Fraction of the computed delay to randomise (0.0–1.0; default: [DEFAULT_JITTER_FRACTION]).
 * @param sleepFn         Injectable sleep function (default: [Thread.sleep]) for unit-test overriding.
 */
class IpcRetryPolicy(
    val maxRetries: Int = DEFAULT_MAX_RETRIES,
    val baseDelayMs: Long = DEFAULT_BASE_DELAY_MS,
    val jitterFraction: Double = DEFAULT_JITTER_FRACTION,
    private val sleepFn: (Long) -> Unit = { ms -> Thread.sleep(ms) }
) {
    companion object {
        private val TAG = IpcRetryPolicy::class.simpleName

        /** Default maximum number of retry attempts. */
        const val DEFAULT_MAX_RETRIES = 3

        /** Base delay (ms) before the first retry; doubles with each attempt. */
        const val DEFAULT_BASE_DELAY_MS = 500L

        /** Fraction of the computed back-off delay added as random jitter. */
        const val DEFAULT_JITTER_FRACTION = 0.2
    }

    /**
     * A functional interface that allows implementations to throw [BaseException] (checked).
     * This enables Java callers to use lambda syntax while still propagating checked exceptions.
     */
    @FunctionalInterface
    fun interface IpcOperation<T> {
        @Throws(BaseException::class)
        fun execute(): T
    }

    /**
     * Executes [operation] with exponential back-off retry on transient IPC connection errors.
     *
     * Retries happen only when [BrokerCommunicationException.Category.CONNECTION_ERROR] is thrown.
     * Non-connection errors and [BaseException] subclasses that are not [BrokerCommunicationException]
     * propagate immediately.
     *
     * @param strategyName  Human-readable name of the IPC strategy (used in log messages).
     * @param correlationId Correlation ID of the current request (used in log messages).
     * @param operation     The IPC operation to execute and potentially retry.
     * @return [RetryResult] holding the operation's return value and the number of retries performed.
     * @throws BrokerCommunicationException when [maxRetries] is exhausted for CONNECTION_ERROR.
     * @throws BaseException for any non-retryable exception thrown by [operation].
     */
    @Throws(BaseException::class)
    fun <T> executeWithRetry(
        strategyName: String,
        correlationId: String,
        operation: IpcOperation<T>
    ): RetryResult<T> {
        var lastException: BrokerCommunicationException? = null
        for (attempt in 0..maxRetries) {
            try {
                val value = operation.execute()
                return RetryResult(value, attempt)
            } catch (e: BrokerCommunicationException) {
                if (e.category != BrokerCommunicationException.Category.CONNECTION_ERROR || attempt == maxRetries) {
                    throw e
                }
                lastException = e
                val delayMs = computeDelayMs(attempt)
                Logger.warn(
                    TAG,
                    correlationId,
                    "IPC strategy $strategyName attempt ${attempt + 1}/$maxRetries " +
                        "failed with CONNECTION_ERROR: ${e.message}. Retrying in ${delayMs}ms."
                )
                sleepFn(delayMs)
            }
        }
        // Should be unreachable, but satisfies the compiler.
        throw lastException!!
    }

    /**
     * Computes exponential back-off delay in milliseconds for [attempt] (0-based), adding random jitter.
     *
     * Delays: attempt 0 → ~500 ms, attempt 1 → ~1000 ms, attempt 2 → ~2000 ms (with jitter applied).
     */
    internal fun computeDelayMs(attempt: Int): Long {
        val exponential = baseDelayMs * (1L shl attempt)
        val jitter = (exponential * jitterFraction * Math.random()).toLong()
        return exponential + jitter
    }

    /**
     * Holds the return value of an [executeWithRetry] call together with the number of retries performed.
     *
     * @param value      The result returned by the IPC operation.
     * @param retryCount The number of retries performed (0 means the first attempt succeeded).
     */
    data class RetryResult<T>(val value: T, val retryCount: Int)
}
