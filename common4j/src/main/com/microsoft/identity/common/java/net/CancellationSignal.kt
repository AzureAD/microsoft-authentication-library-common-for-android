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
import java.io.Closeable
import java.net.HttpURLConnection
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Thread-safe cancellation signal shared between the command dispatcher (caller)
 * and the HTTP client (worker). When the command-level timeout fires, the caller
 * sets this signal to cancelled, which disconnects any registered [HttpURLConnection].
 *
 * Lifecycle:
 * 1. Created by CommandDispatcher BEFORE submitting to the thread pool
 * 2. Stored on FinalizableResultFuture so the caller can trigger cancellation
 * 3. Passed to the worker thread via ThreadLocal (set at start of run(), cleared in finally)
 * 4. UrlConnectionHttpClient registers/unregisters the active HttpURLConnection
 * 5. On [cancel], the registered connection is disconnected immediately
 */
class CancellationSignal : Closeable {

    private val mCancelled = AtomicBoolean(false)
    private val mActiveConnection = AtomicReference<HttpURLConnection>(null)

    /**
     * Returns true if this signal has been cancelled.
     */
    val isCancelled: Boolean
        get() = mCancelled.get()

    /**
     * Cancels the signal and disconnects any active HTTP connection.
     * Thread-safe — can be called from any thread (typically the caller thread).
     */
    fun cancel() {
        if (mCancelled.compareAndSet(false, true)) {
            val connection = mActiveConnection.getAndSet(null)
            if (connection != null) {
                safeDisconnect(connection,
                    "Disconnecting HTTP connection due to command-level timeout")
            }
        }
    }

    /**
     * Alias for [cancel]. Enables try-with-resources / use {} blocks.
     */
    override fun close() = cancel()

    /**
     * Registers the active [HttpURLConnection] so it can be disconnected on cancellation.
     * If already cancelled, disconnects immediately and returns false.
     *
     * Uses a fast-path check before setting, plus a double-check after setting,
     * to minimize unnecessary atomic operations in the already-cancelled case
     * while still handling the race where [cancel] fires between set and check.
     *
     * @param connection the connection to register
     * @return true if registered successfully, false if already cancelled
     */
    fun registerConnection(connection: HttpURLConnection): Boolean {
        // Fast path: if already cancelled, don't even register
        if (mCancelled.get()) {
            safeDisconnect(connection,
                "Connection registered after cancellation — disconnecting immediately")
            return false
        }

        mActiveConnection.set(connection)

        // Double-check: cancel() may have fired between the fast-path check and set()
        if (mCancelled.get()) {
            mActiveConnection.set(null)
            safeDisconnect(connection,
                "Cancel race detected in registerConnection — disconnecting")
            return false
        }
        return true
    }

    /**
     * Unregisters the active connection (called after HTTP response is read).
     */
    fun unregisterConnection() {
        mActiveConnection.set(null)
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
         * Safely disconnects an [HttpURLConnection], swallowing any exceptions.
         * [HttpURLConnection.disconnect] is idempotent — safe to call multiple times.
         */
        private fun safeDisconnect(connection: HttpURLConnection, reason: String) {
            try {
                Logger.info(TAG, reason)
                connection.disconnect()
            } catch (e: Exception) {
                Logger.warn(TAG, "Exception during disconnect ($reason): ${e.message}")
            }
        }
    }
}
