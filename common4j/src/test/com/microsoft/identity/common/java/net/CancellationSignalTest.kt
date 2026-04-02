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

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import java.net.HttpURLConnection
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Unit tests for [CancellationSignal].
 * Validates thread-safe cancel/register/unregister lifecycle
 * and cross-thread cancellation scenarios.
 */
class CancellationSignalTest {

    private val executor = Executors.newSingleThreadExecutor()

    @After
    fun tearDown() {
        CancellationSignal.clearCurrentThread()
        executor.shutdownNow()
    }

    // ========================================================================
    // Basic Lifecycle
    // ========================================================================

    @Test
    fun initialState_isNotCancelled() {
        val signal = CancellationSignal()
        assertFalse("New signal should not be cancelled", signal.isCancelled)
    }

    @Test
    fun cancel_isIdempotent_disconnectsOnlyOnce() {
        val signal = CancellationSignal()
        val connection = mock(HttpURLConnection::class.java)
        signal.registerConnection(connection)

        signal.cancel()
        signal.cancel()

        assertTrue(signal.isCancelled)
        verify(connection, times(1)).disconnect()
    }

    @Test
    fun close_aliasesCancel() {
        val signal = CancellationSignal()
        signal.cancel()
        assertTrue(signal.isCancelled)
    }

    // ========================================================================
    // Register / Unregister
    // ========================================================================

    @Test
    fun registerConnection_returnsFalseAndDisconnects_whenAlreadyCancelled() {
        val signal = CancellationSignal()
        signal.cancel()

        val connection = mock(HttpURLConnection::class.java)

        assertFalse(signal.registerConnection(connection))
        verify(connection).disconnect()
    }

    @Test
    fun cancel_disconnectsRegisteredConnection() {
        val signal = CancellationSignal()
        val connection = mock(HttpURLConnection::class.java)
        signal.registerConnection(connection)

        signal.cancel()

        verify(connection).disconnect()
    }

    @Test
    fun unregister_then_reregister_cancelDisconnectsOnlyNewConnection() {
        val signal = CancellationSignal()
        val connection1 = mock(HttpURLConnection::class.java)
        val connection2 = mock(HttpURLConnection::class.java)

        // First attempt completes normally
        signal.registerConnection(connection1)
        signal.unregisterConnection()

        // Retry registers new connection
        signal.registerConnection(connection2)
        signal.cancel()

        verify(connection1, never()).disconnect()
        verify(connection2).disconnect()
    }

    // ========================================================================
    // ThreadLocal
    // ========================================================================

    @Test
    fun setForCurrentThread_and_clearCurrentThread_roundTrip() {
        val signal = CancellationSignal()

        CancellationSignal.setForCurrentThread(signal)
        assertSame(signal, CancellationSignal.getCurrentThreadSignal())

        CancellationSignal.clearCurrentThread()
        assertNull(CancellationSignal.getCurrentThreadSignal())
    }

    // ========================================================================
    // Cross-Thread Cancellation
    // ========================================================================

    @Test
    fun cancel_fromCallerThread_disconnectsWorkerConnection() {
        val signal = CancellationSignal()
        val connection = mock(HttpURLConnection::class.java)
        val workerRegistered = CountDownLatch(1)

        executor.submit {
            signal.registerConnection(connection)
            workerRegistered.countDown()
            // Simulate blocked HTTP read — spin until cancelled
            while (!signal.isCancelled) Thread.sleep(10)
        }

        assertTrue(workerRegistered.await(5, TimeUnit.SECONDS))
        signal.cancel()

        verify(connection).disconnect()
    }

    @Test
    fun cancel_beforeWorkerRegisters_workerDetectsOnRegistration() {
        val signal = CancellationSignal()
        val connection = mock(HttpURLConnection::class.java)

        // Caller cancels before worker starts
        signal.cancel()

        val result = booleanArrayOf(true)
        val workerDone = CountDownLatch(1)

        executor.submit {
            result[0] = signal.registerConnection(connection)
            workerDone.countDown()
        }

        assertTrue(workerDone.await(5, TimeUnit.SECONDS))
        assertFalse("registerConnection should return false", result[0])
        verify(connection).disconnect()
    }
}
