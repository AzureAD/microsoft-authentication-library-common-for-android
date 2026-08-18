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

import com.microsoft.identity.common.java.nativeauth.providers.interactors.NativeAuthV2Interactor
import com.microsoft.identity.common.java.nativeauth.providers.interactors.JITInteractor
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
import org.junit.Assert.assertNotSame
import org.junit.Test
import java.net.URL

class NativeAuthOAuth2StrategyTest {

    @Test
    fun nativeAuthV2Configuration_doesNotExposeUnguardedAuthorizeChallengeEndpoint() {
        assertFalse(
            NativeAuthOAuth2Configuration::class.java.methods.any {
                it.name == "getAuthorizeChallengeEndpoint"
            }
        )
    }

    @Test
    fun secondaryConstructor_createsDedicatedNativeAuthV2ClientWithoutRedirects() {
        val strategy = NativeAuthOAuth2Strategy(
            strategyParameters = OAuth2StrategyParameters.builder().build(),
            config = config(),
            signInInteractor = mockk<SignInInteractor>(relaxed = true),
            signUpInteractor = mockk<SignUpInteractor>(relaxed = true),
            resetPasswordInteractor = mockk<ResetPasswordInteractor>(relaxed = true),
            jitInteractor = mockk<JITInteractor>(relaxed = true)
        )

        val v2Client = getNativeAuthV2Client(strategy)

        assertNotSame(UrlConnectionHttpClient.getDefaultInstance(), v2Client)
        assertFalse(readFollowRedirects(v2Client))
    }

    @Test
    fun createStrategy_createsDedicatedNativeAuthV2ClientWithoutRedirects() {
        val strategy = NativeAuthOAuth2StrategyFactory.createStrategy(
            config = config(),
            strategyParameters = OAuth2StrategyParameters.builder().build()
        )

        val v2Client = getNativeAuthV2Client(strategy)

        assertNotSame(UrlConnectionHttpClient.getDefaultInstance(), v2Client)
        assertFalse(readFollowRedirects(v2Client))
    }

    @Test
    fun performAuthorizeChallengeStart_exposesUnmangledJavaSignatureWithStringEntryRelation() {
        val method = NativeAuthOAuth2Strategy::class.java.getMethod(
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

        val strategy = NativeAuthOAuth2Strategy(
            strategyParameters = OAuth2StrategyParameters.builder().build(),
            config = config(),
            signInInteractor = mockk<SignInInteractor>(relaxed = true),
            signUpInteractor = mockk<SignUpInteractor>(relaxed = true),
            resetPasswordInteractor = mockk<ResetPasswordInteractor>(relaxed = true),
            jitInteractor = mockk<JITInteractor>(relaxed = true),
            nativeAuthV2Interactor = nativeAuthV2Interactor
        )

        val method = NativeAuthOAuth2Strategy::class.java.getMethod(
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

    private fun config() = NativeAuthOAuth2Configuration(
        authorityUrl = URL("https://login.contoso.com/tenant"),
        clientId = "client-id",
        challengeType = "oob",
        capabilities = null
    )

    private fun getNativeAuthV2Client(strategy: NativeAuthOAuth2Strategy): Any {
        val interactorField = NativeAuthOAuth2Strategy::class.java.getDeclaredField("nativeAuthV2Interactor")
        interactorField.isAccessible = true
        val interactor = interactorField.get(strategy)

        val clientField = interactor.javaClass.getDeclaredField("httpClient")
        clientField.isAccessible = true
        return clientField.get(interactor)
    }

    private fun readFollowRedirects(client: Any): Boolean {
        val followRedirectsField = client.javaClass.getDeclaredField("followRedirects")
        followRedirectsField.isAccessible = true
        return followRedirectsField.getBoolean(client)
    }
}
