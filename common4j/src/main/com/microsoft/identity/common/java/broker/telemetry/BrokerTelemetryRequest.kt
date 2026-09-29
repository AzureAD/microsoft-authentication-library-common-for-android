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
 * The client to broker telemetry request blob, serialized onto the request Bundle under
 * `AuthenticationConstants.Broker.BROKER_TELEMETRY_REQUEST`.
 *
 * This is the counterpart of [BrokerIpcTelemetry]: the client declares which schema version it
 * understands, and the broker constructs its response payload in that version's format. The
 * wire contract is deliberately minimal:
 *
 * ```json
 * { "correlation_id": "uuid-v4-string", "schema_version": "1.0.0" }
 * ```
 *
 * Key *presence* is the opt-in signal. A request Bundle without the key means telemetry was not
 * requested, which is also how an older client appears to a newer broker; the broker must treat
 * an absent blob as "not requested" rather than as a malformed payload.
 *
 * Only the fields above are carried. Internal accumulator state such as
 * [EventCollector.startTimeNanos] is process-local and meaningless once it crosses the IPC
 * boundary, and any client-side events already recorded have no consumer in the broker.
 *
 * The blob is intentionally extensible: future flows may add fields (for example a session
 * correlation ID for multi-IPC onboarding) without altering the two fields defined here.
 *
 * @param correlationId UUID v4 matching the correlation ID of the authentication request this
 *   telemetry belongs to. Used to stitch the broker's payload to client-side events at query
 *   time.
 * @param schemaVersion Highest [BrokerIpcTelemetry] version this client is able to consume, in
 *   `MAJOR.MINOR.PATCH` form. Defaults to [BrokerIpcTelemetry.CURRENT_VERSION].
 */
data class BrokerTelemetryRequest @JvmOverloads constructor(
    @SerializedName("correlation_id") val correlationId: String,
    @SerializedName("schema_version") val schemaVersion: String =
        BrokerIpcTelemetry.CURRENT_VERSION
) {
    companion object {
        /**
         * JSON key for [correlationId], so the broker parses the same literal the client emits.
         */
        const val KEY_CORRELATION_ID = "correlation_id"

        /**
         * JSON key for [schemaVersion], so the broker parses the same literal the client emits.
         */
        const val KEY_SCHEMA_VERSION = "schema_version"
    }
}
