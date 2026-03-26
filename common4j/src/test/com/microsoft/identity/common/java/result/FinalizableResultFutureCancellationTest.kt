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
package com.microsoft.identity.common.java.result

import com.microsoft.identity.common.java.net.CancellationSignal
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.net.HttpURLConnection

/**
 * Tests for cancellation signal support in [FinalizableResultFuture].
 * Validates [addCancellationSignal] and [cancelAllSignals] for both
 * single-request and deduplicated-request (shared future) scenarios.
 */
class FinalizableResultFutureCancellationTest {

    @Test
    fun cancelAllSignals_singleSignal_cancelsAndDisconnects() {
        val future = FinalizableResultFuture<String>()
        val signal = CancellationSignal()
        val connection = mock(HttpURLConnection::class.java)

        future.addCancellationSignal(signal)
        signal.registerConnection(connection)

        future.cancelAllSignals()

        assertTrue("Signal should be cancelled", signal.isCancelled)
        verify(connection).disconnect()
    }

    @Test
    fun cancelAllSignals_dedupCase_cancelsAllSignalsAndDisconnectsWorkerConnection() {
        val future = FinalizableResultFuture<String>()

        // Signal A — original request's worker (has an active HTTP connection)
        val signalA = CancellationSignal()
        val workerConnection = mock(HttpURLConnection::class.java)
        future.addCancellationSignal(signalA)
        signalA.registerConnection(workerConnection)

        // Signal B — deduplicated request (no worker, no connection)
        val signalB = CancellationSignal()
        future.addCancellationSignal(signalB)

        // Either caller times out → cancelAllSignals()
        future.cancelAllSignals()

        assertTrue("SignalA (worker) should be cancelled", signalA.isCancelled)
        assertTrue("SignalB (dedup) should be cancelled", signalB.isCancelled)
        verify(workerConnection).disconnect()
    }

    @Test
    fun addSignalAfterCancelAll_isNotRetroactivelyCancelled() {
        val future = FinalizableResultFuture<String>()
        val signal1 = CancellationSignal()
        future.addCancellationSignal(signal1)

        future.cancelAllSignals()

        // Late add — simulates a new dedup request arriving after cancel
        val signal2 = CancellationSignal()
        future.addCancellationSignal(signal2)

        assertTrue("signal1 should be cancelled", signal1.isCancelled)
        assertFalse("signal2 should NOT be cancelled (added after cancelAll)", signal2.isCancelled)
    }
}
