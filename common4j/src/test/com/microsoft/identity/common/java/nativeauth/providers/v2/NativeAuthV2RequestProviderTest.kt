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
package com.microsoft.identity.common.java.nativeauth.providers.v2

import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.logging.LibraryInfoHelper
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.HalResource
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2HalApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2LinkRelation
import com.microsoft.identity.common.java.net.HttpConstants
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import java.net.URL

class NativeAuthV2RequestProviderTest {
    private val hrefResolver = mockk<NativeAuthV2HrefResolver>()

    private val provider = NativeAuthV2RequestProvider(
        config = mockk<NativeAuthOAuth2Configuration> {
            every { clientId } returns CLIENT_ID
            every { challengeType } returns CHALLENGE_TYPE
            every { getAuthorizeChallengeEndpoint() } returns URL("https://contoso.com/authorize-challenge")
            every { getSignInTokenEndpoint() } returns URL("https://contoso.com/token")
        },
        hrefResolver = hrefResolver
    )

    @Test
    fun constructor_whenV2EndpointsAreUnstubbed_doesNotResolveEndpoints() {
        val config = mockk<NativeAuthOAuth2Configuration>()

        NativeAuthV2RequestProvider(config, hrefResolver)

        verify(exactly = 0) { config.getAuthorizeChallengeEndpoint() }
        verify(exactly = 0) { config.getSignInTokenEndpoint() }
    }

    @Test
    fun createAuthorizeChallengeStartRequest_usesAuthorizeEndpointFormHeadersAndChallengeType() {
        val request = provider.createAuthorizeChallengeStartRequest(CORRELATION_ID)

        assertEquals(URL("https://contoso.com/authorize-challenge"), request.requestUrl)
        assertEquals(CLIENT_ID, request.parameters.clientId)
        assertEquals(CHALLENGE_TYPE, request.parameters.challengeType)
        assertCommonHeaders(request.headers, CORRELATION_ID, FORM_URL_ENCODED_CONTENT_TYPE)
    }

