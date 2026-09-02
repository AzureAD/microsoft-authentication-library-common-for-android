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
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.microsoft.identity.common.java.broker.telemetry.BrokerIpcTelemetry
import com.microsoft.identity.common.java.broker.telemetry.EventTag
import com.microsoft.identity.common.java.broker.telemetry.ExecutionEvent
import com.microsoft.identity.common.java.broker.telemetry.PerformanceRecord
import com.microsoft.identity.common.java.logging.Logger

/**
 * Enforces required-field presence when deserializing [BrokerIpcTelemetry],
 * [PerformanceRecord] and [ExecutionEvent] from the broker IPC response, and maps
 * unrecognized [EventTag] names to [EventTag.Unknown].
 *
 * Gson instantiates Kotlin classes reflectively, bypassing constructors and null-checks.
 * Without this factory a payload missing `correlation_id` still deserializes, leaving a
 * non-nullable `String` property holding null — the resulting NPE then surfaces far from
 * the parse site and is hard to trace back to a malformed broker response. Failing at the
 * parse boundary keeps the blast radius local.
 *
 * Two distinct policies apply, because the two failures mean different things. A *malformed*
 * payload — a required field absent or explicitly null — is rejected, since the sender
 * violated the contract. An *unrecognized* [EventTag] is not malformed: broker and client ship
 * independently, so a newer broker legitimately emits tags this client has never heard of.
 * Rejecting the payload for that would discard an entire request's telemetry over one unknown
 * name, so it degrades to [EventTag.Unknown] instead.
 *
 * Buffering the payload into a [com.google.gson.JsonElement] before delegating is what makes the
 * presence check possible at all: the delegate adapter consumes the reader, so the fields have to
 * be inspected first. [JsonParser.parseReader] is used rather than the internal
 * `com.google.gson.internal.Streams`, so this class depends only on public Gson API.
 * `parseReader` additionally wraps a `StackOverflowError` from a pathologically nested payload
 * in a [com.google.gson.JsonParseException], which callers catching [Exception] can absorb.
 */
