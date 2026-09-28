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
package com.microsoft.identity.common.internal.telemetry

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.microsoft.identity.common.java.logging.DiagnosticContext
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * AB#3708195: contract tests for the process-static recorder handoff between the broker's
 * `AccountChooser` (owner) and `WebViewAuthorizationFragment` (consumer).
 *
 * This is a singleton holding recorders for the life of the broker `:auth` process, so the cases
 * that matter are correlation-id keying (one request must never see another's recorder — including
 * via the shared `UNSET` sentinel), removal (a missed unregister leaks the recorder and its
 * collected steps / blocking errors until eviction), and absent-key behaviour (the no-seed path must
 * get null rather than throw).
 */
@RunWith(RobolectricTestRunner::class)
class OnboardingRecorderRegistryTest {

    @Before
    fun setup() = OnboardingRecorderRegistry.clearForTest()

    @After
    fun tearDown() = OnboardingRecorderRegistry.clearForTest()

    // --- Keying ---

    @Test
    fun testRegisterThenGet_ReturnsSameInstance() {
        val recorder = newRecorder()

        OnboardingRecorderRegistry.register("correlation-a", recorder)

        Assert.assertSame(recorder, OnboardingRecorderRegistry.get("correlation-a"))
    }

    @Test
    fun testGet_WithDifferentCorrelationId_DoesNotCrossWire() {
        // The failure this guards is two concurrent requests sharing a recorder, which would merge
        // one flow's blocking errors and steps into the other's uploaded blob.
        val recorderA = newRecorder()
        val recorderB = newRecorder()
        OnboardingRecorderRegistry.register("correlation-a", recorderA)
        OnboardingRecorderRegistry.register("correlation-b", recorderB)

        Assert.assertSame(recorderA, OnboardingRecorderRegistry.get("correlation-a"))
        Assert.assertSame(recorderB, OnboardingRecorderRegistry.get("correlation-b"))
        Assert.assertNotSame(
            OnboardingRecorderRegistry.get("correlation-a"),
            OnboardingRecorderRegistry.get("correlation-b")
        )
    }

    @Test
    fun testRegister_SameCorrelationIdTwice_LastWins() {
        val first = newRecorder()
        val second = newRecorder()

        OnboardingRecorderRegistry.register("correlation-a", first)
        OnboardingRecorderRegistry.register("correlation-a", second)

        Assert.assertSame(second, OnboardingRecorderRegistry.get("correlation-a"))
        Assert.assertEquals("re-registering must replace, not accumulate", 1, OnboardingRecorderRegistry.size())
    }

    // --- Absent keys / null-and-empty inputs ---

    @Test
    fun testGet_UnknownCorrelationId_ReturnsNullNotThrow() {
        // The MSAL-client path: no onboarding seed, so nothing is ever registered. The WebView host
        // calls get() unconditionally and must simply stay inert.
        Assert.assertNull(OnboardingRecorderRegistry.get("never-registered"))
    }

    @Test
    fun testGet_NullOrEmptyCorrelationId_ReturnsNull() {
        OnboardingRecorderRegistry.register("correlation-a", newRecorder())

        Assert.assertNull(OnboardingRecorderRegistry.get(null))
        Assert.assertNull(OnboardingRecorderRegistry.get(""))
    }

    @Test
    fun testRegister_NullOrEmptyCorrelationId_IsNoOp() {
        OnboardingRecorderRegistry.register(null, newRecorder())
        OnboardingRecorderRegistry.register("", newRecorder())

        Assert.assertEquals(0, OnboardingRecorderRegistry.size())
    }

    @Test
    fun testRegister_NullRecorder_IsNoOp() {
        OnboardingRecorderRegistry.register("correlation-a", null)

        Assert.assertEquals(0, OnboardingRecorderRegistry.size())
        Assert.assertNull(OnboardingRecorderRegistry.get("correlation-a"))
    }

    // --- Removal / lifecycle ---

    @Test
    fun testUnregister_RemovesOnlyTheTargetedEntry() {
        val recorderB = newRecorder()
        OnboardingRecorderRegistry.register("correlation-a", newRecorder())
        OnboardingRecorderRegistry.register("correlation-b", recorderB)

        OnboardingRecorderRegistry.unregister("correlation-a")

        Assert.assertNull("the released recorder must not be retrievable", OnboardingRecorderRegistry.get("correlation-a"))
        Assert.assertSame("an unrelated request must be untouched", recorderB, OnboardingRecorderRegistry.get("correlation-b"))
        Assert.assertEquals(1, OnboardingRecorderRegistry.size())
    }

    @Test
    fun testUnregister_UnknownOrNullCorrelationId_IsSafeNoOp() {
        OnboardingRecorderRegistry.register("correlation-a", newRecorder())

        OnboardingRecorderRegistry.unregister("never-registered")
        OnboardingRecorderRegistry.unregister(null)
        OnboardingRecorderRegistry.unregister("")

        Assert.assertEquals(1, OnboardingRecorderRegistry.size())
    }