    @Test
    fun createAuthorizeChallengeStartRequest_whenCorrelationIdUnset_omitsClientRequestIdHeader() {
        val request = provider.createAuthorizeChallengeStartRequest(UNSET_CORRELATION_ID)

        assertNull(request.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID])
        assertEquals(FORM_URL_ENCODED_CONTENT_TYPE, request.headers[HttpConstants.HeaderField.CONTENT_TYPE])
    }

    @Test
    fun createAuthorizeChallengeContinueRequest_usesContinuationTokenAuthorizeEndpointAndFormHeaders() {
        val state = continuationState()

        val request = provider.createAuthorizeChallengeContinueRequest(state)

        assertEquals(URL("https://contoso.com/authorize-challenge"), request.requestUrl)
        assertEquals(CLIENT_ID, request.parameters.clientId)
        assertEquals(CONTINUATION_TOKEN, request.parameters.continuationToken)
        assertCommonHeaders(request.headers, CORRELATION_ID, FORM_URL_ENCODED_CONTENT_TYPE)
    }

    @Test
    fun createResetPasswordStartRequest_resolvesResetPasswordHrefAndUsesJsonHeaders() {
        val state = continuationState(NativeAuthV2LinkRelation.RESET_PASSWORD to "/password/start")
        val expectedUrl = URL("https://contoso.com/password/start")
        every { hrefResolver.resolve("/password/start", CORRELATION_ID) } returns expectedUrl

        val request = provider.createResetPasswordStartRequest(USERNAME, state)

        assertEquals(expectedUrl, request.requestUrl)
        assertEquals(CLIENT_ID, request.parameters.clientId)
        assertEquals(USERNAME, request.parameters.username)
        assertEquals(CONTINUATION_TOKEN, request.parameters.continuationToken)
        assertCommonHeaders(request.headers, CORRELATION_ID, HttpConstants.MediaType.APPLICATION_JSON)
        verify(exactly = 1) { hrefResolver.resolve("/password/start", CORRELATION_ID) }
    }

    @Test
    fun createChallengeRequest_resolvesChallengeRelationAndUsesJsonHeaders() {
        val state = continuationState(NativeAuthV2LinkRelation.CHALLENGE to "/challenge")
        val expectedUrl = URL("https://contoso.com/challenge")
        every { hrefResolver.resolve("/challenge", CORRELATION_ID) } returns expectedUrl

        val request = provider.createChallengeRequest(state)

        assertEquals(expectedUrl, request.requestUrl)
        assertEquals(CLIENT_ID, request.parameters.clientId)
        assertEquals(CONTINUATION_TOKEN, request.parameters.continuationToken)
        assertCommonHeaders(request.headers, CORRELATION_ID, HttpConstants.MediaType.APPLICATION_JSON)
        verify(exactly = 1) { hrefResolver.resolve("/challenge", CORRELATION_ID) }
    }

    @Test
    fun createResendRequest_resolvesResendRelationAndUsesJsonHeaders() {
        val state = continuationState(NativeAuthV2LinkRelation.RESEND to "/resend")
        val expectedUrl = URL("https://contoso.com/resend")
        every { hrefResolver.resolve("/resend", CORRELATION_ID) } returns expectedUrl

        val request = provider.createResendRequest(state)

        assertEquals(expectedUrl, request.requestUrl)
        assertEquals(CLIENT_ID, request.parameters.clientId)
        assertEquals(CONTINUATION_TOKEN, request.parameters.continuationToken)
        assertCommonHeaders(request.headers, CORRELATION_ID, HttpConstants.MediaType.APPLICATION_JSON)
        verify(exactly = 1) { hrefResolver.resolve("/resend", CORRELATION_ID) }
    }

    @Test
    fun createVerifyRequest_resolvesVerifyRelationAndUsesJsonHeaders() {
        val state = continuationState(NativeAuthV2LinkRelation.VERIFY to "/verify")
        val expectedUrl = URL("https://contoso.com/verify")
        every { hrefResolver.resolve("/verify", CORRELATION_ID) } returns expectedUrl

        val request = provider.createVerifyRequest(state, OTP)

        assertEquals(expectedUrl, request.requestUrl)
        assertEquals(CLIENT_ID, request.parameters.clientId)
        assertEquals(CONTINUATION_TOKEN, request.parameters.continuationToken)
        assertEquals(OTP, request.parameters.otp)
        assertCommonHeaders(request.headers, CORRELATION_ID, HttpConstants.MediaType.APPLICATION_JSON)
        verify(exactly = 1) { hrefResolver.resolve("/verify", CORRELATION_ID) }
    }

    @Test
    fun createUpdatePasswordRequest_whenUpdateRelationPresent_prefersUpdateRelation() {
        val state = continuationState(
            NativeAuthV2LinkRelation.UPDATE to "/password/update",
            NativeAuthV2LinkRelation.SELF to "/password/self"
        )
        val expectedUrl = URL("https://contoso.com/password/update")
        every { hrefResolver.resolve("/password/update", CORRELATION_ID) } returns expectedUrl

        val request = provider.createUpdatePasswordRequest(state, NEW_PASSWORD)

        assertEquals(expectedUrl, request.requestUrl)
        assertEquals(CLIENT_ID, request.parameters.clientId)
        assertEquals(CONTINUATION_TOKEN, request.parameters.continuationToken)
        assertSame(NEW_PASSWORD, request.parameters.newPassword)
        assertCommonHeaders(request.headers, CORRELATION_ID, HttpConstants.MediaType.APPLICATION_JSON)
        verify(exactly = 1) { hrefResolver.resolve("/password/update", CORRELATION_ID) }
        verify(exactly = 0) { hrefResolver.resolve("/password/self", CORRELATION_ID) }
    }

    @Test
    fun createUpdatePasswordRequest_whenUpdateRelationMissing_fallsBackToSelfRelation() {
        val state = continuationState(
            NativeAuthV2LinkRelation.SELF to "/password/self"
        )
        val expectedUrl = URL("https://contoso.com/password/self")
        every { hrefResolver.resolve("/password/self", CORRELATION_ID) } returns expectedUrl

        val request = provider.createUpdatePasswordRequest(state, NEW_PASSWORD)

        assertEquals(expectedUrl, request.requestUrl)
        assertEquals(CLIENT_ID, request.parameters.clientId)
        assertEquals(CONTINUATION_TOKEN, request.parameters.continuationToken)
        assertSame(NEW_PASSWORD, request.parameters.newPassword)
        assertCommonHeaders(request.headers, CORRELATION_ID, HttpConstants.MediaType.APPLICATION_JSON)
        verify(exactly = 1) { hrefResolver.resolve("/password/self", CORRELATION_ID) }
        verify(exactly = 0) { hrefResolver.resolve("/password/update", CORRELATION_ID) }
    }

    @Test
    fun createPollRequest_resolvesPollRelationAndUsesJsonHeaders() {
        val state = continuationState(NativeAuthV2LinkRelation.POLL to "/poll")
        val expectedUrl = URL("https://contoso.com/poll")
        every { hrefResolver.resolve("/poll", CORRELATION_ID) } returns expectedUrl

        val request = provider.createPollRequest(state)

        assertEquals(expectedUrl, request.requestUrl)
        assertEquals(CLIENT_ID, request.parameters.clientId)
        assertEquals(CONTINUATION_TOKEN, request.parameters.continuationToken)
        assertCommonHeaders(request.headers, CORRELATION_ID, HttpConstants.MediaType.APPLICATION_JSON)
        verify(exactly = 1) { hrefResolver.resolve("/poll", CORRELATION_ID) }
    }

    @Test
    fun createTokenRequest_usesTokenEndpointFormHeadersAndJoinedScopes() {
        val request = provider.createTokenRequest(
            code = AUTHORIZATION_CODE,
            scopes = listOf("User.Read", "offline_access"),
            correlationId = CORRELATION_ID
        )

        assertEquals(URL("https://contoso.com/token"), request.requestUrl)
        assertEquals(CLIENT_ID, request.parameters.clientId)
        assertEquals("authorization_code", request.parameters.grantType)
        assertEquals(AUTHORIZATION_CODE, request.parameters.code)
        assertEquals("User.Read offline_access", request.parameters.scope)
        assertCommonHeaders(request.headers, CORRELATION_ID, FORM_URL_ENCODED_CONTENT_TYPE)
    }

    @Test
    fun relationDrivenRequests_whenRequiredRelationMissing_throwClientExceptionWithCorrelationId() {
        listOf(
            MissingRelationCase("resetPassword", NativeAuthV2LinkRelation.CHALLENGE to "/challenge") { state ->
                provider.createResetPasswordStartRequest(USERNAME, state)
            },
            MissingRelationCase("challenge", NativeAuthV2LinkRelation.VERIFY to "/verify") { state ->
                provider.createChallengeRequest(state)
            },
            MissingRelationCase("resend", NativeAuthV2LinkRelation.CHALLENGE to "/challenge") { state ->
                provider.createResendRequest(state)
            },
            MissingRelationCase("verify", NativeAuthV2LinkRelation.CHALLENGE to "/challenge") { state ->
                provider.createVerifyRequest(state, OTP)
            },
            MissingRelationCase("poll", NativeAuthV2LinkRelation.CHALLENGE to "/challenge") { state ->
                provider.createPollRequest(state)
            }
        ).forEach { testCase ->
            val state = continuationState(testCase.presentRelation)

            val exception = assertClientException {
                testCase.action(state)
            }

            assertEquals(ClientException.MISSING_PARAMETER, exception.errorCode)
            assertEquals(CORRELATION_ID, exception.correlationId)
            assertEquals(
                "Native Auth V2 continuation state is missing the required '${testCase.expectedRelation}' relation.",
                exception.message
            )
        }
    }

    @Test
    fun createUpdatePasswordRequest_whenUpdateAndSelfRelationsAreMissing_throwsClientExceptionWithCorrelationId() {
        val exception = assertClientException {
            provider.createUpdatePasswordRequest(
                continuationState(NativeAuthV2LinkRelation.CHALLENGE to "/challenge"),
                NEW_PASSWORD
            )
        }

        assertEquals(ClientException.MISSING_PARAMETER, exception.errorCode)
        assertEquals(CORRELATION_ID, exception.correlationId)
        assertEquals(
            "Native Auth V2 continuation state is missing the required 'update' relation.",
            exception.message
        )
    }

    private fun assertCommonHeaders(
        headers: Map<String, String?>,
        correlationId: String,
        expectedContentType: String
    ) {
        assertEquals(correlationId, headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID])
        assertEquals(expectedContentType, headers[HttpConstants.HeaderField.CONTENT_TYPE])
        assertEquals(LibraryInfoHelper.getLibraryName(), headers[AuthenticationConstants.SdkPlatformFields.PRODUCT])
        assertEquals(LibraryInfoHelper.getLibraryVersion(), headers[AuthenticationConstants.SdkPlatformFields.VERSION])
    }

    private fun assertClientException(block: () -> Unit): ClientException {
        try {
            block()
            fail("Expected ClientException")
        } catch (exception: ClientException) {
            return exception
        }
        throw AssertionError("Unreachable")
    }

    private fun continuationState(vararg links: Pair<NativeAuthV2LinkRelation, String>): NativeAuthV2ContinuationState {
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
            scopes = listOf("User.Read"),
            scenario = NativeAuthV2FlowScenario.RESET_PASSWORD
        )!!
    }

    private data class MissingRelationCase(
        val expectedRelation: String,
        val presentRelation: Pair<NativeAuthV2LinkRelation, String>,
        val action: (NativeAuthV2ContinuationState) -> Unit
    )

    private companion object {
        private const val CLIENT_ID = "client-id"
        private const val CHALLENGE_TYPE = "oob"
        private const val CORRELATION_ID = "correlation-id"
        private const val CONTINUATION_TOKEN = "token"
        private const val AUTHORIZATION_CODE = "authorization-code"
        private const val OTP = "654321"
        private const val UNSET_CORRELATION_ID = "UNSET"
        private const val USERNAME = "ada@contoso.com"
        private const val FORM_URL_ENCODED_CONTENT_TYPE = "application/x-www-form-urlencoded"
        private val NEW_PASSWORD = "newPassword!".toCharArray()
    }
}
