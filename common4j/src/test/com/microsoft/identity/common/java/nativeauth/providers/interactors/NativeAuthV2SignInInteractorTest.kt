//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.java.nativeauth.providers.interactors

import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.NativeAuthV2ChallengeRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.NativeAuthV2EntryRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.NativeAuthV2PasswordVerifyRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.NativeAuthV2VerifyRequest
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.HalResource
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2HalApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2InteractionApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2LinkRelation
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2Operation
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ResponseParser
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2FlowScenario
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2RequestProvider
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2ResponseHandler
import com.microsoft.identity.common.java.net.HttpConstants
import com.microsoft.identity.common.java.net.HttpResponse
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.providers.oauth2.OAuth2RequestInterceptor
import io.mockk.Called
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Covers the V2 sign-in interactor operations: the sign-in entry call, method-driven challenges,
 * password verification (including buffer lifetime on every exit path), and MFA verification.
 */
class NativeAuthV2SignInInteractorTest {

    private val signInUrl = URL("https://contoso.ciamlogin.com/nativeauth/v2/signin/start")
    private val challengeUrl = URL("https://contoso.ciamlogin.com/nativeauth/v2/password/pwd-1/challenge")
    private val passwordVerifyUrl = URL("https://contoso.ciamlogin.com/nativeauth/v2/password/pwd-1/verify")
    private val mfaVerifyUrl = URL("https://contoso.ciamlogin.com/nativeauth/v2/email/email-1/verify")

    private val httpClient = mockk<UrlConnectionHttpClient>()
    private val requestProvider = mockk<NativeAuthV2RequestProvider>()
    private val responseHandler = mockk<NativeAuthV2ResponseHandler>()
    private val responseParser = mockk<NativeAuthV2ResponseParser>()

    private val requestInterceptor = object : OAuth2RequestInterceptor {
        override fun additionalHeaders(requestUrl: URL): Map<String, String> =
            mapOf(INTERCEPTOR_HEADER to INTERCEPTOR_VALUE)
    }

    private fun createInteractor(): NativeAuthV2Interactor = NativeAuthV2Interactor(
        httpClient = httpClient,
        requestProvider = requestProvider,
        responseHandler = responseHandler,
        responseParser = responseParser,
        requestInterceptor = requestInterceptor
    )

    @Test
    fun performSignInStart_postsUsernameAndContinuationTokenAndParsesAsSignInStart() {
        val state = continuationState(NativeAuthV2LinkRelation.SIGN_IN to "/signin/start")
        val request = NativeAuthV2EntryRequest.create(
            clientId = CLIENT_ID,
            username = USERNAME,
            continuationToken = CONTINUATION_TOKEN,
            requestUrl = signInUrl.toString(),
            headers = jsonHeaders()
        )
        val httpResponse = HttpResponse(200, """{"action":"challenge"}""", emptyMap())
        val halResponse = mockk<NativeAuthV2HalApiResponse>(relaxed = true)
        val expected = mockk<NativeAuthV2InteractionApiResult.ChallengeRequired>(relaxed = true)
        val captured = capturePost(httpResponse)

        every { requestProvider.createSignInStartRequest(USERNAME, state) } returns request
        every { responseHandler.getHalApiResponse(CORRELATION_ID, httpResponse) } returns halResponse
        every {
            responseParser.parseInteraction(halResponse, state, NativeAuthV2Operation.SIGN_IN_START)
        } returns expected

        val actual = createInteractor().performSignInStart(USERNAME, state)

        assertSame(expected, actual)
        assertEquals(signInUrl, captured.url.captured)
        assertMergedHeaders(captured.headers.captured)
        assertJsonBody(
            captured.body.captured,
            mapOf("username" to USERNAME, "continuationToken" to CONTINUATION_TOKEN)
        )
    }

