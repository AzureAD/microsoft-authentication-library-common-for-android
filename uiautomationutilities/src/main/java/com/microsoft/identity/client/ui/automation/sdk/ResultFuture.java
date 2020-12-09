package com.microsoft.identity.client.ui.automation.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ResultFuture<T, E extends Exception> {

    private final CountDownLatch mCountDownLatch = new CountDownLatch(1);
    private T mResult = null;
    private E mException = null;

    public boolean cancel(boolean b) {
        return false;
    }

    public boolean isCancelled() {
        return false;
    }

    public boolean isDone() {
        return mCountDownLatch.getCount() == 0;
    }

    public T get() throws Exception {
        mCountDownLatch.await();

        if (null != mException) {
            throw mException;
        }

        return mResult;
    }

    public T get(final long l, @NonNull final TimeUnit timeUnit) throws Throwable {
        if (mCountDownLatch.await(l, timeUnit)) {
            if (null != mException) {
                throw mException;
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
    public synchronized void setException(@NonNull final E exception) {
        mException = exception;
        mCountDownLatch.countDown();
    }

    /**
     * Sets the Result on this ResultFuture.
     *
     * @param result The Result to set.
     */
    public synchronized void setResult(@Nullable final T result) {
        mResult = result;
        mCountDownLatch.countDown();
    }
}