    @Test
    fun testUnregister_Twice_IsIdempotent() {
        // The broker unregisters in a finally on the finalize path; a retried or doubled teardown
        // must not throw.
        OnboardingRecorderRegistry.register("correlation-a", newRecorder())

        OnboardingRecorderRegistry.unregister("correlation-a")
        OnboardingRecorderRegistry.unregister("correlation-a")

        Assert.assertEquals(0, OnboardingRecorderRegistry.size())
    }

    // --- Leak bound ---

    @Test
    fun testRegistry_IsBoundedWhenCallersFailToUnregister() {
        // This registry is process-static in the long-lived broker :auth process, so a terminal path
        // that forgets to unregister would leak a recorder (and its Context) permanently. Simulate
        // that: register far past the cap and never release.
        repeat(100) { i -> OnboardingRecorderRegistry.register("correlation-$i", newRecorder()) }

        Assert.assertEquals(
            "a missed unregister must be a bounded leak, not an unbounded one",
            16, OnboardingRecorderRegistry.size()
        )
        Assert.assertNull("the oldest entries must have been evicted", OnboardingRecorderRegistry.get("correlation-0"))
        Assert.assertNotNull("the newest entry must survive", OnboardingRecorderRegistry.get("correlation-99"))
    }

    @Test
    fun testEviction_KeepsTheEntryStillBeingUsed() {
        // Eviction is least-recently-USED, not least-recently-registered. Today's host resolves its
        // recorder once and holds the reference, so this is not load-bearing for the current flow —
        // it is the safer default if a future caller ever re-resolves mid-request.
        val liveRecorder = newRecorder()
        OnboardingRecorderRegistry.register("live-request", liveRecorder)

        repeat(20) { i ->
            OnboardingRecorderRegistry.register("leaked-$i", newRecorder())
            // Simulates a caller that re-resolves rather than caching the reference.
            Assert.assertSame(liveRecorder, OnboardingRecorderRegistry.get("live-request"))
        }

        Assert.assertSame(
            "the entry in active use must survive eviction pressure",
            liveRecorder, OnboardingRecorderRegistry.get("live-request")
        )
    }

    // --- Correlation-id sentinel ---

    @Test
    fun testRegister_UnsetSentinelCorrelationId_IsRejected() {
        // DiagnosticContext seeds every thread's request context with UNSET, and the authorization
        // Intent extra is populated from that map directly rather than through
        // getThreadCorrelationId(), so the raw sentinel can reach this registry. It is shared by
        // definition: accepting it would let two unrelated requests resolve to the same recorder and
        // merge one flow's blocking errors into the other's uploaded blob.
        OnboardingRecorderRegistry.register(DiagnosticContext.UNSET_CORRELATION_ID, newRecorder())

        Assert.assertEquals("the sentinel must never become a key", 0, OnboardingRecorderRegistry.size())
        Assert.assertNull(OnboardingRecorderRegistry.get(DiagnosticContext.UNSET_CORRELATION_ID))
    }

    @Test
    fun testUnsetSentinel_CannotCrossWireTwoRequests() {
        // The concrete failure the guard prevents: two requests whose threads never had a request
        // context both fall back to the sentinel. Without the guard the second registration silently
        // displaces the first and BOTH WebView hosts resolve to request two's recorder.
        OnboardingRecorderRegistry.register(DiagnosticContext.UNSET_CORRELATION_ID, newRecorder())
        OnboardingRecorderRegistry.register(DiagnosticContext.UNSET_CORRELATION_ID, newRecorder())

        Assert.assertEquals(0, OnboardingRecorderRegistry.size())
        Assert.assertNull(
            "inert is the correct failure; sharing a recorder would mis-attribute telemetry",
            OnboardingRecorderRegistry.get(DiagnosticContext.UNSET_CORRELATION_ID)
        )
    }

    @Test
    fun testUnregister_UnsetSentinel_DoesNotDisturbRealEntries() {
        val real = newRecorder()
        OnboardingRecorderRegistry.register("correlation-a", real)

        OnboardingRecorderRegistry.unregister(DiagnosticContext.UNSET_CORRELATION_ID)

        Assert.assertSame(real, OnboardingRecorderRegistry.get("correlation-a"))
        Assert.assertEquals(1, OnboardingRecorderRegistry.size())
    }

    private fun newRecorder(): OnboardingTelemetryRecorder = OnboardingTelemetryRecorder(
        "{\"schema_version\":\"1.0.0\"," +
            "\"session_correlation_id\":\"test-uuid-123\"," +
            "\"onboarding_mode\":\"brokered\"}",
        "test-client-id",
        "scope1",
        ApplicationProvider.getApplicationContext<Context>()
    )
}
