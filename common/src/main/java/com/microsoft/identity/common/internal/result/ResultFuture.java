package com.microsoft.identity.common.internal.result;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ResultFuture<T> implements Future<T> {

    private final CountDownLatch mCountDownLatch = new CountDownLatch(1);
    private T mResult;

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
    public T get() throws InterruptedException {
        mCountDownLatch.await();
        return mResult;
    }

    @Override
    public T get(long l, TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        if (mCountDownLatch.await(l, timeUnit)) {
            return mResult;
        } else {
            throw new TimeoutException();
        }

    }

    public void setResult(T result) {
        mResult = result;
        mCountDownLatch.countDown();
    }
}
