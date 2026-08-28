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
package com.microsoft.identity.common.java.broker.telemetry

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

/**
 * Thread-safe accumulator for [ExecutionEvent]s during an authentication flow.
 *
 * Create one instance per authentication request and call [addEvent] at each phase boundary.
 * When the flow completes, call [toBrokerIpcTelemetry] to obtain the structured result.
 *
 * @param correlationId The correlation ID of the authentication request being tracked.
 */
class EventCollector(private val correlationId: String) {
    private val events = CopyOnWriteArrayList<ExecutionEvent>()

    /**
     * Wall-clock anchor, used solely to render [PerformanceRecord.startTime] as an
     * absolute instant. The unified broker telemetry contract types this field as a
     * datetime; ISO 8601 UTC is our chosen encoding, pending confirmation from the
     * OneAuth team.
     * Never used for elapsed-time arithmetic — see [startTimeNanos].
     */
    private val startTimeMs: Long = System.currentTimeMillis()

    /**
     * Monotonic origin for all elapsed-time measurements. Unlike the wall clock,
     * [System.nanoTime] is immune to NTP corrections and device clock changes, so
     * elapsed values can never go backwards. Reported on the wire in milliseconds.
     */
    private val startTimeNanos: Long = System.nanoTime()

    companion object {
        /**
         * Offset applied to broker-side thread IDs to avoid collision with
         * OneAuth client-side thread IDs when the two event streams are
         * stitched in Kusto via correlation_id.
         */
        const val BROKER_THREAD_ID_OFFSET = 10_000L

        /**
         * ISO 8601 / RFC 3339 UTC pattern with millisecond precision,
         * e.g. `2026-01-13T10:30:45.123Z`.
         *
         * [java.time.Instant] is deliberately not used here: this library is consumed on Android, and
         * core library desugaring is disabled (`common4j/build.gradle`), so `java.time` APIs are not
         * available on older Android runtimes. [SimpleDateFormat] is available since API 1.
         */
        private const val ISO_8601_UTC_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

        /**
         * Formats [epochMs] as an ISO 8601 UTC timestamp.
         *
         * The [SimpleDateFormat] is allocated per call rather than shared, because it is
         * not thread-safe and [EventCollector] is documented as thread-safe. The allocation
         * cost is negligible: this runs once per authentication request.
         */
        private fun formatIso8601Utc(epochMs: Long): String =
            SimpleDateFormat(ISO_8601_UTC_PATTERN, Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .format(Date(epochMs))
    }

    /**
     * Milliseconds elapsed since this collector was created, measured on the
     * monotonic clock. Non-negative because [System.nanoTime] never decreases
     * within a JVM; sub-millisecond precision is truncated, so rapid successive
     * events may share a timestamp.
     */
    private fun elapsedMs(): Long =
        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos)

    /**
     * Records a new [ExecutionEvent] with the current elapsed time.
     * Thread ID is offset by [BROKER_THREAD_ID_OFFSET] to avoid collision
     * with client-side (OneAuth) thread IDs during Kusto stitching.
     *
     * @param tag The [EventTag] identifying this phase.
     * @param statusCode Optional status code (arbitrary integer, e.g. HTTP status or cache-expiry minutes).
     * @param errorCode Optional error code if this event represents a failure.
     */
    fun addEvent(tag: EventTag, statusCode: Int? = null, errorCode: Int? = null) {
        events.add(
            ExecutionEvent(
                tag = tag,
                timestampMs = elapsedMs(),
                threadId = Thread.currentThread().id + BROKER_THREAD_ID_OFFSET,
                statusCode = statusCode,
                errorCode = errorCode
            )
        )
    }

    /**
     * Builds a [BrokerIpcTelemetry] from all events collected so far.
     *
     * Snapshots the event list first, then captures the end time, ensuring
     * that [PerformanceRecord.duration] is always >= the last event's timestamp.
     *
     * The broker identity and outcome are supplied by the caller rather than tracked by this
     * collector: they are only known once the flow terminates, and the parent Unified Broker
     * Telemetry contract marks all three as required.
     *
     * @param name Logical name of the broker producing this payload, e.g. `authenticator`.
     * @param version Version string of the broker application.
     * @param authOutcome Outcome of the authentication, e.g. `success` or `failure`.
     * @return A [BrokerIpcTelemetry] containing a [PerformanceRecord] with all collected events.
     */
    fun toBrokerIpcTelemetry(
        name: String,
        version: String,
        authOutcome: String
    ): BrokerIpcTelemetry {
        // Snapshot events first, then capture end time — guarantees duration >= last event ts.
        val eventSnapshot = events.toList()
        val duration = elapsedMs()
        return BrokerIpcTelemetry(
            correlationId = correlationId,
            name = name,
            version = version,
            authOutcome = authOutcome,
            performanceRecord = PerformanceRecord(
                startTime = formatIso8601Utc(startTimeMs),
                duration = duration,
                executionFlow = eventSnapshot
            )
        )
    }
}
