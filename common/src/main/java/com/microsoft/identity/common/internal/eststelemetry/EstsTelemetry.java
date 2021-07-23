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

import com.microsoft.identity.common.SharedPreferenceStringStorage;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.internal.commands.BaseCommand;
import com.microsoft.identity.common.internal.controllers.CommandResult;
import com.microsoft.identity.common.java.eststelemetry.LastRequestTelemetryCache;
import com.microsoft.identity.common.java.eststelemetry.RequestTelemetry;
import com.microsoft.identity.common.logging.Logger;

import java.util.Map;

/**
 * Deprecated.
 *
 * Currently serving as an adapter of {@link com.microsoft.identity.common.java.eststelemetry.EstsTelemetry}
 * */
public class EstsTelemetry {

    private static final String TAG = EstsTelemetry.class.getSimpleName();

    /**
     * The name of the SharedPreferences file on disk for the last request telemetry.
     */
    private static final String LAST_REQUEST_TELEMETRY_SHARED_PREFERENCES =
            "com.microsoft.identity.client.last_request_telemetry";

    private static volatile EstsTelemetry sInstance;

    private final com.microsoft.identity.common.java.eststelemetry.EstsTelemetry mAdaptedInstance;

    private EstsTelemetry(@NonNull final com.microsoft.identity.common.java.eststelemetry.EstsTelemetry adaptedInstance) {
        mAdaptedInstance = adaptedInstance;
    }

    /**
     * Get an instance of {@link EstsTelemetry}. This method will return an existing
     * instance of EstsTelemetry or create and return a new instance if the existing instance is null.
     *
     * @return EstsTelemetry object instance
     */
    public static synchronized EstsTelemetry getInstance() {
        if (sInstance == null) {
            final com.microsoft.identity.common.java.eststelemetry.EstsTelemetry defaultInstance =
                    com.microsoft.identity.common.java.eststelemetry.EstsTelemetry.getInstance();

            sInstance = new EstsTelemetry(defaultInstance);
        }
        return sInstance;
    }

    /**
     * Creates an entry for a Current Telemetry object for the passed in command based on whether
     * the command is eligible for telemetry. Saves the telemetry object to telemetry map.
     *
     * @param command The command for which to capture telemetry
     */
    public void initTelemetryForCommand(@NonNull final BaseCommand<?> command) {
        mAdaptedInstance.setUp(createLastRequestTelemetryCacheOnAndroid(command.getParameters().getAndroidApplicationContext()));
        mAdaptedInstance.initTelemetryForCommand(command);
    }

    @Nullable
    public static LastRequestTelemetryCache createLastRequestTelemetryCacheOnAndroid(@Nullable final Context context) {
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

        return new LastRequestTelemetryCache(
                new SharedPreferenceStringStorage(context, LAST_REQUEST_TELEMETRY_SHARED_PREFERENCES));
    }

    /**
     * Emit multiple telemetry fields by passing a map of telemetry fields.
     * The fields will be saved in {@link RequestTelemetry} object associated to the current request.
     *
     * @param telemetry a map containing telemetry fields and their values
     */
    public void emit(@Nullable final Map<String, String> telemetry) {
        mAdaptedInstance.emit(telemetry);
    }

    /**
     * Emit the provided telemetry field. The field will be saved in {@link RequestTelemetry} object
     * associated to the current request.
     *
     * @param key   the key associated to the telemetry field
     * @param value the value associated to the telemetry field
     */
    public void emit(final String key, final String value) {
        mAdaptedInstance.emit(key, value);
    }

    /**
     * Emit the ApiId for the current request. The field will be saved in {@link RequestTelemetry}
     * object associated to the current request.
     *
     * @param apiId the api id to emit to telemetry
     */
    public void emitApiId(final String apiId) {
        mAdaptedInstance.emitApiId(apiId);
    }

    /**
     * Emit the forceRefresh value for the current request. The field will be saved in
     * {@link RequestTelemetry} object associated to the current request.
     *
     * @param forceRefresh the force refresh value to emit to telemetry
     */
    public void emitForceRefresh(final boolean forceRefresh) {
        mAdaptedInstance.emitForceRefresh(forceRefresh);
    }

    /**
     * Flush the telemetry data for the current request to the {@link android.content.SharedPreferences} using the {@link LastRequestTelemetryCache}.
     * Removes the telemetry associated to the correlation id from the telemetry map,
     * and saves it to the cache (SharedPreferences) as the last request telemetry.
     */
    public synchronized void flush(@SuppressWarnings(WarningType.rawtype_warning) @NonNull final BaseCommand command, @NonNull final CommandResult commandResult) {
        mAdaptedInstance.flush(command, commandResult);
    }

    /**
     * Get the headers for the Ests Telemetry. These headers can be attached to the requests made to the ests.
     *
     * @return a map containing telemetry headers and their values
     */
    public Map<String, String> getTelemetryHeaders() {
        return mAdaptedInstance.getTelemetryHeaders();
    }

}
