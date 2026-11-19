// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.tools.flightgovernance

import com.sun.source.tree.AnnotationTree
import com.sun.source.tree.AssignmentTree
import com.sun.source.tree.LiteralTree
import com.sun.source.tree.Tree
import com.sun.source.tree.VariableTree
import com.sun.source.util.JavacTask
import com.sun.source.util.TreeScanner
import com.sun.source.util.Trees
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Locale
import javax.tools.Diagnostic
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.ToolProvider

internal data class PolicySource(
    val declarations: List<PolicyDeclaration>,
    val runtimeShape: String
)

internal class JavaPolicyAdapter {
    fun parse(source: FlightSource): PolicySource {
        val declarations = FlightSourceParser().parse(source)
        val metadata = mutableMapOf<Pair<String, Long>, PolicyMetadata>()
        val removals = mutableListOf<IntRange>()
        val compiler = checkNotNull(ToolProvider.getSystemJavaCompiler()) { "JDK 17 is required" }
        val diagnostics = DiagnosticCollector<JavaFileObject>()
        compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8).use { files ->
            fun parse(content: String): Pair<JavacTask, com.sun.source.tree.CompilationUnitTree> {
                val file = object : SimpleJavaFileObject(URI("string", null, "/${source.path}", null),
                    JavaFileObject.Kind.SOURCE) {
                    override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence = content
                }
                val task = compiler.getTask(null, files, diagnostics, listOf("-proc:none"), null, listOf(file))
                check(task is JavacTask) { "A javac compiler is required" }
                val unit = task.parse().single()
                require(diagnostics.diagnostics.none { it.kind == Diagnostic.Kind.ERROR }) {
                    "Cannot normalize Java policy input: ${source.path}"
                }
                return task to unit
            }
            val (task, unit) = parse(source.content)
            val positions = Trees.instance(task).sourcePositions
            fun remove(tree: Tree) {
                val start = positions.getStartPosition(unit, tree).toInt()
                val end = positions.getEndPosition(unit, tree).toInt()
                require(start >= 0 && end > start) { "Missing Java metadata source positions" }
                removals.add(start until end)
            }
            val namespace = "com.microsoft.identity.common.java.flighting"
            val imports = unit.imports.map { it.qualifiedIdentifier.toString() }.toSet()
            val contractInScope = unit.packageName?.toString() == namespace ||
                "$namespace.FlightMeta" in imports || "$namespace.*" in imports
            fun recognized(annotation: AnnotationTree): Boolean =
                annotation.annotationType.toString() == "$namespace.FlightMeta" ||
                    (contractInScope && annotation.annotationType.toString() == "FlightMeta")
            unit.imports.filter {
                it.qualifiedIdentifier.toString() in setOf("$namespace.FlightMeta", "$namespace.FlagType") ||
                    (it.isStatic && it.qualifiedIdentifier.toString().startsWith("$namespace.FlagType."))
            }.forEach(::remove)
            object : TreeScanner<Void?, Void?>() {
                override fun visitVariable(node: VariableTree, unused: Void?): Void? {
                    val annotations = node.modifiers.annotations.filter(::recognized)
                    if (annotations.isNotEmpty()) {
                        val values = mutableMapOf<String, String>()
                        val invalid = mutableSetOf<String>()
                        for (annotation in annotations) {
                            remove(annotation)
                            for (argument in annotation.arguments) {
                                val assignment = argument as? AssignmentTree
                                val key = assignment?.variable?.toString() ?: "value"
                                val value = assignment?.expression
                                val resolved = when {
                                    key in setOf("owner", "premortem") ->
                                        (value as? LiteralTree)?.value as? String
                                    key == "type" && value != null ->
                                        value.toString().takeIf {
                                            it.matches(Regex("(?:(?:[A-Za-z_$][\\w$]*\\.)*FlagType\\.)?(RELEASE|KILL_SWITCH|CONFIG)"))
                                        }?.substringAfterLast('.')
                                    else -> null
                                }
                                if (resolved == null || values.put(key, resolved) != null) invalid.add(key)
                            }
                        }
                        if (annotations.size != 1) invalid.add("duplicate-annotation")
                        val line = unit.lineMap.getLineNumber(positions.getStartPosition(unit, node))
                        metadata[node.name.toString() to line] = PolicyMetadata(
                            values["owner"].orEmpty(), values["type"].orEmpty(),
                            values["premortem"].orEmpty(), invalid
                        )
                    }
                    return super.visitVariable(node, unused)
                }
            }.scan(unit, null)
            val runtimeSource = StringBuilder(source.content)
            removals.distinct().sortedByDescending { it.first }.forEach {
                runtimeSource.delete(it.first, it.last + 1)
            }
            val runtimeUnit = parse(runtimeSource.toString()).second
            // Printing the compilation unit would retain its Javadoc comment table.
            val shape = (runtimeUnit.packageAnnotations + listOfNotNull(runtimeUnit.packageName) +
                runtimeUnit.imports + runtimeUnit.typeDecls).joinToString("\n") { it.toString() }
                .replace("\r\n", "\n").replace('\r', '\n')
            return PolicySource(declarations.map {
                PolicyDeclaration(it.id, it.path, it.line, it.key, it.default.normalizedExpression,
                    metadata[it.constant to it.line])
            }, shape)
        }
    }
}
