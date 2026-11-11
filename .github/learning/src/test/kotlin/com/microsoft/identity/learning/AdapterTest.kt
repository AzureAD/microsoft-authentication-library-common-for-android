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
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Timeout
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

internal class StubCall(private val value: Request, private val status: Int, private val text: String) : Call {
    override fun request(): Request = value
    override fun execute(): okhttp3.Response = okhttp3.Response.Builder().request(value).protocol(Protocol.HTTP_1_1)
        .code(status).message("fixture").body(text.toResponseBody()).build()
    override fun enqueue(responseCallback: Callback) { throw AssertionError("Unexpected async callback") }
    override fun cancel() { }
    override fun isExecuted(): Boolean = true
    override fun isCanceled(): Boolean = false
    override fun timeout(): Timeout = Timeout.NONE
    override fun clone(): Call = StubCall(value, status, text)
}

internal class AdapterTest {
    private val publicationContext = mapOf(
        "REVIEW_LEARNING_PUBLISH_APPROVED" to "true",
        "REVIEW_LEARNING_PROTECTION_VERIFIED" to "true",
        "GITHUB_ACTIONS" to "true",
        "GITHUB_EVENT_NAME" to "workflow_dispatch",
        "GITHUB_REF" to "refs/heads/dev",
        "GITHUB_SERVER_URL" to "https://github.com"
    )

    private fun pullRecords(count: Int, paddingFrom: Int = 1, olderFrom: Int = Int.MAX_VALUE): List<Map<String, Any?>> =
        (1..count).map { number ->
            mapOf("number" to number, "user" to mapOf("id" to 3, "type" to "User"),
                "base" to mapOf("ref" to "dev"), "head" to mapOf("ref" to "fix-$number", "sha" to "a".repeat(40)),
                "updated_at" to (if (number >= olderFrom) now.minusSeconds(7200) else now).toString(),
                "merged_at" to now.toString(), "state" to "closed", "html_url" to "${policy.web}/pull/$number",
                "body" to if (number >= paddingFrom) "fixture ".repeat(3_200) else "")
        }

    private fun pullTransport(records: List<Map<String, Any?>>, requests: MutableList<Pair<Int, Int>>): HttpTransport =
        HttpTransport(policy, "unused-pagination-fixture", object : Call.Factory {
            override fun newCall(request: Request): Call {
                assertEquals("GET", request.method)
                val size = request.url.queryParameter("per_page")!!.toInt()
                val page = request.url.queryParameter("page")!!.toInt()
                requests.add(size to page)
                return StubCall(request, 200, Json.encode(records.drop((page - 1) * size).take(size)))
            }
        })

    @Test fun oversizedPullPagesAreReducedWithoutSkippingOrDuplicatingRecords() = runBlocking {
        val requests = mutableListOf<Pair<Int, Int>>()
        val records = pullRecords(220, paddingFrom = 101)
        assertTrue(Json.encode(records.take(100)).length < 2_000_000)
        assertTrue(Json.encode(records.drop(100).take(100)).length > 2_000_000)
        val github = GitHub(policy, pullTransport(records, requests))
        val result = github.recentPulls(now.minusSeconds(3600))
        assertEquals((1L..220L).toList(), result.map { it.number })
        assertEquals(listOf(100 to 1, 100 to 2, 50 to 3, 50 to 4, 50 to 5), requests)
    }

    @Test fun oversizedFirstPullPageStillHonorsLookbackAndSuccessfulPageBudget() = runBlocking {
        val requests = mutableListOf<Pair<Int, Int>>()
        val records = pullRecords(180, olderFrom = 131)
        val github = GitHub(policy.copy(maxPages = 3), pullTransport(records, requests))
        val result = github.recentPulls(now.minusSeconds(3600))
        assertEquals((1L..130L).toList(), result.map { it.number })
        assertEquals(listOf(100 to 1, 50 to 1, 50 to 2, 50 to 3), requests)
    }

    @Test fun singleOversizedRecordFailsClosedAfterFinitePageReductions() = runBlocking {
        val requests = mutableListOf<Pair<Int, Int>>()
        val records = listOf(pullRecords(1).single() + ("body" to "fixture ".repeat(260_000)))
        val github = GitHub(policy.copy(maxPages = 1), pullTransport(records, requests))
        fails { runBlocking { github.recentPulls(now.minusSeconds(3600)) } }
        assertEquals(listOf(100 to 1, 50 to 1, 25 to 1, 5 to 1, 1 to 1), requests)
    }