    @Test
    fun performPasswordMethodChallenge_followsSelectedMethodHrefAndReportsPasswordChallengeOperation() {
        val state = signInChallengeState()
        val selectedState = state.withSelectedMethod("pwd-1")!!
        val request = NativeAuthV2ChallengeRequest.create(
            continuationToken = CONTINUATION_TOKEN,
            requestUrl = challengeUrl.toString(),
            headers = jsonHeaders()
        )
        val httpResponse = HttpResponse(200, """{"action":"verify"}""", emptyMap())
        val halResponse = mockk<NativeAuthV2HalApiResponse>(relaxed = true)
        val expected = mockk<NativeAuthV2InteractionApiResult.PasswordRequired>(relaxed = true)
        val captured = capturePost(httpResponse)
        val stateSlot = slot<NativeAuthV2ContinuationState>()

        every { requestProvider.createChallengeRequest(capture(stateSlot)) } returns request
        every { responseHandler.getHalApiResponse(CORRELATION_ID, httpResponse) } returns halResponse
        every {
            responseParser.parseInteraction(
                halResponse,
                any(),
                NativeAuthV2Operation.SIGN_IN_PASSWORD_CHALLENGE
            )
        } returns expected

        val actual = createInteractor().performPasswordMethodChallenge(state, "pwd-1")

        assertSame(expected, actual)
        assertEquals(challengeUrl, captured.url.captured)
        // The request is built from the method-selected successor, so the challenge relation now
        // points at the password method's own href.
        assertEquals(
            selectedState.href(NativeAuthV2LinkRelation.CHALLENGE),
            stateSlot.captured.href(NativeAuthV2LinkRelation.CHALLENGE)
        )
    }

    @Test
    fun performMfaMethodChallenge_reportsMfaChallengeOperation() {
        val state = signInChallengeState()
        val request = NativeAuthV2ChallengeRequest.create(
            continuationToken = CONTINUATION_TOKEN,
            requestUrl = challengeUrl.toString(),
            headers = jsonHeaders()
        )
        val httpResponse = HttpResponse(200, """{"action":"verify"}""", emptyMap())
        val halResponse = mockk<NativeAuthV2HalApiResponse>(relaxed = true)
        val expected = mockk<NativeAuthV2InteractionApiResult.CodeRequired>(relaxed = true)
        capturePost(httpResponse)

        every { requestProvider.createChallengeRequest(any()) } returns request
        every { responseHandler.getHalApiResponse(CORRELATION_ID, httpResponse) } returns halResponse
        every {
            responseParser.parseInteraction(
                halResponse,
                any(),
                NativeAuthV2Operation.MFA_METHOD_CHALLENGE
            )
        } returns expected

        assertSame(expected, createInteractor().performMfaMethodChallenge(state, "email-1"))
    }

    @Test
    fun performMethodChallenge_whenMethodWasNotOffered_failsWithoutIssuingARequest() {
        val result = createInteractor().performMfaMethodChallenge(signInChallengeState(), "stale-id")

        assertTrue(result is NativeAuthV2InteractionApiResult.UnknownError)
        verify { httpClient wasNot Called }
        verify { requestProvider wasNot Called }
        verify { responseHandler wasNot Called }
        verify { responseParser wasNot Called }
    }

    @Test
    fun performPasswordVerify_postsPasswordBodyAndClearsBufferOnSuccess() {
        val state = continuationState(NativeAuthV2LinkRelation.VERIFY to "/password/pwd-1/verify")
        val password = PASSWORD.clone()
        val request = NativeAuthV2PasswordVerifyRequest.create(
            continuationToken = CONTINUATION_TOKEN,
            password = password,
            requestUrl = passwordVerifyUrl.toString(),
            headers = jsonHeaders()
        )
        val httpResponse = HttpResponse(200, """{"state":"continue"}""", emptyMap())
        val halResponse = mockk<NativeAuthV2HalApiResponse>(relaxed = true)
        val expected = mockk<NativeAuthV2InteractionApiResult.ReadyToComplete>(relaxed = true)
        val captured = capturePost(httpResponse)

        every { requestProvider.createPasswordVerifyRequest(state, password) } returns request
        every { responseHandler.getHalApiResponse(CORRELATION_ID, httpResponse) } returns halResponse
        every {
            responseParser.parseInteraction(
                halResponse,
                state,
                NativeAuthV2Operation.SIGN_IN_PASSWORD_VERIFY
            )
        } returns expected

        val actual = createInteractor().performPasswordVerify(state, password, deferredSubmission = false)

        assertSame(expected, actual)
        assertEquals(passwordVerifyUrl, captured.url.captured)
        assertMergedHeaders(captured.headers.captured)
        assertJsonBody(
            captured.body.captured,
            mapOf("continuationToken" to CONTINUATION_TOKEN, "password" to String(PASSWORD)),
            secretKeys = setOf("password")
        )
        assertPasswordCleared(password)
    }

