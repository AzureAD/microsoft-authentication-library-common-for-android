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
 * Top-level telemetry schema emitted by the broker for a single authentication request.
 * Captures metadata, auth outcome, and a structured performance record.
 *
 * @param schemaVersion Version of this schema format. Defaults to [CURRENT_VERSION].
 * @param correlationId Correlation ID linking this schema to the authentication request.
 * @param name Optional name of the authentication operation.
 * @param version Optional version string of the caller SDK.
 * @param authOutcome Optional outcome of the authentication (e.g., "success", "failure").
 * @param errorCode Optional error code if the authentication failed.
 * @param responseStarvationDuration Optional duration in milliseconds the client waited
 *   without a response.
 * @param powerPolicy Optional flag indicating whether power policy restrictions were active.
 * @param deviceIdle Optional flag indicating whether the device was in idle mode.
 * @param performanceRecord Optional [PerformanceRecord] containing the detailed event timeline.
 */
data class TelemetrySchema(
    @SerializedName("schema_version") val schemaVersion: String = CURRENT_VERSION,
    @SerializedName("correlation_id") val correlationId: String,
    @SerializedName("name") val name: String? = null,
    @SerializedName("version") val version: String? = null,
    @SerializedName("auth_outcome") val authOutcome: String? = null,
    @SerializedName("error_code") val errorCode: String? = null,
    @SerializedName("response_starvation_duration") val responseStarvationDuration: Int? = null,
    @SerializedName("power_policy") val powerPolicy: Boolean? = null,
    @SerializedName("device_idle") val deviceIdle: Boolean? = null,
    @SerializedName("perf") val performanceRecord: PerformanceRecord? = null
) {
    companion object {
        const val CURRENT_VERSION = "1.0.0"
        const val MIN_SUPPORTED_MAJOR = 1
    }
}
