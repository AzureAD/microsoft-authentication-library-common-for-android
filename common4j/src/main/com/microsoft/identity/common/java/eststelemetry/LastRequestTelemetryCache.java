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
package com.microsoft.identity.common.java.eststelemetry;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.java.logging.Logger;

import lombok.NonNull;

public class LastRequestTelemetryCache implements IRequestTelemetryCache<LastRequestTelemetry> {

    final static String LAST_TELEMETRY_OBJECT_CACHE_KEY = "last_telemetry_object";
    final static String LAST_TELEMETRY_HEADER_STRING_CACHE_KEY = "last_telemetry_header_string";
    final static String LAST_TELEMETRY_SCHEMA_VERSION_CACHE_KEY = "last_telemetry_schema_version";

    private final static String TAG = LastRequestTelemetryCache.class.getSimpleName();

    private static final Gson mGson = new Gson();

    // Storage for request telemetry data
    private final INameValueStorage<String> mStorage;

    /**
     * Constructor of LastRequestTelemetryCache.
     *
     * @param keyPairStorage INameValueStorage
     */
    public LastRequestTelemetryCache(@NonNull final INameValueStorage<String> keyPairStorage) {
        Logger.verbose(TAG, "Init: " + TAG);
        mStorage = keyPairStorage;
    }

    @Override
    public synchronized LastRequestTelemetry getRequestTelemetryFromCache() {
        final String methodName = ":getRequestTelemetryFromCache";

        try {
            final String cacheValue = mStorage.get(LAST_TELEMETRY_OBJECT_CACHE_KEY);

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
            mStorage.clear();
            throw e;
        }
    }

    public void clear() {
        mStorage.clear();
    }

    @Override
    public synchronized void saveRequestTelemetryToCache(@NonNull final LastRequestTelemetry requestTelemetry) {
        Logger.verbose(TAG, "Saving Last Request Telemetry to cache...");

        saveRequestTelemetryObjectToCache(requestTelemetry);
        saveTelemetryHeaderStringToCache(requestTelemetry);
        saveTelemetrySchemaVersionToCache(requestTelemetry);
    }

    private void saveRequestTelemetryObjectToCache(@NonNull final LastRequestTelemetry requestTelemetry) {
        final String cacheValue = generateCacheValue(requestTelemetry);
        saveToTelemetryCache(LAST_TELEMETRY_OBJECT_CACHE_KEY, cacheValue);
    }

    private void saveTelemetryHeaderStringToCache(@NonNull final LastRequestTelemetry requestTelemetry) {
        final String cacheValue = requestTelemetry.getCompleteHeaderString();
        saveToTelemetryCache(LAST_TELEMETRY_HEADER_STRING_CACHE_KEY, cacheValue);
    }

    private void saveTelemetrySchemaVersionToCache(@NonNull final LastRequestTelemetry requestTelemetry) {
        final String cacheValue = requestTelemetry.getSchemaVersion();
        saveToTelemetryCache(LAST_TELEMETRY_SCHEMA_VERSION_CACHE_KEY, cacheValue);
    }

    private void saveToTelemetryCache(@NonNull final String cacheKey, @NonNull final String cacheValue) {
        mStorage.put(cacheKey, cacheValue);
    }

    private String generateCacheValue(final LastRequestTelemetry requestTelemetry) {
        JsonElement outboundElement = mGson.toJsonTree(requestTelemetry);
        JsonObject outboundObject = outboundElement.getAsJsonObject();
        return mGson.toJson(outboundObject);
    }
}
