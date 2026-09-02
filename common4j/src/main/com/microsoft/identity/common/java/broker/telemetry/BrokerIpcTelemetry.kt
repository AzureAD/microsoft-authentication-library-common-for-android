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
 * Nullability of each property mirrors the **Optional** column of the parent Unified Broker
 * Telemetry contract: contract-required fields are declared non-null so a population site
 * cannot omit them, while genuinely optional fields remain nullable.
 *
 * Note that non-null declarations are enforced at **construction** time only. Gson populates
 * instances reflectively without invoking the Kotlin constructor, so a malformed inbound
 * payload can still leave a non-null-typed property holding `null`. Consumers deserializing
 * untrusted JSON must validate rather than rely on the declared types.
 *
 * @param schemaVersion Version of this schema format. Defaults to [CURRENT_VERSION].
 * @param correlationId Correlation ID linking this schema to the authentication request.
 * @param name Logical name of the broker that produced this payload, e.g. `authenticator`,
 *   `LTW`, `companyportal`.
 * @param version Version string of the broker application that produced this payload.
 * @param authOutcome Outcome of the authentication (e.g., "success", "failure").
 * @param errorCode Optional error code if the authentication failed.
 * @param responseStarvationDuration Optional time in milliseconds the broker spent waiting on
 *   the thread pool before it began executing the request. Per the Unified Broker Telemetry
 *   contract, this field may be retired once brokers fully adopt `execution_flow`.
 * @param powerPolicy Optional flag indicating whether power policy restrictions were active.
 * @param deviceIdle Optional flag indicating whether the device was in idle mode.
 * @param performanceRecord [PerformanceRecord] containing the detailed event timeline.
 */
data class BrokerIpcTelemetry(
    @SerializedName("schema_version") val schemaVersion: String = CURRENT_VERSION,
    @SerializedName("correlation_id") val correlationId: String,
    @SerializedName("name") val name: String,
    @SerializedName("version") val version: String,
    @SerializedName("auth_outcome") val authOutcome: String,
    @SerializedName("error_code") val errorCode: String? = null,
    @SerializedName("response_starvation_duration") val responseStarvationDuration: Int? = null,
    @SerializedName("power_policy") val powerPolicy: Boolean? = null,
    @SerializedName("device_idle") val deviceIdle: Boolean? = null,
    @SerializedName("perf") val performanceRecord: PerformanceRecord
) {
    companion object {
        /**
         * Current schema version emitted by the broker, in `MAJOR.MINOR.PATCH` form.
         */
        const val CURRENT_VERSION = "1.0.0"

        /**
         * Minimum schema **major** version a consumer is able to deserialize.
         *
         * Only the major component is compared, not the full [CURRENT_VERSION] string:
         * under semantic versioning a major bump signals a breaking schema change, while
         * minor and patch bumps remain backward-compatible and must not be rejected.
         *
         * Reserved for the schema version negotiation introduced with the flight-gated
         * broker integration; no consumer exists yet, and v1 accepts all payloads.
         */
        const val MIN_SUPPORTED_MAJOR = 1
    }
}
