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

import androidx.annotation.GuardedBy
import androidx.annotation.VisibleForTesting
import com.microsoft.identity.common.java.logging.DiagnosticContext
import com.microsoft.identity.common.logging.Logger

/**
 * In-process handoff for the onboarding telemetry recorder between the component that OWNS it and
 * the interactive WebView host that CONSUMES it.
 *
 * On the broker side the recorder is built per-request from the onboarding seed by
 * `AccountChooser` (main broker logic), while the WebView that renders the interactive auth /
 * remediation pages lives in a separate `AuthorizationActivity` created by the OS from an Intent.
 * A live recorder instance cannot ride an Intent, but both components run in the broker `:auth`
 * process, so a process-static registry keyed by the request **correlationId** bridges them:
 *
 *  - `AccountChooser` [register]s its recorder when it seeds one.
 *  - [com.microsoft.identity.common.internal.providers.oauth2.WebViewAuthorizationFragment] looks it
 *    up by correlationId and hands it to
 *    [com.microsoft.identity.common.internal.ui.webview.AzureActiveDirectoryWebViewClient.setOnboardingTelemetryRecorder]
 *    so WebView-observed onboarding steps (MDM enrollment, Company Portal launch, broker install)
 *    and the Auth UX `log_telemetry` error code are recorded into the same blob that is finalized
 *    and returned in the broker result.
 *
 * Entries MUST be [unregister]ed on terminal outcome / teardown to avoid leaking recorders across
 * requests. Stores the concrete [OnboardingTelemetryRecorder] because the WebView client's
 * telemetry hooks (e.g. `setLastLoadedDomain`) use methods that are not on the common4j interface.
 *
 * **The correlation id must be a real request id.** [DiagnosticContext.UNSET_CORRELATION_ID] is the
 * value every thread carries before its request context is set, and the authorization Intent extra
 * is populated by reading the request context map directly rather than through
 * `getThreadCorrelationId()` — so the raw sentinel can reach this class. It is shared by definition,
 * so accepting it as a key would let two unrelated requests resolve to the same recorder and merge
 * one flow's blocking errors into the other's uploaded blob. All three accessors reject it: the
 * feature goes inert (and [register] warns) instead of silently mis-attributing telemetry, which for
 * a component whose entire purpose is correct attribution is the only acceptable failure.
 *
 * Because this is process-static state in the long-lived broker `:auth` process, a missed
 * [unregister] would leak a recorder permanently — the recorder object and its collected steps /
 * blocking errors, not an Activity, since [OnboardingTelemetryRecorder] deliberately holds only the
 * application context. The map is therefore capped at [MAX_ENTRIES]: once full, registering evicts
 * the least-recently-used entry and logs a warning. That converts an unbounded leak into a bounded
 * one and makes the underlying bug visible, rather than hiding it. The cap is far above real
 * concurrency (the broker drives one interactive request at a time), so eviction should not happen
 * at all; if the warning appears, it is the signal that a terminal path is failing to unregister.
 *
 * Eviction is least-recently-*used* rather than oldest-registered. In today's flow that is not
 * load-bearing — the WebView host calls [get] exactly once per request and holds the reference
 * thereafter, so evicting a live entry would not disturb the in-flight request anyway — but it is
 * free, and it is the safer default if a future caller ever re-resolves mid-request.
 */
object OnboardingRecorderRegistry {

    private val TAG = OnboardingRecorderRegistry::class.java.simpleName

    /**
     * Upper bound on concurrently-registered recorders. Generous relative to real concurrency; it
     * exists to bound a leak from a missed [unregister], not to constrain legitimate use.
     */
    private const val MAX_ENTRIES = 16

