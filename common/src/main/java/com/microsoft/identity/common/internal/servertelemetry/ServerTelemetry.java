package com.microsoft.identity.common.internal.servertelemetry;

import android.content.Context;

import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;

import java.util.HashMap;
import java.util.Map;

public class ServerTelemetry {
    private final static String TAG = ServerTelemetry.class.getSimpleName();

    /**
     * The name of the SharedPreferences file on disk for the last request.
     */
    private static final String LAST_REQUEST_TELEMETRY_SHARED_PREFERENCES =
            "com.microsoft.identity.client.last_request_telemetry";

    private static IRequestTelemetryCache sLastRequestTelemetryCache;

    private static RequestTelemetry sCurrentRequestTelemetry = null;
    private static RequestTelemetry sLastRequestTelemetry = null;

    public static void initializeServerTelemetry(Context context) {
        final String methodName = ":initializeServerTelemetry";

        Logger.verbose(
                TAG + methodName,
                "Initializing server side telemetry"
        );

        sLastRequestTelemetryCache = createLastRequestTelemetryCache(context);
    }

    public static void emit(Map<String, String> telemetry) {
        for (Map.Entry<String, String> entry : telemetry.entrySet()) {
            emit(entry.getKey(), entry.getValue());
        }
    }

    // always emits to current request
    public static void emit(String key, String value) {
        putForCurrent(key, value);
    }

    private static RequestTelemetry getCurrentTelemetryInstance() {
        final String methodName = ":getCurrentTelemetryInstance";

        if (sCurrentRequestTelemetry == null) {
            Logger.verbose(
                    TAG + methodName,
                    "sCurrentRequestTelemetry object was null. " +
                            "Creating a new current request telemetry object."
            );

            sCurrentRequestTelemetry = new RequestTelemetry(Schema.Value.SCHEMA_VERSION, true);
        }

        return sCurrentRequestTelemetry;
    }

    private static RequestTelemetry getLastTelemetryInstance() {
        final String methodName = ":getLastTelemetryInstance";

        if (sLastRequestTelemetry == null) {
            Logger.verbose(
                    TAG + methodName,
                    "sLastRequestTelemetry object was null. " +
                            "Creating a new last request telemetry object."
            );

            sLastRequestTelemetry = new RequestTelemetry(Schema.Value.SCHEMA_VERSION, false);
        }

        return sLastRequestTelemetry;
    }

    public static void startScenario() {
        final String methodName = ":startScenario";
        sCurrentRequestTelemetry = new RequestTelemetry(true);
        if (sLastRequestTelemetryCache == null) {
            Logger.verbose(
                    TAG + methodName,
                    "Last Request Telemetry Cache has not been initialized. " +
                            "Cannot load Last Request Telemetry data from cache."
            );
            return;
        }

        sLastRequestTelemetry = sLastRequestTelemetryCache.getRequestTelemetryFromCache();
    }

    private static void putForCurrent(String key, String value) {
        getCurrentTelemetryInstance().putTelemetry(key, value);
    }

    private static void putForLast(String key, String value) {
        getLastTelemetryInstance().putTelemetry(key, value);
    }

    private static void putLastErrorCode(String errorCode) {
        putForLast(Schema.Key.ERROR_CODE, errorCode);
    }

    private static void putLastCorrelationId(String correlationId) {
        putForLast(Schema.Key.CORRELATION_ID, correlationId);
    }

    public static RequestTelemetry getCurrentTelemetry() {
        return sCurrentRequestTelemetry;
    }

    public static RequestTelemetry getLastTelemetry() {
        return sLastRequestTelemetry;
    }

    private static IRequestTelemetryCache createLastRequestTelemetryCache(Context context) {
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

    static void clearLastRequestTelemetry() {
        if (sLastRequestTelemetry != null) {
            sLastRequestTelemetry.clearTelemetry();
            sLastRequestTelemetry = null;
        }

        if (sLastRequestTelemetryCache != null) {
            sLastRequestTelemetryCache.clearAll();
        }
    }

    static void clearCurrentRequestTelemetry() {
        if (sCurrentRequestTelemetry != null) {
            sCurrentRequestTelemetry.clearTelemetry();
            sCurrentRequestTelemetry = null;
        }
    }

    private static void setupLastFromCurrent() {
        sLastRequestTelemetry = new RequestTelemetry(sCurrentRequestTelemetry.getSchemaVersion(), false);

        // grab whatever common fields we can from current request
        for (Map.Entry<String, String> entry : sCurrentRequestTelemetry.getCommonTelemetry().entrySet()) {
            putForLast(entry.getKey(), entry.getValue());
        }

        // grab whatever platform fields we can from current request
        for (Map.Entry<String, String> entry : sCurrentRequestTelemetry.getPlatformTelemetry().entrySet()) {
            putForLast(entry.getKey(), entry.getValue());
        }
    }

    private static void completeScenario(String correlationId, String errorCode) {
        final String methodName = ":completeScenario";

        clearLastRequestTelemetry();
        setupLastFromCurrent();
        putLastCorrelationId(correlationId);
        putLastErrorCode(errorCode);

        if (sLastRequestTelemetryCache != null) {
            sLastRequestTelemetryCache.saveRequestTelemetryToCache(sLastRequestTelemetry);
        } else {
            Logger.verbose(
                    TAG + methodName,
                    "Last Request Telemetry Cache object was null. " +
                            "Unable to save request telemetry to cache."
            );
        }

        clearCurrentRequestTelemetry();
    }

    public static void completeScenario(String correlationId, AcquireTokenResult acquireTokenResult) {
        final String errorCode = TelemetryUtils.errorFromAcquireTokenResult(acquireTokenResult);
        completeScenario(correlationId, errorCode);
    }

    static String getCurrentTelemetryHeaderString() {
        if (sCurrentRequestTelemetry == null) {
            return null;
        }

        return sCurrentRequestTelemetry.getCompleteTelemetryHeaderString();
    }

    static String getLastTelemetryHeaderString() {
        if (sLastRequestTelemetry == null) {
            return null;
        }

        return sLastRequestTelemetry.getCompleteTelemetryHeaderString();
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
