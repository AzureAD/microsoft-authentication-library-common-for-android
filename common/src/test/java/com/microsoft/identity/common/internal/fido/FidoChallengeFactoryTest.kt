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
import com.microsoft.identity.common.internal.fido.FidoChallengeFactory.Companion.DEFAULT_USER_VERIFICATION_POLICY
import com.microsoft.identity.common.internal.fido.FidoChallengeFactory.Companion.DELIMITER
import com.microsoft.identity.common.java.exception.ClientException
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

//Robolectric used for Uri dependency
@RunWith(RobolectricTestRunner::class)
class FidoChallengeFactoryTest {
    val challengeStr = "T1xCsnxM2DNL2KdK5CLa6fMhD7OBqho6syzInk_n-Uo"
    val relyingPartyIdentifier = "login.microsoft.com"
    val otherUserVerificationPolicy = "preferred"
    val version = "1.0"
    val submitUrl = "submiturl"
    val context = "123456"
    val authority = "urn:http-auth:PassKey"

    //Auth
    val allowCredentialsEmpty = ""
    val allowCredentialsOneUser = "user1"
    val allowCredentialsTwoUsers = "user1,user2"
    val keyTypes = "passkey"
    val keyTypesEmpty = ""

    @Test
    fun testCreateFidoChallengeFromRedirect_AuthAllowCredentialsOneUser() {
        val fullUrl = Uri.Builder().authority(authority)
            .appendQueryParameter(FidoRequestField.CHALLENGE, challengeStr)
            .appendQueryParameter(AuthFidoRequestField.ALLOWED_CREDENTIALS, allowCredentialsOneUser)
            .appendQueryParameter(FidoRequestField.RELYING_PARTY_IDENTIFIER, relyingPartyIdentifier)
            .appendQueryParameter(FidoRequestField.VERSION, version)
            .appendQueryParameter(FidoRequestField.SUBMIT_URL, submitUrl)
            .appendQueryParameter(AuthFidoRequestField.KEY_TYPES, keyTypes)
            .appendQueryParameter(FidoRequestField.CONTEXT, context)
            .build().toString()
        val fidoChallenge = FidoChallengeFactory.createFidoChallengeFromRedirect(fullUrl)
        assertTrue(fidoChallenge is AuthFidoChallenge)
        val authFidoChallenge : AuthFidoChallenge = fidoChallenge as AuthFidoChallenge
        assertEquals(authFidoChallenge.userVerificationPolicy, DEFAULT_USER_VERIFICATION_POLICY)
        assertNotNull(authFidoChallenge.allowedCredentials)
        authFidoChallenge.allowedCredentials?.let {
            assertTrue(it.size == 1)
        }
    }

    @Test
    fun testCreateFidoChallengeFromRedirect_AuthSingleAllowCredentialsTwoUsers() {
        val fullUrl = Uri.Builder().authority(authority)
            .appendQueryParameter(FidoRequestField.CHALLENGE, challengeStr)
            .appendQueryParameter(AuthFidoRequestField.ALLOWED_CREDENTIALS, allowCredentialsTwoUsers)
            .appendQueryParameter(FidoRequestField.RELYING_PARTY_IDENTIFIER, relyingPartyIdentifier)
            .appendQueryParameter(FidoRequestField.VERSION, version)
            .appendQueryParameter(FidoRequestField.SUBMIT_URL, submitUrl)
            .appendQueryParameter(AuthFidoRequestField.KEY_TYPES, keyTypes)
            .appendQueryParameter(FidoRequestField.CONTEXT, context)
            .build().toString()
        val fidoChallenge = FidoChallengeFactory.createFidoChallengeFromRedirect(fullUrl)
        assertTrue(fidoChallenge is AuthFidoChallenge)
        val authFidoChallenge : AuthFidoChallenge = fidoChallenge as AuthFidoChallenge
        assertEquals(authFidoChallenge.userVerificationPolicy, DEFAULT_USER_VERIFICATION_POLICY)
        assertNotNull(authFidoChallenge.allowedCredentials)
        authFidoChallenge.allowedCredentials?.let {
            assertTrue(it.size == 2)
        }
    }

