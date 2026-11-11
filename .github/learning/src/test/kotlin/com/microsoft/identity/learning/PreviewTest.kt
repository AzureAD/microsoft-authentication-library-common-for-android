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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Paths

internal class PreviewTest {
    @get:Rule val temporary = TemporaryFolder()
    private val approvedSha = "b".repeat(40)
    private val environment = mapOf(
        "REVIEW_LEARNING_ENABLED" to "true",
        "REVIEW_LEARNING_POLICY" to Json.encode(mapOf("version" to 1, "host" to policy.host,
            "repository" to policy.repository, "base" to policy.base, "humanIds" to policy.humanIds,
            "ownerIds" to policy.ownerIds, "automationIds" to policy.automationIds, "guidancePaths" to policy.guidancePaths)),
        "REVIEW_LEARNING_PREVIEW_SHA" to approvedSha,
        "REVIEW_LEARNING_MODEL" to "approved-fixture",
        "REVIEW_LEARNING_MAX_AI_CREDITS" to "2",
        "GITHUB_REPOSITORY" to policy.repository,
        "GITHUB_EVENT_NAME" to "pull_request",
        "GITHUB_TOKEN" to "unused-preview-fixture"
    )

    private fun event(
        action: String = "labeled",
        label: String = PREVIEW_LABEL,
        actor: Actor = Actor(2, "User"),
        repository: String = policy.repository,
        headRepository: String = policy.repository,
        baseRepository: String = policy.repository,
        base: String = policy.base,
        head: String = approvedSha
    ): String = Json.encode(mapOf(
        "action" to action, "label" to mapOf("name" to label),
        "repository" to mapOf("full_name" to repository),
        "sender" to mapOf("id" to actor.id, "type" to actor.type),
        "pull_request" to mapOf("number" to 123,
            "head" to mapOf("sha" to head, "repo" to mapOf("full_name" to headRepository)),
            "base" to mapOf("ref" to base, "repo" to mapOf("full_name" to baseRepository)))
    ))

    @Test fun onlyAnExplicitOwnerLabelOnTheApprovedSameRepositoryRevisionIsAdmitted() {
        authorizePreview(policy, approvedSha, event())
        val rejected = listOf(
            event(action = "opened"), event(action = "synchronize"), event(action = "reopened"),
            event(label = "unrelated-label"), event(actor = Actor(1, "User")),
            event(actor = Actor(777, "User")), event(actor = Actor(99, "User")),
            event(actor = Actor(2, "Bot")), event(repository = "other/repo"),
            event(headRepository = "fork/repo"), event(baseRepository = "other/repo"),
            event(base = "master"), event(head = "c".repeat(40))
        )
        for (input in rejected) fails { authorizePreview(policy, approvedSha, input) }
        for (revision in listOf("", "dev", "b".repeat(39), approvedSha.uppercase())) {
            fails { authorizePreview(policy, revision, event()) }
        }
        fails { authorizePreview(policy, approvedSha, "{}") }
        fails { authorizePreview(policy, approvedSha, event().replace("\"id\":2", "\"id\":\"2\"")) }
    }

    @Test fun rejectedEventsCannotInitializeApiOrModelOrCreateAnOutputFile() = runBlocking {
        val eventFile = temporary.newFile("event.json").toPath()
        val output = temporary.root.toPath().resolve("proposal.json")
        var clients = 0
        val inputs = listOf(
            event(action = "synchronize") to emptyMap(),
            event(actor = Actor(1, "User")) to emptyMap(),
            event(headRepository = "fork/repo") to emptyMap(),
            event() to mapOf("REVIEW_LEARNING_PREVIEW_SHA" to "c".repeat(40)),
            event() to mapOf("REVIEW_LEARNING_PREVIEW_SHA" to ""),
            " ".repeat(2_000_001) to emptyMap()
        )
        for ((input, changes) in inputs) {
            Files.write(eventFile, input.toByteArray(Charsets.UTF_8))
            val command = Command(environment + ("GITHUB_EVENT_PATH" to eventFile.toString()) + changes,
                { _, _ -> clients++; throw AssertionError("Denied API client") },
                { _, _, _ -> clients++; throw AssertionError("Denied model client") })
            fails { runBlocking { command.run(arrayOf("prepare", output.toString())) } }
        }
        assertEquals(0, clients)
        assertFalse(Files.exists(output))
    }

