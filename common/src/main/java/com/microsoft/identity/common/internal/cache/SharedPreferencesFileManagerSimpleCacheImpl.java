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
import android.os.Build;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.microsoft.identity.common.AndroidCommonComponents;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.common.java.interfaces.ICommonComponents;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.logging.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * A simple metadata store definition that uses SharedPreferencesFileManager to persist, read,
 * update, and delete data. Please note that all CRUD actions return success, as the underlying
 * store's API does not surface a success indicator. If you need stronger guarantees that an
 * operation was successful, use {@link SharedPreferencesSimpleCacheImpl} which is less performance
 * oriented.
 * <p>
 * Data serializes as JSON.
 *
 * @param <T> The type of metadata that will be persisted.
 * @see SharedPreferencesSimpleCacheImpl
 */
public abstract class SharedPreferencesFileManagerSimpleCacheImpl<T> implements ISimpleCache<T>, IListTypeToken {

    private static final String TAG = SharedPreferencesFileManagerSimpleCacheImpl.class.getSimpleName();
    private static final String EMTPY_ARRAY = "[]";
    private static final String TIMING_TAG = "execWithTiming";

    private final INameValueStorage<String> mSharedPrefsFileManager;
    private final String mKeySingleEntry;
    private final boolean mForceReinsertionOfDuplicates;
    private final Gson mGson = new Gson();

    /**
     * Constructs a new SharedPreferencesFileManagerSimpleCacheImpl. Convenience class for persisting
     * lists of arbitrarily-typed data. Duplicate reinsertion is disabled (backcompat) by default.
     *
     * @param components   The current app's {@link Context}.
     * @param prefsName The name of the underlying {@link android.content.SharedPreferences} file.
     * @param singleKey The name of the key under which all entries will be cached.
     */
    public SharedPreferencesFileManagerSimpleCacheImpl(@NonNull final ICommonComponents components,
                                                       @NonNull final String prefsName,
                                                       @NonNull final String singleKey) {
        this(components.getNameValueStore(prefsName, String.class),
                singleKey,
                false
        );
    }

    /**
     * Constructs a new SharedPreferencesFileManagerSimpleCacheImpl. Convenience class for persisting
     * lists of arbitrarily-typed data.
     *
     * @param components                      The current app's {@link Context}.
     * @param prefsName                    The name of the underlying {@link android.content.SharedPreferences} file.
     * @param singleKey                    The name of the key under which all entries will be cached.
     * @param forceReinsertionOfDuplicates If true, calling insert() on a value that already exists
     *                                     replaces the existing value with the newly-provided one.
     */
    public SharedPreferencesFileManagerSimpleCacheImpl(@NonNull final ICommonComponents components,
                                                       @NonNull final String prefsName,
                                                       @NonNull final String singleKey,
                                                       final boolean forceReinsertionOfDuplicates) {
        this(components.getNameValueStore(prefsName, String.class),
                singleKey,
                forceReinsertionOfDuplicates
        );
    }

    /**
     * Constructs a new SharedPreferencesFileManagerSimpleCacheImpl. Convenience class for persisting
     * lists of arbitrarily-typed data. Duplicate reinsertion is disabled (backcompat) by default.
     *
     * @param context   The current app's {@link Context}.
     * @param prefsName The name of the underlying {@link android.content.SharedPreferences} file.
     * @param singleKey The name of the key under which all entries will be cached.
     */
    @Deprecated
    public SharedPreferencesFileManagerSimpleCacheImpl(@NonNull final Context context,
                                                       @NonNull final String prefsName,
                                                       @NonNull final String singleKey) {
        this(new AndroidCommonComponents(context).getNameValueStore(prefsName, String.class),
                singleKey,
                false
        );
    }

    /**
     * Constructs a new SharedPreferencesFileManagerSimpleCacheImpl. Convenience class for persisting
     * lists of arbitrarily-typed data.
     *
     * @param context                      The current app's {@link Context}.
     * @param prefsName                    The name of the underlying {@link android.content.SharedPreferences} file.
     * @param singleKey                    The name of the key under which all entries will be cached.
     * @param forceReinsertionOfDuplicates If true, calling insert() on a value that already exists
     *                                     replaces the existing value with the newly-provided one.
     */
    @Deprecated
    public SharedPreferencesFileManagerSimpleCacheImpl(@NonNull final Context context,
                                                       @NonNull final String prefsName,
                                                       @NonNull final String singleKey,
                                                       final boolean forceReinsertionOfDuplicates) {
        this(new AndroidCommonComponents(context).getNameValueStore(prefsName, String.class),
                singleKey,
                forceReinsertionOfDuplicates
        );
    }