    @Test fun authenticationFailuresAreNotRetriedAsOversizedPages() = runBlocking {
        var requests = 0
        val factory = object : Call.Factory {
            override fun newCall(request: Request): Call {
                requests++
                return StubCall(request, 403, "private diagnostic")
            }
        }
        val github = GitHub(policy, HttpTransport(policy, "unused", factory))
        fails { runBlocking { github.recentPulls(now.minusSeconds(3600)) } }
        assertEquals(1, requests)
    }

    @Test fun sourceLimitsStayEnforcedForDeclaredDecodedAndStreamedSizes() = runBlocking {
        fun source(size: Int, content: String): String = Json.encode(mapOf("type" to "file", "encoding" to "base64",
            "size" to size, "content" to java.util.Base64.getEncoder().encodeToString(content.toByteArray(Charsets.UTF_8))))
        var response = source(128_000, "x".repeat(128_000))
        val factory = object : Call.Factory {
            override fun newCall(request: Request): Call = StubCall(request, 200, response)
        }
        val github = GitHub(policy, HttpTransport(policy, "unused", factory))
        assertEquals(128_000, github.source("src/Fixture.kt", "a".repeat(40))!!.length)
        for (oversized in listOf(source(128_001, ""), source(1, "x".repeat(128_001)), "x".repeat(2_000_001))) {
            response = oversized
            assertThrows(SourceTooLarge::class.java) {
                runBlocking { github.source("src/Fixture.kt", "a".repeat(40)) }
            }
        }
    }

    @Test fun httpAdapterSupportsPatchAndNeverChangesCredentialOrigin() = runBlocking {
        val requests = mutableListOf<Request>()
        val factory = object : Call.Factory {
            override fun newCall(request: Request): Call {
                requests.add(request)
                return StubCall(request, 200, "{}")
            }
        }
        val transport = HttpTransport(policy, "unused-fixture-token", factory)
        assertEquals(200, transport.request("PATCH", "/repos/example/repo/git/refs/heads/topic", "{}").status)
        assertEquals("PATCH", requests.single().method)
        assertEquals("api.github.com", requests.single().url.host)
        assertEquals("Bearer unused-fixture-token", requests.single().header("Authorization"))
        assertEquals("application/vnd.github+json", requests.single().header("Accept"))
        val buffer = okio.Buffer()
        requests.single().body!!.writeTo(buffer)
        assertEquals("{}", buffer.readUtf8())
        HttpTransport(policy.copy(host = "msft.ghe.com"), "unused-fixture-token", factory)
            .request("POST", "/graphql", "{}")
        assertEquals("api.msft.ghe.com", requests.last().url.host)
        assertEquals("/graphql", requests.last().url.encodedPath)
        fails { runBlocking { transport.request("GET", "//other-host/path", null) } }
        fails { runBlocking { transport.request("DELETE", "/repos/example", null) } }
        assertEquals(2, requests.size)
    }

    @Test fun httpAdapterDoesNotExposeErrorBodiesAndBoundsSuccessfulBodies() = runBlocking {
        var code = 403
        var body = "private diagnostic"
        val factory = object : Call.Factory {
            override fun newCall(request: Request): Call = StubCall(request, code, body)
        }
        val transport = HttpTransport(policy, "unused", factory)
        assertEquals("", transport.request("GET", "/repos/example", null).body)
        code = 200
        body = "x".repeat(2_000_001)
        fails { runBlocking { transport.request("GET", "/repos/example", null) } }
    }

