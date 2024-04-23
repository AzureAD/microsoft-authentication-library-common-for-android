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
package com.microsoft.identity.common.java.cache;

import lombok.NonNull;

import com.google.gson.Gson;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.util.StringUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * A simple metadata store definition that uses INameValueStorage to persist, read,
 * update, and delete data. Please note that all CRUD actions return success, as the underlying
 * store's API does not surface a success indicator. If you need stronger guarantees that an
 * operation was successful, use SharedPreferencesSimpleCacheImpl which is less performance
 * oriented.
 * <p>
 * Data serializes as JSON.
 *
 * @param <T> The type of metadata that will be persisted.
 */
public abstract class NameValueStorageFileManagerSimpleCacheImpl<T> implements ISimpleCache<T>, IListTypeToken {

    private static final String TAG = NameValueStorageFileManagerSimpleCacheImpl.class.getSimpleName();
    private static final String EMTPY_ARRAY = "[]";
    private static final String TIMING_TAG = "execWithTiming";

    private final IPlatformComponents mComponents;
    private final INameValueStorage<String> mStorage;
    private final String mKeySingleEntry;
    private final boolean mForceReinsertionOfDuplicates;
    private final Gson mGson = new Gson();

    /**
     * Constructs a new NameValueStorageFileManagerSimpleCacheImpl. Convenience class for persisting
     * lists of arbitrarily-typed data. Duplicate reinsertion is disabled (backcompat) by default.
     *
     * @param components The current app's {@link IPlatformComponents}.
     * @param name       The name of the underlying storage file.
     * @param singleKey  The name of the key under which all entries will be cached.
     */
    public NameValueStorageFileManagerSimpleCacheImpl(@NonNull final IPlatformComponents components,
                                                      @NonNull final String name,
                                                      @NonNull final String singleKey) {
        this(components, name, singleKey, false);
    }

    /**
     * Constructs a new NameValueStorageFileManagerSimpleCacheImpl. Convenience class for persisting
     * lists of arbitrarily-typed data.
     *
     * @param components                   The current app's {@link IPlatformComponents}.
     * @param name                         The name of the underlying storage file.
     * @param singleKey                    The name of the key under which all entries will be cached.
     * @param forceReinsertionOfDuplicates If true, calling insert() on a value that already exists
     *                                     replaces the existing value with the newly-provided one.
     */
    public NameValueStorageFileManagerSimpleCacheImpl(@NonNull final IPlatformComponents components,
                                                      @NonNull final String name,
                                                      @NonNull final String singleKey,
                                                      final boolean forceReinsertionOfDuplicates) {

        Logger.verbose(TAG + "::ctor", "Init");
        mComponents = components;
        mStorage = components.getStorageSupplier().getUnencryptedNameValueStore(name, String.class);
        mKeySingleEntry = singleKey;
        mForceReinsertionOfDuplicates = forceReinsertionOfDuplicates;
    }

    private interface NamedRunnable<V> extends Callable<V> {
        String getName();
    }

    private <V> V execWithTiming(@NonNull final NamedRunnable<V> runnable) {
        final long startTime = mComponents.getPlatformUtil().getNanosecondTime();

        V v = null;
        try {
            v = runnable.call();
        } catch (final Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            Logger.error(TAG + TIMING_TAG, "Error during operation", e);
        } finally {
            final long execTime =  mComponents.getPlatformUtil().getNanosecondTime() - startTime;
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
                mStorage.put(mKeySingleEntry, json);
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
                mStorage.put(mKeySingleEntry, json);
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
                String jsonList = mStorage.get(mKeySingleEntry);

                if (StringUtil.isNullOrEmpty(jsonList)) {
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
                mStorage.clear();
                return true;
            }
        });
    }
}
