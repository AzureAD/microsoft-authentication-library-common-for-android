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
import org.junit.Assert.fail
import org.junit.Test
import java.net.URL

/**
 * Covers the V2 sign-up request contract: the entry (`signup/start`) body carries only the
 * continuation token (the username is submitted later as an attribute), and the `submitattributes`
 * body carries the continuation token plus the attribute map. All hrefs come from the server
 * response, never assembled locally, and attribute values are never rendered in either string form.
 */
class NativeAuthV2SignUpRequestProviderTest {

    @Test
    fun createSignUpStartRequest_postsOnlyContinuationTokenToServerHref() {
        val request = provider().createSignUpStartRequest(entryState())

        assertEquals(URL("https://login.contoso.com/tenant/api/v0.1/signup/start"), request.requestUrl)
        assertEquals(JSON_CONTENT_TYPE, request.headers[HttpConstants.HeaderField.CONTENT_TYPE])
        assertEquals(
            mapOf("continuationToken" to "flow-token"),
            jsonBodyOf(request.parameters)
        )
    }

    @Test
    fun createSignUpStartRequest_whenSignUpRelationIsMissing_throwsMissingParameter() {
        val exception = assertClientException {
            provider().createSignUpStartRequest(entryState(signUpHref = null))
        }

        assertEquals(ClientException.MISSING_PARAMETER, exception.errorCode)
        assertEquals(CORRELATION_ID, exception.correlationId)
    }

    @Test
    fun createSubmitAttributesRequest_postsContinuationTokenAndAttributesToServerHref() {
        val request = provider().createSubmitAttributesRequest(
            state = collectAttributesState(),
            attributes = linkedMapOf("city" to "Seattle", "country" to "US")
        )

        assertEquals(
            URL("https://login.contoso.com/tenant/api/v0.1/signup/submitattributes"),
            request.requestUrl
        )
        assertEquals(JSON_CONTENT_TYPE, request.headers[HttpConstants.HeaderField.CONTENT_TYPE])

        val body = JsonParser.parseString(
            ObjectMapper.serializeObjectToJsonString(request.parameters)
        ).asJsonObject
        assertEquals("ct-1", body.get("continuationToken").asString)
        val attributes = body.getAsJsonObject("attributes")
        assertEquals("Seattle", attributes.get("city").asString)
        assertEquals("US", attributes.get("country").asString)
    }

    @Test
    fun createSubmitAttributesRequest_whenSubmitAttributesRelationIsMissing_throwsMissingParameter() {
        val exception = assertClientException {
            provider().createSubmitAttributesRequest(
                state = entryState(),
                attributes = mapOf("city" to "Seattle")
            )
        }

        assertEquals(ClientException.MISSING_PARAMETER, exception.errorCode)
    }

    @Test
    fun createSubmitAttributesRequest_neverRendersAttributeValuesInEitherStringForm() {
        val request = provider().createSubmitAttributesRequest(
            state = collectAttributesState(),
            attributes = mapOf("password" to "Password123!")
        )

        listOf(request.toString(), request.toUnsanitizedString()).forEach { rendered ->
            assertFalse(rendered.contains("Password123!"))
            assertFalse(rendered.contains("ct-1"))
        }
    }

    @Test
    fun createSignUpStartRequest_whenProductionAuthorityIsHttp_rejectsInsecureEndpoint() {
        val exception = assertClientException {
            provider(authorityUrl = "http://login.contoso.com/tenant")
                .createSignUpStartRequest(entryState())
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

    private fun entryState(signUpHref: String? = "/tenant/api/v0.1/signup/start"): NativeAuthV2ContinuationState {
        val signUpProperty = signUpHref?.let { """"sign_up": "$it",""" } ?: ""
        val response = responseFrom(
            """
            {
              $signUpProperty
              "continuation_token": "flow-token"
            }
            """.trimIndent()
        )
        return (parser.parseAuthorizeChallenge(
            response = response,
            entryRelation = NativeAuthV2LinkRelation.SIGN_UP,
            scenario = NativeAuthV2FlowScenario.SIGN_UP,
            scopes = SCOPES
        ) as? AuthorizeChallengeApiResult.ContinuationRequired)?.continuationState
            ?: NativeAuthV2ContinuationState.fromAuthorizeChallengeResponse(
                response = response,
                continuationToken = "flow-token",
                scopes = SCOPES,
                entryRelation = NativeAuthV2LinkRelation.SIGN_UP,
                scenario = NativeAuthV2FlowScenario.SIGN_UP
            )
    }

    /** State produced by a `collectAttributes` response, carrying the submitAttributes href. */
    private fun collectAttributesState(): NativeAuthV2ContinuationState {
        val result = parser.parseInteraction(
            response = responseFrom(
                """
                {
                  "continuationToken": "ct-1",
                  "action": "collectAttributes",
                  "attributes": [ { "attributeId": "city", "inputType": "text", "required": true } ],
                  "_links": { "submitAttributes": { "href": "/tenant/api/v0.1/signup/submitattributes" } }
                }
                """.trimIndent()
            ),
            previousState = entryState(),
            operation = NativeAuthV2Operation.SIGN_UP_START
        )
        return (result as NativeAuthV2InteractionApiResult.AttributesRequired).continuationState
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
