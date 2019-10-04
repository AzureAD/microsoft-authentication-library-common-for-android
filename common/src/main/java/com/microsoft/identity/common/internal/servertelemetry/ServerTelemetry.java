package com.microsoft.identity.common.internal.servertelemetry;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.internal.logging.DiagnosticContext;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerTelemetry {
    private final static String TAG = ServerTelemetry.class.getSimpleName();

    /**
     * The name of the SharedPreferences file on disk for the last request.
     */
    private static final String LAST_REQUEST_TELEMETRY_SHARED_PREFERENCES =
            "com.microsoft.identity.client.last_request_telemetry";

    private static IRequestTelemetryCache sLastRequestTelemetryCache;
    private static Map<String, RequestTelemetry> sTelemetryMap;

    public static void initializeServerTelemetry(@NonNull final Context context) {
        final String methodName = ":initializeServerTelemetry";

        Logger.verbose(
                TAG + methodName,
                "Initializing server side telemetry"
        );

        sTelemetryMap = new ConcurrentHashMap<>();

        sLastRequestTelemetryCache = createLastRequestTelemetryCache(context);
    }

    public static void emit(@NonNull final Map<String, String> telemetry) {
        if (telemetry == null) {
            return;
        }

        for (Map.Entry<String, String> entry : telemetry.entrySet()) {
            emit(entry.getKey(), entry.getValue());
        }
    }

    public static void emit(final String key, final String value) {
        final String correlationId = DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID);
        emit(correlationId, key, value);
    }

    private static void emit(final String correlationId, final String key, final String value) {
        RequestTelemetry currentTelemetryInstance = getCurrentTelemetryInstance(correlationId);
        if (currentTelemetryInstance != null) {
            currentTelemetryInstance.putTelemetry(key, value);
        }
    }

    private static RequestTelemetry getCurrentTelemetryInstance(final String correlationId) {
        if (sTelemetryMap == null || correlationId == null) {
            return null;
        }

        RequestTelemetry currentTelemetry = sTelemetryMap.get(correlationId);
        if (currentTelemetry != null) {
            return currentTelemetry;
        } else {
            RequestTelemetry telemetry = new RequestTelemetry(true);
            sTelemetryMap.put(correlationId, telemetry);
            return telemetry;
        }
    }

    private static RequestTelemetry loadLastRequestTelemetryFromCache() {
        final String methodName = ":loadLastRequestTelemetry";

        if (sLastRequestTelemetryCache == null) {
            Logger.verbose(
                    TAG + methodName,
                    "Last Request Telemetry Cache has not been initialized. " +
                            "Cannot load Last Request Telemetry data from cache."
            );
            return null;
        }

        return sLastRequestTelemetryCache.getRequestTelemetryFromCache();
    }


    public static void emitApiId(final String apiId) {
        emit(Schema.Key.API_ID, apiId);
    }

    public static void emitForceRefresh(final boolean forceRefresh) {
        String val = Schema.getSchemaCompliantStringFromBoolean(forceRefresh);
        emit(Schema.Key.FORCE_REFRESH, val);
    }

    private static IRequestTelemetryCache createLastRequestTelemetryCache(@NonNull final Context context) {
        final String methodName = ":createLastRequestTelemetryCache";

        Logger.verbose(
                TAG + methodName,
                "Creating Last Request Telemetry Cache"
        );

        final ISharedPreferencesFileManager sharedPreferencesFileManager =
                new SharedPreferencesFileManager(
                        context,
                        LAST_REQUEST_TELEMETRY_SHARED_PREFERENCES
                );

        return new SharedPreferencesLastRequestTelemetryCache(sharedPreferencesFileManager);
    }

    private static RequestTelemetry setupLastFromCurrent(@Nullable RequestTelemetry currentTelemetry) {
        if (currentTelemetry == null) {
            return new RequestTelemetry(Schema.Value.SCHEMA_VERSION, false);
        }

        RequestTelemetry lastTelemetry = new RequestTelemetry(currentTelemetry.getSchemaVersion(), false);

        // grab whatever common fields we can from current request
        for (Map.Entry<String, String> entry : currentTelemetry.getCommonTelemetry().entrySet()) {
            lastTelemetry.putTelemetry(entry.getKey(), entry.getValue());
        }

        // grab whatever platform fields we can from current request
        for (Map.Entry<String, String> entry : currentTelemetry.getPlatformTelemetry().entrySet()) {
            lastTelemetry.putTelemetry(entry.getKey(), entry.getValue());
        }

        return lastTelemetry;
    }

    public static void flush(final String correlationId, final String errorCode) {
        final String methodName = ":flush";
        if (sTelemetryMap == null) {
            return;
        }

        RequestTelemetry currentTelemetry = sTelemetryMap.get(correlationId);
        if (currentTelemetry == null) {
            return;
        }

        RequestTelemetry lastTelemetry = setupLastFromCurrent(currentTelemetry);
        lastTelemetry.putTelemetry(Schema.Key.CORRELATION_ID, correlationId);
        lastTelemetry.putTelemetry(Schema.Key.ERROR_CODE, errorCode);

        currentTelemetry.clearTelemetry();
        sTelemetryMap.remove(correlationId);


        if (sLastRequestTelemetryCache != null) {
            // remove old last request telemetry data from cache
            sLastRequestTelemetryCache.clearAll();
            // save new last request telemetry data to cache
            sLastRequestTelemetryCache.saveRequestTelemetryToCache(lastTelemetry);
        } else {
            Logger.verbose(
                    TAG + methodName,
                    "Last Request Telemetry Cache object was null. " +
                            "Unable to save request telemetry to cache."
            );
        }
    }

    public static void flush(final String correlationId, final BaseException baseException) {
        flush(correlationId, baseException == null ? null : baseException.getErrorCode());
    }

    public static void flush(final String correlationId) {
        flush(correlationId, (String) null);
    }

    public static void flush(final String correlationId, final AcquireTokenResult acquireTokenResult) {
        final String errorCode = TelemetryUtils.errorFromAcquireTokenResult(acquireTokenResult);
        flush(correlationId, errorCode);
    }

    static String getCurrentTelemetryHeaderString() {
        final String correlationId = DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID);
        if (sTelemetryMap == null || correlationId == null) {
            return null;
        }

        RequestTelemetry currentTelemetry = sTelemetryMap.get(correlationId);

        if (currentTelemetry == null) {
            return null;
        }

        return currentTelemetry.getCompleteTelemetryHeaderString();
    }

    static String getLastTelemetryHeaderString() {
        RequestTelemetry lastTelemetry = loadLastRequestTelemetryFromCache();

        if (lastTelemetry == null) {
            return null;
        }

        return lastTelemetry.getCompleteTelemetryHeaderString();
    }

    public static Map<String, String> getTelemetryHeaders() {
        final String methodName = ":getTelemetryHeaders";
        final Map<String, String> headerMap = new HashMap<>();

        final String currentHeader = getCurrentTelemetryHeaderString();
        final String lastHeader = getLastTelemetryHeaderString();

        if (currentHeader != null) {
            headerMap.put(Schema.CURRENT_REQUEST_HEADER_NAME, currentHeader);
        } else {
            Logger.verbose(
                    TAG + methodName,
                    "Current Request Telemetry Header is null"
            );
        }

        if (lastHeader != null) {
            headerMap.put(Schema.LAST_REQUEST_HEADER_NAME, lastHeader);
        } else {
            Logger.verbose(
                    TAG + methodName,
                    "Last Request Telemetry Header is null"
            );
        }

        return headerMap;
    }

}
