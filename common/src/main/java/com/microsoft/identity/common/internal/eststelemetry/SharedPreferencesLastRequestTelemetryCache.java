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

import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;
import com.microsoft.identity.common.internal.logging.Logger;

import java.util.Map;

public class SharedPreferencesLastRequestTelemetryCache extends SharedPreferencesRequestTelemetryCache {

    private final static String TAG = SharedPreferencesLastRequestTelemetryCache.class.getSimpleName();

    /**
     * Constructor of SharedPreferencesLastRequestTelemetryCache.
     *
     * @param sharedPreferencesFileManager ISharedPreferencesFileManager
     */
    SharedPreferencesLastRequestTelemetryCache(
            @NonNull final ISharedPreferencesFileManager sharedPreferencesFileManager) {
        super(sharedPreferencesFileManager);
    }

    @Override
    public synchronized RequestTelemetry getRequestTelemetryFromCache() {
        final String methodName = ":getRequestTelemetryFromCache";

        final Map<String, String> data = super.getSharedPreferencesFileManager().getAll();

        if (data == null || data.isEmpty()) {
            Logger.verbose(TAG + methodName,
                    "Last Request telemetry not found in cache. " +
                            "Returning an empty RequestTelemetry object."
            );

            return new RequestTelemetry(null, false);
        }

        final String schemaVersion = data.get(Schema.Key.SCHEMA_VERSION);
        final RequestTelemetry lastRequestTelemetry = new RequestTelemetry(schemaVersion, false);

        final String[] lastCommonFields = Schema.getCommonFields(false);
        final String[] lastPlatformFields = Schema.getPlatformFields(false);

        for (String key : lastCommonFields) {
            lastRequestTelemetry.putTelemetry(key, data.get(key));
        }

        for (String key : lastPlatformFields) {
            lastRequestTelemetry.putTelemetry(key, data.get(key));
        }

        return lastRequestTelemetry;
    }
}
