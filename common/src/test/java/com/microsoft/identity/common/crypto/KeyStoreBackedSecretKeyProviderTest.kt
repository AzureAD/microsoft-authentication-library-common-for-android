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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.microsoft.identity.common.internal.util.AndroidKeyStoreUtil
import com.microsoft.identity.common.java.exception.ClientException
import io.mockk.every
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Tests for the wipe-telemetry root-cause resolution used by
 * [KeyStoreBackedSecretKeyProvider.readSecretKeyFromStorage]. These verify that the real underlying
 * failure (not the generic [ClientException] wrapper) is recovered, that the full cause message is
 * preserved, and that pathological cause chains cannot crash or hang the read/wipe path.
 */
@RunWith(RobolectricTestRunner::class)
class KeyStoreBackedSecretKeyProviderTest {

    private companion object {
        const val KEY_ALIAS = "test-key-alias"
        const val KEY_FILE_PATH = "test-wrapped-key"
    }

    private lateinit var keyProvider: KeyStoreBackedSecretKeyProvider
    private lateinit var keyFile: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        keyProvider = KeyStoreBackedSecretKeyProvider(context, KEY_ALIAS, KEY_FILE_PATH)
        keyFile = File(context.getDir(context.packageName, Context.MODE_PRIVATE), KEY_FILE_PATH)
        keyFile.writeBytes(byteArrayOf(1, 2, 3))
        mockkStatic(AndroidKeyStoreUtil::class)
    }

    @After
    fun tearDown() {
        keyFile.delete()
        keyProvider.clearKeyFromCache()
        unmockkAll()
    }

    /**
     * A [Throwable] whose cause can be redirected to any node (including itself), used to build
     * self-referential and multi-node cause cycles that a normal [Throwable.initCause] forbids.
     */
    private class LoopingThrowable(message: String) : Throwable(message) {
        var link: Throwable? = null
        override val cause: Throwable? get() = link
    }

    @Test
    fun shouldPreserveKeyData_returnsTrue_onlyForTransientErrors() {
        assertTrue(
            KeyStoreBackedSecretKeyProvider.shouldPreserveKeyData(
                AndroidKeyStoreUtil.KeyStoreErrorTransience.TRANSIENT
            )
        )

        AndroidKeyStoreUtil.KeyStoreErrorTransience.values()
            .filterNot { it == AndroidKeyStoreUtil.KeyStoreErrorTransience.TRANSIENT }
            .forEach { transience ->
                assertFalse(
                    "$transience errors must wipe key data",
                    KeyStoreBackedSecretKeyProvider.shouldPreserveKeyData(transience)
                )
            }
    }

    @Test
    fun readSecretKeyFromStorage_preservesKeyData_forTransientKeyStoreError() {
        val readException = ClientException("read_error", "transient read failure")
        every { AndroidKeyStoreUtil.readKey(KEY_ALIAS) } throws readException
        every { AndroidKeyStoreUtil.getKeyStoreErrorTransience(readException) } returns
            AndroidKeyStoreUtil.KeyStoreErrorTransience.TRANSIENT

        val thrown = try {
            keyProvider.readSecretKeyFromStorage()
            throw AssertionError("Expected read failure")
        } catch (exception: ClientException) {
            exception
        }

        assertSame(readException, thrown)
        assertTrue("Transient failures must preserve the wrapped-key file", keyFile.exists())
        verify(exactly = 0) { AndroidKeyStoreUtil.deleteKey(any()) }
    }

    @Test
    fun readSecretKeyFromStorage_wipesKeyData_forPermanentKeyStoreError() {
        val readException = ClientException("read_error", "permanent read failure")
        every { AndroidKeyStoreUtil.readKey(KEY_ALIAS) } throws readException
        every { AndroidKeyStoreUtil.getKeyStoreErrorTransience(readException) } returns
            AndroidKeyStoreUtil.KeyStoreErrorTransience.NOT_TRANSIENT
        every { AndroidKeyStoreUtil.deleteKey(KEY_ALIAS) } just runs

        val thrown = try {
            keyProvider.readSecretKeyFromStorage()
            throw AssertionError("Expected read failure")
        } catch (exception: ClientException) {
            exception
        }

        assertSame(readException, thrown)
        assertFalse("Permanent failures must delete the wrapped-key file", keyFile.exists())
        verify(exactly = 1) { AndroidKeyStoreUtil.deleteKey(KEY_ALIAS) }
    }

    @Test
    fun readSecretKeyFromStorage_suppressesCleanupFailure_andStillDeletesFile() {
        val readException = ClientException("read_error", "read failure")
        val cleanupException = ClientException("cleanup_error", "cleanup failure")
        every { AndroidKeyStoreUtil.readKey(KEY_ALIAS) } throws readException
        every { AndroidKeyStoreUtil.getKeyStoreErrorTransience(readException) } returns
            AndroidKeyStoreUtil.KeyStoreErrorTransience.NOT_TRANSIENT
        every { AndroidKeyStoreUtil.deleteKey(KEY_ALIAS) } throws cleanupException

        val thrown = try {
            keyProvider.readSecretKeyFromStorage()
            throw AssertionError("Expected read failure")
        } catch (exception: ClientException) {
            exception
        }

        assertSame(readException, thrown)
        assertEquals(listOf(cleanupException), thrown.suppressed.toList())
        assertFalse("File cleanup must run when KeyStore deletion fails", keyFile.exists())
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
