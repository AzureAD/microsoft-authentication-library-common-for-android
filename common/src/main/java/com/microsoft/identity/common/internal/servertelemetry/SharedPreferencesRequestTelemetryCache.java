package com.microsoft.identity.common.internal.servertelemetry;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;
import com.microsoft.identity.common.internal.logging.Logger;

import java.util.Map;

public abstract class SharedPreferencesRequestTelemetryCache implements IRequestTelemetryCache {

    private final static String TAG = SharedPreferencesRequestTelemetryCache.class.getSimpleName();


    // SharedPreferences used to store request telemetry data
    private final ISharedPreferencesFileManager mSharedPreferencesFileManager;

    /**
     * Constructor of SharedPreferencesRequestTelemetryCache.
     *
     * @param sharedPreferencesFileManager ISharedPreferencesFileManager
     */
    SharedPreferencesRequestTelemetryCache(
            @NonNull final ISharedPreferencesFileManager sharedPreferencesFileManager) {
        Logger.verbose(TAG, "Init: " + TAG);
        mSharedPreferencesFileManager = sharedPreferencesFileManager;
    }

    ISharedPreferencesFileManager getSharedPreferencesFileManager() {
        return mSharedPreferencesFileManager;
    }

    @Override
    public synchronized void saveRequestTelemetryToCache(@NonNull final RequestTelemetry requestTelemetry) {
        Logger.verbose(TAG, "Saving Request Telemetry to cache...");

        mSharedPreferencesFileManager.putString(Schema.Key.SCHEMA_VERSION, Schema.Value.SCHEMA_VERSION);
        saveTelemetryDataToCache(requestTelemetry.getCommonTelemetry());
        saveTelemetryDataToCache(requestTelemetry.getPlatformTelemetry());
    }

    private synchronized void saveTelemetryDataToCache(@NonNull final Map<String, String> data) {
        for (Map.Entry<String, String> entry : data.entrySet()) {
            final String cacheKey = entry.getKey();
            final String cacheValue = entry.getValue();
            mSharedPreferencesFileManager.putString(cacheKey, cacheValue);
        }
    }

    @Override
    public synchronized void clearAll() {
        Logger.info(TAG, "Clearing all SharedPreferences entries...");
        mSharedPreferencesFileManager.clear();
        Logger.info(TAG, "SharedPreferences cleared.");
    }


}
