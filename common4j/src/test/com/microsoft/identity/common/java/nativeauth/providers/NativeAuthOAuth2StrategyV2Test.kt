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

import com.microsoft.identity.common.java.nativeauth.BuildValues
import com.microsoft.identity.common.java.nativeauth.providers.interactors.JITInteractor
import com.microsoft.identity.common.java.nativeauth.providers.interactors.NativeAuthV2Interactor
import com.microsoft.identity.common.java.nativeauth.providers.interactors.ResetPasswordInteractor
import com.microsoft.identity.common.java.nativeauth.providers.interactors.SignInInteractor
import com.microsoft.identity.common.java.nativeauth.providers.interactors.SignUpInteractor
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.AuthorizeChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
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
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.net.URL

class NativeAuthOAuth2StrategyV2Test {
    @After
    fun tearDown() {
        BuildValues.setDC("")
    }

    private val v2Interactor = mockk<NativeAuthV2Interactor>()
    private val strategy = NativeAuthV2OAuth2Strategy(
        strategyParameters = mockk<OAuth2StrategyParameters>(relaxed = true),
        config = NativeAuthOAuth2Configuration(
            authorityUrl = URL("https://login.contoso.com/tenant"),
            clientId = "client-id",
            challengeType = "oob",
            capabilities = null,
            useMockApiForNativeAuth = false
        ),
        nativeAuthV2Interactor = v2Interactor
    )

    @Test
    fun signInFlow_followsReturnedPasswordAndAbsoluteContinueRoutes() {
        val entryHref = "/tenant-id/api/v2.0/signin/start"
        val challengeHref = "/tenant-id/api/v2.0/password/challenge?dc=service"
        val verifyHref = "/tenant.onmicrosoft.com/api/v2.0/signin/verify?dc=service&x=%2B&x=%2F"
        val continueHref = "https://service.ciamlogin.com/tenant-id/oauth2/v2.0/authorize-challenge?dc=service"
        val urls = mutableListOf<URL>()
        val bodies = mutableListOf<ByteArray>()
        val client = mockk<UrlConnectionHttpClient>()
        every { client.post(capture(urls), any(), capture(bodies)) } returnsMany listOf(
            HttpResponse(401, """{"continuation_token":"entry-token","sign_in":"$entryHref"}""", emptyMap()),
            HttpResponse(200, """{"authorizationCode":"auth-code"}""", emptyMap())
        )
        every {
            client.method(HttpClient.HttpMethod.POST, capture(urls), any(), capture(bodies))
        } returnsMany listOf(
            HttpResponse(200, """{"action":"challenge","continuationToken":"challenge-token",
                "challengeContext":{"authenticationFactor":"singleFactor"},
                "_embedded":{"methods":[{"id":"password","type":"password",
                "_links":{"challenge":{"href":"$challengeHref"}}}]}}""", emptyMap()),
            HttpResponse(200, """{"action":"verify","type":"password","continuationToken":"password-token",
                "_links":{"verify":{"href":"$verifyHref"}}}""", emptyMap()),
            HttpResponse(200, """{"state":"continue","continuationToken":"complete-token",
                "_links":{"continue":{"href":"$continueHref"}}}""", emptyMap())
        )
        val routedStrategy = createRoutingStrategy(client)

        val entry = routedStrategy.performAuthorizeChallengeStart(
            CORRELATION_ID, NativeAuthV2LinkRelation.SIGN_IN.value,
            NativeAuthV2FlowScenario.SIGN_IN, SCOPES
        ) as AuthorizeChallengeApiResult.ContinuationRequired
        val challenge = routedStrategy.performSignInStart(USERNAME, entry.continuationState)
            as NativeAuthV2InteractionApiResult.ChallengeRequired
        val passwordRequired = routedStrategy.performMethodChallenge(challenge.continuationState, "password")
            as NativeAuthV2InteractionApiResult.PasswordRequired
        val password = "test-password".toCharArray()
        val ready = routedStrategy.performPasswordVerify(passwordRequired.continuationState, password)
            as NativeAuthV2InteractionApiResult.ReadyToComplete
        val completed = routedStrategy.performAuthorizeChallengeContinue(ready.continuationState)
            as AuthorizeChallengeApiResult.AuthorizationCode

        assertEquals("auth-code", completed.code)
        assertEquals(
            listOf(
                "https://custom.example.com/alias/oauth2/v2.0/authorize-challenge?dc=configured",
                "https://custom.example.com$entryHref",
                "https://custom.example.com$challengeHref",
                "https://custom.example.com$verifyHref",
                continueHref
            ),
            urls.map { it.toString() }
        )
        val verifyBody = JSONObject(String(bodies[3], Charsets.UTF_8))
        assertEquals("password-token", verifyBody.getString("continuationToken"))
        assertEquals("test-password", verifyBody.getString("password"))
        verify(exactly = 2) { client.post(any(), any(), any()) }
        verify(exactly = 3) { client.method(HttpClient.HttpMethod.POST, any(), any(), any()) }
    }

