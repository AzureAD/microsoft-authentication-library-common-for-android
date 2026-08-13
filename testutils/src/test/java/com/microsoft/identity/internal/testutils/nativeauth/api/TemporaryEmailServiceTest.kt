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

import com.microsoft.identity.internal.test.labapi.ApiClient
import com.microsoft.identity.internal.test.labapi.ApiException
import com.squareup.okhttp.Protocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.net.ServerSocket
import java.util.ArrayDeque
import java.util.Locale
import java.util.TimeZone

class TemporaryEmailServiceTest {

    @Test
    fun okHttpTransport_forcesHttp11ForMailTmCompatibility() {
        val apiClient = ApiClient("https://api.mail.tm")

        createOkHttpTransport(apiClient)

        assertEquals(listOf(Protocol.HTTP_1_1), apiClient.getHttpClient().protocols)
    }

    @Test
    fun retrieveCodeFromInbox_requestsHydraJsonLdAndPostsJsonCredentials() {
        val transport = FakeMailTmTransport(
            """{"token":"mail-tm-token"}""",
            messages("message-1" to "2026-08-12T20:00:00.000Z"),
            source("Account verification code: 123456")
        )

        createService(transport).retrieveCodeFromInbox(ADDRESS)

        assertEquals("application/ld+json", transport.requests[0].headers["Accept"])
        assertEquals("application/json", transport.requests[0].headers["Content-Type"])
        assertEquals("application/ld+json", transport.requests[1].headers["Accept"])
        assertEquals(null, transport.requests[1].headers["Content-Type"])
    }

    @Test
    fun retrieveCodeFromInbox_authenticatesAndUsesBearerHeader() {
        val transport = FakeMailTmTransport(
            """{"token":"mail-tm-token"}""",
            messages("message-1" to "2026-08-12T20:00:00.000Z"),
            source("Account verification code:\r\n123456\r\n")
        )
        val service = createService(transport)

        val code = service.retrieveCodeFromInbox(ADDRESS)

        assertEquals("123456", code)
        assertEquals("/token", transport.requests[0].endpoint)
        assertEquals(null, transport.requests[0].headers["Authorization"])
        assertEquals("Bearer mail-tm-token", transport.requests[1].headers["Authorization"])
        assertEquals("Bearer mail-tm-token", transport.requests[2].headers["Authorization"])
    }

    @Test
    fun retrieveCodeFromInbox_checkpointIgnoresStaleMessages() {
        val transport = FakeMailTmTransport(
            """{"token":"mail-tm-token"}""",
            messages(
                "stale-message" to "2026-08-12T19:59:59.000Z",
                "fresh-message" to "2026-08-12T20:00:01.000Z"
            ),
            source("Account verification code: 654321")
        )
        val service = createService(
            transport = transport,
            clock = { timestamp("2026-08-12T20:00:00.000Z") }
        )
        service.markCheckpoint(ADDRESS)

        assertEquals("654321", service.retrieveCodeFromInbox(ADDRESS))
        assertTrue(transport.requests.any { it.endpoint == "/sources/fresh-message" })
        assertTrue(transport.requests.none { it.endpoint == "/sources/stale-message" })
    }

    @Test
    fun retrieveCodeFromInbox_afterResendReturnsOnlyNewerCode() {
        var now = timestamp("2026-08-12T19:59:59.000Z")
        val transport = FakeMailTmTransport(
            """{"token":"mail-tm-token"}""",
            messages("message-1" to "2026-08-12T20:00:00.000Z"),
            source("Account verification code: 111111"),
            messages(
                "message-2" to "2026-08-12T20:00:02.000Z",
                "message-1" to "2026-08-12T20:00:00.000Z"
            ),
            source("Account verification code: 222222")
        )
        val service = createService(transport = transport, clock = { now })

        service.markCheckpoint(ADDRESS)
        assertEquals("111111", service.retrieveCodeFromInbox(ADDRESS))

        now = timestamp("2026-08-12T20:00:01.000Z")
        service.markCheckpoint(ADDRESS)
        assertEquals("222222", service.retrieveCodeFromInbox(ADDRESS))
        assertEquals(1, transport.requests.count { it.endpoint == "/sources/message-1" })
    }

    @Test
    fun retrieveCodeFromInbox_acceptsWholeSecondTimestampWithinCheckpointSecond() {
        val transport = FakeMailTmTransport(
            """{"token":"mail-tm-token"}""",
            messages("message-1" to "2026-08-12T20:00:00Z"),
            source("Account verification code: 333333")
        )
        val service = createService(
            transport = transport,
            clock = { timestamp("2026-08-12T20:00:00.500Z") }
        )
        service.markCheckpoint(ADDRESS)

        assertEquals("333333", service.retrieveCodeFromInbox(ADDRESS))
    }

    @Test
    fun retrieveCodeFromInbox_extractsStandaloneOtp() {
        val transport = FakeMailTmTransport(
            """{"token":"mail-tm-token"}""",
            messages("message-1" to "2026-08-12T20:00:00Z"),
            source(
                "Date: Wed, 12 Aug 2026 20:00:00 +0000\r\n" +
                    "Subject: Verification\r\n\r\nUse 87654321 to continue."
            )
        )

        assertEquals("87654321", createService(transport).retrieveCodeFromInbox(ADDRESS))
    }

