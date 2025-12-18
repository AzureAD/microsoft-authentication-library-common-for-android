package com.microsoft.identity.common.java.broker

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
        var EMPTY = run {
            val now = System.currentTimeMillis()
            BrokerPerformanceMetrics(
                brokerRequestReceivedTimestamp = now,
                brokerResponseGenerationTimestamp = now
            )
        }
    }
}