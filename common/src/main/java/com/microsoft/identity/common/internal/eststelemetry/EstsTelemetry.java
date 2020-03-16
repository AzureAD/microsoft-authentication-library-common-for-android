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
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.internal.controllers.BaseCommand;
import com.microsoft.identity.common.internal.controllers.CommandResult;
import com.microsoft.identity.common.internal.controllers.TokenCommand;
import com.microsoft.identity.common.internal.logging.DiagnosticContext;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.result.ILocalAuthenticationResult;

import java.util.Collections;
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
    private Map<String, CurrentRequestTelemetry> mTelemetryMap;

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

    private void setupLastRequestTelemetryCache(@NonNull final Context context) {
        this.mLastRequestTelemetryCache = createLastRequestTelemetryCache(context);

        if (this.mLastRequestTelemetryCache != null) {
            com.microsoft.identity.common.internal.logging.Logger.verbose(
                    TAG,
                    "Ests Telemetry cache has been initialized properly."
            );
        }
    }

    public void initTelemetryForCommand(@NonNull final BaseCommand command) {
        setupLastRequestTelemetryCache(command.getParameters().getAppContext());
        final String correlationId = command.getParameters().getCorrelationId();
        if (command.isEligibleForEstsTelemetry()) {
            final CurrentRequestTelemetry currentRequestTelemetry = new CurrentRequestTelemetry();
            mTelemetryMap.put(correlationId, currentRequestTelemetry);
        }
    }

    private boolean isDisabled() {
        final String correlationId = DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID);
        final CurrentRequestTelemetry currentRequestTelemetry = getCurrentTelemetryInstance(correlationId);
        return currentRequestTelemetry == null;
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
        CurrentRequestTelemetry currentTelemetryInstance = getCurrentTelemetryInstance(correlationId);
        if (currentTelemetryInstance != null) {
            currentTelemetryInstance.put(key, value);
        }
    }

    @Nullable
    private CurrentRequestTelemetry getCurrentTelemetryInstance(@Nullable final String correlationId) {
        if (mTelemetryMap == null || correlationId == null || correlationId.equals("UNSET")) {
            return null;
        }

        return mTelemetryMap.get(correlationId);
    }

    @Nullable
    private LastRequestTelemetry loadLastRequestTelemetryFromCache() {
        final String methodName = ":loadLastRequestTelemetry";

        if (mLastRequestTelemetryCache == null) {
            Logger.verbose(
                    TAG + methodName,
                    "Last Request Telemetry Cache has not been initialized. " +
                            "Cannot load Last Request Telemetry data from cache."
            );
            return null;
        }

        return (LastRequestTelemetry) mLastRequestTelemetryCache.getRequestTelemetryFromCache();
    }


    /**
     * Emit the ApiId for the current request. The field will be saved in {@link RequestTelemetry}
     * object associated to the current request.
     *
     * @param apiId the api id to emit to telemetry
     */
    public void emitApiId(final String apiId) {
        emit(SchemaConstants.Key.API_ID, apiId);
    }

    /**
     * Emit the forceRefresh value for the current request. The field will be saved in
     * {@link RequestTelemetry} object associated to the current request.
     *
     * @param forceRefresh the force refresh value to emit to telemetry
     */
    public void emitForceRefresh(final boolean forceRefresh) {
        String val = TelemetryUtils.getSchemaCompliantStringFromBoolean(forceRefresh);
        emit(SchemaConstants.Key.FORCE_REFRESH, val);
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

    /**
     * Flush the telemetry data for the current request to the {@link android.content.SharedPreferences} using the {@link SharedPreferencesLastRequestTelemetryCache}.
     * Removes the telemetry associated to the correlation id from the telemetry map,
     * and saves it to the cache (SharedPreferences) as the last request telemetry.
     */
    public void flush(@NonNull final BaseCommand command, @NonNull final CommandResult commandResult) {
        final String methodName = ":flush";

        final String correlationId = command.getParameters().getCorrelationId();

        if (mTelemetryMap == null || correlationId == null) {
            return;
        }

        CurrentRequestTelemetry currentTelemetry = mTelemetryMap.get(correlationId);

        if (currentTelemetry == null) {
            return;
        }

        LastRequestTelemetry lastRequestTelemetry = loadLastRequestTelemetryFromCache();

        if (lastRequestTelemetry == null) {
            lastRequestTelemetry = new LastRequestTelemetry(currentTelemetry.getSchemaVersion());
            lastRequestTelemetry = (LastRequestTelemetry) lastRequestTelemetry.derive(currentTelemetry);
        }

        final String errorCode = getErrorFromCommandResult(commandResult);

        if (isTelemetryLoggedByServer(command, commandResult)) {
            lastRequestTelemetry.wipeFailedRequestData();
            lastRequestTelemetry.resetSilentSuccessCount();
        }

        if (errorCode != null) {
            lastRequestTelemetry.appendFailedRequestWithError(
                    currentTelemetry.getApiId(),
                    correlationId,
                    errorCode);
        } else if (command instanceof TokenCommand) {
            final ILocalAuthenticationResult localAuthenticationResult =
                    (ILocalAuthenticationResult) commandResult.getResult();

            final Boolean isTokenReturnedFromCache = localAuthenticationResult.isServicedFromCache();
            if (isTokenReturnedFromCache != null && isTokenReturnedFromCache) {
                lastRequestTelemetry.incrementSilentSuccessCount();
            }
        } // else leave everything as is

        mTelemetryMap.remove(correlationId);

        if (mLastRequestTelemetryCache != null) {
            mLastRequestTelemetryCache.saveRequestTelemetryToCache(lastRequestTelemetry);
        } else {
            Logger.warn(
                    TAG + methodName,
                    "Last Request Telemetry Cache object was null. " +
                            "Unable to save request telemetry to cache."
            );
        }
    }

    @Nullable
    private String getErrorFromCommandResult(final CommandResult commandResult) {
        if (commandResult.getStatus() == CommandResult.ResultStatus.ERROR) {
            final BaseException baseException = (BaseException) commandResult.getResult();
            return baseException.getErrorCode();
        } else if (commandResult.getStatus() == CommandResult.ResultStatus.CANCEL) {
            return "user_cancel";
        } else {
            return null;
        }
    }

    private boolean isTelemetryLoggedByServer(@NonNull final BaseCommand command, @NonNull final CommandResult commandResult) {
        // This was a local operation - we didn't reach token endpoint and hence telemetry wasn't sent
        if (!(command instanceof TokenCommand)) {
            return false;
        }

        if (commandResult.getStatus() == CommandResult.ResultStatus.ERROR) {
            BaseException baseException = (BaseException) commandResult.getResult();
            if (!(baseException instanceof ServiceException)) {
                // Telemetry not logged by server
                return false;
            } else {
                final ServiceException serviceException = (ServiceException) baseException;
                final int statusCode = serviceException.getHttpStatusCode();
                if (
                        statusCode == ServiceException.DEFAULT_STATUS_CODE ||
                                statusCode == 429 ||
                                statusCode >= 500
                ) {
                    // For these status codes, headers aren't logged by sts
                    return false;
                }
            }
        } else if (commandResult.getStatus() == CommandResult.ResultStatus.CANCEL) {
            return false;
        } else if (commandResult.getStatus() == CommandResult.ResultStatus.COMPLETED) {
            if (commandResult.getResult() instanceof ILocalAuthenticationResult) {
                final ILocalAuthenticationResult localAuthenticationResult = (ILocalAuthenticationResult) commandResult.getResult();
                if (localAuthenticationResult.isServicedFromCache()) {
                    return false;
                }
            } else {
                // command probably wasn't a token command - we should never get here in that case
                return false;
            }
        }

        return true;
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

        return currentTelemetry.getCompleteHeaderString();
    }

    String getLastTelemetryHeaderString() {
        if (mLastRequestTelemetryCache == null) {
            return null;
        }

        final String lastTelemetryHeaderFromCache = mLastRequestTelemetryCache.getTelemetryHeaderStringFromCache();

        if (lastTelemetryHeaderFromCache != null) {
            return lastTelemetryHeaderFromCache;
        } else {
            final String correlationId = DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID);
            final CurrentRequestTelemetry currentRequestTelemetry = mTelemetryMap.get(correlationId);
            LastRequestTelemetry lastRequestTelemetry = new LastRequestTelemetry(currentRequestTelemetry.getSchemaVersion());
            lastRequestTelemetry = (LastRequestTelemetry) lastRequestTelemetry.derive(currentRequestTelemetry);
            return lastRequestTelemetry.getCompleteHeaderString();
        }

    }

    /**
     * Get the headers for the Ests Telemetry. These headers can be attached to the requests made to the ests.
     *
     * @return a map containing telemetry headers and their values
     */
    public Map<String, String> getTelemetryHeaders() {
        final String methodName = ":getTelemetryHeaders";

        final Map<String, String> headerMap = new HashMap<>();

        if (isDisabled()) {
            return headerMap;
        }

        final String currentHeader = getCurrentTelemetryHeaderString();
        final String lastHeader = getLastTelemetryHeaderString();

        if (currentHeader != null) {
            headerMap.put(SchemaConstants.CURRENT_REQUEST_HEADER_NAME, currentHeader);
        } else {
            Logger.verbose(
                    TAG + methodName,
                    "Current Request Telemetry Header is null"
            );
        }

        if (lastHeader != null) {
            headerMap.put(SchemaConstants.LAST_REQUEST_HEADER_NAME, lastHeader);
        } else {
            Logger.verbose(
                    TAG + methodName,
                    "Last Request Telemetry Header is null"
            );
        }

        return Collections.unmodifiableMap(headerMap);
    }

}
