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
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2LinkRelation
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.net.URL

class NativeAuthV2OAuth2StrategyTest {

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
            List::class.java
        )

        assertNotNull(method)
        assertEquals(
            listOf(String::class.java, String::class.java, List::class.java),
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
            List::class.java
        )

        val result = method.invoke(strategy, "correlation-id", "signIn", listOf("openid"))

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
