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

import com.google.gson.JsonParser
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.AuthorizeChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.HalResource
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2HalApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2InteractionApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2LinkRelation
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2Operation
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ResponseParser
import com.microsoft.identity.common.java.net.HttpConstants
import com.microsoft.identity.common.java.util.ObjectMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test
import java.net.URL

/**
 * Covers the V2 sign-in request contract: the sign-in entry body, method-driven challenge href
 * resolution, and the password verify body. All hrefs come from the server response, never from a
 * path assembled out of a method ID.
 */
class NativeAuthV2SignInRequestProviderTest {

    @Test
    fun createSignInStartRequest_postsUsernameAndContinuationTokenToServerHref() {
        val request = provider().createSignInStartRequest(
            username = "user@contoso.com",
            state = entryState()
        )

        assertEquals(URL("https://login.contoso.com/tenant/api/v0.1/signin/start"), request.requestUrl)
        assertEquals(JSON_CONTENT_TYPE, request.headers[HttpConstants.HeaderField.CONTENT_TYPE])
        assertEquals(
            mapOf("username" to "user@contoso.com", "continuationToken" to "flow-token"),
            jsonBodyOf(request.parameters)
        )
    }

    @Test
    fun createSignInStartRequest_whenSignInRelationIsMissing_throwsMissingParameter() {
        val state = entryState(signInHref = null)

        val exception = assertClientException {
            provider().createSignInStartRequest(username = "user@contoso.com", state = state)
        }

        assertEquals(ClientException.MISSING_PARAMETER, exception.errorCode)
        assertEquals(CORRELATION_ID, exception.correlationId)
    }

    @Test
    fun createChallengeRequest_forSelectedMethod_followsThatMethodsHref() {
        val selected = signInChallengeState().withSelectedMethod("pwd-1")
        assertNotNull(selected)

        val request = provider().createChallengeRequest(selected!!)

        assertEquals(
            URL("https://login.contoso.com/tenant/api/v0.1/password/pwd-1/challenge"),
            request.requestUrl
        )
        assertEquals(
            mapOf("continuationToken" to "ct-1"),
            jsonBodyOf(request.parameters)
        )
    }

    @Test
    fun createChallengeRequest_forADifferentSelectedMethod_followsThatMethodsHref() {
        val selected = signInChallengeState().withSelectedMethod("email-1")

        val request = provider().createChallengeRequest(selected!!)

        assertEquals(
            URL("https://login.contoso.com/tenant/api/v0.1/email/email-1/challenge"),
            request.requestUrl
        )
    }

    @Test
    fun withSelectedMethod_whenMethodWasNotOffered_returnsNull() {
        assertEquals(null, signInChallengeState().withSelectedMethod("does-not-exist"))
    }

    @Test
    fun createPasswordVerifyRequest_postsPasswordAndContinuationTokenToServerHref() {
        val state = passwordVerifyState()

        val request = provider().createPasswordVerifyRequest(state, "Password123!".toCharArray())

        assertEquals(
            URL("https://login.contoso.com/tenant/api/v0.1/password/pwd-1/verify"),
            request.requestUrl
        )
        assertEquals(JSON_CONTENT_TYPE, request.headers[HttpConstants.HeaderField.CONTENT_TYPE])
        assertEquals(
            mapOf("continuationToken" to "ct-2", "password" to "Password123!"),
            jsonBodyOf(request.parameters)
        )
    }

    @Test
    fun createPasswordVerifyRequest_neverRendersSecretsInEitherStringForm() {
        val request = provider().createPasswordVerifyRequest(
            passwordVerifyState(),
            "Password123!".toCharArray()
        )

        listOf(request.toString(), request.toUnsanitizedString()).forEach { rendered ->
            assertFalse(rendered.contains("Password123!"))
            assertFalse(rendered.contains("ct-2"))
        }
    }

    @Test
    fun createPasswordVerifyRequest_whenVerifyRelationIsMissing_throwsMissingParameter() {
        val exception = assertClientException {
            provider().createPasswordVerifyRequest(entryState(), "Password123!".toCharArray())
        }

        assertEquals(ClientException.MISSING_PARAMETER, exception.errorCode)
    }

    @Test
    fun createSignInStartRequest_whenProductionAuthorityIsHttp_rejectsInsecureEndpoint() {
        val exception = assertClientException {
            provider(authorityUrl = "http://login.contoso.com/tenant")
                .createSignInStartRequest("user@contoso.com", entryState())
        }

        assertEquals(ClientException.UNSUPPORTED_URL, exception.errorCode)
    }

