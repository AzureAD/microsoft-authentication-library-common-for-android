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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;
import com.microsoft.identity.common.logging.Logger;

public class SharedPreferencesLastRequestTelemetryCache implements IRequestTelemetryCache {

    private final static String LAST_TELEMETRY_OBJECT_CACHE_KEY = "last_telemetry_object";
    private final static String LAST_TELEMETRY_HEADER_STRING_CACHE_KEY = "last_telemetry_header_string";
    private final static String LAST_TELEMETRY_SCHEMA_VERSION_CACHE_KEY = "last_telemetry_schema_version";

    private final static String TAG = SharedPreferencesLastRequestTelemetryCache.class.getSimpleName();

    private final Gson mGson;

    // SharedPreferences used to store request telemetry data
    private final ISharedPreferencesFileManager mSharedPreferencesFileManager;

    /**
     * Constructor of SharedPreferencesLastRequestTelemetryCache.
     *
     * @param sharedPreferencesFileManager ISharedPreferencesFileManager
     */
    SharedPreferencesLastRequestTelemetryCache(
            @NonNull final ISharedPreferencesFileManager sharedPreferencesFileManager) {
        Logger.verbose(TAG, "Init: " + TAG);
        mSharedPreferencesFileManager = sharedPreferencesFileManager;
        mGson = new Gson();
    }

    ISharedPreferencesFileManager getSharedPreferencesFileManager() {
        return mSharedPreferencesFileManager;
    }

    @Override
    @Nullable
    public synchronized RequestTelemetry getRequestTelemetryFromCache() {
        final String methodName = ":getRequestTelemetryFromCache";

        try {
            final String cacheValue = mSharedPreferencesFileManager.getString(LAST_TELEMETRY_OBJECT_CACHE_KEY);

            if (cacheValue == null) {
                Logger.info(TAG + methodName, "There is no last request telemetry saved in " +
                        "the cache. Returning NULL");

                return null;
            }

            final LastRequestTelemetry lastRequestTelemetry = mGson.fromJson(cacheValue, LastRequestTelemetry.class);

            if (lastRequestTelemetry == null) {
                Logger.warn(TAG + methodName,
                        "Last Request Telemetry deserialization failed");
            }

            return lastRequestTelemetry;
        } catch (final JsonSyntaxException e) {
            Logger.error(TAG + methodName,
                    "Last Request Telemetry deserialization failed", e);
            return null;
        } catch (final OutOfMemoryError e) {
            mSharedPreferencesFileManager.clear();
            throw e;
        }
    }

    @Override
    public synchronized void saveRequestTelemetryToCache(@NonNull final RequestTelemetry requestTelemetry) {
        Logger.verbose(TAG, "Saving Last Request Telemetry to cache...");

        saveRequestTelemetryObjectToCache(requestTelemetry);
        saveTelemetryHeaderStringToCache(requestTelemetry);
        saveTelemetrySchemaVersionToCache(requestTelemetry);
    }

    private void saveRequestTelemetryObjectToCache(@NonNull final RequestTelemetry requestTelemetry) {
        final String cacheKey = LAST_TELEMETRY_OBJECT_CACHE_KEY;
        final String cacheValue = generateCacheValue(requestTelemetry);
        saveToTelemetryCache(cacheKey, cacheValue);
    }

    private void saveTelemetryHeaderStringToCache(@NonNull final RequestTelemetry requestTelemetry) {
        final String cacheKey = LAST_TELEMETRY_HEADER_STRING_CACHE_KEY;
        final String cacheValue = requestTelemetry.getCompleteHeaderString();
        saveToTelemetryCache(cacheKey, cacheValue);
    }

    private void saveTelemetrySchemaVersionToCache(@NonNull final RequestTelemetry requestTelemetry) {
        final String cacheKey = LAST_TELEMETRY_SCHEMA_VERSION_CACHE_KEY;
        final String cacheValue = requestTelemetry.getSchemaVersion();
        saveToTelemetryCache(cacheKey, cacheValue);
    }

    private void saveToTelemetryCache(@NonNull final String cacheKey, @NonNull final String cacheValue) {
        mSharedPreferencesFileManager.putString(cacheKey, cacheValue);
    }

    private String generateCacheValue(final RequestTelemetry requestTelemetry) {
        JsonElement outboundElement = mGson.toJsonTree(requestTelemetry);
        JsonObject outboundObject = outboundElement.getAsJsonObject();

        final String json = mGson.toJson(outboundObject);

        return json;
    }
}