    @Test
    fun retrieveCodeFromInbox_extractsOtpFromMultipartTextPart() {
        val transport = FakeMailTmTransport(
            """{"token":"mail-tm-token"}""",
            messages("message-1" to "2026-08-12T20:00:00Z"),
            source(
                "Content-Type: multipart/alternative; boundary=\"----=_NextPart_000_0000_01DA1234.5678\"\r\n\r\n" +
                    "------=_NextPart_000_0000_01DA1234.5678\r\n" +
                    "Content-Type: text/plain; charset=utf-8\r\n\r\n" +
                    "Use 87654321 to continue.\r\n"
            )
        )

        assertEquals("87654321", createService(transport).retrieveCodeFromInbox(ADDRESS))
    }

    @Test
    fun retrieveCodeFromInbox_retriesTransientProviderFailure() {
        val transport = FakeMailTmTransport(
            """{"token":"mail-tm-token"}""",
            ApiException("Temporary Mail.tm failure"),
            messages("message-1" to "2026-08-12T20:00:00.000Z"),
            source("Account verification code: 444444")
        )
        val service = createService(
            transport = transport,
            pollingDelaysMs = listOf(10L)
        )

        assertEquals("444444", service.retrieveCodeFromInbox(ADDRESS))
        assertEquals(2, transport.requests.count { it.endpoint == "/messages" })
    }

    @Test
    fun retrieveCodeFromInbox_retriesNormalizedTransportIOException() {
        val unavailablePort = ServerSocket(0).use { it.localPort }
        val transport = IOExceptionThenSuccessTransport(
            failingTransport = createOkHttpTransport("http://127.0.0.1:$unavailablePort"),
            """{"token":"mail-tm-token"}""",
            messages("message-1" to "2026-08-12T20:00:00.000Z"),
            source("Account verification code: 555555")
        )
        val service = createService(
            transport = transport,
            pollingDelaysMs = listOf(0L)
        )

        assertEquals("555555", service.retrieveCodeFromInbox(ADDRESS))
        assertEquals(2, transport.requests.count { it.endpoint == "/messages" })
    }

    @Test
    fun retrieveCodeFromInbox_stopsAfterBoundedProgressivePolling() {
        val delays = mutableListOf<Long>()
        val transport = FakeMailTmTransport(
            """{"token":"mail-tm-token"}""",
            messages(),
            messages(),
            messages()
        )
        val service = createService(
            transport = transport,
            sleeper = { delays.add(it) },
            pollingDelaysMs = listOf(10L, 20L)
        )

        assertThrows(IllegalStateException::class.java) {
            service.retrieveCodeFromInbox(ADDRESS)
        }
        assertEquals(listOf(10L, 20L), delays)
        assertEquals(3, transport.requests.count { it.endpoint == "/messages" })
    }

    private fun createService(
        transport: MailTmTransport,
        clock: () -> Long = { 0L },
        sleeper: (Long) -> Unit = {},
        pollingDelaysMs: List<Long> = emptyList()
    ) = TemporaryEmailService(
        password = PASSWORD,
        api = MailTmApi(transport),
        clock = clock,
        sleeper = sleeper,
        pollingDelaysMs = pollingDelaysMs
    )

    private fun createOkHttpTransport(baseUrl: String): MailTmTransport {
        return createOkHttpTransport(ApiClient(baseUrl))
    }

    private fun createOkHttpTransport(apiClient: ApiClient): MailTmTransport {
        val constructor = Class.forName(
            "com.microsoft.identity.internal.testutils.nativeauth.api.OkHttpMailTmTransport"
        ).getDeclaredConstructor(ApiClient::class.java)
        constructor.isAccessible = true
        return constructor.newInstance(apiClient) as MailTmTransport
    }

    private fun messages(vararg messages: Pair<String, String>): String {
        val members = messages.joinToString(",") { (id, createdAt) ->
            """{"id":"$id","createdAt":"$createdAt"}"""
        }
        return """{"hydra:member":[$members]}"""
    }

    private fun source(body: String) = """{"data":${com.google.gson.Gson().toJson(body)}}"""

    private fun timestamp(value: String): Long {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.parse(value)!!.time
    }

    private class FakeMailTmTransport(vararg responses: Any) : MailTmTransport {
        private val responses = ArrayDeque(responses.toList())
        val requests = mutableListOf<MailTmRequest>()

        override fun execute(request: MailTmRequest): String {
            requests.add(request)
            return when (val response = responses.removeFirst()) {
                is Exception -> throw response
                else -> response as String
            }
        }
    }

    private class IOExceptionThenSuccessTransport(
        private val failingTransport: MailTmTransport,
        vararg responses: String
    ) : MailTmTransport {
        private val responses = ArrayDeque(responses.toList())
        private var failureInjected = false
        val requests = mutableListOf<MailTmRequest>()

        override fun execute(request: MailTmRequest): String {
            requests.add(request)
            if (request.endpoint == "/messages" && !failureInjected) {
                failureInjected = true
                return failingTransport.execute(request)
            }
            return responses.removeFirst()
        }
    }

    companion object {
        private const val ADDRESS = "native-auth@mail.tm"
        private const val PASSWORD = "shared-password"
    }
}
