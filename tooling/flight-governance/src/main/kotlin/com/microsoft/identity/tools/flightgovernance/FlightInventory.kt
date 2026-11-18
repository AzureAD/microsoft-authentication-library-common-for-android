// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.tools.flightgovernance

import com.sun.source.tree.AnnotationTree
import com.sun.source.tree.AssignmentTree
import com.sun.source.tree.ClassTree
import com.sun.source.tree.ExpressionTree
import com.sun.source.tree.LiteralTree
import com.sun.source.tree.NewClassTree
import com.sun.source.tree.ParenthesizedTree
import com.sun.source.tree.Tree
import com.sun.source.tree.UnaryTree
import com.sun.source.tree.VariableTree
import com.sun.source.util.JavacTask
import com.sun.source.util.TreePathScanner
import com.sun.source.util.Trees
import org.json.JSONException
import org.json.JSONObject
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Locale
import javax.tools.Diagnostic
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.ToolProvider

internal data class FlightSource(
    val repository: String,
    val commit: String,
    val path: String,
    val content: String
)

internal enum class DefaultType {
    BOOLEAN, INT, LONG, FLOAT, DOUBLE, STRING, CHAR, NULL, JSON_OBJECT, UNRESOLVED
}

internal data class FlightDefault(
    val type: DefaultType,
    val expression: String,
    val normalizedExpression: String,
    val value: Any? = null,
    val unresolvedReason: String? = null
)

internal data class SourceAnnotation(
    val type: String,
    val arguments: Map<String, String>
)

internal data class FlightDeclaration(
    val repository: String,
    val commit: String,
    val path: String,
    val line: Long,
    val enumName: String,
    val constant: String,
    val key: String,
    val default: FlightDefault,
    val annotations: List<SourceAnnotation>
) {
    val id: String get() = "$repository:$enumName#$constant"
}

