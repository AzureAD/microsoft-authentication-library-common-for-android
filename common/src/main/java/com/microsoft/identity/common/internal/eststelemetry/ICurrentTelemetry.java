package com.microsoft.identity.common.internal.eststelemetry;

import androidx.annotation.Nullable;

public interface ICurrentTelemetry {

    /**
     * Capture telemetry for current request
     *
     * @param key
     * @param value
     */
    void put(@Nullable final String key, @Nullable final String value);
}
