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
import com.microsoft.identity.common.exception.BrokerCommunicationException.Category.CONNECTION_ERROR
import com.microsoft.identity.common.exception.BrokerCommunicationException.Category.NULL_CURSOR
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.opentelemetry.SpanExtension

/**
 * An IPC Strategy decorator that wraps a delegate [IIpcStrategy] and adds automatic retry
 * with exponential back-off for transient IPC failures.
 *
 * Retries are attempted only for [CONNECTION_ERROR] and [NULL_CURSOR] categories.
 * Other error categories are considered non-retryable and are rethrown immediately.
 *
 * @param delegate   The underlying [IIpcStrategy] to invoke.
 * @param maxRetries Maximum number of retry attempts (default 3).
 * @param baseDelayMs Base delay in milliseconds for the exponential back-off (default 500ms).
 *                    Actual delay for attempt N is {@code baseDelayMs * 2^N}.
 */
class IpcStrategyWithRetry(
    private val delegate: IIpcStrategy,
    private val maxRetries: Int = 3,
    private val baseDelayMs: Long = 500L
) : IIpcStrategy {

    companion object {
        const val TAG = "IpcStrategyWithRetry"
    }

    override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle? {
        val methodTag = "$TAG:communicateToBroker"
        val span = SpanExtension.current()

        for (attempt in 0..maxRetries) {
            try {
                val result = delegate.communicateToBroker(bundle)
                if (attempt > 0) {
                    span.setAttribute(AttributeName.ipc_retry_count.name, attempt)
                    span.setAttribute(AttributeName.ipc_retry_succeeded.name, true)
                }
                return result
            } catch (e: BrokerCommunicationException) {
                val isRetryable = e.category == CONNECTION_ERROR || e.category == NULL_CURSOR
                if (isRetryable && attempt < maxRetries) {
                    Logger.info(methodTag, "IPC attempt $attempt failed with ${e.category}, retrying (attempt ${attempt + 1} of $maxRetries).")
                    // IPC calls run on worker threads; Thread.sleep() is safe here.
                    Thread.sleep(baseDelayMs * (1L shl attempt))
                } else {
                    if (attempt > 0) {
                        span.setAttribute(AttributeName.ipc_retry_count.name, attempt)
                        span.setAttribute(AttributeName.ipc_retry_succeeded.name, false)
                    }
                    throw e
                }
            }
        }

        // Unreachable: the loop always returns or throws, but required by the compiler.
        throw IllegalStateException("Unexpected end of retry loop.")
    }

    override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String): Boolean {
        return delegate.isSupportedByTargetedBroker(targetedBrokerPackageName)
    }

    override fun getType(): IIpcStrategy.Type {
        return delegate.getType()
    }
}