    @Test
    fun performPasswordVerify_whenDeferred_reportsSubmitPasswordOperation() {
        val state = continuationState(NativeAuthV2LinkRelation.VERIFY to "/password/pwd-1/verify")
        val password = PASSWORD.clone()
        val request = NativeAuthV2PasswordVerifyRequest.create(
            continuationToken = CONTINUATION_TOKEN,
            password = password,
            requestUrl = passwordVerifyUrl.toString(),
            headers = jsonHeaders()
        )
        val httpResponse = HttpResponse(400, """{"error":{"code":"invalid_grant"}}""", emptyMap())
        val halResponse = mockk<NativeAuthV2HalApiResponse>(relaxed = true)
        val expected = mockk<NativeAuthV2InteractionApiResult.InvalidCredentials>(relaxed = true)
        capturePost(httpResponse)

        every { requestProvider.createPasswordVerifyRequest(state, password) } returns request
        every { responseHandler.getHalApiResponse(CORRELATION_ID, httpResponse) } returns halResponse
        every {
            responseParser.parseInteraction(halResponse, state, NativeAuthV2Operation.SUBMIT_PASSWORD)
        } returns expected

        assertSame(
            expected,
            createInteractor().performPasswordVerify(state, password, deferredSubmission = true)
        )
        verify(exactly = 1) {
            responseParser.parseInteraction(halResponse, state, NativeAuthV2Operation.SUBMIT_PASSWORD)
        }
        assertPasswordCleared(password)
    }

    @Test
    fun performPasswordVerify_clearsPasswordWhenHttpPostFails() {
        val state = continuationState(NativeAuthV2LinkRelation.VERIFY to "/password/pwd-1/verify")
        val password = PASSWORD.clone()
        val request = NativeAuthV2PasswordVerifyRequest.create(
            continuationToken = CONTINUATION_TOKEN,
            password = password,
            requestUrl = passwordVerifyUrl.toString(),
            headers = jsonHeaders()
        )
        val expectedFailure = RuntimeException("HTTP failure")

        every { requestProvider.createPasswordVerifyRequest(state, password) } returns request
        every { httpClient.post(any(), any(), any()) } throws expectedFailure

        try {
            createInteractor().performPasswordVerify(state, password, deferredSubmission = false)
            fail("Expected performPasswordVerify to rethrow the HTTP failure")
        } catch (actual: RuntimeException) {
            assertSame(expectedFailure, actual)
        }

        assertPasswordCleared(password)
    }

    @Test
    fun performPasswordVerify_clearsPasswordWhenRequestCreationFails() {
        val state = continuationState()
        val password = PASSWORD.clone()
        val expectedFailure = RuntimeException("Missing verify relation")

        every { requestProvider.createPasswordVerifyRequest(state, password) } throws expectedFailure

        try {
            createInteractor().performPasswordVerify(state, password, deferredSubmission = true)
            fail("Expected performPasswordVerify to rethrow the request creation failure")
        } catch (actual: RuntimeException) {
            assertSame(expectedFailure, actual)
        }

        assertPasswordCleared(password)
        verify { responseHandler wasNot Called }
        verify { responseParser wasNot Called }
    }

    @Test
    fun performMfaVerify_postsOtpAndReportsMfaVerifyOperation() {
        val state = continuationState(NativeAuthV2LinkRelation.VERIFY to "/email/email-1/verify")
        val request = NativeAuthV2VerifyRequest.create(
            continuationToken = CONTINUATION_TOKEN,
            otp = OTP,
            requestUrl = mfaVerifyUrl.toString(),
            headers = jsonHeaders()
        )
        val httpResponse = HttpResponse(200, """{"state":"continue"}""", emptyMap())
        val halResponse = mockk<NativeAuthV2HalApiResponse>(relaxed = true)
        val expected = mockk<NativeAuthV2InteractionApiResult.ReadyToComplete>(relaxed = true)
        val captured = capturePost(httpResponse)

        every { requestProvider.createVerifyRequest(state, OTP) } returns request
        every { responseHandler.getHalApiResponse(CORRELATION_ID, httpResponse) } returns halResponse
        every {
            responseParser.parseInteraction(halResponse, state, NativeAuthV2Operation.MFA_VERIFY)
        } returns expected

        assertSame(expected, createInteractor().performMfaVerify(state, OTP))
        assertEquals(mfaVerifyUrl, captured.url.captured)
        assertMergedHeaders(captured.headers.captured)
        assertJsonBody(
            captured.body.captured,
            mapOf("continuationToken" to CONTINUATION_TOKEN, "otp" to OTP)
        )
    }