    /**
     * Constructs a new SharedPreferencesFileManagerSimpleCacheImpl from the provided
     * {@link IKeyBasedStorage}, using the provided singleKey for the underlying collection.
     *
     * @param sharedPreferencesFileManager The underlying {@link IKeyBasedStorage} to use.
     * @param singleKey                    The name of the key under which all entries will be cached.
     * @param forceReinsertionOfDuplicates If true, calling insert() on a value that already exists
     *                                     replaces the existing value with the newly-provided one.
     */
    protected SharedPreferencesFileManagerSimpleCacheImpl(@NonNull final INameValueStorage<String> sharedPreferencesFileManager,
                                                       @NonNull final String singleKey,
                                                       final boolean forceReinsertionOfDuplicates) {
        Logger.verbose(TAG + "::ctor", "Init");
        mSharedPrefsFileManager = sharedPreferencesFileManager;
        mKeySingleEntry = singleKey;
        mForceReinsertionOfDuplicates = forceReinsertionOfDuplicates;
    }

    private interface NamedRunnable<V> extends Callable<V> {
        String getName();
    }

    private <V> V execWithTiming(@NonNull final NamedRunnable<V> runnable) {
        final long startTime;
        final long execTime;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            startTime = SystemClock.elapsedRealtimeNanos();
        } else {
            startTime = System.nanoTime();
        }

        V v = null;
        try {
            v = runnable.call();
        } catch (final Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            Logger.error(TAG + TIMING_TAG, "Error during operation", e);
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                execTime = SystemClock.elapsedRealtimeNanos() - startTime;
            } else {
                execTime = System.nanoTime() - startTime;
            }

            Logger.verbose(TAG + TIMING_TAG,
                    runnable.getName() + " finished in: " + execTime + " " + TimeUnit.NANOSECONDS.name());
        }

        return v;
    }

    @Override
    public boolean insert(final T t) {
        return execWithTiming(new NamedRunnable<Boolean>() {
            @Override
            public String getName() {
                return "insert";
            }

            @Override
            public Boolean call() {
                final Set<T> allMetadata = new HashSet<>(getAll());

                if (mForceReinsertionOfDuplicates) {
                    // This is a bit of workaround for Set's default behavior
                    // where items already within the Set are not replaced if they are
                    // inserted but already exist.
                    // This makes it behave more like a Map.
                    allMetadata.remove(t);
                }

                allMetadata.add(t);
                final String json = mGson.toJson(allMetadata);
                mSharedPrefsFileManager.put(mKeySingleEntry, json);
                return true;
            }
        });
    }

    @Override
    public boolean remove(final T t) {
        return execWithTiming(new NamedRunnable<Boolean>() {
            @Override
            public String getName() {
                return "remove";
            }

            @Override
            public Boolean call() {
                final Set<T> allMetadata = new HashSet<>(getAll());
                allMetadata.remove(t);
                final String json = mGson.toJson(allMetadata);
                mSharedPrefsFileManager.put(mKeySingleEntry, json);
                return true;
            }
        });
    }

    @Override
    public List<T> getAll() {
        return execWithTiming(new NamedRunnable<List<T>>() {
            @Override
            public String getName() {
                return "getAll";
            }

            @Override
            public List<T> call() {
                String jsonList = mSharedPrefsFileManager.get(mKeySingleEntry);

                if (StringUtil.isEmpty(jsonList)) {
                    jsonList = EMTPY_ARRAY;
                }

                final List<T> result = mGson.fromJson(jsonList, getListTypeToken());

                return result;
            }
        });
    }

    @Override
    public boolean clear() {
        return execWithTiming(new NamedRunnable<Boolean>() {
            @Override
            public String getName() {
                return "clear";
            }

            @Override
            public Boolean call() {
                mSharedPrefsFileManager.clear();
                return true;
            }
        });
    }
}
