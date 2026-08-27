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
package com.microsoft.identity.common.internal.result

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.internal.Streams
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.microsoft.identity.common.java.broker.telemetry.BrokerIpcTelemetry
import com.microsoft.identity.common.java.broker.telemetry.PerformanceRecord

/**
 * Enforces required-field presence when deserializing [BrokerIpcTelemetry] and
 * [PerformanceRecord] from the broker IPC response.
 *
 * Gson instantiates Kotlin classes reflectively, bypassing constructors and null-checks.
 * Without this factory a payload missing `correlation_id` still deserializes, leaving a
 * non-nullable `String` property holding null — the resulting NPE then surfaces far from
 * the parse site and is hard to trace back to a malformed broker response. Failing at the
 * parse boundary keeps the blast radius local.
 *
 * Note: this relies on [com.google.gson.internal.Streams], which is internal Gson API with
 * no compatibility guarantee. Re-verify this class when upgrading Gson.
 */
internal class BrokerIpcTelemetryTypeAdapterFactory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        val requiredFields = when (type.rawType) {
            BrokerIpcTelemetry::class.java -> BROKER_IPC_TELEMETRY_REQUIRED_FIELDS
            PerformanceRecord::class.java -> PERFORMANCE_RECORD_REQUIRED_FIELDS
            else -> return null
        }
        val delegate = gson.getDelegateAdapter(this, type)

        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T?) {
                delegate.write(out, value)
            }

            override fun read(input: JsonReader): T? {
                val element = Streams.parse(input)
                if (element.isJsonNull) {
                    return null
                }
                if (!element.isJsonObject) {
                    throw JsonSyntaxException("Broker IPC telemetry must be a JSON object.")
                }

                val jsonObject = element.asJsonObject
                requiredFields.forEach { field ->
                    if (!jsonObject.has(field) || jsonObject.get(field).isJsonNull) {
                        throw JsonSyntaxException("Broker IPC telemetry is missing required field: $field")
                    }
                }
                return delegate.fromJsonTree(element)
            }
        }
    }

    private companion object {
        val BROKER_IPC_TELEMETRY_REQUIRED_FIELDS = setOf(
            "schema_version",
            "correlation_id",
            "name",
            "version",
            "auth_outcome",
            "perf"
        )
        val PERFORMANCE_RECORD_REQUIRED_FIELDS = setOf(
            "version",
            "start_time",
            "duration",
            "execution_flow"
        )
    }
}