    // -----------------------------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------------------------

    private fun jsonBodyOf(parameters: Any): Map<String, String> =
        JsonParser.parseString(ObjectMapper.serializeObjectToJsonString(parameters))
            .asJsonObject
            .entrySet()
            .associate { (key, value) -> key to value.asString }

    private fun provider(
        authorityUrl: String = "https://login.contoso.com/tenant"
    ): NativeAuthV2RequestProvider = NativeAuthV2RequestProvider(
        NativeAuthOAuth2Configuration(
            authorityUrl = URL(authorityUrl),
            clientId = CLIENT_ID,
            challengeType = "oob",
            capabilities = null,
            requestInterceptor = null,
            useMockApiForNativeAuth = false,
            MOCK_API_URL_WITH_NATIVE_AUTH_TENANT = "https://localhost/mock-tenant"
        )
    )

    private fun entryState(signInHref: String? = "/tenant/api/v0.1/signin/start"): NativeAuthV2ContinuationState {
        val signInProperty = signInHref?.let { """"sign_in": "$it",""" } ?: ""
        val response = responseFrom(
            """
            {
              $signInProperty
              "continuation_token": "flow-token"
            }
            """.trimIndent()
        )
        return (parser.parseAuthorizeChallenge(
            response = response,
            entryRelation = NativeAuthV2LinkRelation.SIGN_IN,
            scenario = NativeAuthV2FlowScenario.SIGN_IN,
            scopes = SCOPES
        ) as? AuthorizeChallengeApiResult.ContinuationRequired)?.continuationState
            ?: NativeAuthV2ContinuationState.fromAuthorizeChallengeResponse(
                response = response,
                continuationToken = "flow-token",
                scopes = SCOPES,
                entryRelation = NativeAuthV2LinkRelation.SIGN_IN,
                scenario = NativeAuthV2FlowScenario.SIGN_IN
            )
    }

    /** State produced by the sign-in entry response, carrying both offered methods. */
    private fun signInChallengeState(): NativeAuthV2ContinuationState {
        val result = parser.parseInteraction(
            response = responseFrom(
                """
                {
                  "continuationToken": "ct-1",
                  "action": "challenge",
                  "challengeContext": { "authenticationFactor": "singleFactor" },
                  "_embedded": {
                    "methods": [
                      {
                        "id": "pwd-1",
                        "type": "password",
                        "_links": { "challenge": { "href": "/tenant/api/v0.1/password/pwd-1/challenge" } }
                      },
                      {
                        "id": "email-1",
                        "type": "email",
                        "_links": { "challenge": { "href": "/tenant/api/v0.1/email/email-1/challenge" } }
                      }
                    ]
                  }
                }
                """.trimIndent()
            ),
            previousState = entryState(),
            operation = NativeAuthV2Operation.SIGN_IN_START
        )
        return (result as NativeAuthV2InteractionApiResult.ChallengeRequired).continuationState
    }

    /** State produced by the password-method challenge response, carrying the verify href. */
    private fun passwordVerifyState(): NativeAuthV2ContinuationState {
        val result = parser.parseInteraction(
            response = responseFrom(
                """
                {
                  "continuationToken": "ct-2",
                  "action": "verify",
                  "_links": { "verify": { "href": "/tenant/api/v0.1/password/pwd-1/verify" } }
                }
                """.trimIndent()
            ),
            previousState = signInChallengeState(),
            operation = NativeAuthV2Operation.SIGN_IN_PASSWORD_CHALLENGE
        )
        return (result as NativeAuthV2InteractionApiResult.PasswordRequired).continuationState
    }

    private fun responseFrom(json: String): NativeAuthV2HalApiResponse =
        NativeAuthV2HalApiResponse.from(
            halResource = HalResource.from(json),
            statusCode = 200,
            correlationId = CORRELATION_ID
        )

    private fun assertClientException(block: () -> Unit): ClientException {
        try {
            block()
        } catch (e: ClientException) {
            return e
        }
        fail("Expected a ClientException to be thrown.")
        throw IllegalStateException("unreachable")
    }

    private companion object {
        private val parser = NativeAuthV2ResponseParser()
        private const val CLIENT_ID = "client-id"
        private const val CORRELATION_ID = "correlation-id"
        private val SCOPES = listOf("openid", "profile")
        private const val JSON_CONTENT_TYPE = HttpConstants.MediaType.APPLICATION_JSON
    }
}
