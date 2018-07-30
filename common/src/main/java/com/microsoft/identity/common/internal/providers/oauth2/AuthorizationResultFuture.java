package com.microsoft.identity.common.internal.providers.oauth2;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AuthorizationResultFuture implements Future<AuthorizationResult> {

    private final CountDownLatch mCountDownLatch = new CountDownLatch(1);
    private AuthorizationResult mAuthorizationResult;

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
    public AuthorizationResult get() throws InterruptedException, ExecutionException {
        mCountDownLatch.await();
        return mAuthorizationResult;
    }

    @Override
    public AuthorizationResult get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        if(mCountDownLatch.await(l, timeUnit)){
            return mAuthorizationResult;
        }else {
            throw new TimeoutException();
        }

    }

    public void setAuthorizationResult(AuthorizationResult result){
        mAuthorizationResult = result;
        mCountDownLatch.countDown();
    }
}
