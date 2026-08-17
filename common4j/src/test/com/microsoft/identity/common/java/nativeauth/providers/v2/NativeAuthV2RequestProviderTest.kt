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

import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.AuthorizeChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.HalResource
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2HalApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2LinkRelation
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ResponseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.net.URL

class NativeAuthV2RequestProviderTest {

    @Test
    fun createAuthorizeChallengeStartRequest_whenProductionAuthorityIsHttp_rejectsInsecureEndpoint() {
        val exception = assertClientException {
            provider(
                authorityUrl = "http://login.contoso.com/tenant",
                useMockApi = false
            ).createAuthorizeChallengeStartRequest(CORRELATION_ID)
        }

        assertEquals(ClientException.UNSUPPORTED_URL, exception.errorCode)
        assertEquals(CORRELATION_ID, exception.correlationId)
    }

    @Test
    fun createAuthorizeChallengeContinueRequest_whenProductionAuthorityIsHttp_rejectsInsecureEndpoint() {
        val exception = assertClientException {
            provider(
                authorityUrl = "http://login.contoso.com/tenant",
                useMockApi = false
            ).createAuthorizeChallengeContinueRequest(continuationState())
        }

        assertEquals(ClientException.UNSUPPORTED_URL, exception.errorCode)
        assertEquals(CORRELATION_ID, exception.correlationId)
    }

    @Test
    fun createTokenRequest_whenProductionAuthorityIsHttp_rejectsInsecureEndpoint() {
        val exception = assertClientException {
            provider(
                authorityUrl = "http://login.contoso.com/tenant",
                useMockApi = false
            ).createTokenRequest(code = "authorization-code", scopes = SCOPES, correlationId = CORRELATION_ID)
        }

        assertEquals(ClientException.UNSUPPORTED_URL, exception.errorCode)
        assertEquals(CORRELATION_ID, exception.correlationId)
    }

    @Test
    fun createChallengeRequest_whenProductionAuthorityIsHttp_rejectsResolvedRelativeHref() {
        val exception = assertClientException {
            provider(
                authorityUrl = "http://login.contoso.com/tenant",
                useMockApi = false
            ).createChallengeRequest(continuationState())
        }

        assertEquals(ClientException.UNSUPPORTED_URL, exception.errorCode)
        assertEquals(CORRELATION_ID, exception.correlationId)
    }

    @Test
    fun createRequests_whenMockAuthorityIsHttp_allowsExplicitMockEndpoints() {
        val provider = provider(
            authorityUrl = "https://login.contoso.com/tenant",
            useMockApi = true,
            mockAuthorityUrlWithTenant = "http://localhost/mock-tenant"
        )

        val startRequest = provider.createAuthorizeChallengeStartRequest(CORRELATION_ID)
        val continueRequest = provider.createAuthorizeChallengeContinueRequest(continuationState())
        val challengeRequest = provider.createChallengeRequest(continuationState())
        val tokenRequest = provider.createTokenRequest(
            code = "authorization-code",
            scopes = SCOPES,
            correlationId = CORRELATION_ID
        )

        assertEquals(
            URL("http://localhost/mock-tenant/oauth2/v2.0/authorize-challenge"),
            startRequest.requestUrl
        )
        assertEquals(startRequest.requestUrl, continueRequest.requestUrl)
        assertEquals(
            URL("http://localhost/mock-tenant/api/v0.1/auth/challenge"),
            challengeRequest.requestUrl
        )
        assertEquals(
            URL("http://localhost/mock-tenant/oauth2/v2.0/token"),
            tokenRequest.requestUrl
        )
    }

    private fun provider(
        authorityUrl: String,
        useMockApi: Boolean,
        mockAuthorityUrlWithTenant: String = "https://localhost/mock-tenant"
    ): NativeAuthV2RequestProvider {
        val config = NativeAuthOAuth2Configuration(
            authorityUrl = URL(authorityUrl),
            clientId = CLIENT_ID,
            challengeType = CHALLENGE_TYPE,
            capabilities = null,
            requestInterceptor = null,
            useMockApiForNativeAuth = useMockApi,
            MOCK_API_URL_WITH_NATIVE_AUTH_TENANT = mockAuthorityUrlWithTenant
        )
        return NativeAuthV2RequestProvider(config)
    }

    private fun continuationState() =
        (NativeAuthV2ResponseParser().parseAuthorizeChallenge(
            response = NativeAuthV2HalApiResponse.from(
                halResource = HalResource.from(
                    """
                    {
                      "continuation_token": "flow-token",
                      "sign_in": "/oauth2/v2.0/authorize-challenge",
                      "_links": {
                        "challenge": {
                          "href": "/api/v0.1/auth/challenge"
                        }
                      }
                    }
                    """.trimIndent()
                ),
                statusCode = 401,
                correlationId = CORRELATION_ID
            ),
            entryRelation = NativeAuthV2LinkRelation.SIGN_IN,
            scopes = SCOPES
        ) as AuthorizeChallengeApiResult.ContinuationRequired).continuationState

    private fun assertClientException(block: () -> Unit): ClientException {
        try {
            block()
            fail("Expected ClientException")
        } catch (exception: ClientException) {
            return exception
        }
        throw AssertionError("Unreachable")
    }

    private companion object {
        private const val CLIENT_ID = "client-id"
        private const val CHALLENGE_TYPE = "oob"
        private const val CORRELATION_ID = "correlation-id"
        private val SCOPES = listOf("openid", "profile")
    }
}
