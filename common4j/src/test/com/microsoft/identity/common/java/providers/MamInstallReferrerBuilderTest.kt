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

import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import com.microsoft.identity.common.java.flighting.MockFlightsManager
import com.microsoft.identity.common.java.flighting.MockFlightsProvider
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.util.CommonURIBuilder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Tests for [MamInstallReferrerBuilder] - the MAM Conditional Access Phase 1 install-referrer
 * decoration.
 */
class MamInstallReferrerBuilderTest {

    // region decorateAppLinkWithOriginReferrer (ungated)

    @Test
    fun decorate_appendsBareOriginReferrer() {
        val decorated =
            MamInstallReferrerBuilder.decorateAppLinkWithOriginReferrer(CP_APP_LINK, ORIGIN_PKG)!!

        val params = queryParametersOf(decorated)
        assertEquals(CP_ID, params["id"])
        assertEquals(ORIGIN_PKG, params[MamInstallReferrerBuilder.REFERRER_QUERY_PARAM])
        assertTrue(
            "The Play Store path must be preserved.",
            decorated.startsWith("https://play.google.com/store/apps/details")
        )
    }

    /**
     * The server names the calling app on the link directly. This code can only ever see the package
     * of the process hosting the sign-in UI, which is the broker's when a broker is hosting the flow,
     * so overwriting a server-supplied referrer would replace a right answer with a wrong one.
     */
    @Test
    fun decorate_keepsAServerSuppliedReferrer_ratherThanOverwritingIt() {
        val linkWithReferrer = "$CP_APP_LINK&referrer=com.some.other.app"

        val decorated =
            MamInstallReferrerBuilder.decorateAppLinkWithOriginReferrer(linkWithReferrer, ORIGIN_PKG)!!

        assertEquals(
            "The referrer the server chose has to win.",
            "com.some.other.app",
            queryParametersOf(decorated)[MamInstallReferrerBuilder.REFERRER_QUERY_PARAM]
        )
        assertEquals(
            "Exactly one referrer must survive.",
            1,
            Regex("referrer=").findAll(decorated).count()
        )
    }

    /** Case is not ours to choose - the server's spelling of the key counts as present. */
    @Test
    fun decorate_keepsAServerSuppliedReferrer_whateverItsCase() {
        val linkWithReferrer = "$CP_APP_LINK&Referrer=com.some.other.app"

        val decorated =
            MamInstallReferrerBuilder.decorateAppLinkWithOriginReferrer(linkWithReferrer, ORIGIN_PKG)!!

        assertEquals(
            "Exactly one referrer must survive.",
            1,
            Regex("(?i)referrer=").findAll(decorated).count()
        )
        assertTrue(
            "Our own package must not have been appended: $decorated",
            !decorated.contains(ORIGIN_PKG)
        )
    }

    @Test
    fun decorate_missingInputs_returnsOriginalUnchanged() {
        assertNull(MamInstallReferrerBuilder.decorateAppLinkWithOriginReferrer(null, ORIGIN_PKG))
        assertEquals(
            CP_APP_LINK,
            MamInstallReferrerBuilder.decorateAppLinkWithOriginReferrer(CP_APP_LINK, null)
        )
        assertEquals(
            CP_APP_LINK,
            MamInstallReferrerBuilder.decorateAppLinkWithOriginReferrer(CP_APP_LINK, "")
        )
        assertEquals("", MamInstallReferrerBuilder.decorateAppLinkWithOriginReferrer("", ORIGIN_PKG))
    }

    @Test
    fun decorate_unparseableAppLink_returnsOriginalUnchanged() {
        // A broker install must never be broken by referrer decoration.
        val malformed = "https://play.google.com/store/apps/details?id=x^y|z"

        assertEquals(
            malformed,
            MamInstallReferrerBuilder.decorateAppLinkWithOriginReferrer(malformed, ORIGIN_PKG)
        )
    }

