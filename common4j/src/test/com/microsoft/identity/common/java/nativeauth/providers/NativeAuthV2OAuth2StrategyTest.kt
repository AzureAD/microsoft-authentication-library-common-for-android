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
package com.microsoft.identity.common.java.nativeauth.providers

import com.microsoft.identity.common.java.nativeauth.providers.interactors.JITInteractor
import com.microsoft.identity.common.java.nativeauth.providers.interactors.NativeAuthV2Interactor
import com.microsoft.identity.common.java.nativeauth.providers.interactors.ResetPasswordInteractor
import com.microsoft.identity.common.java.nativeauth.providers.interactors.SignInInteractor
import com.microsoft.identity.common.java.nativeauth.providers.interactors.SignUpInteractor
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.AuthorizeChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2InteractionApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2LinkRelation
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ResponseParser
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2FlowScenario
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2RequestProvider
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2ResponseHandler
import com.microsoft.identity.common.java.net.HttpClient
import com.microsoft.identity.common.java.net.HttpResponse
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class NativeAuthV2OAuth2StrategyTest {

    @Test
    fun flowRequestsSendScopesAndClaimsOnlyOnInitialAuthorizeChallenge() {
        val correlationId = "correlation-id"
        val scopes = listOf("User.Read", "openid", "profile", "offline_access")
        for ((relation, scenario) in listOf(
            NativeAuthV2LinkRelation.SIGN_IN to NativeAuthV2FlowScenario.SIGN_IN,
            NativeAuthV2LinkRelation.SIGN_UP to NativeAuthV2FlowScenario.SIGN_UP,
            NativeAuthV2LinkRelation.RESET_PASSWORD to NativeAuthV2FlowScenario.RESET_PASSWORD
        )) {
            val bodies = mutableListOf<ByteArray>()
            val urls = mutableListOf<URL>()
            val httpClient = mockk<UrlConnectionHttpClient>()
            every { httpClient.post(any(), any(), any()) } answers {
                urls.add(firstArg())
                bodies.add(thirdArg<ByteArray>())
                when (bodies.size) {
                    1 -> HttpResponse(401, """
                        {"continuation_token":"initial","_links":{
                          "${relation.value}":{"href":"/tenant/api/v2.0/auth/${relation.value}/start"}
                        }}
                    """.trimIndent(), emptyMap())
                    3 -> HttpResponse(200, """{"code":"authorization-code"}""", emptyMap())
                    4 -> HttpResponse(200, """
                        {"access_token":"test-access-token","scope":"User.Read",
                         "token_type":"Bearer","expires_in":3600}
                    """.trimIndent(), emptyMap())
                    else -> error("Unexpected request in the test flow")
                }
            }
            every { httpClient.method(HttpClient.HttpMethod.POST, any(), any(), any()) } answers {
                urls.add(secondArg())
                bodies.add(arg<ByteArray>(3))
                HttpResponse(200, """{"state":"continue","continuation_token":"completion"}""", emptyMap())
            }
            val config = config()
            val strategy = NativeAuthV2OAuth2Strategy(
                strategyParameters = OAuth2StrategyParameters.builder().build(),
                config = config,
                nativeAuthV2Interactor = NativeAuthV2Interactor(
                    httpClient, NativeAuthV2RequestProvider(config),
                    NativeAuthV2ResponseHandler(), NativeAuthV2ResponseParser()
                )
            )

            val start = strategy.performAuthorizeChallengeStart(
                correlationId, relation.value, scenario, scopes, CLAIMS_REQUEST_JSON
            ) as AuthorizeChallengeApiResult.ContinuationRequired
            val entry = when (relation) {
                NativeAuthV2LinkRelation.SIGN_IN -> strategy.performSignInStart("user@contoso.com", start.continuationState)
                NativeAuthV2LinkRelation.SIGN_UP -> strategy.performSignUpStart("user@contoso.com", start.continuationState)
                else -> strategy.performResetPasswordStart("user@contoso.com", start.continuationState)
            } as NativeAuthV2InteractionApiResult.ReadyToComplete
            assertEquals(scopes, entry.continuationState.scopesForTokenRequest())
            assertEquals(CLAIMS_REQUEST_JSON, entry.continuationState.claimsRequestJsonForTokenRequest())
            val continuation = strategy.performAuthorizeChallengeContinue(entry.continuationState)
                as AuthorizeChallengeApiResult.AuthorizationCode
            val token = strategy.performTokenRequest(continuation.code, correlationId)
                as SignInTokenApiResult.Success

            assertEquals("User.Read", token.tokenResponse.scope)
            val initialForm = parseFormBody(bodies[0])
            assertEquals(scopes.joinToString(" "), initialForm["scope"])
            assertEquals(CLAIMS_REQUEST_JSON, initialForm["claims"])
            val entryBody = String(bodies[1], StandardCharsets.UTF_8)
            assertFalse(entryBody.contains("\"scope\""))
            assertFalse(entryBody.contains("\"claims\""))
            assertEquals(mapOf("continuation_token" to "completion"), parseFormBody(bodies[2]))
            assertTrue(urls.last().path.endsWith("/oauth2/v2.0/token"))
            assertEquals(
                mapOf("client_id" to "client-id", "grant_type" to "authorization_code",
                    "code" to "authorization-code", "client_info" to "true"),
                parseFormBody(bodies[3])
            )
            verify(exactly = 3) { httpClient.post(any(), any(), any()) }
            verify(exactly = 1) { httpClient.method(HttpClient.HttpMethod.POST, any(), any(), any()) }
        }
    }

    private fun parseFormBody(body: ByteArray): Map<String, String> =
        String(body, StandardCharsets.UTF_8).split("&").associate { parameter ->
            val (name, value) = parameter.split("=", limit = 2)
            URLDecoder.decode(name, StandardCharsets.UTF_8.name()) to
                URLDecoder.decode(value, StandardCharsets.UTF_8.name())
        }

    @Test
    fun nativeAuthV2Configuration_doesNotExposeUnguardedAuthorizeChallengeEndpoint() {
        assertFalse(
            NativeAuthOAuth2Configuration::class.java.methods.any {
                it.name == "getAuthorizeChallengeEndpoint"
            }
        )
    }

    @Test
    fun createV2Strategy_reusesDefaultHttpClient() {
        val strategy = NativeAuthOAuth2StrategyFactory.createV2Strategy(
            config = config(),
            strategyParameters = OAuth2StrategyParameters.builder().build()
        )

        val v2Client = getNativeAuthV2Client(strategy)

        assertSame(UrlConnectionHttpClient.getDefaultInstance(), v2Client)
    }

    @Test
    fun nativeAuthV2Strategy_doesNotIncludeV1Interactors() {
        val v1Interactors = setOf(
            SignInInteractor::class.java,
            SignUpInteractor::class.java,
            ResetPasswordInteractor::class.java,
            JITInteractor::class.java
        )

        assertFalse(
            NativeAuthV2OAuth2Strategy::class.java.declaredFields.any {
                it.type in v1Interactors
            }
        )
    }

    @Test
    fun getAuthority_returnsConfiguredAuthority() {
        val strategy = NativeAuthOAuth2StrategyFactory.createV2Strategy(
            config = config(),
            strategyParameters = OAuth2StrategyParameters.builder().build()
        )

        assertEquals("https://login.contoso.com/tenant", strategy.getAuthority())
    }

    @Test
    fun getIssuerCacheIdentifierFromTokenEndpoint_whenUsingMock_returnsMockIdentifier() {
        val strategy = NativeAuthOAuth2StrategyFactory.createV2Strategy(
            config = config(useMockApi = true),
            strategyParameters = OAuth2StrategyParameters.builder().build()
        )

        assertEquals("login.windows.net", strategy.getIssuerCacheIdentifierFromTokenEndpoint())
    }

    @Test
    fun performAuthorizeChallengeStart_exposesUnmangledJavaSignatureWithStringEntryRelation() {
        val method = NativeAuthV2OAuth2Strategy::class.java.getMethod(
            "performAuthorizeChallengeStart",
            String::class.java,
            String::class.java,
            NativeAuthV2FlowScenario::class.java,
            List::class.java
        )

        assertNotNull(method)
        assertEquals(
            listOf(
                String::class.java,
                String::class.java,
                NativeAuthV2FlowScenario::class.java,
                List::class.java
            ),
            method.parameterTypes.toList()
        )
    }

    @Test
    fun performAuthorizeChallengeStart_wrapsStringEntryRelationBeforeDelegatingToInteractor() {
        val nativeAuthV2Interactor = mockk<NativeAuthV2Interactor>()
        every {
            nativeAuthV2Interactor.performAuthorizeChallengeStart(
                correlationId = "correlation-id",
                entryRelation = NativeAuthV2LinkRelation.SIGN_IN,
                scenario = NativeAuthV2FlowScenario.RESET_PASSWORD,
                scopes = listOf("openid")
            )
        } returns AuthorizeChallengeApiResult.Redirect(
            correlationId = "correlation-id",
            redirectReason = "redirect_to_web"
        )

        val strategy = NativeAuthV2OAuth2Strategy(
            strategyParameters = OAuth2StrategyParameters.builder().build(),
            config = config(),
            nativeAuthV2Interactor = nativeAuthV2Interactor
        )

        val method = NativeAuthV2OAuth2Strategy::class.java.getMethod(
            "performAuthorizeChallengeStart",
            String::class.java,
            String::class.java,
            NativeAuthV2FlowScenario::class.java,
            List::class.java
        )

        val result = method.invoke(strategy, "correlation-id", "signIn", NativeAuthV2FlowScenario.RESET_PASSWORD, listOf("openid"))

        assertEquals(
            AuthorizeChallengeApiResult.Redirect(
                correlationId = "correlation-id",
                redirectReason = "redirect_to_web"
            ),
            result
        )
        verify(exactly = 1) {
            nativeAuthV2Interactor.performAuthorizeChallengeStart(
                correlationId = "correlation-id",
                entryRelation = NativeAuthV2LinkRelation.SIGN_IN,
                scenario = NativeAuthV2FlowScenario.RESET_PASSWORD,
                scopes = listOf("openid")
            )
        }
    }

    @Test
    fun performAuthorizeChallengeStart_withClaims_delegatesClaimsToInteractor() {
        val nativeAuthV2Interactor = mockk<NativeAuthV2Interactor>()
        every {
            nativeAuthV2Interactor.performAuthorizeChallengeStart(
                correlationId = "correlation-id",
                entryRelation = NativeAuthV2LinkRelation.SIGN_IN,
                scenario = NativeAuthV2FlowScenario.RESET_PASSWORD,
                scopes = listOf("openid"),
                claimsRequestJson = CLAIMS_REQUEST_JSON
            )
        } returns AuthorizeChallengeApiResult.Redirect(
            correlationId = "correlation-id",
            redirectReason = "redirect_to_web"
        )

        val strategy = NativeAuthV2OAuth2Strategy(
            strategyParameters = OAuth2StrategyParameters.builder().build(),
            config = config(),
            nativeAuthV2Interactor = nativeAuthV2Interactor
        )

        val result = strategy.performAuthorizeChallengeStart(
            correlationId = "correlation-id",
            entryRelation = "signIn",
            scenario = NativeAuthV2FlowScenario.RESET_PASSWORD,
            scopes = listOf("openid"),
            claimsRequestJson = CLAIMS_REQUEST_JSON
        )

        assertEquals(
            AuthorizeChallengeApiResult.Redirect(
                correlationId = "correlation-id",
                redirectReason = "redirect_to_web"
            ),
            result
        )
        verify(exactly = 1) {
            nativeAuthV2Interactor.performAuthorizeChallengeStart(
                correlationId = "correlation-id",
                entryRelation = NativeAuthV2LinkRelation.SIGN_IN,
                scenario = NativeAuthV2FlowScenario.RESET_PASSWORD,
                scopes = listOf("openid"),
                claimsRequestJson = CLAIMS_REQUEST_JSON
            )
        }
    }

    private fun config(useMockApi: Boolean = false) = NativeAuthOAuth2Configuration(
        authorityUrl = URL("https://login.contoso.com/tenant"),
        clientId = "client-id",
        challengeType = "oob",
        capabilities = null,
        useMockApiForNativeAuth = useMockApi,
        MOCK_API_URL_WITH_NATIVE_AUTH_TENANT = "http://localhost/mock-tenant"
    )

    private companion object {
        private const val CLAIMS_REQUEST_JSON = """{"access_token":{"xms_cc":{"values":["cp1"]}}}"""
    }

    private fun getNativeAuthV2Client(strategy: NativeAuthV2OAuth2Strategy): Any {
        val interactorField = NativeAuthV2OAuth2Strategy::class.java.getDeclaredField("nativeAuthV2Interactor")
        interactorField.isAccessible = true
        val interactor = interactorField.get(strategy)

        val clientField = interactor.javaClass.getDeclaredField("httpClient")
        clientField.isAccessible = true
        return clientField.get(interactor)
    }

}
