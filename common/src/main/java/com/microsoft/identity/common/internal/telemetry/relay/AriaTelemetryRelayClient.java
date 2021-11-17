package com.microsoft.identity.common.internal.telemetry.relay;

import android.content.Context;
import android.util.Log;

import com.microsoft.applications.telemetry.EventProperties;
import com.microsoft.applications.telemetry.ILogger;
import com.microsoft.applications.telemetry.LogConfiguration;
import com.microsoft.applications.telemetry.LogManager;
import com.microsoft.identity.common.java.telemetry.relay.ITelemetryRelayClient;
import com.microsoft.identity.common.java.telemetry.relay.TelemetryRelayClientException;

import java.util.Map;

public class AriaTelemetryRelayClient implements ITelemetryRelayClient {
    private static final String TAG = AriaTelemetryRelayClient.class.getSimpleName();

    private ILogger logger;
    private final Context context;
    private final String ariaToken;
    private final LogConfiguration logConfiguration;

    public AriaTelemetryRelayClient(Context context, String ariaToken) {
        this(context, ariaToken, new LogConfiguration());
    }

    public AriaTelemetryRelayClient(Context context, String ariaToken, LogConfiguration logConfiguration) {
        this.context = context;
        this.ariaToken = ariaToken;
        this.logConfiguration = logConfiguration;
    }

    public void setLogger(ILogger logger) {
        this.logger = logger;
    }

    public ILogger getLogger() {
        return logger;
    }

    @Override
    public void initialize() throws TelemetryRelayClientException {
        try {
            logger = LogManager.initialize(context, ariaToken, logConfiguration);
        } catch (Exception exception) {
            logger = LogManager.getLogger(ariaToken, "");

            if (logger == null) {
                throw new TelemetryRelayClientException("Aria failed to initialize LogManager",
                        exception,
                        TelemetryRelayClientException.INITIALIZATION_FAILED);
            }
        }
    }

    @Override
    public void onReceived(Map<String, String> telemetryData) {
        final EventProperties eventProperties = new EventProperties(AriaTable.EVENT.getName());
        for (final Map.Entry<String, String> entry : telemetryData.entrySet()) {
            eventProperties.setProperty(entry.getKey(), entry.getValue());
        }
        logger.logEvent(eventProperties);
        LogManager.flush();
    }


    @Override
    public void unInitialize() {
        LogManager.flushAndTeardown();
    }

}