    /**
     * The embedded WebView rewrites a `browser://` app_link to `https://` before decorating it; the
     * custom-tab and browser fragments launch whatever the server sent. Decoration is scheme-
     * agnostic either way - it appends the referrer and hands the link back with the scheme it
     * arrived with, so neither launch path is changed by the presence of the referrer.
     */
    @Test
    fun decorate_nonHttpsScheme_isStillDecoratedAndKeepsItsScheme() {
        val browserSchemeLink = "browser://play.google.com/store/apps/details?id=$CP_ID"

        val decorated =
            MamInstallReferrerBuilder.decorateAppLinkWithOriginReferrer(browserSchemeLink, ORIGIN_PKG)!!

        assertTrue(decorated.startsWith("browser://"))
        assertEquals(
            ORIGIN_PKG,
            queryParametersOf(decorated)[MamInstallReferrerBuilder.REFERRER_QUERY_PARAM]
        )
    }

    // endregion

    // region decorateAppLinkForMamCaInstall (flight- and marker-gated)

    @Test
    fun gated_flightOnAndMarkerPresent_decorates() {
        setMamCaReferrerFlight(true)

        assertEquals(
            "$CP_APP_LINK&referrer=$ORIGIN_PKG",
            MamInstallReferrerBuilder.decorateAppLinkForMamCaInstall(
                CP_APP_LINK, ORIGIN_PKG, mamCaRedirectParameters()
            )
        )
    }

    @Test
    fun gated_flightOff_returnsOriginalUnchanged() {
        setMamCaReferrerFlight(false)

        assertEquals(
            CP_APP_LINK,
            MamInstallReferrerBuilder.decorateAppLinkForMamCaInstall(
                CP_APP_LINK, ORIGIN_PKG, mamCaRedirectParameters()
            )
        )
    }

    @Test
    fun gated_flightOnButNotAMamCaInstall_returnsOriginalUnchanged() {
        // An ordinary device-registration broker install must keep behaving exactly as it does today.
        setMamCaReferrerFlight(true)

        val plainInstall = mapOf(
            "username" to "user@contoso.com",
            "app_link" to CP_APP_LINK
        )

        assertEquals(
            CP_APP_LINK,
            MamInstallReferrerBuilder.decorateAppLinkForMamCaInstall(
                CP_APP_LINK, ORIGIN_PKG, plainInstall
            )
        )
    }

    @Test
    fun gated_flightOnButMarkerNotEnabledValue_returnsOriginalUnchanged() {
        setMamCaReferrerFlight(true)

        val disabledMarker = mapOf(MamCaRedirect.KEY_INTUNE_APP_PROTECTION to "0")

        assertEquals(
            CP_APP_LINK,
            MamInstallReferrerBuilder.decorateAppLinkForMamCaInstall(
                CP_APP_LINK, ORIGIN_PKG, disabledMarker
            )
        )
    }

    @Test
    fun gated_nullRedirectParameters_returnsOriginalUnchanged() {
        setMamCaReferrerFlight(true)

        assertEquals(
            CP_APP_LINK,
            MamInstallReferrerBuilder.decorateAppLinkForMamCaInstall(CP_APP_LINK, ORIGIN_PKG, null)
        )
    }

    @Test
    fun gated_missingPackage_returnsOriginalUnchanged() {
        setMamCaReferrerFlight(true)

        assertEquals(
            CP_APP_LINK,
            MamInstallReferrerBuilder.decorateAppLinkForMamCaInstall(
                CP_APP_LINK, null, mamCaRedirectParameters()
            )
        )
        assertEquals(
            CP_APP_LINK,
            MamInstallReferrerBuilder.decorateAppLinkForMamCaInstall(
                CP_APP_LINK, "", mamCaRedirectParameters()
            )
        )
    }

    @Test
    fun gated_noFlightsManager_defaultsOff_returnsOriginalUnchanged() {
        // With no flights manager initialized, the CommonFlight default (false) applies.
        assertEquals(
            CP_APP_LINK,
            MamInstallReferrerBuilder.decorateAppLinkForMamCaInstall(
                CP_APP_LINK, ORIGIN_PKG, mamCaRedirectParameters()
            )
        )
    }

