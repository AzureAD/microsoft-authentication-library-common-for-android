// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.internal.broker.ipc

import android.os.Bundle
import com.microsoft.identity.common.exception.BrokerCommunicationException
import com.microsoft.identity.common.logging.Logger
import kotlin.random.Random

/**
 * A decorator for [IIpcStrategy] that retries transient failures with exponential backoff.
 *
 * Retries are only attempted for [BrokerCommunicationException.Category.CONNECTION_ERROR]
 * and [BrokerCommunicationException.Category.NULL_CURSOR]. All other categories are
 * rethrown immediately without retry.
 *
 * @param inner          The underlying [IIpcStrategy] to delegate to.
 * @param maxRetries     Maximum number of retry attempts after the initial attempt.
 * @param initialDelayMs Delay in milliseconds before the first retry.
 * @param backoffFactor  Multiplier applied to the delay after each retry.
 * @param maxDelayMs     Upper bound on the computed delay in milliseconds.
 * @param jitterFactor   Fraction of the delay added as random jitter (0.0–1.0).
 */
class IpcStrategyWithRetry(
    private val inner: IIpcStrategy,
    private val maxRetries: Int = 2,
    private val initialDelayMs: Long = 500L,
    private val backoffFactor: Double = 2.0,
    private val maxDelayMs: Long = 4000L,
    private val jitterFactor: Double = 0.1
) : IIpcStrategy {

    companion object {
        private val TAG = IpcStrategyWithRetry::class.java.simpleName
    }

    override fun getType(): IIpcStrategy.Type = inner.getType()

    override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String): Boolean =
        inner.isSupportedByTargetedBroker(targetedBrokerPackageName)

    @Throws(BrokerCommunicationException::class)
    override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle? {
        val methodTag = "$TAG:communicateToBroker"

        for (attempt in 0..maxRetries) {
            if (attempt > 0) {
                val delayMs = computeDelay(attempt)
                Logger.info(
                    methodTag,
                    "Retrying IPC strategy ${inner.getType()} attempt $attempt of $maxRetries " +
                        "after ${delayMs}ms delay."
                )
                Thread.sleep(delayMs)
            }

            try {
                return inner.communicateToBroker(bundle)
            } catch (e: BrokerCommunicationException) {
                if (isRetryable(e) && attempt < maxRetries) {
                    Logger.warn(
                        methodTag,
                        "Retryable IPC failure on strategy ${inner.getType()} " +
                            "(attempt $attempt, category=${e.category}): ${e.message}"
                    )
                } else {
                    if (attempt >= maxRetries && isRetryable(e)) {
                        Logger.error(
                            methodTag,
                            "IPC strategy ${inner.getType()} failed after $maxRetries retries " +
                                "(category=${e.category}).",
                            e
                        )
                    }
                    throw e
                }
            }
        }

        // Unreachable: the loop always returns or throws on the final attempt.
        throw BrokerCommunicationException(
            BrokerCommunicationException.Category.CONNECTION_ERROR,
            inner.getType(),
            "Retry loop exited unexpectedly.",
            null
        )
    }

    private fun isRetryable(e: BrokerCommunicationException): Boolean =
        e.category == BrokerCommunicationException.Category.CONNECTION_ERROR ||
            e.category == BrokerCommunicationException.Category.NULL_CURSOR

    private fun computeDelay(attempt: Int): Long {
        val base = (initialDelayMs * Math.pow(backoffFactor, (attempt - 1).toDouble()))
            .coerceAtMost(maxDelayMs.toDouble()).toLong()
        val jitter = (base * jitterFactor * Random.nextDouble()).toLong()
        return base + jitter
    }
}
