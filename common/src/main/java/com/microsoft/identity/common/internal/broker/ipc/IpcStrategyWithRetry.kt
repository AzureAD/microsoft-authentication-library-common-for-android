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
import com.microsoft.identity.common.java.logging.Logger

/**
 * An IPC strategy decorator that retries the wrapped strategy on failure using
 * exponential backoff.
 *
 * @param wrapped      The underlying [IIpcStrategy] to retry.
 * @param maxRetries   Maximum number of retry attempts (not counting the initial attempt).
 * @param baseDelayMs  Base delay in milliseconds between retry attempts (doubles each attempt).
 */
class IpcStrategyWithRetry(
    private val wrapped: IIpcStrategy,
    private val maxRetries: Int,
    private val baseDelayMs: Long
) : IIpcStrategy {

    companion object {
        private val TAG = IpcStrategyWithRetry::class.simpleName
    }

    override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle? {
        val methodTag = "$TAG:communicateToBroker"
        var lastException: Throwable? = null
        var delayMs = baseDelayMs
        for (attempt in 0..maxRetries) {
            try {
                if (attempt > 0) {
                    Logger.info(methodTag, "Retrying IPC attempt $attempt of $maxRetries for ${wrapped.getType().name}.")
                    Thread.sleep(delayMs)
                    delayMs *= 2
                }
                return wrapped.communicateToBroker(bundle)
            } catch (t: Throwable) {
                Logger.info(methodTag, "IPC attempt $attempt failed for ${wrapped.getType().name}: ${t.message}")
                lastException = t
            }
        }
        throw lastException!!
    }

    override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String): Boolean {
        return wrapped.isSupportedByTargetedBroker(targetedBrokerPackageName)
    }

    override fun getType(): IIpcStrategy.Type {
        return wrapped.getType()
    }
}
