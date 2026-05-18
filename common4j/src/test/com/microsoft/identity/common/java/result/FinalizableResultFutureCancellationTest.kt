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
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.net.HttpURLConnection

/**
 * Tests for cancellation signal support in [FinalizableResultFuture].
 * Validates single-signal-per-future design: [getCancellationSignal] and [cancelSignal].
 */
class FinalizableResultFutureCancellationTest {

    @Test
    fun cancelSignal_cancelsAndDisconnectsWorkerConnection() {
        val future = FinalizableResultFuture<String>()
        val signal = future.cancellationSignal
        val connection = mock(HttpURLConnection::class.java)

        signal.registerOnCancel { connection.disconnect() }

        future.cancelSignal()

        assertTrue("Signal should be cancelled", signal.isCancelled)
        verify(connection).disconnect()
    }

    @Test
    fun cancelSignal_dedupCase_sharedSignalDisconnectsWorkerConnection() {
        val future = FinalizableResultFuture<String>()

        // Both callers share the same signal from the future
        val signal = future.cancellationSignal
        val workerConnection = mock(HttpURLConnection::class.java)
        signal.registerOnCancel() { workerConnection.disconnect() }

        // Any caller times out → cancelSignal()
        future.cancelSignal()

        assertTrue("Signal should be cancelled", signal.isCancelled)
        verify(workerConnection).disconnect()
    }

    @Test
    fun getCancellationSignal_returnsSameInstance() {
        val future = FinalizableResultFuture<String>()
        val signal1 = future.cancellationSignal
        val signal2 = future.cancellationSignal
        assertSame("Should return same signal instance", signal1, signal2)
    }
}
