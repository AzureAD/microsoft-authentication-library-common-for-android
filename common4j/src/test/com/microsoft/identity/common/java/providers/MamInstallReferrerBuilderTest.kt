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

    // region outcome reporting

    /**
     * The outcome is the ramp-safety signal for this flight: it is what makes "has the server
     * started marking MAM-CA installs, and are we tagging the marked ones?" answerable from
     * telemetry rather than from device logs. Each bail-out therefore has to be distinguishable
     * from the others, not collapsed into a single "not decorated".
     */
    @Test
    fun outcome_reportsWhyTheLinkWasOrWasNotDecorated() {
        setMamCaReferrerFlight(true)

        assertEquals(
            MamInstallReferrerBuilder.Outcome.DECORATED,
            outcomeOf(CP_APP_LINK, ORIGIN_PKG, mamCaRedirectParameters())
        )
        assertEquals(
            MamInstallReferrerBuilder.Outcome.NOT_MAM_CA,
            outcomeOf(CP_APP_LINK, ORIGIN_PKG, mapOf("username" to "user@contoso.com"))
        )
        assertEquals(
            MamInstallReferrerBuilder.Outcome.NO_ORIGIN_PKG,
            outcomeOf(CP_APP_LINK, null, mamCaRedirectParameters())
        )
        assertEquals(
            MamInstallReferrerBuilder.Outcome.NO_APP_LINK,
            outcomeOf(null, ORIGIN_PKG, mamCaRedirectParameters())
        )
        assertEquals(
            MamInstallReferrerBuilder.Outcome.SERVER_REFERRER,
            outcomeOf("$CP_APP_LINK&referrer=com.server.chose.this", ORIGIN_PKG, mamCaRedirectParameters())
        )
        assertEquals(
            MamInstallReferrerBuilder.Outcome.LINK_UNPARSEABLE,
            outcomeOf("not a uri at all", ORIGIN_PKG, mamCaRedirectParameters())
        )
    }

    /**
     * The marker is checked before the package name, so a host that cannot name itself is still
     * reported as a bail-out on a *marked* redirect. Collapsing the two would hide whether the
     * server marking had arrived, which is the more valuable of the two signals.
     */
    @Test
    fun outcome_missingPackageOnAnUnmarkedInstall_reportsTheMarkerNotThePackage() {
        setMamCaReferrerFlight(true)

        assertEquals(
            MamInstallReferrerBuilder.Outcome.NOT_MAM_CA,
            outcomeOf(CP_APP_LINK, null, mapOf("username" to "user@contoso.com"))
        )
    }

    /**
     * With the flight off nothing is evaluated and nothing is reported - the null tag is what keeps
     * the kill switch complete, telemetry included.
     */
    @Test
    fun outcome_flightOff_isNotReportedAtAll() {
        setMamCaReferrerFlight(false)

        val outcome = outcomeOf(CP_APP_LINK, ORIGIN_PKG, mamCaRedirectParameters())
        assertEquals(MamInstallReferrerBuilder.Outcome.FLIGHT_OFF, outcome)
        assertNull("The flight-off outcome must not be reportable.", outcome.tag)
    }

    /** Every reported outcome needs a distinct, non-blank tag, or the funnel cannot be sliced. */
    @Test
    fun outcome_reportableTagsAreDistinctAndNonBlank() {
        val tags = MamInstallReferrerBuilder.Outcome.values().mapNotNull { it.tag }

        assertEquals(
            "Every outcome except FLIGHT_OFF is reportable.",
            MamInstallReferrerBuilder.Outcome.values().size - 1,
            tags.size
        )
        assertTrue("Tags must not be blank: $tags", tags.none { it.isBlank() })
        assertEquals("Tags must be distinct: $tags", tags.size, tags.toSet().size)
    }

    /** The reporting overload must not change what is actually launched. */
    @Test
    fun outcome_overloadReturnsTheSameLinkAsTheStringEntryPoint() {
        setMamCaReferrerFlight(true)

        for (params in listOf(mamCaRedirectParameters(), mapOf("username" to "user@contoso.com"))) {
            assertEquals(
                MamInstallReferrerBuilder.decorateAppLinkForMamCaInstall(CP_APP_LINK, ORIGIN_PKG, params),
                outcomeDecorationOf(CP_APP_LINK, ORIGIN_PKG, params).appLink
            )
        }
    }

    // endregion

    @After
    fun tearDown() {
        CommonFlightsManager.resetFlightsManager()
    }

    private fun outcomeDecorationOf(
        appLink: String?,
        originPkg: String?,
        redirectParameters: Map<String, String>?
    ): MamInstallReferrerBuilder.Decoration =
        MamInstallReferrerBuilder.decorateAppLinkForMamCaInstallWithOutcome(
            appLink, originPkg, redirectParameters
        )

    private fun outcomeOf(
        appLink: String?,
        originPkg: String?,
        redirectParameters: Map<String, String>?
    ): MamInstallReferrerBuilder.Outcome =
        outcomeDecorationOf(appLink, originPkg, redirectParameters).outcome

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
