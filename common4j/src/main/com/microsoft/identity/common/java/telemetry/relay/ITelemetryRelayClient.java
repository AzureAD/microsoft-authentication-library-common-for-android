package com.microsoft.identity.common.java.telemetry.relay;

import com.microsoft.identity.common.java.telemetry.observers.ITelemetryAggregatedObserver;

public interface ITelemetryRelayClient extends ITelemetryAggregatedObserver {

    void initialize() throws TelemetryRelayClientException;

    void unInitialize();
}

