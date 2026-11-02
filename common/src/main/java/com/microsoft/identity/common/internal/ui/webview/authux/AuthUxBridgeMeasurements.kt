// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.common.internal.ui.webview.authux

import com.microsoft.identity.common.internal.ui.webview.authux.AuthUxMeasurementObserver.BridgeAttribute
import com.microsoft.identity.common.internal.ui.webview.authux.AuthUxMeasurementObserver.Event
import com.microsoft.identity.common.internal.ui.webview.authux.AuthUxMeasurementObserver.Value
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.opentelemetry.OTelUtility
import com.microsoft.identity.common.java.opentelemetry.SpanName
import io.opentelemetry.api.trace.Span
import java.util.Locale

/**
 * Accumulates payload-free transport guardrails for one Broker bridge initialization and lifetime.
 * This is not an authentication-request outcome or latency measurement.
 *
 * Initialization owns the observation until [setupFinished]. A disabled or failed initialization
 * requests local completion; an active session retains the observation until cleanup calls [finish].
 * Finalization requires initialization to finish, completion to be requested, and every admitted
 * callback to drain. Closing never waits for callbacks: a callback can itself re-enter close.
 *
 * The lock protects only local state and snapshot creation. SDK writes and span completion occur
 * outside it, and callers must release functional locks before invoking lifecycle completion.
 * Attempts export/end once after cleanup and admitted callbacks drain; delivery is not guaranteed.
 * Process death, missing teardown or SDK failures may lose some or all of this record.
 * A callback that never completes also prevents finalization; there is no timeout or retry mechanism.
 *
 * Uses the existing factory without making this span current or forcing a root. Broker's default
 * configuration assigns this span a 25% trace-ID sampler rather than inheriting the global
 * parent-based decision. Span/app configuration overrides can change the effective sampling rate.
 * Captured attributes are exported as bounded lowercase strings; event counters are longs, including
 * zero. Observer attributes and events map explicitly to shared [AttributeName] constants, so their
 * internal names do not determine the wire keys. State values retain their lowercase [Value] names.
 * The mappings are exhaustive: adding an observer field requires an explicit schema decision.
 * Shared key names and state-value names are wire contracts; keep the Broker allowlist in sync.
 *
 * For finalized records with the required fields present, 1 - setup_failed indicates initialization
 * returned normally, including disabled selection; it does not indicate successful installation.
 * Derive processed callbacks as received - sum(rejected_*), assuming no counter saturation;
 * do not treat absent fields as zero. Number Matching identification does not measure storage,
 * retrieval or MFA completion.
 * Uncertain rollback is represented by
 * transport_selection_reason == web_message_listener_rollback_incomplete, not a separate counter.
 */
class AuthUxBridgeMeasurements private constructor(private val span: Span) : AuthUxMeasurementObserver {
    private val lock = Any()
    private val counts = LongArray(Event.values().size)
    private val states = Array(BridgeAttribute.values().size) { Value.NOT_EVALUATED }.also {
        it[BridgeAttribute.TRANSPORT.ordinal] = Value.NOT_SELECTED
    }
    // These are independent: close can race initialization or run inside an admitted callback.
    private var callbacks = 0L
    private var initializing = true
    private var finishing = false
    private var ended = false

    /** Retains the latest captured decision until the final snapshot is taken. */
    override fun state(attribute: BridgeAttribute, value: Value) {
        synchronized(lock) { if (!ended) states[attribute.ordinal] = value }
    }

    /**
     * Records an occurrence, saturating rather than wrapping on overflow.
     * Admitted callbacks may still record after finish is requested, but never after snapshotting.
     */
    override fun count(event: Event) {
        synchronized(lock) {
            if (!ended && counts[event.ordinal] < Long.MAX_VALUE) counts[event.ordinal]++
        }
    }

    /**
     * Atomically admits and counts one physical callback before the finish cutoff.
     * A false result suppresses observation only; it must not change functional callback behavior.
     */
    override fun admitCallback(): Boolean = synchronized(lock) {
        if (finishing || ended) false else {
            callbacks++
            if (counts[Event.RECEIVED.ordinal] < Long.MAX_VALUE) counts[Event.RECEIVED.ordinal]++
            true
        }
    }

    /**
     * Releases a callback admission and attempts finalization if it was the last outstanding owner.
     * Called once per admission by the outer callback's `finally`, after functional locks are released.
     */
    override fun completeCallback() {
        val snapshot = synchronized(lock) {
            if (callbacks > 0) callbacks--
            snapshotIfFinished()
        }
        export(snapshot)
    }

    /**
     * Releases initialization ownership after setup decisions and failure accounting are complete.
     *
     * @param active whether a returned active session retains this observation for later cleanup.
     * False requests completion locally, including normal disabled selection and throwing setup.
     */
    override fun setupFinished(active: Boolean) {
        val snapshot = synchronized(lock) {
            initializing = false
            if (!active) finishing = true
            snapshotIfFinished()
        }
        export(snapshot)
    }