internal class BrokerIpcTelemetryTypeAdapterFactory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (type.rawType == EventTag::class.java) {
            @Suppress("UNCHECKED_CAST")
            val eventTagAdapter = EventTagAdapter as TypeAdapter<T>
            return eventTagAdapter
        }
        val requiredFields = when (type.rawType) {
            BrokerIpcTelemetry::class.java -> BROKER_IPC_TELEMETRY_REQUIRED_FIELDS
            PerformanceRecord::class.java -> PERFORMANCE_RECORD_REQUIRED_FIELDS
            ExecutionEvent::class.java -> EXECUTION_EVENT_REQUIRED_FIELDS
            else -> return null
        }
        val delegate = gson.getDelegateAdapter(this, type)
        val typeName = type.rawType.simpleName

        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T?) {
                delegate.write(out, value)
            }

            override fun read(input: JsonReader): T? {
                val element = JsonParser.parseReader(input)
                if (element.isJsonNull) {
                    return null
                }
                if (!element.isJsonObject) {
                    throw JsonSyntaxException("$typeName must be a JSON object.")
                }

                val jsonObject = element.asJsonObject
                requiredFields.forEach { field ->
                    if (!jsonObject.has(field) || jsonObject.get(field).isJsonNull) {
                        throw JsonSyntaxException("$typeName is missing required field: $field")
                    }
                }
                return delegate.fromJsonTree(element)
            }
        }
    }

    private companion object {
        private const val EVENT_TAG_ADAPTER_TAG =
            "BrokerIpcTelemetryTypeAdapterFactory:EventTagAdapter"

        // Unknown is deliberately excluded: it is a client-side sentinel, never a legitimate wire
        // value. Leaving it in would let a literal "Unknown" on the wire resolve silently, making
        // a genuinely unrecognized tag indistinguishable downstream from the sentinel echoed back.
        // Excluding it routes both through the same warning.
        private val EVENT_TAG_BY_NAME = EventTag.values()
            .filterNot { it == EventTag.Unknown }
            .associateBy { it.name }

        /**
         * Gson's default enum adapter yields null for an unrecognized name. Because Gson
         * instantiates Kotlin classes reflectively — bypassing constructors and their
         * null-checks — that null lands in [ExecutionEvent.tag], which is declared
         * non-nullable, and Kotlin does not null-check property reads, so it surfaces only
         * as an NPE at first dereference, far from the parse site. Mapping to
         * [EventTag.Unknown] keeps the timeline intact and the failure local.
         *
         * The unrecognized name is logged rather than carried on [ExecutionEvent], which has no
         * field for it and is serialized by the broker — synthesizing a client-only field would
         * put something on the wire contract that is never sent. Logging is per-occurrence and
         * deliberately not deduplicated: dedup state on this singleton would be shared across
         * every parse and thread, and an unrecognized tag means real broker/client version skew,
         * which is worth being noisy about. The name is a schema constant, never user data.
         *
         * Only a JSON string can be *unrecognized*; any other token — object, array, number,
         * boolean or null — is malformed, because the sender broke the contract rather than
         * merely outrunning it. Those are rejected, which propagates up and drops the whole
         * payload, consistent with how a missing required field is handled. Note this means
         * `"t": 123` is rejected rather than coerced to the string `"123"`, which is what
         * [JsonReader.nextString] would otherwise do.
         *
         * Two properties this adapter deliberately does not have:
         *  - It is not wrapped in [TypeAdapter.nullSafe]. That wrapper returns null for a JSON
         *    null without calling [read], which would put null into the non-nullable
         *    [ExecutionEvent.tag]. On today's only path that is defence-in-depth rather than
         *    load-bearing — the required-field check in the wrapping adapter rejects a null `t`
         *    before this adapter is reached — but it keeps the adapter correct in isolation, and
         *    correct for any future [EventTag] field that is not covered by a required-field set.
         *  - [write] is lossy for [EventTag.Unknown], which serializes as the literal string
         *    `"Unknown"` rather than the original name. This is unreachable today: the only Gson
         *    instance carrying this factory is `MsalBrokerResultAdapter.BROKER_IPC_TELEMETRY_GSON`,
         *    which is read-only (`fromJson` at one call site, no `toJson`). It would matter only
         *    if a parsed payload were ever re-serialized with that instance and forwarded.
         */
        private object EventTagAdapter : TypeAdapter<EventTag>() {
            override fun write(out: JsonWriter, value: EventTag?) {
                out.value(value?.name)
            }

            override fun read(input: JsonReader): EventTag {
                val token = input.peek()
                if (token != JsonToken.STRING) {
                    throw JsonSyntaxException(
                        "Event tag must be a JSON string, but was $token."
                    )
                }
                val name = input.nextString()
                return EVENT_TAG_BY_NAME[name] ?: run {
                    Logger.warn(
                        EVENT_TAG_ADAPTER_TAG,
                        "Unrecognized event tag '$name'; mapping to ${EventTag.Unknown.name}. " +
                            "This client is likely older than the broker that produced the payload."
                    )
                    EventTag.Unknown
                }
            }
        }

        // Note on the two version fields below: BrokerIpcTelemetry.schemaVersion and
        // PerformanceRecord.version declare Kotlin defaults, which can read as "optional".
        // Those defaults are dead on this path — Gson instantiates reflectively and bypasses
        // constructors, so a default can never fill a field the payload omitted. Both are
        // therefore required here: the wire contract obliges the sender to state its version
        // explicitly rather than letting an absent field masquerade as the current one.
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

        // Only the two fields ExecutionEvent declares without a Kotlin default. As with the
        // version fields above, those defaults are dead on this path, so an absent tid yields
        // 0 rather than the current thread. That is tolerable for a purely diagnostic field —
        // unlike a version, where a silently wrong value would misrepresent the payload — and
        // s and e are nullable by design, so their absence is meaningful rather than malformed.
        val EXECUTION_EVENT_REQUIRED_FIELDS = setOf(
            "t",
            "ts"
        )
    }
}
