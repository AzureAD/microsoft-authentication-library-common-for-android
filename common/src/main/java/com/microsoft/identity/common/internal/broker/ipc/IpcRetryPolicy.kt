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
import kotlin.random.Random

/**
 * Retry policy for IPC strategy calls in [com.microsoft.identity.common.internal.controllers.BrokerOperationExecutor].
 *
 * Retries are performed only on [BrokerCommunicationException.Category.CONNECTION_ERROR].
 * Other exception categories (e.g. OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE) and non-IPC
 * exceptions propagate immediately without retry.
 *
 * Uses exponential backoff with random jitter:
 * - Attempt 1 delay: ~500ms
 * - Attempt 2 delay: ~1000ms
 * - Attempt 3 delay: ~2000ms
 *
 * @param maxRetries Maximum number of retry attempts. Defaults to [DEFAULT_MAX_RETRIES].
 * @param baseDelayMs Base delay in milliseconds for the first retry. Defaults to [DEFAULT_BASE_DELAY_MS].
 */
class IpcRetryPolicy(
    val maxRetries: Int = DEFAULT_MAX_RETRIES,
    val baseDelayMs: Long = DEFAULT_BASE_DELAY_MS
) {
    companion object {
        val TAG: String? = IpcRetryPolicy::class.simpleName

        /** Default maximum number of retry attempts. */
        const val DEFAULT_MAX_RETRIES = 3

        /** Default base delay in milliseconds (500ms → 1000ms → 2000ms with exponential backoff). */
        const val DEFAULT_BASE_DELAY_MS = 500L
    }

    /**
     * Returns true if the given [BrokerCommunicationException] should trigger a retry.
     * Only [BrokerCommunicationException.Category.CONNECTION_ERROR] is retryable.
     */
    fun shouldRetry(exception: BrokerCommunicationException): Boolean {
        return exception.category == BrokerCommunicationException.Category.CONNECTION_ERROR
    }

    /**
     * Calculates the delay in milliseconds for the given retry attempt.
     *
     * Uses exponential backoff (baseDelay * 2^attempt) plus random jitter of up to 10%
     * of the exponential delay to reduce thundering-herd effects.
     *
     * Note: {@link Random} is used deliberately (not {@link java.security.SecureRandom}) since
     * this jitter is for back-off timing only and has no security implications.
     *
     * @param attempt 0-indexed retry attempt number (0 = first retry, 1 = second retry, …)
     * @return delay in milliseconds
     */
    fun getDelayMs(attempt: Int): Long {
        val exponentialDelay = baseDelayMs * (1L shl attempt)
        val jitter = (Random.nextDouble() * exponentialDelay * 0.1).toLong()
        return exponentialDelay + jitter
    }
}
