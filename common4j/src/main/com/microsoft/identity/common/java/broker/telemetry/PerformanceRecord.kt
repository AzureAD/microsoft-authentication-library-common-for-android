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

import com.google.gson.annotations.SerializedName

/**
 * Aggregation container for the performance data captured during an authentication flow.
 *
 * @param version Schema version for this record. Defaults to [TelemetrySchema.CURRENT_VERSION].
 * @param startTime ISO 8601 UTC timestamp when the flow started.
 * @param duration Total elapsed time in milliseconds for the authentication flow.
 * @param executionFlow Ordered list of [ExecutionEvent]s captured during the flow.
 */
data class PerformanceRecord(
    @SerializedName("version") val version: String = TelemetrySchema.CURRENT_VERSION,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("duration") val duration: Long,
    @SerializedName("execution_flow") val executionFlow: List<ExecutionEvent>
)
