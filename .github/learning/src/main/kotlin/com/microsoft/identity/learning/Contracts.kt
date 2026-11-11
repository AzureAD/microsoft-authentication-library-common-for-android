// Copyright (c) Microsoft Corporation. All rights reserved.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package com.microsoft.identity.learning

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.StringReader
import java.math.BigDecimal
import java.security.MessageDigest
import java.time.Instant

internal open class LearningFailure(message: String) : RuntimeException(message)
internal class SourceTooLarge : LearningFailure("Source file exceeds the limit")
internal const val GITHUB_ACTIONS_BOT_ID = 41898282L

internal fun ensure(condition: Boolean, message: String) {
    if (!condition) throw LearningFailure(message)
}

internal object Json {
    private val gson = GsonBuilder().disableHtmlEscaping().create()

    fun encode(value: Any): String = gson.toJson(value)

    fun parse(text: String): JsonElement {
        ensure(text.length <= 2_000_000, "JSON exceeds the input limit")
        try {
            JsonReader(StringReader(text)).use { reader ->
                reader.isLenient = false
                val result = read(reader, 0)
                ensure(reader.peek() == JsonToken.END_DOCUMENT, "Trailing JSON data")
                return result
            }
        } catch (_: java.io.IOException) {
            throw LearningFailure("Malformed JSON contract")
        }
    }

    private fun read(reader: JsonReader, depth: Int): JsonElement {
        ensure(depth <= 32, "JSON nesting exceeds the limit")
        return when (reader.peek()) {
            JsonToken.BEGIN_OBJECT -> JsonObject().also { result ->
                reader.beginObject()
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    ensure(!result.has(name), "Duplicate JSON field")
                    result.add(name, read(reader, depth + 1))
                }
                reader.endObject()
            }
            JsonToken.BEGIN_ARRAY -> JsonArray().also { result ->
                reader.beginArray()
                while (reader.hasNext()) result.add(read(reader, depth + 1))
                reader.endArray()
            }
            JsonToken.STRING -> JsonPrimitive(reader.nextString())
            JsonToken.NUMBER -> JsonPrimitive(BigDecimal(reader.nextString()))
            JsonToken.BOOLEAN -> JsonPrimitive(reader.nextBoolean())
            JsonToken.NULL -> {
                reader.nextNull()
                JsonNull.INSTANCE
            }
            else -> throw LearningFailure("Invalid JSON token")
        }
    }
}

internal fun JsonElement.obj(): JsonObject {
    ensure(isJsonObject, "Expected a JSON object")
    return asJsonObject
}

internal fun JsonObject.fields(required: Set<String>, optional: Set<String> = emptySet()) {
    ensure(keySet().containsAll(required), "Missing contract field")
    ensure((keySet() - required - optional).isEmpty(), "Unexpected contract field")
}

internal fun JsonObject.text(name: String): String {
    val value = get(name)
    ensure(value != null && value.isJsonPrimitive && value.asJsonPrimitive.isString, "Invalid text field: $name")
    return value.asString
}

internal fun JsonObject.number(name: String): Long {
    val value = get(name)
    ensure(value != null && value.isJsonPrimitive && value.asJsonPrimitive.isNumber, "Invalid number field: $name")
    return try {
        value.asBigDecimal.longValueExact()
    } catch (_: ArithmeticException) {
        throw LearningFailure("Invalid integer field: $name")
    }
}

internal fun JsonObject.flag(name: String): Boolean {
    val value = get(name)
    ensure(value != null && value.isJsonPrimitive && value.asJsonPrimitive.isBoolean, "Invalid boolean field: $name")
    return value.asBoolean
}

internal fun JsonObject.list(name: String): List<JsonElement> {
    val value = get(name)
    ensure(value != null && value.isJsonArray, "Invalid array field: $name")
    return value.asJsonArray.toList()
}

internal fun hash(value: String): String =
    MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

internal fun normalized(value: String): String = value.lowercase().replace(Regex("\\s+"), " ").trim()

internal fun safePath(path: String): Boolean =
    path.isNotBlank() && path.length <= 300 &&
        path.split('/').all { it.isNotEmpty() && it != "." && it != ".." } &&
        path.none { it == '\\' || it == ':' || it.code < 32 || it.code == 127 }

internal fun sha(value: String): Boolean = value.matches(Regex("[0-9a-f]{40}"))

