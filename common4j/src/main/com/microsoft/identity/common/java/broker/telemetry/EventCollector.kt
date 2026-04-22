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

import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Thread-safe accumulator for [ExecutionEvent]s during an authentication flow.
 *
 * Create one instance per authentication request and call [addEvent] at each phase boundary.
 * When the flow completes, call [toTelemetrySchema] to obtain the structured result.
 *
 * @param correlationId The correlation ID of the authentication request being tracked.
 */
class EventCollector(private val correlationId: String) {
    private val events = CopyOnWriteArrayList<ExecutionEvent>()
    private val startTimeMs: Long = System.currentTimeMillis()

    /**
     * Records a new [ExecutionEvent] with the current elapsed time.
     *
     * @param tag The [EventTag] identifying this phase.
     * @param diagnosticCode Optional diagnostic code for additional context.
     * @param errorCode Optional error code if this event represents a failure.
     */
    fun addEvent(tag: EventTag, diagnosticCode: Int? = null, errorCode: Int? = null) {
        events.add(
            ExecutionEvent(
                tag = tag,
                timestampMs = System.currentTimeMillis() - startTimeMs,
                diagnosticCode = diagnosticCode,
                errorCode = errorCode
            )
        )
    }

    /**
     * Builds a [TelemetrySchema] from all events collected so far.
     * The returned schema captures the total elapsed duration and the full event timeline.
     *
     * @return A [TelemetrySchema] containing a [PerformanceRecord] with all collected events.
     */
    fun toTelemetrySchema(): TelemetrySchema {
        val duration = System.currentTimeMillis() - startTimeMs
        return TelemetrySchema(
            correlationId = correlationId,
            performanceRecord = PerformanceRecord(
                startTime = Instant.ofEpochMilli(startTimeMs).toString(),
                duration = duration,
                executionFlow = events.toList()
            )
        )
    }
}
