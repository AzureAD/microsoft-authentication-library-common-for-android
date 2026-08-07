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
package com.microsoft.identity.common.java.providers

import com.microsoft.identity.common.java.logging.Logger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Tests for [MamCaRedirect] - reading the MAM Conditional Access marker off the broker-install
 * redirect.
 */
class MamCaRedirectTest {

    @Test
    fun isMamCaInstall_onlyWhenMarkerIsExactlyOne() {
        assertTrue(MamCaRedirect.isMamCaInstall(markedRedirect()))

        assertFalse(MamCaRedirect.isMamCaInstall(null))
        assertFalse(MamCaRedirect.isMamCaInstall(emptyMap()))
        assertFalse(MamCaRedirect.isMamCaInstall(redirectWithMarkerValue("0")))
        assertFalse(MamCaRedirect.isMamCaInstall(redirectWithMarkerValue("true")))
        assertFalse(MamCaRedirect.isMamCaInstall(redirectWithMarkerValue("")))
    }

    @Test
    fun isMamCaInstall_plainBrokerInstall_isFalse() {
        // An ordinary device-registration broker install must not pick up MAM-CA behaviors.
        val plainInstall = mapOf(MamCaRedirect.KEY_USERNAME to UPN)

        assertFalse(MamCaRedirect.isMamCaInstall(plainInstall))
    }

    @Test
    fun isMamCaInstall_readsTheServerInstallLinkShape() {
        // Mirrors what the service appends to the broker-install link when it fails the request
        // with AADSTS50127 for MAM: intuneAppProtection is a top-level parameter alongside
        // app_link, not something nested inside it.
        val redirect = mapOf(
            "wpj" to "1",
            MamCaRedirect.KEY_USERNAME to UPN,
            "app_link" to
                "https://play.google.com/store/apps/details?id=com.microsoft.windowsintune.companyportal",
            MamCaRedirect.KEY_INTUNE_APP_PROTECTION to
                MamCaRedirect.VALUE_INTUNE_APP_PROTECTION_ENABLED
        )

        assertTrue(MamCaRedirect.isMamCaInstall(redirect))
        assertEquals(UPN, MamCaRedirect.getUsername(redirect))
    }

    @Test
    fun getUsername_returnsUpnOrNull() {
        assertEquals(UPN, MamCaRedirect.getUsername(markedRedirect()))

        assertNull(MamCaRedirect.getUsername(null))
        assertNull(MamCaRedirect.getUsername(emptyMap()))
        assertNull(MamCaRedirect.getUsername(mapOf(MamCaRedirect.KEY_USERNAME to "")))
    }

    @Test
    fun logRedirectParameterNames_isNullSafe() {
        MamCaRedirect.logRedirectParameterNames("test", null)
        MamCaRedirect.logRedirectParameterNames("test", markedRedirect())
    }

    /**
     * [MamCaRedirect.printableParameterNames] is tested directly below, but that only proves the
     * guard works - not that the public entry point still uses it. This pins the property where it
     * actually matters: on what reaches the log.
     */
    @Test
    fun logRedirectParameterNames_neverPutsAUpnOnTheLog() {
        val redirect = markedRedirect() + (UPN to "")

        val logged = captureLogsWhile {
            MamCaRedirect.logRedirectParameterNames(TAG_UNDER_TEST, redirect)
        }

        assertFalse(
            "A UPN must never reach the log line: $logged",
            logged.any { it.contains(UPN) }
        )
        assertTrue(
            "The parameter names that are safe to print should still be reported: $logged",
            logged.any { it.contains(MamCaRedirect.KEY_INTUNE_APP_PROTECTION) }
        )
    }

    /**
     * A trailing token with no `=` is parsed as a key with no value, so a malformed redirect can
     * present a UPN as a parameter name. Names are logged on the non-PII channel, so anything that
     * is not shaped like a parameter name has to be held back.
     */
    @Test
    fun printableParameterNames_withholdsAnythingThatIsNotAName() {
        val keys = setOf(
            "app_link",
            MamCaRedirect.KEY_INTUNE_APP_PROTECTION,
            MamCaRedirect.KEY_USERNAME,
            UPN,
            "someone else@contoso.com"
        )

        val printable = MamCaRedirect.printableParameterNames(keys)

        assertTrue(printable.contains("app_link"))
        assertTrue(printable.contains(MamCaRedirect.KEY_INTUNE_APP_PROTECTION))
        assertTrue(printable.contains(MamCaRedirect.KEY_USERNAME))
        assertFalse("a UPN must never reach the log line", printable.contains(UPN))
        assertFalse(printable.contains("someone else@contoso.com"))
        assertEquals(3, printable.size)
    }

    private fun markedRedirect(): Map<String, String> =
        redirectWithMarkerValue(MamCaRedirect.VALUE_INTUNE_APP_PROTECTION_ENABLED) +
            (MamCaRedirect.KEY_USERNAME to UPN)

    private fun redirectWithMarkerValue(value: String): Map<String, String> =
        mapOf(MamCaRedirect.KEY_INTUNE_APP_PROTECTION to value)

    /**
     * Runs [block] with a log sink attached and returns the messages tagged by the class under test.
     *
     * [Logger] hands each line to a single-threaded executor, so detaching the callback as soon as
     * [block] returns races the delivery. Emitting a marker afterwards and waiting for it to come
     * back removes the race rather than papering over it with a sleep: the executor is FIFO, so once
     * the marker has arrived, everything [block] logged has arrived too.
     */
    private fun captureLogsWhile(block: () -> Unit): List<String> {
        val captured = CopyOnWriteArrayList<String>()
        val delivered = CountDownLatch(1)
        val identifier = "MamCaRedirectTest-${System.nanoTime()}"

        Logger.setLogger(identifier) { tag, _, message, _ ->
            when {
                tag == FENCE_TAG -> delivered.countDown()
                tag != null && tag.startsWith(TAG_UNDER_TEST) -> captured.add(message)
            }
        }
        try {
            block()
            Logger.info(FENCE_TAG, "Waiting for the log executor to drain.")
            assertTrue(
                "The log executor never drained, so the captured lines cannot be trusted.",
                delivered.await(30, TimeUnit.SECONDS)
            )
        } finally {
            Logger.setLogger(identifier, null)
        }
        return captured.toList()
    }

    companion object {
        private const val UPN = "user@contoso.com"
        private const val TAG_UNDER_TEST = "MamCaRedirectTest-caller"
        private const val FENCE_TAG = "MamCaRedirectTest-fence"
    }
}
