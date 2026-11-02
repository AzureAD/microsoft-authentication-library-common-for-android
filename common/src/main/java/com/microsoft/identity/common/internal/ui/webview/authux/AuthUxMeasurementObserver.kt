// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.common.internal.ui.webview.authux

/**
 * Bounded, payload-free instrumentation for one Broker bridge initialization and lifetime.
 *
 * This is a transport guardrail, not an authentication-request outcome or latency measurement.
 * Callers use [AuthUxBridgeTelemetry] to isolate instrumentation failures from functional processing.
 * Implementations must not retain payloads, matching values, request identifiers, WebViews or recorders.
 *
 * [AuthUxBridgeMeasurements] implements the production lifecycle: initialization owns the observation
 * until [setupFinished]; an active session then requests [finish] after cleanup. Finalization waits
 * for admitted callbacks to drain without blocking the closing thread. A callback can re-enter close,
 * so blocking in [finish] would deadlock. Export must run outside observation and functional locks.
 *
 * Default lifecycle methods allow lightweight observers without span ownership. An implementation
 * that owns a span must provide thread-safe admission, draining and idempotent finalization.
 */
interface AuthUxMeasurementObserver {
    /**
     * Admits a physical native callback for measurement, not for functional execution.
     *
     * The lifetime owner atomically checks the finish cutoff, increments its in-flight count and
     * records [Event.RECEIVED]. Returning false suppresses only measurement. The outer callback
     * boundary admits once; modern forwarding reuses that handle rather than admitting again.
     */
    fun admitCallback(): Boolean = true

    /**
     * Releases one admission after processing and rejection accounting, in an outermost `finally`.
     * Call outside the processor's functional lock: the last callback may trigger export.
     */
    fun completeCallback() {}

    /**
     * Releases initializer ownership after decisions and setup-failure accounting are recorded.
     *
     * @param active true only when a returned modern/legacy session retains the observation.
     * False covers both disabled selection and throwing setup, and requests local finalization.
     * A normal factory return does not by itself imply an installed or active transport.
     */
    fun setupFinished(active: Boolean) {}

    /**
     * Requests completion after session cleanup and closes admission to new observations.
     * Must be idempotent and nonblocking; initialization and admitted callbacks may still be running.
     */
    fun finish() {}

    /**
     * Records the latest actual decision for an attribute; never performs a second flight lookup.
     * A later setup failure can replace the selection reason with [Value.BRIDGE_SETUP_FAILED].
     */
    fun state(attribute: BridgeAttribute, value: Value)

    /** Increments a bounded event counter without changing routing, validation or mutation ordering. */
    fun count(event: Event)

    /**
     * Captured flight decisions and actual transport selection, not additional flight evaluations.
     * JavaScript Interface selection is not proof of installation: an unapproved initial URL can
     * defer installation.
     */
    enum class BridgeAttribute {
        /** Captured EnableJsApiForAuthUx decision. */
        AUTH_UX_BRIDGE_FLIGHT,
        /** Captured EnableAuthUxWebMessageListenerTransport decision. */
        WEB_MESSAGE_LISTENER_FLIGHT,
        /** Selected transport, which can differ from the requested transport after fallback. */
        TRANSPORT,
        /** Explanation for transport selection, disablement or setup failure. */
        TRANSPORT_SELECTION_REASON
    }

