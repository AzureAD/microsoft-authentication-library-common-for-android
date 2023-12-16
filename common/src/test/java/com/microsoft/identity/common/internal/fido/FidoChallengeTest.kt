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
import com.microsoft.identity.common.internal.fido.FidoChallenge.Companion.DEFAULT_USER_VERIFICATION_POLICY
import com.microsoft.identity.common.java.exception.ClientException
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

//Robolectric used for Uri dependency
@RunWith(RobolectricTestRunner::class)
class FidoChallengeTest {
    val challengeStr = "T1xCsnxM2DNL2KdK5CLa6fMhD7OBqho6syzInk_n-Uo"
    val relyingPartyIdentifier = "login.microsoft.com"
    val otherUserVerificationPolicy = "preferred"
    val version = "1.0"
    val submitUrl = "https://submiturl"
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
            .appendQueryParameter(FidoRequestField.CHALLENGE.fieldName, challengeStr)
            .appendQueryParameter(FidoRequestField.ALLOWED_CREDENTIALS.fieldName, allowCredentialsOneUser)
            .appendQueryParameter(FidoRequestField.RELYING_PARTY_IDENTIFIER.fieldName, relyingPartyIdentifier)
            .appendQueryParameter(FidoRequestField.VERSION.fieldName, version)
            .appendQueryParameter(FidoRequestField.SUBMIT_URL.fieldName, submitUrl)
            .appendQueryParameter(FidoRequestField.KEY_TYPES.fieldName, keyTypes)
            .appendQueryParameter(FidoRequestField.CONTEXT.fieldName, context)
            .build().toString()
        val fidoChallenge = FidoChallenge(fullUrl)
        assertEquals(DEFAULT_USER_VERIFICATION_POLICY, fidoChallenge.userVerificationPolicy.getOrThrow())
        assertNotNull(fidoChallenge.allowedCredentials.getOrThrow())
        fidoChallenge.allowedCredentials.getOrThrow()?.let {
            assertTrue(it.size == 1)
        }
    }

    @Test
    fun testCreateFidoChallengeFromRedirect_AuthSingleAllowCredentialsTwoUsers() {
        val fullUrl = Uri.Builder().authority(authority)
            .appendQueryParameter(FidoRequestField.CHALLENGE.fieldName, challengeStr)
            .appendQueryParameter(FidoRequestField.ALLOWED_CREDENTIALS.fieldName, allowCredentialsTwoUsers)
            .appendQueryParameter(FidoRequestField.RELYING_PARTY_IDENTIFIER.fieldName, relyingPartyIdentifier)
            .appendQueryParameter(FidoRequestField.VERSION.fieldName, version)
            .appendQueryParameter(FidoRequestField.SUBMIT_URL.fieldName, submitUrl)
            .appendQueryParameter(FidoRequestField.KEY_TYPES.fieldName, keyTypes)
            .appendQueryParameter(FidoRequestField.CONTEXT.fieldName, context)
            .build().toString()
        val fidoChallenge = FidoChallenge(fullUrl)
        assertEquals(DEFAULT_USER_VERIFICATION_POLICY, fidoChallenge.userVerificationPolicy.getOrThrow())
        assertNotNull(fidoChallenge.allowedCredentials.getOrThrow())
        fidoChallenge.allowedCredentials.getOrThrow()?.let {
            assertTrue(it.size == 2)
        }
    }

    @Test
    fun testCreateFidoChallengeFromRedirect_AuthNoAllowedCredentials() {
        val url = Uri.Builder().authority(authority)
            .appendQueryParameter(FidoRequestField.CHALLENGE.fieldName, challengeStr)
            .appendQueryParameter(FidoRequestField.RELYING_PARTY_IDENTIFIER.fieldName, relyingPartyIdentifier)
            .appendQueryParameter(FidoRequestField.VERSION.fieldName, version)
            .appendQueryParameter(FidoRequestField.SUBMIT_URL.fieldName, submitUrl)
            .appendQueryParameter(FidoRequestField.KEY_TYPES.fieldName, keyTypes)
            .appendQueryParameter(FidoRequestField.CONTEXT.fieldName, context)
            .build().toString()
        val fidoChallenge = FidoChallenge(url)
        assertEquals(DEFAULT_USER_VERIFICATION_POLICY, fidoChallenge.userVerificationPolicy.getOrThrow())
        assertNull(fidoChallenge.allowedCredentials.getOrThrow())
    }

    @Test
    fun testCreateFidoChallengeFromRedirect_AuthNoKeyTypes() {
        val url = Uri.Builder().authority(authority)
            .appendQueryParameter(FidoRequestField.CHALLENGE.fieldName, challengeStr)
            .appendQueryParameter(FidoRequestField.ALLOWED_CREDENTIALS.fieldName, allowCredentialsOneUser)
            .appendQueryParameter(FidoRequestField.RELYING_PARTY_IDENTIFIER.fieldName, relyingPartyIdentifier)
            .appendQueryParameter(FidoRequestField.VERSION.fieldName, version)
            .appendQueryParameter(FidoRequestField.SUBMIT_URL.fieldName, submitUrl)
            .appendQueryParameter(FidoRequestField.CONTEXT.fieldName, context)
            .build().toString()
        val fidoChallenge = FidoChallenge(url)
        assertEquals(DEFAULT_USER_VERIFICATION_POLICY, fidoChallenge.userVerificationPolicy.getOrThrow())
        assertNull(fidoChallenge.keyTypes.getOrThrow())
    }

    @Test
    fun testCreateFidoChallengeFromRedirect_AuthOnlyRequiredParams() {
        val url = Uri.Builder().authority(authority)
            .appendQueryParameter(FidoRequestField.CHALLENGE.fieldName, challengeStr)
            .appendQueryParameter(FidoRequestField.RELYING_PARTY_IDENTIFIER.fieldName, relyingPartyIdentifier)
            .appendQueryParameter(FidoRequestField.VERSION.fieldName, version)
            .appendQueryParameter(FidoRequestField.SUBMIT_URL.fieldName, submitUrl)
            .appendQueryParameter(FidoRequestField.CONTEXT.fieldName, context)
            .build().toString()
        val fidoChallenge = FidoChallenge(url)
        assertEquals(challengeStr, fidoChallenge.challenge.getOrThrow())
        assertEquals(relyingPartyIdentifier, fidoChallenge.relyingPartyIdentifier.getOrThrow())
        assertEquals(version, fidoChallenge.version.getOrThrow())
        assertEquals(submitUrl, fidoChallenge.submitUrl.getOrThrow())
        assertEquals(context, fidoChallenge.context.getOrThrow())
        assertEquals(DEFAULT_USER_VERIFICATION_POLICY, fidoChallenge.userVerificationPolicy.getOrThrow())
        assertNull(fidoChallenge.allowedCredentials.getOrThrow())
        assertNull(fidoChallenge.keyTypes.getOrThrow())
    }

    @Test
    fun testCreateFidoChallengeFromRedirect_AuthNoChallenge() {
        val malformedUrl = Uri.Builder().authority(authority)
            .appendQueryParameter(FidoRequestField.ALLOWED_CREDENTIALS.fieldName, allowCredentialsOneUser)
            .appendQueryParameter(FidoRequestField.RELYING_PARTY_IDENTIFIER.fieldName, relyingPartyIdentifier)
            .appendQueryParameter(FidoRequestField.VERSION.fieldName, version)
            .appendQueryParameter(FidoRequestField.SUBMIT_URL.fieldName, submitUrl)
            .appendQueryParameter(FidoRequestField.KEY_TYPES.fieldName, keyTypes)
            .appendQueryParameter(FidoRequestField.CONTEXT.fieldName, context)
            .build().toString()
        val fidoChallenge = FidoChallenge(malformedUrl)
        assertThrows(ClientException::class.java) {
            fidoChallenge.challenge.getOrThrow()
        }
        assertNotNull(fidoChallenge.allowedCredentials.getOrThrow())
        fidoChallenge.allowedCredentials.getOrThrow()?.let {
            assertTrue(it.size == 1)
        }
        assertEquals(relyingPartyIdentifier, fidoChallenge.relyingPartyIdentifier.getOrThrow())
        assertEquals(version, fidoChallenge.version.getOrThrow())
        assertEquals(submitUrl, fidoChallenge.submitUrl.getOrThrow())
        assertEquals(context, fidoChallenge.context.getOrThrow())
        assertEquals(DEFAULT_USER_VERIFICATION_POLICY, fidoChallenge.userVerificationPolicy.getOrThrow())
        assertNotNull(fidoChallenge.keyTypes.getOrThrow())
        fidoChallenge.keyTypes.getOrThrow()?.let {
            assertTrue(it.size == 1)
        }
    }

    @Test
    fun testCreateFidoChallengeFromRedirect_AuthNoRelyingPartyIdentifier() {
        val malformedUrl = Uri.Builder().authority(authority)
            .appendQueryParameter(FidoRequestField.CHALLENGE.fieldName, challengeStr)
            .appendQueryParameter(FidoRequestField.ALLOWED_CREDENTIALS.fieldName, allowCredentialsOneUser)
            .appendQueryParameter(FidoRequestField.VERSION.fieldName, version)
            .appendQueryParameter(FidoRequestField.SUBMIT_URL.fieldName, submitUrl)
            .appendQueryParameter(FidoRequestField.KEY_TYPES.fieldName, keyTypes)
            .appendQueryParameter(FidoRequestField.CONTEXT.fieldName, context)
            .build().toString()
        val fidoChallenge = FidoChallenge(malformedUrl)
        assertEquals(challengeStr, fidoChallenge.challenge.getOrThrow())
        assertNotNull(fidoChallenge.allowedCredentials)
        fidoChallenge.allowedCredentials.getOrThrow()?.let {
            assertTrue(it.size == 1)
        }

        assertThrows(ClientException::class.java) {
            fidoChallenge.relyingPartyIdentifier.getOrThrow()
        }
        assertEquals(version, fidoChallenge.version.getOrThrow())
        assertEquals(submitUrl, fidoChallenge.submitUrl.getOrThrow())
        assertEquals(context, fidoChallenge.context.getOrThrow())
        assertEquals(DEFAULT_USER_VERIFICATION_POLICY, fidoChallenge.userVerificationPolicy.getOrThrow())
        assertNotNull(fidoChallenge.keyTypes.getOrThrow())
        fidoChallenge.keyTypes.getOrThrow()?.let {
            assertTrue(it.size == 1)
        }
    }

    //Note that this case shouldn't really happen for passkey protocol 1.0,
    // as ESTS assumes userVerificationPolicy is always "required".
    @Test
    fun testCreateFidoChallengeFromRedirect_AuthSettingDifferentUserVerificationPolicy() {
        val url = Uri.Builder().authority(authority)
            .appendQueryParameter(FidoRequestField.CHALLENGE.fieldName, challengeStr)
            .appendQueryParameter(FidoRequestField.ALLOWED_CREDENTIALS.fieldName, allowCredentialsOneUser)
            .appendQueryParameter(FidoRequestField.RELYING_PARTY_IDENTIFIER.fieldName, relyingPartyIdentifier)
            .appendQueryParameter(FidoRequestField.USER_VERIFICATION_POLICY.fieldName, otherUserVerificationPolicy)
            .appendQueryParameter(FidoRequestField.VERSION.fieldName, version)
            .appendQueryParameter(FidoRequestField.SUBMIT_URL.fieldName, submitUrl)
            .appendQueryParameter(FidoRequestField.KEY_TYPES.fieldName, keyTypes)
            .appendQueryParameter(FidoRequestField.CONTEXT.fieldName, context)
            .build().toString()
        val fidoChallenge = FidoChallenge(url)
        assertEquals(challengeStr, fidoChallenge.challenge.getOrThrow())
        assertNotNull(fidoChallenge.allowedCredentials.getOrThrow())
        fidoChallenge.allowedCredentials.getOrThrow()?.let {
            assertTrue(it.size == 1)
        }
        assertEquals(relyingPartyIdentifier, fidoChallenge.relyingPartyIdentifier.getOrThrow())
        assertEquals(version, fidoChallenge.version.getOrThrow())
        assertEquals(submitUrl, fidoChallenge.submitUrl.getOrThrow())
        assertEquals(context, fidoChallenge.context.getOrThrow())
        assertEquals(otherUserVerificationPolicy, fidoChallenge.userVerificationPolicy.getOrThrow())
        assertNotNull(fidoChallenge.keyTypes.getOrThrow())
        fidoChallenge.keyTypes.getOrThrow()?.let {
            assertTrue(it.size == 1)
        }
    }

    @Test
    fun testCreateFidoChallengeFromRedirect_AuthNoVersion() {
        val malformedUrl = Uri.Builder().authority(authority)
            .appendQueryParameter(FidoRequestField.CHALLENGE.fieldName, challengeStr)
            .appendQueryParameter(FidoRequestField.ALLOWED_CREDENTIALS.fieldName, allowCredentialsOneUser)
            .appendQueryParameter(FidoRequestField.RELYING_PARTY_IDENTIFIER.fieldName, relyingPartyIdentifier)
            .appendQueryParameter(FidoRequestField.SUBMIT_URL.fieldName, submitUrl)
            .appendQueryParameter(FidoRequestField.KEY_TYPES.fieldName, keyTypes)
            .appendQueryParameter(FidoRequestField.CONTEXT.fieldName, context)
            .build().toString()
        val fidoChallenge = FidoChallenge(malformedUrl)
        assertEquals(challengeStr, fidoChallenge.challenge.getOrThrow())
        assertNotNull(fidoChallenge.allowedCredentials)
        fidoChallenge.allowedCredentials.getOrThrow()?.let {
            assertTrue(it.size == 1)
        }
        assertEquals(relyingPartyIdentifier, fidoChallenge.relyingPartyIdentifier.getOrThrow())
        assertThrows(ClientException::class.java) {
            fidoChallenge.version.getOrThrow()
        }
        assertEquals(submitUrl, fidoChallenge.submitUrl.getOrThrow())
        assertEquals(context, fidoChallenge.context.getOrThrow())
        assertEquals(DEFAULT_USER_VERIFICATION_POLICY, fidoChallenge.userVerificationPolicy.getOrThrow())
        assertNotNull(fidoChallenge.keyTypes.getOrThrow())
        fidoChallenge.keyTypes.getOrThrow()?.let {
            assertTrue(it.size == 1)
        }
    }

    @Test
    fun testCreateFidoChallengeFromRedirect_AuthNoSubmitUrl() {
        val malformedUrl = Uri.Builder().authority(authority)
            .appendQueryParameter(FidoRequestField.CHALLENGE.fieldName, challengeStr)
            .appendQueryParameter(FidoRequestField.ALLOWED_CREDENTIALS.fieldName, allowCredentialsOneUser)
            .appendQueryParameter(FidoRequestField.RELYING_PARTY_IDENTIFIER.fieldName, relyingPartyIdentifier)
            .appendQueryParameter(FidoRequestField.VERSION.fieldName, version)
            .appendQueryParameter(FidoRequestField.KEY_TYPES.fieldName, keyTypes)
            .appendQueryParameter(FidoRequestField.CONTEXT.fieldName, context)
            .build().toString()
        val fidoChallenge = FidoChallenge(malformedUrl)
        assertEquals(challengeStr, fidoChallenge.challenge.getOrThrow())
        assertNotNull(fidoChallenge.allowedCredentials.getOrThrow())
        fidoChallenge.allowedCredentials.getOrThrow()?.let {
            assertTrue(it.size == 1)
        }
        assertEquals(relyingPartyIdentifier, fidoChallenge.relyingPartyIdentifier.getOrThrow())
        assertEquals(version, fidoChallenge.version.getOrThrow())
        assertThrows(ClientException::class.java) {
            fidoChallenge.submitUrl.getOrThrow()
        }
        assertEquals(context, fidoChallenge.context.getOrThrow())
        assertEquals(DEFAULT_USER_VERIFICATION_POLICY, fidoChallenge.userVerificationPolicy.getOrThrow())
        assertNotNull(fidoChallenge.keyTypes.getOrThrow())
        fidoChallenge.keyTypes.getOrThrow()?.let {
            assertTrue(it.size == 1)
        }
    }

    @Test
    fun testCreateFidoChallengeFromRedirect_AuthNoContext() {
        val malformedUrl = Uri.Builder().authority(authority)
            .appendQueryParameter(FidoRequestField.CHALLENGE.fieldName, challengeStr)
            .appendQueryParameter(FidoRequestField.ALLOWED_CREDENTIALS.fieldName, allowCredentialsOneUser)
            .appendQueryParameter(FidoRequestField.RELYING_PARTY_IDENTIFIER.fieldName, relyingPartyIdentifier)
            .appendQueryParameter(FidoRequestField.VERSION.fieldName, version)
            .appendQueryParameter(FidoRequestField.SUBMIT_URL.fieldName, submitUrl)
            .appendQueryParameter(FidoRequestField.KEY_TYPES.fieldName, keyTypes)
            .build().toString()
        val fidoChallenge = FidoChallenge(malformedUrl)
        assertEquals(challengeStr, fidoChallenge.challenge.getOrThrow())
        assertNotNull(fidoChallenge.allowedCredentials.getOrThrow())
        fidoChallenge.allowedCredentials.getOrThrow()?.let {
            assertTrue(it.size == 1)
        }
        assertEquals(relyingPartyIdentifier, fidoChallenge.relyingPartyIdentifier.getOrThrow())
        assertEquals(version, fidoChallenge.version.getOrThrow())
        assertEquals(submitUrl, fidoChallenge.submitUrl.getOrThrow())
        assertThrows(ClientException::class.java) {
            fidoChallenge.context.getOrThrow()
        }
        assertEquals(DEFAULT_USER_VERIFICATION_POLICY, fidoChallenge.userVerificationPolicy.getOrThrow())
        assertNotNull(fidoChallenge.keyTypes.getOrThrow())
        fidoChallenge.keyTypes.getOrThrow()?.let {
            assertTrue(it.size == 1)
        }
    }

    @Test
    fun testCreateFidoChallengeFromRedirect_AuthAllowCredentialsEmpty() {
        val malformedUrl = Uri.Builder().authority(authority)
            .appendQueryParameter(FidoRequestField.CHALLENGE.fieldName, challengeStr)
            .appendQueryParameter(FidoRequestField.ALLOWED_CREDENTIALS.fieldName, allowCredentialsEmpty)
            .appendQueryParameter(FidoRequestField.RELYING_PARTY_IDENTIFIER.fieldName, relyingPartyIdentifier)
            .appendQueryParameter(FidoRequestField.VERSION.fieldName, version)
            .appendQueryParameter(FidoRequestField.SUBMIT_URL.fieldName, submitUrl)
            .appendQueryParameter(FidoRequestField.KEY_TYPES.fieldName, keyTypes)
            .appendQueryParameter(FidoRequestField.CONTEXT.fieldName, context)
            .build().toString()
        val fidoChallenge = FidoChallenge(malformedUrl)
        assertEquals(challengeStr, fidoChallenge.challenge.getOrThrow())
        assertThrows(ClientException::class.java) {
            fidoChallenge.allowedCredentials.getOrThrow()
        }
        assertEquals(relyingPartyIdentifier, fidoChallenge.relyingPartyIdentifier.getOrThrow())
        assertEquals(version, fidoChallenge.version.getOrThrow())
        assertEquals(submitUrl, fidoChallenge.submitUrl.getOrThrow())
        assertEquals(context, fidoChallenge.context.getOrThrow())
        assertEquals(DEFAULT_USER_VERIFICATION_POLICY, fidoChallenge.userVerificationPolicy.getOrThrow())
        assertNotNull(fidoChallenge.keyTypes.getOrThrow())
        fidoChallenge.keyTypes.getOrThrow()?.let {
            assertTrue(it.size == 1)
        }
    }

    @Test
    fun testCreateFidoChallengeFromRedirect_AuthKeyTypesEmpty() {
        val malformedUrl = Uri.Builder().authority(authority)
            .appendQueryParameter(FidoRequestField.CHALLENGE.fieldName, challengeStr)
            .appendQueryParameter(FidoRequestField.ALLOWED_CREDENTIALS.fieldName, allowCredentialsOneUser)
            .appendQueryParameter(FidoRequestField.RELYING_PARTY_IDENTIFIER.fieldName, relyingPartyIdentifier)
            .appendQueryParameter(FidoRequestField.VERSION.fieldName, version)
            .appendQueryParameter(FidoRequestField.SUBMIT_URL.fieldName, submitUrl)
            .appendQueryParameter(FidoRequestField.KEY_TYPES.fieldName, keyTypesEmpty)
            .appendQueryParameter(FidoRequestField.CONTEXT.fieldName, context)
            .build().toString()
        val fidoChallenge = FidoChallenge(malformedUrl)
        assertEquals(challengeStr, fidoChallenge.challenge.getOrThrow())
        assertNotNull(fidoChallenge.allowedCredentials.getOrThrow())
        fidoChallenge.allowedCredentials.getOrThrow()?.let {
            assertTrue(it.size == 1)
        }
        assertEquals(relyingPartyIdentifier, fidoChallenge.relyingPartyIdentifier.getOrThrow())
        assertEquals(version, fidoChallenge.version.getOrThrow())
        assertEquals(submitUrl, fidoChallenge.submitUrl.getOrThrow())
        assertEquals(context, fidoChallenge.context.getOrThrow())
        assertEquals(DEFAULT_USER_VERIFICATION_POLICY, fidoChallenge.userVerificationPolicy.getOrThrow())
        assertThrows(ClientException::class.java) {
            fidoChallenge.keyTypes.getOrThrow()
        }
    }
}