    // -----------------------------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------------------------

    private fun assertMergedHeaders(headers: Map<String, String?>) {
        assertEquals(
            HttpConstants.MediaType.APPLICATION_JSON,
            headers[HttpConstants.HeaderField.CONTENT_TYPE]
        )
        assertEquals(INTERCEPTOR_VALUE, headers[INTERCEPTOR_HEADER])
    }

    /**
     * [secretKeys] identifies fields whose values must never be printed into a JUnit failure
     * message, matching the convention already used by the SSPR interactor tests.
     */
    private fun assertJsonBody(
        body: ByteArray,
        expected: Map<String, String>,
        secretKeys: Set<String> = emptySet()
    ) {
        val json = JSONObject(String(body, StandardCharsets.UTF_8))
        assertEquals(expected.size, json.length())
        expected.forEach { (key, value) ->
            if (key in secretKeys) {
                assertTrue("Request body field '$key' did not match the expected value", json.getString(key) == value)
            } else {
                assertEquals(value, json.getString(key))
            }
        }
    }

    private fun assertPasswordCleared(password: CharArray) {
        assertTrue(password.all { it == '\u0000' })
    }

    private fun capturePost(httpResponse: HttpResponse): HttpRequestCapture {
        val capturedUrl = slot<URL>()
        val capturedHeaders = slot<Map<String, String?>>()
        val capturedBody = slot<ByteArray>()
        every {
            httpClient.post(capture(capturedUrl), capture(capturedHeaders), capture(capturedBody))
        } returns httpResponse
        return HttpRequestCapture(capturedUrl, capturedHeaders, capturedBody)
    }

    private fun continuationState(
        vararg links: Pair<NativeAuthV2LinkRelation, String>
    ): NativeAuthV2ContinuationState {
        val linksJson = links.joinToString(",") { (relation, href) ->
            "\"${relation.value}\":{\"href\":\"$href\"}"
        }
        val response = NativeAuthV2HalApiResponse.from(
            halResource = HalResource.from("""{"continuationToken":"$CONTINUATION_TOKEN","_links":{$linksJson}}"""),
            statusCode = 200,
            correlationId = CORRELATION_ID
        )
        return NativeAuthV2ContinuationState.fromAuthorizeChallengeResponse(
            response = response,
            continuationToken = CONTINUATION_TOKEN,
            entryRelation = NativeAuthV2LinkRelation.SIGN_IN,
            scopes = listOf("User.Read"),
            scenario = NativeAuthV2FlowScenario.SIGN_IN
        )
    }

    /** State produced by a real sign-in challenge response, so per-method hrefs are retained. */
    private fun signInChallengeState(): NativeAuthV2ContinuationState {
        val response = NativeAuthV2HalApiResponse.from(
            halResource = HalResource.from(
                """
                {
                  "continuationToken": "$CONTINUATION_TOKEN",
                  "action": "challenge",
                  "_embedded": {
                    "methods": [
                      {
                        "id": "pwd-1",
                        "type": "password",
                        "_links": { "challenge": { "href": "/password/pwd-1/challenge" } }
                      },
                      {
                        "id": "email-1",
                        "type": "email",
                        "_links": { "challenge": { "href": "/email/email-1/challenge" } }
                      }
                    ]
                  }
                }
                """.trimIndent()
            ),
            statusCode = 200,
            correlationId = CORRELATION_ID
        )
        return NativeAuthV2ContinuationState.next(continuationState(), response, selectedMethod = null)!!
    }

    private fun jsonHeaders(): Map<String, String?> = mapOf(
        HttpConstants.HeaderField.CONTENT_TYPE to HttpConstants.MediaType.APPLICATION_JSON
    )

    private data class HttpRequestCapture(
        val url: CapturingSlot<URL>,
        val headers: CapturingSlot<Map<String, String?>>,
        val body: CapturingSlot<ByteArray>
    )

    private companion object {
        private const val CLIENT_ID = "client-id"
        private const val CORRELATION_ID = "correlation-id"
        private const val CONTINUATION_TOKEN = "continuation-token"
        private const val USERNAME = "ada@contoso.com"
        private const val OTP = "654321"
        private const val INTERCEPTOR_HEADER = "x-akamai-sensor"
        private const val INTERCEPTOR_VALUE = "sensor-data-123"
        private val PASSWORD = "P@ssw0rd!".toCharArray()
    }
}