    @Test fun gitApiCreatesOnlyFastForwardLearningRefsAndDrafts() = runBlocking {
        val parent = "a".repeat(40)
        val tree = "b".repeat(40)
        val next = "c".repeat(40)
        val requests = mutableListOf<Triple<String, String, String?>>()
        var exists = false
        val github = GitHub(policy, Transport { method, path, body ->
            requests.add(Triple(method, path, body))
            when {
                method == "GET" && path.contains("/git/ref/") -> if (exists) Response(200, """{"object":{"sha":"$parent"}}""") else Response(404, "")
                method == "GET" && path.contains("/git/commits/") -> Response(200, """{"tree":{"sha":"$tree"}}""")
                method == "POST" && path.endsWith("/git/trees") -> Response(200, """{"sha":"$tree"}""")
                method == "POST" && path.endsWith("/git/commits") -> Response(200, """{"sha":"$next"}""")
                method == "POST" && path.endsWith("/pulls") -> Response(200, """{"number":100}""")
                else -> Response(201, "{}")
            }
        })
        assertEquals(next, github.commit(parent, mapOf(topic to "new guidance"), "message"))
        github.updateBranch(policy.stateBranch, null, next)
        val create = Json.parse(requests.single { it.first == "POST" && it.second.endsWith("/git/refs") }.third!!).obj()
        assertEquals("refs/heads/${policy.stateBranch}", create.text("ref"))
        exists = true
        github.updateBranch(policy.stateBranch, parent, next)
        val update = Json.parse(requests.single { it.first == "PATCH" }.third!!).obj()
        assertFalse(update.flag("force"))
        assertEquals(next, update.text("sha"))
        assertEquals(100L, github.createDraft(policy.proposalPrefix + "fixture", "human-reviewed body"))
        val draft = Json.parse(requests.single { it.second.endsWith("/pulls") }.third!!).obj()
        assertTrue(draft.flag("draft"))
        assertEquals("dev", draft.text("base"))
        assertFalse(draft.text("title").contains("AB#"))
        val count = requests.size
        fails { runBlocking { github.updateBranch("dev", parent, next) } }
        assertEquals(count, requests.size)
    }

    @Test fun actionsPublisherRequiresProtectedBaseWithoutAdministrationRead() = runBlocking {
        val requests = mutableListOf<String>()
        var response = Response(200, """{"name":"dev","protected":true}""")
        val github = GitHub(policy, Transport { method, path, body ->
            assertEquals("GET", method)
            assertNull(body)
            requests.add(path)
            response
        })
        github.verifyPublicationPolicy()
        for (invalid in listOf(
            """{"name":"dev","protected":false}""",
            """{"name":"master","protected":true}""",
            """{"name":"dev"}""",
            """{"name":"dev","protected":"true"}"""
        )) {
            response = Response(200, invalid)
            fails { runBlocking { github.verifyPublicationPolicy() } }
        }
        for (status in listOf(401, 403, 404)) {
            response = Response(status, "private diagnostic")
            fails { runBlocking { github.verifyPublicationPolicy() } }
        }
        assertTrue(requests.all { it == "/repos/${policy.repository}/branches/dev" })
    }

    @Test fun policyAndCheckpointCodecsRejectConflictsOverflowAndInvalidMetadata() {
        val value = mapOf("version" to 1, "host" to policy.host, "repository" to policy.repository, "base" to "dev",
            "humanIds" to policy.humanIds, "ownerIds" to policy.ownerIds, "automationIds" to policy.automationIds,
            "guidancePaths" to policy.guidancePaths)
        assertEquals(policy, Policy.parse(Json.encode(value)))
        fails { Policy.parse(Json.encode(value + ("automationIds" to listOf(1)))) }
        fails { Policy.parse(Json.encode(value + ("lookbackDays" to 4_294_967_326L))) }
        fails { Policy.parse(Json.encode(value + ("host" to "untrusted.example"))) }
        fails { Policy.parse(Json.encode(value + ("guidancePaths" to listOf("src/Production.kt")))) }
        fails { Policy.parse(Json.encode(value + ("base" to "master"))) }
        val state = State(repository = policy.repository, pendingPrs = listOf(1), decisions =
            listOf(Decision("a".repeat(64), "accepted", now.toString())), lastNoProposalFingerprint = "b".repeat(64))
        assertEquals(state, Codec.state(Json.encode(state)))
        fails { Codec.state(Json.encode(state.copy(decisions = listOf(state.decisions.single().copy(recordedAt = "invalid"))))) }
    }

