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
                // Write statistics for the spans (averages, percentiles, etc.)
                val statisticalData = calculateStatistics(spans)

                if (statisticalData.isEmpty()) {
                    val timestamp = DATE_FORMAT.format(Date())
                    writer.appendLine("| $timestamp | N/A | No status entries recorded (batch size: ${spans.size})")
                    return@use
                }

                // Write session header
                val formattedTimestamp = DATE_FORMAT.format(Date(System.currentTimeMillis()))

                // Calculate total duration using dedicated end times (convert nanoseconds to milliseconds)
                val totalDurationFormatted = spans.mapNotNull { span ->
                    val spanStartTime = span.getStartTimeInNanoSeconds()
                    val spanEndTime = span.getEndTimeInNanoSeconds()
                    if (spanEndTime > 0) { // Only include spans that have been ended
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

                // Calculate average concurrent size
                val avgConcurrentSize = spans.map { it.getConcurrentSilentRequestSize() }.average()
                val avgConcurrentSizeFormatted = String.format(Locale.US, "%.2f", avgConcurrentSize)

                writer.appendLine("")
                writer.appendLine("=== Statistical Benchmark Session: $formattedTimestamp | Avg Total Duration: $totalDurationFormatted | Avg Concurrent Size: $avgConcurrentSizeFormatted | Batch Size: ${spans.size} ===")
                writer.appendLine("")

                writer.appendLine("| Status Entry                                     | Metric | Time Since Previous | Time Since Start |")
                writer.appendLine("|--------------------------------------------------|--------|---------------------|------------------|")

                statisticalData.forEach { statsData ->
                    val paddedStatus = statsData.statusName.take(48).padEnd(48)

                    // Print only the configured metrics
                    metricsToDisplay.forEach { metricType ->
                        val metricLabel = metricType.displayName.padEnd(6)
                        val sincePrevValue = getMetricValue(statsData.timeSincePreviousStats, metricType).padEnd(19)
                        val sinceStartValue = getMetricValue(statsData.timeSinceStartStats, metricType).padEnd(16)

                        val statusColumn = if (metricType == metricsToDisplay.first()) {
                            paddedStatus
                        } else {
                            "".padEnd(48)
                        }

                        writer.appendLine("| $statusColumn | $metricLabel | $sincePrevValue | $sinceStartValue |")
                    }

                    // Separator line between status entries
                    writer.appendLine("|--------------------------------------------------|--------|---------------------|------------------|")
                }

                writer.appendLine("")

                // Check if any status contains "recordException" and print slowest exceptions
                val hasExceptions = statisticalData.any { it.statusName == "recordException" }
                if (hasExceptions) {
                    writeSlowestExceptions(writer, spans)
                }

                writer.flush()
            }
        } catch (e: IOException) {
            Logger.error(TAG, "IOException while writing averaged batch to file: $outputDirectoryAbsolutePath", e)
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
     * Print the 5 slowest exceptions (if any) in the batch.
     **/
    private fun writeSlowestExceptions(writer: FileWriter, spans: List<IBenchmarkSpan>) {
        // Collect all exception timings across all spans
        data class ExceptionTiming(val spanIndex: Int, val timeSinceStartMs: Long, val exception: Throwable)

        val exceptionTimings = mutableListOf<ExceptionTiming>()

        spans.forEachIndexed { index, span ->
            val exception = span.getException()
            if (exception != null) {
                val statuses = span.getStatuses()
                val startTime = span.getStartTimeInNanoSeconds()

                // Find the recordException status to get the timestamp
                val exceptionStatus = statuses.find { it.first == "recordException" }
                if (exceptionStatus != null) {
                    val timeSinceStartMs = TimeUnit.NANOSECONDS.toMillis(exceptionStatus.second - startTime)
                    exceptionTimings.add(ExceptionTiming(index + 1, timeSinceStartMs, exception))
                }
            }
        }

        if (exceptionTimings.isNotEmpty()) {
            // Sort by time since start (descending) and take top 5
            val slowestExceptions = exceptionTimings.sortedByDescending { it.timeSinceStartMs }.take(5)

            writer.appendLine("=== 5 Slowest Exceptions (Time Since Start) ===")
            writer.appendLine("")
            writer.appendLine("| Rank | Span # | Time Since Start | Exception Type                           | Message                                  ")
            writer.appendLine("|------|--------|------------------|------------------------------------------|------------------------------------------")

            slowestExceptions.forEachIndexed { rank, exceptionData ->
                val rankStr = (rank + 1).toString().padEnd(4)
                val spanNumStr = exceptionData.spanIndex.toString().padEnd(6)
                val timeStr = "${exceptionData.timeSinceStartMs}ms".padEnd(16)
                val exceptionType = exceptionData.exception.javaClass.simpleName.take(40).padEnd(40)
                val exceptionMessage = (exceptionData.exception.message ?: "N/A")
                writer.appendLine("| $rankStr | $spanNumStr | $timeStr | $exceptionType | $exceptionMessage ")
            }

            writer.appendLine("")
        }
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
     * @param spans     List of spans to analyze (all spans should have the same span name)
     *
     * @return List of statistical data for each status occurrence, sorted by the first configured metric's time since start
     */
    private fun calculateStatistics(spans: List<IBenchmarkSpan>): List<StatisticalStatusData> {
        if (spans.isEmpty()) return emptyList()

        // Build a map of status position -> (display name, list of timing data)
        // We need to track each occurrence separately across all spans
        data class StatusOccurrence(val statusName: String, val occurrenceIndex: Int)
        val statusOccurrencesMap = mutableMapOf<StatusOccurrence, MutableList<Pair<Long, Long>>>() // Pair<timeSincePrevious, timeSinceStart>

        for (span in spans) {
            val statuses = span.getStatuses()
            val startTime = span.getStartTimeInNanoSeconds()

            // Track how many times we've seen each status name in this span
            val statusCounts = mutableMapOf<String, Int>()

            statuses.forEachIndexed { statusIndex, (statusName, timestamp) ->
                // Determine which occurrence this is (1st, 2nd, 3rd, etc.)
                val occurrenceIndex = statusCounts.getOrDefault(statusName, 0) + 1
                statusCounts[statusName] = occurrenceIndex

                val timeSinceStartMs = TimeUnit.NANOSECONDS.toMillis(timestamp - startTime)

                val previousTime = if (statusIndex > 0) {
                    statuses[statusIndex - 1].second
                } else {
                    startTime
                }
                val timeSincePreviousMs = TimeUnit.NANOSECONDS.toMillis(timestamp - previousTime)

                val occurrence = StatusOccurrence(statusName, occurrenceIndex)
                statusOccurrencesMap.getOrPut(occurrence) { mutableListOf() }
                    .add(Pair(timeSincePreviousMs, timeSinceStartMs))
            }
        }

        val result = mutableListOf<StatisticalStatusData>()

        // Precompute the number of occurrences for each status name
        val occurrenceCountsByName = statusOccurrencesMap.keys
            .groupBy { it.statusName }
            .mapValues { it.value.size }

        for ((occurrence, timingPairs) in statusOccurrencesMap) {
            val timeSincePreviousValues = timingPairs.map { it.first }
            val timeSinceStartValues = timingPairs.map { it.second }

            // Create display name with occurrence number if there are multiple occurrences
            val displayName = if ((occurrenceCountsByName[occurrence.statusName] ?: 1) > 1) {
                "${occurrence.statusName} [${occurrence.occurrenceIndex}]"
            } else {
                occurrence.statusName
            }

            result.add(
                StatisticalStatusData(
                    statusName = displayName,
                    timeSinceStartStats = calculateMetrics(timeSinceStartValues),
                    timeSincePreviousStats = calculateMetrics(timeSincePreviousValues)
                )
            )
        }

        // Sort by the first configured metric's Time Since Start value, or by status name if no metrics
        return if (metricsToDisplay.isNotEmpty()) {
            result.sortedBy { it.timeSinceStartStats[metricsToDisplay.first()] ?: 0L }
        } else {
            result.sortedBy { it.statusName }
        }
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
     * containing both "time since start" and "time since previous" statistics
     * calculated across all spans in the batch.
     *
     * @property statusName                The name of the status entry (e.g., "acquireToken", "networkCall")
     * @property timeSinceStartStats       Map of metric type to value (in ms) for time elapsed from span start to this status
     * @property timeSincePreviousStats    Map of metric type to value (in ms) for time elapsed from previous status to this status
     */
    data class StatisticalStatusData(
        val statusName: String,
        val timeSinceStartStats: Map<MetricType, Long>,
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