    @Test
    fun testCreateFidoChallengeFromRedirect_AuthNoAllowedCredentials() {
        val url = Uri.Builder().authority(authority)
            .appendQueryParameter(FidoRequestField.CHALLENGE, challengeStr)
            .appendQueryParameter(FidoRequestField.RELYING_PARTY_IDENTIFIER, relyingPartyIdentifier)
            .appendQueryParameter(FidoRequestField.VERSION, version)
            .appendQueryParameter(FidoRequestField.SUBMIT_URL, submitUrl)
            .appendQueryParameter(AuthFidoRequestField.KEY_TYPES, keyTypes)
            .appendQueryParameter(FidoRequestField.CONTEXT, context)
            .build().toString()
        val fidoChallenge = FidoChallengeFactory.createFidoChallengeFromRedirect(url)
        assertTrue(fidoChallenge is AuthFidoChallenge)
        val authFidoChallenge : AuthFidoChallenge = fidoChallenge as AuthFidoChallenge
        assertEquals(authFidoChallenge.userVerificationPolicy, DEFAULT_USER_VERIFICATION_POLICY)
        assertNull(authFidoChallenge.allowedCredentials)
    }

    @Test
    fun testCreateFidoChallengeFromRedirect_AuthNoKeyTypes() {
        val url = Uri.Builder().authority(authority)
            .appendQueryParameter(FidoRequestField.CHALLENGE, challengeStr)
            .appendQueryParameter(AuthFidoRequestField.ALLOWED_CREDENTIALS, allowCredentialsOneUser)
            .appendQueryParameter(FidoRequestField.RELYING_PARTY_IDENTIFIER, relyingPartyIdentifier)
            .appendQueryParameter(FidoRequestField.VERSION, version)
            .appendQueryParameter(FidoRequestField.SUBMIT_URL, submitUrl)
            .appendQueryParameter(FidoRequestField.CONTEXT, context)
            .build().toString()
        val fidoChallenge = FidoChallengeFactory.createFidoChallengeFromRedirect(url)
        assertTrue(fidoChallenge is AuthFidoChallenge)
        val authFidoChallenge : AuthFidoChallenge = fidoChallenge as AuthFidoChallenge
        assertEquals(authFidoChallenge.userVerificationPolicy, DEFAULT_USER_VERIFICATION_POLICY)
        assertNull(authFidoChallenge.keyTypes)
    }

    @Test
    fun testCreateFidoChallengeFromRedirect_AuthOnlyRequiredParams() {
        val url = Uri.Builder().authority(authority)
            .appendQueryParameter(FidoRequestField.CHALLENGE, challengeStr)
            .appendQueryParameter(FidoRequestField.RELYING_PARTY_IDENTIFIER, relyingPartyIdentifier)
            .appendQueryParameter(FidoRequestField.VERSION, version)
            .appendQueryParameter(FidoRequestField.SUBMIT_URL, submitUrl)
            .appendQueryParameter(FidoRequestField.CONTEXT, context)
            .build().toString()
        val fidoChallenge = FidoChallengeFactory.createFidoChallengeFromRedirect(url)
        assertTrue(fidoChallenge is AuthFidoChallenge)
        val authFidoChallenge : AuthFidoChallenge = fidoChallenge as AuthFidoChallenge
        assertEquals(authFidoChallenge.userVerificationPolicy, DEFAULT_USER_VERIFICATION_POLICY)
        assertNull(authFidoChallenge.allowedCredentials)
        assertNull(authFidoChallenge.keyTypes)
    }

    @Test(expected = ClientException::class)
    fun testCreateFidoChallengeFromRedirect_AuthNoChallenge() {
        val malformedUrl = Uri.Builder().authority(authority)
            .appendQueryParameter(AuthFidoRequestField.ALLOWED_CREDENTIALS, allowCredentialsOneUser)
            .appendQueryParameter(FidoRequestField.RELYING_PARTY_IDENTIFIER, relyingPartyIdentifier)
            .appendQueryParameter(FidoRequestField.VERSION, version)
            .appendQueryParameter(FidoRequestField.SUBMIT_URL, submitUrl)
            .appendQueryParameter(AuthFidoRequestField.KEY_TYPES, keyTypes)
            .appendQueryParameter(FidoRequestField.CONTEXT, context)
            .build().toString()
        FidoChallengeFactory.createFidoChallengeFromRedirect(malformedUrl)
    }

    @Test(expected = ClientException::class)
    fun testCreateFidoChallengeFromRedirect_AuthNoRelyingPartyIdentifier() {
        val malformedUrl = Uri.Builder().authority(authority)
            .appendQueryParameter(FidoRequestField.CHALLENGE, challengeStr)
            .appendQueryParameter(AuthFidoRequestField.ALLOWED_CREDENTIALS, allowCredentialsOneUser)
            .appendQueryParameter(FidoRequestField.VERSION, version)
            .appendQueryParameter(FidoRequestField.SUBMIT_URL, submitUrl)
            .appendQueryParameter(AuthFidoRequestField.KEY_TYPES, keyTypes)
            .appendQueryParameter(FidoRequestField.CONTEXT, context)
            .build().toString()
        FidoChallengeFactory.createFidoChallengeFromRedirect(malformedUrl)
    }

