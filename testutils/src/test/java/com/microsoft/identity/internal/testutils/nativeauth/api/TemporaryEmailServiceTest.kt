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
import com.squareup.okhttp.MediaType
import com.squareup.okhttp.Protocol
import com.squareup.okhttp.Request
import com.squareup.okhttp.Response
import com.squareup.okhttp.ResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ServerSocket
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TemporaryEmailServiceTest {

    @Test
    fun resolveEmailProviderPassword_whenEnvironmentContainsPassword_returnsPassword() {
        val password = resolveEmailProviderPassword(
            mapOf("EMAIL_PROVIDER_PASSWORD" to PASSWORD)
        )

        assertEquals(PASSWORD, password)
    }

    @Test
    fun resolveEmailProviderPassword_whenEnvironmentDoesNotContainPassword_returnsEmptyString() {
        assertEquals("", resolveEmailProviderPassword(emptyMap()))
    }

    @Test
    fun constructor_withoutPassword_remainsSourceCompatible() {
        val service = TemporaryEmailService()

        assertTrue(service.generateRandomUnregisteredEmailAddress().endsWith("@mail.tm"))
    }

    @Test
    fun generateRandomEmailAddressLocally_createsAuthenticatedInboxForCompatibility() {
        val transport = FakeMailTmTransport(
            """{"hydra:member":[{"domain":"example.mail.tm","isActive":true,"isPrivate":false}]}""",
            "",
            """{"token":"mail-tm-token"}"""
        )

        val address = createService(transport).generateRandomEmailAddressLocally()

        assertTrue(address.endsWith("@example.mail.tm"))
        assertEquals(listOf("/domains", "/accounts", "/token"), transport.requests.map { it.endpoint })
    }

    @Test
    fun okHttpTransport_forcesHttp11ForMailTmCompatibility() {
        val apiClient = ApiClient("https://api.mail.tm")

        createOkHttpTransport(apiClient)

        assertEquals(listOf(Protocol.HTTP_1_1), apiClient.getHttpClient().protocols)
    }

    @Test
    fun createRandomEmailAddress_acrossServicesSharingApi_fetchesDomainsOnce() {
        val transport = FakeMailTmTransport(
            """{"hydra:member":[{"domain":"example.mail.tm","isActive":true,"isPrivate":false}]}""",
            "",
            """{"token":"first-token"}""",
            "",
            """{"token":"second-token"}"""
        )
        val api = MailTmApi(transport)
        fun service() = TemporaryEmailService(PASSWORD, api, { 0L }, {}, emptyList())

        val first = service().createRandomEmailAddress()
        val second = service().createRandomEmailAddress()

        assertTrue(first.endsWith("@example.mail.tm"))
        assertTrue(second.endsWith("@example.mail.tm"))
        assertTrue(first != second)
        assertEquals(
            listOf("/domains", "/accounts", "/token", "/accounts", "/token"),
            transport.requests.map { it.endpoint }
        )
    }

    @Test
    fun getDomains_whenLookupFails_doesNotCacheFailure() {
        val transport = FakeMailTmTransport(
            ApiException(429, "Rate limit exceeded"),
            """{"hydra:member":[{"domain":"example.mail.tm","isActive":true,"isPrivate":false}]}"""
        )
        val api = MailTmApi(transport)

        assertThrows(ApiException::class.java) { api.getDomains() }
        assertEquals(listOf("example.mail.tm"), api.getDomains())
        assertEquals(listOf("example.mail.tm"), api.getDomains())
        assertEquals(2, transport.requests.size)
    }

    @Test
    fun okHttpTransport_whenAccountCreationIsThrottled_retriesRejectedRequest() {
        val apiClient = ApiClient("https://api.mail.tm")
        val statuses = ArrayDeque(listOf(429, 200))
        val requests = mutableListOf<MailTmRequest>()
        val bodies = mutableListOf<String>()
        apiClient.getHttpClient().interceptors().add { chain ->
            requests.add(MailTmRequest(chain.request().url().path, chain.request().method(), emptyMap()))
            bodies.add(Buffer().also { chain.request().body().writeTo(it) }.readUtf8())
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(statuses.removeFirst())
                .message("Test response")
                .header("Retry-After", "0")
                .body(ResponseBody.create(MediaType.parse("application/json"), "{}"))
                .build()
        }
        val transport = createOkHttpTransport(apiClient)

        assertEquals(
            "{}",
            transport.execute(MailTmRequest("/accounts", "POST", emptyMap(), mapOf("address" to ADDRESS)))
        )
        assertEquals(listOf("/accounts", "/accounts"), requests.map { it.endpoint })
        assertEquals(listOf("POST", "POST"), requests.map { it.method })
        assertEquals(listOf("""{"address":"native-auth@mail.tm"}""", """{"address":"native-auth@mail.tm"}"""), bodies)
    }

    @Test
    fun getDomains_whenResponseHasNoAvailableDomains_doesNotCacheIt() {
        listOf(
            """{"hydra:member":[]}""",
            """{"hydra:member":[{"domain":"private.mail.tm","isActive":true,"isPrivate":true}]}""",
            """{"hydra:member":[{"domain":"inactive.mail.tm","isActive":false,"isPrivate":false}]}"""
        ).forEach { invalidResponse ->
            val transport = FakeMailTmTransport(
                invalidResponse,
                """{"hydra:member":[{"domain":"example.mail.tm","isActive":true,"isPrivate":false}]}"""
            )
            val api = MailTmApi(transport)

            assertThrows(IllegalStateException::class.java) { api.getDomains() }
            assertEquals(listOf("example.mail.tm"), api.getDomains())
            assertEquals(listOf("example.mail.tm"), api.getDomains())
            assertEquals(2, transport.requests.size)
        }
    }

    @Test
    fun getDomains_whenResponseIsMalformed_doesNotCacheIt() {
        val transport = FakeMailTmTransport(
            "{",
            """{"hydra:member":[{"domain":"example.mail.tm","isActive":true,"isPrivate":false}]}"""
        )
        val api = MailTmApi(transport)

        assertThrows(ApiException::class.java) { api.getDomains() }
        assertEquals(listOf("example.mail.tm"), api.getDomains())
        assertEquals(listOf("example.mail.tm"), api.getDomains())
        assertEquals(2, transport.requests.size)
    }

    @Test
    fun getDomains_whenCalledConcurrently_initializesCacheOnce() {
        val transport = FakeMailTmTransport(
            """{"hydra:member":[{"domain":"example.mail.tm","isActive":true,"isPrivate":false}]}"""
        )
        val api = MailTmApi(transport)
        val executor = Executors.newFixedThreadPool(4)
        val start = CountDownLatch(1)
        try {
            val lookups = (1..12).map {
                executor.submit(Callable {
                    start.await()
                    api.getDomains()
                })
            }
            start.countDown()
            lookups.forEach { assertEquals(listOf("example.mail.tm"), it.get(10, TimeUnit.SECONDS)) }
            assertEquals(1, transport.requests.size)
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun okHttpTransport_whenRetryAfterIsSeconds_honorsDelayAndHeaderCase() {
        val delays = mutableListOf<Long>()
        val transport = scriptedTransport(429 to " 7 ", 200 to null, delays = delays)

        assertEquals("{}", transport.execute(accountRequest()))
        assertEquals(listOf(7_000L), delays)
    }

    @Test
    fun okHttpTransport_whenRetryAfterIsHttpDate_waitsUntilDate() {
        listOf(
            "Sun, 06 Nov 1994 08:49:37 GMT",
            "Sunday, 06-Nov-94 08:49:37 GMT",
            "Sun Nov  6 08:49:37 1994"
        ).forEach { header ->
            val delays = mutableListOf<Long>()
            val transport = scriptedTransport(
                429 to header, 200 to null,
                delays = delays,
                clock = { timestamp("1994-11-06T08:49:30.000Z") }
            )

            assertEquals("{}", transport.execute(accountRequest()))
            assertEquals(listOf(7_000L), delays)
        }
    }

    @Test
    fun okHttpTransport_whenRetryAfterDateIsPast_doesNotWait() {
        val delays = mutableListOf<Long>()
        val transport = scriptedTransport(
            429 to "Sun, 06 Nov 1994 08:49:37 GMT", 200 to null,
            delays = delays,
            clock = { timestamp("1994-11-06T08:49:38.000Z") }
        )

        assertEquals("{}", transport.execute(MailTmRequest("/domains", "GET", emptyMap())))
        assertEquals(listOf(0L), delays)
    }

    @Test
    fun okHttpTransport_whenRetryAfterIsMissingOrInvalid_usesJitteredBackoff() {
        listOf(null, "", "invalid", "-1", "1.5", "7 trailing", "Sun, 99 Nov 1994 08:49:37 GMT").forEach { header ->
            val delays = mutableListOf<Long>()
            val transport = scriptedTransport(
                429 to header, 200 to null,
                delays = delays,
                jitter = { it / 2 + 123L }
            )

            assertEquals("{}", transport.execute(accountRequest()))
            assertEquals(listOf(2_623L), delays)
        }
    }

    @Test
    fun okHttpTransport_whenThrottlePersists_boundsAttemptsAndExponentialDelays() {
        val delays = mutableListOf<Long>()
        val requests = mutableListOf<Request>()
        val transport = scriptedTransport(
            429 to null, 429 to null, 429 to null, 429 to null,
            delays = delays,
            requests = requests,
            jitter = { it }
        )

        val error = assertThrows(AssertionError::class.java) {
            transport.execute(accountRequest())
        }

        assertEquals(429, (error.cause as ApiException).code)
        assertEquals(listOf(5_000L, 10_000L, 20_000L), delays)
        assertEquals(4, requests.size)
    }

    @Test
    fun okHttpTransport_whenUsingRandomJitter_keepsDelayWithinExponentialWindows() {
        val delays = mutableListOf<Long>()
        val transport = scriptedTransport(
            429 to null, 429 to null, 429 to null, 200 to null,
            delays = delays
        )

        assertEquals("{}", transport.execute(MailTmRequest("/messages", "GET", emptyMap())))
        assertEquals(3, delays.size)
        assertTrue(delays[0] in 2_500L..5_000L)
        assertTrue(delays[1] in 5_000L..10_000L)
        assertTrue(delays[2] in 10_000L..20_000L)
    }

    @Test
    fun okHttpTransport_whenRetryAfterExceedsBudget_failsWithoutRetryingEarly() {
        listOf("61", "9223372036854775807", "999999999999999999999999999999").forEach { header ->
            val delays = mutableListOf<Long>()
            val requests = mutableListOf<Request>()
            val transport = scriptedTransport(
                429 to header,
                delays = delays,
                requests = requests
            )

            val error = assertThrows(AssertionError::class.java) {
                transport.execute(accountRequest())
            }

            val providerError = error.cause as ApiException
            assertEquals(429, providerError.code)
            assertEquals(listOf(header), providerError.responseHeaders.entries.first {
                it.key.equals("Retry-After", ignoreCase = true)
            }.value)
            assertTrue(delays.isEmpty())
            assertEquals(1, requests.size)
        }
    }

    @Test
    fun okHttpTransport_whenCumulativeRetryAfterExceedsBudget_stopsAtBudget() {
        val delays = mutableListOf<Long>()
        val requests = mutableListOf<Request>()
        val transport = scriptedTransport(
            429 to "30", 429 to "30", 429 to "1",
            delays = delays,
            requests = requests
        )

        val error = assertThrows(AssertionError::class.java) {
            transport.execute(accountRequest())
        }

        assertEquals(429, (error.cause as ApiException).code)
        assertEquals(listOf(30_000L, 30_000L), delays)
        assertEquals(3, requests.size)
    }

    @Test
    fun okHttpTransport_whenStatusIsNot429_doesNotRetry() {
        listOf(400, 401, 404, 409, 422, 500, 503).forEach { status ->
            val delays = mutableListOf<Long>()
            val requests = mutableListOf<Request>()
            val transport = scriptedTransport(status to "1", delays = delays, requests = requests)

            val error = assertThrows(ApiException::class.java) {
                transport.execute(accountRequest())
            }

            assertEquals(status, error.code)
            assertTrue(delays.isEmpty())
            assertEquals(1, requests.size)
        }
    }

    @Test
    fun okHttpTransport_whenRetryAfterIsZero_stillBoundsAttempts() {
        val delays = mutableListOf<Long>()
        val requests = mutableListOf<Request>()
        val transport = scriptedTransport(
            429 to "0", 429 to "0", 429 to "0", 429 to "0",
            delays = delays,
            requests = requests
        )

        assertThrows(AssertionError::class.java) { transport.execute(accountRequest()) }
        assertEquals(listOf(0L, 0L, 0L), delays)
        assertEquals(4, requests.size)
    }

    @Test
    fun okHttpTransport_whenConnectionFails_doesNotReplayAmbiguousPost() {
        val failure = IOException("Connection failed")
        val apiClient = ApiClient("https://api.mail.tm")
        var attempts = 0
        apiClient.getHttpClient().interceptors().add {
            attempts++
            throw failure
        }
        val delays = mutableListOf<Long>()
        val transport = OkHttpMailTmTransport(apiClient, sleeper = { delays.add(it) })

        val error = assertThrows(ApiException::class.java) { transport.execute(accountRequest()) }

        assertEquals(0, error.code)
        assertEquals(failure, error.cause)
        assertEquals(1, attempts)
        assertTrue(delays.isEmpty())
    }

    @Test
    fun okHttpTransport_whenSleepIsInterrupted_preservesInterruptAndStops() {
        val requests = mutableListOf<Request>()
        val transport = scriptedTransport(
            429 to "1",
            delays = mutableListOf(),
            requests = requests,
            sleeper = { throw InterruptedException("Test interrupted") }
        )
        try {
            assertThrows(InterruptedException::class.java) { transport.execute(accountRequest()) }
            assertTrue(Thread.currentThread().isInterrupted)
            assertEquals(1, requests.size)
        } finally {
            Thread.interrupted()
        }
    }

    @Test
    fun retrieveCodeFromInbox_whenThrottleBudgetIsExhausted_doesNotRestartPolling() {
        val failure = AssertionError("Mail.tm request to /messages failed with status 429").apply {
            initCause(ApiException(429, message))
        }
        val transport = FakeMailTmTransport("""{"token":"mail-tm-token"}""", failure)
        val delays = mutableListOf<Long>()
        val service = createService(transport, sleeper = { delays.add(it) }, pollingDelaysMs = listOf(10L))

        assertEquals(failure, assertThrows(AssertionError::class.java) {
            service.retrieveCodeFromInbox(ADDRESS)
        })
        assertTrue(delays.isEmpty())
        assertEquals(listOf("/token", "/messages"), transport.requests.map { it.endpoint })
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
            """
                {
                    "hydra:member": [
                        {
                            "id":"stale-message",
                            "createdAt":"2026-08-12T19:59:59.000Z",
                            "updatedAt":"2026-08-12T20:00:03.000Z"
                        },
                        {
                            "id":"fresh-message",
                            "createdAt":"2026-08-12T20:00:01.000Z",
                            "updatedAt":"2026-08-12T20:00:01.000Z"
                        }
                    ]
                }
            """.trimIndent(),
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
    fun retrieveCodeFromInbox_ignoresMalformedMessageEntries() {
        val transport = FakeMailTmTransport(
            """{"token":"mail-tm-token"}""",
            """
                {
                    "hydra:member": [
                        {"createdAt":"2026-08-12T20:00:01.000Z"},
                        {"id":"missing-created-at"},
                        {"id":"message-1","createdAt":"2026-08-12T20:00:00.000Z"}
                    ]
                }
            """.trimIndent(),
            source("Account verification code: 333333")
        )

        assertEquals("333333", createService(transport).retrieveCodeFromInbox(ADDRESS))
        assertTrue(transport.requests.any { it.endpoint == "/sources/message-1" })
    }

    @Test
    fun retrieveCodeFromInbox_whenNewestSourceIsMissing_checksNextEligibleMessage() {
        val transport = FakeMailTmTransport(
            """{"token":"mail-tm-token"}""",
            messages(
                "newest-message" to "2026-08-12T20:00:01.000Z",
                "older-message" to "2026-08-12T20:00:00.000Z"
            ),
            ApiException(404, "Mail.tm source was not found"),
            source("Account verification code: 333333")
        )

        assertEquals("333333", createService(transport).retrieveCodeFromInbox(ADDRESS))
        assertEquals(
            listOf("/sources/newest-message", "/sources/older-message"),
            transport.requests.filter { it.endpoint.startsWith("/sources/") }.map { it.endpoint }
        )
    }

    @Test
    fun retrieveCodeFromInbox_whenNewestSourceIsInvalid_checksNextEligibleMessage() {
        listOf("{", "{}").forEach { invalidSource ->
            val transport = FakeMailTmTransport(
                """{"token":"mail-tm-token"}""",
                messages(
                    "newest-message" to "2026-08-12T20:00:01.000Z",
                    "older-message" to "2026-08-12T20:00:00.000Z"
                ),
                invalidSource,
                source("Account verification code: 333333")
            )

            assertEquals("333333", createService(transport).retrieveCodeFromInbox(ADDRESS))
            assertEquals(
                listOf("/sources/newest-message", "/sources/older-message"),
                transport.requests.filter { it.endpoint.startsWith("/sources/") }.map { it.endpoint }
            )
        }
    }

    @Test
    fun retrieveCodeFromInbox_whenSourceFailureIsSystemic_abortsAttempt() {
        val failures = listOf(
            ApiException(401, "Mail.tm token is invalid"),
            ApiException(429, "Mail.tm rate limit exceeded"),
            ApiException(500, "Mail.tm server failed"),
            ApiException("Mail.tm transport failed", IOException(), 0, null)
        )

        failures.forEach { failure ->
            val transport = FakeMailTmTransport(
                """{"token":"mail-tm-token"}""",
                messages(
                    "newest-message" to "2026-08-12T20:00:01.000Z",
                    "older-message" to "2026-08-12T20:00:00.000Z"
                ),
                failure,
                source("Account verification code: 333333")
            )

            assertEquals(
                failure,
                assertThrows(ApiException::class.java) {
                    createService(transport).retrieveCodeFromInbox(ADDRESS)
                }
            )
            assertEquals(
                listOf("/sources/newest-message"),
                transport.requests.filter { it.endpoint.startsWith("/sources/") }.map { it.endpoint }
            )
        }
    }

    @Test
    fun retrieveCodeFromInbox_whenSourceFailureIsSystemic_retriesAfterPollingDelay() {
        val delays = mutableListOf<Long>()
        val transport = FakeMailTmTransport(
            """{"token":"mail-tm-token"}""",
            messages(
                "newest-message" to "2026-08-12T20:00:01.000Z",
                "older-message" to "2026-08-12T20:00:00.000Z"
            ),
            ApiException(429, "Mail.tm rate limit exceeded"),
            messages("newest-message" to "2026-08-12T20:00:01.000Z"),
            source("Account verification code: 333333")
        )
        val service = createService(
            transport = transport,
            sleeper = { delays.add(it) },
            pollingDelaysMs = listOf(10L)
        )

        assertEquals("333333", service.retrieveCodeFromInbox(ADDRESS))
        assertEquals(listOf(10L), delays)
        assertEquals(
            listOf("/sources/newest-message", "/sources/newest-message"),
            transport.requests.filter { it.endpoint.startsWith("/sources/") }.map { it.endpoint }
        )
    }

    @Test
    fun retrieveCodeFromInbox_whenAllEligibleSourcesFail_throwsLastSourceException() {
        val lastException = ApiException("Mail.tm source is missing required data")
        val transport = FakeMailTmTransport(
            """{"token":"mail-tm-token"}""",
            messages(
                "newest-message" to "2026-08-12T20:00:01.000Z",
                "older-message" to "2026-08-12T20:00:00.000Z"
            ),
            ApiException(404, "Mail.tm source was not found"),
            lastException
        )

        assertEquals(
            lastException,
            assertThrows(ApiException::class.java) {
                createService(transport).retrieveCodeFromInbox(ADDRESS)
            }
        )
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
        return OkHttpMailTmTransport(apiClient)
    }

    private fun accountRequest() = MailTmRequest(
        "/accounts", "POST", emptyMap(),
        mapOf("address" to ADDRESS, "password" to PASSWORD)
    )

    private fun scriptedTransport(
        vararg responses: Pair<Int, String?>,
        delays: MutableList<Long>,
        requests: MutableList<Request> = mutableListOf(),
        clock: () -> Long = { 0L },
        jitter: ((Long) -> Long)? = null,
        sleeper: (Long) -> Unit = { delays.add(it) }
    ): MailTmTransport {
        val queue = ArrayDeque(responses.toList())
        val apiClient = ApiClient("https://api.mail.tm")
        apiClient.getHttpClient().interceptors().add { chain ->
            requests.add(chain.request())
            val (status, retryAfter) = queue.removeFirst()
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(status)
                .message("Test response")
                .body(ResponseBody.create(MediaType.parse("application/json"), "{}"))
                .apply { if (retryAfter != null) header("rEtRy-AfTeR", retryAfter) }
                .build()
        }
        return if (jitter == null) {
            OkHttpMailTmTransport(apiClient, clock, sleeper)
        } else {
            OkHttpMailTmTransport(apiClient, clock, sleeper, jitter)
        }
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
                is Throwable -> throw response
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
