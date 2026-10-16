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
package com.microsoft.identity.common.java.controllers;

import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.commands.BaseCommand;
import com.microsoft.identity.common.java.logging.Logger;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;
import lombok.NonNull;

/**
 * Name: CommandResultCache
 * Responsibilities: Caching results of commands on behalf of the command dispatcher
 */
// Suppressing rawtype warnings due to the generic type BaseCommand
@SuppressWarnings(WarningType.rawtype_warning)
public class CommandResultCache {

    private final static int DEFAULT_ITEM_COUNT = 250;
    public static final long DEFAULT_MAX_PAYLOAD_BYTES = 16L * 1024 * 1024;

    private final Object cacheLock = new Object();
    private final Map<CommandKey, CommandResultCacheItem> mCache;
    private final int mMaximumItems;
    private final LongSupplier mClock;
    private long mMaximumPayloadBytes;
    private long mPayloadBytes;

    public CommandResultCache() {
        this(DEFAULT_ITEM_COUNT);
    }

    public CommandResultCache(final int maxItemCount) {
        this(maxItemCount, DEFAULT_MAX_PAYLOAD_BYTES);
    }

    /**
     * Creates a best-effort cache bounded by entries and accounted serialized UTF-16 payload bytes, not VM RSS.
     * Commands are held weakly so cache keys cannot prolong callback/controller/context lifetimes.
     */
    public CommandResultCache(final int maxItemCount, final long maximumPayloadBytes) {
        this(maxItemCount, maximumPayloadBytes, System::nanoTime);
    }

    CommandResultCache(final int maxItemCount, final long maximumPayloadBytes, @NonNull final LongSupplier clock) {
        if (maxItemCount < 0 || maximumPayloadBytes < 0) {
            throw new IllegalArgumentException("Result-cache limits cannot be negative");
        }
        mMaximumItems = maxItemCount;
        mMaximumPayloadBytes = maximumPayloadBytes;
        mClock = clock;
        mCache = new LinkedHashMap<>(Math.min(maxItemCount, DEFAULT_ITEM_COUNT) + 1, .75f, true);
    }

    public CommandResult get(@NonNull final BaseCommand key) {
        synchronized (cacheLock) {
            prune();
            final CommandResultCacheItem item = mCache.get(new CommandKey(key));
            return item == null ? null : item.getValue();
        }
    }

    public void put(@NonNull final BaseCommand key, @NonNull final CommandResult value) {
        final long budget;
        synchronized (cacheLock) {
            budget = mMaximumPayloadBytes;
        }
        final long weight = mMaximumItems == 0 ? -1 : CommandResultCacheSizer.sizeOf(key, value, budget);
        synchronized (cacheLock) {
            final long now = mClock.getAsLong();
            prune(now);
            final CommandKey cacheKey = new CommandKey(key);
            final CommandResultCacheItem previous = mCache.remove(cacheKey);
            if (previous != null) {
                mPayloadBytes -= previous.getPayloadBytes();
            }
            if (weight < 0 || weight > mMaximumPayloadBytes || mMaximumItems == 0) {
                Logger.verbose("CommandResultCache", "Result is not retained within the configured payload budget");
                return;
            }
            while (!mCache.isEmpty()
                    && (mCache.size() >= mMaximumItems || weight > mMaximumPayloadBytes - mPayloadBytes)) {
                removeEldest();
            }
            mCache.put(cacheKey, new CommandResultCacheItem(value, now, weight, mClock));
            mPayloadBytes += weight;
        }
    }

    public int getSize() {
        synchronized (cacheLock) {
            prune();
            return this.mCache.size();
        }
    }

    /** Returns accounted retained payload bytes after removing expired or collected entries. */
    public long getPayloadSizeBytes() {
        synchronized (cacheLock) {
            prune();
            return mPayloadBytes;
        }
    }

    /** Changes the payload ceiling and evicts least-recently-used entries before returning. Zero disables retention. */
    public void setMaximumPayloadBytes(final long maximumPayloadBytes) {
        if (maximumPayloadBytes < 0) {
            throw new IllegalArgumentException("Result-cache budget cannot be negative");
        }
        synchronized (cacheLock) {
            mMaximumPayloadBytes = maximumPayloadBytes;
            prune();
            while (mPayloadBytes > maximumPayloadBytes) {
                removeEldest();
            }
        }
    }

    public void clear() {
        synchronized (cacheLock) {
            mCache.clear();
            mPayloadBytes = 0;
        }
    }

    /** Releases only advisory command-result memory; it does not delete token-cache data. */
    public void trimMemory() {
        clear();
    }

    private void prune() {
        if (!mCache.isEmpty()) {
            prune(mClock.getAsLong());
        }
    }

    private void prune(final long now) {
        final Iterator<Map.Entry<CommandKey, CommandResultCacheItem>> entries = mCache.entrySet().iterator();
        while (entries.hasNext()) {
            final Map.Entry<CommandKey, CommandResultCacheItem> entry = entries.next();
            if (entry.getKey().get() == null || entry.getValue().isExpired(now)) {
                mPayloadBytes -= entry.getValue().getPayloadBytes();
                entries.remove();
            }
        }
    }

    private void removeEldest() {
        final Iterator<CommandResultCacheItem> entries = mCache.values().iterator();
        final CommandResultCacheItem eldest = entries.next();
        mPayloadBytes -= eldest.getPayloadBytes();
        entries.remove();
    }

    private static final class CommandKey extends WeakReference<BaseCommand> {
        private final int hash;

        CommandKey(final BaseCommand command) {
            super(command);
            hash = command.hashCode();
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(final Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof CommandKey)) {
                return false;
            }
            final BaseCommand command = get();
            final BaseCommand candidate = ((CommandKey) other).get();
            return command != null && candidate != null && command.equals(candidate);
        }
    }
}
