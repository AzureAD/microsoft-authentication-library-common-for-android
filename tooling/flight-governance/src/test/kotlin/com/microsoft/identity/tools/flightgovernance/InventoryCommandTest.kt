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

internal class InventoryCommandTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun command_readsCommittedSourceAndRefusesOverwriteOrCheckoutOutput() {
        val root = temporaryFolder.newFolder("repository").toPath()
        git(root, "init", "-q")
        git(root, "config", "core.hooksPath", temporaryFolder.newFolder("hooks").absolutePath)
        val javaFile = root.resolve("Features.java")
        Files.writeString(javaFile, """
            // Copyright (c) Microsoft Corporation. All rights reserved.
            enum Features implements IFlightConfig {
                FEATURE_v2("ExampleFeature", false);
                Features(String key, Object value) {}
            }
        """.trimIndent())
        git(root, "add", "Features.java")
        git(root, "-c", "user.name=Fixture", "-c", "user.email=fixture@example.invalid",
            "-c", "commit.gpgsign=false", "commit", "-q", "-m",
            "Create fixture\n\nCo-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>")
        val commit = git(root, "rev-parse", "HEAD").trim()
        Files.writeString(javaFile, "this working-tree copy is intentionally invalid")
        val before = git(root, "status", "--porcelain")
        val output = temporaryFolder.root.toPath().resolve("inventory.json")
        val messages = ByteArrayOutputStream()
        PrintStream(messages, true, StandardCharsets.UTF_8).use { stream ->
            val arguments = listOf("--source", "fixture", root.toString(), "HEAD", "Features.java",
                "--output", output.toString())
            assertEquals(0, runInventory(arguments, stream, stream))
            val bytes = Files.readAllBytes(output)
            val snapshot = JSONObject(Files.readString(output))
            assertEquals(commit, snapshot.getJSONArray("sources").getJSONObject(0).getString("commit"))
            assertEquals("ExampleFeature", snapshot.getJSONArray("declarations").getJSONObject(0).getString("key"))
            assertTrue(runInventory(arguments, stream, stream) != 0)
            assertTrue(bytes.contentEquals(Files.readAllBytes(output)))
            assertTrue(runInventory(arguments.dropLast(1) + root.resolve("forbidden.json").toString(), stream, stream) != 0)
            assertTrue(!Files.exists(root.resolve("forbidden.json")))
            assertEquals(before, git(root, "status", "--porcelain"))
        }
    }

    @Test
    fun command_rejectsBadArgumentsWithoutCreatingOutput() {
        val messages = ByteArrayOutputStream()
        PrintStream(messages, true, StandardCharsets.UTF_8).use { stream ->
            assertEquals(2, runInventory(emptyList(), stream, stream))
            assertEquals(2, runInventory(listOf("--unknown"), stream, stream))
            assertEquals(2, runInventory(listOf("--source", "incomplete"), stream, stream))
            assertEquals(0, runInventory(listOf("--help"), stream, stream))
        }
        assertTrue(messages.toString(StandardCharsets.UTF_8).contains("Inventory failed:"))
    }

    private fun git(root: Path, vararg arguments: String): String {
        val process = ProcessBuilder(listOf("git", "-C", root.toString()) + arguments)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        check(process.waitFor() == 0) { output }
        return output
    }
}
