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
package com.microsoft.identity.common.internal.controllers;

import android.util.LruCache;

/**
 * Name: CommandResultCache
 * Responsibilities: Caching results of commands on behalf of the command dispatcher
 */
public class CommandResultCache {

    private final static int DEFAULT_ITEM_COUNT = 250;

    //Cache items allowed is still TBD... for now using default value of 250
    private LruCache<BaseCommand, CommandResultCacheItem> mCache;

    public CommandResultCache(){
        mCache = new LruCache<>(DEFAULT_ITEM_COUNT);
    }

    public CommandResultCache(int maxItemCount){
        mCache = new LruCache<>(maxItemCount);
    }

    public CommandResult get(BaseCommand key){
        synchronized (mCache) {
            CommandResultCacheItem item = mCache.get(key);
            if (item != null) {
                if(item.isExpired()){
                    mCache.remove(key);
                    return null;
                }else{
                    return item.getValue();
                }
            }else{
                return null;
            }
        }
    }

    public void put(BaseCommand key, CommandResult value){

        CommandResultCacheItem cacheItem = new CommandResultCacheItem(value);
        //NOTE: If an existing item using this key already in the cache it will be replaced
        mCache.put(key, cacheItem);
        //Object old = mCache.put(key, cacheItem);
        //We may want to log old if we see problems here, since the the old value is the value being replace with the new item.
    }

    public int getSize(){
        return this.mCache.size();
    }

    public void clear(){
        synchronized (mCache) {
            mCache = new LruCache<>(DEFAULT_ITEM_COUNT);
        }
    }

}
