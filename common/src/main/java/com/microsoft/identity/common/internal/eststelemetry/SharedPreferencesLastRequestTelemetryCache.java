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
import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;
import com.microsoft.identity.common.internal.dto.IAccountRecord;
import com.microsoft.identity.common.internal.logging.Logger;

import java.util.Locale;

import static com.microsoft.identity.common.internal.cache.CacheKeyValueDelegate.CACHE_VALUE_SEPARATOR;
import static com.microsoft.identity.common.internal.eststelemetry.SharedPreferencesLastRequestTelemetryCache.CacheKeyReplacements.ENVIRONMENT;
import static com.microsoft.identity.common.internal.eststelemetry.SharedPreferencesLastRequestTelemetryCache.CacheKeyReplacements.HOME_ACCOUNT_ID;
import static com.microsoft.identity.common.internal.eststelemetry.SharedPreferencesLastRequestTelemetryCache.CacheKeyReplacements.REALM;

public class SharedPreferencesLastRequestTelemetryCache implements IRequestTelemetryCache {

    private final static String CACHE_KEY = "last_telemetry";

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

        final String cacheValue = mSharedPreferencesFileManager.getString(CACHE_KEY);

        LastRequestTelemetry lastRequestTelemetry = mGson.fromJson(cacheValue, LastRequestTelemetry.class);

        if (lastRequestTelemetry == null) {
            Logger.warn(TAG + methodName,
                    "Last Request Telemetry deserialization failed");
        }

        return lastRequestTelemetry;
    }

    @Override
    public synchronized void saveRequestTelemetryToCache(@NonNull final RequestTelemetry requestTelemetry) {
        Logger.verbose(TAG, "Saving Request Telemetry to cache...");

        final String cacheValue = generateCacheValue(requestTelemetry);
        mSharedPreferencesFileManager.putString(CACHE_KEY, cacheValue);
    }

//    private synchronized void saveTelemetryDataToCache(@NonNull final Map<String, String> data) {
//        for (Map.Entry<String, String> entry : data.entrySet()) {
//            final String cacheKey = entry.getKey();
//            final String cacheValue = entry.getValue();
//            if (!TextUtils.isEmpty(cacheKey) && !TextUtils.isEmpty(cacheValue)) {
//                mSharedPreferencesFileManager.putString(cacheKey, cacheValue);
//            }
//        }
//    }

    @Override
    public synchronized void clearRequestTelemetry() {
        Logger.info(TAG, "Removing telemetry for account from cache...");
        mSharedPreferencesFileManager.remove(CACHE_KEY);
    }

    private String generateCacheValue(final RequestTelemetry requestTelemetry) {
        JsonElement outboundElement = mGson.toJsonTree(requestTelemetry);
        JsonObject outboundObject = outboundElement.getAsJsonObject();

        final String json = mGson.toJson(outboundObject);

        return json;
    }

    public String generateCacheKey(IAccountRecord account) {
        String cacheKey = HOME_ACCOUNT_ID
                + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT
                + CACHE_VALUE_SEPARATOR
                + REALM;
        cacheKey = cacheKey.replace(HOME_ACCOUNT_ID, sanitizeNull(account.getHomeAccountId()));
        cacheKey = cacheKey.replace(ENVIRONMENT, sanitizeNull(account.getEnvironment()));
        cacheKey = cacheKey.replace(REALM, sanitizeNull(account.getRealm()));

        return cacheKey;
    }

    static class CacheKeyReplacements {
        static final String HOME_ACCOUNT_ID = "<home_account_id>";
        static final String ENVIRONMENT = "<environment>";
        static final String REALM = "<realm>";
        static final String CREDENTIAL_TYPE = "<credential_type>";
        static final String CLIENT_ID = "<client_id>";
        static final String TARGET = "<target>";
    }

    private static String sanitizeNull(final String input) {
        String outValue = null == input ? "" : input.toLowerCase(Locale.US).trim();

        return outValue;
    }


}
