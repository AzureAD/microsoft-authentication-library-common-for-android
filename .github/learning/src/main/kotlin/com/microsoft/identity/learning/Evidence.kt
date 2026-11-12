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

import java.time.Instant
import java.time.temporal.ChronoUnit

internal object Safety {
    private val forbidden = listOf(
        Regex("-----BEGIN [A-Z ]*PRIVATE KEY-----"),
        Regex("\\b(?:gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,})\\b"),
        Regex("\\beyJ[A-Za-z0-9_-]{12,}\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\b"),
        Regex("(?i)\\b(?:password|access[_-]?token|client[_-]?secret)\\s*[:=]\\s*[\"'][^\"']{8,}[\"']"),
        Regex("(?i)(?:ignore|disregard|override) (?:all |the |your |previous |prior |system )*(?:instructions|rules|safety)"),
        Regex("(?i)(?:allow-all-tools|--yolo|disable.*(?:approval|validation)|exfiltrat)"),
        Regex("https://[^\\s/]+:[^\\s/]+@")
    )

    fun verify(text: String, policy: Policy) {
        ensure(forbidden.none { it.containsMatchIn(text) }, "Sensitive or instruction-injection content rejected")
        if (policy.host == "github.com") {
            ensure(!Regex("(?i)https?://(?:[^/\\s]+\\.ghe\\.com|(?:dev\\.azure\\.com|[^/\\s]+\\.visualstudio\\.com))/")
                .containsMatchIn(text), "Private-host references cannot be published in public guidance")
        }
    }
}

internal data class Adjudication(val sourceId: Long, val rule: String, val location: Location, val severe: Boolean)

internal fun adjudication(comment: Comment): Adjudication? {
    val matches = Regex("```review-learning-adjudication\\s*\\n([\\s\\S]*?)\\n```").findAll(comment.body).toList()
    if (matches.isEmpty()) return null
    ensure(matches.size == 1, "Ambiguous adjudication record")
    val value = Json.parse(matches.single().groupValues[1]).obj()
    value.fields(setOf("version", "sourceCommentId", "decision", "rule", "path", "commit", "firstLine", "lastLine", "severeSingleCase"))
    ensure(value.number("version") == 1L && value.text("decision") == "verified", "Invalid adjudication decision")
    val first = value.number("firstLine")
    val last = value.number("lastLine")
    ensure(first in 1..1_000_000 && last in first..(first + 39), "Invalid adjudication source range")
    ensure(value.number("sourceCommentId") > 0 && value.text("rule").isNotBlank(), "Incomplete adjudication")
    return Adjudication(value.number("sourceCommentId"), value.text("rule"),
        Location(value.text("path"), value.text("commit"), first.toInt(), last.toInt()), value.flag("severeSingleCase"))
}

internal class Extractor(private val repository: Repository, private val now: Instant) {
    private val policy = repository.policy
    val exclusions = mutableMapOf<String, Int>()

    private fun exclude(reason: String) {
        exclusions[reason] = (exclusions[reason] ?: 0) + 1
    }

    private fun location(comment: Comment): Location? {
        if (comment.path != null && comment.commit != null && comment.line != null) {
            return Location(comment.path, comment.commit, maxOf(1, comment.line - 3), comment.line)
        }
        val pattern = Regex(Regex.escape(policy.web) + "/blob/([0-9a-f]{40})/([^\\s#)]+)#L([0-9]+)(?:-L([0-9]+))?")
        val match = pattern.find(comment.body) ?: return null
        val first = match.groupValues[3].toIntOrNull() ?: return null
        val last = match.groupValues[4].takeIf { it.isNotBlank() }?.toIntOrNull() ?: first
        return Location(match.groupValues[2], match.groupValues[1], first, last)
    }

