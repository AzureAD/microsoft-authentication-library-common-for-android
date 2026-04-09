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
package com.microsoft.identity.common.java.net

import com.microsoft.identity.common.java.logging.Logger
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Thread-safe cancellation signal shared between the command dispatcher (caller)
 * and the worker thread. When the command-level timeout fires, the caller sets this
 * signal to cancelled, which executes all registered cancel actions (e.g., disconnecting
 * HTTP connections).
 *
 * Multiple cancel actions can be registered additively via [registerOnCancel].
 * On [cancel], all registered actions are executed and the list is cleared.
 *
 * Lifecycle:
 * 1. Created internally by FinalizableResultFuture
 * 2. Passed to the worker thread via ThreadLocal (set at start of run(), cleared in finally)
 * 3. Worker registers cancel actions via [registerOnCancel] (e.g., urlConnection::disconnect)
 * 4. On [cancel], all registered actions are executed immediately
 */
class CancellationSignal {

    private val mCancelled = AtomicBoolean(false)
    private val mOnCancelActions = CopyOnWriteArrayList<Runnable>()

    /**
     * Returns true if this signal has been cancelled.
     */
    val isCancelled: Boolean
        get() = mCancelled.get()

    /**
     * Cancels the signal and executes all registered cancel actions.
     * Thread-safe — can be called from any thread (typically the caller thread).
     */
    fun cancel() {
        if (mCancelled.compareAndSet(false, true)) {
            for (action in mOnCancelActions) {
                safeRun(action, "Executing cancel action due to command-level timeout")
            }
            mOnCancelActions.clear()
        }
    }

    /**
     * Registers an action to execute on cancellation (e.g., disconnecting an HTTP connection).
     * Multiple actions can be registered — all will be executed when [cancel] is called.
     * If already cancelled, executes immediately and returns false.
     *
     * @param action the action to execute on cancellation
     * @return true if registered successfully, false if already cancelled
     */
    fun registerOnCancel(action: Runnable): Boolean {
        // Fast path: if already cancelled, execute immediately
        if (mCancelled.get()) {
            safeRun(action, "Action registered after cancellation — executing immediately")
            return false
        }

        mOnCancelActions.add(action)

        // Double-check: cancel() may have fired between the fast-path check and add()
        if (mCancelled.get()) {
            mOnCancelActions.remove(action)
            safeRun(action, "Cancel race detected in registerOnCancel — executing")
            return false
        }
        return true
    }

    /**
     * Unregisters a specific cancel action (called after the cancellable operation completes).
     * The caller must pass the same [Runnable] instance that was registered.
     *
     * @param action the action to unregister
     */
    fun unregisterOnCancel(action: Runnable) {
        mOnCancelActions.remove(action)
    }

    companion object {
        private const val TAG = "CancellationSignal"

        private val sThreadLocalSignal = ThreadLocal<CancellationSignal>()

        /**
         * Sets the cancellation signal for the current worker thread.
         */
        @JvmStatic
        fun setForCurrentThread(signal: CancellationSignal) {
            sThreadLocalSignal.set(signal)
        }

        /**
         * Returns the cancellation signal for the current thread, or null if none is set.
         */
        @JvmStatic
        fun getCurrentThreadSignal(): CancellationSignal? = sThreadLocalSignal.get()

        /**
         * Clears the cancellation signal for the current thread.
         * Must be called in finally blocks to prevent ThreadLocal leaks.
         */
        @JvmStatic
        fun clearCurrentThread() {
            sThreadLocalSignal.remove()
        }

        /**
         * Safely executes a cancel action, swallowing any exceptions.
         */
        private fun safeRun(action: Runnable, reason: String) {
            try {
                Logger.info(TAG, reason)
                action.run()
            } catch (e: Exception) {
                Logger.warn(TAG, "Exception during cancel action ($reason): ${e.message}")
            }
        }
    }
}
