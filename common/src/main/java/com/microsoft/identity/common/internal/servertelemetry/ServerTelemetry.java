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

    private static IRequestTelemetryCache mLastRequestTelemetryCache;

    private static RequestTelemetry mCurrentRequestTelemetry;
    private static RequestTelemetry mLastRequestTelemetry;

    public static void initializeServerTelemetry(Context context) {
        final String methodName = ":initializeServerTelemetry";

        Logger.verbose(
                TAG + methodName,
                "Initializing server side telemetry"
        );

        final IRequestTelemetryCache lastRequestTelemetryCache = createLastRequestTelemetryCache(context);
        mLastRequestTelemetryCache = lastRequestTelemetryCache;
        startScenario();
    }

    public static void startScenario() {
        final String methodName = ":startScenario";
        mCurrentRequestTelemetry = new RequestTelemetry(true);
        //putCurrentLoggingEnabled(Logger.getAllowLogcat());
        if (mLastRequestTelemetryCache == null) {
            Logger.verbose(
                    TAG + methodName,
                    "Last Request Telemetry Cache has not been initialized. " +
                            "Cannot load Last Request Telemetry data from cache."
            );
            return;
        }

        mLastRequestTelemetry = mLastRequestTelemetryCache.getRequestTelemetryFromCache();
    }

    private static void putForCurrent(String key, Object value) {
        final String methodName = ":putForCurrent";

        /**
         * This should never happen. If it does happen, it means there is an error in the code.
         * Lets check for null here to avoid a NullPointerException,
         * and create a new RequestTelemetry object to continue capturing telemetry data.
         **/
        if (mCurrentRequestTelemetry == null) {
            Logger.verbose(
                    TAG + methodName,
                    "mCurrentRequestTelemetry object was null. " +
                            "Creating a new object to capture as much data as possible"
            );

            mCurrentRequestTelemetry = new RequestTelemetry(Schema.Value.SCHEMA_VERSION, true);
        }

        mCurrentRequestTelemetry.putTelemetry(key, value);
    }

    private static void putForLast(String key, Object value) {
        final String methodName = ":putForLast";

        /**
         * This should never happen. If it does happen, it means there is an error in the code.
         * Lets check for null here to avoid a NullPointerException,
         * and create a new RequestTelemetry object to continue capturing telemetry data.
         **/
        if (mLastRequestTelemetry == null) {
            Logger.verbose(
                    TAG + methodName,
                    "mLastRequestTelemetry object was null. " +
                            "Creating a new object to capture as much data as possible"
            );

            mLastRequestTelemetry = new RequestTelemetry(Schema.Value.SCHEMA_VERSION, false);
        }

        mLastRequestTelemetry.putTelemetry(key, value);
    }

    public static void putCurrentApiId(String apiId) {
        putForCurrent(Schema.Key.API_ID, apiId);
    }

    public static void putCurrentScenarioId(String scenarioId) {
        putForCurrent(Schema.Key.SCENARIO_ID, scenarioId);
    }

    public static void putCurrentTelemetryEnabled(boolean telemetryEnabled) {
        putForCurrent(Schema.Key.TELEMETRY_ENABLED, telemetryEnabled);
    }

    public static void putCurrentLoggingEnabled(boolean loggingEnabled) {
        putForCurrent(Schema.Key.LOGGING_ENABLED, loggingEnabled);
    }

    public static void putCurrentForceRefresh(boolean forceRefresh) {
        putForCurrent(Schema.Key.FORCE_REFRESH, forceRefresh);
    }

    public static void putLastErrorCode(String errorCode) {
        putForLast(Schema.Key.ERROR_CODE, errorCode);
    }

    public static void putLastCorrelationId(String correlationId) {
        putForLast(Schema.Key.CORRELATION_ID, correlationId);
    }

    public static RequestTelemetry getCurrentTelemetry() {
        return mCurrentRequestTelemetry;
    }

    public static RequestTelemetry getLastTelemetry() {
        return mLastRequestTelemetry;
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
        mLastRequestTelemetry.clearTelemetry();
        mLastRequestTelemetryCache.clearAll();
        mLastRequestTelemetry = null;
    }

    private static void clearCurrentRequestTelemetry() {
        mCurrentRequestTelemetry.clearTelemetry();
        mCurrentRequestTelemetry = null;
    }

    private static void setupLastFromCurrent(String correlationId) {
        mLastRequestTelemetry = new RequestTelemetry(mCurrentRequestTelemetry.getSchemaVersion(), false);

        mLastRequestTelemetry.putTelemetry(
                Schema.Key.API_ID,
                mCurrentRequestTelemetry.getTelemetryFieldValue(Schema.Key.API_ID)
        );

        mLastRequestTelemetry.putTelemetry(
                Schema.Key.SCENARIO_ID,
                mCurrentRequestTelemetry.getTelemetryFieldValue(Schema.Key.SCENARIO_ID)
        );

        mLastRequestTelemetry.putTelemetry(
                Schema.Key.CORRELATION_ID,
                correlationId
        );
    }

    public static void completeScenario(String correlationId, String errorCode) {
        ServerTelemetry.clearLastRequestTelemetry();
        ServerTelemetry.setupLastFromCurrent(correlationId);
        ServerTelemetry.putLastErrorCode(errorCode);
        mLastRequestTelemetryCache.saveRequestTelemetryToCache(mLastRequestTelemetry);
        clearCurrentRequestTelemetry();
    }

    public static String getCurrentTelemetryHeaderString() {
        return mCurrentRequestTelemetry.getCompleteTelemetryHeaderString();
    }

    public static String getLastTelemetryHeaderString() {
        return mLastRequestTelemetry.getCompleteTelemetryHeaderString();
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
