// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.identity.internal.testutils.nativeauth.api

import com.google.gson.JsonParseException
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.microsoft.identity.common.nativeauth.ApiConstants
import com.microsoft.identity.internal.test.labapi.ApiClient
import com.microsoft.identity.internal.test.labapi.ApiException
import com.microsoft.identity.internal.test.labapi.JSON
import com.microsoft.identity.internal.test.labapi.Pair
import com.squareup.okhttp.Protocol
import com.squareup.okhttp.Request
import com.squareup.okhttp.ResponseBody
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * Mail.tm inbox helper for Native Auth end-to-end tests.
 */
class TemporaryEmailService internal constructor(
    private val password: String,
    private val api: MailTmApi,
    private val clock: () -> Long,
    private val sleeper: (Long) -> Unit,
    private val pollingDelaysMs: List<Long>
) {
    constructor(password: String) : this(
        password = password,
        api = MailTmApi(),
        clock = { System.currentTimeMillis() },
        sleeper = { Thread.sleep(it) },
        pollingDelaysMs = DEFAULT_POLLING_DELAYS_MS
    )

    private val inboxes = mutableMapOf<String, InboxState>()

    /**
     * Creates and authenticates a random Mail.tm inbox.
     */
    fun createRandomEmailAddress(): String {
        requirePassword()
        val domain = api.getDomains().firstOrNull()
            ?: throw IllegalStateException("Mail.tm did not return an available domain")
        val address = "native-auth-signup-${UUID.randomUUID()}@$domain"
        api.createAccount(address, password)
        inboxes[address] = InboxState(token = api.authenticate(address, password))
        return address
    }

    /**
     * Returns a random, validly formatted address without creating an inbox.
     */
    fun generateRandomUnregisteredEmailAddress(): String {
        return "native-auth-signup-${UUID.randomUUID()}@mail.tm"
    }

    /**
     * Records the time immediately before an operation that sends an OTP.
     */
    fun markCheckpoint(emailAddress: String) {
        inboxes.getOrPut(emailAddress) { InboxState() }.checkpointMillis = clock()
    }

    /**
     * Polls Mail.tm for the first OTP received after the address checkpoint.
     */
    fun retrieveCodeFromInbox(emailAddress: String): String {
        val state = inboxes.getOrPut(emailAddress) { InboxState() }
        val token = state.token ?: authenticate(emailAddress).also { state.token = it }
        val attempts = pollingDelaysMs.size + 1
        var lastException: ApiException? = null

        repeat(attempts) { attempt ->
            try {
                api.getMessages(token)
                    .sortedByDescending(::messageTime)
                    .filter { isAfterCheckpoint(it, state) }
                    .forEach { message ->
                        val otp = extractOtp(api.getMessageSource(token, message.id))
                        if (otp != null) {
                            state.checkpointMillis = messageTime(message)
                            state.consumedMessageIds.add(message.id)
                            return otp
                        }
                    }
            } catch (exception: ApiException) {
                lastException = exception
            }

            if (attempt < pollingDelaysMs.size) {
                sleeper(pollingDelaysMs[attempt])
            }
        }

        throw lastException ?: IllegalStateException("Unable to fetch a new OTP from Mail.tm")
    }

    private fun authenticate(emailAddress: String): String {
        requirePassword()
        return api.authenticate(emailAddress, password)
    }

    private fun requirePassword() {
        if (password.isBlank()) {
            throw IllegalStateException("emailProviderPassword is required for Mail.tm inbox access")
        }
    }

    private fun messageTime(message: MailTmMessage): Long {
        return parseTimestamp(message.createdAt) ?: Long.MIN_VALUE
    }

    private fun isAfterCheckpoint(message: MailTmMessage, state: InboxState): Boolean {
        if (message.id in state.consumedMessageIds) {
            return false
        }
        val timestamp = message.createdAt
        val time = messageTime(message)
        return if (timestamp.contains('.')) {
            time > state.checkpointMillis
        } else {
            time >= state.checkpointMillis / 1000 * 1000
        }
    }

    private fun parseTimestamp(value: String): Long? {
        TIMESTAMP_PATTERNS.forEach { pattern ->
            try {
                return SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(value)?.time
            } catch (_: java.text.ParseException) {
                // Try the next supported Mail.tm timestamp format.
            }
        }
        return null
    }

    private fun extractOtp(source: String): String? {
        val explicitMatch = EXPLICIT_OTP_REGEX.find(source)
        if (explicitMatch != null) {
            return explicitMatch.groupValues[1]
        }
        var body = when {
            "\r\n\r\n" in source -> source.substringAfter("\r\n\r\n")
            "\n\n" in source -> source.substringAfter("\n\n")
            else -> source
        }
        val textPart = TEXT_PART_REGEX.find(body)
        if (textPart != null) {
            body = textPart.groupValues[1]
        }
        return FALLBACK_OTP_REGEX.find(body)?.groupValues?.get(1)
    }

    private data class InboxState(
        var token: String? = null,
        var checkpointMillis: Long = Long.MIN_VALUE,
        val consumedMessageIds: MutableSet<String> = mutableSetOf()
    )

    companion object {
        private val DEFAULT_POLLING_DELAYS_MS = listOf(10_000L, 20_000L, 30_000L, 40_000L)
        private val TIMESTAMP_PATTERNS = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX"
        )
        private val EXPLICIT_OTP_REGEX =
            Regex("Account verification code:\\s*([0-9]+)", RegexOption.IGNORE_CASE)
        private val TEXT_PART_REGEX = Regex(
            "Content-Type:\\s*text/(?:plain|html)[^\\r\\n]*(?:\\r?\\n[^\\r\\n]*)*\\r?\\n\\r?\\n(.*?)(?=\\r?\\n--|$)",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        private val FALLBACK_OTP_REGEX = Regex("(?<![0-9])([0-9]{4,8})(?![0-9])")
    }
}

