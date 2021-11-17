package com.microsoft.identity.common.java.telemetry.relay;

import com.microsoft.identity.common.java.telemetry.observers.ITelemetryAggregatedObserver;

public interface ITelemetryRelayClient extends ITelemetryAggregatedObserver {

    /**
     * Handle initialization of the relay client before being registered as an observer.
     *
     * @throws TelemetryRelayClientException when initialization failed, with the appropriate
     *                                       error code.
     */
    void initialize() throws TelemetryRelayClientException;

    /**
     * Handle detach from the telemetry system.
     * This would be where we de-register the relay system from sending any more events.
     */
    void unInitialize();
}
