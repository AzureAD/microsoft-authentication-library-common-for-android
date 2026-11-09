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

internal object Codec {
    private fun strings(value: com.google.gson.JsonObject, name: String): List<String> =
        value.list(name).map {
            ensure(it.isJsonPrimitive && it.asJsonPrimitive.isString, "Invalid string list")
            it.asString
        }

    fun proposal(text: String): Proposal {
        val root = Json.parse(text).obj()
        root.fields(setOf("version", "rules"))
        ensure(root.number("version") == 1L, "Unsupported proposal version")
        val rules = root.list("rules").map { item ->
            val value = item.obj()
            value.fields(setOf("id", "statement", "evidenceIds", "edits"))
            val edits = value.list("edits").map {
                val edit = it.obj()
                edit.fields(setOf("path", "before", "after"))
                Edit(edit.text("path"), edit.text("before"), edit.text("after"))
            }
            Rule(value.text("id"), value.text("statement"), strings(value, "evidenceIds"), edits)
        }
        return Proposal(1, rules)
    }

    fun state(text: String): State {
        val root = Json.parse(text).obj()
        root.fields(setOf("version", "repository", "pendingPrs", "decisions"), setOf("active", "lastNoProposalFingerprint"))
        ensure(root.number("version") == 1L, "Unsupported checkpoint version")
        val active = root.get("active")?.takeUnless { it.isJsonNull }?.obj()?.let {
            it.fields(setOf("number", "head", "evidenceIds"))
            Active(it.number("number"), it.text("head"), strings(it, "evidenceIds"))
        }
        val pending = root.list("pendingPrs").map {
            val obj = com.google.gson.JsonObject().also { value -> value.add("number", it) }
            obj.number("number").also { number -> ensure(number > 0, "Invalid checkpoint PR") }
        }
        val decisions = root.list("decisions").map {
            val value = it.obj()
            value.fields(setOf("evidenceId", "outcome", "recordedAt"))
            Decision(value.text("evidenceId"), value.text("outcome"), value.text("recordedAt")).also { decision ->
                ensure(decision.evidenceId.matches(Regex("[0-9a-f]{64}")) &&
                    decision.outcome in setOf("accepted", "declined"), "Invalid evidence disposition")
                try { java.time.Instant.parse(decision.recordedAt) } catch (_: java.time.format.DateTimeParseException) {
                    throw LearningFailure("Invalid checkpoint timestamp")
                }
            }
        }
        ensure(pending.size <= 500 && decisions.size <= 10_000, "Checkpoint exceeds the pilot bound")
        ensure(decisions.map { it.evidenceId }.distinct().size == decisions.size, "Duplicate checkpoint disposition")
        val fingerprint = root.get("lastNoProposalFingerprint")?.takeUnless { it.isJsonNull }?.let {
            root.text("lastNoProposalFingerprint").also { value ->
                ensure(value.matches(Regex("[0-9a-f]{64}")), "Invalid synthesis fingerprint")
            }
        }
        return State(repository = root.text("repository"), pendingPrs = pending, decisions = decisions, active = active,
            lastNoProposalFingerprint = fingerprint)
    }
}

