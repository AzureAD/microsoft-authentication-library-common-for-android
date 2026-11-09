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

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.time.Instant
import java.util.Base64
import java.util.concurrent.TimeUnit
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

internal data class Response(val status: Int, val body: String)
internal fun interface Transport {
    suspend fun request(method: String, path: String, body: String?): Response
}

internal class HttpTransport(
    private val policy: Policy,
    private val token: String,
    private val calls: Call.Factory = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).callTimeout(60, TimeUnit.SECONDS).build()
) : Transport {
    override suspend fun request(method: String, path: String, body: String?): Response = withContext(Dispatchers.IO) {
        ensure(path.startsWith("/") && !path.startsWith("//") && path.none { it.code < 32 }, "Invalid API path")
        val url = policy.api + path
        ensure(method in setOf("GET", "POST", "PATCH"), "Unsupported API method")
        val request = Request.Builder().url(url).header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json").header("X-GitHub-Api-Version", "2022-11-28")
            .method(method, body?.toRequestBody("application/json".toMediaType())).build()
        calls.newCall(request).execute().use { response ->
            val status = response.code
            if (status !in 200..299) return@withContext Response(status, "")
            val stream = response.body?.byteStream() ?: throw LearningFailure("Missing API response body")
            val text = stream.bufferedReader(Charsets.UTF_8).use { reader ->
                val result = StringBuilder()
                val buffer = CharArray(8192)
                while (true) {
                    val count = reader.read(buffer)
                    if (count == -1) break
                    ensure(result.length + count <= 2_000_000, "API response exceeds the limit")
                    result.append(buffer, 0, count)
                }
                result.toString()
            }
            Response(status, text)
        }
    }
}

internal interface Repository {
    val policy: Policy
    suspend fun baseSha(): String
    suspend fun recentPulls(since: Instant): List<Pull>
    suspend fun pull(number: Long): Pull
    suspend fun openLearningPulls(): List<Pull>
    suspend fun changedPaths(number: Long): Set<String>
    suspend fun comments(number: Long): List<Comment>
    suspend fun comment(kind: String, id: Long, pr: Long): Comment?
    suspend fun source(path: String, ref: String): String?
    suspend fun branchSha(branch: String): String?
    suspend fun commit(parent: String, files: Map<String, String>, message: String): String
    suspend fun matchesCommit(commit: String, parent: String, files: Map<String, String>): Boolean
    suspend fun updateBranch(branch: String, previous: String?, next: String)
    suspend fun createDraft(branch: String, body: String): Long
    suspend fun verifyPublicationPolicy()
}

