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
package com.microsoft.identity.common.kotlin.broker

import com.microsoft.identity.common.java.logging.Logger

/**
 * Holds broker performance metrics derived from timestamps in authentication flows.
 * Calculates processing and latency durations at construction time.
 */
class BrokerPerformanceMetrics(
    /**
     * Timestamp when the broker received the authentication request (epoch milliseconds).
     * Used to calculate broker handling time.
     */
    val brokerRequestReceivedTimestamp: Long,

    /**
     * Timestamp when the broker generated the authentication response (epoch milliseconds).
     * Used to calculate both broker handling time and response latency.
     */
    val brokerResponseGenerationTimestamp: Long
) {
    /**
     * Time spent by broker processing the request (in milliseconds).
     * Calculated as: brokerResponseGenerationTimestamp - brokerRequestReceivedTimestamp
     */
    val brokerHandlingTime: Long

    /**
     * Time for the response to reach and be processed by the client (in milliseconds).
     * Calculated as: currentTime - brokerResponseGenerationTimestamp
     */
    val responseLatency: Long

    init {
        // Validation to avoid negative durations due to clock skew or invalid timestamps
        brokerHandlingTime =
            if (brokerResponseGenerationTimestamp < brokerRequestReceivedTimestamp) {
                Logger.warn(TAG, "Invalid timestamps: response before request")
                0
            } else {
                brokerResponseGenerationTimestamp - brokerRequestReceivedTimestamp
            }
        responseLatency = System.currentTimeMillis() - brokerResponseGenerationTimestamp
    }

    override fun toString(): String {
        return "BrokerPerformanceMetrics(" +
                "brokerHandlingTime=$brokerHandlingTime, " +
                "responseLatency=$responseLatency, " +
                "brokerRequestReceivedTimestamp=$brokerRequestReceivedTimestamp, " +
                "brokerResponseGenerationTimestamp=$brokerResponseGenerationTimestamp)"
    }

    companion object {
        private val TAG = BrokerPerformanceMetrics::class.java.simpleName


        /**
         * A safe default instance used when no real metrics are available.
         * Ensures non-null contract for Java callers.
         *
         * @param brokerRequestReceivedTimestamp Current time as placeholder.
         * @param brokerResponseGenerationTimestamp Current time as placeholder.
         */
        @JvmField
        var EMPTY = BrokerPerformanceMetrics(
            brokerRequestReceivedTimestamp =  System.currentTimeMillis(),
            brokerResponseGenerationTimestamp = System.currentTimeMillis()
        )

    }
}