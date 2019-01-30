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
    public synchronized boolean insert(@NonNull final BrokerApplicationMetadata metadata) {
        final Set<BrokerApplicationMetadata> allMetadata = new HashSet<>(getAll());
        allMetadata.add(metadata);

        final String json = mGson.toJson(allMetadata);

        return mSharedPrefs.edit().putString(KEY_CACHE_LIST, json).commit();
    }

    @Override
    public synchronized boolean remove(@NonNull final BrokerApplicationMetadata metadata) {
        final Set<BrokerApplicationMetadata> allMetadata = new HashSet<>(getAll());
        final boolean removed = allMetadata.remove(metadata);

        if (!removed) {
            // Nothing to do, wasn't cached in the first place!
            return true;
        } else {
            final String json = mGson.toJson(allMetadata);
            return mSharedPrefs.edit().putString(KEY_CACHE_LIST, json).commit();
        }
    }

    @Override
    public synchronized List<BrokerApplicationMetadata> getAll() {
        final String jsonList = mSharedPrefs.getString(KEY_CACHE_LIST, EMPTY_ARRAY);

        final Type listType = new TypeToken<List<BrokerApplicationMetadata>>() {
        }.getType();

        return mGson.fromJson(
                jsonList,
                listType
        );
    }

    @Override
    public synchronized boolean clear() {
        return mSharedPrefs.edit().clear().commit();
    }
}
