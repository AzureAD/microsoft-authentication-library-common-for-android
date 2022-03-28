//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.internal.cache;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.microsoft.identity.common.java.cache.IListTypeToken;
import com.microsoft.identity.common.java.cache.ISimpleCache;
import com.microsoft.identity.common.logging.Logger;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A simple metadata store definition that uses SharedPreferences to persist, read, update, and
 * delete data.
 * <p>
 * Data serializes as JSON.
 *
 * @param <T> The type of metadata that will be persisted.
 */
public abstract class SharedPreferencesSimpleCacheImpl<T> implements ISimpleCache<T>, IListTypeToken {

    private static final String TAG = SharedPreferencesSimpleCacheImpl.class.getSimpleName();

    private static final String EMPTY_ARRAY = "[]";

    private final SharedPreferences mSharedPrefs;
    private final String mKeySingleEntry;
    private final Gson mGson = new Gson();

    public SharedPreferencesSimpleCacheImpl(@NonNull final Context context,
                                            @NonNull final String prefsName,
                                            @NonNull final String singleKey) {
        Logger.verbose(
                TAG + "::constructor",
                "Init"
        );
        mSharedPrefs = context.getSharedPreferences(
                prefsName,
                Context.MODE_PRIVATE
        );
        mKeySingleEntry = singleKey;
    }

    @Override
    public boolean insert(T t) {
        final String methodTag = TAG + ":insert";

        final Set<T> allMetadata = new HashSet<>(getAll());
        Logger.verbose(
                methodTag,
                "Existing metadata contained ["
                        + allMetadata.size()
                        + "] elements."
        );

        allMetadata.add(t);

        Logger.verbose(
                methodTag,
                "New metadata set size: ["
                        + allMetadata.size()
                        + "]"
        );

        final String json = mGson.toJson(allMetadata);

        Logger.verbose(
                methodTag,
                "Writing cache entry."
        );

        final boolean success = mSharedPrefs.edit().putString(mKeySingleEntry, json).commit();

        if (success) {
            Logger.verbose(
                    methodTag,
                    "Cache successfully updated."
            );
        } else {
            Logger.warn(
                    methodTag,
                    "Error writing to cache."
            );
        }

        return success;
    }

    @Override
    public boolean remove(T t) {
        final String methodTag = TAG + ":remove";

        final Set<T> allMetadata = new HashSet<>(getAll());

        Logger.verbose(
                methodTag,
                "Existing metadata contained ["
                        + allMetadata.size()
                        + "] elements."
        );

        final boolean removed = allMetadata.remove(t);

        Logger.verbose(
                methodTag,
                "New metadata set size: ["
                        + allMetadata.size()
                        + "]"
        );

        if (!removed) {
            // Nothing to do, wasn't cached in the first place!
            Logger.warn(
                    methodTag,
                    "Nothing to delete -- cache entry is missing!"
            );

            return true;
        } else {
            final String json = mGson.toJson(allMetadata);

            Logger.verbose(
                    methodTag,
                    "Writing new cache values..."
            );

            final boolean written = mSharedPrefs.edit().putString(mKeySingleEntry, json).commit();

            Logger.verbose(
                    methodTag,
                    "Updated cache contents written? ["
                            + written
                            + "]"
            );

            return written;
        }
    }

    @Override
    public List<T> getAll() {
        final String methodTag = TAG + ":getAll";
        final String jsonList = mSharedPrefs.getString(mKeySingleEntry, EMPTY_ARRAY);

        final Type listType = getListTypeToken();

        final List<T> result = mGson.fromJson(
                jsonList,
                listType
        );

        Logger.verbose(
                methodTag,
                "Found ["
                        + result.size()
                        + "] cache entries."
        );

        return result;
    }

    @Override
    public boolean clear() {
        final String methodTag = TAG + ":clear";

        final boolean cleared = mSharedPrefs.edit().clear().commit();

        if (!cleared) {
            Logger.warn(
                    methodTag,
                    "Failed to clear cache."
            );
        } else {
            Logger.verbose(
                    methodTag,
                    "Cache successfully cleared."
            );
        }

        return cleared;
    }
}