internal data class Policy(
    val host: String,
    val repository: String,
    val base: String,
    val humanIds: Set<Long>,
    val ownerIds: Set<Long>,
    val automationIds: Set<Long>,
    val guidancePaths: Set<String>,
    val lookbackDays: Int = 30,
    val maxPages: Int = 10
) {
    val api: String get() = if (host == "github.com") "https://api.github.com" else "https://api.$host"
    val web: String get() = "https://$host/$repository"
    val stateBranch: String get() = "copilot/review-learning-state"
    val proposalPrefix: String get() = "copilot/review-learning/"

    fun verify() {
        ensure(host in setOf("github.com", "msft.ghe.com"), "Unsupported host")
        ensure(repository.matches(Regex("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")), "Invalid repository")
        ensure(base == "dev", "This pilot only targets dev")
        ensure(humanIds.isNotEmpty() && ownerIds.isNotEmpty(), "Approved human and owner identity registries are required")
        ensure((humanIds + ownerIds).all { it > 0 } && automationIds.all { it > 0 }, "Invalid identity registry")
        ensure(((humanIds + ownerIds) intersect automationIds).isEmpty(), "Conflicting identity registries")
        ensure(lookbackDays in 1..180 && maxPages in 1..20, "Invalid extraction bounds")
        ensure(guidancePaths.isNotEmpty() && guidancePaths.size <= 20, "Invalid guidance allowlist")
        ensure(guidancePaths.all {
            safePath(it) && (it == ".github/copilot-instructions.md" ||
                (it.startsWith(".github/skills/code-review/references/") && it.endsWith(".md")))
        }, "Only existing review instructions and focused references may be edited")
    }

    fun human(actor: Actor): Boolean =
        actor.type == "User" && actor.id in humanIds + ownerIds && actor.id !in automationIds

    companion object {
        fun parse(text: String): Policy {
            val root = Json.parse(text).obj()
            root.fields(setOf("version", "host", "repository", "base", "humanIds", "ownerIds", "automationIds", "guidancePaths"),
                setOf("lookbackDays", "maxPages"))
            ensure(root.number("version") == 1L, "Unsupported policy version")
            fun ids(name: String): Set<Long> = root.list(name).map {
                val wrapper = JsonObject().also { obj -> obj.add("id", it) }
                wrapper.number("id")
            }.toSet()
            val paths = root.list("guidancePaths").map {
                ensure(it.isJsonPrimitive && it.asJsonPrimitive.isString, "Invalid guidance path")
                it.asString
            }.toSet()
            fun bound(name: String, default: Int, maximum: Int): Int {
                if (!root.has(name)) return default
                val number = root.number(name)
                ensure(number in 1..maximum.toLong(), "Invalid extraction bound")
                return number.toInt()
            }
            return Policy(root.text("host"), root.text("repository"), root.text("base"),
                ids("humanIds"), ids("ownerIds"), ids("automationIds"), paths,
                bound("lookbackDays", 30, 180), bound("maxPages", 10, 20)).also { it.verify() }
        }
    }
}

internal data class Actor(val id: Long, val type: String)
internal data class Pull(val number: Long, val author: Actor, val base: String, val head: String, val updated: Instant,
    val merged: Boolean, val open: Boolean, val headSha: String, val url: String, val closedAt: Instant? = null)
internal data class Comment(val id: Long, val kind: String, val pr: Long, val actor: Actor, val body: String,
    val url: String, val path: String? = null, val commit: String? = null, val line: Int? = null,
    val rootId: Long? = null, val threadResolved: Boolean? = null, val automated: Boolean = false,
    val reviewState: String? = null, val reviewCommit: String? = null, val reviewedAt: String? = null)
internal data class Location(val path: String, val commit: String, val first: Int, val last: Int)
internal data class Citation(val commentId: Long, val kind: String, val pr: Long, val actorId: Long,
    val bodyHash: String, val url: String, val location: Location, val codeHash: String,
    val authorSourceId: Long? = null, val authorSourceHash: String? = null)
internal data class Evidence(val id: String, val rule: String, val citation: Citation, val code: String,
    val severe: Boolean = false, val threadResolved: Boolean? = null)
internal data class Decision(val evidenceId: String, val outcome: String, val recordedAt: String)
internal data class Active(val number: Long, val head: String, val evidenceIds: List<String>)
internal data class State(val version: Int = 1, val repository: String, val pendingPrs: List<Long> = emptyList(),
    val decisions: List<Decision> = emptyList(), val active: Active? = null, val lastNoProposalFingerprint: String? = null)
internal data class Packet(val version: Int = 1, val repository: String, val baseSha: String, val stateSha: String?,
    val hasSignal: Boolean, val reason: String, val evidence: List<Evidence>, val nextState: State)
internal data class Edit(val path: String, val before: String, val after: String)
internal data class Rule(val id: String, val statement: String, val evidenceIds: List<String>, val edits: List<Edit>)
internal data class Proposal(val version: Int, val rules: List<Rule>)
internal data class Validated(val packet: Packet, val proposal: Proposal, val files: Map<String, String>)

internal fun evidenceFingerprint(packet: Packet): String =
    hash(packet.baseSha + "/" + packet.evidence.map { it.id }.sorted().joinToString("/"))

internal suspend fun Repository.requireHumanApproval(pull: Pull) {
    val latest = mutableMapOf<Long, Pair<Instant, Comment>>()
    for (review in comments(pull.number)) {
        if (review.kind != "review" || !policy.human(review.actor) || review.automated ||
            review.actor.id == pull.author.id || review.reviewState !in setOf("APPROVED", "CHANGES_REQUESTED", "DISMISSED")) continue
        val submitted = review.reviewedAt ?: continue
        val time = try { Instant.parse(submitted) } catch (_: java.time.format.DateTimeParseException) {
            throw LearningFailure("Invalid review timestamp")
        }
        val previous = latest[review.actor.id]
        if (previous == null || time > previous.first || (time == previous.first && review.id > previous.second.id)) {
            latest[review.actor.id] = time to review
        }
    }
    ensure(latest.values.any { it.second.reviewState == "APPROVED" && it.second.reviewCommit == pull.headSha },
        "Merged guidance has no verifiable current-head approval from an independent registered human")
}
