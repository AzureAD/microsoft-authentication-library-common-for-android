
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
    public void setException(@NonNull final Throwable exception) {
        final List<BiConsumer<T, Throwable>> consumers;
        synchronized (this) {
            if (isDone()) {
                return;
            }
            mException = exception;
            consumers = new ArrayList<>(mConsumers);
            mConsumers.clear();
            mCountDownLatch.countDown();
        }
        notifyConsumers(consumers, mResult, exception);
    }

    /**
     * Sets the Result on this ResultFuture.
     *
     * @param result The Result to set.
     */
    public void setResult(@Nullable final T result) {
        final List<BiConsumer<T, Throwable>> consumers;
        synchronized (this) {
            if (isDone()) {
                return;
            }
            mResult = result;
            consumers = new ArrayList<>(mConsumers);
            mConsumers.clear();
            mCountDownLatch.countDown();
        }
        notifyConsumers(consumers, result, mException);
    }

    /**
     * Sets the whenComplete {@link BiConsumer} callback. Invoked upon completion
     * (either success/error).
     *
     * @param consumerToAdd The BiConsumer to invoke.
     */
    public void whenComplete(@NonNull final BiConsumer<T, Throwable> consumerToAdd) {
        final T result;
        final Throwable exception;
        synchronized (this) {
            if (!isDone()) {
                mConsumers.add(consumerToAdd);
                return;
            }
            result = mResult;
            exception = mException;
        }
        consumerToAdd.accept(result, exception);
    }

    /** Registers a pending callback without ever invoking it inline. False means completion already won the race. */
    public synchronized boolean tryWhenComplete(@NonNull final BiConsumer<T, Throwable> consumer) {
        if (isDone()) {
            return false;
        }
        mConsumers.add(consumer);
        return true;
    }

    /**
     * Detaches a still-pending caller without canceling shared work or affecting other callers.
     */
    public synchronized boolean removeWhenComplete(@NonNull final BiConsumer<T, Throwable> consumer) {
        return mConsumers.remove(consumer);
    }

    /** Returns the number of callbacks that still await terminal completion. */
    public synchronized int getPendingConsumerCount() {
        return mConsumers.size();
    }

    private void notifyConsumers(final List<BiConsumer<T, Throwable>> consumers,
                                 final T result, final Throwable exception) {
        // One failing callback must not strand other accepted callers or their cleanup reservations.
        Throwable failure = null;
        for (final BiConsumer<T, Throwable> consumer : consumers) {
            try {
                consumer.accept(result, exception);
            } catch (final RuntimeException | Error e) {
                if (failure == null) {
                    failure = e;
                } else if (failure != e) {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure instanceof Error) {
            throw (Error) failure;
        }
        if (failure instanceof RuntimeException) {
            throw (RuntimeException) failure;
        }
    }
}