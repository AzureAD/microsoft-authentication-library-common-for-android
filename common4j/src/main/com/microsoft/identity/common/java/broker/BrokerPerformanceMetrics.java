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
package com.microsoft.identity.common.java.broker;

import com.microsoft.identity.common.java.logging.Logger;

/**
 * Holds broker performance metrics derived from timestamps in authentication flows.
 * Calculates processing and latency durations at construction time.
 */
public class BrokerPerformanceMetrics {
    private static final String TAG = BrokerPerformanceMetrics.class.getSimpleName();

    /**
     * Time spent by broker processing the request (in milliseconds).
     * Calculated as: brokerResponseGenerationTimestamp - brokerRequestReceivedTimestamp
     */
    private final long brokerHandlingTime;

    /**
     * Time for the response to reach and be processed by the client (in milliseconds).
     * Calculated as: currentTime - brokerResponseGenerationTimestamp
     */
    private final long responseLatency;


    /**
     * Timestamp when the broker received the authentication request (epoch milliseconds).
     * Used to calculate broker handling time.
     */
    private final long brokerRequestReceivedTimestamp;

    /**
     * Timestamp when the broker generated the authentication response (epoch milliseconds).
     * Used to calculate both broker handling time and response latency.
     */
    private final long brokerResponseGenerationTimestamp;

    /**
     * Creates broker performance metrics with calculated durations.
     *
     * @param brokerRequestReceivedTimestamp When broker received the request (epoch millis)
     * @param brokerResponseGenerationTimestamp When broker generated the response (epoch millis)
     */
    public BrokerPerformanceMetrics(final long brokerRequestReceivedTimestamp,
                                    final long brokerResponseGenerationTimestamp) {
        this.brokerRequestReceivedTimestamp = brokerRequestReceivedTimestamp;
        this.brokerResponseGenerationTimestamp = brokerResponseGenerationTimestamp;
        // Validation to avoid negative durations due to clock skew or invalid timestamps
        if (brokerResponseGenerationTimestamp < brokerRequestReceivedTimestamp) {
            Logger.warn(TAG, "Invalid timestamps: response before request");
            this.brokerHandlingTime = 0;
        } else {
            this.brokerHandlingTime = brokerResponseGenerationTimestamp - brokerRequestReceivedTimestamp;
        }
        this.responseLatency = System.currentTimeMillis() - brokerResponseGenerationTimestamp;
    }

    public long getBrokerHandlingTime() {
        return brokerHandlingTime;
    }

    public long getBrokerRequestReceivedTimestamp() {
        return brokerRequestReceivedTimestamp;
    }

    public long getBrokerResponseGenerationTimestamp() {
        return brokerResponseGenerationTimestamp;
    }

    public long getResponseLatency() {
        return responseLatency;
    }

    @Override
    public String toString() {
        return "BrokerPerformanceMetrics{" +
                "brokerHandlingTime=" + brokerHandlingTime +
                ", responseLatency=" + responseLatency +
                ", brokerRequestReceivedTimestamp=" + brokerRequestReceivedTimestamp +
                ", brokerResponseGenerationTimestamp=" + brokerResponseGenerationTimestamp +
                '}';
    }
}