    @Test fun admittedLivePreviewExtractsSynthesizesAndValidatesWithoutWrites() = runBlocking {
        val eventFile = temporary.newFile("event.json").toPath()
        Files.write(eventFile, event().toByteArray(Charsets.UTF_8))
        val output = temporary.root.toPath().resolve("proposal.json")
        val repository = MemoryRepository().also { it.add(1); it.add(2) }
        val source = packet(repository)
        var calls = 0
        val command = Command(environment + ("GITHUB_EVENT_PATH" to eventFile.toString()),
            { _, token -> assertEquals("unused-preview-fixture", token); repository },
            { _, _, token ->
                assertEquals("unused-preview-fixture", token)
                Engine { calls++; Json.encode(proposal(source)) }
            }, clock = { now })
        assertEquals(true, command.run(arrayOf("inspect", output.toString()))["hasSignal"])
        assertEquals(0, calls)
        assertEquals(1, command.run(arrayOf("prepare", output.toString()))["ruleCount"])
        assertEquals(1, calls)
        assertEquals(proposal(source), bundle(String(Files.readAllBytes(output), Charsets.UTF_8)).proposal)
        assertTrue(repository.writes.isEmpty())
        assertTrue(repository.branches.isEmpty())
    }

    @Test fun previewPublicationIsRejectedEvenWithPublicationApprovalAndWithoutReadingCredentials() = runBlocking {
        var clients = 0
        val command = Command(mapOf("REVIEW_LEARNING_ENABLED" to "true", "GITHUB_EVENT_NAME" to "pull_request",
            "REVIEW_LEARNING_PUBLISH_APPROVED" to "true"),
            { _, _ -> clients++; throw AssertionError("PR publisher client") })
        val output = temporary.root.toPath().resolve("nonexistent.json")
        fails { runBlocking { command.run(arrayOf("publish", output.toString())) } }
        assertEquals(0, clients)
        assertFalse(Files.exists(output))
    }

    @Test fun disabledPreviewRequiresNeitherEventNorCredentials() = runBlocking {
        val output = temporary.root.toPath().resolve("proposal.json")
        val command = Command(mapOf("GITHUB_EVENT_NAME" to "pull_request"),
            { _, _ -> throw AssertionError("Disabled preview API client") })
        assertEquals("disabled", command.run(arrayOf("prepare", output.toString()))["status"])
        assertFalse(Files.exists(output))
    }

    @Test fun workflowKeepsExactHeadCheckoutReadOnlyPreviewAndDevOnlyPublisher() {
        val workflow = String(Files.readAllBytes(Paths.get("..", "workflows", "review-learning.yml")), Charsets.UTF_8)
        val preparation = workflow.substringAfter("\n  prepare:\n").substringBefore("\n  publish:\n")
        val publishing = workflow.substringAfter("\n  publish:\n")
        assertFalse(workflow.contains("pull_request_target"))
        for (gate in listOf(
            "github.event.action == 'labeled'", "github.event.label.name == '$PREVIEW_LABEL'",
            "github.event.pull_request.head.repo.full_name == github.repository",
            "github.event.pull_request.base.repo.full_name == github.repository",
            "github.event.pull_request.base.ref == 'dev'", "github.event.sender.type == 'User'",
            "github.event.pull_request.head.sha == vars.REVIEW_LEARNING_PREVIEW_SHA",
            "contains(fromJSON(vars.REVIEW_LEARNING_POLICY).ownerIds, github.event.sender.id)",
            "!contains(fromJSON(vars.REVIEW_LEARNING_POLICY).automationIds, github.event.sender.id)"
        )) assertTrue("Missing preview admission gate: $gate", preparation.substringBefore("    runs-on:").contains(gate))
        assertTrue(preparation.contains("ref: \${{ github.event_name == 'pull_request' && github.event.pull_request.head.sha || github.sha }}"))
        assertFalse(preparation.contains("secrets."))
        assertFalse(preparation.contains("PUBLISH_APP"))
        assertFalse(preparation.contains("contents: write"))
        assertFalse(preparation.contains("pull-requests: write"))
        assertTrue(preparation.contains("copilot-requests: write"))
        for (gate in listOf("github.event_name == 'workflow_dispatch'", "github.ref == 'refs/heads/dev'",
            "inputs.mode == 'propose'", "vars.REVIEW_LEARNING_PUBLISH_APPROVED == 'true'",
            "vars.REVIEW_LEARNING_PROTECTION_VERIFIED == 'true'")) {
            assertTrue("Missing publication gate: $gate", publishing.substringBefore("    runs-on:").contains(gate))
        }
        assertFalse(workflow.contains("create-github-app-token"))
        assertFalse(workflow.contains("REVIEW_LEARNING_PUBLISH_APP_ID"))
        assertFalse(workflow.contains("REVIEW_LEARNING_PUBLISH_APP_PRIVATE_KEY"))
        assertFalse(publishing.contains("secrets."))
        assertFalse(publishing.contains("administration"))
        assertFalse(publishing.contains("copilot-requests: write"))
        assertTrue(publishing.contains("contents: write"))
        assertTrue(publishing.contains("pull-requests: write"))
        assertTrue(publishing.contains("GITHUB_TOKEN: \${{ github.token }}"))
    }
}
