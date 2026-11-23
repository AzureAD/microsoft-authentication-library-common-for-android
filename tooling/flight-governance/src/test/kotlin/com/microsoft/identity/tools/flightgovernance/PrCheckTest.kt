// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.tools.flightgovernance

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

internal class PrCheckTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()
    private val sourcePath = "Flights.java"
    private val policyPath = "config/flight-governance.json"
    private val id = "app:example.Flights#FEATURE"

    @Test
    fun advisoryScan_readsCommittedSourceAndRefusesCheckoutWritesOrOverwrite() {
        val root = repository()
        writeSource(root, "FEATURE(\"Example\", false);")
        val base = commit(root)
        writeSource(root, "FEATURE(\"Example\", true);")
        val head = commit(root)
        Files.writeString(root.resolve(sourcePath), "uncommitted source must not be scanned")
        val before = git(root, "status", "--porcelain")
        val report = temporaryFolder.root.toPath().resolve("report.json")
        assertEquals(0, command(root, base, head, report))
        val result = JSONObject(Files.readString(report))
        assertEquals("complete", result.getString("status"))
        assertEquals("MISSING_METADATA", result.getJSONArray("findings").getJSONObject(0).getString("rule"))
        assertEquals(head, result.getString("sourceCommit"))
        assertTrue(Files.exists(report.resolveSibling("report.json.md")))
        val original = Files.readString(report)
        assertTrue(command(root, base, head, report) != 0)
        assertEquals(original, Files.readString(report))
        assertTrue(command(root, base, head, root.resolve("forbidden.json")) != 0)
        assertTrue(!Files.exists(root.resolve("forbidden.json")))
        assertEquals(before, git(root, "status", "--porcelain"))
    }

    @Test
    fun baselineIsReadFromTarget_notThePrAuthoredPolicy() {
        val root = repository()
        writeSource(root, "FEATURE(\"Example\", false);")
        val baseline = commit(root)
        writePolicy(root, baseline)
        val target = commit(root)
        writePolicy(root, "f".repeat(40))
        writeSource(root, metadata() + "\nFEATURE(\"Example\", false);")
        val head = commit(root)
        val result = comparePr(root, "app", target, head, listOf(sourcePath), policyPath)
        assertTrue(result.result.findings.any { it.rule == "MISSING_DOCUMENT" })
        assertTrue(result.result.legacyDocumentGaps.isEmpty())
        assertEquals(target, result.target)
        assertEquals(baseline, result.baseline)
    }

    @Test
    fun trustedBaseline_allowsOnlyInitialMetadataOnlyMigration() {
        val root = repository()
        writeSource(root, "FEATURE(\"Example\", false);")
        val baseline = commit(root)
        writePolicy(root, baseline)
        val target = commit(root)
        writeSource(root, metadata() + "\nFEATURE(\"Example\", false);")
        val migrated = commit(root)
        val result = comparePr(root, "app", target, migrated, listOf(sourcePath), policyPath).result
        assertTrue(result.findings.isEmpty())
        assertEquals(listOf(id), result.legacyDocumentGaps)
        Files.writeString(root.resolve("Runtime.kt"), "internal fun changedBehavior() = true\n")
        val mixed = commit(root)
        assertTrue(comparePr(root, "app", target, mixed, listOf(sourcePath), policyPath)
            .result.findings.any { it.rule == "MISSING_DOCUMENT" })
    }

    @Test
    fun staleBranch_cannotBackfillOverTargetMetadataOrDefaultChanges() {
        for (targetDeclaration in listOf(
            metadata() + "\nFEATURE(\"Example\", false);",
            "FEATURE(\"Example\", true);"
        )) {
            val root = repository()
            writeSource(root, "FEATURE(\"Example\", false);")
            val baseline = commit(root)
            writePolicy(root, baseline)
            val fork = commit(root)
            writeSource(root, targetDeclaration)
            val target = commit(root)
            git(root, "checkout", "--no-track", "-b", "stale-pr", fork)
            writeSource(root, metadata(owner = "new-team") + "\nFEATURE(\"Example\", false);")
            val head = commit(root)
            val result = comparePr(root, "app", target, head, listOf(sourcePath), policyPath).result
            assertEquals(listOf("MISSING_DOCUMENT"), result.findings.map { it.rule })
            assertTrue(result.legacyDocumentGaps.isEmpty())
        }
    }

    @Test
    fun baselineCannotGrandfatherAKeyAddedAfterCutover() {
        val root = repository()
        writeSource(root, "OTHER(\"Other\", false);")
        val baseline = commit(root)
        writeSource(root, "OTHER(\"Other\", false), FEATURE(\"Example\", false);")
        writePolicy(root, baseline)
        val target = commit(root)
        writeSource(root, "OTHER(\"Other\", false), ${metadata()}\nFEATURE(\"Example\", false);")
        val head = commit(root)
        val result = comparePr(root, "app", target, head, listOf(sourcePath), policyPath).result
        assertTrue(result.findings.any { it.rule == "MISSING_DOCUMENT" })
        assertTrue(result.legacyDocumentGaps.isEmpty())
    }

    @Test
    fun documentDeletionAndExplicitOwnership_areEvaluatedFromCommits() {
        val root = repository()
        writeSource(root, metadata(owner = "", document = "docs/example.md") + "\nFEATURE(\"Example\", false);")
        Files.createDirectories(root.resolve("docs"))
        Files.writeString(root.resolve("docs/example.md"), "Owner-reviewed premortem.\n")
        val target = commit(root)
        Files.delete(root.resolve("docs/example.md"))
        val head = commit(root)
        assertEquals(listOf("BROKEN_DOCUMENT"),
            comparePr(root, "app", target, head, listOf(sourcePath), policyPath).result.findings.map { it.rule })
        Files.writeString(root.resolve("docs/example.md"), "Restored.\n")
        writeSource(root, metadata(owner = "", document = "docs/example.md") + "\nFEATURE(\"Example\", true);")
        val changed = commit(root)
        assertEquals(listOf("MISSING_OWNER"),
            comparePr(root, "app", target, changed, listOf(sourcePath), policyPath).result.findings.map { it.rule })
        writeSource(root, metadata(owner = "team", document = "docs/example.md") + "\nFEATURE(\"Example\", true);")
        val owned = commit(root)
        assertTrue(comparePr(root, "app", target, owned, listOf(sourcePath), policyPath).result.findings.isEmpty())
    }

    @Test
    fun missingRevision_producesAnExplicitIncompleteReport() {
        val root = repository()
        writeSource(root, "FEATURE(\"Example\", false);")
        val head = commit(root)
        val report = temporaryFolder.root.toPath().resolve("failed.json")
        assertTrue(command(root, "missing-target", head, report) != 0)
        assertEquals("incomplete", JSONObject(Files.readString(report)).getString("status"))
    }

    @Test
    fun shallowHistory_producesAnExplicitIncompleteReport() {
        val root = repository()
        writeSource(root, "FEATURE(\"Example\", false);")
        commit(root)
        writeSource(root, "FEATURE(\"Example\", true);")
        val head = commit(root)
        val shallow = temporaryFolder.newFolder().toPath()
        git(temporaryFolder.root.toPath(), "clone", "--quiet", "--depth", "1",
            root.toUri().toString(), shallow.toString())
        val report = temporaryFolder.root.toPath().resolve("shallow.json")
        assertTrue(command(shallow, head, head, report) != 0)
        val result = JSONObject(Files.readString(report))
        assertEquals("incomplete", result.getString("status"))
        assertTrue(result.getString("error").contains("Full Git history"))
    }

    @Test
    fun malformedSource_producesAnExplicitIncompleteReport() {
        val root = repository()
        writeSource(root, "FEATURE(\"Example\", false);")
        val base = commit(root)
        writeSource(root, "FEATURE(\"Example\", false;")
        val head = commit(root)
        val report = temporaryFolder.root.toPath().resolve("parse-failure.json")
        assertTrue(command(root, base, head, report) != 0)
        assertEquals("incomplete", JSONObject(Files.readString(report)).getString("status"))
    }

    @Test
    fun targetChanges_doNotContaminateThePrMergeBase() {
        val root = repository()
        writeSource(root, "FEATURE(\"Example\", false);")
        val base = commit(root)
        writeSource(root, "FEATURE(\"Example\", true);")
        val head = commit(root)
        git(root, "checkout", "--no-track", "-b", "target", base)
        Files.writeString(root.resolve("target-only.md"), "Unrelated target change.\n")
        val target = commit(root)
        val result = comparePr(root, "app", target, head, listOf(sourcePath), policyPath)
        assertEquals(base, result.mergeBase)
        assertEquals(head, result.head)
        assertEquals(listOf("MISSING_METADATA"), result.result.findings.map { it.rule })
    }

    @Test
    fun branchOlderThanBaseline_completesWithTargetBaseline() {
        val root = repository()
        writeSource(root, "FEATURE(\"Example\", false);")
        val fork = commit(root)
        Files.writeString(root.resolve("baseline.md"), "Baseline introduced after the source branch.\n")
        val baseline = commit(root)
        writePolicy(root, baseline)
        val target = commit(root)
        git(root, "checkout", "--no-track", "-b", "older-pr", fork)
        writeSource(root, "FEATURE(\"Example\", true);")
        val head = commit(root)
        val report = temporaryFolder.root.toPath().resolve("older-branch.json")
        assertEquals(0, command(root, target, head, report))
        val result = JSONObject(Files.readString(report))
        assertEquals("complete", result.getString("status"))
        assertEquals(fork, result.getString("mergeBase"))
        assertEquals(baseline, result.getString("legacyBaseline"))
        assertEquals("MISSING_METADATA", result.getJSONArray("findings").getJSONObject(0).getString("rule"))
    }

    @Test
    fun olderBranch_cannotGrandfatherAFlightAbsentAtItsMergeBase() {
        val root = repository()
        writeSource(root, "OTHER(\"Other\", false);")
        val fork = commit(root)
        writeSource(root, "OTHER(\"Other\", false), FEATURE(\"Example\", false);")
        val baseline = commit(root)
        writePolicy(root, baseline)
        val target = commit(root)
        git(root, "checkout", "--no-track", "-b", "older-pr", fork)
        writeSource(root, "OTHER(\"Other\", false), ${metadata()}\nFEATURE(\"Example\", false);")
        val head = commit(root)
        val comparison = comparePr(root, "app", target, head, listOf(sourcePath), policyPath)
        assertEquals(fork, comparison.mergeBase)
        assertEquals(listOf("MISSING_DOCUMENT"), comparison.result.findings.map { it.rule })
        assertTrue(comparison.result.legacyDocumentGaps.isEmpty())
    }

    @Test
    fun summaryWriteFailure_cannotLeaveACompleteJsonReport() {
        val root = repository()
        writeSource(root, "FEATURE(\"Example\", false);")
        val head = commit(root)
        // JSON's 253-character filename is valid; the appended Markdown name exceeds 255.
        val report = temporaryFolder.root.toPath().resolve("r".repeat(248) + ".json")
        Files.writeString(report, "Verify that the JSON destination itself is writable.")
        Files.delete(report)
        assertTrue(command(root, head, head, report) != 0)
        assertEquals("incomplete", JSONObject(Files.readString(report)).getString("status"))
    }

    @Test
    fun divergentBranch_validatesDocumentLinksAgainstProspectiveMergedTree() {
        val root = repository()
        Files.createDirectories(root.resolve("docs"))
        Files.writeString(root.resolve("docs/deleted-on-target.md"), "Will be deleted on target.\n")
        Files.writeString(root.resolve("docs/retired-on-target.md"), "Will be replaced on target.\n")
        writeSource(root, metadata(document = "docs/retired-on-target.md") + "\nRETIRED(\"Retired\", false);")
        val fork = commit(root)
        Files.delete(root.resolve("docs/deleted-on-target.md"))
        Files.delete(root.resolve("docs/retired-on-target.md"))
        Files.writeString(root.resolve("docs/replacement-on-target.md"), "Target replacement.\n")
        writeSource(root, metadata(document = "docs/replacement-on-target.md") + "\nRETIRED(\"Retired\", false);")
        val target = commit(root)
        git(root, "checkout", "--no-track", "-b", "divergent-pr", fork)
        Files.writeString(root.resolve("docs/new-by-pr.md"), "Supplied by PR.\n")
        writeSource(root, """
            ${metadata(document = "docs/retired-on-target.md")}
            RETIRED("Retired", false),
            ${metadata(document = "docs/deleted-on-target.md")}
            STALE_LINK("StaleLink", false),
            ${metadata(document = "docs/new-by-pr.md")}
            NEW_WITH_PR_DOC("NewWithPrDoc", false);
        """.trimIndent())
        val head = commit(root)
        val comparison = comparePr(root, "app", target, head, listOf(sourcePath), policyPath)
        assertEquals(listOf("app:example.Flights#STALE_LINK"),
            comparison.result.findings.map { it.declaration.id })
        assertEquals(listOf("MISSING_DOCUMENT"), comparison.result.findings.map { it.rule })
    }

    @Test
    fun movedRegistry_marksScanIncompleteInsteadOfSilentlyDroppingNewFlights() {
        val root = repository()
        writeSource(root, "EXISTING(\"Existing\", false);")
        val target = commit(root)
        Files.createDirectories(root.resolve("moved"))
        Files.delete(root.resolve(sourcePath))
        Files.writeString(root.resolve("moved/$sourcePath"), """
            // Copyright (c) Microsoft Corporation. All rights reserved.
            package example;
            enum Flights implements IFlightConfig {
                EXISTING("Existing", false),
                ADDED_WITHOUT_METADATA("AddedWithoutMetadata", false);
                Flights(String key, Object value) {}
            }
        """.trimIndent())
        val head = commit(root)
        val report = temporaryFolder.root.toPath().resolve("moved-registry.json")
        assertTrue(command(root, target, head, report) != 0)
        assertEquals("incomplete", JSONObject(Files.readString(report)).getString("status"))
    }

    private fun repository(): Path = temporaryFolder.newFolder().toPath().also { git(it, "init", "-q") }

    private fun writeSource(root: Path, constants: String) {
        Files.writeString(root.resolve(sourcePath), """
            // Copyright (c) Microsoft Corporation. All rights reserved.
            package example;
            import com.microsoft.identity.common.java.flighting.FlightMeta;
            import com.microsoft.identity.common.java.flighting.FlagType;
            enum Flights implements IFlightConfig {
                $constants
                Flights(String key, Object value) {}
            }
        """.trimIndent())
    }

    private fun metadata(owner: String = "team", document: String = "") =
        """@FlightMeta(owner = "$owner", type = FlagType.RELEASE, premortem = "$document")"""

    private fun writePolicy(root: Path, baseline: String) {
        Files.createDirectories(root.resolve("config"))
        Files.writeString(root.resolve(policyPath), JSONObject().put("schemaVersion", 1)
            .put("legacyRevision", baseline).toString())
    }

    private fun commit(root: Path): String {
        git(root, "add", ".")
        git(root, "-c", "user.name=Fixture", "-c", "user.email=fixture@example.invalid",
            "-c", "commit.gpgsign=false", "commit", "-q", "-m",
            "Fixture change\n\nCo-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>")
        return git(root, "rev-parse", "HEAD").trim()
    }

    private fun command(root: Path, base: String, head: String, output: Path): Int {
        val messages = ByteArrayOutputStream()
        return PrintStream(messages, true, StandardCharsets.UTF_8).use {
            runPrCheck(listOf("--repository", "app", root.toString(), "--base", base, "--head", head,
                "--source", sourcePath, "--policy", policyPath, "--output", output.toString()), it, it)
        }
    }

    private fun git(root: Path, vararg arguments: String): String {
        val process = ProcessBuilder(listOf("git", "-c", "core.hooksPath=" + temporaryFolder.root.path,
            "-C", root.toString()) + arguments).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        check(process.waitFor() == 0) { output }
        return output
    }
}
