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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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
     * All cancellation signals associated with this future.
     * In the dedup case, multiple callers each contribute their own signal.
     * The first caller's signal (the original worker) has the actual
     * {@link java.net.HttpURLConnection} registered.
     * Cancelling ALL signals ensures the worker's connection gets disconnected.
     *
     * <p>Uses {@link CopyOnWriteArrayList} because:</p>
     * <ul>
     *   <li>Writes ({@link #addCancellationSignal}) are rare — typically 1-2 per command lifetime</li>
     *   <li>Reads ({@link #cancelAll} iteration) happen once on timeout</li>
     *   <li>No external synchronization needed for iteration — avoids
     *       {@code ConcurrentModificationException} that {@code Collections.synchronizedList}
     *       would require manual synchronization to prevent</li>
     * </ul>
     */
    private final List<CancellationSignal> mCancellationSignals = new CopyOnWriteArrayList<>();

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
     * Adds a cancellation signal for a caller of this future.
     * Called once per caller (original + each duplicate).
     * Thread-safe via CopyOnWriteArrayList.
     *
     * @param signal the cancellation signal for this caller
     */
    public void addCancellationSignal(@NonNull final CancellationSignal signal) {
        mCancellationSignals.add(signal);
    }

    /**
     * Cancels all cancellation signals associated with this future.
     * This ensures the worker thread's signal (which holds the HTTP connection
     * reference) gets cancelled, regardless of which caller times out first.
     *
     * <p>In the non-dedup case (single caller), the list has one entry.
     * In the dedup case, iteration covers all signals including the worker's.</p>
     *
     * <p>Thread-safe: CopyOnWriteArrayList provides snapshot iteration —
     * no ConcurrentModificationException even if addCancellationSignal()
     * is called concurrently from a dedup request.</p>
     *
     * <p>Named {@code cancelAllSignals()} instead of {@code cancel()} to avoid
     * confusion with {@link java.util.concurrent.Future#cancel(boolean)} inherited
     * from {@link com.microsoft.identity.common.java.util.ResultFuture}.</p>
     */
    public void cancelAllSignals() {
        for (final CancellationSignal signal : mCancellationSignals) {
            signal.cancel();
        }
    }
}
