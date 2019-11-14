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
import android.text.TextUtils;

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

public class EstsTelemetry {
    private final static String TAG = EstsTelemetry.class.getSimpleName();

    /**
     * The name of the SharedPreferences file on disk for the last request telemetry.
     */
    private static final String LAST_REQUEST_TELEMETRY_SHARED_PREFERENCES =
            "com.microsoft.identity.client.last_request_telemetry";

    private static volatile EstsTelemetry sEstsTelemetryInstance = null;
    private IRequestTelemetryCache mLastRequestTelemetryCache;
    private Map<String, RequestTelemetry> mTelemetryMap;

    private EstsTelemetry() {
        mTelemetryMap = new ConcurrentHashMap<>();
    }

    /**
     * Get an instance of {@link EstsTelemetry}. This method will return an existing
     * instance of EstsTelemetry or create and return a new instance if the existing instance is null.
     *
     * @return EstsTelemetry object instance
     */
    public static synchronized EstsTelemetry getInstance() {
        if (sEstsTelemetryInstance == null) {
            sEstsTelemetryInstance = new EstsTelemetry();
        }

        return sEstsTelemetryInstance;
    }

    public void setupLastRequestTelemetryCache(@NonNull final Context context) {
        this.mLastRequestTelemetryCache = createLastRequestTelemetryCache(context);

        if (this.mLastRequestTelemetryCache != null) {
            com.microsoft.identity.common.internal.logging.Logger.verbose(
                    TAG,
                    "Ests Telemetry cache has been initialized properly."
            );
        }
    }

    /**
     * Emit multiple telemetry fields by passing a map of telemetry fields.
     * The fields will be saved in {@link RequestTelemetry} object associated to the current request.
     *
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
     * Emit the provided telemetry field. The field will be saved in {@link RequestTelemetry} object
     * associated to the current request.
     *
     * @param key   the key associated to the telemetry field
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
        if (mTelemetryMap == null || correlationId == null || correlationId.equals("UNSET")) {
            return null;
        }

        RequestTelemetry currentTelemetry = mTelemetryMap.get(correlationId);
        if (currentTelemetry != null) {
            return currentTelemetry;
        } else {
            RequestTelemetry telemetry = new RequestTelemetry(true);
            mTelemetryMap.put(correlationId, telemetry);
            return telemetry;
        }
    }

    @Nullable
    private RequestTelemetry loadLastRequestTelemetryFromCache() {
        final String methodName = ":loadLastRequestTelemetry";

        if (mLastRequestTelemetryCache == null) {
            Logger.verbose(
                    TAG + methodName,
                    "Last Request Telemetry Cache has not been initialized. " +
                            "Cannot load Last Request Telemetry data from cache."
            );
            return null;
        }

        return mLastRequestTelemetryCache.getRequestTelemetryFromCache();
    }


    /**
     * Emit the ApiId for the current request. The field will be saved in {@link RequestTelemetry}
     * object associated to the current request.
     *
     * @param apiId the api id to emit to telemetry
     */
    public void emitApiId(final String apiId) {
        emit(Schema.Key.API_ID, apiId);
    }

    /**
     * Emit the forceRefresh value for the current request. The field will be saved in
     * {@link RequestTelemetry} object associated to the current request.
     *
     * @param forceRefresh the force refresh value to emit to telemetry
     */
    public void emitForceRefresh(final boolean forceRefresh) {
        String val = Schema.getSchemaCompliantStringFromBoolean(forceRefresh);
        emit(Schema.Key.FORCE_REFRESH, val);
    }

    private IRequestTelemetryCache createLastRequestTelemetryCache(@NonNull final Context context) {
        final String methodName = ":createLastRequestTelemetryCache";

        if (context == null) {
            Logger.verbose(
                    TAG + methodName,
                    "Context is NULL. Unable to create last request telemetry cache."
            );
            return null;
        }

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

    /**
     * Flush the telemetry data for the current request to the {@link android.content.SharedPreferences} using the {@link SharedPreferencesLastRequestTelemetryCache}.
     * Removes the telemetry associated to the correlation id from the telemetry map,
     * and saves it to the cache (SharedPreferences) as the last request telemetry.
     */
    void flush() {
        String correlationId = DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID);
        flush(correlationId);
    }

