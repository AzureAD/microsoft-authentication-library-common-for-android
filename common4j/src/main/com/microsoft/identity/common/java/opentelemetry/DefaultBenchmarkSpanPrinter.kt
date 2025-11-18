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

import com.microsoft.identity.common.java.logging.Logger
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Represents the different metric types that can be calculated and displayed.
 */
enum class MetricType(val displayName: String) {
    AVERAGE("Avg"),
    P50("P50"),
    P75("P75"),
    P90("P90"),
    P95("P95"),
    P99("P99")
}

/**
 * Default implementation of IBenchmarkSpanPrinter that asynchronously writes
 * benchmark span status information to a file.
 *
 * @param outputDirectoryAbsolutePath   Path to the directory where benchmark files will be written.
 * @param batchSize                     Size of batches to accumulate before writing to file (default: 1, meaning write each span immediately).
 * @param metricsToDisplay              List of MetricType to display in the output (default: AVERAGE, P50, P75, P90).
 */
class DefaultBenchmarkSpanPrinter(
    private val outputDirectoryAbsolutePath: String,
    private val batchSize: Int = 1,
    private val metricsToDisplay: List<MetricType> = listOf(MetricType.AVERAGE, MetricType.P50, MetricType.P75, MetricType.P90)
) : IBenchmarkSpanPrinter {

    companion object {
        private val TAG = DefaultBenchmarkSpanPrinter::class.java.simpleName
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    }

    private val singleThreadExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "BenchmarkSpanPrinter").apply {
            isDaemon = true
        }
    }

    // Storage for batching spans - now per span name
    private val batchedSpansByName = mutableMapOf<String, MutableList<IBenchmarkSpan>>()
    private val batchCounterByName = mutableMapOf<String, Int>()

    override fun printAsync(span: IBenchmarkSpan) {
        singleThreadExecutor.submit {
            try {
                val spanName = span.getSpanName()

                // Get or create the batch list for this span name
                val batchList = batchedSpansByName.getOrPut(spanName) { mutableListOf() }
                batchList.add(span)

                // Increment the counter for this span name
                val currentCount = batchCounterByName.getOrDefault(spanName, 0) + 1
                batchCounterByName[spanName] = currentCount

                if (currentCount >= batchSize) {
                    writeSpansToFile(batchList.toList())
                    batchList.clear()
                    batchCounterByName[spanName] = 0
                }
            } catch (e: Exception) {
                Logger.error(TAG, "Failed to write span status to file", e)
            }
        }
    }

    private fun writeSpansToFile(spans: List<IBenchmarkSpan>) {
        if (spans.isEmpty()) return

        // All spans in the batch now have the same name due to per-span-name batching
        val spanName = spans.first().getSpanName()

        try {
            val file = getFile(spanName)

            FileWriter(file, true).use { writer ->
                // Separate spans into two groups: with exceptions and without exceptions
                val spansWithExceptions = spans.filter { it.getException() != null }
                val spansWithoutExceptions = spans.filter { it.getException() == null }

                // Write session header
                val formattedTimestamp = DATE_FORMAT.format(Date(System.currentTimeMillis()))
                writer.appendLine("")
                writer.appendLine("=== Statistical Benchmark Session: $formattedTimestamp | Batch Size: ${spans.size} | With Exceptions: ${spansWithExceptions.size} | Without Exceptions: ${spansWithoutExceptions.size} ===")
                writer.appendLine("")

                // Write statistics for spans WITHOUT exceptions
                if (spansWithoutExceptions.isNotEmpty()) {
                    writeSpanGroupStatistics(writer, spansWithoutExceptions, "SUCCESS FLOWS (No Exceptions)")
                }

                // Write statistics for spans WITH exceptions, grouped by exception message
                if (spansWithExceptions.isNotEmpty()) {
                    writeSpanGroupsByExceptionMessage(writer, spansWithExceptions)
                }

                writer.flush()
            }
        } catch (e: IOException) {
            Logger.error(TAG, "IOException while writing averaged batch to file: $outputDirectoryAbsolutePath", e)
        }
    }

    /**
     * Write statistical data for a specific group of spans
     */
    private fun writeSpanGroupStatistics(writer: FileWriter, spans: List<IBenchmarkSpan>, groupTitle: String) {
        val statisticalData = calculateStatistics(spans)

        if (statisticalData.isEmpty()) {
            writer.appendLine("=== $groupTitle ===")
            writer.appendLine("No status entries recorded for this group (${spans.size} spans)")
            writer.appendLine("")
            return
        }

        // Calculate group-specific metrics
        val totalDurationFormatted = spans.mapNotNull { span ->
            val spanStartTime = span.getStartTimeInNanoSeconds()
            val spanEndTime = span.getEndTimeInNanoSeconds()
            if (spanEndTime > 0) {
                TimeUnit.NANOSECONDS.toMillis(spanEndTime - spanStartTime)
            } else null
        }.let { durations ->
            if (durations.isNotEmpty()) {
                val avgDuration = durations.average().toLong()
                "${avgDuration}ms"
            } else {
                "N/A"
            }
        }

        val avgConcurrentSize = spans.map { it.getConcurrentSilentRequestSize() }.average()
        val avgConcurrentSizeFormatted = String.format(Locale.US, "%.2f", avgConcurrentSize)

        writer.appendLine("=== $groupTitle ===")
        writer.appendLine("Avg Total Duration: $totalDurationFormatted | Avg Concurrent Size: $avgConcurrentSizeFormatted | Spans: ${spans.size}")
        writer.appendLine("")

        writer.appendLine("| Status Entry                                     | Metric | Time Since Previous |")
        writer.appendLine("|--------------------------------------------------|--------|---------------------|")

        statisticalData.forEach { statsData ->
            val paddedStatus = statsData.statusName.take(48).padEnd(48)

            // Print only the configured metrics
            metricsToDisplay.forEach { metricType ->
                val metricLabel = metricType.displayName.padEnd(6)
                val sincePrevValue = getMetricValue(statsData.timeSincePreviousStats, metricType).padEnd(19)

                val statusColumn = if (metricType == metricsToDisplay.first()) {
                    paddedStatus
                } else {
                    "".padEnd(48)
                }

                writer.appendLine("| $statusColumn | $metricLabel | $sincePrevValue |")
            }

            // Separator line between status entries
            writer.appendLine("|--------------------------------------------------|--------|---------------------|")
        }

        writer.appendLine("")
    }

    /**
     * Write separate statistical tables for each unique exception message
     */
    private fun writeSpanGroupsByExceptionMessage(writer: FileWriter, spansWithExceptions: List<IBenchmarkSpan>) {
        // Group spans by exception message
        val spansByExceptionMessage = spansWithExceptions.groupBy { span ->
            val exception = span.getException()
            val exceptionMessage = exception?.message ?: "Unknown Error"
            val exceptionType = exception?.javaClass?.simpleName ?: "Unknown"
            "$exceptionType: $exceptionMessage"
        }

        // Write a table for each exception message group
        spansByExceptionMessage.entries.sortedBy { it.key }.forEachIndexed { index, (exceptionMessage, spans) ->
            val groupTitle = "ERROR FLOWS - ${exceptionMessage.take(60)}"
            writeSpanGroupStatistics(writer, spans, groupTitle)
        }
    }



    /**
     * Get a file to write the benchmark result to.
     * Separate file for each span name.
     **/
    private fun getFile(spanName: String): File {
        val outputDir = File(outputDirectoryAbsolutePath)
        outputDir.mkdirs()

        val sanitizedSpanName = sanitizeFileName(spanName)
        val filename = "${sanitizedSpanName}_benchmark.log"
        val file = File(outputDir, filename)
        return file
    }

    /**
     * Replace characters that are not safe for filenames
     **/
    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .takeIf { it.isNotEmpty() } ?: "span"
    }



    /**
     * Calculate statistical metrics (average, percentiles) for all status entries across the batch of spans.
     *
     * For each status occurrence found across all spans (including duplicates), this method:
     * 1. Collects timing values (time since previous status, time since start) from all spans
     * 2. Calculates configured statistical metrics (e.g., Avg, P50, P75, P90)
     * 3. Returns the results sorted by the first configured metric's time since start value
     *
     * Note: If the same status name appears multiple times, each occurrence is tracked separately
     * with an enumeration (e.g., "status [1]", "status [2]").
     *
     * The "Time Since Previous" column shows the statistical timing for how long each status
     * typically takes to execute relative to the previous status within individual spans.
     * When aggregated across spans, these represent independent timing measurements.
     *
     * @param spans     List of spans to analyze (all spans should have the same span name)
     *
     * @return List of statistical data for each status occurrence, sorted by the first configured metric's time since start
     */
    private fun calculateStatistics(spans: List<IBenchmarkSpan>): List<StatisticalStatusData> {
        if (spans.isEmpty()) return emptyList()

        // Use LinkedHashMap to maintain insertion order based on first span's status appearances
        val statusOccurrencesMap = linkedMapOf<String, MutableList<Long>>() // timeSincePrevious values

        // Process first span to establish the order and which statuses to track
        val firstSpan = spans.first()
        val firstSpanStatuses = firstSpan.getStatuses()
        val firstSpanStatusCounts = mutableMapOf<String, Int>()

        // Initialize the map with statuses from the first span to establish order
        firstSpanStatuses.forEach { (statusName, _) ->
            val spanOccurrenceIndex = firstSpanStatusCounts.getOrDefault(statusName, 0) + 1
            firstSpanStatusCounts[statusName] = spanOccurrenceIndex
            val occurrenceStatusName = getStatusName(spanOccurrenceIndex, statusName)

            // Initialize empty list for this status (maintains insertion order)
            statusOccurrencesMap[occurrenceStatusName] = mutableListOf()
        }

        // Now process all spans (including the first one again) to collect timing data
        for (span in spans) {
            val statuses = span.getStatuses()
            val startTime = span.getStartTimeInNanoSeconds()
            val spanStatusCounts = mutableMapOf<String, Int>()

            statuses.forEachIndexed { statusIndex, (statusName, timestamp) ->
                val spanOccurrenceIndex = spanStatusCounts.getOrDefault(statusName, 0) + 1
                spanStatusCounts[statusName] = spanOccurrenceIndex
                val occurrenceStatusName = getStatusName(spanOccurrenceIndex, statusName)

                // Only process if this status was in the first span (exists in our map)
                statusOccurrencesMap[occurrenceStatusName]?.let {
                    val previousTime = if (statusIndex > 0) {
                        statuses[statusIndex - 1].second
                    } else {
                        startTime
                    }
                    val timeSincePreviousMs = TimeUnit.NANOSECONDS.toMillis(timestamp - previousTime)
                    it.add(timeSincePreviousMs)
                }
            }
        }

        // LinkedHashMap maintains insertion order, so no sorting needed
        return statusOccurrencesMap.map { (statusName, timingSincePrevsValues) ->
            StatisticalStatusData(
                statusName = statusName,
                timeSincePreviousStats = calculateMetrics(timingSincePrevsValues)
            )
        }
    }

    private fun getStatusName(spanOccurrenceIndex: Int, statusName: String): String {
        val occurrenceStatusName = if (spanOccurrenceIndex > 1) {
            "[$spanOccurrenceIndex] $statusName"
        } else {
            statusName
        }
        return occurrenceStatusName
    }

    private fun calculateMetrics(values: List<Long>): Map<MetricType, Long> {
        if (values.isEmpty()) return emptyMap()

        val result = mutableMapOf<MetricType, Long>()

        // Only calculate metrics that are actually displayed
        metricsToDisplay.forEach { metricType ->
            val value = when (metricType) {
                MetricType.AVERAGE -> values.average().toLong()
                MetricType.P50 -> percentile(values, 50.0)
                MetricType.P75 -> percentile(values, 75.0)
                MetricType.P90 -> percentile(values, 90.0)
                MetricType.P95 -> percentile(values, 95.0)
                MetricType.P99 -> percentile(values, 99.0)
            }
            result[metricType] = value
        }

        return result
    }

    private fun percentile(values: List<Long>, percentile: Double): Long {
        if (values.isEmpty()) return 0L
        if (values.size == 1) return values[0]

        val sortedValues = values.sorted()

        val index = (percentile / 100.0) * (sortedValues.size - 1)
        val lower = kotlin.math.floor(index).toInt()
        val upper = kotlin.math.ceil(index).toInt()

        if (lower == upper) {
            return sortedValues[lower]
        }

        val weight = index - lower
        return (sortedValues[lower] * (1 - weight) + sortedValues[upper] * weight).toLong()
    }

    /**
     * Get the metric value from the metrics map based on MetricType
     */
    private fun getMetricValue(metricsMap: Map<MetricType, Long>, metricType: MetricType): String {
        val value = metricsMap[metricType] ?: 0L
        return "${value}ms"
    }

    /**
     * Holds statistical data for a single status entry across multiple spans.
     *
     * This data class aggregates timing metrics for a specific status name,
     * containing "time since previous" statistics calculated across all spans in the batch.
     *
     * @property statusName                      The name of the status entry (e.g., "acquireToken", "networkCall")
     * @property timeSincePreviousStats          Map of metric type to value (in ms) for time elapsed from previous status to this status
     */
    data class StatisticalStatusData(
        val statusName: String,
        val timeSincePreviousStats: Map<MetricType, Long>
    )

    /**
     * Flushes any remaining spans in the batch and writes them to file.
     * This should be called when shutting down or when you want to force writing
     * of incomplete batches.
     */
    fun flushRemainingSpans() {
        singleThreadExecutor.submit {
            synchronized(batchedSpansByName) {
                batchedSpansByName.forEach { (spanName, spans) ->
                    if (spans.isNotEmpty()) {
                        writeSpansToFile(spans.toList())
                        spans.clear()
                        batchCounterByName[spanName] = 0
                    }
                }
            }
        }
    }

    /**
     * Shuts down the executor service. Should be called when the printer is no longer needed.
     * This will also flush any remaining spans before shutting down.
     */
    fun shutdown() {
        // Flush any remaining spans before shutdown
        flushRemainingSpans()

        // Wait a bit for the flush to complete, then shutdown
        singleThreadExecutor.submit {
            // This empty task ensures the flush completes before shutdown
        }

        singleThreadExecutor.shutdown()
    }
}