    /**
     * Closed vocabulary, never page-supplied strings.
     *
     * Flight attributes use ON, OFF, NOT_EVALUATED or LOOKUP_FAILED. TRANSPORT uses NOT_SELECTED,
     * WEB_MESSAGE_LISTENER, JAVASCRIPT_INTERFACE or DISABLED. TRANSPORT_SELECTION_REASON starts
     * NOT_EVALUATED and otherwise uses one of the explicit selection/failure reasons below.
     */
    enum class Value {
        ON, OFF, NOT_EVALUATED, LOOKUP_FAILED, NOT_SELECTED,
        WEB_MESSAGE_LISTENER, JAVASCRIPT_INTERFACE, DISABLED,
        /** Transport policy selected addJavascriptInterface without attempting WebMessageListener. */
        JAVASCRIPT_INTERFACE_REQUESTED,
        /** Required WebView features or initial-URL eligibility were unavailable. */
        WEB_MESSAGE_LISTENER_PREREQUISITES_NOT_MET,
        /** WebMessageListener installation completed successfully. */
        WEB_MESSAGE_LISTENER_INSTALLED,
        /** The bridge gate or capability eligibility disabled the bridge. */
        BRIDGE_NOT_ENABLED,
        /** Failed WebMessageListener setup was fully cleaned up, permitting fallback. */
        WEB_MESSAGE_LISTENER_ROLLED_BACK,
        /** Cleanup could not be confirmed complete, so the bridge was disabled. */
        WEB_MESSAGE_LISTENER_ROLLBACK_INCOMPLETE,
        /** Initialization exited exceptionally; may replace an earlier fallback reason. */
        BRIDGE_SETUP_FAILED
    }
    /**
     * Fixed counters exported by [AuthUxBridgeMeasurements] as lowercase `authux_` attributes.
     *
     * For finalized records with all required fields present and no counter saturation, derive:
     * setup completed = 1 - SETUP_FAILED; processed = RECEIVED - sum(REJECTED_*).
     * Number Matching store outcomes are not measured. Do not treat absent fields as zero. Uncertain
     * rollback is represented by TRANSPORT_SELECTION_REASON ==
     * WEB_MESSAGE_LISTENER_ROLLBACK_INCOMPLETE, not a counter.
     */
    enum class Event {
        /** Initialization did not return normally; disabled selection alone is not failure. */
        SETUP_FAILED,
        /**
         * Installation attempts: one for combined WebMessageListener/script setup, or one per
         * addJavascriptInterface call, including later navigation installations.
         */
        INSTALL_ATTEMPTS,
        /**
         * Exceptions from the WebMessageListener setup block or an addJavascriptInterface call,
         * even if rollback and fallback subsequently succeeded.
         */
        INSTALL_FAILED,
        /** WebMessageListener rollback completed; retained if fallback later overwrites the reason. */
        ROLLBACK_COMPLETE,
        /** Successful addJavascriptInterface calls, not simply legacy selection. */
        LEGACY_INSTALLED,
        /** Physical callbacks admitted before the observation finish cutoff. */
        RECEIVED,
        /** Processor was closed when an admitted callback reached it. */
        REJECTED_CLOSED,
        /** Callback origin or frame failed the existing origin policy. */
        REJECTED_ORIGIN,
        /** Native message type was not a string. */
        REJECTED_TYPE,
        /**
         * Missing data, malformed payload or invalid required telemetry fields; also includes
         * NullPointerException and JSON exceptions caught during processing, not proof of bad input.
         */
        REJECTED_MALFORMED,
        /** Parsed action/operation did not match a supported route. */
        REJECTED_UNSUPPORTED,
        /** The request did not permit the identified capability. */
        REJECTED_CAPABILITY,
        /** Processing failed outside the other rejection categories, including escaping errors. */
        REJECTED_PROCESSING,
        /** Parsed number_matching operation, excluding log_telemetry precedence; not a store attempt. */
        NUMBER_MATCH_IDENTIFIED,
        /** Applicable in-memory recorder writes completed; not proof of final blob delivery. */
        RECORDER_WRITTEN,
        /** Missing or mismatched host correlation ID excluded the sink delivery. */
        RECORDER_EXCLUDED_CID,
        /** Existing error-qualification policy excluded the sink delivery. */
        RECORDER_EXCLUDED_POLICY,
        /** Blocking-error append did not complete. */
        RECORDER_FAILED,
        /** Blocking-error append completed, but a subsequent step write failed. */
        RECORDER_PARTIAL
    }
}

/**
 * Nonthrowing boundary around optional host instrumentation; it owns no counters or lifecycle locks.
 * Instrumentation exceptions must not change authentication behavior. An observer failure can lose
 * measurements; this wrapper does not retry, repair or guarantee delivery.
 */
class AuthUxBridgeTelemetry private constructor(private val observer: AuthUxMeasurementObserver?) {
    /**
     * Returns this handle when admitted, otherwise [NONE], without changing functional execution.
     * Use the returned handle for all callback counts and its single outer-finally completion.
     */
    fun admitCallback(): AuthUxBridgeTelemetry = try {
        if (observer?.admitCallback() == true) this else NONE
    } catch (_: Throwable) { NONE }

    /** Called only by the outer callback boundary, after functional locks are released. */
    fun completeCallback() {
        try { observer?.completeCallback() } catch (_: Throwable) { }
    }

    /** Completes setup ownership, including disabled and failed initializations. */
    fun setupFinished(active: Boolean) {
        try { observer?.setupFinished(active) } catch (_: Throwable) { }
    }

    /** Requests completion without waiting for admitted callbacks. */
    fun finish() {
        try { observer?.finish() } catch (_: Throwable) { }
    }

    /** Forwards a captured decision through the failure-isolation boundary. */
    fun state(attribute: AuthUxMeasurementObserver.BridgeAttribute, value: AuthUxMeasurementObserver.Value) {
        try { observer?.state(attribute, value) } catch (_: Throwable) { }
    }

    /** Forwards one occurrence; no-op handles discard it without changing functional processing. */
    fun count(event: AuthUxMeasurementObserver.Event) {
        try { observer?.count(event) } catch (_: Throwable) { }
    }

    companion object {
        /** Inert handle for non-Broker flows, absent instrumentation and unobserved late callbacks. */
        @JvmField val NONE = AuthUxBridgeTelemetry(null)
        /** Wraps an optional observer without starting a span or taking lifecycle ownership. */
        @JvmStatic fun wrap(observer: AuthUxMeasurementObserver?): AuthUxBridgeTelemetry =
            if (observer == null) NONE else AuthUxBridgeTelemetry(observer)
    }
}
