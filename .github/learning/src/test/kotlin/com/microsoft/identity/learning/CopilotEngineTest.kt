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

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

private class CliFixture(
    stdout: String,
    stderr: String = "",
    private val status: Int = 0,
    private val timeout: Boolean = false
) : Process() {
    val stdin = ByteArrayOutputStream()
    private val output = ByteArrayInputStream(stdout.toByteArray(Charsets.UTF_8))
    private val errors = ByteArrayInputStream(stderr.toByteArray(Charsets.UTF_8))
    private var finished = false
    var destroyed = false
    override fun getOutputStream() = stdin
    override fun getInputStream() = output
    override fun getErrorStream() = errors
    override fun waitFor(): Int { finished = true; return status }
    override fun waitFor(timeout: Long, unit: TimeUnit): Boolean {
        finished = !this.timeout
        return finished
    }
    override fun exitValue(): Int = if (finished) status else throw IllegalThreadStateException("Still running")
    override fun isAlive(): Boolean = !finished
    override fun destroy() { destroyed = true; finished = true }
    override fun destroyForcibly(): Process { destroy(); return this }
}

internal class CopilotEngineTest {
    private val help = "--max-ai-credits --no-custom-instructions --excluded-tools --sync-session"

    @Test fun subprocessReceivesOnlyStdinEvidenceAndAnIsolatedCredentialEnvironment() {
        val commands = mutableListOf<List<String>>()
        val homes = mutableListOf<Path>()
        val synthesis = CliFixture("""{"version":1,"rules":[]}""")
        val engine = CopilotEngine("fixture-cli", "approved-model", "30", "unused-process-fixture") { builder ->
            commands.add(builder.command().toList())
            val home = builder.directory().toPath()
            homes.add(home)
            assertTrue(Files.isDirectory(home))
            val env = builder.environment()
            assertTrue(env.keys.all { it in setOf("PATH", "SystemRoot", "HOME", "USERPROFILE", "TMPDIR", "TEMP", "TMP", "GITHUB_TOKEN") })
            assertEquals("unused-process-fixture", env["GITHUB_TOKEN"])
            for (key in listOf("HOME", "USERPROFILE", "TMPDIR", "TEMP", "TMP")) assertEquals(home.toString(), env[key])
            if (builder.command().last() == "--help") CliFixture(help) else synthesis
        }
        val prompt = "private-evidence-fixture"
        assertEquals("""{"version":1,"rules":[]}""", runBlocking { engine.generate(prompt) })
        assertEquals(2, commands.size)
        assertTrue(commands.flatten().none { prompt in it || "unused-process-fixture" in it })
        assertEquals(prompt, synthesis.stdin.toString("UTF-8"))
        assertTrue(commands.last().contains("--deny-tool=read,write,shell,url,memory"))
        assertTrue(commands.last().last().startsWith("--excluded-tools="))
        assertTrue(homes.distinct().size == 1 && homes.none { Files.exists(it) })
    }

    @Test fun failureReportsOnlyStaticCategoryExitAndStageNotRawDiagnostics() {
        val cases = listOf(
            "Invalid --deny-tool value. Invalid rule format: *" to "invalid_arguments",
            "The max-ai-credits limit must be at least 30" to "credit_limit",
            "HTTP 403 forbidden" to "authentication_or_policy",
            "Error: could not connect to API; proxy CONNECT failed" to "network",
            "Unknown model" to "model_access",
            "Unrecognized failure" to "unclassified"
        )
        for ((diagnostic, category) in cases) {
            lateinit var home: Path
            val engine = CopilotEngine("fixture-cli", "approved-model", "30", "unused-process-fixture") { builder ->
                home = builder.directory().toPath()
                if (builder.command().last() == "--help") CliFixture(help) else
                    CliFixture("private-output-fixture", "$diagnostic; private-error-fixture; unused-process-fixture", 2)
            }
            val failure = assertThrows(LearningFailure::class.java) { runBlocking { engine.generate("private-input-fixture") } }
            val message = failure.message!!
            assertTrue(message.contains("synthesis failed (exit=2, category=$category)"))
            assertFalse(message.contains("private-"))
            assertFalse(message.contains("unused-process-fixture"))
            assertFalse(Files.exists(home))
        }
    }

    @Test fun incompatibleHelpOrFailedHelpStopsBeforeSynthesisAndCleansUp() {
        for (exit in listOf(0, 3)) {
            var calls = 0
            lateinit var home: Path
            val engine = CopilotEngine("fixture-cli", "approved-model", "30", "unused") { builder ->
                calls++
                home = builder.directory().toPath()
                CliFixture("unsupported CLI", "unknown option; private-error-fixture", exit)
            }
            val failure = assertThrows(LearningFailure::class.java) { runBlocking { engine.generate("private-input-fixture") } }
            assertEquals(1, calls)
            assertTrue(failure.message!!.contains(if (exit == 0) "does not support" else "help preflight failed"))
            assertFalse(failure.message!!.contains("private-"))
            assertFalse(Files.exists(home))
        }
    }

    @Test fun unavailableExecutableAndTimeoutRemainExplicitAndReleaseTemporaryState() {
        lateinit var missingHome: Path
        val missing = CopilotEngine("fixture-cli", "approved-model", "30", "unused") { builder ->
            missingHome = builder.directory().toPath()
            throw IOException("private-installation-path-fixture")
        }
        val failure = assertThrows(LearningFailure::class.java) { runBlocking { missing.generate("fixture") } }
        assertTrue(failure.message!!.contains("could not start"))
        assertFalse(failure.message!!.contains("private-"))
        assertFalse(Files.exists(missingHome))
        lateinit var timedHome: Path
        val process = CliFixture("", timeout = true)
        val timed = CopilotEngine("fixture-cli", "approved-model", "30", "unused") { builder ->
            timedHome = builder.directory().toPath()
            if (builder.command().last() == "--help") CliFixture(help) else process
        }
        val timeout = assertThrows(LearningFailure::class.java) { runBlocking { timed.generate("fixture") } }
        assertTrue(timeout.message!!.contains("synthesis timed out"))
        assertTrue(process.destroyed)
        assertFalse(Files.exists(timedHome))
    }

    @Test fun oversizedProcessOutputFailsWithoutReturningPartialModelContent() {
        for (errorStream in listOf(false, true)) {
            lateinit var home: Path
            val engine = CopilotEngine("fixture-cli", "approved-model", "30", "unused") { builder ->
                home = builder.directory().toPath()
                if (builder.command().last() == "--help") CliFixture(help) else
                    CliFixture(if (errorStream) "" else "x".repeat(256_001), if (errorStream) "x".repeat(16_001) else "")
            }
            val failure = assertThrows(LearningFailure::class.java) { runBlocking { engine.generate("fixture") } }
            assertEquals("Synthesis output exceeds the limit", failure.message)
            assertFalse(Files.exists(home))
        }
    }
}
