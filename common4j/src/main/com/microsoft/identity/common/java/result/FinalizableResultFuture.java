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
package com.microsoft.identity.common.java.result;

import com.microsoft.identity.common.java.net.CancellationSignal;
import com.microsoft.identity.common.java.util.ResultFuture;

import java.util.concurrent.CountDownLatch;

import lombok.NonNull;

/**
 * A specialization of ResultFuture that can represent whether a task is not just complete,
 * but all follow-up tasks are complete as well.
 * @param <T> the type of object held by the future.
 */
public class FinalizableResultFuture<T> extends ResultFuture<T> {
    private final CountDownLatch mFinalized = new CountDownLatch(1);

    /**
     * Cancellation signal owned by this future. Shared by all callers (original + dedup)
     * and the worker thread. The worker registers its {@link java.net.HttpURLConnection}
     * on this signal; on timeout, any caller can invoke {@link #cancelSignal()} to
     * disconnect the worker's active connection.
     */
    private final CancellationSignal mCancellationSignal = new CancellationSignal();

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

    /**
     * Returns the cancellation signal owned by this future.
     * All callers (original + dedup) and the worker thread share this single signal.
     * The worker registers its {@link java.net.HttpURLConnection} on it; any caller
     * that times out calls {@link #cancelSignal()} to disconnect the worker's connection.
     *
     * @return the cancellation signal for this future
     */
    @NonNull
    public CancellationSignal getCancellationSignal() {
        return mCancellationSignal;
    }

    /**
     * Cancels the cancellation signal, disconnecting any active HTTP connection
     * registered by the worker thread.
     *
     * <p>Named {@code cancelSignal()} instead of {@code cancel()} to avoid
     * confusion with {@link java.util.concurrent.Future#cancel(boolean)} inherited
     * from {@link com.microsoft.identity.common.java.util.ResultFuture}.</p>
     */
    public void cancelSignal() {
        mCancellationSignal.cancel();
    }
}