    @Test
    fun signUpFlow_followsReturnedSelfLinkForAttributesAndAbsoluteContinueRoute() {
        val entryHref = "/tenant-id/api/v2.0/signup/start?dc=service"
        val attributesHref = "/tenant.onmicrosoft.com/api/v2.0/signup/attributes?dc=service&x=a%2Bb&x=c%2Fd"
        val continueHref = "https://service.ciamlogin.com/tenant-id/oauth2/v2.0/authorize-challenge"
        val urls = mutableListOf<URL>()
        val bodies = mutableListOf<ByteArray>()
        val client = mockk<UrlConnectionHttpClient>()
        every { client.post(capture(urls), any(), capture(bodies)) } returnsMany listOf(
            HttpResponse(401, """{"continuation_token":"entry-token","sign_up":"$entryHref"}""", emptyMap()),
            HttpResponse(200, """{"authorizationCode":"auth-code"}""", emptyMap())
        )
        every {
            client.method(HttpClient.HttpMethod.POST, capture(urls), any(), capture(bodies))
        } returnsMany listOf(
            HttpResponse(200, """{"action":"collectAttributes","continuationToken":"attributes-token",
                "attributes":[{"attributeId":"displayName","inputType":"text","required":true}],
                "_links":{"self":{"href":"$attributesHref"}}}""", emptyMap()),
            HttpResponse(200, """{"state":"continue","continuationToken":"complete-token",
                "_links":{"continue":{"href":"$continueHref"}}}""", emptyMap())
        )
        val routedStrategy = createRoutingStrategy(client)

        val entry = routedStrategy.performAuthorizeChallengeStart(
            CORRELATION_ID, NativeAuthV2LinkRelation.SIGN_UP.value,
            NativeAuthV2FlowScenario.SIGN_UP, SCOPES
        ) as AuthorizeChallengeApiResult.ContinuationRequired
        val attributes = routedStrategy.performSignUpStart(USERNAME, entry.continuationState)
            as NativeAuthV2InteractionApiResult.AttributesRequired
        val ready = routedStrategy.performSubmitAttributes(
            attributes.continuationState, mapOf("displayName" to "Test user")
        ) as NativeAuthV2InteractionApiResult.ReadyToComplete
        val completed = routedStrategy.performAuthorizeChallengeContinue(ready.continuationState)
            as AuthorizeChallengeApiResult.AuthorizationCode

        assertEquals("auth-code", completed.code)
        assertEquals(
            listOf(
                "https://custom.example.com/alias/oauth2/v2.0/authorize-challenge?dc=configured",
                "https://custom.example.com$entryHref",
                "https://custom.example.com$attributesHref",
                continueHref
            ),
            urls.map { it.toString() }
        )
        val attributesBody = JSONObject(String(bodies[2], Charsets.UTF_8))
        assertEquals("attributes-token", attributesBody.getString("continuationToken"))
        assertEquals("Test user", attributesBody.getJSONObject("attributes").getString("displayName"))
        verify(exactly = 2) { client.post(any(), any(), any()) }
        verify(exactly = 2) { client.method(HttpClient.HttpMethod.POST, any(), any(), any()) }
    }

    private fun createRoutingStrategy(client: UrlConnectionHttpClient): NativeAuthV2OAuth2Strategy {
        val config = NativeAuthOAuth2Configuration(
            authorityUrl = URL("https://custom.example.com/alias?dc=configured"),
            clientId = "client-id",
            challengeType = "oob",
            capabilities = null,
            useMockApiForNativeAuth = false
        )
        return NativeAuthV2OAuth2Strategy(
            strategyParameters = mockk(relaxed = true),
            config = config,
            nativeAuthV2Interactor = NativeAuthV2Interactor(
                httpClient = client,
                requestProvider = NativeAuthV2RequestProvider(config),
                responseHandler = NativeAuthV2ResponseHandler(),
                responseParser = NativeAuthV2ResponseParser()
            )
        )
    }

    @Test
    fun authorizeChallengeMethods_delegateAndReturnInteractorResults() {
        val state = mockk<NativeAuthV2ContinuationState>()
        val startResult = mockk<AuthorizeChallengeApiResult>()
        val continueResult = mockk<AuthorizeChallengeApiResult>()
        every {
            v2Interactor.performAuthorizeChallengeStart(
                correlationId = CORRELATION_ID,
                entryRelation = NativeAuthV2LinkRelation.RESET_PASSWORD,
                scenario = NativeAuthV2FlowScenario.RESET_PASSWORD,
                scopes = SCOPES
            )
        } returns startResult
        every { v2Interactor.performAuthorizeChallengeContinue(state) } returns continueResult

        assertSame(
            startResult,
            strategy.performAuthorizeChallengeStart(
                correlationId = CORRELATION_ID,
                entryRelation = NativeAuthV2LinkRelation.RESET_PASSWORD.value,
                scenario = NativeAuthV2FlowScenario.RESET_PASSWORD,
                scopes = SCOPES
            )
        )
        assertSame(continueResult, strategy.performAuthorizeChallengeContinue(state))
    }