internal data class MailTmRequest(
    val endpoint: String,
    val method: String,
    val headers: Map<String, String>,
    val body: Any? = null
)

internal interface MailTmTransport {
    fun execute(request: MailTmRequest): String
}

internal class MailTmApi(
    private val transport: MailTmTransport = OkHttpMailTmTransport()
) {
    fun getDomains(): List<String> {
        val type = TypeToken.getParameterized(
            MailTmCollection::class.java,
            MailTmDomain::class.java
        ).type
        val response = parse<MailTmCollection<MailTmDomain>>(
            transport.execute(MailTmRequest("/domains", GET, headers())),
            type,
            "/domains"
        )
        return response.members.orEmpty()
            .filter { it.isActive == true && it.isPrivate == false && !it.domain.isNullOrBlank() }
            .map { it.domain!! }
    }

    fun createAccount(address: String, password: String) {
        transport.execute(
            MailTmRequest(
                endpoint = "/accounts",
                method = POST,
                headers = headers(includeContentType = true),
                body = MailTmCredentials(address, password)
            )
        )
    }

    fun authenticate(address: String, password: String): String {
        val response = parse<MailTmToken>(
            transport.execute(
                MailTmRequest(
                    endpoint = "/token",
                    method = POST,
                    headers = headers(includeContentType = true),
                    body = MailTmCredentials(address, password)
                )
            ),
            MailTmToken::class.java,
            "/token"
        )
        return response.token
            ?: throw ApiException("Mail.tm response for /token is missing required field 'token'")
    }

    fun getMessages(token: String): List<MailTmMessage> {
        val type = TypeToken.getParameterized(
            MailTmCollection::class.java,
            MailTmMessageResponse::class.java
        ).type
        val response = parse<MailTmCollection<MailTmMessageResponse>>(
            transport.execute(MailTmRequest("/messages", GET, headers(token = token))),
            type,
            "/messages"
        )
        return response.members.orEmpty().mapNotNull { message ->
            val id = message.id?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val createdAt = message.createdAt?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            MailTmMessage(id, createdAt, message.updatedAt)
        }
    }

    fun getMessageSource(token: String, messageId: String): String {
        val endpoint = "/sources/$messageId"
        val response = parse<MailTmSource>(
            transport.execute(MailTmRequest(endpoint, GET, headers(token = token))),
            MailTmSource::class.java,
            endpoint
        )
        return response.data
            ?: throw ApiException("Mail.tm response for $endpoint is missing required field 'data'")
    }

    private fun headers(
        includeContentType: Boolean = false,
        token: String? = null
    ): Map<String, String> {
        val headers = linkedMapOf("Accept" to APPLICATION_LD_JSON)
        if (includeContentType) {
            headers["Content-Type"] = APPLICATION_JSON
        }
        if (token != null) {
            headers["Authorization"] = "Bearer $token"
        }
        return headers
    }

    private fun <T> parse(body: String, type: java.lang.reflect.Type, endpoint: String): T {
        return try {
            GSON.fromJson<T>(body, type)
                ?: throw ApiException("Mail.tm response body was empty for $endpoint")
        } catch (exception: JsonParseException) {
            throw ApiException("Failed to parse Mail.tm response for $endpoint", exception, 0, null)
        }
    }
}

private class OkHttpMailTmTransport(
    private val apiClient: ApiClient = ApiClient(ApiConstants.TemporaryMailService.BASE_URL)
) : MailTmTransport {
    init {
        apiClient.getHttpClient().protocols = listOf(Protocol.HTTP_1_1)
    }

    override fun execute(request: MailTmRequest): String {
        var responseBody: ResponseBody? = null
        return try {
            val response = apiClient.getHttpClient().newCall(buildRequest(request)).execute()
            responseBody = response.body()
            val body = responseBody?.string().orEmpty()
            if (!response.isSuccessful) {
                throw ApiException(
                    response.code(),
                    "Mail.tm request to ${request.endpoint} failed with status ${response.code()}"
                )
            }
            body
        } catch (exception: IOException) {
            throw ApiException("Mail.tm request failed for ${request.endpoint}", exception, 0, null)
        } finally {
            responseBody?.close()
        }
    }

    private fun buildRequest(request: MailTmRequest): Request {
        val builder = Request.Builder().url(
            apiClient.buildUrl(
                request.endpoint,
                emptyList<Pair>(),
                emptyList<Pair>()
            )
        )
        request.headers.forEach { (name, value) -> builder.header(name, value) }
        val body = if (request.method == GET) {
            null
        } else {
            apiClient.serialize(request.body, APPLICATION_JSON)
        }
        return builder.method(request.method, body).build()
    }
}

private data class MailTmCollection<T>(
    @SerializedName("hydra:member") val members: List<T>?
)

private data class MailTmDomain(
    val domain: String?,
    val isActive: Boolean?,
    val isPrivate: Boolean?
)

private data class MailTmCredentials(
    val address: String,
    val password: String
)

private data class MailTmToken(val token: String?)

private data class MailTmMessageResponse(
    val id: String?,
    val createdAt: String?,
    val updatedAt: String?
)

internal data class MailTmMessage(
    val id: String,
    val createdAt: String,
    val updatedAt: String?
)

private data class MailTmSource(val data: String?)

private const val APPLICATION_JSON = "application/json"
private const val APPLICATION_LD_JSON = "application/ld+json"
private const val GET = "GET"
private const val POST = "POST"
private val GSON = JSON.createGson().create()
