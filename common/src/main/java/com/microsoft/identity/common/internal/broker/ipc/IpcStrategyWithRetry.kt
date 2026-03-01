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
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.opentelemetry.SpanExtension
import kotlin.math.min
import kotlin.random.Random

/**
 * An [IIpcStrategy] decorator that retries the wrapped strategy with exponential backoff
 * and jitter on transient failures.
 *
 * Only [BrokerCommunicationException] with category [BrokerCommunicationException.Category.CONNECTION_ERROR]
 * or [BrokerCommunicationException.Category.NULL_CURSOR] are considered retryable.
 * All other exceptions are rethrown immediately without retrying.
 *
 * **Threading**: [communicateToBroker] must be called from a background (worker) thread, as it
 * may call [Thread.sleep] between retry attempts.
 *
 * @param inner          The underlying [IIpcStrategy] to decorate.
 * @param maxRetries     Maximum number of retry attempts (not counting the initial attempt).
 * @param initialDelayMs Delay before the first retry, in milliseconds.
 * @param backoffFactor  Multiplicative factor applied to the delay after each retry.
 * @param maxDelayMs     Upper bound on the computed delay between retries, in milliseconds.
 * @param jitterFactor   Fraction of the current delay added/subtracted as random jitter.
 */
class IpcStrategyWithRetry(
    private val inner: IIpcStrategy,
    private val maxRetries: Int = DEFAULT_MAX_RETRIES,
    private val initialDelayMs: Long = DEFAULT_INITIAL_DELAY_MS,
    private val backoffFactor: Double = DEFAULT_BACKOFF_FACTOR,
    private val maxDelayMs: Long = DEFAULT_MAX_DELAY_MS,
    private val jitterFactor: Double = DEFAULT_JITTER_FACTOR
) : IIpcStrategy {

    companion object {
        private val TAG = IpcStrategyWithRetry::class.simpleName

        /** Default maximum number of retry attempts. */
        const val DEFAULT_MAX_RETRIES: Int = 2

        /** Default initial delay in milliseconds before the first retry. */
        const val DEFAULT_INITIAL_DELAY_MS: Long = 500L

        /** Default exponential backoff factor applied after each retry. */
        const val DEFAULT_BACKOFF_FACTOR: Double = 2.0

        /** Default maximum delay cap in milliseconds between retries. */
        const val DEFAULT_MAX_DELAY_MS: Long = 4000L

        /** Default jitter fraction (+/- this fraction of the current delay). */
        const val DEFAULT_JITTER_FACTOR: Double = 0.1

        private val RETRYABLE_CATEGORIES = setOf(
            BrokerCommunicationException.Category.CONNECTION_ERROR,
            BrokerCommunicationException.Category.NULL_CURSOR
        )
    }

    /**
     * Communicates with the broker via the wrapped [inner] strategy, retrying up to [maxRetries]
     * times on retryable [BrokerCommunicationException] categories using exponential backoff with jitter.
     *
     * @param bundle The [BrokerOperationBundle] describing the operation.
     * @return The response [Bundle] from the broker, or `null` if the broker returns no data.
     * @throws BrokerCommunicationException if all retry attempts are exhausted or the failure is non-retryable.
     */
    override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle? {
        val methodTag = "$TAG:communicateToBroker"
        var currentDelay = initialDelayMs
        var lastException: BrokerCommunicationException? = null

        for (attempt in 0..maxRetries) {
            if (attempt > 0) {
                val jitter = (Random.nextDouble() * 2 - 1) * jitterFactor * currentDelay
                val sleepMs = (currentDelay + jitter).toLong().coerceAtLeast(0L)
                Logger.info(
                    methodTag,
                    "Retrying IPC (attempt $attempt/$maxRetries) after ${sleepMs}ms. " +
                        "Strategy: ${inner.getType().name}. " +
                        "Last error: ${lastException?.message}"
                )
                Thread.sleep(sleepMs)
                currentDelay = min((currentDelay * backoffFactor).toLong(), maxDelayMs)
            }

            try {
                val result = inner.communicateToBroker(bundle)
                SpanExtension.current().setAttribute(
                    AttributeName.ipc_retry_total_count.name,
                    attempt.toLong()
                )
                return result
            } catch (e: BrokerCommunicationException) {
                if (e.category in RETRYABLE_CATEGORIES && attempt < maxRetries) {
                    Logger.warn(
                        methodTag,
                        "Retryable IPC failure (attempt $attempt/$maxRetries): ${e.message}"
                    )
                    lastException = e
                } else {
                    SpanExtension.current().setAttribute(
                        AttributeName.ipc_retry_total_count.name,
                        attempt.toLong()
                    )
                    if (attempt >= maxRetries) {
                        Logger.error(
                            methodTag,
                            "IPC retry exhausted after $attempt attempt(s). " +
                                "Strategy: ${inner.getType().name}.",
                            e
                        )
                    }
                    throw e
                }
            }
        }

        // Should not be reachable: lastException is set on every iteration that catches a retryable
        // exception, and the loop always exits either via return (success) or throw (non-retryable).
        SpanExtension.current().setAttribute(
            AttributeName.ipc_retry_total_count.name,
            maxRetries.toLong()
        )
        throw checkNotNull(lastException) { "Retry logic error: loop exited without result or exception" }
    }

    /**
     * Delegates to the wrapped [inner] strategy.
     */
    override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String): Boolean {
        return inner.isSupportedByTargetedBroker(targetedBrokerPackageName)
    }

    /**
     * Delegates to the wrapped [inner] strategy.
     */
    override fun getType(): IIpcStrategy.Type {
        return inner.getType()
    }
}
