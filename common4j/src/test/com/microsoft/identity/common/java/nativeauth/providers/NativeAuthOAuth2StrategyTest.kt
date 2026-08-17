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
import com.microsoft.identity.common.java.nativeauth.providers.interactors.ResetPasswordInteractor
import com.microsoft.identity.common.java.nativeauth.providers.interactors.SignInInteractor
import com.microsoft.identity.common.java.nativeauth.providers.interactors.SignUpInteractor
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Test
import java.net.URL

class NativeAuthOAuth2StrategyTest {

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
