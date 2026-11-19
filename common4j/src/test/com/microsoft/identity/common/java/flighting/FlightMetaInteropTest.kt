// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.common.java.flighting

import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Locale
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.ToolProvider

internal class FlightMetaInteropTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun metadata_isUsableOnJavaEnumConstants() {
        val source = object : SimpleJavaFileObject(
            URI.create("string:///Fixture.java"),
            JavaFileObject.Kind.SOURCE
        ) {
            override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence = """
                import com.microsoft.identity.common.java.flighting.FlightMeta;
                import com.microsoft.identity.common.java.flighting.FlagType;
                enum Fixture {
                    @FlightMeta(owner = "owning-team", type = FlagType.RELEASE,
                        premortem = "docs/premortem/example.md")
                    RELEASE_FEATURE,
                    @FlightMeta(type = FlagType.KILL_SWITCH)
                    SAFETY_CONTROL,
                    @FlightMeta(type = FlagType.CONFIG)
                    CONFIGURATION;
                }
            """.trimIndent()
        }
        val compiler = checkNotNull(ToolProvider.getSystemJavaCompiler()) { "JDK 17 is required" }
        val diagnostics = DiagnosticCollector<JavaFileObject>()
        val classpath = listOf(FlightMeta::class.java, Unit::class.java)
            .map { File(it.protectionDomain.codeSource.location.toURI()).absolutePath }
            .distinct()
            .joinToString(File.pathSeparator)
        compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8).use {
            val success = compiler.getTask(
                null,
                it,
                diagnostics,
                listOf("-proc:none", "-classpath", classpath, "-d", temporaryFolder.root.path),
                null,
                listOf(source)
            ).call()
            assertTrue(diagnostics.diagnostics.joinToString("\n"), success)
        }
    }
}