    @Test fun commandInspectAndPrepareWriteOnlySanitizedBundlesAndRejectWrongRepositoryOrPublication() = runBlocking {
        val repository = MemoryRepository().also { it.add(1); it.add(2) }
        val environment = mapOf("REVIEW_LEARNING_POLICY" to Json.encode(mapOf("version" to 1, "host" to policy.host,
            "repository" to policy.repository, "base" to "dev", "humanIds" to policy.humanIds, "ownerIds" to policy.ownerIds,
            "automationIds" to policy.automationIds, "guidancePaths" to policy.guidancePaths)),
            "GITHUB_REPOSITORY" to policy.repository, "GITHUB_TOKEN" to "unused", "REVIEW_LEARNING_MODEL" to "approved",
            "REVIEW_LEARNING_MAX_AI_CREDITS" to "2", "REVIEW_LEARNING_ENABLED" to "true")
        val output = Files.createTempFile("learning-command-fixture-", ".json")
        val githubOutput = Files.createTempFile("learning-output-fixture-", ".txt")
        try {
            val source = packet(repository)
            var calls = 0
            val command = Command(environment + ("GITHUB_OUTPUT" to githubOutput.toString()), { _, _ -> repository },
                { _, _, _ -> Engine { calls++; Json.encode(proposal(source)) } }, clock = { now })
            assertEquals(true, command.run(arrayOf("inspect", output.toString()))["hasSignal"])
            assertEquals(0, calls)
            assertTrue(bundle(String(Files.readAllBytes(output))).proposal.rules.isEmpty())
            assertEquals(1, command.run(arrayOf("prepare", output.toString()))["ruleCount"])
            assertEquals(1, calls)
            val result = String(Files.readAllBytes(output))
            assertFalse(result.contains("actorId"))
            assertFalse(result.contains("codeHash"))
            assertEquals("has_signal=true\nhas_signal=true\n", String(Files.readAllBytes(githubOutput)))
            fails { runBlocking { command.run(arrayOf("publish", output.toString())) } }
            fails { runBlocking { command.run(arrayOf("invalid", output.toString())) } }
            fails { runBlocking { Command(environment + ("GITHUB_REPOSITORY" to "other/repo"), { _, _ -> repository })
                .run(arrayOf("inspect", output.toString())) } }
            fails { runBlocking { Command(mapOf("REVIEW_LEARNING_ENABLED" to "true")).run(arrayOf("inspect", output.toString())) } }
            val publish = Command(environment + publicationContext, { _, _ -> repository }, clock = { now })
            assertEquals("draft_created", publish.run(arrayOf("publish", output.toString()))["status"])
        } finally {
            Files.delete(output)
            Files.delete(githubOutput)
        }
    }

    @Test fun disabledCommandDoesNotInitializeApiModelOrOutputFiles() = runBlocking {
        var clients = 0
        val command = Command(emptyMap(), { _, _ -> clients++; throw AssertionError("Disabled API client") },
            { _, _, _ -> clients++; throw AssertionError("Disabled model client") }, clock = { now })
        val path = Files.createTempFile("disabled-learning-fixture-", ".json")
        try {
            for (action in listOf("inspect", "prepare", "publish")) {
                assertEquals("disabled", command.run(arrayOf(action, path.toString()))["status"])
            }
            assertEquals(0, clients)
            assertEquals(0L, Files.size(path))
        } finally {
            Files.delete(path)
        }
    }

    @Test fun gheCredentialCannotFallThroughToGithubComSynthesis() = runBlocking {
        val memory = MemoryRepository().also { it.add(1); it.add(2) }
        val repository = object : Repository by memory {
            override val policy = com.microsoft.identity.learning.policy.copy(host = "msft.ghe.com")
        }
        val environment = mapOf("REVIEW_LEARNING_POLICY" to Json.encode(mapOf("version" to 1, "host" to "msft.ghe.com",
            "repository" to policy.repository, "base" to "dev", "humanIds" to policy.humanIds, "ownerIds" to policy.ownerIds,
            "automationIds" to policy.automationIds, "guidancePaths" to policy.guidancePaths)),
            "GITHUB_REPOSITORY" to policy.repository, "GITHUB_TOKEN" to "unused-ghe-fixture", "REVIEW_LEARNING_ENABLED" to "true")
        var calls = 0
        val output = Files.createTempFile("ghe-learning-fixture-", ".json")
        try {
            val command = Command(environment, { _, _ -> repository },
                { _, _, _ -> calls++; throw AssertionError("GHE token must not reach this client") }, clock = { now })
            fails { runBlocking { command.run(arrayOf("prepare", output.toString())) } }
            assertEquals(0, calls)
        } finally {
            Files.delete(output)
        }
    }

