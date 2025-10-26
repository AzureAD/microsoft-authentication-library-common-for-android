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

import com.microsoft.identity.common.java.controllers.CommandDispatcher
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.StatusCode
import java.util.concurrent.TimeUnit

interface IBenchmarkSpan {
    /**
     * Returns a list of status changes along with their timestamps in nanoseconds.
     **/
    fun getStatuses(): List<Pair<String, Long>>

    /**
     * Returns the span name.
     **/
    fun getSpanName(): String

    /**
     * The start time of the span in nanoseconds.
     **/
    fun getStartTimeInNanoSeconds(): Long

    /**
     * The end time of the span in nanoseconds.
     **/
    fun getEndTimeInNanoSeconds(): Long

    /**
     * # of concurrent active silent requests when this span is started.
     **/
    fun getConcurrentSilentRequestSize(): Int

    /**
     * The exception recorded on this span, if any.
     **/
    fun getException(): Throwable?
}

/**
 * A span wrapper class for benchmarking purposes.
 *
 * @param originalSpan  The original span to be wrapped.
 * @param printer       The printer to print the benchmark results.
 * @param spanName      The name of the span.
 **/
class BenchmarkSpan(
    val originalSpan: Span,
    val printer: IBenchmarkSpanPrinter,
    private val spanName: String) : Span, IBenchmarkSpan {

    // Pair of (status name, timestamp in nano seconds)
    val statuses : ArrayList<Pair<String, Long>> = arrayListOf()
    private var exception: Throwable? = null

    private var startTimeInNanoSeconds: Long = System.nanoTime()
    private var endTimeInNanoSeconds: Long = 0L

    // # of concurrent active silent requests when this span is started.
    private var concurrentSize = 1

    override fun getStatuses(): List<Pair<String, Long>> {
        return statuses
    }

    override fun getSpanName(): String {
        return spanName
    }

    override fun getStartTimeInNanoSeconds(): Long {
        return startTimeInNanoSeconds
    }

    override fun getEndTimeInNanoSeconds(): Long {
        return endTimeInNanoSeconds
    }

    override fun getConcurrentSilentRequestSize(): Int {
        return concurrentSize
    }

    override fun getException(): Throwable? {
        return exception
    }

    fun start(){
        startTimeInNanoSeconds = System.nanoTime()
        concurrentSize = CommandDispatcher.getSilentRequestActiveCount()
    }

    override fun end() {
        endTimeInNanoSeconds = System.nanoTime()
        printer.printAsync(this)
        return originalSpan.end()
    }

    override fun end(timestamp: Long, unit: TimeUnit) {
        endTimeInNanoSeconds = System.nanoTime()
        printer.printAsync(this)
        return originalSpan.end(timestamp, unit)
    }

    override fun <T : Any?> setAttribute(
        key: AttributeKey<T>,
        value: T
    ): Span? {
        statuses.add(Pair(key.toString(), System.nanoTime()))
        return originalSpan.setAttribute(key, value)
    }

    override fun addEvent(
        name: String,
        attributes: Attributes
    ): Span? {
        statuses.add(Pair(name, System.nanoTime()))
        return originalSpan.addEvent(name, attributes)
    }

    override fun addEvent(
        name: String,
        attributes: Attributes,
        timestamp: Long,
        unit: TimeUnit
    ): Span? {
        statuses.add(Pair(name, System.nanoTime()))
        return originalSpan.addEvent(name, attributes, timestamp, unit)
    }

    override fun setStatus(
        statusCode: StatusCode,
        description: String
    ): Span? {
        statuses.add(Pair("SetStatus:$statusCode", System.nanoTime()))
        return originalSpan.setStatus(statusCode, description)
    }

    override fun recordException(
        exception: Throwable,
        additionalAttributes: Attributes
    ): Span? {
        val timestamp = System.nanoTime()
        statuses.add(Pair("recordException", timestamp))
        this.exception = exception
        return originalSpan.recordException(exception, additionalAttributes)
    }

    override fun updateName(name: String): Span? {
        return originalSpan.updateName(name)
    }

    override fun getSpanContext(): SpanContext? {
        return originalSpan.spanContext
    }

    override fun isRecording(): Boolean {
        return originalSpan.isRecording
    }
}