    //Note that this case shouldn't really happen for passkey protocol 1.0,
    // as ESTS assumes userVerificationPolicy is always "required".
    fun testCreateFidoChallengeFromRedirect_AuthSettingDifferentUserVerificationPolicy() {
        val url = Uri.Builder().authority(authority)
            .appendQueryParameter(FidoRequestField.CHALLENGE, challengeStr)
            .appendQueryParameter(AuthFidoRequestField.ALLOWED_CREDENTIALS, allowCredentialsOneUser)
            .appendQueryParameter(FidoRequestField.RELYING_PARTY_IDENTIFIER, relyingPartyIdentifier)
            .appendQueryParameter(FidoRequestField.USER_VERIFICATION_POLICY, otherUserVerificationPolicy)
            .appendQueryParameter(FidoRequestField.VERSION, version)
            .appendQueryParameter(FidoRequestField.SUBMIT_URL, submitUrl)
            .appendQueryParameter(AuthFidoRequestField.KEY_TYPES, keyTypes)
            .appendQueryParameter(FidoRequestField.CONTEXT, context)
            .build().toString()
        val fidoChallenge =FidoChallengeFactory.createFidoChallengeFromRedirect(url)
        assertTrue(fidoChallenge is AuthFidoChallenge)
        val authFidoChallenge : AuthFidoChallenge = fidoChallenge as AuthFidoChallenge
        assertEquals(authFidoChallenge.allowedCredentials, otherUserVerificationPolicy)
    }

    @Test(expected = ClientException::class)
    fun testCreateFidoChallengeFromRedirect_AuthNoVersion() {
        val malformedUrl = Uri.Builder().authority(authority)
            .appendQueryParameter(FidoRequestField.CHALLENGE, challengeStr)
            .appendQueryParameter(AuthFidoRequestField.ALLOWED_CREDENTIALS, allowCredentialsOneUser)
            .appendQueryParameter(FidoRequestField.RELYING_PARTY_IDENTIFIER, relyingPartyIdentifier)
            .appendQueryParameter(FidoRequestField.SUBMIT_URL, submitUrl)
            .appendQueryParameter(AuthFidoRequestField.KEY_TYPES, keyTypes)
            .appendQueryParameter(FidoRequestField.CONTEXT, context)
            .build().toString()
        FidoChallengeFactory.createFidoChallengeFromRedirect(malformedUrl)
    }

    @Test(expected = ClientException::class)
    fun testCreateFidoChallengeFromRedirect_AuthNoSubmitUrl() {
        val malformedUrl = Uri.Builder().authority(authority)
            .appendQueryParameter(FidoRequestField.CHALLENGE, challengeStr)
            .appendQueryParameter(AuthFidoRequestField.ALLOWED_CREDENTIALS, allowCredentialsOneUser)
            .appendQueryParameter(FidoRequestField.RELYING_PARTY_IDENTIFIER, relyingPartyIdentifier)
            .appendQueryParameter(FidoRequestField.VERSION, version)
            .appendQueryParameter(AuthFidoRequestField.KEY_TYPES, keyTypes)
            .appendQueryParameter(FidoRequestField.CONTEXT, context)
            .build().toString()
        FidoChallengeFactory.createFidoChallengeFromRedirect(malformedUrl)
    }

    @Test(expected = ClientException::class)
    fun testCreateFidoChallengeFromRedirect_AuthNoContext() {
        val malformedUrl = Uri.Builder().authority(authority)
            .appendQueryParameter(FidoRequestField.CHALLENGE, challengeStr)
            .appendQueryParameter(AuthFidoRequestField.ALLOWED_CREDENTIALS, allowCredentialsOneUser)
            .appendQueryParameter(FidoRequestField.RELYING_PARTY_IDENTIFIER, relyingPartyIdentifier)
            .appendQueryParameter(FidoRequestField.VERSION, version)
            .appendQueryParameter(FidoRequestField.SUBMIT_URL, submitUrl)
            .appendQueryParameter(AuthFidoRequestField.KEY_TYPES, keyTypes)
            .build().toString()
        FidoChallengeFactory.createFidoChallengeFromRedirect(malformedUrl)
    }

