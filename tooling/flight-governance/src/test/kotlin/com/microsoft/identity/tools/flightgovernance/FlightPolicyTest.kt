// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.tools.flightgovernance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class FlightPolicyTest {
    private val document = "docs/premortem/example.md"
    private val release = PolicyMetadata("owning-team", "RELEASE", document)
    private val flight = PolicyDeclaration("app:example.Flights#FEATURE", "Flights.java", 3,
        "ExampleFeature", "false", null)

    @Test
    fun newDeclarations_requireMetadataAndReleaseDocumentsRegardlessOfDefault() {
        for (default in listOf("true", "false")) {
            val added = flight.copy(defaultExpression = default)
            assertEquals(listOf("MISSING_METADATA"), rules(check(after = listOf(added))))
            assertEquals(listOf("MISSING_PREMORTEM"),
                rules(check(after = listOf(added.copy(metadata = release)), documents = emptySet())))
            assertTrue(check(after = listOf(added.copy(metadata = release))).findings.isEmpty())
        }
        assertTrue(check(after = listOf(flight.copy(metadata = PolicyMetadata("team", "CONFIG"))))
            .findings.isEmpty())
    }

    @Test
    fun unchangedLegacy_isGrandfatheredButChangedDefaultAndRemovedMetadataAreNot() {
        assertTrue(check(before = listOf(flight), after = listOf(flight.copy(line = 20))).checked.isEmpty())
        assertEquals(listOf("MISSING_METADATA"),
            rules(check(before = listOf(flight), after = listOf(flight.copy(defaultExpression = "true")))))
        assertEquals(listOf("MISSING_METADATA"),
            rules(check(before = listOf(flight.copy(metadata = release)), after = listOf(flight))))
    }

    @Test
    fun ownerFallback_isExplicitAndDoesNotMaskUnresolvedMetadata() {
        val noOwner = flight.copy(metadata = release.copy(owner = ""))
        assertEquals(listOf("MISSING_OWNER"), rules(check(after = listOf(noOwner))))
        assertTrue(check(after = listOf(noOwner), candidates = setOf(flight.id)).findings.isEmpty())
        assertEquals(listOf("UNRESOLVED_METADATA"), rules(check(after = listOf(
            flight.copy(metadata = release.copy(invalidArguments = setOf("owner")))
        ))))
    }

    @Test
    fun approvedInitialMigration_preservesHistoricalGapOnlyForExactMetadataOnlyChange() {
        val migrated = flight.copy(metadata = release.copy(premortem = ""))
        val approval = MigrationApproval(flight.id, requireNotNull(migrated.metadata), "reviewer", "review-123")
        val input = PolicyInputs(listOf(flight), listOf(migrated), emptySet(), emptySet(),
            legacy = listOf(flight), approvals = listOf(approval), metadataOnly = true)
        val approved = FlightPolicy.evaluate(input)
        assertTrue(approved.findings.isEmpty())
        assertEquals(listOf(flight.id), approved.legacyDocumentGaps)
        val denied = listOf(
            input.copy(approvals = emptyList()),
            input.copy(metadataOnly = false),
            input.copy(legacy = emptyList()),
            input.copy(after = listOf(migrated.copy(key = "Renamed"))),
            input.copy(after = listOf(migrated.copy(defaultExpression = "true"))),
            input.copy(after = listOf(migrated.copy(metadata = migrated.metadata?.copy(owner = "different-team")))),
            input.copy(before = listOf(flight.copy(metadata = PolicyMetadata("team", "CONFIG"))))
        )
        for (candidate in denied) {
            val result = FlightPolicy.evaluate(candidate)
            assertTrue(result.findings.any { it.rule == "MISSING_PREMORTEM" })
            assertTrue(result.legacyDocumentGaps.isEmpty())
        }
    }

    @Test
    fun reclassification_doesNotEvadeReleaseDocumentation() {
        val result = check(before = listOf(flight.copy(metadata = release)),
            after = listOf(flight.copy(metadata = PolicyMetadata("team", "CONFIG"))),
            documents = emptySet())
        assertEquals(listOf("MISSING_PREMORTEM"), rules(result))
    }

    @Test
    fun documentDeletion_isCheckedEvenWithoutAnEnumChangeAndRenameRequiresUpdatedLink() {
        val annotated = flight.copy(metadata = release)
        val deleted = FlightPolicy.evaluate(PolicyInputs(
            listOf(annotated), listOf(annotated), setOf(document), emptySet()
        ))
        assertEquals(listOf("BROKEN_PREMORTEM"), rules(deleted))
        assertTrue(deleted.checked.isEmpty())
        val renamed = "docs/premortem/renamed.md"
        assertTrue(check(before = listOf(annotated),
            after = listOf(annotated.copy(metadata = release.copy(premortem = renamed))),
            documents = setOf(renamed)).findings.isEmpty())
    }

    @Test
    fun documentMustBeAnExistingRepositoryRelativeMarkdownFile() {
        for (path in listOf("../outside.md", "/outside.md", "C:/outside.md", "docs\\file.md", document)) {
            assertEquals(listOf("MISSING_PREMORTEM"), rules(check(
                after = listOf(flight.copy(metadata = release.copy(premortem = path))),
                documents = emptySet()
            )))
        }
        assertEquals(listOf("MISSING_TYPE"),
            rules(check(after = listOf(flight.copy(metadata = release.copy(type = "UNKNOWN"))))))
    }

    private fun check(
        before: List<PolicyDeclaration> = emptyList(),
        after: List<PolicyDeclaration>,
        documents: Set<String> = setOf(document),
        candidates: Set<String> = emptySet()
    ) = FlightPolicy.evaluate(PolicyInputs(before, after, emptySet(), documents, candidates))

    private fun rules(result: PolicyResult) = result.findings.map { it.rule }
}
