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

import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.AuthorizeChallengeContinueRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.AuthorizeChallengeStartRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.NativeAuthV2ChallengeRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.NativeAuthV2EntryRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.NativeAuthV2PollRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.NativeAuthV2TokenRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.NativeAuthV2UpdatePasswordRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.NativeAuthV2VerifyRequest
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.AuthorizeChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.HalResource
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2HalApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2InteractionApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2LinkRelation
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ResponseParser
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2FlowScenario
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2RequestProvider
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2ResponseHandler
import com.microsoft.identity.common.java.net.HttpClient
import com.microsoft.identity.common.java.net.HttpConstants
import com.microsoft.identity.common.java.net.HttpResponse
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.providers.oauth2.OAuth2RequestInterceptor
import io.mockk.*
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class NativeAuthV2InteractorTest {

    private val authorizeChallengeUrl = URL("https://contoso.ciamlogin.com/oauth2/v2.0/authorize-challenge")
    private val resetPasswordUrl = URL("https://contoso.ciamlogin.com/nativeauth/v2/reset-password/start")
    private val challengeUrl = URL("https://contoso.ciamlogin.com/nativeauth/v2/challenge")
    private val resendUrl = URL("https://contoso.ciamlogin.com/nativeauth/v2/resend")
    private val verifyUrl = URL("https://contoso.ciamlogin.com/nativeauth/v2/verify")
    private val updatePasswordUrl = URL("https://contoso.ciamlogin.com/nativeauth/v2/update-password")
    private val pollUrl = URL("https://contoso.ciamlogin.com/nativeauth/v2/poll")
    private val tokenUrl = URL("https://contoso.ciamlogin.com/oauth2/v2.0/token")

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
    fun performAuthorizeChallengeStart_postsFormBodyAndParsesAuthorizeChallengeResult() {
        val scopes = listOf("openid", "profile")
        val request = AuthorizeChallengeStartRequest.create(
            clientId = CLIENT_ID,
            requestUrl = authorizeChallengeUrl.toString(),
            headers = formHeaders()
        )
        val httpResponse = HttpResponse(200, """{"state":"continue"}""", emptyMap())
        val halResponse = mockk<NativeAuthV2HalApiResponse>(relaxed = true)
        val expected = AuthorizeChallengeApiResult.AuthorizationCode(CORRELATION_ID, "auth-code")
        val captured = capturePost(httpResponse)

        every { requestProvider.createAuthorizeChallengeStartRequest(CORRELATION_ID) } returns request
        every { responseHandler.getHalApiResponse(CORRELATION_ID, httpResponse) } returns halResponse
        every {
            responseParser.parseAuthorizeChallenge(
                response = halResponse,
                entryRelation = NativeAuthV2LinkRelation.RESET_PASSWORD,
                scenario = NativeAuthV2FlowScenario.RESET_PASSWORD,
                scopes = scopes
            )
        } returns expected

        val actual = createInteractor().performAuthorizeChallengeStart(
            correlationId = CORRELATION_ID,
            entryRelation = NativeAuthV2LinkRelation.RESET_PASSWORD,
            scenario = NativeAuthV2FlowScenario.RESET_PASSWORD,
            scopes = scopes
        )

        assertSame(expected, actual)
        assertEquals(authorizeChallengeUrl, captured.url.captured)
        assertMergedHeaders(captured.headers.captured, FORM_URL_ENCODED)
        assertFormBody(
            captured.body.captured,
            mapOf(
                "client_id" to CLIENT_ID
            )
        )
        verify(exactly = 1) { requestProvider.createAuthorizeChallengeStartRequest(CORRELATION_ID) }
        verify(exactly = 1) { responseHandler.getHalApiResponse(CORRELATION_ID, httpResponse) }
        verify(exactly = 1) {
            responseParser.parseAuthorizeChallenge(
                response = halResponse,
                entryRelation = NativeAuthV2LinkRelation.RESET_PASSWORD,
                scenario = NativeAuthV2FlowScenario.RESET_PASSWORD,
                scopes = scopes
            )
        }
    }

    @Test
    fun performAuthorizeChallengeContinue_postsContinuationStateAndParsesAuthorizeChallengeResult() {
        val state = continuationState(
            NativeAuthV2LinkRelation.RESET_PASSWORD to "/nativeauth/v2/reset-password/start"
        )
        val request = AuthorizeChallengeContinueRequest.create(
            continuationToken = CONTINUATION_TOKEN,
            requestUrl = authorizeChallengeUrl.toString(),
            headers = formHeaders()
        )
        val httpResponse = HttpResponse(200, """{"state":"continue"}""", emptyMap())
        val halResponse = mockk<NativeAuthV2HalApiResponse>(relaxed = true)
        val expected = AuthorizeChallengeApiResult.ContinuationRequired(
            CORRELATION_ID,
            continuationState(NativeAuthV2LinkRelation.CHALLENGE to "/nativeauth/v2/challenge")
        )
        val captured = capturePost(httpResponse)

        every { requestProvider.createAuthorizeChallengeContinueRequest(state) } returns request
        every { responseHandler.getHalApiResponse(CORRELATION_ID, httpResponse) } returns halResponse
        every {
            responseParser.parseAuthorizeChallenge(
                response = halResponse,
                entryRelation = NativeAuthV2LinkRelation.RESET_PASSWORD,
                scenario = NativeAuthV2FlowScenario.RESET_PASSWORD,
                scopes = state.scopesForTokenRequest()
            )
        } returns expected

        val actual = createInteractor().performAuthorizeChallengeContinue(state)

        assertSame(expected, actual)
        assertEquals(authorizeChallengeUrl, captured.url.captured)
        assertMergedHeaders(captured.headers.captured, FORM_URL_ENCODED)
        assertFormBody(
            captured.body.captured,
            mapOf(
                "continuation_token" to CONTINUATION_TOKEN
            )
        )
        verify(exactly = 1) { requestProvider.createAuthorizeChallengeContinueRequest(state) }
        verify(exactly = 1) { responseHandler.getHalApiResponse(CORRELATION_ID, httpResponse) }
        verify(exactly = 1) {
            responseParser.parseAuthorizeChallenge(
                response = halResponse,
                entryRelation = NativeAuthV2LinkRelation.RESET_PASSWORD,
                scenario = NativeAuthV2FlowScenario.RESET_PASSWORD,
                scopes = state.scopesForTokenRequest()
            )
        }
    }

    @Test
    fun performResetPasswordStart_postsJsonBodyAndParsesInteractionResult() {
        val state = continuationState(
            NativeAuthV2LinkRelation.RESET_PASSWORD to "/nativeauth/v2/reset-password/start"
        )
        val request = NativeAuthV2EntryRequest.create(
            clientId = CLIENT_ID,
            username = USERNAME,
            continuationToken = CONTINUATION_TOKEN,
            requestUrl = resetPasswordUrl.toString(),
            headers = jsonHeaders()
        )
        val httpResponse = HttpResponse(200, """{"action":"challenge"}""", emptyMap())
        val halResponse = mockk<NativeAuthV2HalApiResponse>(relaxed = true)
        val expected = NativeAuthV2InteractionApiResult.ChallengeRequired(
            correlationId = CORRELATION_ID,
            continuationState = continuationState(NativeAuthV2LinkRelation.CHALLENGE to "/nativeauth/v2/challenge"),
            hint = "a***@contoso.com"
        )
        val captured = capturePost(httpResponse)

        every { requestProvider.createResetPasswordStartRequest(USERNAME, state) } returns request
        every { responseHandler.getHalApiResponse(CORRELATION_ID, httpResponse) } returns halResponse
        every {
            responseParser.parseInteraction(
                response = halResponse,
                previousState = state
            )
        } returns expected

        val actual = createInteractor().performResetPasswordStart(USERNAME, state)

        assertSame(expected, actual)
        assertEquals(resetPasswordUrl, captured.url.captured)
        assertMergedHeaders(captured.headers.captured, HttpConstants.MediaType.APPLICATION_JSON)
        assertJsonBody(
            captured.body.captured,
            mapOf(
                "username" to USERNAME,
                "continuationToken" to CONTINUATION_TOKEN
            )
        )
        verify(exactly = 1) { requestProvider.createResetPasswordStartRequest(USERNAME, state) }
        verify(exactly = 1) { responseHandler.getHalApiResponse(CORRELATION_ID, httpResponse) }
        verify(exactly = 1) {
            responseParser.parseInteraction(
                response = halResponse,
                previousState = state
            )
        }
    }

    @Test
    fun performResend_postsJsonBodyAndParsesInteractionResult() {
        val state = continuationState(
            NativeAuthV2LinkRelation.RESEND to "/nativeauth/v2/resend"
        )
        val request = NativeAuthV2ChallengeRequest.create(
            continuationToken = CONTINUATION_TOKEN,
            requestUrl = resendUrl.toString(),
            headers = jsonHeaders()
        )
        val httpResponse = HttpResponse(200, """{"action":"verify"}""", emptyMap())
        val halResponse = mockk<NativeAuthV2HalApiResponse>(relaxed = true)
        val expected = NativeAuthV2InteractionApiResult.CodeRequired(
            correlationId = CORRELATION_ID,
            continuationState = continuationState(NativeAuthV2LinkRelation.VERIFY to "/nativeauth/v2/verify"),
            challengeTargetLabel = "a***@contoso.com",
            challengeChannel = "email",
            codeLength = 6
        )
        val captured = capturePost(httpResponse)

        every { requestProvider.createResendRequest(state) } returns request
        every { responseHandler.getHalApiResponse(CORRELATION_ID, httpResponse) } returns halResponse
        every {
            responseParser.parseInteraction(
                response = halResponse,
                previousState = state
            )
        } returns expected

        val actual = createInteractor().performResend(state)

        assertSame(expected, actual)
        assertEquals(resendUrl, captured.url.captured)
        assertMergedHeaders(captured.headers.captured, HttpConstants.MediaType.APPLICATION_JSON)
        assertJsonBody(
            captured.body.captured,
            mapOf("continuationToken" to CONTINUATION_TOKEN)
        )
        verify(exactly = 1) { requestProvider.createResendRequest(state) }
        verify(exactly = 1) { responseHandler.getHalApiResponse(CORRELATION_ID, httpResponse) }
        verify(exactly = 1) {
            responseParser.parseInteraction(
                response = halResponse,
                previousState = state
            )
        }
    }

    @Test
    fun performVerify_postsJsonBodyAndParsesInteractionResult() {
        val state = continuationState(
            NativeAuthV2LinkRelation.VERIFY to "/nativeauth/v2/verify"
        )
        val request = NativeAuthV2VerifyRequest.create(
            continuationToken = CONTINUATION_TOKEN,
            otp = OTP,
            requestUrl = verifyUrl.toString(),
            headers = jsonHeaders()
        )
        val httpResponse = HttpResponse(200, """{"action":"update"}""", emptyMap())
        val halResponse = mockk<NativeAuthV2HalApiResponse>(relaxed = true)
        val expected = NativeAuthV2InteractionApiResult.UpdateRequired(
            correlationId = CORRELATION_ID,
            continuationState = continuationState(NativeAuthV2LinkRelation.UPDATE to "/nativeauth/v2/update-password")
        )
        val captured = capturePost(httpResponse)

        every { requestProvider.createVerifyRequest(state, OTP) } returns request
        every { responseHandler.getHalApiResponse(CORRELATION_ID, httpResponse) } returns halResponse
        every {
            responseParser.parseInteraction(
                response = halResponse,
                previousState = state
            )
        } returns expected

        val actual = createInteractor().performVerify(state, OTP)

        assertSame(expected, actual)
        assertEquals(verifyUrl, captured.url.captured)
        assertMergedHeaders(captured.headers.captured, HttpConstants.MediaType.APPLICATION_JSON)
        assertJsonBody(
            captured.body.captured,
            mapOf(
                "continuationToken" to CONTINUATION_TOKEN,
                "otp" to OTP
            )
        )
        verify(exactly = 1) { requestProvider.createVerifyRequest(state, OTP) }
        verify(exactly = 1) { responseHandler.getHalApiResponse(CORRELATION_ID, httpResponse) }
        verify(exactly = 1) {
            responseParser.parseInteraction(
                response = halResponse,
                previousState = state
            )
        }
    }

    @Test
    fun performUpdatePassword_putsJsonBodyParsesInteractionResultAndClearsPassword() {
        val state = continuationState(
            NativeAuthV2LinkRelation.UPDATE to "/nativeauth/v2/update-password"
        )
        val newPassword = NEW_PASSWORD.clone()
        val request = NativeAuthV2UpdatePasswordRequest.create(
            clientId = CLIENT_ID,
            continuationToken = CONTINUATION_TOKEN,
            newPassword = newPassword,
            requestUrl = updatePasswordUrl.toString(),
            headers = jsonHeaders()
        )
        val httpResponse = HttpResponse(200, """{"action":"poll"}""", emptyMap())
        val halResponse = mockk<NativeAuthV2HalApiResponse>(relaxed = true)
        val expected = NativeAuthV2InteractionApiResult.PollInProgress(
            correlationId = CORRELATION_ID,
            continuationState = continuationState(NativeAuthV2LinkRelation.POLL to "/nativeauth/v2/poll"),
            retryAfterMillis = 3000L
        )
        val captured = capturePut(httpResponse)

        every { requestProvider.createUpdatePasswordRequest(state, newPassword) } returns request
        every { responseHandler.getHalApiResponse(CORRELATION_ID, httpResponse) } returns halResponse
        every {
            responseParser.parseInteraction(
                response = halResponse,
                previousState = state
            )
        } returns expected

        val actual = createInteractor().performUpdatePassword(state, newPassword)

        assertSame(expected, actual)
        assertEquals(updatePasswordUrl, captured.url.captured)
        assertMergedHeaders(captured.headers.captured, HttpConstants.MediaType.APPLICATION_JSON)
        assertJsonBody(
            captured.body.captured,
            mapOf(
                "continuationToken" to CONTINUATION_TOKEN,
                "newPassword" to String(NEW_PASSWORD)
            ),
            secretKeys = setOf("newPassword")
        )
        assertPasswordCleared(newPassword)
        verify(exactly = 1) { requestProvider.createUpdatePasswordRequest(state, newPassword) }
        verify(exactly = 1) { responseHandler.getHalApiResponse(CORRELATION_ID, httpResponse) }
        verify(exactly = 1) {
            responseParser.parseInteraction(
                response = halResponse,
                previousState = state
            )
        }
    }

    @Test
    fun performUpdatePassword_clearsPasswordWhenHttpPutFails() {
        val state = continuationState(
            NativeAuthV2LinkRelation.UPDATE to "/nativeauth/v2/update-password"
        )
        val newPassword = NEW_PASSWORD.clone()
        val request = NativeAuthV2UpdatePasswordRequest.create(
            clientId = CLIENT_ID,
            continuationToken = CONTINUATION_TOKEN,
            newPassword = newPassword,
            requestUrl = updatePasswordUrl.toString(),
            headers = jsonHeaders()
        )
        val expectedFailure = RuntimeException("HTTP failure")
        val captured = capturePut(expectedFailure)

        every { requestProvider.createUpdatePasswordRequest(state, newPassword) } returns request

        try {
            createInteractor().performUpdatePassword(state, newPassword)
            fail("Expected performUpdatePassword to rethrow the HTTP failure")
        } catch (actual: RuntimeException) {
            assertSame(expectedFailure, actual)
        }

        assertEquals(updatePasswordUrl, captured.url.captured)
        assertMergedHeaders(captured.headers.captured, HttpConstants.MediaType.APPLICATION_JSON)
        assertJsonBody(
            captured.body.captured,
            mapOf(
                "continuationToken" to CONTINUATION_TOKEN,
                "newPassword" to String(NEW_PASSWORD)
            ),
            secretKeys = setOf("newPassword")
        )
        assertPasswordCleared(newPassword)
        verify(exactly = 1) { requestProvider.createUpdatePasswordRequest(state, newPassword) }
        verify { responseHandler wasNot Called }
        verify { responseParser wasNot Called }
    }

    @Test
    fun performUpdatePassword_clearsPasswordWhenRequestCreationFails() {
        val state = continuationState(
            NativeAuthV2LinkRelation.UPDATE to "/nativeauth/v2/update-password"
        )
        val newPassword = NEW_PASSWORD.clone()
        val expectedFailure = RuntimeException("Missing update relation")

        every { requestProvider.createUpdatePasswordRequest(state, newPassword) } throws expectedFailure

        try {
            createInteractor().performUpdatePassword(state, newPassword)
            fail("Expected performUpdatePassword to rethrow the request creation failure")
        } catch (actual: RuntimeException) {
            assertSame(expectedFailure, actual)
        }

        assertPasswordCleared(newPassword)
        verify { responseHandler wasNot Called }
        verify { responseParser wasNot Called }
    }

    @Test
    fun performPoll_postsJsonBodyAndParsesInteractionResult() {
        val state = continuationState(
            NativeAuthV2LinkRelation.POLL to "/nativeauth/v2/poll"
        )
        val request = NativeAuthV2PollRequest.create(
            continuationToken = CONTINUATION_TOKEN,
            requestUrl = pollUrl.toString(),
            headers = jsonHeaders()
        )
        val httpResponse = HttpResponse(200, """{"state":"continue"}""", emptyMap())
        val halResponse = mockk<NativeAuthV2HalApiResponse>(relaxed = true)
        val expected = NativeAuthV2InteractionApiResult.ReadyToComplete(
            correlationId = CORRELATION_ID,
            continuationState = continuationState(NativeAuthV2LinkRelation.CONTINUE to "/nativeauth/v2/continue")
        )
        val captured = capturePost(httpResponse)

        every { requestProvider.createPollRequest(state) } returns request
        every { responseHandler.getHalApiResponse(CORRELATION_ID, httpResponse) } returns halResponse
        every {
            responseParser.parseInteraction(
                response = halResponse,
                previousState = state
            )
        } returns expected

        val actual = createInteractor().performPoll(state)

        assertSame(expected, actual)
        assertEquals(pollUrl, captured.url.captured)
        assertMergedHeaders(captured.headers.captured, HttpConstants.MediaType.APPLICATION_JSON)
        assertJsonBody(
            captured.body.captured,
            mapOf("continuationToken" to CONTINUATION_TOKEN)
        )
        verify(exactly = 1) { requestProvider.createPollRequest(state) }
        verify(exactly = 1) { responseHandler.getHalApiResponse(CORRELATION_ID, httpResponse) }
        verify(exactly = 1) {
            responseParser.parseInteraction(
                response = halResponse,
                previousState = state
            )
        }
    }

    @Test
    fun performTokenRequest_postsFormBodyAndReturnsTokenResult() {
        val scopes = listOf("User.Read", "offline_access")
        val claimsRequestJson = """{"access_token":{"xms_cc":{"values":["cp1"]}}}"""
        val request = NativeAuthV2TokenRequest.create(
            clientId = CLIENT_ID,
            code = AUTHORIZATION_CODE,
            scopes = scopes,
            claimsRequestJson = claimsRequestJson,
            requestUrl = tokenUrl.toString(),
            headers = formHeaders()
        )
        val httpResponse = HttpResponse(200, """{"access_token":"secret"}""", emptyMap())
        val expected = SignInTokenApiResult.UnknownError(
            correlationId = CORRELATION_ID,
            error = "temporarily_unavailable",
            errorDescription = "Retry later",
            errorCodes = emptyList()
        )
        val captured = capturePost(httpResponse)

        every {
            requestProvider.createTokenRequest(
                code = AUTHORIZATION_CODE,
                scopes = scopes,
                correlationId = CORRELATION_ID,
                claimsRequestJson = claimsRequestJson
            )
        } returns request
        every { responseHandler.getTokenApiResponse(CORRELATION_ID, httpResponse) } returns expected

        val actual = createInteractor().performTokenRequest(
            code = AUTHORIZATION_CODE,
            scopes = scopes,
            correlationId = CORRELATION_ID,
            claimsRequestJson = claimsRequestJson
        )

        assertSame(expected, actual)
        assertEquals(tokenUrl, captured.url.captured)
        assertMergedHeaders(captured.headers.captured, FORM_URL_ENCODED)
        assertFormBody(
            captured.body.captured,
            mapOf(
                "client_id" to CLIENT_ID,
                "grant_type" to "authorization_code",
                "code" to AUTHORIZATION_CODE,
                "scope" to "User.Read offline_access",
                "claims" to claimsRequestJson,
                "client_info" to "true"
            )
        )
        verify(exactly = 1) {
            requestProvider.createTokenRequest(
                code = AUTHORIZATION_CODE,
                scopes = scopes,
                correlationId = CORRELATION_ID,
                claimsRequestJson = claimsRequestJson
            )
        }
        verify(exactly = 1) { responseHandler.getTokenApiResponse(CORRELATION_ID, httpResponse) }
        verify { responseParser wasNot Called }
    }

    @Test
    fun performVerify_propagatesResponseParserExceptionWithoutConvertingToSuccess() {
        val state = continuationState(
            NativeAuthV2LinkRelation.VERIFY to "/nativeauth/v2/verify"
        )
        val request = NativeAuthV2VerifyRequest.create(
            continuationToken = CONTINUATION_TOKEN,
            otp = OTP,
            requestUrl = verifyUrl.toString(),
            headers = jsonHeaders()
        )
        val httpResponse = HttpResponse(200, """{"action":"update"}""", emptyMap())
        val halResponse = mockk<NativeAuthV2HalApiResponse>(relaxed = true)
        val expectedFailure = IllegalStateException("parser failure")
        capturePost(httpResponse)

        every { requestProvider.createVerifyRequest(state, OTP) } returns request
        every { responseHandler.getHalApiResponse(CORRELATION_ID, httpResponse) } returns halResponse
        every {
            responseParser.parseInteraction(
                response = halResponse,
                previousState = state
            )
        } throws expectedFailure

        try {
            createInteractor().performVerify(state, OTP)
            fail("Expected performVerify to rethrow the response parser failure")
        } catch (actual: IllegalStateException) {
            assertSame(expectedFailure, actual)
        }

        verify(exactly = 1) {
            responseParser.parseInteraction(
                response = halResponse,
                previousState = state
            )
        }
    }

    @Test
    fun performAuthorizeChallengeStart_propagatesResponseParserExceptionWithoutConvertingToSuccess() {
        val scopes = listOf("openid", "profile")
        val request = AuthorizeChallengeStartRequest.create(
            clientId = CLIENT_ID,
            requestUrl = authorizeChallengeUrl.toString(),
            headers = formHeaders()
        )
        val httpResponse = HttpResponse(200, """{"state":"continue"}""", emptyMap())
        val halResponse = mockk<NativeAuthV2HalApiResponse>(relaxed = true)
        val expectedFailure = IllegalStateException("parser failure")
        capturePost(httpResponse)

        every { requestProvider.createAuthorizeChallengeStartRequest(CORRELATION_ID) } returns request
        every { responseHandler.getHalApiResponse(CORRELATION_ID, httpResponse) } returns halResponse
        every {
            responseParser.parseAuthorizeChallenge(
                response = halResponse,
                entryRelation = NativeAuthV2LinkRelation.RESET_PASSWORD,
                scenario = NativeAuthV2FlowScenario.RESET_PASSWORD,
                scopes = scopes
            )
        } throws expectedFailure

        try {
            createInteractor().performAuthorizeChallengeStart(
                correlationId = CORRELATION_ID,
                entryRelation = NativeAuthV2LinkRelation.RESET_PASSWORD,
                scenario = NativeAuthV2FlowScenario.RESET_PASSWORD,
                scopes = scopes
            )
            fail("Expected performAuthorizeChallengeStart to rethrow the response parser failure")
        } catch (actual: IllegalStateException) {
            assertSame(expectedFailure, actual)
        }
    }

    @Test
    fun performTokenRequest_propagatesResponseHandlerException() {
        val scopes = listOf("User.Read", "offline_access")
        val request = NativeAuthV2TokenRequest.create(
            clientId = CLIENT_ID,
            code = AUTHORIZATION_CODE,
            scopes = scopes,
            requestUrl = tokenUrl.toString(),
            headers = formHeaders()
        )
        val httpResponse = HttpResponse(200, """{"access_token":"secret"}""", emptyMap())
        val expectedFailure = IllegalStateException("handler failure")
        capturePost(httpResponse)

        every {
            requestProvider.createTokenRequest(
                code = AUTHORIZATION_CODE,
                scopes = scopes,
                correlationId = CORRELATION_ID
            )
        } returns request
        every { responseHandler.getTokenApiResponse(CORRELATION_ID, httpResponse) } throws expectedFailure

        try {
            createInteractor().performTokenRequest(
                code = AUTHORIZATION_CODE,
                scopes = scopes,
                correlationId = CORRELATION_ID
            )
            fail("Expected performTokenRequest to rethrow the response handler failure")
        } catch (actual: IllegalStateException) {
            assertSame(expectedFailure, actual)
        }

        verify { responseParser wasNot Called }
    }

    @Test
    fun assertJsonBody_neverLeaksSecretKeyValueOnMismatch() {
        val actualPassword = String(NEW_PASSWORD)
        val expectedPassword = "a-different-password"
        val body = """{"newPassword":"$actualPassword"}""".toByteArray(StandardCharsets.UTF_8)

        val error = try {
            assertJsonBody(
                body,
                mapOf("newPassword" to expectedPassword),
                secretKeys = setOf("newPassword")
            )
            fail("Expected assertJsonBody to fail for a mismatched secret field")
            null
        } catch (failure: AssertionError) {
            failure
        }

        val message = error?.message.orEmpty()
        assertFalse("Failure message must not contain the actual secret value", message.contains(actualPassword))
        assertFalse("Failure message must not contain the expected secret value", message.contains(expectedPassword))
    }

    private fun assertMergedHeaders(headers: Map<String, String?>, expectedContentType: String) {
        assertEquals(expectedContentType, headers[HttpConstants.HeaderField.CONTENT_TYPE])
        assertEquals(BASE_CLIENT_SKU, headers["x-client-SKU"])
        assertEquals(INTERCEPTOR_VALUE, headers[INTERCEPTOR_HEADER])
    }

    private fun assertFormBody(body: ByteArray, expected: Map<String, String>) {
        assertEquals(expected, parseFormBody(body))
    }

    /**
     * [secretKeys] identifies fields (e.g. "newPassword") whose values must never appear in a
     * JUnit assertion failure message. For those keys a boolean check with a value-free message
     * is used instead of [assertEquals], which would otherwise print both the expected and actual
     * secret values into (potentially shared) test/CI logs on mismatch.
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

    private fun parseFormBody(body: ByteArray): Map<String, String> =
        String(body, StandardCharsets.UTF_8)
            .split("&")
            .associate { pair ->
                val separatorIndex = pair.indexOf('=')
                val name = URLDecoder.decode(pair.substring(0, separatorIndex), StandardCharsets.UTF_8.name())
                val value = URLDecoder.decode(pair.substring(separatorIndex + 1), StandardCharsets.UTF_8.name())
                name to value
            }

    private fun capturePost(httpResponse: HttpResponse): HttpRequestCapture {
        val capturedUrl = slot<URL>()
        val capturedHeaders = slot<Map<String, String?>>()
        val capturedBody = slot<ByteArray>()
        // Form-encoded calls (authorize-challenge, token) still use the post() convenience method,
        // while JSON interactions go through method(POST, ...); both fill the same slots.
        every { httpClient.post(capture(capturedUrl), capture(capturedHeaders), capture(capturedBody)) } returns httpResponse
        every {
            httpClient.method(
                HttpClient.HttpMethod.POST,
                capture(capturedUrl),
                capture(capturedHeaders),
                capture(capturedBody)
            )
        } returns httpResponse
        return HttpRequestCapture(capturedUrl, capturedHeaders, capturedBody)
    }

    private fun capturePut(httpResponse: HttpResponse): HttpRequestCapture {
        val capturedUrl = slot<URL>()
        val capturedHeaders = slot<Map<String, String?>>()
        val capturedBody = slot<ByteArray>()
        every {
            httpClient.method(
                HttpClient.HttpMethod.PUT,
                capture(capturedUrl),
                capture(capturedHeaders),
                capture(capturedBody)
            )
        } returns httpResponse
        return HttpRequestCapture(capturedUrl, capturedHeaders, capturedBody)
    }

    private fun capturePut(throwable: RuntimeException): HttpRequestCapture {
        val capturedUrl = slot<URL>()
        val capturedHeaders = slot<Map<String, String?>>()
        val capturedBody = slot<ByteArray>()
        every {
            httpClient.method(
                HttpClient.HttpMethod.PUT,
                capture(capturedUrl),
                capture(capturedHeaders),
                capture(capturedBody)
            )
        } throws throwable
        return HttpRequestCapture(capturedUrl, capturedHeaders, capturedBody)
    }

    private fun continuationState(
        vararg links: Pair<NativeAuthV2LinkRelation, String>,
        continuationToken: String = CONTINUATION_TOKEN,
        correlationId: String = CORRELATION_ID
    ): NativeAuthV2ContinuationState {
        val linksJson = links.joinToString(",") { (relation, href) ->
            "\"${relation.value}\":{\"href\":\"$href\"}"
        }
        val response = NativeAuthV2HalApiResponse.from(
            halResource = HalResource.from("""{"continuationToken":"$continuationToken","_links":{$linksJson}}"""),
            statusCode = 200,
            correlationId = correlationId
        )

        return NativeAuthV2ContinuationState.fromAuthorizeChallengeResponse(
            response = response,
            continuationToken = "seed",
            entryRelation = NativeAuthV2LinkRelation.RESET_PASSWORD,
            scopes = listOf("User.Read"),
            scenario = NativeAuthV2FlowScenario.RESET_PASSWORD
        )
    }

    private fun formHeaders(): Map<String, String?> = mapOf(
        HttpConstants.HeaderField.CONTENT_TYPE to FORM_URL_ENCODED,
        "x-client-SKU" to BASE_CLIENT_SKU
    )

    private fun jsonHeaders(): Map<String, String?> = mapOf(
        HttpConstants.HeaderField.CONTENT_TYPE to HttpConstants.MediaType.APPLICATION_JSON,
        "x-client-SKU" to BASE_CLIENT_SKU
    )

    private data class HttpRequestCapture(
        val url: io.mockk.CapturingSlot<URL>,
        val headers: io.mockk.CapturingSlot<Map<String, String?>>,
        val body: io.mockk.CapturingSlot<ByteArray>
    )

    private companion object {
        private const val CLIENT_ID = "client-id"
        private const val CORRELATION_ID = "correlation-id"
        private const val CONTINUATION_TOKEN = "continuation-token"
        private const val FORM_URL_ENCODED = "application/x-www-form-urlencoded"
        private const val CHALLENGE_TYPE = "oob"
        private const val USERNAME = "ada@contoso.com"
        private const val OTP = "654321"
        private const val AUTHORIZATION_CODE = "authorization-code"
        private const val BASE_CLIENT_SKU = "MSAL.Android"
        private const val INTERCEPTOR_HEADER = "x-akamai-sensor"
        private const val INTERCEPTOR_VALUE = "sensor-data-123"
        private val NEW_PASSWORD = "P@ssw0rd!".toCharArray()
    }
}
