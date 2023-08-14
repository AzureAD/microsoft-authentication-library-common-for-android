
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

package com.microsoft.identity.common.java.util;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SuppressFBWarnings(value = "IS2_INCONSISTENT_SYNC",
        justification = "get() invokes await and is a blocking call. making it synchronized will create a deadlock.")
public class ResultFuture<T> implements Future<T> {

    private final CountDownLatch mCountDownLatch = new CountDownLatch(1);
    private T mResult = null;
    private Throwable mException = null;
    private final List<BiConsumer<T, Throwable>> mConsumers = new ArrayList<>();

    @Override
    public boolean cancel(boolean b) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return mCountDownLatch.getCount() == 0;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        mCountDownLatch.await();

        if (null != mException) {
            throw new ExecutionException(mException);
        }

        return mResult;
    }

    @Override
    public T get(final long l, @NonNull final TimeUnit timeUnit) throws InterruptedException, TimeoutException, ExecutionException {
        if (mCountDownLatch.await(l, timeUnit)) {
            if (null != mException) {
                throw new ExecutionException(mException);
            }

            return mResult;
        } else {
            throw new TimeoutException(
                    "Timed out waiting for: "
                            + l // duration
                            + timeUnit.name() // units
            );
        }
    }

    /**
     * Sets the Exception on this ResultFuture.
     *
     * @param exception The Exception to set.
     */
    public synchronized void setException(@NonNull final Throwable exception) {
        mException = exception;
        mCountDownLatch.countDown();

        for (final BiConsumer<T, Throwable> consumer : mConsumers) {
            consumer.accept(mResult, exception);
        }
    }

    /**
     * Sets the Result on this ResultFuture.
     *
     * @param result The Result to set.
     */
    public synchronized void setResult(@Nullable final T result) {
        mResult = result;
        mCountDownLatch.countDown();

        for (final BiConsumer<T, Throwable> consumer : mConsumers) {
            consumer.accept(result, mException);
        }

        mConsumers.clear();
    }

    /**
     * Sets the whenComplete {@link BiConsumer} callback. Invoked upon completion
     * (either success/error).
     *
     * @param consumerToAdd The BiConsumer to invoke.
     */
    public synchronized void whenComplete(@NonNull final BiConsumer<T, Throwable> consumerToAdd) {
        if (isDone()) {
            consumerToAdd.accept(mResult, mException);
            return;
        }

        mConsumers.add(consumerToAdd);
    }
}