    @Test fun provenanceRequiresAnExactSingleParentAndChangedFileSet() = runBlocking {
        val parent = "a".repeat(40)
        val commit = "b".repeat(40)
        var extra = false
        var wrongParent = false
        val github = GitHub(policy, Transport { _, path, _ ->
            when {
                path.contains("/git/commits/") -> Response(200, """{"parents":[{"sha":"${if (wrongParent) commit else parent}"}]}""")
                path.contains("/compare/") -> Response(200, Json.encode(mapOf("total_commits" to 1,
                    "files" to (listOf(mapOf("filename" to topic)) + if (extra) listOf(mapOf("filename" to "src/Unexpected.kt")) else emptyList()))))
                else -> Response(200, """{"type":"file","encoding":"base64","size":3,"content":"bmV3"}""")
            }
        })
        assertTrue(github.matchesCommit(commit, parent, mapOf(topic to "new")))
        extra = true
        assertFalse(github.matchesCommit(commit, parent, mapOf(topic to "new")))
        extra = false
        wrongParent = true
        assertFalse(github.matchesCommit(commit, parent, mapOf(topic to "new")))
    }

    @Test fun unverifiedPublisherHostOrBotCannotInitializePublicationApi() = runBlocking {
        var requests = 0
        val transport = Transport { _, _, _ -> requests++; throw AssertionError("Unverified publisher API") }
        for (invalid in listOf(policy.copy(host = "msft.ghe.com"), policy.copy(automationIds = setOf(99)))) {
            fails { runBlocking { GitHub(invalid, transport).verifyPublicationPolicy() } }
        }
        assertEquals(0, requests)
    }

    @Test fun publicationAdmissionRejectsMissingAttestationWrongEventsAndBotRegistryBeforeClientInitialization() = runBlocking {
        val contract = Json.encode(mapOf("version" to 1, "host" to policy.host, "repository" to policy.repository,
            "base" to policy.base, "humanIds" to policy.humanIds, "ownerIds" to policy.ownerIds,
            "automationIds" to policy.automationIds, "guidancePaths" to policy.guidancePaths))
        val environment = mapOf("REVIEW_LEARNING_POLICY" to contract, "GITHUB_REPOSITORY" to policy.repository,
            "REVIEW_LEARNING_ENABLED" to "true", "GITHUB_TOKEN" to "unused") + publicationContext
        val invalid = listOf(
            mapOf("REVIEW_LEARNING_PUBLISH_APPROVED" to ""),
            mapOf("REVIEW_LEARNING_PUBLISH_APPROVED" to "false"),
            mapOf("REVIEW_LEARNING_PROTECTION_VERIFIED" to ""),
            mapOf("REVIEW_LEARNING_PROTECTION_VERIFIED" to "false"),
            mapOf("GITHUB_ACTIONS" to "false"),
            mapOf("GITHUB_EVENT_NAME" to "push"),
            mapOf("GITHUB_EVENT_NAME" to "pull_request"),
            mapOf("GITHUB_EVENT_NAME" to "pull_request_target"),
            mapOf("GITHUB_REF" to "refs/heads/feature"),
            mapOf("GITHUB_SERVER_URL" to "https://msft.ghe.com"),
            mapOf("REVIEW_LEARNING_POLICY" to contract.replace("\"github.com\"", "\"msft.ghe.com\"")),
            mapOf("REVIEW_LEARNING_POLICY" to Json.parse(contract).obj().also {
                it.add("automationIds", Json.parse("[99]"))
            }.toString())
        )
        var clients = 0
        val output = Files.createTempFile("publication-admission-fixture-", ".json")
        try {
            for (changes in invalid) {
                val command = Command(environment + changes,
                    { _, _ -> clients++; throw AssertionError("Denied publication client") })
                fails { runBlocking { command.run(arrayOf("publish", output.toString())) } }
            }
            assertEquals(0, clients)
            assertEquals(0L, Files.size(output))
        } finally {
            Files.delete(output)
        }
    }
}
