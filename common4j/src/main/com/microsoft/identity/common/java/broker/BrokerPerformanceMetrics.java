package com.microsoft.identity.common.java.broker;

/**
 * Holds broker performance metrics derived from timestamps in authentication flows.
 * Calculates processing and latency durations at construction time.
 */
public class BrokerPerformanceMetrics {
    /**
     * Time spent by broker processing the request (in milliseconds).
     * Calculated as: brokerResponseGenerationTimestamp - brokerRequestReceivedTimestamp
     */
    private final Long brokerHandlingTime;

    /**
     * Time for the response to reach and be processed by the client (in milliseconds).
     * Calculated as: currentTime - brokerResponseGenerationTimestamp
     */
    private final Long responseLatency;


    /**
     * Timestamp when the broker received the authentication request (epoch milliseconds).
     * Used to calculate broker handling time.
     */
    private final Long brokerRequestReceivedTimestamp;

    /**
     * Timestamp when the broker generated the authentication response (epoch milliseconds).
     * Used to calculate both broker handling time and response latency.
     */
    private final Long brokerResponseGenerationTimestamp;

    /**
     * Creates broker performance metrics with calculated durations.
     *
     * @param brokerRequestReceivedTimestamp When broker received the request (epoch millis)
     * @param brokerResponseGenerationTimestamp When broker generated the response (epoch millis)
     */
    public BrokerPerformanceMetrics(final Long brokerRequestReceivedTimestamp,
                                    final Long brokerResponseGenerationTimestamp) {
        this.brokerRequestReceivedTimestamp = brokerRequestReceivedTimestamp;
        this.brokerResponseGenerationTimestamp = brokerResponseGenerationTimestamp;
        this.brokerHandlingTime = brokerResponseGenerationTimestamp - brokerRequestReceivedTimestamp;
        this.responseLatency = System.currentTimeMillis() - brokerResponseGenerationTimestamp;
    }

    public Long getBrokerHandlingTime() {
        return brokerHandlingTime;
    }

    public Long getBrokerRequestReceivedTimestamp() {
        return brokerRequestReceivedTimestamp;
    }

    public Long getBrokerResponseGenerationTimestamp() {
        return brokerResponseGenerationTimestamp;
    }

    public Long getResponseLatency() {
        return responseLatency;
    }

    @Override
    public String toString() {
        return "BrokerPerformanceMetrics{" +
                "brokerHandlingTime=" + brokerHandlingTime +
                ", responseLatency=" + responseLatency +
                ", brokerRequestReceivedTimestamp=" + brokerRequestReceivedTimestamp ;
    }
}
