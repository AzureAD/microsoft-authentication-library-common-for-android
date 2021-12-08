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
import androidx.annotation.RequiresPermission;

import com.google.gson.Gson;
import com.microsoft.identity.common.logging.Logger;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A simple metadata store definition that uses SharedPreferences to persist, read, update, and
 * delete data.
 * <p>
 * Data serializes as JSON.
 *
 * @param <T> The type of metadata that will be persisted.
 */
public abstract class SharedPreferencesSimpleCacheImpl<T> implements ISimpleCache<T> {

    private static final String TAG = SharedPreferencesSimpleCacheImpl.class.getSimpleName();

    private static final String EMPTY_ARRAY = "[]";

    private final SharedPreferences mSharedPrefs;
    private final String mKeySingleEntry;
    private final Gson mGson = new Gson();

    //ReentrantReadWriteLock - https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/locks/ReentrantReadWriteLock.html
    protected final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    protected final Lock readLock = readWriteLock.readLock();
    protected final Lock writeLock = readWriteLock.writeLock();

    //In memory copy of the cached list
    protected List<T> mList = null;

    protected SharedPreferencesSimpleCacheImpl(@NonNull final Context context,
                                            @NonNull final String prefsName,
                                            @NonNull final String singleKey) {
        Logger.verbose(
                TAG + "::ctor",
                "Init"
        );
        mSharedPrefs = context.getSharedPreferences(
                prefsName,
                Context.MODE_PRIVATE
        );
        mKeySingleEntry = singleKey;
        //Implementations of this class are singletons.... and construction is synchronized
        mList = load();
    }

    /**
     * The List-type token for Gson, used for correctly deserializing JSON stored on disk.
     *
     * @return The List type to which the target JSON should be deserialized.
     */
    protected abstract Type getListTypeToken();

    @Override
    public boolean insert(T t) {
        final String methodName = ":insert";

        boolean success = false;

        writeLock.lock();

        try {

            final Set<T> allMetadata = new HashSet<>(mList);
            Logger.verbose(
                    TAG + methodName,
                    "Existing metadata contained ["
                            + allMetadata.size()
                            + "] elements."
            );

            allMetadata.add(t);

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

            success = mSharedPrefs.edit().putString(mKeySingleEntry, json).commit();
            //Put the updated list in memory
            mList = load();
        }finally {
            writeLock.unlock();
        }

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
    public boolean remove(T t) {
        final String methodName = ":remove";
        boolean removed = false;

        writeLock.lock();

        try {
            final Set<T> allMetadata = new HashSet<>(mList);

            Logger.verbose(
                    TAG + methodName,
                    "Existing metadata contained ["
                            + allMetadata.size()
                            + "] elements."
            );

            removed = allMetadata.remove(t);

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

                removed = true;
            } else {
                final String json = mGson.toJson(allMetadata);

                Logger.verbose(
                        TAG + methodName,
                        "Writing new cache values..."
                );

                removed = mSharedPrefs.edit().putString(mKeySingleEntry, json).commit();

                Logger.verbose(
                        TAG + methodName,
                        "Updated cache contents written? ["
                                + removed
                                + "]"
                );

            }
            //Put the updated list in memory
            mList = load();
        }finally {
            writeLock.unlock();
        }

        return removed;
    }

    public List<T> getAll(){
        //Return copy of list
        readLock.lock();
        try {
            return new ArrayList<T>(mList);
        } finally {
            readLock.unlock();
        }
    }

    protected List<T> load() {
        final String methodName = ":load";
        List<T> result = null;

        final String jsonList = mSharedPrefs.getString(mKeySingleEntry, EMPTY_ARRAY);

        final Type listType = getListTypeToken();

        result = mGson.fromJson(
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
    public boolean clear() {
        final String methodName = ":clear";
        boolean cleared = false;

        writeLock.lock();
        try {
            //Clear disk
            cleared = mSharedPrefs.edit().clear().commit();
            //clear memory
            mList.clear();
        }finally{
            writeLock.unlock();
        }

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
