package com.microsoft.identity.common.java.telemetry.relay;

public interface ITelemetryEventFilter<T> {
    boolean shouldRelay(T telemetryEvent);
}
