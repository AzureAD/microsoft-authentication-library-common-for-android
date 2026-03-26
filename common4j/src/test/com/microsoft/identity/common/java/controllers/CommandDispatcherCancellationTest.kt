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
package com.microsoft.identity.common.java.controllers

import com.microsoft.identity.common.java.net.CancellationSignal
import com.microsoft.identity.common.java.result.FinalizableResultFuture
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.net.HttpURLConnection
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Integration tests simulating the CommandDispatcher -> CancellationSignal -> HttpURLConnection
 * lifecycle. These tests validate the full E2E cancellation flow across threads without
 * requiring the actual [CommandDispatcher] and its heavyweight dependencies.
 *
 * Each test mirrors a real production scenario described in the proposal:
 * - Normal timeout (worker blocked on HTTP read)
 * - Pre-start cancellation (caller timed out before worker started)
 * - Dedup timeout (shared future with multiple callers)
 */
class CommandDispatcherCancellationTest {

    private val executor = Executors.newSingleThreadExecutor()

    @After
    fun tearDown() {
        executor.shutdownNow()
    }

    /**
     * Simulates the normal timeout scenario:
     * 1. Worker registers connection and blocks on HTTP read
     * 2. Caller times out and calls cancelAllSignals()
     * 3. Worker's connection is disconnected, worker unblocks immediately
     */
    @Test
    fun normalTimeout_cancelDisconnectsActiveConnection_workerUnblocks() {
        val future = FinalizableResultFuture<String>()
        val signal = CancellationSignal()
        future.addCancellationSignal(signal)

        val connection = mock(HttpURLConnection::class.java)
        val workerRegistered = CountDownLatch(1)
        val workerFinished = CountDownLatch(1)

        // Worker thread: mimics submitSilentReturningFuture's Runnable
        executor.submit {
            CancellationSignal.setForCurrentThread(signal)
            try {
                signal.registerConnection(connection)
                workerRegistered.countDown()
                // Simulate blocked on HTTP read — spin until cancelled
                while (!signal.isCancelled) Thread.sleep(10)
                future.setResult("cancelled_by_signal")
            } finally {
                signal.unregisterConnection()
                CancellationSignal.clearCurrentThread()
                workerFinished.countDown()
            }
        }

        // Caller thread: wait for worker to register, then simulate TimeoutException
        assertTrue(
            "Worker should register within 5s",
            workerRegistered.await(5, TimeUnit.SECONDS)
        )

        // Simulate: catch(TimeoutException) { future.cancelAllSignals(); }
        future.cancelAllSignals()

        // Worker should unblock and finish quickly
        assertTrue(
            "Worker should finish within 2s after cancel",
            workerFinished.await(2, TimeUnit.SECONDS)
        )

        assertTrue(signal.isCancelled)
        verify(connection).disconnect()
    }

    /**
     * Simulates pre-start cancellation:
     * 1. Caller times out before worker starts
     * 2. Worker checks isCancelled() at start and skips execution
     */
    @Test
    fun preStartCancellation_workerSkipsExecution() {
        val future = FinalizableResultFuture<String>()
        val signal = CancellationSignal()
        future.addCancellationSignal(signal)

        // Caller times out BEFORE worker starts
        future.cancelAllSignals()
        assertTrue("Signal should already be cancelled", signal.isCancelled)

        val commandExecuted = booleanArrayOf(false)
        val workerFinished = CountDownLatch(1)

        // Worker starts AFTER cancel (simulates delayed pool pickup)
        executor.submit {
            CancellationSignal.setForCurrentThread(signal)
            try {
                // Pre-start check (as in CommandDispatcher worker Runnable)
                if (signal.isCancelled) {
                    return@submit
                }
                // This should NOT be reached
                commandExecuted[0] = true
            } finally {
                CancellationSignal.clearCurrentThread()
                workerFinished.countDown()
            }
        }

        assertTrue(
            "Worker should finish within 5s",
            workerFinished.await(5, TimeUnit.SECONDS)
        )
        assertFalse("Command should NOT have executed", commandExecuted[0])
    }

    /**
     * Simulates dedup timeout:
     * 1. Request A's worker has an active connection (signalA registered)
     * 2. Request B is deduplicated — adds signalB to same future, no worker
     * 3. Any caller times out — cancelAllSignals() disconnects the worker's connection
     */
    @Test
    fun dedupTimeout_cancelsOriginalWorkerConnection() {
        val future = FinalizableResultFuture<String>()

        // Request A: original — worker has an active connection
        val signalA = CancellationSignal()
        val workerConnection = mock(HttpURLConnection::class.java)
        future.addCancellationSignal(signalA)

        val workerRegistered = CountDownLatch(1)
        val workerFinished = CountDownLatch(1)

        executor.submit {
            CancellationSignal.setForCurrentThread(signalA)
            try {
                signalA.registerConnection(workerConnection)
                workerRegistered.countDown()
                while (!signalA.isCancelled) Thread.sleep(10)
            } finally {
                signalA.unregisterConnection()
                CancellationSignal.clearCurrentThread()
                workerFinished.countDown()
            }
        }

        assertTrue(workerRegistered.await(5, TimeUnit.SECONDS))

        // Request B: duplicate — adds signal to same future, no worker
        val signalB = CancellationSignal()
        future.addCancellationSignal(signalB)

        // Request B's caller times out — cancelAllSignals()
        future.cancelAllSignals()

        assertTrue(
            "Worker should finish within 2s after cancel",
            workerFinished.await(2, TimeUnit.SECONDS)
        )

        assertTrue("SignalA (worker) should be cancelled", signalA.isCancelled)
        assertTrue("SignalB (dedup) should be cancelled", signalB.isCancelled)
        verify(workerConnection).disconnect()
    }
}
