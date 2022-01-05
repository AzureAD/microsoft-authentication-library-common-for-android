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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Name: CommandResultCache
 * Responsibilities: Caching results of commands on behalf of the command dispatcher
 */
// Suppressing rawtype warnings due to the generic type BaseCommand
@SuppressWarnings(WarningType.rawtype_warning)
public class CommandResultCache {

    private static final int DEFAULT_ITEM_COUNT = 250;

    private final Object cacheLock = new Object();
    // Cache items allowed is still TBD... for now using default value of 250
    private final Map<BaseCommand, CommandResultCacheItem> mCache;

    public CommandResultCache() {
        this(DEFAULT_ITEM_COUNT);
    }

    public CommandResultCache(final int maxItemCount) {
        mCache =
                new LinkedHashMap<BaseCommand, CommandResultCacheItem>(
                        maxItemCount + 1, .75f, true) {
                    @Override
                    protected boolean removeEldestEntry(
                            Map.Entry<BaseCommand, CommandResultCacheItem> eldest) {
                        return size() > maxItemCount;
                    }
                };
    }

    public CommandResult get(@SuppressWarnings(WarningType.rawtype_warning) BaseCommand key) {
        synchronized (cacheLock) {
            CommandResultCacheItem item = mCache.get(key);
            if (item != null) {
                if (item.isExpired()) {
                    mCache.remove(key);
                    return null;
                } else {
                    return item.getValue();
                }
            } else {
                return null;
            }
        }
    }

    public void put(
            @SuppressWarnings(WarningType.rawtype_warning) BaseCommand key, CommandResult value) {
        synchronized (cacheLock) {
            CommandResultCacheItem cacheItem = new CommandResultCacheItem(value);
            // NOTE: If an existing item using this key already in the cache it will be replaced
            mCache.put(key, cacheItem);
            // Object old = mCache.put(key, cacheItem);
            // We may want to log old if we see problems here, since the the old value is the value
            // being replace with the new item.
        }
    }

    public int getSize() {
        synchronized (cacheLock) {
            return this.mCache.size();
        }
    }

    public void clear() {
        synchronized (cacheLock) {
            mCache.clear();
        }
    }
}
