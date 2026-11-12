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
    private val publicationEnvironment = environment + mapOf(
        "REVIEW_LEARNING_PUBLISH_TEST_SHA" to approvedSha,
        "REVIEW_LEARNING_PUBLISH_APPROVED" to "true",
        "REVIEW_LEARNING_PROTECTION_VERIFIED" to "true",
        "REVIEW_LEARNING_PUBLISH_APP_ID" to testPublisherAppId.toString(),
        "REVIEW_LEARNING_PUBLISH_BOT_ID" to testPublisherBotId.toString(),
        "GITHUB_ACTIONS" to "true",
        "GITHUB_SERVER_URL" to "https://github.com",
        "GITHUB_REF" to "refs/pull/123/merge"
    )

    private fun event(
        action: String = "labeled",
        label: String = PREVIEW_LABEL,
        actor: Actor = Actor(2, "User"),
        repository: String = policy.repository,
        headRepository: String = policy.repository,
        baseRepository: String = policy.repository,
        base: String = policy.base,
        head: String = approvedSha,
        state: String = "open"
    ): String = Json.encode(mapOf(
        "action" to action, "label" to mapOf("name" to label),
        "repository" to mapOf("full_name" to repository),
        "sender" to mapOf("id" to actor.id, "type" to actor.type),
        "pull_request" to mapOf("number" to 123, "state" to state,
            "head" to mapOf("sha" to head, "repo" to mapOf("full_name" to headRepository)),
            "base" to mapOf("ref" to base, "repo" to mapOf("full_name" to baseRepository)))
    ))

    @Test fun onlyAnExplicitOwnerLabelOnTheApprovedSameRepositoryRevisionIsAdmitted() {
        assertFalse(authorizePreview(policy, approvedSha, event()))
        val rejected = listOf(
            event(action = "opened"), event(action = "synchronize"), event(action = "reopened"),
            event(label = "unrelated-label"), event(actor = Actor(1, "User")),
            event(actor = Actor(777, "User")), event(actor = Actor(99, "User")),
            event(actor = Actor(2, "Bot")), event(repository = "other/repo"),
            event(headRepository = "fork/repo"), event(baseRepository = "other/repo"),
            event(base = "master"), event(head = "c".repeat(40)), event(state = "closed")
        )
        for (input in rejected) fails { authorizePreview(policy, approvedSha, input) }
        for (revision in listOf("", "dev", "b".repeat(39), approvedSha.uppercase())) {
            fails { authorizePreview(policy, revision, event()) }
        }
        fails { authorizePreview(policy, approvedSha, "{}") }
        fails { authorizePreview(policy, approvedSha, event().replace("\"id\":2", "\"id\":\"2\"")) }
    }

    @Test fun publicationLabelNeedsItsOwnCommitApprovalAndCannotReuseReadOnlyConsent() {
        val input = event(label = PUBLISH_TEST_LABEL)
        assertTrue(authorizePreview(policy, "", input, approvedSha, publishing = true))
        fails { authorizePreview(policy, approvedSha, input, publishing = true) }
        fails { authorizePreview(policy, approvedSha, input, "c".repeat(40), publishing = true) }
        fails { authorizePreview(policy, approvedSha, event(), approvedSha, publishing = true) }
        fails { authorizePreview(policy, "", input, "dev", publishing = true) }
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

    @Test fun disabledPublicationTestDoesNotReadEventsOrCreateClientsOrOutput() = runBlocking {
        val output = temporary.root.toPath().resolve("proposal.json")
        val command = Command(publicationEnvironment + ("REVIEW_LEARNING_ENABLED" to "false"),
            { _, _ -> throw AssertionError("Disabled publication-test client") },
            { _, _, _ -> throw AssertionError("Disabled publication-test model") })
        for (action in listOf("inspect", "prepare", "publish")) {
            assertEquals("disabled", command.run(arrayOf(action, output.toString()))["status"])
        }
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

    @Test fun approvedPublicationTestUsesReadOnlyGenerationThenCreatesOnlyDraftAndCheckpointFromDev() = runBlocking {
        val eventFile = temporary.newFile("publication-event.json").toPath()
        Files.write(eventFile, event(label = PUBLISH_TEST_LABEL).toByteArray(Charsets.UTF_8))
        val output = temporary.root.toPath().resolve("proposal.json")
        val repository = MemoryRepository().also { it.add(1); it.add(2) }
        val source = packet(repository)
        val context = publicationEnvironment + ("GITHUB_EVENT_PATH" to eventFile.toString())
        var syntheses = 0
        val generator = Command(context,
            { _, token -> assertEquals("unused-preview-fixture", token); repository },
            { _, _, token ->
                assertEquals("unused-preview-fixture", token)
                Engine { syntheses++; Json.encode(proposal(source)) }
            }, clock = { now })
        assertEquals(true, generator.run(arrayOf("inspect", output.toString()))["hasSignal"])
        assertEquals(1, generator.run(arrayOf("prepare", output.toString()))["ruleCount"])
        assertEquals(1, syntheses)
        assertTrue(repository.writes.isEmpty())
        assertEquals(repository.base, bundle(String(Files.readAllBytes(output), Charsets.UTF_8)).baseSha)
        assertNotEquals(approvedSha, repository.base)

        val writer = Command(context + ("GITHUB_TOKEN" to "unused-publication-fixture"),
            { _, token -> assertEquals("unused-publication-fixture", token); repository },
            { _, _, _ -> throw AssertionError("Publisher must not construct a model client") }, clock = { now })
        assertEquals("draft_created", writer.run(arrayOf("publish", output.toString()))["status"])
        val draft = repository.openLearningPulls().single()
        assertEquals(testPublisherBotId, draft.author.id)
        assertEquals("dev", draft.base)
        assertEquals(setOf(repository.policy.stateBranch, draft.head), repository.writes.toSet())
        val edits = repository.files.filter { (key, value) ->
            key.second == draft.headSha && repository.files[key.first to repository.base] != value
        }.keys.map { it.first }.toSet()
        assertEquals(setOf(topic, ".github/review-learning-proposal.json"), edits)
        assertEquals("## Invariants\nOld guidance.\n", repository.files[topic to repository.base])

        assertEquals(false, generator.run(arrayOf("inspect", output.toString()))["hasSignal"])
        assertEquals(0, generator.run(arrayOf("prepare", output.toString()))["ruleCount"])
        assertEquals("queued", writer.run(arrayOf("publish", output.toString()))["status"])
        assertEquals(1, repository.openLearningPulls().size)
        assertEquals(1, syntheses)
    }

    @Test fun publicationTestRejectsUnapprovedEventsAndSettingsBeforeAnyClientOrOutput() = runBlocking {
        val eventFile = temporary.newFile("publication-event.json").toPath()
        val output = temporary.root.toPath().resolve("proposal.json")
        val valid = event(label = PUBLISH_TEST_LABEL)
        val inputs = listOf(
            event(label = PUBLISH_TEST_LABEL, action = "synchronize") to emptyMap(),
            event(label = PUBLISH_TEST_LABEL, actor = Actor(1, "User")) to emptyMap(),
            event(label = PUBLISH_TEST_LABEL, actor = Actor(777, "User")) to emptyMap(),
            event(label = PUBLISH_TEST_LABEL, actor = Actor(2, "Bot")) to emptyMap(),
            event(label = PUBLISH_TEST_LABEL, actor = Actor(testPublisherBotId, "Bot")) to emptyMap(),
            event(label = PUBLISH_TEST_LABEL, headRepository = "fork/repo") to emptyMap(),
            event(label = PUBLISH_TEST_LABEL, baseRepository = "fork/repo") to emptyMap(),
            event(label = PUBLISH_TEST_LABEL, repository = "other/repo") to emptyMap(),
            event(label = PUBLISH_TEST_LABEL, base = "master") to emptyMap(),
            event(label = PUBLISH_TEST_LABEL, state = "closed") to emptyMap(),
            event(label = PUBLISH_TEST_LABEL, head = "c".repeat(40)) to emptyMap(),
            event(label = "unrelated-label") to emptyMap(),
            valid to mapOf("REVIEW_LEARNING_PUBLISH_TEST_SHA" to ""),
            valid to mapOf("REVIEW_LEARNING_PUBLISH_TEST_SHA" to "c".repeat(40)),
            valid to mapOf("REVIEW_LEARNING_PUBLISH_APPROVED" to ""),
            valid to mapOf("REVIEW_LEARNING_PUBLISH_APPROVED" to "false"),
            valid to mapOf("REVIEW_LEARNING_PROTECTION_VERIFIED" to ""),
            valid to mapOf("REVIEW_LEARNING_PROTECTION_VERIFIED" to "false"),
            valid to mapOf("REVIEW_LEARNING_PUBLISH_APP_ID" to ""),
            valid to mapOf("REVIEW_LEARNING_PUBLISH_APP_ID" to "0"),
            valid to mapOf("REVIEW_LEARNING_PUBLISH_BOT_ID" to ""),
            valid to mapOf("REVIEW_LEARNING_PUBLISH_BOT_ID" to "0"),
            valid to mapOf("GITHUB_ACTIONS" to "false"),
            valid to mapOf("GITHUB_SERVER_URL" to "https://msft.ghe.com"),
            valid to mapOf("REVIEW_LEARNING_POLICY" to Json.parse(environment.getValue("REVIEW_LEARNING_POLICY"))
                .obj().also { it.add("automationIds", Json.parse("[99]")) }.toString())
        )
        var clients = 0
        for ((input, changes) in inputs) {
            Files.write(eventFile, input.toByteArray(Charsets.UTF_8))
            val command = Command(publicationEnvironment + ("GITHUB_EVENT_PATH" to eventFile.toString()) + changes,
                { _, _ -> clients++; throw AssertionError("Denied publication-test client") },
                { _, _, _ -> clients++; throw AssertionError("Denied publication-test model") }, clock = { now })
            for (action in listOf("inspect", "prepare", "publish")) {
                fails { runBlocking { command.run(arrayOf(action, output.toString())) } }
            }
        }
        assertEquals(0, clients)
        assertFalse(Files.exists(output))
    }

    @Test fun readOnlyPreviewStillCannotPublishWithEveryPublicationFlagEnabled() = runBlocking {
        val eventFile = temporary.newFile("readonly-event.json").toPath()
        Files.write(eventFile, event().toByteArray(Charsets.UTF_8))
        val command = Command(publicationEnvironment + ("GITHUB_EVENT_PATH" to eventFile.toString()),
            { _, _ -> throw AssertionError("Read-only preview publisher") })
        fails { runBlocking { command.run(arrayOf("publish", temporary.root.toPath().resolve("missing.json").toString())) } }
    }

    @Test fun disabledPreviewRequiresNeitherEventNorCredentials() = runBlocking {
        val output = temporary.root.toPath().resolve("proposal.json")
        val command = Command(mapOf("GITHUB_EVENT_NAME" to "pull_request"),
            { _, _ -> throw AssertionError("Disabled preview API client") })
        assertEquals("disabled", command.run(arrayOf("prepare", output.toString()))["status"])
        assertFalse(Files.exists(output))
    }

    @Test fun workflowKeepsExactHeadCheckoutReadOnlyPreviewAndSeparatelyApprovedPublicationTest() {
        val workflow = String(Files.readAllBytes(Paths.get("..", "workflows", "review-learning.yml")), Charsets.UTF_8)
        val preparation = workflow.substringAfter("\n  prepare:\n").substringBefore("\n  publish:\n")
        val publishing = workflow.substringAfter("\n  publish:\n")
        val verification = workflow.substringAfter("\n  verify:\n").substringBefore("\n  prepare:\n")
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
        assertFalse(preparation.contains("REVIEW_LEARNING_PUBLISH_APP_PRIVATE_KEY"))
        assertFalse(preparation.contains("contents: write"))
        assertFalse(preparation.contains("pull-requests: write"))
        assertTrue(preparation.contains("copilot-requests: write"))
        for (gate in listOf("github.event.action == 'labeled'", "github.event.label.name == '$PUBLISH_TEST_LABEL'",
            "github.event.pull_request.head.repo.full_name == github.repository",
            "github.event.pull_request.base.repo.full_name == github.repository",
            "github.event.pull_request.base.ref == 'dev'", "github.event.pull_request.state == 'open'",
            "github.event.sender.type == 'User'", "vars.REVIEW_LEARNING_PUBLISH_TEST_SHA != ''",
            "github.event.pull_request.head.sha == vars.REVIEW_LEARNING_PUBLISH_TEST_SHA",
            "contains(fromJSON(vars.REVIEW_LEARNING_POLICY).ownerIds, github.event.sender.id)",
            "!contains(fromJSON(vars.REVIEW_LEARNING_POLICY).automationIds, github.event.sender.id)",
            "vars.REVIEW_LEARNING_PUBLISH_APP_ID != ''",
            "vars.REVIEW_LEARNING_PUBLISH_BOT_ID != ''",
            "contains(fromJSON(vars.REVIEW_LEARNING_POLICY).automationIds, fromJSON(vars.REVIEW_LEARNING_PUBLISH_BOT_ID))",
            "vars.REVIEW_LEARNING_PUBLISH_APPROVED == 'true'",
            "vars.REVIEW_LEARNING_PROTECTION_VERIFIED == 'true'")) {
            assertTrue("Missing preparation test gate: $gate", preparation.substringBefore("    runs-on:").contains(gate))
            assertTrue("Missing publication test gate: $gate", publishing.substringBefore("    runs-on:").contains(gate))
        }
        for (gate in listOf("github.event_name == 'workflow_dispatch'", "github.ref == 'refs/heads/dev'",
            "inputs.mode == 'propose'", "vars.REVIEW_LEARNING_PUBLISH_APPROVED == 'true'",
            "vars.REVIEW_LEARNING_PROTECTION_VERIFIED == 'true'",
            "vars.REVIEW_LEARNING_PUBLISH_APP_ID != ''",
            "vars.REVIEW_LEARNING_PUBLISH_BOT_ID != ''")) {
            assertTrue("Missing publication gate: $gate", publishing.substringBefore("    runs-on:").contains(gate))
        }
        assertTrue(publishing.contains("actions/create-github-app-token@fee1f7d63c2ff003460e3d139729b119787bc349"))
        assertTrue(publishing.contains("app-id: \${{ vars.REVIEW_LEARNING_PUBLISH_APP_ID }}"))
        assertTrue(publishing.contains("private-key: \${{ secrets.REVIEW_LEARNING_PUBLISH_APP_PRIVATE_KEY }}"))
        assertTrue(publishing.contains("permission-contents: write"))
        assertTrue(publishing.contains("permission-pull-requests: write"))
        assertFalse(publishing.contains("administration"))
        assertFalse(publishing.contains("copilot-requests: write"))
        assertFalse(publishing.contains("\n      contents: write"))
        assertFalse(publishing.contains("\n      pull-requests: write"))
        assertTrue(publishing.contains("GITHUB_TOKEN: \${{ steps.app-token.outputs.token }}"))
        assertFalse(publishing.contains("github.event.label.name == '$PREVIEW_LABEL'"))
        val checkout = "ref: \${{ github.event_name == 'pull_request' && github.event.pull_request.head.sha || github.sha }}"
        assertTrue(publishing.contains(checkout))
        assertTrue(verification.contains(checkout))
        assertFalse(verification.contains("contents: write"))
    }
}
