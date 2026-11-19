// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.tools.flightgovernance

internal data class PolicyMetadata(
    val owner: String = "",
    val type: String = "",
    val premortem: String = "",
    val invalidArguments: Set<String> = emptySet()
)

internal data class PolicyDeclaration(
    val id: String,
    val path: String,
    val line: Long,
    val key: String,
    val defaultExpression: String,
    val metadata: PolicyMetadata?
) {
    fun sameBehavior(other: PolicyDeclaration): Boolean =
        key == other.key && defaultExpression == other.defaultExpression
}

internal data class MigrationApproval(
    val id: String,
    val metadata: PolicyMetadata,
    val reviewedBy: String,
    val review: String
)

internal data class PolicyInputs(
    val before: List<PolicyDeclaration>,
    val after: List<PolicyDeclaration>,
    val beforeFiles: Set<String>,
    val afterFiles: Set<String>,
    val gitOwnerCandidates: Set<String> = emptySet(),
    val legacy: List<PolicyDeclaration> = emptyList(),
    val approvals: List<MigrationApproval> = emptyList(),
    val metadataOnly: Boolean = false
)

internal data class PolicyFinding(
    val rule: String,
    val declaration: PolicyDeclaration,
    val message: String,
    val fix: String
)

internal data class PolicyResult(
    val findings: List<PolicyFinding>,
    val checked: List<String>,
    val legacyDocumentGaps: List<String>
)

/** Evaluates normalized declarations; language extraction and Git access belong to adapters. */
internal object FlightPolicy {
    fun evaluate(input: PolicyInputs): PolicyResult {
        require(input.before.map { it.id }.distinct().size == input.before.size) { "Duplicate old declaration" }
        require(input.after.map { it.id }.distinct().size == input.after.size) { "Duplicate new declaration" }
        val before = input.before.associateBy { it.id }
        val legacy = input.legacy.associateBy { it.id }
        val findings = mutableListOf<PolicyFinding>()
        val checked = mutableListOf<String>()
        val gaps = mutableListOf<String>()
        for (flight in input.after.sortedBy { it.id }) {
            val old = before[flight.id]
            val metadata = flight.metadata
            fun finding(rule: String, message: String, fix: String) {
                findings.add(PolicyFinding(rule, flight, message, fix))
            }
            val brokenLink = old?.metadata?.premortem?.takeIf { it.isNotBlank() }?.let {
                it == metadata?.premortem && it in input.beforeFiles && it !in input.afterFiles
            } == true
            if (brokenLink) {
                finding("BROKEN_PREMORTEM", "Previously linked premortem was removed or renamed.",
                    "Restore the document or update the flight's premortem link.")
            }
            if (old != null && old.sameBehavior(flight) && old.metadata == metadata) {
                continue
            }
            checked.add(flight.id)
            if (metadata == null) {
                finding("MISSING_METADATA", "Added or modified flight has no governance metadata.",
                    "Add owner/type metadata and a premortem for a Release flight.")
                continue
            }
            if (metadata.invalidArguments.isNotEmpty()) {
                finding("UNRESOLVED_METADATA", "Unsupported metadata arguments: ${metadata.invalidArguments.sorted()}.",
                    "Use literal owner/document values and a supported lifecycle type.")
            }
            if (metadata.type !in setOf("RELEASE", "KILL_SWITCH", "CONFIG")) {
                finding("MISSING_TYPE", "A supported lifecycle type is required.",
                    "Classify the flight as RELEASE, KILL_SWITCH or CONFIG.")
            }
            if (metadata.owner.isBlank() && flight.id !in input.gitOwnerCandidates) {
                finding("MISSING_OWNER", "Neither an explicit owner nor a Git ownership candidate could be resolved.",
                    "Record an accountable owner/team; Git authorship is not current-owner confirmation.")
            }
            val requiresDocument = metadata.type == "RELEASE" || old?.metadata?.type == "RELEASE"
            val approvedMigration = old != null && old.metadata == null && old.sameBehavior(flight) &&
                legacy[flight.id]?.sameBehavior(flight) == true && input.metadataOnly &&
                metadata.owner.isNotBlank() && metadata.invalidArguments.isEmpty() &&
                input.approvals.any {
                    it.id == flight.id && it.metadata == metadata &&
                        it.reviewedBy.isNotBlank() && it.review.isNotBlank()
                }
            if (requiresDocument && !brokenLink) {
                if (metadata.premortem.isBlank() && approvedMigration) {
                    gaps.add(flight.id)
                } else if (!repositoryPath(metadata.premortem) ||
                    !metadata.premortem.endsWith(".md", ignoreCase = true) ||
                    metadata.premortem !in input.afterFiles
                ) {
                    finding("MISSING_PREMORTEM", "Release flight requires an existing same-repository Markdown premortem.",
                        "Add and reference a current premortem, or obtain a separate trusted legacy migration approval.")
                }
            }
        }
        return PolicyResult(findings, checked, gaps)
    }
}

internal fun repositoryPath(path: String): Boolean =
    path.isNotBlank() && !path.startsWith('/') && ':' !in path && '\\' !in path &&
        path.split('/').none { it in setOf("", ".", "..") }
