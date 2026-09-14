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
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2FlowScenario
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertEquals
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
        val resendResult = mockk<NativeAuthV2InteractionApiResult>()
        val verifyResult = mockk<NativeAuthV2InteractionApiResult>()
        val updateResult = mockk<NativeAuthV2InteractionApiResult>()
        val pollResult = mockk<NativeAuthV2InteractionApiResult>()
        every { v2Interactor.performResetPasswordStart(USERNAME, state) } returns resetResult
        every { v2Interactor.performMethodChallenge(state, "method-id") } returns challengeResult
        every { v2Interactor.performResend(state) } returns resendResult
        every { v2Interactor.performVerify(state, OTP) } returns verifyResult
        every { v2Interactor.performUpdatePassword(state, password) } returns updateResult
        every { v2Interactor.performPoll(state) } returns pollResult

        assertSame(resetResult, strategy.performResetPasswordStart(USERNAME, state))
        assertSame(challengeResult, strategy.performMethodChallenge(state, "method-id"))
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
                scopes = SCOPES,
                correlationId = CORRELATION_ID
            )
        } returns result

        assertSame(
            result,
            strategy.performTokenRequest(
                code = AUTHORIZATION_CODE,
                scopes = SCOPES,
                correlationId = CORRELATION_ID
            )
        )
    }

    @Test
    fun configuration_buildsV2EndpointsFromAuthorityAndDc() {
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
