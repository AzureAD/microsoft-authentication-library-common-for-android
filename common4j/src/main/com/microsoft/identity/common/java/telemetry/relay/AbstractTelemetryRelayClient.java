package com.microsoft.identity.common.java.telemetry.relay;

import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.telemetry.observers.ITelemetryObserver;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractTelemetryRelayClient<T> implements ITelemetryObserver<T> {

    private static final String TAG = AbstractTelemetryRelayClient.class.getSimpleName();
    private final List<ITelemetryEventFilter<T>> eventFilters = new ArrayList<>();

    public AbstractTelemetryRelayClient() {
    }

    @Override
    public void onReceived(T telemetryData) {
        final String methodTag = TAG + ":onReceived";

        for (final ITelemetryEventFilter<T> filter : eventFilters) {
            if (!filter.shouldRelay(telemetryData)) {
                return;
            }
        }

        try {
            relayEvent(telemetryData);
        } catch (TelemetryRelayException e) {
            Logger.error(methodTag, "Error relaying telemetry data", e);
        }
    }

    public void removeFilter(ITelemetryEventFilter<T> filter) {
        this.eventFilters.remove(filter);
    }

    public void addFilter(ITelemetryEventFilter<T> filter) {
        this.eventFilters.add(filter);
    }

    public void clearFilters() {
        this.eventFilters.clear();
    }

    public abstract void initialize() throws TelemetryRelayException;

    public abstract boolean isInitialized();

    public abstract void relayEvent(final T eventData) throws TelemetryRelayException;
}
