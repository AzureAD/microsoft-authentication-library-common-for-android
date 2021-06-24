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
package com.microsoft.identity.common.internal.result;

import com.microsoft.identity.common.java.util.ResultFuture;

import java.util.concurrent.CountDownLatch;

/**
 * A specialization of ResultFuture that can represent whether a task is not just complete,
 * but all follow-up tasks are complete as well.
 * @param <T> the type of object held by the future.
 */
public class FinalizableResultFuture<T> extends ResultFuture<T> {
    private final CountDownLatch mFinalized = new CountDownLatch(1);

    /**
     * Set this future to be fully complete, including any cleanup tasks.
     */
    public void setCleanedUp() {
        mFinalized.countDown();
    }

    /**
     * Tell whether setFinalized has been called, or block until it has.
     * @return true if this future has been completed, including any cleanup tasks.
     */
    public boolean isCleanedUp() {
        try {
            mFinalized.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return true;
    }
}
