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
package com.microsoft.identity.client.ui.automation.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.ui.automation.logging.Logger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A wrapper class to remove the dependency on latches to wait for setting result or exception
 * with some fix timeout or infinite time. This class could be inherited by Test Cases to await
 * for the result and leveraging it to set result or exceptions as output of Test Case.
 */
public class ResultFuture<T, E extends Exception> {

    private static final String TAG = ResultFuture.class.getSimpleName();
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
        Logger.i(TAG, "Gets the Result from ResultFuture object untill its set..");
        mCountDownLatch.await();

        if (null != mException) {
            throw mException;
        }

        return mResult;
    }

    public T get(final long l, @NonNull final TimeUnit timeUnit) throws Throwable {
        Logger.i(TAG, "Gets the Result from ResultFuture object after a particular Time Unit..");
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
        Logger.i(TAG, "Sets the Exception on the ResultFuture object..");
        mException = exception;
        mCountDownLatch.countDown();
    }

    /**
     * Sets the Result on this ResultFuture.
     *
     * @param result The Result to set.
     */
    public synchronized void setResult(@Nullable final T result) {
        Logger.i(TAG, "Sets the Auth Result on the ResultFuture object..");
        mResult = result;
        mCountDownLatch.countDown();
    }
}