    /**
     * Turning the flight off has to be a complete kill switch, and a log line is a side effect like
     * any other: a tenant that never opted in should not start emitting new diagnostics because the
     * code shipped.
     */
    @Test
    fun gated_flightOff_logsNothingAtAll() {
        setMamCaReferrerFlight(false)

        val logged = captureLogsWhile {
            MamInstallReferrerBuilder.decorateAppLinkForMamCaInstall(
                CP_APP_LINK, ORIGIN_PKG, mamCaRedirectParameters()
            )
        }

        assertTrue(
            "The flight is off, so this class should have logged nothing: $logged",
            logged.isEmpty()
        )
    }

    /**
     * With the flight on, the parameter names are logged whether or not the marker is there.
     * Whether the server has started marking MAM-CA installs is exactly what is being rolled out,
     * and it cannot be read off a log that only fires once the marker has arrived.
     */
    @Test
    fun gated_flightOnButUnmarkedInstall_stillReportsWhatTheRedirectCarried() {
        setMamCaReferrerFlight(true)

        val logged = captureLogsWhile {
            MamInstallReferrerBuilder.decorateAppLinkForMamCaInstall(
                CP_APP_LINK, ORIGIN_PKG, mapOf("username" to "user@contoso.com")
            )
        }

        assertTrue(
            "The unmarked case is the one worth reporting: $logged",
            logged.any { it.contains("Broker-install redirect carried parameters") }
        )
    }

    // endregion

    @After
    fun tearDown() {
        CommonFlightsManager.resetFlightsManager()
    }

    private fun mamCaRedirectParameters(): Map<String, String> = mapOf(
        "username" to "user@contoso.com",
        "app_link" to CP_APP_LINK,
        MamCaRedirect.KEY_INTUNE_APP_PROTECTION to MamCaRedirect.VALUE_INTUNE_APP_PROTECTION_ENABLED
    )

    private fun queryParametersOf(url: String): Map<String, String> =
        CommonURIBuilder(url).queryParams.associate { it.name to it.value }

    /** Enables or disables the MAM-CA install referrer flight for the duration of a test. */
    private fun setMamCaReferrerFlight(enabled: Boolean) {
        val provider = MockFlightsProvider()
        provider.addFlight(CommonFlight.ENABLE_MAM_CA_INSTALL_REFERRER.key, enabled.toString())
        val manager = MockFlightsManager()
        manager.setMockBrokerFlightsProvider(provider)
        CommonFlightsManager.initializeCommonFlightsManager(manager)
    }

    /**
     * Runs [block] with a log sink attached and returns the messages this class emitted while it ran.
     *
     * [Logger] hands each line to a single-threaded executor, so a callback that is detached as soon
     * as [block] returns races the delivery. Emitting a marker afterwards and waiting for it to come
     * back removes the race rather than papering over it with a sleep: the executor is FIFO, so once
     * the marker has arrived, everything [block] logged has arrived too.
     *
     * Only lines tagged by the class under test are returned, so an unrelated subsystem logging on
     * its own schedule cannot decide whether an assertion passes.
     */
    private fun captureLogsWhile(block: () -> Unit): List<String> {
        val captured = CopyOnWriteArrayList<String>()
        val delivered = CountDownLatch(1)
        val identifier = "MamInstallReferrerBuilderTest-${System.nanoTime()}"

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
        private const val CP_ID = "com.microsoft.windowsintune.companyportal"
        private const val CP_APP_LINK = "https://play.google.com/store/apps/details?id=$CP_ID"
        private const val ORIGIN_PKG = "com.microsoft.office.outlook"

        /** Every line this class logs is tagged with its own name, method suffix aside. */
        private const val TAG_UNDER_TEST = "MamInstallReferrerBuilder"
        private const val FENCE_TAG = "MamInstallReferrerBuilderTest-fence"
    }
}
