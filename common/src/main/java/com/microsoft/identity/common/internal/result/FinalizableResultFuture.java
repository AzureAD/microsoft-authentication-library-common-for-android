package com.microsoft.identity.common.internal.result;

import java.util.concurrent.CountDownLatch;

/**
 * A specialization of ResultFuture that can represent whether a task is not just complete,
 * but all follow-up tasks are complete as well.
 * @param <T> the type of object held by the future.
 */
public class FinalizableResultFuture<T> extends ResultFuture<T> {
    private CountDownLatch mFinalized = new CountDownLatch(1);

    /**
     * Set this future to be fully complete, including any cleanup tasks.
     */
    public synchronized void setFinalized() {
        mFinalized.countDown();
    }

    /**
     * Tell whether setFinalized has been called, or block until it has.
     * @return true if this future has been completed, including any cleanup tasks.
     */
    public synchronized boolean isFinalized() {
        try {
            mFinalized.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return true;
    }
}
