package com.microsoft.identity.common.internal.servertelemetry;

import android.content.Context;

import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
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

        final IRequestTelemetryCache lastRequestTelemetryCache = createLastRequestTelemetryCache(context);
        sLastRequestTelemetryCache = lastRequestTelemetryCache;
        startScenario(); // do we need to call this here?
    }

    public static void emit(Map<String, String> telemetry) {
        for(Map.Entry<String, String> entry: telemetry.entrySet()) {
            emit(entry.getKey(), entry.getValue());
        }
    }

    // always emits to current request
    public static void emit(String key, String value) {
        putForCurrent(key, value);
    }

    private static RequestTelemetry getCurrentTelemetryInstance() {
        final String methodName = ":getCurrentTelemetryInstance";

        /**
         * This should never happen. If it does happen, it means there is an error in the code.
         * Lets check for null here to avoid a NullPointerException,
         * and create a new RequestTelemetry object to continue capturing telemetry data.
         **/
        if (sCurrentRequestTelemetry == null) {
            Logger.verbose(
                    TAG + methodName,
                    "sCurrentRequestTelemetry object was null. " +
                            "Creating a new object to capture as much data as possible"
            );

            sCurrentRequestTelemetry = new RequestTelemetry(Schema.Value.SCHEMA_VERSION, true);
        }

        return sCurrentRequestTelemetry;
    }

    private static RequestTelemetry getLastTelemetryInstance() {
        final String methodName = ":getLastTelemetryInstance";

        /**
         * This should never happen. If it does happen, it means there is an error in the code.
         * Lets check for null here to avoid a NullPointerException,
         * and create a new RequestTelemetry object to continue capturing telemetry data.
         **/
        if (sLastRequestTelemetry == null) {
            Logger.verbose(
                    TAG + methodName,
                    "sLastRequestTelemetry object was null. " +
                            "Creating a new object to capture as much data as possible"
            );

            sLastRequestTelemetry = new RequestTelemetry(Schema.Value.SCHEMA_VERSION, false);
        }

        return sLastRequestTelemetry;
    }

    public static void startScenario() {
        final String methodName = ":startScenario";
        sCurrentRequestTelemetry = new RequestTelemetry(true);
        //putCurrentLoggingEnabled(Logger.getAllowLogcat());
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

    private static void transformCurrentToLast() {
        sLastRequestTelemetry = new RequestTelemetry(sCurrentRequestTelemetry.getSchemaVersion(), false);

        for(Map.Entry<String, String> entry : sCurrentRequestTelemetry.getCommonTelemetry().entrySet()) {
            putForLast(entry.getKey(), entry.getValue());
        }

        for(Map.Entry<String, String> entry : sCurrentRequestTelemetry.getPlatformTelemetry().entrySet()) {
            putForLast(entry.getKey(), entry.getValue());
        }
    }

    private static void putForCurrent(String key, String value) {
        getCurrentTelemetryInstance().putTelemetry(key, value);
    }

    private static void putForLast(String key, String value) {
        getLastTelemetryInstance().putTelemetry(key, value);
    }

//    public static void putCurrentApiId(String apiId) {
//        putForCurrent(Schema.Key.API_ID, apiId);
//    }
//
//    public static void putCurrentScenarioId(String scenarioId) {
//        putForCurrent(Schema.Key.SCENARIO_ID, scenarioId);
//    }
//
//    public static void putCurrentTelemetryEnabled(boolean telemetryEnabled) {
//        putForCurrent(Schema.Key.TELEMETRY_ENABLED, telemetryEnabled);
//    }
//
//    public static void putCurrentLoggingEnabled(boolean loggingEnabled) {
//        putForCurrent(Schema.Key.LOGGING_ENABLED, loggingEnabled);
//    }
//
//    public static void putCurrentForceRefresh(boolean forceRefresh) {
//        putForCurrent(Schema.Key.FORCE_REFRESH, forceRefresh);
//    }
//
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

        final IRequestTelemetryCache lastRequestTelemetryCache =
                new SharedPreferencesLastRequestTelemetryCache(sharedPreferencesFileManager);

        return lastRequestTelemetryCache;
    }

    private static void clearLastRequestTelemetry() {
        if (sLastRequestTelemetry != null) {
            sLastRequestTelemetry.clearTelemetry();
            sLastRequestTelemetry = null; // do we need to create new objects for each request? or is clearing the hashmap going to be enough?
        }

        if (sLastRequestTelemetryCache != null) {
            sLastRequestTelemetryCache.clearAll();
        }
    }

    private static void clearCurrentRequestTelemetry() {
        if (sCurrentRequestTelemetry != null) {
            sCurrentRequestTelemetry.clearTelemetry();
            sCurrentRequestTelemetry = null;
        }
    }

    private static void setupLastFromCurrent() {
        sLastRequestTelemetry = new RequestTelemetry(sCurrentRequestTelemetry.getSchemaVersion(), false);

        // grab whatever common fields we can from current request
        for(Map.Entry<String, String> entry: sCurrentRequestTelemetry.getCommonTelemetry().entrySet()) {
            putForLast(entry.getKey(), entry.getValue());
        }

        // grab whatever platform fields we can from current request
        for(Map.Entry<String, String> entry: sCurrentRequestTelemetry.getPlatformTelemetry().entrySet()) {
            putForLast(entry.getKey(), entry.getValue());
        }
    }

    public static void completeScenario(String correlationId, String errorCode) {
        clearLastRequestTelemetry();
        setupLastFromCurrent();
        putLastCorrelationId(correlationId);
        putLastErrorCode(errorCode);
        sLastRequestTelemetryCache.saveRequestTelemetryToCache(sLastRequestTelemetry);
        clearCurrentRequestTelemetry();
    }

    public static void completeScenario(String correlationId, AcquireTokenResult acquireTokenResult) {
        final String errorCode = errorFromAcquireTokenResult(acquireTokenResult);
        completeScenario(correlationId, errorCode);
    }

    public static String getCurrentTelemetryHeaderString() {
        return sCurrentRequestTelemetry.getCompleteTelemetryHeaderString();
    }

    public static String getLastTelemetryHeaderString() {
        return sLastRequestTelemetry.getCompleteTelemetryHeaderString();
    }

    public static Map<String, String> getTelemetryHeaders() {
        return new HashMap<String, String>() {{
            put(Schema.CURRENT_REQUEST_HEADER_NAME, getCurrentTelemetryHeaderString());
            put(Schema.LAST_REQUEST_HEADER_NAME, getLastTelemetryHeaderString());
        }};
    }

    public static String errorFromAcquireTokenResult(final AcquireTokenResult acquireTokenResult) {
        final String errorFromAuthorization = getErrorFromAuthorizationResult(acquireTokenResult.getAuthorizationResult());
        if (errorFromAuthorization != null) {
            return errorFromAuthorization;
        } else {
            return getErrorFromTokenResult(acquireTokenResult.getTokenResult());
        }
    }

    private static String getErrorFromAuthorizationResult(final AuthorizationResult authorizationResult) {
        if (authorizationResult != null && authorizationResult.getErrorResponse() != null) {
            return authorizationResult.getErrorResponse().getError();
        } else {
            return null;
        }
    }

    private static String getErrorFromTokenResult(final TokenResult tokenResult) {
        if (tokenResult != null && tokenResult.getErrorResponse() != null) {
            return tokenResult.getErrorResponse().getError();
        } else {
            return null;
        }
    }

}