    private suspend fun evidence(comment: Comment, pull: Pull, comments: List<Comment>, paths: Set<String>): Evidence? {
        if (!policy.human(comment.actor) || comment.automated || comment.actor.id == pull.author.id) {
            exclude("not_independent_registered_human")
            return null
        }
        ensure(comment.body.length <= 16_000, "Comment exceeds the evidence limit")
        val judgment = adjudication(comment)
        var source: Comment? = null
        val rule: String
        val where: Location
        val severe: Boolean
        if (judgment != null) {
            source = comments.singleOrNull { it.id == judgment.sourceId && it.actor.id == pull.author.id }
            if (source == null || source.automated || source.actor.type != "User" || source.actor.id in policy.automationIds) {
                exclude("unverified_author_source")
                return null
            }
            ensure(comment.actor.id != source.actor.id, "An author cannot adjudicate their own correction")
            ensure(judgment.location.commit in comments.mapNotNull { it.commit } + pull.headSha,
                "Adjudicated source commit is not anchored to this PR")
            rule = judgment.rule
            where = judgment.location
            severe = judgment.severe
            ensure(!severe || comment.actor.id in policy.ownerIds, "Only an independent approved owner can authorize single-case capture")
        } else {
            rule = comment.body.trim()
            where = location(comment) ?: run { exclude("missing_immutable_code_citation"); return null }
            severe = false
            ensure(where.commit in comments.mapNotNull { it.commit } + pull.headSha,
                "General code citation is not anchored to this PR")
        }
        if (rule.isBlank()) { exclude("empty_comment"); return null }
        ensure(safePath(where.path) && sha(where.commit) && where.first >= 1 &&
            where.last in where.first..(where.first + 39), "Invalid immutable code citation")
        ensure(where.path in paths, "Citation is outside the source PR's changed paths")
        val fullCode = try {
            repository.source(where.path, where.commit) ?: run { exclude("inaccessible_source"); return null }
        } catch (_: SourceTooLarge) {
            exclude("source_exceeds_limit")
            return null
        }
        val lines = fullCode.lines()
        ensure(where.last <= lines.size, "Source line range does not exist")
        val code = lines.subList(where.first - 1, where.last).joinToString("\n")
        Safety.verify(rule + "\n" + code + "\n" + (source?.body ?: ""), policy)
        val citation = Citation(comment.id, comment.kind, pull.number, comment.actor.id, hash(comment.body), comment.url,
            where, hash(code), source?.id, source?.body?.let(::hash))
        val id = hash("${policy.host}/${policy.repository}/${comment.kind}/${comment.id}/${citation.actorId}/${citation.bodyHash}/${citation.codeHash}/${citation.authorSourceHash}")
        return Evidence(id, rule, citation, code, severe, comment.threadResolved)
    }

    suspend fun extract(state: State, stateSha: String?): Packet {
        ensure(state.repository == policy.repository && state.version == 1, "Checkpoint repository/version mismatch")
        val base = repository.baseSha()
        val open = repository.openLearningPulls()
        ensure(open.size <= 1, "Multiple active learning PRs require human reconciliation")
        var next = state.copy(decisions = state.decisions.filter {
            !Instant.parse(it.recordedAt).isBefore(now.minus(180, ChronoUnit.DAYS))
        })
        if (state.active != null) {
            val prior = repository.pull(state.active.number)
            ensure(prior.head == state.active.head && prior.base == policy.base, "Learning PR identity changed")
            if (!prior.open) {
                if (prior.merged) repository.requireHumanApproval(prior)
                next = next.copy(active = null, decisions = (next.decisions +
                    state.active.evidenceIds.map { Decision(it, if (prior.merged) "accepted" else "declined", now.toString()) })
                    .distinctBy { it.evidenceId })
            }
        }
        val pulls = (repository.recentPulls(now.minus(policy.lookbackDays.toLong(), ChronoUnit.DAYS)) +
            next.pendingPrs.map { repository.pull(it) }).distinctBy { it.number }
        ensure(pulls.size <= 500, "Pending evidence exceeds the pilot bound")
        val candidates = mutableListOf<Evidence>()
        val pending = mutableListOf<Long>()
        for (pull in pulls) {
            if (pull.author.id <= 0 || pull.base != policy.base || !pull.merged || pull.head.startsWith("copilot/review-learning") ||
                pull.updated.isBefore(now.minus(180, ChronoUnit.DAYS))) {
                exclude("ineligible_pr")
                continue
            }
            val paths = repository.changedPaths(pull.number)
            if (paths.any { it == ".github/copilot-instructions.md" || it.startsWith(".github/skills/") } ||
                paths.all { it.startsWith("generated/") || it.startsWith("versioning/") }) {
                exclude("guidance_or_rollout_pr")
                continue
            }
            val comments = repository.comments(pull.number)
            for (comment in comments) {
                val item = evidence(comment, pull, comments, paths) ?: continue
                if (next.decisions.any { it.evidenceId == item.id }) { exclude("processed_evidence"); continue }
                candidates.add(item)
            }
            if (candidates.any { it.citation.pr == pull.number }) pending.add(pull.number)
        }
        ensure(candidates.size <= 80, "Evidence batch exceeds the pilot bound")
        next = next.copy(pendingPrs = pending.distinct())
        val independent = candidates.map { it.citation.pr }.distinct().size >= 2 &&
            candidates.map { it.citation.location.commit }.distinct().size >= 2 &&
            candidates.map { it.citation.location.path to it.citation.codeHash }.distinct().size >= 2
        val signal = independent || candidates.any { it.severe }
        val unchanged = next.lastNoProposalFingerprint == hash(base + "/" + candidates.map { it.id }.sorted().joinToString("/"))
        val reason = when {
            open.isNotEmpty() -> "active_guidance_pr"
            !signal -> "no_qualifying_signal"
            unchanged -> "unchanged_evidence"
            else -> "qualified_human_evidence"
        }
        return Packet(repository = policy.repository, baseSha = base, stateSha = stateSha,
            hasSignal = signal && open.isEmpty() && !unchanged, reason = reason, evidence = candidates, nextState = next)
    }
}