    // Access-ordered LinkedHashMap so eviction drops the least-recently-touched entry rather than
    // simply the oldest-registered one. Guarded by its own monitor: LinkedHashMap is not
    // thread-safe, and get() mutates access order, so reads need the lock too.
    private val recorders = object : LinkedHashMap<String, OnboardingTelemetryRecorder>(
        /* initialCapacity = */ MAX_ENTRIES,
        /* loadFactor = */ 0.75f,
        /* accessOrder = */ true
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, OnboardingTelemetryRecorder>?
        ): Boolean {
            if (size <= MAX_ENTRIES) {
                return false
            }
            // Recorded rather than logged here: this runs inside put(), which runs under the
            // registry lock, and every other log on this class is emitted outside it.
            evictedKey = eldest?.key
            return true
        }
    }

    // Written by removeEldestEntry, which runs inside put() and therefore already under the registry
    // lock, then read and cleared by register() in that same critical section. Only the resulting
    // warning is logged after the lock is released, so nothing on this class logs on-lock.
    @GuardedBy("recorders")
    private var evictedKey: String? = null

    /**
     * Returns [correlationId] when it can safely key an entry — present, and not
     * [DiagnosticContext.UNSET_CORRELATION_ID] — or null when it cannot.
     *
     * The sentinel is the value carried by any thread whose request context was never set, so it is
     * shared rather than unique. Keying on it would let unrelated requests resolve to the same
     * recorder — see this class's KDoc.
     *
     * Returns the key rather than a Boolean so callers get a non-null String to index the map with.
     */
    private fun usableKeyOrNull(correlationId: String?): String? =
        if (!correlationId.isNullOrEmpty() &&
            correlationId != DiagnosticContext.UNSET_CORRELATION_ID
        ) {
            correlationId
        } else {
            null
        }

    /**
     * Register [recorder] for [correlationId]. No-op when the recorder is null or the correlation id
     * is not usable as a key (null, empty, or the unset sentinel), which is logged because a seeded
     * recorder that cannot be handed off is a real defect.
     */
    @JvmStatic
    fun register(correlationId: String?, recorder: OnboardingTelemetryRecorder?) {
        if (recorder == null) {
            return
        }
        val key = usableKeyOrNull(correlationId)
        if (key == null) {
            Logger.warn(
                TAG, correlationId,
                "Not registering the onboarding recorder: the correlation id is missing or is the " +
                    "unset sentinel, so it cannot identify this request. Onboarding telemetry will " +
                    "be inert for it."
            )
            return
        }
        val evicted = synchronized(recorders) {
            recorders[key] = recorder
            evictedKey.also { evictedKey = null }
        }
        Logger.info(TAG, key, "Registered onboarding recorder")
        if (evicted != null) {
            Logger.warn(
                TAG, evicted,
                "Onboarding recorder registry was full ($MAX_ENTRIES); evicted the " +
                    "least-recently-used entry. A terminal path is likely failing to unregister."
            )
        }
    }

    /**
     * Return the recorder registered for [correlationId], or null when none is registered (e.g. the
     * request carried no onboarding seed) or the correlation id is not usable as a key.
     *
     * Deliberately silent: the no-seed path calls this on every interactive request.
     */
    @JvmStatic
    fun get(correlationId: String?): OnboardingTelemetryRecorder? {
        val key = usableKeyOrNull(correlationId) ?: return null
        return synchronized(recorders) { recorders[key] }
    }

    /**
     * Remove the recorder registered for [correlationId]. Safe to call when none is registered or
     * the correlation id is not usable as a key.
     */
    @JvmStatic
    fun unregister(correlationId: String?) {
        val key = usableKeyOrNull(correlationId) ?: return
        val removed = synchronized(recorders) { recorders.remove(key) }
        if (removed != null) {
            Logger.info(TAG, key, "Unregistered onboarding recorder")
        }
    }

    /** Number of currently-registered recorders. Test-only. */
    @JvmStatic
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun size(): Int = synchronized(recorders) { recorders.size }

    /** Drop all entries so one test cannot observe another's registrations. Test-only. */
    @JvmStatic
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun clearForTest() {
        synchronized(recorders) { recorders.clear() }
    }
}
