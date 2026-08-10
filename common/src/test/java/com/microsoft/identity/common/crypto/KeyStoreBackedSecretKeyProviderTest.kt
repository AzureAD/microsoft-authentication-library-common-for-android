// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.crypto

import com.microsoft.identity.common.java.exception.ClientException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Tests for the wipe-telemetry root-cause resolution used by
 * [KeyStoreBackedSecretKeyProvider.readSecretKeyFromStorage]. These verify that the real underlying
 * failure (not the generic [ClientException] wrapper) is recovered, that the full cause message is
 * preserved, and that pathological cause chains cannot crash or hang the read/wipe path.
 */
class KeyStoreBackedSecretKeyProviderTest {

    /**
     * A [Throwable] whose cause can be redirected to any node (including itself), used to build
     * self-referential and multi-node cause cycles that a normal [Throwable.initCause] forbids.
     */
    private class LoopingThrowable(message: String) : Throwable(message) {
        var link: Throwable? = null
        override val cause: Throwable? get() = link
    }

    @Test
    fun findRootCause_returnsThrowableItself_whenNoCause() {
        val solo = ClientException("io_error", "no wrapped cause")

        assertSame(solo, KeyStoreBackedSecretKeyProvider.findRootCause(solo))
    }

    @Test
    fun findRootCause_returnsDeepestCause_forNestedChain() {
        val root = IllegalStateException("keystore hardware unavailable")
        val middle = RuntimeException("unwrap failed", root)
        val wrapper = ClientException("failed_to_load_key", "generic wrapper message", middle)

        assertSame(root, KeyStoreBackedSecretKeyProvider.findRootCause(wrapper))
    }

    @Test
    fun findRootCause_preservesFullRootCauseMessage() {
        // Longer than the previously-hardcoded 256-char cap to prove the message is no longer truncated.
        val longMessage = "x".repeat(1024)
        val root = IllegalStateException(longMessage)
        val wrapper = ClientException("failed_to_load_key", "wrapper", root)

        val rootCause = KeyStoreBackedSecretKeyProvider.findRootCause(wrapper)

        assertEquals(longMessage, rootCause.message)
    }

    @Test(timeout = 5_000)
    fun findRootCause_terminates_onSelfReferentialCause() {
        val self = LoopingThrowable("self-caused")
        self.link = self

        assertSame(self, KeyStoreBackedSecretKeyProvider.findRootCause(self))
    }

    @Test(timeout = 5_000)
    fun findRootCause_terminates_onMultiNodeCycle() {
        val a = LoopingThrowable("A")
        val b = LoopingThrowable("B")
        a.link = b
        b.link = a

        // Must terminate (guarded by timeout) and return the last unvisited node before the cycle closes.
        assertSame(b, KeyStoreBackedSecretKeyProvider.findRootCause(a))
    }
}