internal class Validator(private val repository: Repository) {
    suspend fun validate(packet: Packet, proposal: Proposal): Validated {
        val policy = repository.policy
        ensure(packet.repository == policy.repository && sha(packet.baseSha), "Proposal repository/base mismatch")
        ensure(proposal.version == 1 && proposal.rules.size <= 3, "Invalid proposal size/version")
        ensure(proposal.rules.isEmpty() || packet.hasSignal, "No-signal packets cannot produce guidance")
        ensure(proposal.rules.map { it.id }.distinct().size == proposal.rules.size, "Duplicate proposed rule")
        val originals = mutableMapOf<String, String>()
        val allEdits = mutableListOf<Edit>()
        for (rule in proposal.rules) {
            ensure(rule.id.matches(Regex("[a-z][a-z0-9-]{2,79}")) && rule.statement.length in 20..1200,
                "Invalid focused rule")
            ensure(rule.edits.isNotEmpty() && rule.edits.size <= 3, "A rule needs bounded edits")
            ensure(rule.evidenceIds.isNotEmpty() && rule.evidenceIds.distinct().size == rule.evidenceIds.size,
                "Invalid rule citations")
            val supporting = rule.evidenceIds.map { id ->
                packet.evidence.singleOrNull { it.id == id } ?: throw LearningFailure("Unverifiable evidence citation")
            }
            val independent = supporting.map { it.citation.pr }.distinct().size >= 2 &&
                supporting.map { it.citation.location.commit }.distinct().size >= 2 &&
                supporting.map { it.citation.location.path to it.citation.codeHash }.distinct().size >= 2
            ensure(independent || supporting.any { it.severe && it.citation.actorId in policy.ownerIds },
                "Rules need independent PR/code support or owner-approved severe capture")
            Safety.verify(rule.statement, policy)
            for (edit in rule.edits) {
                ensure(edit.path in policy.guidancePaths && safePath(edit.path), "Edit outside approved guidance paths")
                ensure(edit.before.isNotBlank() && edit.after.isNotBlank() && edit.before != edit.after, "Invalid exact-context edit")
                ensure(edit.before.lines().size <= 30 && edit.after.lines().size <= 60 && edit.after.length <= 8000,
                    "Guidance edit exceeds the bound")
                Safety.verify(edit.after, policy)
                if (!originals.containsKey(edit.path)) {
                    originals[edit.path] = repository.source(edit.path, packet.baseSha)
                        ?: throw LearningFailure("The guidance topic must already exist")
                }
                ensure(!normalized(originals.getValue(edit.path)).contains(normalized(rule.statement)),
                    "Proposed rule is already represented in guidance")
                allEdits.add(edit)
            }
        }
        ensure(originals.size <= 3, "Too many guidance topics in one proposal")
        val files = allEdits.groupBy { it.path }.mapValues { (path, edits) ->
            val original = originals.getValue(path)
            val ranges = edits.map { edit ->
                val start = original.indexOf(edit.before)
                ensure(start >= 0 && original.indexOf(edit.before, start + 1) == -1, "Edit context is missing or ambiguous")
                Triple(start, start + edit.before.length, edit.after)
            }.sortedBy { it.first }
            ensure(ranges.zipWithNext().all { (first, second) -> first.second <= second.first }, "Overlapping guidance edits")
            var result = original
            for ((first, last, after) in ranges.asReversed()) result = result.substring(0, first) + after + result.substring(last)
            result
        }
        return Validated(packet, proposal, files)
    }
}

internal data class Bundle(val version: Int = 1, val repository: String, val baseSha: String, val stateSha: String?,
    val proposal: Proposal)

internal fun bundle(text: String): Bundle {
    val root = Json.parse(text).obj()
    root.fields(setOf("version", "repository", "baseSha", "proposal"), setOf("stateSha"))
    ensure(root.number("version") == 1L, "Unsupported publication version")
    return Bundle(repository = root.text("repository"), baseSha = root.text("baseSha"),
        stateSha = root.get("stateSha")?.takeUnless { it.isJsonNull }?.asString,
        proposal = Codec.proposal(root.get("proposal").toString()))
}

internal class Publisher(private val repository: Repository, private val now: java.time.Instant) {
    private val statePath = ".github/review-learning-state.json"
    private val manifestPath = ".github/review-learning-proposal.json"

    suspend fun checkpoint(): Pair<State, String?> {
        val current = repository.branchSha(repository.policy.stateBranch)
        var state = if (current == null) State(repository = repository.policy.repository) else {
            val content = repository.source(statePath, current)
                ?: throw LearningFailure("Checkpoint branch exists without a state file")
            Codec.state(content)
        }
        ensure(state.repository == repository.policy.repository, "Checkpoint belongs to a different repository")
        if (state.active == null) {
            val proposals = (repository.openLearningPulls() +
                repository.recentPulls(now.minus(repository.policy.lookbackDays.toLong(), java.time.temporal.ChronoUnit.DAYS))
                    .filter { it.head.startsWith(repository.policy.proposalPrefix) &&
                        it.closedAt?.isBefore(now.minus(repository.policy.lookbackDays.toLong(),
                            java.time.temporal.ChronoUnit.DAYS)) == false }).distinctBy { it.number }
            ensure(proposals.count { it.open } <= 1, "Multiple active learning PRs require reconciliation")
            for (pull in proposals) {
                ensure(pull.author.type == "Bot" && pull.author.id in repository.policy.automationIds,
                    "Learning PR publisher identity is not approved")
                val text = repository.source(manifestPath, pull.headSha)
                    ?: throw LearningFailure("Learning PR is missing its committed provenance manifest")
                val manifest = Json.parse(text).obj()
                manifest.fields(setOf("bundle", "sourcePrs"))
                val input = bundle(manifest.get("bundle").toString())
                val ids = input.proposal.rules.flatMap { it.evidenceIds }.distinct()
                if (!pull.open && ids.all { id -> state.decisions.any { it.evidenceId == id } }) continue
                val sourcePrs = manifest.list("sourcePrs").map {
                    val value = com.google.gson.JsonObject().also { obj -> obj.add("id", it) }
                    value.number("id").also { id -> ensure(id > 0, "Invalid manifest source PR") }
                }
                ensure(sourcePrs.size <= 80 && input.repository == repository.policy.repository, "Invalid provenance manifest")
                val source = Extractor(repository, now).extract(State(repository = repository.policy.repository,
                    pendingPrs = sourcePrs), current).copy(baseSha = input.baseSha, hasSignal = true)
                val validated = Validator(repository).validate(source, input.proposal)
                ensure(validated.files.isNotEmpty() &&
                    repository.matchesCommit(pull.headSha, input.baseSha, validated.files + (manifestPath to text)),
                    "Learning branch provenance changed; human reconciliation is required")
                if (pull.merged) repository.requireHumanApproval(pull)
                state = if (pull.open) state.copy(active = Active(pull.number, pull.head, ids)) else state.copy(
                    decisions = (state.decisions + ids.map { Decision(it, if (pull.merged) "accepted" else "declined", now.toString()) })
                        .distinctBy { it.evidenceId })
            }
        }
        return state to current
    }