    /**
     * Flush the telemetry data associated to the correlation id to the {@link android.content.SharedPreferences} using the {@link SharedPreferencesLastRequestTelemetryCache}.
     * Removes the telemetry associated to the correlation id from the telemetry map,
     * and saves it to the cache (SharedPreferences) as the last request telemetry.
     *
     * @param correlationId correlation id of the request
     */
    public void flush(final String correlationId) {
        final String errorCode = null; // there was no error
        flush(correlationId, errorCode);
    }

    /**
     * Flush the telemetry data associated to the correlation id to the {@link android.content.SharedPreferences} using the {@link SharedPreferencesLastRequestTelemetryCache}.
     * Removes the telemetry associated to the correlation id from the telemetry map,
     * and saves it to the cache (SharedPreferences) as the last request telemetry.
     *
     * @param correlationId correlation id of the request
     * @param baseException exception that may have occurred during the request
     */
    public void flush(final String correlationId, final BaseException baseException) {
        flush(correlationId, baseException == null ? null : baseException.getErrorCode());
    }

    /**
     * Flush the telemetry data associated to the correlation id to the {@link android.content.SharedPreferences} using the {@link SharedPreferencesLastRequestTelemetryCache}.
     * Removes the telemetry associated to the correlation id from the telemetry map,
     * and saves it to the cache (SharedPreferences) as the last request telemetry.
     *
     * @param correlationId      correlation id of the request
     * @param acquireTokenResult the result obtained from the acquire token call
     */
    public void flush(final String correlationId, final AcquireTokenResult acquireTokenResult) {
        final String errorCode = TelemetryUtils.errorFromAcquireTokenResult(acquireTokenResult);
        flush(correlationId, errorCode);
    }

    /**
     * Flush the telemetry data associated to the correlation id to the {@link android.content.SharedPreferences} using the {@link SharedPreferencesLastRequestTelemetryCache}.
     * Removes the telemetry associated to the correlation id from the telemetry map,
     * and saves it to the cache (SharedPreferences) as the last request telemetry.
     *
     * @param correlationId correlation id of the request
     * @param errorCode     error that may have occurred during the request
     */
    public void flush(final String correlationId, final String errorCode) {
        final String methodName = ":flush";
        if (mTelemetryMap == null || correlationId == null) {
            return;
        }

        RequestTelemetry currentTelemetry = mTelemetryMap.get(correlationId);
        if (currentTelemetry == null) {
            return;
        }

        RequestTelemetry lastTelemetry = setupLastFromCurrent(currentTelemetry);
        lastTelemetry.putTelemetry(Schema.Key.CORRELATION_ID, correlationId);
        lastTelemetry.putTelemetry(Schema.Key.ERROR_CODE, errorCode);

        currentTelemetry.clearTelemetry();
        mTelemetryMap.remove(correlationId);

        if (mLastRequestTelemetryCache == null) {
            Logger.warn(
                    TAG + methodName,
                    "Last Request Telemetry Cache object was null. " +
                            "Unable to save request telemetry to cache."
            );
        } else if (eligibleToCache(lastTelemetry)) {
            // remove old last request telemetry data from cache
            mLastRequestTelemetryCache.clearAll();
            // save new last request telemetry data to cache
            mLastRequestTelemetryCache.saveRequestTelemetryToCache(lastTelemetry);
        }
    }

    // if we don't have api id then we won't save telemetry to cache
    // this can happen for commands like the GetDeviceModeCommand
    // that are generated via a method for which we don't want telemetry
    private boolean eligibleToCache(RequestTelemetry lastTelemetry) {
        return !TextUtils.isEmpty(lastTelemetry.getSchemaVersion()) &&
                !TextUtils.isEmpty(lastTelemetry.getCommonTelemetry().get(Schema.Key.API_ID));
    }

    String getCurrentTelemetryHeaderString() {
        final String correlationId = DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID);
        if (mTelemetryMap == null || correlationId == null) {
            return null;
        }

        RequestTelemetry currentTelemetry = mTelemetryMap.get(correlationId);

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
     *
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