internal class GitHub(override val policy: Policy, private val transport: Transport, private val publisherAppId: Long? = null) : Repository {
    private val repo = "/repos/${policy.repository}"
    private fun encoded(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private suspend fun request(method: String, path: String, value: Any? = null, missing: Boolean = false): JsonElement? {
        val response = transport.request(method, path, value?.let(Json::encode))
        if (missing && response.status == 404) return null
        ensure(response.status in 200..299, "GitHub API failed: HTTP ${response.status}")
        return if (response.body.isBlank()) JsonObject() else Json.parse(response.body)
    }

    private suspend fun pages(path: String): List<JsonObject> {
        val result = mutableListOf<JsonObject>()
        for (page in 1..policy.maxPages) {
            val separator = if ('?' in path) "&" else "?"
            val response = request("GET", "$path${separator}per_page=100&page=$page")
                ?: throw LearningFailure("Missing API response")
            ensure(response.isJsonArray, "Expected paginated API records")
            val items = response.asJsonArray.map { it.obj() }
            result.addAll(items)
            if (items.size < 100) return result
        }
        throw LearningFailure("Pagination bound reached; extraction is incomplete")
    }

    private fun actor(value: JsonObject?): Actor =
        if (value == null) Actor(0, "Unknown") else Actor(value.number("id"), value.text("type"))
    private fun optionalText(value: JsonObject, name: String): String? =
        value.get(name)?.takeUnless { it.isJsonNull }?.let {
            ensure(it.isJsonPrimitive && it.asJsonPrimitive.isString, "Invalid API text")
            it.asString
        }

    private fun optionalLine(value: JsonObject, name: String): Int? =
        value.get(name)?.takeUnless { it.isJsonNull }?.let {
            val number = value.number(name)
            ensure(number in 1..Int.MAX_VALUE.toLong(), "Invalid source line")
            number.toInt()
        }

    private fun parsePull(value: JsonObject): Pull = Pull(
        value.number("number"), actor(value.get("user")?.takeIf { it.isJsonObject }?.obj()), value.getAsJsonObject("base").text("ref"),
        value.getAsJsonObject("head").text("ref"), Instant.parse(value.text("updated_at")),
        optionalText(value, "merged_at") != null, value.text("state") == "open",
        value.getAsJsonObject("head").text("sha"), value.text("html_url"),
        optionalText(value, "closed_at")?.let(Instant::parse))

    override suspend fun baseSha(): String = branchSha(policy.base) ?: throw LearningFailure("Missing target branch")

    override suspend fun recentPulls(since: Instant): List<Pull> {
        val result = mutableListOf<Pull>()
        for (page in 1..policy.maxPages) {
            val response = request("GET", "$repo/pulls?state=closed&sort=updated&direction=desc&per_page=100&page=$page")
                ?: throw LearningFailure("Missing pull response")
            ensure(response.isJsonArray, "Expected pull records")
            val pulls = response.asJsonArray.map { parsePull(it.obj()) }
            result.addAll(pulls.takeWhile { !it.updated.isBefore(since) })
            if (pulls.size < 100 || pulls.any { it.updated.isBefore(since) }) return result
        }
        throw LearningFailure("Pull pagination bound reached; extraction is incomplete")
    }

    override suspend fun pull(number: Long): Pull =
        parsePull(request("GET", "$repo/pulls/$number")!!.obj())

    override suspend fun openLearningPulls(): List<Pull> =
        pages("$repo/pulls?state=open&base=${encoded(policy.base)}").map(::parsePull)
            .filter { it.head.startsWith(policy.proposalPrefix) }

    override suspend fun changedPaths(number: Long): Set<String> =
        pages("$repo/pulls/$number/files").map { it.text("filename") }.toSet()

    private fun parseComment(value: JsonObject, kind: String, pr: Long): Comment {
        val rightSide = optionalText(value, "side") != "LEFT"
        return Comment(value.number("id"), kind, pr, actor(value.get("user")?.takeIf { it.isJsonObject }?.obj()),
            optionalText(value, "body") ?: "", value.text("html_url"),
            optionalText(value, "path"),
            optionalText(value, "original_commit_id"),
            if (rightSide) optionalLine(value, "original_line") else null,
            value.get("in_reply_to_id")?.takeUnless { it.isJsonNull }?.let { value.number("in_reply_to_id") },
            automated = value.get("performed_via_github_app")?.let { !it.isJsonNull } ?: false,
            reviewState = if (kind == "review") optionalText(value, "state") else null,
            reviewCommit = if (kind == "review") optionalText(value, "commit_id") else null,
            reviewedAt = if (kind == "review") optionalText(value, "submitted_at") else null)
    }

    override suspend fun comments(number: Long): List<Comment> {
        val inline = pages("$repo/pulls/$number/comments").map { parseComment(it, "inline", number) }
        val states = threadStates(number)
        return inline.map { it.copy(threadResolved = states[it.rootId ?: it.id]) } +
            pages("$repo/issues/$number/comments").map { parseComment(it, "issue", number) } +
            pages("$repo/pulls/$number/reviews").map { parseComment(it, "review", number) }
    }

    private suspend fun threadStates(number: Long): Map<Long, Boolean> {
        val query = """query(${'$'}owner:String!,${'$'}name:String!,${'$'}number:Int!,${'$'}after:String){
            repository(owner:${'$'}owner,name:${'$'}name){pullRequest(number:${'$'}number){
            reviewThreads(first:100,after:${'$'}after){pageInfo{hasNextPage endCursor}
            nodes{isResolved comments(first:1){nodes{databaseId}}}}}}}"""
        val result = mutableMapOf<Long, Boolean>()
        var cursor: String? = null
        for (page in 1..policy.maxPages) {
            val response = request("POST", "/graphql", mapOf("query" to query, "variables" to mapOf(
                "owner" to policy.repository.substringBefore('/'), "name" to policy.repository.substringAfter('/'),
                "number" to number, "after" to cursor)))!!.obj()
            ensure(!response.has("errors"), "GraphQL context retrieval failed")
            val threads = response.getAsJsonObject("data").getAsJsonObject("repository").getAsJsonObject("pullRequest")
                .getAsJsonObject("reviewThreads")
            for (item in threads.list("nodes")) {
                val thread = item.obj()
                val first = thread.getAsJsonObject("comments").list("nodes").firstOrNull()?.obj()
                if (first != null && !first.get("databaseId").isJsonNull) result[first.number("databaseId")] = thread.flag("isResolved")
            }
            val info = threads.getAsJsonObject("pageInfo")
            if (!info.flag("hasNextPage")) return result
            val next = info.text("endCursor")
            ensure(next.isNotBlank() && next != cursor, "Invalid review-thread cursor")
            cursor = next
        }
        throw LearningFailure("Review-thread pagination bound reached")
    }

    override suspend fun comment(kind: String, id: Long, pr: Long): Comment? {
        val path = when (kind) {
            "inline" -> "$repo/pulls/comments/$id"
            "issue" -> "$repo/issues/comments/$id"
            "review" -> "$repo/pulls/$pr/reviews/$id"
            else -> throw LearningFailure("Invalid comment kind")
        }
        val value = request("GET", path, missing = true)?.obj() ?: return null
        val parent = optionalText(value, if (kind == "inline") "pull_request_url" else "issue_url")
        if (kind != "review") ensure(parent?.substringAfterLast('/') == pr.toString(), "Citation belongs to a different PR")
        return parseComment(value, kind, pr)
    }

    override suspend fun source(path: String, ref: String): String? {
        ensure(safePath(path) && sha(ref), "Source must use a safe path and immutable commit")
        val apiPath = path.split('/').joinToString("/") { encoded(it) }
        val value = request("GET", "$repo/contents/$apiPath?ref=$ref", missing = true)?.obj() ?: return null
        ensure(value.text("type") == "file" && value.text("encoding") == "base64", "Unsupported source object")
        ensure(value.number("size") <= 128_000, "Source file exceeds the limit")
        val decoded = Base64.getDecoder().decode(value.text("content").replace("\n", ""))
        return Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(decoded)).toString()
    }

