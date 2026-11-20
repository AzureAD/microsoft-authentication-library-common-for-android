// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.tools.flightgovernance

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption

private const val CHECK_USAGE = """Usage: flight-governance check-pr
  --repository <label> <directory> --base <target-revision> --head <source-revision>
  --source <relative-java-file> [--source ...]
  --policy <target-tracked-policy-json>
  --output <new-report-json-outside-input-repository>
Policy findings are advisory. Incomplete execution returns nonzero.
"""

internal data class PrComparison(
    val target: String,
    val head: String,
    val mergeBase: String,
    val policyPresent: Boolean,
    val baseline: String?,
    val result: PolicyResult
)

internal fun runPrCheck(args: List<String>, out: PrintStream, error: PrintStream): Int {
    if (args == listOf("--help")) {
        out.print(CHECK_USAGE)
        return 0
    }
    var output: Path? = null
    var outputValidated = false
    fun failed(exception: Exception): Int {
        val message = exception.message ?: exception.javaClass.simpleName
        error.println("Flight governance scan incomplete: $message")
        val report = JSONObject().put("schemaVersion", 1).put("mode", "report-only")
            .put("status", "incomplete").put("error", message)
        output?.takeIf { outputValidated }?.let {
            try {
                Files.writeString(it, canonicalJson(report) + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
            } catch (writeFailure: IOException) {
                error.println("Cannot write incomplete-scan report: ${writeFailure.message}")
            }
        }
        return 1
    }
    return try {
        var root: Path? = null
        var label: String? = null
        var base: String? = null
        var head: String? = null
        var policy: String? = null
        val sources = mutableListOf<String>()
        var index = 0
        fun argument(option: String): String {
            require(index < args.size) { "$option requires a value" }
            return args[index++]
        }
        while (index < args.size) {
            when (val option = args[index++]) {
                "--repository" -> {
                    require(root == null) { "Specify --repository once" }
                    label = argument(option)
                    require(label.matches(Regex("[A-Za-z0-9._-]+"))) { "Invalid repository label" }
                    root = Path.of(argument(option)).toRealPath()
                }
                "--base" -> {
                    require(base == null) { "Specify --base once" }
                    base = argument(option)
                }
                "--head" -> {
                    require(head == null) { "Specify --head once" }
                    head = argument(option)
                }
                "--source" -> sources.add(argument(option))
                "--policy" -> {
                    require(policy == null) { "Specify --policy once" }
                    policy = argument(option)
                }
                "--output" -> {
                    require(output == null) { "Specify --output once" }
                    val requested = Path.of(argument(option)).toAbsolutePath().normalize()
                    output = requested.parent.toRealPath().resolve(requested.fileName)
                }
                else -> throw IllegalArgumentException("Unknown option: $option")
            }
        }
        val directory = requireNotNull(root) { "--repository is required" }
        val repositoryRoot = Path.of(git(directory, "rev-parse", "--show-toplevel").trim()).toRealPath()
        val destination = requireNotNull(output) { "--output is required" }
        require(!destination.startsWith(repositoryRoot)) { "Reports must be outside the input repository" }
        require(!Files.exists(destination, LinkOption.NOFOLLOW_LINKS) &&
            !Files.exists(destination.resolveSibling("${destination.fileName}.md"), LinkOption.NOFOLLOW_LINKS)
        ) {
            "Report destinations already exist; refusing to overwrite them"
        }
        outputValidated = true
        val comparison = comparePr(
            repositoryRoot, requireNotNull(label),
            requireNotNull(base) { "--base is required" },
            requireNotNull(head) { "--head is required" }, sources,
            requireNotNull(policy) { "--policy is required" }
        )
        val result = comparison.result
        val report = JSONObject().put("schemaVersion", 1).put("mode", "report-only")
            .put("status", "complete").put("repository", label)
            .put("targetCommit", comparison.target).put("sourceCommit", comparison.head)
            .put("mergeBase", comparison.mergeBase).put("policyCommit", comparison.target)
            .put("policyPresent", comparison.policyPresent)
            .put("legacyBaseline", comparison.baseline ?: JSONObject.NULL)
            .put("checkedDeclarations", JSONArray(result.checked))
            .put("legacyDocumentGaps", JSONArray(result.legacyDocumentGaps))
            .put("findings", JSONArray(result.findings.map {
                JSONObject().put("rule", it.rule).put("id", it.declaration.id)
                    .put("key", it.declaration.key).put("path", it.declaration.path)
                    .put("line", it.declaration.line).put("message", it.message).put("fix", it.fix)
            }))
        val summary = buildString {
            append("# Flight governance: report-only\n\n")
            append("Scan complete; ${result.findings.size} advisory findings; ")
            append("${result.checked.size} added/modified declarations checked.\n\n")
            append("Target: `${comparison.target}`; source: `${comparison.head}`; ")
            append("merge base: `${comparison.mergeBase}`.\n\n")
            if (!comparison.policyPresent) {
                append("No target-trusted migration policy exists; no legacy exceptions are available.\n\n")
            }
            for (finding in result.findings) {
                append("- **${finding.rule}** `${finding.declaration.id}` ")
                append("(${finding.declaration.path}:${finding.declaration.line}): ")
                append("${finding.message} ${finding.fix}\n")
            }
            for (id in result.legacyDocumentGaps) {
                append("- **Approved legacy document gap:** `$id`.\n")
            }
        }
        writePrReport(destination, report, summary)
        out.println("Flight governance scan complete: ${result.findings.size} advisory findings.")
        out.println("Report: $destination")
        0
    } catch (exception: IllegalArgumentException) {
        failed(exception)
    } catch (exception: IllegalStateException) {
        failed(exception)
    } catch (exception: IOException) {
        failed(exception)
    } catch (exception: JSONException) {
        failed(exception)
    }
}

internal fun comparePr(
    root: Path,
    label: String,
    baseRevision: String,
    headRevision: String,
    sourcePaths: List<String>,
    policyPath: String
): PrComparison {
    require(sourcePaths.isNotEmpty() && sourcePaths.distinct().size == sourcePaths.size &&
        sourcePaths.all { repositoryPath(it) && it.endsWith(".java") }
    ) { "Supply unique repository-relative Java source paths using / separators" }
    require(repositoryPath(policyPath)) { "Invalid migration-policy path" }
    require(git(root, "rev-parse", "--is-shallow-repository").trim() == "false") {
        "Full Git history is required for merge-base and trusted-baseline checks; fetch the missing history"
    }
    fun commit(revision: String): String =
        git(root, "rev-parse", "--verify", "--end-of-options", "$revision^{commit}").trim().also {
            require(it.matches(Regex("[0-9a-f]{40,64}"))) { "Invalid resolved revision" }
        }
    val target = commit(baseRevision)
    val head = commit(headRevision)
    val mergeBases = git(root, "merge-base", "--all", target, head).lineSequence()
        .filter { it.isNotBlank() }.toList()
    require(mergeBases.size == 1) { "Exactly one PR-target merge base is required" }
    val mergeBase = mergeBases.single()
    fun files(revision: String): Set<String> =
        git(root, "ls-tree", "-r", "-z", "--full-tree", revision).split('\u0000')
            .filter { it.isNotEmpty() && it.substringBefore(' ') in setOf("100644", "100755") }
            .map { it.substringAfter('\t') }.toSet()
    val beforeFiles = files(mergeBase)
    val afterFiles = files(head)
    val targetFiles = files(target)
    val adapter = JavaPolicyAdapter()
    fun parse(revision: String, paths: Set<String>): Map<String, PolicySource> =
        sourcePaths.filter { it in paths }.associateWith {
            adapter.parse(FlightSource(label, revision, it, git(root, "show", "$revision:$it")))
        }
    val before = parse(mergeBase, beforeFiles)
    val after = parse(head, afterFiles)
    require(before.isNotEmpty() || after.isNotEmpty()) { "None of the configured flight sources exist" }
    val changedPaths = git(root, "diff", "--no-ext-diff", "--no-renames", "--name-only", "-z", mergeBase, head)
        .split('\u0000').filter { it.isNotEmpty() }
    val metadataOnly = before.keys == after.keys &&
        before.all { (path, old) -> old.runtimeShape == after.getValue(path).runtimeShape } &&
        changedPaths.all { it in sourcePaths || (it.startsWith("docs/") && it.endsWith(".md")) }
    var baseline: String? = null
    var legacy = emptyList<PolicyDeclaration>()
    var approvals = emptyList<MigrationApproval>()
    val policyPresent = policyPath in targetFiles
    if (policyPresent) {
        val policy = JSONObject(git(root, "show", "$target:$policyPath"))
        require(policy.getInt("schemaVersion") == 1) { "Unsupported migration-policy schema" }
        val revision = policy.getString("legacyRevision")
        require(revision.matches(Regex("[0-9a-f]{40,64}"))) { "Legacy baseline must be an immutable commit ID" }
        baseline = commit(revision)
        git(root, "merge-base", "--is-ancestor", baseline, target)
        legacy = parse(baseline, files(baseline)).values.flatMap { it.declarations }
        val entries = policy.getJSONArray("migrationApprovals")
        approvals = (0 until entries.length()).map {
            val entry = entries.getJSONObject(it)
            MigrationApproval(entry.getString("id"), PolicyMetadata(
                entry.getString("owner"), entry.getString("type"), entry.getString("premortem")
            ), entry.getString("reviewedBy"), entry.getString("review"))
        }
        require(approvals.map { it.id }.distinct().size == approvals.size) { "Duplicate migration approval" }
    }
    val oldDeclarations = before.values.flatMap { it.declarations }
    val newDeclarations = after.values.flatMap { it.declarations }
    return PrComparison(target, head, mergeBase, policyPresent, baseline, FlightPolicy.evaluate(
        PolicyInputs(oldDeclarations, newDeclarations, beforeFiles, afterFiles, legacy, approvals, metadataOnly)
    ))
}

private fun writePrReport(output: Path, report: JSONObject, summary: String) {
    Files.writeString(output.resolveSibling("${output.fileName}.md"), summary, StandardCharsets.UTF_8,
        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
    Files.writeString(output, canonicalJson(report) + "\n", StandardCharsets.UTF_8,
        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
}