    @Test
    fun interactionMethods_delegateAndReturnInteractorResults() {
        val state = mockk<NativeAuthV2ContinuationState>()
        val password = "P@ssw0rd!".toCharArray()
        val resetResult = mockk<NativeAuthV2InteractionApiResult>()
        val challengeResult = mockk<NativeAuthV2InteractionApiResult>()
        val riskVerificationResult = mockk<NativeAuthV2InteractionApiResult>()
        val resendResult = mockk<NativeAuthV2InteractionApiResult>()
        val verifyResult = mockk<NativeAuthV2InteractionApiResult>()
        val updateResult = mockk<NativeAuthV2InteractionApiResult>()
        val pollResult = mockk<NativeAuthV2InteractionApiResult>()
        every { v2Interactor.performResetPasswordStart(USERNAME, state) } returns resetResult
        every { v2Interactor.performMethodChallenge(state, "method-id") } returns challengeResult
        every { v2Interactor.performRiskVerification(state) } returns riskVerificationResult
        every { v2Interactor.performResend(state) } returns resendResult
        every { v2Interactor.performVerify(state, OTP) } returns verifyResult
        every { v2Interactor.performUpdatePassword(state, password) } returns updateResult
        every { v2Interactor.performPoll(state) } returns pollResult

        assertSame(resetResult, strategy.performResetPasswordStart(USERNAME, state))
        assertSame(challengeResult, strategy.performMethodChallenge(state, "method-id"))
        assertSame(riskVerificationResult, strategy.performRiskVerification(state))
        assertSame(resendResult, strategy.performResend(state))
        assertSame(verifyResult, strategy.performVerify(state, OTP))
        assertSame(updateResult, strategy.performUpdatePassword(state, password))
        assertSame(pollResult, strategy.performPoll(state))
        verify(exactly = 1) { v2Interactor.performUpdatePassword(state, password) }
    }

    @Test
    fun tokenRequest_delegatesAndReturnsInteractorResult() {
        val result = mockk<SignInTokenApiResult>()
        every {
            v2Interactor.performTokenRequest(
                code = AUTHORIZATION_CODE,
                correlationId = CORRELATION_ID
            )
        } returns result

        assertSame(
            result,
            strategy.performTokenRequest(
                code = AUTHORIZATION_CODE,
                correlationId = CORRELATION_ID
            )
        )
    }

    @Test
    fun configuration_preservesConfiguredDcForInitialV2EntryAndTokenRouting() {
        BuildValues.setDC("westus")
        val config = NativeAuthOAuth2Configuration(
            authorityUrl = URL("https://login.contoso.com/tenant"),
            clientId = "client-id",
            challengeType = "oob",
            capabilities = null,
            useMockApiForNativeAuth = false
        )

        assertEquals(
            URL("https://login.contoso.com/tenant/oauth2/v2.0/authorize-challenge?dc=westus"),
            config.getNativeAuthV2AuthorizeChallengeEndpoint(CORRELATION_ID)
        )
        assertEquals(
            URL("https://login.contoso.com/tenant/oauth2/v2.0/token?dc=westus"),
            config.getSignInTokenEndpoint()
        )
        assertEquals(
            URL("https://login.contoso.com/tenant/oauth2/v2.0/token?dc=westus"),
            config.getNativeAuthV2TokenEndpoint(CORRELATION_ID)
        )
    }

    @Test
    fun factory_createsStrategyWithProvidedConfiguration() {
        val config = NativeAuthOAuth2Configuration(
            authorityUrl = URL("https://login.contoso.com/tenant"),
            clientId = "client-id",
            challengeType = "oob",
            capabilities = null,
            useMockApiForNativeAuth = false
        )

        val created = NativeAuthOAuth2StrategyFactory.createStrategy(
            config = config,
            strategyParameters = mockk(relaxed = true)
        )

        assertSame(config, created.config)
        assertEquals("https://login.contoso.com/tenant", created.getAuthority())
    }

    private companion object {
        private const val CORRELATION_ID = "correlation-id"
        private const val USERNAME = "ada@contoso.com"
        private const val OTP = "123456"
        private const val AUTHORIZATION_CODE = "authorization-code"
        private val SCOPES = listOf("User.Read", "offline_access")
    }
}