    override suspend fun branchSha(branch: String): String? {
        val value = request("GET", "$repo/git/ref/heads/${encoded(branch)}", missing = true)?.obj() ?: return null
        val result = value.getAsJsonObject("object").text("sha")
        ensure(sha(result), "Invalid branch commit")
        return result
    }

    override suspend fun commit(parent: String, files: Map<String, String>, message: String): String {
        ensure(sha(parent) && files.isNotEmpty() && files.keys.all(::safePath), "Invalid commit request")
        val parentCommit = request("GET", "$repo/git/commits/$parent")!!.obj()
        val tree = request("POST", "$repo/git/trees", mapOf(
            "base_tree" to parentCommit.getAsJsonObject("tree").text("sha"),
            "tree" to files.map { (path, text) -> mapOf("path" to path, "mode" to "100644", "type" to "blob", "content" to text) }
        ))!!.obj().text("sha")
        return request("POST", "$repo/git/commits", mapOf("message" to message, "tree" to tree, "parents" to listOf(parent)))!!
            .obj().text("sha").also { ensure(sha(it), "Invalid new commit") }
    }

    override suspend fun updateBranch(branch: String, previous: String?, next: String) {
        ensure(branch == policy.stateBranch || branch.startsWith(policy.proposalPrefix), "Protected branch write rejected")
        ensure(branch != policy.base && sha(next), "Invalid branch write")
        ensure(branchSha(branch) == previous, "Learning branch changed concurrently")
        if (previous == null) {
            request("POST", "$repo/git/refs", mapOf("ref" to "refs/heads/$branch", "sha" to next))
        } else {
            request("PATCH", "$repo/git/refs/heads/${encoded(branch)}", mapOf("sha" to next, "force" to false))
        }
    }

