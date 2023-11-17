// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.internal.fido

import androidx.lifecycle.testing.TestLifecycleOwner
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PasskeyFidoChallengeHandlerTest {
    //Challenge parameters
    val challengeStr = "T1xCsnxM2DNL2KdK5CLa6fMhD7OBqho6syzInk_n-Uo"
    val relyingPartyIdentifier = "login.microsoft.com"
    val userVerificationPolicy = "required"
    val version = "1.0"
    val submitUrl = "submiturl"
    val context = "contextValue flowToken"

    //Challenge auth parameters
    val allowCredentialsOneUser = listOf("user")
    val keyTypes = listOf("passkey")

    //Handler parameters
    val testFidoManager = TestFidoManager()
    val testLifecycleOwner = TestLifecycleOwner()
    lateinit var webView : ExtendedTestWebView
    lateinit var passkeyFidoChallengeHandler: PasskeyFidoChallengeHandler

    @Before
    fun setUp() {
        webView = ExtendedTestWebView()
        passkeyFidoChallengeHandler = PasskeyFidoChallengeHandler(
            fidoManager = testFidoManager,
            webView = webView,
            spanContext = null,
            lifecycleOwner = testLifecycleOwner,
        )
    }

    @Test
    fun testProcessChallenge_AuthSuccess() {
        assertFalse(webView.urlLoaded)
        passkeyFidoChallengeHandler.processChallenge(
            AuthFidoChallenge(
                challenge = challengeStr,
                relyingPartyIdentifier = relyingPartyIdentifier,
                userVerificationPolicy = userVerificationPolicy,
                version = version,
                submitUrl = submitUrl,
                context = context,
                keyTypes = keyTypes,
                allowedCredentials = allowCredentialsOneUser
        ))
        assertTrue(webView.urlLoaded)
        assertTrue(webView.isRegularAssertion())
    }

    //Note that a cancellation by the user also results in an exception thrown by the API.
    @Test
    fun testProcessChallenge_AuthExceptionInManager() {
        assertFalse(webView.urlLoaded)
        passkeyFidoChallengeHandler.processChallenge(
            AuthFidoChallenge(
                challenge = TestFidoManager.EXCEPTION_CHALLENGE,
                relyingPartyIdentifier = relyingPartyIdentifier,
                userVerificationPolicy = userVerificationPolicy,
                version = version,
                submitUrl = submitUrl,
                context = context,
                keyTypes = keyTypes,
                allowedCredentials = allowCredentialsOneUser
            ))
        assertTrue(webView.urlLoaded)
        assertFalse(webView.isRegularAssertion())
    }

    //Passing a null lifecycleOwner will end the operation before any calls can be made from the manager.
    @Test
    fun testProcessChallenge_AuthErrorInHandler() {
        passkeyFidoChallengeHandler = PasskeyFidoChallengeHandler(
            fidoManager = testFidoManager,
            webView = webView,
            spanContext = null,
            lifecycleOwner = null
        )
        assertFalse(webView.urlLoaded)
        passkeyFidoChallengeHandler.processChallenge(
            AuthFidoChallenge(
                challenge = challengeStr,
                relyingPartyIdentifier = relyingPartyIdentifier,
                userVerificationPolicy = userVerificationPolicy,
                version = version,
                submitUrl = submitUrl,
                context = context,
                keyTypes = keyTypes,
                allowedCredentials = allowCredentialsOneUser
            ))
        assertTrue(webView.urlLoaded)
        assertFalse(webView.isRegularAssertion())
    }

    @Test
    fun testRespondToChallenge_RegularContext() {
        passkeyFidoChallengeHandler.respondToChallenge(
            submitUrl,
            TestFidoManager.SAMPLE_ASSERTION,
            context
        )
        assertTrue(webView.urlLoaded)
        assertTrue(webView.hasContext())
        assertTrue(webView.hasFlowToken())
    }

    @Test
    fun testRespondToChallenge_NoFlowTokenInContext() {
        passkeyFidoChallengeHandler.respondToChallenge(
            submitUrl,
            TestFidoManager.SAMPLE_ASSERTION,
            "contextValue"
        )
        assertTrue(webView.urlLoaded)
        assertTrue(webView.hasContext())
        assertFalse(webView.hasFlowToken())
    }

    @Test
    fun testRespondToChallenge_EmptyContext() {
        passkeyFidoChallengeHandler.respondToChallenge(
            submitUrl,
            TestFidoManager.SAMPLE_ASSERTION,
            ""
        )
        assertTrue(webView.urlLoaded)
        assertFalse(webView.hasContext())
        assertFalse(webView.hasFlowToken())
    }
}
