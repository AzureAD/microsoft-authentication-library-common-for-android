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
package com.microsoft.identity.common.internal.fido

import org.junit.Assert.assertEquals
import org.junit.Test

class WebAuthnJsonUtilTest {
    val challengeStr = "T1xCsnxM2DNL2KdK5CLa6fMhD7OBqho6syzInk_n-Uo"
    val relyingPartyIdentifier = "login.microsoft.com"
    val userVerificationPolicy = "required"
    val version = "1.0"
    val submitUrl = "submiturl"
    val context = "123456"
    val keyTypes = "passkey"

    //Auth
    val allowCredentials1 = "id1"
    val allowCredentials2 = "id2"

    val expectedJsonAllFieldsFilled = """{"challenge":"$challengeStr","rpId":"$relyingPartyIdentifier","allowCredentials":[{"type":"public-key","id":"$allowCredentials1"},{"type":"public-key","id":"$allowCredentials2"}],"userVerification":"$userVerificationPolicy"}"""
    val expectedJsonOnlyRequiredFields = """{"challenge":"$challengeStr","rpId":"$relyingPartyIdentifier","allowCredentials":[],"userVerification":"$userVerificationPolicy"}"""

    @Test
    fun testCreateJsonAuthRequestFromChallengeObject_AllFieldsFilled() {
        val authChallenge = AuthFidoChallenge(
            challenge = challengeStr,
            relyingPartyIdentifier = relyingPartyIdentifier,
            userVerificationPolicy = userVerificationPolicy,
            version = version,
            submitUrl = submitUrl,
            keyTypes = listOf(keyTypes),
            context = context,
            allowedCredentials = listOf(allowCredentials1, allowCredentials2)
        )
        val result = WebAuthnJsonUtil.createJsonAuthRequestFromChallengeObject(authChallenge)
        assertEquals(result, expectedJsonAllFieldsFilled)
    }

    @Test
    fun testCreateJsonAuthRequestFromChallengeObject_OnlyRequiredFields() {
        val authChallenge = AuthFidoChallenge(
            challenge = challengeStr,
            relyingPartyIdentifier = relyingPartyIdentifier,
            userVerificationPolicy = userVerificationPolicy,
            version = version,
            submitUrl = submitUrl,
            keyTypes = null,
            context = context,
            allowedCredentials = null
        )
        val result = WebAuthnJsonUtil.createJsonAuthRequestFromChallengeObject(authChallenge)
        assertEquals(result, expectedJsonOnlyRequiredFields)
    }

    //No tests created for missing required fields because
    // the AuthFidoChallenge's required fields are non-null.
}
