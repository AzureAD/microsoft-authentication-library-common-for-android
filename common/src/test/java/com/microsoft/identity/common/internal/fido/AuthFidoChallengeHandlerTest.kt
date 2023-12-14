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

import android.net.Uri
import androidx.lifecycle.testing.TestLifecycleOwner
import com.microsoft.identity.common.java.exception.ClientException
import org.junit.Assert
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthFidoChallengeHandlerTest {
    //Challenge parameters
    val challengeStr = "T1xCsnxM2DNL2KdK5CLa6fMhD7OBqho6syzInk_n-Uo"
    val relyingPartyIdentifier = "login.microsoft.com"
    val userVerificationPolicy = "required"
    val version = "1.0"
    val submitUrl = "https://submiturl"
    val context = "contextValue flowToken"
    val authority = "urn:http-auth:PassKey"

    //Challenge auth parameters
    val allowCredentialsOneUserString = "user"
    val allowCredentialsOneUser = listOf("user")
    val allowCredentialsTwoUsers = listOf("user1","user2")
    val keyTypesString = "passkey"
    val keyTypes = listOf(keyTypesString)

    //Test Exception
    val testException = ClientException("A message")

    //Handler parameters
    val testFidoManager = TestFidoManager()
    val testLifecycleOwner = TestLifecycleOwner()
    lateinit var webView : ExtendedTestWebView
    lateinit var authFidoChallengeHandler: AuthFidoChallengeHandler

    @Before
    fun setUp() {
        webView = ExtendedTestWebView()
        authFidoChallengeHandler = AuthFidoChallengeHandler(
            fidoManager = testFidoManager,
            webView = webView,
            spanContext = null,
            lifecycleOwner = testLifecycleOwner,
        )
    }

    @Test
    fun testProcessChallenge_AuthSuccess() {
        assertFalse(webView.urlLoaded)
        val fullUrl = Uri.Builder().authority(authority)
            .appendQueryParameter(FidoRequestField.CHALLENGE.fieldName, challengeStr)
            .appendQueryParameter(AuthFidoRequestField.ALLOWED_CREDENTIALS.fieldName, allowCredentialsOneUserString)
            .appendQueryParameter(FidoRequestField.RELYING_PARTY_IDENTIFIER.fieldName, relyingPartyIdentifier)
            .appendQueryParameter(FidoRequestField.VERSION.fieldName, version)
            .appendQueryParameter(FidoRequestField.SUBMIT_URL.fieldName, submitUrl)
            .appendQueryParameter(AuthFidoRequestField.KEY_TYPES.fieldName, keyTypesString)
            .appendQueryParameter(FidoRequestField.CONTEXT.fieldName, context)
            .build().toString()
        authFidoChallengeHandler.processChallenge(FidoChallenge(fullUrl))
        assertTrue(webView.urlLoaded)
        assertTrue(webView.isRegularAssertion())
    }

    @Test(expected = ClientException::class)
    fun testProcessChallenge_AuthSubmitUrlValidationError() {
        assertFalse(webView.urlLoaded)
        val fullUrl = Uri.Builder().authority(authority)
            .appendQueryParameter(FidoRequestField.CHALLENGE.fieldName, challengeStr)
            .appendQueryParameter(AuthFidoRequestField.ALLOWED_CREDENTIALS.fieldName, allowCredentialsOneUserString)
            .appendQueryParameter(FidoRequestField.RELYING_PARTY_IDENTIFIER.fieldName, relyingPartyIdentifier)
            .appendQueryParameter(FidoRequestField.VERSION.fieldName, version)
            .appendQueryParameter(FidoRequestField.SUBMIT_URL.fieldName, "someUnexpectedUrl")
            .appendQueryParameter(AuthFidoRequestField.KEY_TYPES.fieldName, keyTypesString)
            .appendQueryParameter(FidoRequestField.CONTEXT.fieldName, context)
            .build().toString()
        authFidoChallengeHandler.processChallenge(FidoChallenge(fullUrl))
    }

    @Test
    fun testProcessChallenge_AuthGeneralValidationError() {
        assertFalse(webView.urlLoaded)
        val fullUrl = Uri.Builder().authority(authority)
            .appendQueryParameter(FidoRequestField.CHALLENGE.fieldName, "")
            .appendQueryParameter(AuthFidoRequestField.ALLOWED_CREDENTIALS.fieldName, allowCredentialsOneUserString)
            .appendQueryParameter(FidoRequestField.RELYING_PARTY_IDENTIFIER.fieldName, relyingPartyIdentifier)
            .appendQueryParameter(FidoRequestField.VERSION.fieldName, version)
            .appendQueryParameter(FidoRequestField.SUBMIT_URL.fieldName, submitUrl)
            .appendQueryParameter(AuthFidoRequestField.KEY_TYPES.fieldName, keyTypesString)
            .appendQueryParameter(FidoRequestField.CONTEXT.fieldName, context)
            .build().toString()
        authFidoChallengeHandler.processChallenge(FidoChallenge(fullUrl))
        assertTrue(webView.urlLoaded)
        assertFalse(webView.isRegularAssertion())
    }

    //Passing a null lifecycleOwner will end the operation before any calls can be made from the manager.
    @Test
    fun testProcessChallenge_AuthNoLifecycleOwner() {
        authFidoChallengeHandler = AuthFidoChallengeHandler(
            fidoManager = testFidoManager,
            webView = webView,
            spanContext = null,
            lifecycleOwner = null
        )
        assertFalse(webView.urlLoaded)
        val fullUrl = Uri.Builder().authority(authority)
            .appendQueryParameter(FidoRequestField.CHALLENGE.fieldName, challengeStr)
            .appendQueryParameter(AuthFidoRequestField.ALLOWED_CREDENTIALS.fieldName, allowCredentialsOneUserString)
            .appendQueryParameter(FidoRequestField.RELYING_PARTY_IDENTIFIER.fieldName, relyingPartyIdentifier)
            .appendQueryParameter(FidoRequestField.VERSION.fieldName, version)
            .appendQueryParameter(FidoRequestField.SUBMIT_URL.fieldName, submitUrl)
            .appendQueryParameter(AuthFidoRequestField.KEY_TYPES.fieldName, keyTypesString)
            .appendQueryParameter(FidoRequestField.CONTEXT.fieldName, context)
            .build().toString()
        authFidoChallengeHandler.processChallenge(FidoChallenge(fullUrl))
        assertTrue(webView.urlLoaded)
        assertFalse(webView.isRegularAssertion())
    }

    //Note that a cancellation by the user also results in an exception thrown by the API.
    @Test
    fun testProcessChallenge_AuthExceptionInManager() {
        assertFalse(webView.urlLoaded)
        val fullUrl = Uri.Builder().authority(authority)
            .appendQueryParameter(FidoRequestField.CHALLENGE.fieldName, TestFidoManager.EXCEPTION_CHALLENGE)
            .appendQueryParameter(AuthFidoRequestField.ALLOWED_CREDENTIALS.fieldName, allowCredentialsOneUserString)
            .appendQueryParameter(FidoRequestField.RELYING_PARTY_IDENTIFIER.fieldName, relyingPartyIdentifier)
            .appendQueryParameter(FidoRequestField.VERSION.fieldName, version)
            .appendQueryParameter(FidoRequestField.SUBMIT_URL.fieldName, submitUrl)
            .appendQueryParameter(AuthFidoRequestField.KEY_TYPES.fieldName, keyTypesString)
            .appendQueryParameter(FidoRequestField.CONTEXT.fieldName, context)
            .build().toString()
        authFidoChallengeHandler.processChallenge(FidoChallenge(fullUrl))
        assertTrue(webView.urlLoaded)
        assertFalse(webView.isRegularAssertion())
    }

    @Test
    fun testRespondToChallenge_RegularContext() {
        authFidoChallengeHandler.respondToChallenge(
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
        authFidoChallengeHandler.respondToChallenge(
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
        authFidoChallengeHandler.respondToChallenge(
            submitUrl,
            TestFidoManager.SAMPLE_ASSERTION,
            ""
        )
        assertTrue(webView.urlLoaded)
        assertFalse(webView.hasContext())
        assertFalse(webView.hasFlowToken())
    }

    @Test
    fun testRespondToChallengeWithError_RegularContext() {
        authFidoChallengeHandler.respondToChallengeWithError(
            submitUrl,
            context,
            testException.message.toString(),
            testException
        )
        assertTrue(webView.urlLoaded)
        assertTrue(webView.hasContext())
        assertTrue(webView.hasFlowToken())
    }

    @Test
    fun testRespondToChallengeWithError_NoFlowTokenInContext() {
        authFidoChallengeHandler.respondToChallengeWithError(
            submitUrl,
            "contextValue",
            testException.message.toString(),
            testException
        )
        assertTrue(webView.urlLoaded)
        assertTrue(webView.hasContext())
        assertFalse(webView.hasFlowToken())
    }

    @Test
    fun testRespondToChallengeWithError_EmptyContext() {
        authFidoChallengeHandler.respondToChallengeWithError(
            submitUrl,
            "",
            testException.message.toString(),
            testException
        )
        assertTrue(webView.urlLoaded)
        assertFalse(webView.hasContext())
        assertFalse(webView.hasFlowToken())
    }

    @Test
    fun testRespondToChallengeWithError_NoException() {
        authFidoChallengeHandler.respondToChallengeWithError(
            submitUrl,
            context,
            testException.message.toString(),
        )
        assertTrue(webView.urlLoaded)
        assertTrue(webView.hasContext())
        assertTrue(webView.hasFlowToken())
    }

    @Test
    fun testValidateRequiredParameter_ExpectedFieldAndValue() {
        Assert.assertEquals(
            challengeStr,
            authFidoChallengeHandler.validateRequiredParameter(
                FidoRequestField.CHALLENGE.fieldName,
                challengeStr
            )
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateRequiredParameter_MissingField() {
        authFidoChallengeHandler.validateRequiredParameter(FidoRequestField.CHALLENGE.fieldName, null)
    }

    @Test(expected = ClientException::class)
    fun testValidateRequiredParameter_EmptyValue() {
        authFidoChallengeHandler.validateRequiredParameter(FidoRequestField.CHALLENGE.fieldName, "")
    }

    @Test
    fun testValidateSubmitUrl_ExpectedFieldAndValue() {
        authFidoChallengeHandler.validateSubmitUrl(submitUrl)
    }

    @Test(expected = ClientException::class)
    fun testValidateSubmitUrl_MissingField() {
        authFidoChallengeHandler.validateSubmitUrl(null)
    }

    @Test(expected = ClientException::class)
    fun testValidateSubmitUrl_EmptyValue() {
        authFidoChallengeHandler.validateSubmitUrl("")
    }

    @Test(expected = ClientException::class)
    fun testValidateSubmitUrl_MalformedUrl() {
        authFidoChallengeHandler.validateSubmitUrl("url")
    }

    @Test
    fun testValidateProtocolVersion_ExpectedFieldAndValue() {
        authFidoChallengeHandler.validateProtocolVersion(version)
    }

    @Test(expected = ClientException::class)
    fun testValidateProtocolVersion_MissingField() {
        authFidoChallengeHandler.validateProtocolVersion(null)
    }

    @Test(expected = ClientException::class)
    fun testValidateProtocolVersion_EmptyValue() {
        authFidoChallengeHandler.validateProtocolVersion("")
    }

    @Test(expected = ClientException::class)
    fun testValidateProtocolVersion_UnsupportedVersion() {
        authFidoChallengeHandler.validateProtocolVersion("2.0")
    }

    @Test
    fun testValidateOptionalListParameter_ExpectedFieldAndValue() {
        Assert.assertEquals(
            allowCredentialsTwoUsers,
            authFidoChallengeHandler.validateOptionalListParameter(
                AuthFidoRequestField.ALLOWED_CREDENTIALS.fieldName,
                allowCredentialsTwoUsers
            )
        )
    }

    @Test
    fun testValidateOptionalListParameter_MissingField() {
        Assert.assertNull(
            authFidoChallengeHandler.validateOptionalListParameter(
                AuthFidoRequestField.ALLOWED_CREDENTIALS.fieldName,
                null
            )
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateOptionalListParameter_EmptyValue() {
        authFidoChallengeHandler.validateOptionalListParameter(AuthFidoRequestField.ALLOWED_CREDENTIALS.fieldName, emptyList())
    }
}
