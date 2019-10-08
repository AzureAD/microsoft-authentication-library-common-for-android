// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.internal.eststelemetry;

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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class EstsTelemetry {
    private final static String TAG = EstsTelemetry.class.getSimpleName();

    /**
     * The name of the SharedPreferences file on disk for the last request.
     */
    private static final String LAST_REQUEST_TELEMETRY_SHARED_PREFERENCES =
            "com.microsoft.identity.client.last_request_telemetry";

    private static volatile EstsTelemetry sEstsTelemetryInstance = null;
    private static IRequestTelemetryCache sLastRequestTelemetryCache;
    private Map<String, RequestTelemetry> sTelemetryMap;

    private EstsTelemetry() {
        sTelemetryMap = new ConcurrentHashMap<>();
    }

    private EstsTelemetry(@NonNull final Context context) {
        this();
        sLastRequestTelemetryCache = createLastRequestTelemetryCache(context);
    }

    /**
     * Creates and returns an instance of EstsTelemetry class
     **/
    private synchronized static EstsTelemetry prepareInstance(@Nullable final Context context) {
        sEstsTelemetryInstance = context == null ? new EstsTelemetry() : new EstsTelemetry(context);
        return sEstsTelemetryInstance;
    }

    /**
     * Get an instance of EstsTelemetry by passing a context. This method will return an instance
     * that has both the telemetry map initialized however the cache might not be initialized.
     * @return EstsTelemetry object
     */
    public static synchronized EstsTelemetry getInstance() {
        if (sEstsTelemetryInstance != null) {
            return sEstsTelemetryInstance;
        } else {
            return prepareInstance(null);
        }
    }

    /**
     * Get an instance of EstsTelemetry by passing a context. This method will always return an instance
     * that has both the telemetry map and the cache initialized.
     * @return EstsTelemetry object
     */
    public static synchronized EstsTelemetry getInstance(@NonNull final Context context) {
        if (sEstsTelemetryInstance != null) {
            return sEstsTelemetryInstance;
        } else {
            return prepareInstance(context);
        }
    }

    /**
     * Initialize the Ests Telemetry Cache for last request
     * @param context the application context
     */
    public static void initializeEstsTelemetryCache(@NonNull final Context context) {
        final String methodName = ":initializeEstsTelemetryCache";

        Logger.verbose(
                TAG + methodName,
                "Initializing ests telemetry cache"
        );

        sLastRequestTelemetryCache = createLastRequestTelemetryCache(context);
    }

    /**
     * Emit multiple telemetry fields by passing a map of telemetry fields
     * @param telemetry a map containing telemetry fields and their values
     */
    public void emit(@Nullable final Map<String, String> telemetry) {
        if (telemetry == null) {
            return;
        }

        for (Map.Entry<String, String> entry : telemetry.entrySet()) {
            emit(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Emit the provided telemetry field
     * @param key the key associated to the telemetry field
     * @param value the value associated to the telemetry field
     */
    public void emit(final String key, final String value) {
        final String correlationId = DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID);
        emit(correlationId, key, value);
    }

    private void emit(final String correlationId, final String key, final String value) {
        RequestTelemetry currentTelemetryInstance = getCurrentTelemetryInstance(correlationId);
        if (currentTelemetryInstance != null) {
            currentTelemetryInstance.putTelemetry(key, value);
        }
    }

    private RequestTelemetry getCurrentTelemetryInstance(@Nullable final String correlationId) {
        if (sTelemetryMap == null || correlationId == null || correlationId.equals("UNSET")) {
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

    @Nullable
    private RequestTelemetry loadLastRequestTelemetryFromCache() {
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


    public void emitApiId(final String apiId) {
        emit(Schema.Key.API_ID, apiId);
    }

    public void emitForceRefresh(final boolean forceRefresh) {
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

    private RequestTelemetry setupLastFromCurrent(@Nullable RequestTelemetry currentTelemetry) {
        if (currentTelemetry == null) {
            return new RequestTelemetry(Schema.CURRENT_SCHEMA_VERSION, false);
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

    public void flush() {
        String correlationId = DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID);
        flush(correlationId);
    }

    public void flush(final String correlationId) {
        flush(correlationId, (String) null);
    }

    public void flush(final BaseException baseException) {
        String correlationId = DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID);
        flush(correlationId, baseException);
    }

    public void flush(final String correlationId, final BaseException baseException) {
        flush(correlationId, baseException == null ? null : baseException.getErrorCode());
    }

    public void flush(final String correlationId, final AcquireTokenResult acquireTokenResult) {
        final String errorCode = TelemetryUtils.errorFromAcquireTokenResult(acquireTokenResult);
        flush(correlationId, errorCode);
    }

    /**
     * Flush the telemetry data associated to the correlation id.
     * Remove the telemetry associated to the correlation id from the telemetry map,
     * and saves it to the cache as the last request telemetry.
     * @param correlationId correlation id of the request
     * @param errorCode error that may have occurred during the request
     */
    public void flush(final String correlationId, final String errorCode) {
        final String methodName = ":flush";
        if (sTelemetryMap == null || correlationId == null) {
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

    String getCurrentTelemetryHeaderString() {
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

    String getLastTelemetryHeaderString() {
        RequestTelemetry lastTelemetry = loadLastRequestTelemetryFromCache();

        if (lastTelemetry == null) {
            return null;
        }

        return lastTelemetry.getCompleteTelemetryHeaderString();
    }

    /**
     * Get the headers for the Ests Telemetry. These headers can be attached to the requests made to the ests.
     * @return a map containing telemetry headers and their values
     */
    public Map<String, String> getTelemetryHeaders() {
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