    override suspend fun matchesCommit(commit: String, parent: String, files: Map<String, String>): Boolean {
        ensure(sha(commit) && sha(parent), "Invalid proposal history")
        val value = request("GET", "$repo/git/commits/$commit")!!.obj()
        val parents = value.list("parents").map { it.obj().text("sha") }
        if (parents != listOf(parent)) return false
        val comparison = request("GET", "$repo/compare/$parent...$commit")!!.obj()
        if (comparison.number("total_commits") != 1L ||
            comparison.list("files").map { it.obj().text("filename") }.toSet() != files.keys) return false
        return files.all { (path, content) -> source(path, commit) == content }
    }

    override suspend fun createDraft(branch: String, body: String): Long {
        ensure(branch.startsWith(policy.proposalPrefix), "Invalid proposal branch")
        return request("POST", "$repo/pulls", mapOf("title" to "Review guidance learning proposal",
            "head" to branch, "base" to policy.base, "body" to body, "draft" to true))!!.obj().number("number")
    }

    override suspend fun verifyPublicationPolicy() {
        val appId = publisherAppId ?: throw LearningFailure("An owner-approved publishing GitHub App ID is required")
        ensure(appId > 0, "Invalid publishing GitHub App ID")
        val protection = request("GET", "$repo/branches/${encoded(policy.base)}/protection", missing = true)?.obj()
        val reviews = protection?.get("required_pull_request_reviews")?.takeUnless { it.isJsonNull }?.obj()
        if (reviews != null && reviews.number("required_approving_review_count") >= 1 && reviews.flag("dismiss_stale_reviews")) {
            val bypass = reviews.get("bypass_pull_request_allowances")?.takeUnless { it.isJsonNull }?.obj()
            if (bypass?.get("apps")?.isJsonArray == true && bypass.list("apps").none { it.obj().number("id") == appId }) return
        }
        val effective = request("GET", "$repo/rules/branches/${encoded(policy.base)}")!!
        ensure(effective.isJsonArray, "Invalid effective branch rules")
        val candidates = effective.asJsonArray.map { it.obj() }.filter { rule ->
            if (rule.text("type") != "pull_request") false else {
                val parameters = rule.get("parameters")?.takeUnless { it.isJsonNull }?.obj()
                parameters != null && parameters.number("required_approving_review_count") >= 1 &&
                    ((parameters.has("dismiss_stale_reviews_on_push") && parameters.flag("dismiss_stale_reviews_on_push")) ||
                        (parameters.has("require_last_push_approval") && parameters.flag("require_last_push_approval")))
            }
        }
        ensure(candidates.isNotEmpty(), "Fresh PR approval must be enforced by an existing branch rule")
        var redacted = false
        for (rule in candidates) {
            val detail = request("GET", "$repo/rulesets/${rule.number("ruleset_id")}?includes_parents=true")!!.obj()
            ensure(detail.text("enforcement") == "active" && detail.text("target") == "branch", "Branch rule is not active")
            val bypass = detail.get("bypass_actors")
            if (bypass == null || !bypass.isJsonArray) { redacted = true; continue }
            val canBypass = bypass.asJsonArray.any {
                val actor = it.obj()
                actor.text("actor_type") == "Integration" &&
                    (actor.get("actor_id")?.isJsonNull != false || actor.number("actor_id") == appId)
            }
            if (!canBypass) return
        }
        throw LearningFailure(if (redacted) "Ruleset bypass metadata is redacted; owner-approved administration-read access must be verified"
            else "Publishing app can bypass the required PR-approval rules")
    }
}
