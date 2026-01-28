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
package com.microsoft.identity.common.java.opentelemetry

import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import java.util.concurrent.TimeUnit

object OTelUtility {
    private val TAG = OTelUtility::class.java.simpleName

    /**
     * The currently selected span factory. Defaults to DefaultOTelSpanFactory.
     */
    @Volatile
    private var spanFactory: IOTelSpanFactory = DefaultOTelSpanFactory()

    /**
     * Sets the span factory to use for all span creation operations.
     * This allows switching between different factory implementations (e.g., benchmarking vs. default).
     *
     * @param factory The factory to use for span creation
     */
    @JvmStatic
    fun setSpanFactory(factory: IOTelSpanFactory) {
        spanFactory = factory
    }

    /**
     * Creates a span (with shared basic attributes).
     */
    @JvmStatic
    fun createSpan(name: String): Span {
        return spanFactory.createSpan(name)
    }

    /**
     * Creates a span from a parent Span Context (with shared basic attributes) and caller pkg name
     * pre-populated on the span upon creation.
     */
    @JvmStatic
    fun createSpan(name: String, callingPackageName: String): Span {
        return spanFactory.createSpan(name, callingPackageName)
    }

    /**
     * Creates a span from a parent Span Context (with shared basic attributes).
     */
    @JvmStatic
    fun createSpanFromParent(name: String, parentSpanContext: SpanContext?): Span {
        return spanFactory.createSpanFromParent(name, parentSpanContext)
    }

    /**
     * Creates a span from a parent Span Context (with shared basic attributes) and caller pkg name
     * pre-populated on the span upon creation.
     */
    @JvmStatic
    fun createSpanFromParent(
        name: String,
        parentSpanContext: SpanContext?,
        callingPackageName: String
    ): Span {
        return spanFactory.createSpanFromParent(name, parentSpanContext, callingPackageName)
    }

    /**
     * Creates a span (with shared basic attributes).
     */
    @JvmStatic
    fun createLongCounter(name: String, description: String): LongCounter {
        val meter: Meter = OpenTelemetryHolder.getMeter(TAG)

        return meter
            .counterBuilder(name)
            .setDescription(description)
            .setUnit("count")
            .build()
    }

    /**
     * Helper method to calculate and record the elapsed time since the provided start time.
     *
     * @param attributeName The name of the attribute to record in the telemetry.
     * @param startTimeMillis The start time in milliseconds.
     *                       The time unit recorded is milliseconds.
     *                       If [startTimeMillis] is negative or in the future (greater than the current time),
     *                       the method will record a negative or unexpected elapsed time value.
     *                       No validation is performed on the input value.
     */
    @JvmStatic
    fun recordElapsedTime(attributeName: String, startTimeMillis: Long) {
        val endTimeMillis = System.currentTimeMillis()
        val elapsedTimeMillis = endTimeMillis - startTimeMillis
        SpanExtension.current().setAttribute(attributeName, elapsedTimeMillis)
    }

    /**
     * Records the elapsed time in milliseconds since the provided start time in nanoseconds as a span attribute.
     *
     * @param attributeName The name of the attribute to record in the telemetry.
     * @param startTimeNanos The start time in nanoseconds.
     *                       The elapsed time is calculated using System.nanoTime() and converted to milliseconds.
     */
    @JvmStatic
    fun recordElapsedTimeFromNanos(attributeName: String, startTimeNanos: Long) {
        val elapsedTimeMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos)
        SpanExtension.current().setAttribute(attributeName, elapsedTimeMillis)
    }
}