    /**
     * Closes admission and requests completion without waiting for initialization or callbacks.
     * Repeated calls are harmless; an outstanding owner triggers export when it finishes.
     */
    override fun finish() {
        val snapshot = synchronized(lock) {
            finishing = true
            snapshotIfFinished()
        }
        export(snapshot)
    }

    /**
     * Freezes one snapshot only when all owners have finished. Must be called under [lock].
     * Setting [ended] before releasing the lock prevents another thread from exporting a second copy.
     * No SDK, recorder, Number Matching store or WebKit calls occur under this lock.
     */
    private fun snapshotIfFinished(): Map<String, Any>? {
        if (ended || initializing || !finishing || callbacks != 0L) return null
        ended = true
        return linkedMapOf<String, Any>().also { result ->
            BridgeAttribute.values().forEach {
                result[it.toAttributeName().name] =
                    states[it.ordinal].name.lowercase(Locale.ROOT)
            }
            Event.values().forEach {
                result[it.toAttributeName().name] = counts[it.ordinal]
            }
        }
    }

    private fun BridgeAttribute.toAttributeName(): AttributeName = when (this) {
        BridgeAttribute.AUTH_UX_BRIDGE_FLIGHT -> AttributeName.authux_auth_ux_bridge_flight
        BridgeAttribute.WEB_MESSAGE_LISTENER_FLIGHT -> AttributeName.authux_web_message_listener_flight
        BridgeAttribute.TRANSPORT -> AttributeName.authux_transport
        BridgeAttribute.TRANSPORT_SELECTION_REASON -> AttributeName.authux_transport_selection_reason
    }

    private fun Event.toAttributeName(): AttributeName = when (this) {
        Event.SETUP_FAILED -> AttributeName.authux_setup_failed
        Event.INSTALL_ATTEMPTS -> AttributeName.authux_install_attempts
        Event.INSTALL_FAILED -> AttributeName.authux_install_failed
        Event.ROLLBACK_COMPLETE -> AttributeName.authux_rollback_complete
        Event.LEGACY_INSTALLED -> AttributeName.authux_legacy_installed
        Event.RECEIVED -> AttributeName.authux_received
        Event.REJECTED_CLOSED -> AttributeName.authux_rejected_closed
        Event.REJECTED_ORIGIN -> AttributeName.authux_rejected_origin
        Event.REJECTED_TYPE -> AttributeName.authux_rejected_type
        Event.REJECTED_MALFORMED -> AttributeName.authux_rejected_malformed
        Event.REJECTED_UNSUPPORTED -> AttributeName.authux_rejected_unsupported
        Event.REJECTED_CAPABILITY -> AttributeName.authux_rejected_capability
        Event.REJECTED_PROCESSING -> AttributeName.authux_rejected_processing
        Event.NUMBER_MATCH_IDENTIFIED -> AttributeName.authux_number_match_identified
        Event.RECORDER_WRITTEN -> AttributeName.authux_recorder_written
        Event.RECORDER_EXCLUDED_CID -> AttributeName.authux_recorder_excluded_cid
        Event.RECORDER_EXCLUDED_POLICY -> AttributeName.authux_recorder_excluded_policy
        Event.RECORDER_FAILED -> AttributeName.authux_recorder_failed
        Event.RECORDER_PARTIAL -> AttributeName.authux_recorder_partial
    }

    /**
     * Writes a detached snapshot outside lifecycle locks; null means there is nothing to export.
     * Isolates each attribute failure so remaining writes and span.end are still attempted.
     * There is no retry: instrumentation failures must not alter authentication behavior.
     */
    private fun export(snapshot: Map<String, Any>?) {
        if (snapshot == null) return
        try {
            snapshot.forEach { (key, value) ->
                try {
                    when (value) {
                        is Long -> span.setAttribute(key, value)
                        is String -> span.setAttribute(key, value)
                    }
                } catch (_: Throwable) { }
            }
        } finally {
            try { span.end() } catch (_: Throwable) { }
        }
    }

    companion object {
        /** Bridge lifetime duration must not be interpreted as setup or authentication latency. */
        @JvmField
        val SPAN_NAME = SpanName.AuthUxBridge.name

        /**
         * Starts a best-effort observation using the existing OpenTelemetry factory.
         * The caller must first check the Broker process; this method does not perform that check.
         * Returns [AuthUxBridgeTelemetry.NONE] if creation fails, leaving functional setup unaffected.
         */
        @JvmStatic
        fun start(): AuthUxBridgeTelemetry = try {
            AuthUxBridgeTelemetry.wrap(AuthUxBridgeMeasurements(OTelUtility.createSpan(SPAN_NAME)))
        } catch (_: Throwable) { AuthUxBridgeTelemetry.NONE }
    }
}