internal class FlightSourceParser {
    fun parse(source: FlightSource): List<FlightDeclaration> {
        val compiler = checkNotNull(ToolProvider.getSystemJavaCompiler()) {
            "The inventory requires a JDK, not a JRE"
        }
        val diagnostics = DiagnosticCollector<JavaFileObject>()
        val input = object : SimpleJavaFileObject(
            URI("string", null, "/${source.path}", null),
            JavaFileObject.Kind.SOURCE
        ) {
            override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence = source.content
        }
        compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8).use {
            val task = compiler.getTask(null, it, diagnostics, listOf("-proc:none"), null, listOf(input))
            check(task is JavacTask) { "A JDK javac compiler is required" }
            val units = task.parse().toList()
            val errors = diagnostics.diagnostics.filter { diagnostic ->
                diagnostic.kind == Diagnostic.Kind.ERROR
            }
            require(errors.isEmpty()) {
                errors.joinToString("\n") { error ->
                    "${source.path}:${error.lineNumber}: ${error.getMessage(Locale.ROOT)}"
                }
            }
            val records = mutableListOf<FlightDeclaration>()
            val positions = Trees.instance(task).sourcePositions
            for (unit in units) {
                fun expression(tree: Tree): String {
                    val start = positions.getStartPosition(unit, tree)
                    val end = positions.getEndPosition(unit, tree)
                    check(start >= 0 && end >= start) { "Missing source location in ${source.path}" }
                    return source.content.substring(start.toInt(), end.toInt())
                }
                val jsonImported = unit.packageName?.toString() == "org.json" ||
                    unit.imports.any { import ->
                        import.qualifiedIdentifier.toString() in setOf("org.json.JSONObject", "org.json.*")
                    }
                val scanner = object : TreePathScanner<Void?, List<String>>() {
                    override fun visitClass(node: ClassTree, owners: List<String>): Void? {
                        val name = node.simpleName.toString()
                        val names = if (name.isEmpty()) owners else owners + name
                        if (node.kind == Tree.Kind.ENUM && node.implementsClause.any { type ->
                                type.toString().substringAfterLast('.') == "IFlightConfig"
                            }
                        ) {
                            val enumName = (listOfNotNull(unit.packageName?.toString()) + names)
                                .joinToString(".")
                            for (field in node.members.filterIsInstance<VariableTree>()) {
                                val initializer = field.initializer
                                if (initializer !is NewClassTree ||
                                    field.type?.toString()?.substringAfterLast('.') != name ||
                                    initializer.identifier.toString().substringAfterLast('.') != name
                                ) {
                                    continue
                                }
                                val identity = "$enumName#${field.name}"
                                require(initializer.arguments.size == 2) {
                                    "$identity must declare exactly a key and default"
                                }
                                val keyTree = initializer.arguments[0]
                                val key = (keyTree as? LiteralTree)?.value
                                require(key is String && key.isNotBlank()) {
                                    "$identity requires a nonblank string-literal key"
                                }
                                val defaultTree = initializer.arguments[1]
                                records.add(
                                    FlightDeclaration(
                                        source.repository,
                                        source.commit,
                                        source.path,
                                        unit.lineMap.getLineNumber(positions.getStartPosition(unit, field)),
                                        enumName,
                                        field.name.toString(),
                                        key,
                                        defaultValue(defaultTree, expression(defaultTree), jsonImported),
                                        field.modifiers.annotations.map { annotation ->
                                            annotation(annotation, ::expression)
                                        }
                                    )
                                )
                            }
                        }
                        return super.visitClass(node, names)
                    }
                }
                scanner.scan(unit, emptyList())
            }
            require(records.isNotEmpty()) { "No supported IFlightConfig enum declarations in ${source.path}" }
            return records.toList()
        }
    }

    private fun annotation(
        annotation: AnnotationTree,
        expression: (Tree) -> String
    ): SourceAnnotation {
        val arguments = linkedMapOf<String, String>()
        for (argument in annotation.arguments) {
            val name = if (argument is AssignmentTree) argument.variable.toString() else "value"
            require(name !in arguments) { "Duplicate annotation argument: $name" }
            arguments[name] = expression(if (argument is AssignmentTree) argument.expression else argument)
        }
        return SourceAnnotation(annotation.annotationType.toString(), arguments.toMap())
    }

    private fun defaultValue(
        tree: ExpressionTree,
        expression: String,
        jsonImported: Boolean
    ): FlightDefault {
        fun known(type: DefaultType, value: Any?) =
            FlightDefault(type, expression, tree.toString(), value)
        fun unresolved(reason: String) =
            FlightDefault(DefaultType.UNRESOLVED, expression, tree.toString(), unresolvedReason = reason)

        if (tree is ParenthesizedTree) {
            return defaultValue(tree.expression, expression, jsonImported)
                .copy(normalizedExpression = tree.toString())
        }
        if (tree is LiteralTree) {
            return when (val value = tree.value) {
                is Boolean -> known(DefaultType.BOOLEAN, value)
                is Int -> known(DefaultType.INT, value)
                is Long -> known(DefaultType.LONG, value)
                is Float -> known(DefaultType.FLOAT, value)
                is Double -> known(DefaultType.DOUBLE, value)
                is String -> known(DefaultType.STRING, value)
                is Char -> known(DefaultType.CHAR, value.toString())
                null -> known(DefaultType.NULL, null)
                else -> unresolved("Unsupported literal type")
            }
        }
        if (tree is UnaryTree && tree.kind in setOf(Tree.Kind.UNARY_MINUS, Tree.Kind.UNARY_PLUS)) {
            val operand = defaultValue(tree.expression, expression, jsonImported)
            val negative = tree.kind == Tree.Kind.UNARY_MINUS
            val value = when (val number = operand.value) {
                is Int -> if (negative) -number else number
                is Long -> if (negative) -number else number
                is Float -> if (negative) -number else number
                is Double -> if (negative) -number else number
                else -> return unresolved("Unary operand is not a resolved numeric literal")
            }
            return known(operand.type, value)
        }
        if (tree is NewClassTree && tree.classBody == null &&
            (tree.identifier.toString() == "org.json.JSONObject" ||
                (jsonImported && tree.identifier.toString() == "JSONObject"))
        ) {
            if (tree.arguments.isEmpty()) {
                return known(DefaultType.JSON_OBJECT, JSONObject())
            }
            val text = (tree.arguments.singleOrNull() as? LiteralTree)?.value
            if (text is String) {
                return try {
                    known(DefaultType.JSON_OBJECT, JSONObject(text))
                } catch (exception: JSONException) {
                    unresolved("Invalid JSONObject literal: ${exception.message}")
                }
            }
        }
        return unresolved("Expression requires evaluation; the inventory does not execute source code")
    }
}
