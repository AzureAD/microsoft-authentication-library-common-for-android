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

    //Moshi's built in adapter alphabetizes  the fields.
    val expectedJsonAllFieldsFilled = """{"allowCredentials":[{"id":"$allowCredentials1","type":"public-key"},{"id":"$allowCredentials2","type":"public-key"}],"challenge":"$challengeStr","rpId":"$relyingPartyIdentifier","userVerification":"$userVerificationPolicy"}"""
    val expectedJsonOnlyRequiredFields = """{"allowCredentials":[],"challenge":"$challengeStr","rpId":"$relyingPartyIdentifier","userVerification":"$userVerificationPolicy"}"""

    //Test AuthenticationResponse values and Json strings.
    //Demo values from Google's Credential Manager docs, or made up.
    val id = "KEDetxZcUfinhVi6Za5nZQ"
    val rawId = "KEDetxZcUfinhVi6Za5nZQ"
    val response = """{
    "clientDataJSON": "eyJ0eXBlIjoid2ViYXV0aG4uZ2V0IiwiY2hhbGxlbmdlIjoiVDF4Q3NueE0yRE5MMktkSzVDTGE2Zk1oRDdPQnFobzZzeXpJbmtfbi1VbyIsIm9yaWdpbiI6ImFuZHJvaWQ6YXBrLWtleS1oYXNoOk1MTHpEdll4UTRFS1R3QzZVNlpWVnJGUXRIOEdjVi0xZDQ0NEZLOUh2YUkiLCJhbmRyb2lkUGFja2FnZU5hbWUiOiJjb20uZ29vZ2xlLmNyZWRlbnRpYWxtYW5hZ2VyLnNhbXBsZSJ9",
    "authenticatorData": "j5r_fLFhV-qdmGEwiukwD5E_5ama9g0hzXgN8thcFGQdAAAAAA",
    "signature": "MEUCIQCO1Cm4SA2xiG5FdKDHCJorueiS04wCsqHhiRDbbgITYAIgMKMFirgC2SSFmxrh7z9PzUqr0bK1HZ6Zn8vZVhETnyQ",
    "userHandle": "2HzoHm_hY0CjuEESY9tY6-3SdjmNHOoNqaPDcZGzsr0"
  }"""
    val authenticatorAttachment = "cross-platform"
    val clientExtensionResults = "{}"
    val type = "public-key"

    val demoAuthenticationResponseJsonAllFieldsFilled = ""

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
        assertEquals(expectedJsonAllFieldsFilled, result)
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
        assertEquals(expectedJsonOnlyRequiredFields, result)
    }

    //No tests created for missing required fields because
    // the AuthFidoChallenge's required fields are non-null.

    @Test
    fun testExtractAuthenticatorAssertionResponseJson_AllFieldsFilled() {
        val expectedAuthenticationResponse = AuthenticationResponse(
            id,
            rawId,
            response,
            authenticatorAttachment,
            clientExtensionResults,
            type
        )
        val result = WebAuthnJsonUtil.extractAuthenticatorAssertionResponseJson()
    }

    @Test
    fun testExtractAuthenticatorAssertionResponseJson_OnlyRequiredFields() {

    }

    @Test
    fun testExtractAuthenticatorAssertionResponseJson_MissingRequiredFields() {

    }
}
