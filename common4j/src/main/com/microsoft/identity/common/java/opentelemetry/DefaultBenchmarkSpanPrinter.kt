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
 * NOTE: in order to simulate concurrent request coming from multiple apps (while we only have one MSALTestApp),
 * shouldSkipSilentTokenCommandCacheForStressTest is required to be set to true.
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
        /** Tag used for logging messages from this class */
        private val TAG = DefaultBenchmarkSpanPrinter::class.java.simpleName

        /** Date format pattern for timestamp display in benchmark output */
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

        /** Maximum width in characters for the status entry column in output tables */
        private const val STATUS_COLUMN_WIDTH = 48

        /** Maximum width in characters for the metric label column in output tables */
        private const val METRIC_COLUMN_WIDTH = 6

        /** Maximum width in characters for the time value column in output tables */
        private const val TIME_COLUMN_WIDTH = 19

        /** Maximum length in characters for exception messages displayed in group titles */
        private const val EXCEPTION_MESSAGE_MAX_LENGTH = 60
    }

    private val singleThreadExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "BenchmarkSpanPrinter").apply {
            isDaemon = true
        }
    }

    // Storage for batching spans - now per span name
    private val batchedSpansByName = mutableMapOf<String, MutableList<IBenchmarkSpan>>()
    private val batchCounterByName = mutableMapOf<String, Int>()

    /**
     * Asynchronously prints a benchmark span to file.
     *
     * Spans are batched by span name, and written to file when the batch size is reached.
     * Each span name has its own batch and counter to ensure proper grouping.
     *
     * @param span The benchmark span to print
     */
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

    /**
     * Writes a batch of spans to file with statistical analysis.
     *
     * Spans are separated into success flows (without exceptions) and error flows (with exceptions).
     * Each group is written with its own statistical table. Error flows are further grouped by
     * exception message for better organization.
     *
     * @param spans List of spans to write (all spans should have the same span name)
     */
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
     * Write statistical data for a specific group of spans.
     *
     * Generates a formatted table showing status entries with their timing metrics.
     * Includes group summary information such as average total duration and concurrent request size.
     * Each status entry shows the configured metrics (e.g., Avg, P50, P75, P90).
     *
     * @param writer FileWriter to write the output to
     * @param spans List of spans to analyze for this group
     * @param groupTitle Title to display for this group (e.g., "SUCCESS FLOWS" or "ERROR FLOWS - Exception")
     */
    private fun writeSpanGroupStatistics(writer: FileWriter, spans: List<IBenchmarkSpan>, groupTitle: String) {
        val statisticalData = calculateStatistics(spans)

        if (statisticalData.isEmpty()) {
            writer.appendLine("=== $groupTitle ===")
            writer.appendLine("No status entries recorded for this group (${spans.size} spans)")
            writer.appendLine("")
            return
        }

        // Calculate average total duration for this group
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

        // Calculate average concurrent request size (during each span) for this group.
        val avgConcurrentSize = spans.map { it.getConcurrentSilentRequestSize() }.average()
        val avgConcurrentSizeFormatted = String.format(Locale.US, "%.2f", avgConcurrentSize)

        writer.appendLine("=== $groupTitle ===")
        writer.appendLine("Avg Total Duration: $totalDurationFormatted | Avg Concurrent Size: $avgConcurrentSizeFormatted | Spans: ${spans.size}")
        writer.appendLine("")

        // Print table header and separator for status entries with their timing metrics
        writer.appendLine("| Status Entry                                     | Metric | Time Since Previous |")
        writer.appendLine("|--------------------------------------------------|--------|---------------------|")

        statisticalData.forEach { statsData ->
            val paddedStatus = statsData.statusName.take(STATUS_COLUMN_WIDTH).padEnd(STATUS_COLUMN_WIDTH)

            // Print each configured metric type (Avg, P50, P75, P90, etc.) for this status entry
            metricsToDisplay.forEach { metricType ->
                val metricLabel = metricType.displayName.padEnd(METRIC_COLUMN_WIDTH)
                val sincePrevValue = getMetricValue(statsData.timeSincePreviousStats, metricType).padEnd(TIME_COLUMN_WIDTH)

                val statusColumn = if (metricType == metricsToDisplay.first()) {
                    paddedStatus
                } else {
                    "".padEnd(STATUS_COLUMN_WIDTH)
                }

                writer.appendLine("| $statusColumn | $metricLabel | $sincePrevValue |")
            }

            writer.appendLine("|--------------------------------------------------|--------|---------------------|")
        }

        writer.appendLine("")
    }

    /**
     * Write separate statistical tables for each unique exception message.
     *
     * Groups spans by their exception type and message, then writes a statistical table
     * for each group. This allows for detailed analysis of different error scenarios.
     *
     * @param writer FileWriter to write the output to
     * @param spansWithExceptions List of spans that have exceptions
     */
    private fun writeSpanGroupsByExceptionMessage(writer: FileWriter, spansWithExceptions: List<IBenchmarkSpan>) {
        // Group spans by normalized exception message to avoid unbounded cardinality.
        val spansByExceptionMessage = spansWithExceptions.groupBy { span ->
            val exception = span.getException()
            val exceptionMessage = exception?.message ?: "Unknown Error"
            val exceptionType = exception?.javaClass?.simpleName ?: "Unknown"
            val normalizedMessage = normalizeExceptionMessage(exceptionMessage)
            "$exceptionType: $normalizedMessage"
        }

        // Write a table for each exception message group
        spansByExceptionMessage.entries.sortedBy { it.key }.forEach { (exceptionMessage, spans) ->
            val groupTitle = "ERROR FLOWS - $exceptionMessage"
            writeSpanGroupStatistics(writer, spans, groupTitle)
        }
    }

    /**
     * Normalize an exception message for grouping to reduce cardinality.
     *
     * Replaces numeric sequences (e.g., IDs, timestamps) with a placeholder and
     * truncates the result to EXCEPTION_MESSAGE_MAX_LENGTH characters.
     */
    private fun normalizeExceptionMessage(message: String): String {
        // Replace digit sequences to avoid unique groups per ID/timestamp-like value.
        val normalized = message.replace(Regex("\\d+"), "#")
        return if (normalized.length > EXCEPTION_MESSAGE_MAX_LENGTH) {
            normalized.take(EXCEPTION_MESSAGE_MAX_LENGTH) + "..."
        } else {
            normalized
        }
    }
    /**
     * Get a file to write the benchmark result to.
     *
     * Creates a separate file for each span name in the configured output directory.
     * The filename is based on the sanitized span name with "_benchmark.log" suffix.
     *
     * @param spanName The name of the span to create a file for
     * @return File object for writing benchmark results
     */
    private fun getFile(spanName: String): File {
        val outputDir = File(outputDirectoryAbsolutePath)
        outputDir.mkdirs()

        val sanitizedSpanName = sanitizeFileName(spanName)
        val filename = "${sanitizedSpanName}_benchmark.log"
        val file = File(outputDir, filename)
        return file
    }

    /**
     * Sanitizes a string to make it safe for use as a filename.
     *
     * Replaces all characters that are not alphanumeric, underscore, or hyphen with underscores.
     * Multiple consecutive underscores are collapsed into one, and leading/trailing underscores are removed.
     *
     * @param name The original name to sanitize
     * @return A sanitized filename-safe string, or "span" if the result would be empty
     */
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
     * 1. Scans all spans to discover all unique status occurrences (not just from the first span)
     * 2. Collects timing values (time since previous status) from all spans for each occurrence
     * 3. Calculates configured statistical metrics (e.g., Avg, P50, P75, P90)
     * 4. Returns the results in the order they appear in the first span, with any additional
     *    occurrences found only in later spans appended at the end
     *
     * Note: If the same status name appears multiple times within a span, each occurrence is tracked separately
     * with an enumeration (e.g., "[2] status", "[3] status"). The first occurrence uses the plain status name.
     *
     * The "Time Since Previous" metrics show the statistical timing for how long each status
     * typically takes to execute relative to the previous status within individual spans.
     * When aggregated across spans, these represent independent timing measurements.
     *
     * @param spans     List of spans to analyze (all spans should have the same span name)
     *
     * @return List of statistical data for each status occurrence, maintaining insertion order from the first span
     */
    private fun calculateStatistics(spans: List<IBenchmarkSpan>): List<StatisticalStatusData> {
        if (spans.isEmpty()) return emptyList()

        // Use LinkedHashMap to maintain insertion order based on first span's status sequence
        val statusOccurrencesMap = linkedMapOf<String, MutableList<Long>>() // Maps enumerated status name to timing values

        // First pass: Discover all unique status occurrences across ALL spans
        // This ensures we don't silently drop occurrences that only appear in later spans
        val allUniqueOccurrences = linkedSetOf<String>() // Maintains first-seen order

        for (span in spans) {
            val statuses = span.getStatuses()
            val spanStatusCounts = mutableMapOf<String, Int>()

            statuses.forEach { (statusName, _) ->
                val spanOccurrenceIndex = spanStatusCounts.getOrDefault(statusName, 0) + 1
                spanStatusCounts[statusName] = spanOccurrenceIndex
                val occurrenceStatusName = getStatusName(spanOccurrenceIndex, statusName)
                allUniqueOccurrences.add(occurrenceStatusName)
            }
        }

        // Initialize the map with all discovered status occurrences
        allUniqueOccurrences.forEach { occurrenceStatusName ->
            statusOccurrencesMap[occurrenceStatusName] = mutableListOf()
        }

        // Second pass: Collect timing data for each status occurrence from all spans
        for (span in spans) {
            val statuses = span.getStatuses()
            val startTime = span.getStartTimeInNanoSeconds()
            val spanStatusCounts = mutableMapOf<String, Int>()

            statuses.forEachIndexed { statusIndex, (statusName, timestamp) ->
                // Track how many times this status name has appeared in this specific span
                val spanOccurrenceIndex = spanStatusCounts.getOrDefault(statusName, 0) + 1
                spanStatusCounts[statusName] = spanOccurrenceIndex
                val occurrenceStatusName = getStatusName(spanOccurrenceIndex, statusName)

                // Now this will always succeed because we discovered all occurrences in the first pass
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

        // Map each status occurrence to its statistical data, maintaining insertion order
        return statusOccurrencesMap.map { (statusName, timeSincePreviousValues) ->
            StatisticalStatusData(
                statusName = statusName,
                timeSincePreviousStats = calculateMetrics(timeSincePreviousValues)
            )
        }
    }

    /**
     * Generates an enumerated status name for tracking duplicate status occurrences.
     *
     * For the first occurrence (occurrence index 1), returns the plain status name.
     * For subsequent occurrences (occurrence index 2+), returns the status name prefixed with the occurrence number in brackets.
     *
     * @param spanOccurrenceIndex The occurrence index of this status name within a span (1-based)
     * @param statusName The original status name
     * @return The enumerated status name (e.g., "status" for first occurrence, "[2] status" for second)
     */
    private fun getStatusName(spanOccurrenceIndex: Int, statusName: String): String {
        return if (spanOccurrenceIndex > 1) {
            "[$spanOccurrenceIndex] $statusName"
        } else {
            statusName
        }
    }

    /**
     * Calculates statistical metrics for a list of timing values.
     *
     * Only calculates the metrics configured in metricsToDisplay parameter.
     * Supports AVERAGE, P50, P75, P90, P95, and P99 metric types.
     *
     * @param values List of timing values in milliseconds
     * @return Map of MetricType to calculated metric value in milliseconds
     */
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

    /**
     * Calculates the percentile value from a list of values using linear interpolation.
     *
     * Uses the nearest-rank method with linear interpolation between values when the
     * percentile index falls between two data points.
     *
     * @param values List of timing values in milliseconds
     * @param percentile The percentile to calculate (0.0 to 100.0)
     * @return The calculated percentile value in milliseconds
     */
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
     * Retrieves and formats a metric value from the metrics map.
     *
     * Extracts the specified metric value and formats it with "ms" suffix.
     * Returns "0ms" if the metric type is not found in the map.
     *
     * @param metricsMap Map containing calculated metric values
     * @param metricType The type of metric to retrieve
     * @return Formatted metric value string with "ms" suffix
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