    @Test(expected = ClientException::class)
    fun testCreateFidoChallengeFromRedirect_AuthAllowCredentialsEmpty() {
        val fullUrl = Uri.Builder().authority(authority)
            .appendQueryParameter(FidoRequestField.CHALLENGE, challengeStr)
            .appendQueryParameter(AuthFidoRequestField.ALLOWED_CREDENTIALS, allowCredentialsEmpty)
            .appendQueryParameter(FidoRequestField.RELYING_PARTY_IDENTIFIER, relyingPartyIdentifier)
            .appendQueryParameter(FidoRequestField.VERSION, version)
            .appendQueryParameter(FidoRequestField.SUBMIT_URL, submitUrl)
            .appendQueryParameter(AuthFidoRequestField.KEY_TYPES, keyTypes)
            .appendQueryParameter(FidoRequestField.CONTEXT, context)
            .build().toString()
        FidoChallengeFactory.createFidoChallengeFromRedirect(fullUrl)
    }

    @Test(expected = ClientException::class)
    fun testCreateFidoChallengeFromRedirect_AuthKeyTypesEmpty() {
        val fullUrl = Uri.Builder().authority(authority)
            .appendQueryParameter(FidoRequestField.CHALLENGE, challengeStr)
            .appendQueryParameter(AuthFidoRequestField.ALLOWED_CREDENTIALS, allowCredentialsOneUser)
            .appendQueryParameter(FidoRequestField.RELYING_PARTY_IDENTIFIER, relyingPartyIdentifier)
            .appendQueryParameter(FidoRequestField.VERSION, version)
            .appendQueryParameter(FidoRequestField.SUBMIT_URL, submitUrl)
            .appendQueryParameter(AuthFidoRequestField.KEY_TYPES, keyTypesEmpty)
            .appendQueryParameter(FidoRequestField.CONTEXT, context)
            .build().toString()
        FidoChallengeFactory.createFidoChallengeFromRedirect(fullUrl)
    }

    @Test
    fun testValidateRequiredParameter_ExpectedFieldAndValue() {
        assertEquals(
            FidoChallengeFactory.validateRequiredParameter(FidoRequestField.CHALLENGE, challengeStr),
            challengeStr
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateRequiredParameter_MissingField() {
        FidoChallengeFactory.validateRequiredParameter(FidoRequestField.CHALLENGE, null)
    }

    @Test(expected = ClientException::class)
    fun testValidateRequiredParameter_EmptyValue() {
        FidoChallengeFactory.validateRequiredParameter(FidoRequestField.CHALLENGE, "")
    }

    @Test
    fun testValidateOptionalListParameter_ExpectedFieldAndValue() {
        assertEquals(
            FidoChallengeFactory.validateOptionalListParameter(AuthFidoRequestField.ALLOWED_CREDENTIALS, allowCredentialsTwoUsers),
            allowCredentialsTwoUsers.split(DELIMITER).toList()
        )
    }

    @Test
    fun testValidateOptionalListParameter_MissingField() {
        assertNull(FidoChallengeFactory.validateOptionalListParameter(AuthFidoRequestField.ALLOWED_CREDENTIALS, null))
    }

    @Test(expected = ClientException::class)
    fun testValidateOptionalListParameter_EmptyValue() {
        FidoChallengeFactory.validateOptionalListParameter(AuthFidoRequestField.ALLOWED_CREDENTIALS, "")
    }

    @Test
    fun validateParameterOrReturnDefault_ExpectedFieldAndValue() {
        assertEquals(
            FidoChallengeFactory.validateParameterOrReturnDefault(
                FidoRequestField.USER_VERIFICATION_POLICY,
                otherUserVerificationPolicy,
                DEFAULT_USER_VERIFICATION_POLICY),
            otherUserVerificationPolicy
        )
    }

    fun validateParameterOrReturnDefault_MissingField() {
        assertEquals(
            FidoChallengeFactory.validateParameterOrReturnDefault(
                FidoRequestField.USER_VERIFICATION_POLICY,
                null,
                DEFAULT_USER_VERIFICATION_POLICY),
            DEFAULT_USER_VERIFICATION_POLICY
        )
    }

    @Test(expected = ClientException::class)
    fun validateParameterOrReturnDefault_EmptyValue() {
        FidoChallengeFactory.validateParameterOrReturnDefault(
            FidoRequestField.USER_VERIFICATION_POLICY,
            "",
            DEFAULT_USER_VERIFICATION_POLICY)
    }
}
