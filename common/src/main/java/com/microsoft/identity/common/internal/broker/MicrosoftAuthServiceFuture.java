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

package com.microsoft.identity.common.internal.broker;

import com.microsoft.identity.client.IMicrosoftAuthService;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MicrosoftAuthServiceFuture implements Future<IMicrosoftAuthService> {

    private final CountDownLatch mCountDownLatch = new CountDownLatch(1);
    private IMicrosoftAuthService mMicrosoftAuthService;

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
    public IMicrosoftAuthService get() throws InterruptedException, ExecutionException {
        mCountDownLatch.await();
        return mMicrosoftAuthService;
    }

    @Override
    public IMicrosoftAuthService get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        if (mCountDownLatch.await(l, timeUnit)) {
            return mMicrosoftAuthService;
        } else {
            throw new TimeoutException();
        }

    }

    public void setMicrosoftAuthService(IMicrosoftAuthService result) {
        mMicrosoftAuthService = result;
        mCountDownLatch.countDown();
    }
}
