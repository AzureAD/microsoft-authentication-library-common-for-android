// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.tools.flightgovernance

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.system.exitProcess

private const val USAGE = """Usage: flight-governance
  --source <label> <repository-directory> <revision> <relative-java-file> [--source ...]
  --exclude <label:qualified.Enum#CONSTANT> [--exclude ...]
  --output <new-json-file-outside-input-repositories>
"""

/** Writes a deterministic, create-once source inventory; it does not enforce flight policy. */
fun main(args: Array<String>) {
    exitProcess(runInventory(args.toList(), System.out, System.err))
}

internal data class SourceRequest(
    val repository: String,
    val root: Path,
    val revision: String,
    val path: String
)

internal fun runInventory(args: List<String>, out: PrintStream, error: PrintStream): Int {
    if (args == listOf("--help")) {
        out.print(USAGE)
        return 0
    }
    return try {
        val requests = mutableListOf<SourceRequest>()
        val exclusions = mutableSetOf<String>()
        var output: Path? = null
        var index = 0
        while (index < args.size) {
            when (val option = args[index++]) {
                "--source" -> {
                    require(args.size - index >= 4) { "--source requires four arguments" }
                    val label = args[index++]
                    require(label.matches(Regex("[A-Za-z0-9._-]+"))) { "Invalid source label: $label" }
                    val root = Path.of(args[index++]).toRealPath()
                    val revision = args[index++]
                    val path = args[index++].replace('\\', '/')
                    require(path.endsWith(".java") && !path.startsWith("/") &&
                        ':' !in path && path.split('/').none { it in setOf("", ".", "..") }
                    ) { "Source file must be a repository-relative Java path: $path" }
                    requests.add(SourceRequest(label, root, revision, path))
                }
                "--exclude" -> {
                    require(index < args.size) { "--exclude requires a declaration identity" }
                    require(exclusions.add(args[index++])) { "Duplicate exclusion" }
                }
                "--output" -> {
                    require(output == null && index < args.size) { "Specify --output exactly once" }
                    output = Path.of(args[index++]).toAbsolutePath().normalize()
                }
                else -> throw IllegalArgumentException("Unknown option: $option")
            }
        }
        require(requests.isNotEmpty()) { "At least one --source is required" }
        val destination = requireNotNull(output) { "--output is required" }
        val sources = mutableListOf<FlightSource>()
        val roots = mutableListOf<Path>()
        for (request in requests) {
            val root = Path.of(git(request.root, "rev-parse", "--show-toplevel").trim()).toRealPath()
            roots.add(root)
            val commit = git(root, "rev-parse", "--verify", "--end-of-options", "${request.revision}^{commit}")
                .trim()
            require(commit.matches(Regex("[0-9a-f]{40,64}"))) { "Invalid resolved commit" }
            sources.add(FlightSource(request.repository, commit, request.path, git(root, "show", "$commit:${request.path}")))
        }
        val realDestination = destination.parent.toRealPath().resolve(destination.fileName)
        require(roots.none { realDestination.startsWith(it) }) {
            "Output must be outside every input repository"
        }
        val snapshot = createSnapshot(sources, exclusions)
        Files.writeString(
            realDestination,
            canonicalJson(snapshot) + "\n",
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE_NEW,
            StandardOpenOption.WRITE
        )
        out.println("Created inventory: $realDestination")
        0
    } catch (exception: IllegalArgumentException) {
        error.println("Inventory failed: ${exception.message}")
        2
    } catch (exception: IOException) {
        error.println("Inventory failed: ${exception.message}")
        1
    }
}

internal fun createSnapshot(sources: List<FlightSource>, exclusions: Set<String>): JSONObject {
    require(sources.isNotEmpty()) { "No sources supplied" }
    require(sources.map { Triple(it.repository, it.commit, it.path) }.distinct().size == sources.size) {
        "Duplicate source"
    }
    require(sources.groupBy { it.repository }.values.all { group ->
        group.map { it.commit }.distinct().size == 1
    }) { "Each repository label must identify one commit" }
    val parser = FlightSourceParser()
    val declarations = sources.flatMap(parser::parse).sortedBy { it.id }
    require(declarations.map { it.id }.distinct().size == declarations.size) {
        "Duplicate declaration identity; use distinct source labels for different snapshots"
    }
    val available = declarations.map { it.id }.toSet()
    require(exclusions.all { it in available }) { "Unknown exclusion: ${(exclusions - available).sorted()}" }
    val included = declarations.filterNot { it.id in exclusions }
    val excluded = declarations.filter { it.id in exclusions }
    return JSONObject()
        .put("schemaVersion", 1)
        .put("sources", JSONArray(sources.sortedWith(compareBy({ it.repository }, { it.path })).map {
            JSONObject().put("repository", it.repository).put("commit", it.commit).put("path", it.path)
        }))
        .put("declarations", JSONArray(included.map(::declarationJson)))
        .put("excludedDeclarations", JSONArray(excluded.map(::declarationJson)))
        .put("summary", JSONObject()
            .put("declarationCount", included.size)
            .put("uniqueKeyCount", included.map { it.key }.toSet().size)
            .put("excludedCount", excluded.size)
            .put("unresolvedDefaultCount", included.count { it.default.type == DefaultType.UNRESOLVED }))
}

private fun declarationJson(declaration: FlightDeclaration): JSONObject = JSONObject()
    .put("id", declaration.id)
    .put("repository", declaration.repository)
    .put("commit", declaration.commit)
    .put("path", declaration.path)
    .put("line", declaration.line)
    .put("enum", declaration.enumName)
    .put("constant", declaration.constant)
    .put("key", declaration.key)
    .put("default", JSONObject()
        .put("type", declaration.default.type.name)
        .put("expression", declaration.default.expression)
        .put("normalizedExpression", declaration.default.normalizedExpression)
        .put("value", declaration.default.value ?: JSONObject.NULL)
        .put("unresolvedReason", declaration.default.unresolvedReason ?: JSONObject.NULL))
    .put("annotations", JSONArray(declaration.annotations.map {
        JSONObject().put("type", it.type).put("arguments", JSONObject(it.arguments))
    }))

internal fun canonicalJson(value: Any?): String = when (value) {
    is JSONObject -> value.keySet().sorted().joinToString(",", "{", "}") { key ->
        "${JSONObject.quote(key)}:${canonicalJson(value.get(key))}"
    }
    is JSONArray -> (0 until value.length()).joinToString(",", "[", "]") { canonicalJson(value.get(it)) }
    else -> JSONObject.valueToString(value)
}

private fun git(root: Path, vararg arguments: String): String {
    val process = ProcessBuilder(listOf("git", "-C", root.toString()) + arguments)
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
    require(process.waitFor() == 0) { "git ${arguments.first()} failed: ${output.trim()}" }
    return output
}