    suspend fun publish(input: Bundle): String {
        val policy = repository.policy
        ensure(input.repository == policy.repository, "Publication repository mismatch")
        repository.verifyPublicationPolicy()
        val (state, previous) = checkpoint()
        ensure(input.stateSha == previous, "Checkpoint changed; rerun instead of overwriting it")
        val packet = Extractor(repository, now).extract(state, previous)
        ensure(packet.baseSha == input.baseSha, "Target branch changed; rerun synthesis")
        if (packet.reason == "active_guidance_pr") {
            ensure(input.proposal.rules.isEmpty(), "A guidance PR is already being reviewed")
            save(packet.nextState, previous, packet.baseSha)
            return "queued"
        }
        val validated = Validator(repository).validate(packet, input.proposal)
        if (validated.files.isEmpty()) {
            val next = if (packet.hasSignal) packet.nextState.copy(lastNoProposalFingerprint = evidenceFingerprint(packet))
                else packet.nextState
            save(next, previous, packet.baseSha)
            return "checkpointed"
        }
        val branch = policy.proposalPrefix + hash(input.baseSha + "/" + input.stateSha + "/" + Json.encode(input.proposal)).take(16)
        val manifest = Json.encode(mapOf("bundle" to input,
            "sourcePrs" to input.proposal.rules.flatMap { rule -> rule.evidenceIds.map { id ->
                packet.evidence.single { it.id == id }.citation.pr } }.distinct()))
        val files = validated.files + (manifestPath to manifest)
        val existing = repository.branchSha(branch)
        if (existing == null) {
            val commit = repository.commit(packet.baseSha, files,
                "Propose citation-backed review guidance\n\nCo-authored-by: Copilot App <223556219+Copilot@users.noreply.github.com>")
            repository.updateBranch(branch, null, commit)
        } else {
            ensure(repository.matchesCommit(existing, packet.baseSha, files),
                "Proposal branch changed; refusing to overwrite human edits")
        }
        val number = repository.createDraft(branch, body(validated))
        val ids = input.proposal.rules.flatMap { it.evidenceIds }.distinct()
        save(packet.nextState.copy(active = Active(number, branch, ids)), previous, packet.baseSha)
        return "draft_created"
    }

    private suspend fun save(state: State, previous: String?, base: String) {
        val text = Json.encode(state)
        if (previous != null && repository.source(statePath, previous) == text) return
        val next = repository.commit(previous ?: base, mapOf(statePath to text),
            "Checkpoint review-learning evidence\n\nCo-authored-by: Copilot App <223556219+Copilot@users.noreply.github.com>")
        repository.updateBranch(repository.policy.stateBranch, previous, next)
    }

    private fun body(validated: Validated): String = buildString {
        append("Human approval and merge are required. Existing GitHub review remains unchanged.\n\n")
        append("Generated by the learning-only workflow. A repository owner must arrange the required checks before merge.\n\n")
        for (rule in validated.proposal.rules) {
            append("### ${rule.id}\n\n${rule.statement}\n\n")
            append("Guidance topics: ${rule.edits.map { "`${it.path}`" }.distinct().joinToString(", ")}.\n\n")
            for (id in rule.evidenceIds) {
                val citation = validated.packet.evidence.single { it.id == id }.citation
                append("- [Human evidence](${citation.url}); `${citation.location.path}` ")
                append("at `${citation.location.commit}`, lines ${citation.location.first}-${citation.location.last}.\n")
            }
        }
        append("\n<!-- review-learning:v1 -->\n")
    }
}
