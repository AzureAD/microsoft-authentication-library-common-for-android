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

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public class CommandResultCacheItem {

    static final long VALIDITY_NANOS = TimeUnit.SECONDS.toNanos(30);
    private final CommandResult mValue;
    private final long mExpiresOn;
    private final long mPayloadBytes;
    private final LongSupplier mClock;

    public CommandResultCacheItem(CommandResult value){
        this(value, System.nanoTime(), 0, System::nanoTime);
    }

    CommandResultCacheItem(final CommandResult value, final long now, final long payloadBytes,
                           final LongSupplier clock) {
        mValue = value;
        mExpiresOn = now + VALIDITY_NANOS;
        mPayloadBytes = payloadBytes;
        mClock = clock;
    }

    public boolean isExpired(){
        return isExpired(mClock.getAsLong());
    }

    boolean isExpired(final long now) {
        return now - mExpiresOn >= 0;
    }

    long getPayloadBytes() {
        return mPayloadBytes;
    }
    public CommandResult getValue(){
        return mValue;
    }
}
