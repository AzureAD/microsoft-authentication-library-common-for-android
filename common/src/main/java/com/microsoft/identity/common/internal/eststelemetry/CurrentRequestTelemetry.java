package com.microsoft.identity.common.internal.eststelemetry;

import androidx.annotation.Nullable;

// Current Telemetry won't be saved to the cache
public class CurrentRequestTelemetry extends RequestTelemetry implements ICurrentTelemetry {

    private String mApiId;
    private boolean mForceRefresh;

    CurrentRequestTelemetry() {
        super(SchemaConstants.CURRENT_SCHEMA_VERSION);
    }

    String getApiId() {
        return mApiId;
    }

    boolean getForceRefresh() {
        return mForceRefresh;
    }

    @Override
    public String getHeaderStringForFields() {
        return TelemetryUtils.getSchemaCompliantString(mApiId) + "," +
                TelemetryUtils.getSchemaCompliantStringFromBoolean(mForceRefresh);

    }

    @Override
    public void put(@Nullable final String key, @Nullable final String value) {
        switch (key) {
            case SchemaConstants.Key.API_ID:
                mApiId = value;
                break;
            case SchemaConstants.Key.FORCE_REFRESH:
                mForceRefresh = TelemetryUtils.getBooleanFromSchemaString(value);
                break;
            default:
                putInPlatformTelemetry(key, value);
                break;
        }
    }
}
