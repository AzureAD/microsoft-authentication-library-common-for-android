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
package com.microsoft.identity.common.java.ipc

import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager

/**
 * Configuration data class for IPC retry with exponential back-off.
 *
 * All values are loaded from flights via [CommonFlightsManager]. Use [fromFlights] to
 * construct an instance with current flight values.
 *
 * Instances of this class are immutable and safe to share across threads. The delay values
 * (e.g. [initialDelayMs], [maxDelayMs]) describe pauses that must be observed on a
 * background/worker thread; the retry loop itself must never execute on the main thread.
 *
 * @param enabled       Whether IPC retry is active; mirrors [CommonFlight.ENABLE_IPC_RETRY].
 * @param maxRetries    Maximum number of retry attempts; mirrors [CommonFlight.IPC_RETRY_COUNT].
 * @param initialDelayMs Initial delay before the first retry, in milliseconds;
 *                       mirrors [CommonFlight.IPC_RETRY_INITIAL_DELAY_MS].
 * @param extensionFactor Multiplicative back-off factor applied after each attempt;
 *                        mirrors [CommonFlight.IPC_RETRY_EXTENSION_FACTOR].
 * @param maxDelayMs    Upper bound on delay between retries, in milliseconds;
 *                      mirrors [CommonFlight.IPC_RETRY_MAX_DELAY_MS].
 */
data class IpcRetryPolicy(
    val enabled: Boolean,
    val maxRetries: Int,
    val initialDelayMs: Int,
    val extensionFactor: Int,
    val maxDelayMs: Int
) {
    companion object {
        /**
         * Creates an [IpcRetryPolicy] populated from the current flight values provided by
         * [CommonFlightsManager]. Falls back to each flight's default value when no
         * [com.microsoft.identity.common.java.flighting.IFlightsManager] has been initialised.
         */
        @JvmStatic
        fun fromFlights(): IpcRetryPolicy {
            val flightsProvider = CommonFlightsManager.getFlightsProvider()
            return IpcRetryPolicy(
                enabled = flightsProvider.getBooleanValue(CommonFlight.ENABLE_IPC_RETRY),
                maxRetries = flightsProvider.getIntValue(CommonFlight.IPC_RETRY_COUNT),
                initialDelayMs = flightsProvider.getIntValue(CommonFlight.IPC_RETRY_INITIAL_DELAY_MS),
                extensionFactor = flightsProvider.getIntValue(CommonFlight.IPC_RETRY_EXTENSION_FACTOR),
                maxDelayMs = flightsProvider.getIntValue(CommonFlight.IPC_RETRY_MAX_DELAY_MS)
            )
        }
    }
}
