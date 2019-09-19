package com.microsoft.identity.common.internal.servertelemetry;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;
import com.microsoft.identity.common.internal.logging.Logger;

import java.util.Map;

public class SharedPreferencesLastRequestTelemetryCache extends SharedPreferencesRequestTelemetryCache {

    private final static String TAG = SharedPreferencesLastRequestTelemetryCache.class.getSimpleName();

    /**
     * Constructor of SharedPreferencesLastRequestTelemetryCache.
     *
     * @param sharedPreferencesFileManager ISharedPreferencesFileManager
     */
    SharedPreferencesLastRequestTelemetryCache(
            @NonNull final ISharedPreferencesFileManager sharedPreferencesFileManager) {
        super(sharedPreferencesFileManager);
    }

    @Override
    public synchronized RequestTelemetry getRequestTelemetryFromCache() {
        final String methodName = ":getRequestTelemetryFromCache";

        final Map<String, String> data = super.getSharedPreferencesFileManager().getAll();

        if (data == null || data.isEmpty()) {
            Logger.verbose(TAG + methodName,
                    "Last Request telemetry not found in cache. " +
                            "Returning an empty RequestTelemetry object."
            );

            return new RequestTelemetry(null, false);
        }

        final String schemaVersion = data.get(Schema.Key.SCHEMA_VERSION);
        final RequestTelemetry lastRequestTelemetry = new RequestTelemetry(schemaVersion, false);

        final String[] lastCommonFields = Schema.getCommonFields(false);
        final String[] lastPlatformFields = Schema.getPlatformFields(false);

        for (String key : lastCommonFields) {
            lastRequestTelemetry.putTelemetry(key, data.get(key));
        }

        for (String key : lastPlatformFields) {
            lastRequestTelemetry.putTelemetry(key, data.get(key));
        }

        return lastRequestTelemetry;
    }
}
