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
package com.microsoft.identity.common.internal.cache;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.identity.common.internal.logging.Logger;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SharedPreferencesBrokerApplicationMetadataCache
        implements IBrokerApplicationMetadataCache {

    private static final String TAG = SharedPreferencesBrokerApplicationMetadataCache.class.getSimpleName();

    private static final String DEFAULT_APP_METADATA_CACHE_NAME = "com.microsoft.identity.app-meta-cache";

    private static final String KEY_CACHE_LIST = "app-meta-cache";
    private static final String EMPTY_ARRAY = "[]";

    private final SharedPreferences mSharedPrefs;

    private final Gson mGson = new Gson();

    public SharedPreferencesBrokerApplicationMetadataCache(@NonNull final Context context) {
        Logger.verbose(
                TAG + "::ctor",
                "Init"
        );
        mSharedPrefs = context.getSharedPreferences(
                DEFAULT_APP_METADATA_CACHE_NAME,
                Context.MODE_PRIVATE
        );
    }

    @Override
    public Set<String> getAllClientIds() {
        final String methodName = ":getAllClientIds";

        final Set<String> allClientIds = new HashSet<>();

        for (final BrokerApplicationMetadata metadata : getAll()) {
            allClientIds.add(metadata.getClientId());
        }

        Logger.verbose(
                TAG + methodName,
                "Found ["
                        + allClientIds.size()
                        + "] client ids."
        );

        return allClientIds;
    }

    @Nullable
    @Override
    public BrokerApplicationMetadata getMetadata(@NonNull final String clientId,
                                                 @NonNull final String environment) {
        final String methodName = ":getMetadata";

        final List<BrokerApplicationMetadata> allMetadata = getAll();
        BrokerApplicationMetadata result = null;

        for (final BrokerApplicationMetadata metadata : allMetadata) {
            if (clientId.equals(metadata.getClientId())
                    && environment.equals(metadata.getEnvironment())) {
                Logger.verbose(
                        TAG + metadata,
                        "Metadata located."
                );

                result = metadata;
                break;
            }
        }

        if (null == result) {
            Logger.warn(
                    TAG + methodName,
                    "Metadata could not be found for clientId, environment: ["
                            + clientId
                            + ", "
                            + environment
                            + "]"
            );
        }

        return result;
    }

    @Nullable
    @Override
    public synchronized Integer getUidForApp(@NonNull final String clientId,
                                             @NonNull final String environment) {
        final BrokerApplicationMetadata applicationMetadata = getMetadata(clientId, environment);

        return null == applicationMetadata ? null : applicationMetadata.getUid();
    }

    @Nullable
    @Override
    public String getFamilyId(@NonNull final String clientId,
                              @NonNull final String environment) {
        final BrokerApplicationMetadata applicationMetadata = getMetadata(clientId, environment);

        return null == applicationMetadata ? null : applicationMetadata.getFoci();
    }

    @Override
    public synchronized boolean insert(@NonNull final BrokerApplicationMetadata metadata) {
        final String methodName = ":insert";

        final Set<BrokerApplicationMetadata> allMetadata = new HashSet<>(getAll());
        Logger.verbose(
                TAG + methodName,
                "Existing metadata contained ["
                        + allMetadata.size()
                        + "] elements."
        );

        allMetadata.add(metadata);

        Logger.verbose(
                TAG + methodName,
                "New metadata set size: ["
                        + allMetadata.size()
                        + "]"
        );

        final String json = mGson.toJson(allMetadata);

        Logger.verbose(
                TAG + methodName,
                "Writing cache entry."
        );

        final boolean success = mSharedPrefs.edit().putString(KEY_CACHE_LIST, json).commit();

        if (success) {
            Logger.verbose(
                    TAG + methodName,
                    "Cache successfully updated."
            );
        } else {
            Logger.warn(
                    TAG + methodName,
                    "Error writing to cache."
            );
        }

        return success;
    }

    @Override
    public synchronized boolean remove(@NonNull final BrokerApplicationMetadata metadata) {
        final String methodName = ":remove";

        final Set<BrokerApplicationMetadata> allMetadata = new HashSet<>(getAll());

        Logger.verbose(
                TAG + methodName,
                "Existing metadata contained ["
                        + allMetadata.size()
                        + "] elements."
        );

        final boolean removed = allMetadata.remove(metadata);

        Logger.verbose(
                TAG + methodName,
                "New metadata set size: ["
                        + allMetadata.size()
                        + "]"
        );

        if (!removed) {
            // Nothing to do, wasn't cached in the first place!
            Logger.warn(
                    TAG + methodName,
                    "Nothing to delete -- cache entry is missing!"
            );

            return true;
        } else {
            final String json = mGson.toJson(allMetadata);

            Logger.verbose(
                    TAG + methodName,
                    "Writing new cache values..."
            );

            final boolean written = mSharedPrefs.edit().putString(KEY_CACHE_LIST, json).commit();

            Logger.verbose(
                    TAG + methodName,
                    "Updated cache contents written? ["
                            + written
                            + "]"
            );

            return written;
        }
    }

    @Override
    public synchronized List<BrokerApplicationMetadata> getAll() {
        final String methodName = ":getAll";
        final String jsonList = mSharedPrefs.getString(KEY_CACHE_LIST, EMPTY_ARRAY);

        final Type listType = new TypeToken<List<BrokerApplicationMetadata>>() {
        }.getType();

        final List<BrokerApplicationMetadata> result = mGson.fromJson(
                jsonList,
                listType
        );

        Logger.verbose(
                TAG + methodName,
                "Found ["
                        + result.size()
                        + "] cache entries."
        );

        return result;
    }

    @Override
    public synchronized boolean clear() {
        final String methodName = ":clear";

        final boolean cleared = mSharedPrefs.edit().clear().commit();

        if (!cleared) {
            Logger.warn(
                    TAG + methodName,
                    "Failed to clear cache."
            );
        } else {
            Logger.verbose(
                    TAG + methodName,
                    "Cache successfully cleared."
            );
        }

        return cleared;
    }
}
