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
    fun newDeclarations_requireMetadataAndDocumentsForEveryTypeAndDefault() {
        for (default in listOf("true", "false")) {
            val added = flight.copy(defaultExpression = default)
            assertEquals(listOf("MISSING_METADATA"), rules(check(after = listOf(added))))
            for (type in listOf("RELEASE", "KILL_SWITCH", "CONFIG")) {
                val annotated = added.copy(metadata = release.copy(type = type))
                assertEquals(listOf("MISSING_DOCUMENT"),
                    rules(check(after = listOf(annotated), documents = emptySet())))
                assertTrue(check(after = listOf(annotated)).findings.isEmpty())
            }
        }
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
    fun explicitOwner_isRequiredForEveryTypeAndDoesNotMaskUnresolvedMetadata() {
        for (type in listOf("RELEASE", "KILL_SWITCH", "CONFIG")) {
            val noOwner = flight.copy(metadata = release.copy(owner = "", type = type))
            assertEquals(listOf("MISSING_OWNER"), rules(check(after = listOf(noOwner))))
        }
        assertEquals(listOf("UNRESOLVED_METADATA"), rules(check(after = listOf(
            flight.copy(metadata = release.copy(invalidArguments = setOf("owner")))
        ))))
    }

    @Test
    fun initialLegacyBackfill_preservesHistoricalGapOnlyForMetadataOnlyChange() {
        for (type in listOf("RELEASE", "KILL_SWITCH", "CONFIG")) {
            val metadata = release.copy(type = type, premortem = "")
            val migrated = flight.copy(metadata = metadata)
            val input = PolicyInputs(listOf(flight), listOf(migrated), emptySet(), emptySet(),
                legacy = listOf(flight), metadataOnly = true)
            val result = FlightPolicy.evaluate(input)
            assertTrue(result.findings.isEmpty())
            assertEquals(listOf(flight.id), result.legacyDocumentGaps)
            val denied = listOf(
                input.copy(metadataOnly = false),
                input.copy(legacy = emptyList()),
                input.copy(legacy = listOf(flight.copy(defaultExpression = "true"))),
                input.copy(legacy = listOf(flight.copy(metadata = release))),
                input.copy(target = listOf(flight.copy(metadata = metadata))),
                input.copy(target = listOf(flight.copy(defaultExpression = "true"))),
                input.copy(before = emptyList()),
                input.copy(after = listOf(migrated.copy(key = "Renamed"))),
                input.copy(after = listOf(migrated.copy(defaultExpression = "true"))),
                input.copy(after = listOf(migrated.copy(metadata = metadata.copy(owner = "")))),
                input.copy(after = listOf(migrated.copy(metadata = metadata.copy(type = "UNKNOWN")))),
                input.copy(after = listOf(migrated.copy(
                    metadata = metadata.copy(invalidArguments = setOf("owner"))
                ))),
                input.copy(before = listOf(flight.copy(metadata = PolicyMetadata("team", "CONFIG"))))
            )
            for (candidate in denied) {
                val deniedResult = FlightPolicy.evaluate(candidate)
                assertTrue(deniedResult.findings.any { it.rule == "MISSING_DOCUMENT" })
                assertTrue(deniedResult.legacyDocumentGaps.isEmpty())
            }
        }
    }

    @Test
    fun reclassification_doesNotEvadeDocumentation() {
        val result = check(before = listOf(flight.copy(metadata = release)),
            after = listOf(flight.copy(metadata = PolicyMetadata("team", "CONFIG"))),
            documents = emptySet())
        assertEquals(listOf("MISSING_DOCUMENT"), rules(result))
    }

    @Test
    fun documentDeletion_isCheckedEvenWithoutAnEnumChangeAndRenameRequiresUpdatedLink() {
        val annotated = flight.copy(metadata = release)
        val deleted = FlightPolicy.evaluate(PolicyInputs(
            listOf(annotated), listOf(annotated), setOf(document), emptySet()
        ))
        assertEquals(listOf("BROKEN_DOCUMENT"), rules(deleted))
        assertTrue(deleted.checked.isEmpty())
        val renamed = "docs/premortem/renamed.md"
        assertTrue(check(before = listOf(annotated),
            after = listOf(annotated.copy(metadata = release.copy(premortem = renamed))),
            documents = setOf(renamed)).findings.isEmpty())
    }

    @Test
    fun documentMustBeAnExistingRepositoryRelativeMarkdownFile() {
        for (path in listOf("../outside.md", "/outside.md", "C:/outside.md", "docs\\file.md", document)) {
            assertEquals(listOf("MISSING_DOCUMENT"), rules(check(
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
        documents: Set<String> = setOf(document)
    ) = FlightPolicy.evaluate(PolicyInputs(before, after, emptySet(), documents))

    private fun rules(result: PolicyResult) = result.findings.map { it.rule }
}
