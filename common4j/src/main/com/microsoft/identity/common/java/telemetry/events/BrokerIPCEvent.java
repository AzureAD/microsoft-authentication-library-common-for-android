package com.microsoft.identity.common.java.telemetry.events;

import static com.microsoft.identity.common.java.telemetry.TelemetryEventStrings.Event;

import com.microsoft.identity.common.java.telemetry.TelemetryEventStrings;
import com.microsoft.identity.common.java.util.StringUtil;

import javax.annotation.Nullable;

import lombok.NonNull;

public class BrokerIPCEvent extends BaseEvent {

    public BrokerIPCEvent() {
        super();
        names(Event.BROKER_STRATEGY_EVENT);
        types(TelemetryEventStrings.EventType.BROKER_EVENT);
    }

    public BrokerIPCEvent putIpcStrategy(@NonNull final String ipcStrategy, @Nullable final String correlationId) {
        if (!StringUtil.isNullOrEmpty(correlationId)) {
            put(TelemetryEventStrings.Key.IPC_STRATEGY, ipcStrategy);
            put(TelemetryEventStrings.Key.CORRELATION_ID, correlationId);
        }
        return this;
    }
}
