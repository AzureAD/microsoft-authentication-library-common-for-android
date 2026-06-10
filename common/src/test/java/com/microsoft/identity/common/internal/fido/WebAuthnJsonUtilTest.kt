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

import com.microsoft.identity.common.internal.fido.WebAuthnJsonUtil.base64UrlEncoded
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

@RunWith(RobolectricTestRunner::class)
class WebAuthnJsonUtilTest {
    // Demo challenge from https://jwt.io/
    val challengeStr = "O.eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
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
    val expectedJsonAllFieldsFilled = """{"allowCredentials":[{"id":"$allowCredentials1","type":"public-key"},{"id":"$allowCredentials2","type":"public-key"}],"challenge":"${challengeStr.base64UrlEncoded()}","rpId":"$relyingPartyIdentifier","userVerification":"$userVerificationPolicy"}"""
    val expectedJsonOnlyRequiredFields = """{"allowCredentials":[],"challenge":"${challengeStr.base64UrlEncoded()}","rpId":"$relyingPartyIdentifier","userVerification":"$userVerificationPolicy"}"""

    //Test AuthenticationResponse values and Json strings.
    //Demo values from Google's Credential Manager docs, or made up.
    val id = "KEDetxZcUfinhVi6Za5nZQ"
    val rawId = "KEDetxZcUfinhVi6Za5nZQ"
    val authenticatorAttachment = "cross-platform"
    val clientExtensionResults = {}
    val type = "public-key"

    //Authentication AssertionResponse values
    val clientDataJSON = "eyJ0eXBlIjoid2ViYXV0aG4uZ2V0IiwiY2hhbGxlbmdlIjoiVDF4Q3NueE0yRE5MMktkSzVDTGE2Zk1oRDdPQnFobzZzeXpJbmtfbi1VbyIsIm9yaWdpbiI6ImFuZHJvaWQ6YXBrLWtleS1oYXNoOk1MTHpEdll4UTRFS1R3QzZVNlpWVnJGUXRIOEdjVi0xZDQ0NEZLOUh2YUkiLCJhbmRyb2lkUGFja2FnZU5hbWUiOiJjb20uZ29vZ2xlLmNyZWRlbnRpYWxtYW5hZ2VyLnNhbXBsZSJ9"
    val authenticatorData = "j5r_fLFhV-qdmGEwiukwD5E_5ama9g0hzXgN8thcFGQdAAAAAA"
    val signature = "MEUCIQCO1Cm4SA2xiG5FdKDHCJorueiS04wCsqHhiRDbbgITYAIgMKMFirgC2SSFmxrh7z9PzUqr0bK1HZ6Zn8vZVhETnyQ"
    val userHandle = "2HzoHm_hY0CjuEESY9tY6-3SdjmNHOoNqaPDcZGzsr0"
    val idAssertionResponse = "KEDetxZcUfinhVi6Za5nZQ"
    val attestationObject = "o2NmbXRkbm9uZWdhdHRTdG10oGhhdXRoRGF0YViUj5r_fLFhV-qdmGEwiukwD5E_5ama9g0hzXgN8thcFGRdAAAAAAAAAAAAAAAAAAAAAAAAAAAAEChA3rcWXFH4p4VYumWuZ2WlAQIDJiABIVgg4RqZaJyaC24Pf4tT-8ONIZ5_Elddf3dNotGOx81jj3siWCAWXS6Lz70hvC2g8hwoLllOwlsbYatNkO2uYFO-eJID6A"

    val expectedAuthenticationAssertionResponseJsonAllFieldsFilled = """{"authenticatorData":"$authenticatorData","clientDataJSON":"$clientDataJSON","id":"$idAssertionResponse","signature":"$signature","userHandle":"$userHandle"}"""
    val expectedAuthenticationAssertionResponseOnlyRequiredFields = """{"authenticatorData":"$authenticatorData","clientDataJSON":"$clientDataJSON","id":"$idAssertionResponse","signature":"$signature"}"""

    val demoAuthenticationResponseJsonAllFieldsFilled = """{"authenticatorAttachment":"$authenticatorAttachment","clientExtensionResults":{},"id":"KEDetxZcUfinhVi6Za5nZQ","rawId":"KEDetxZcUfinhVi6Za5nZQ","response":{"attestationObject":"$attestationObject","authenticatorData":"$authenticatorData","clientDataJSON":"$clientDataJSON","id":"$idAssertionResponse","signature":"$signature","userHandle":"$userHandle"},"type":"public-key"}"""
    val demoAuthenticationResponseJsonOnlyRequiredFields = """{"clientExtensionResults":{},"id":"KEDetxZcUfinhVi6Za5nZQ","rawId":"KEDetxZcUfinhVi6Za5nZQ","response":{"attestationObject":null,"authenticatorData":"$authenticatorData","clientDataJSON":"$clientDataJSON","id":"$idAssertionResponse","signature":"$signature"},"type":"public-key"}"""
    val demoAuthenticationResponseJsonMissingSignature = """{"clientExtensionResults":{},"id":"KEDetxZcUfinhVi6Za5nZQ","rawId":"KEDetxZcUfinhVi6Za5nZQ","response":{"attestationObject":null,"authenticatorData":"$authenticatorData","clientDataJSON":"$clientDataJSON","id":"$idAssertionResponse","userHandle":"$userHandle"},"type":"public-key"}"""

    // Demo JWT from https://jwt.io/
    val demoJWT = "O.eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
    val expectedEncodedJWT = "Ty5leUpoYkdjaU9pSklVekkxTmlJc0luUjVjQ0k2SWtwWFZDSjkuZXlKemRXSWlPaUl4TWpNME5UWTNPRGt3SWl3aWJtRnRaU0k2SWtwdmFHNGdSRzlsSWl3aWFXRjBJam94TlRFMk1qTTVNREl5ZlEuU2ZsS3h3UkpTTWVLS0YyUVQ0ZndwTWVKZjM2UE9rNnlKVl9hZFFzc3c1Yw"
    val randomString = "qwerty12345.QWERTYSomethingElseHere"
    val expectedEncodedRandomString = "cXdlcnR5MTIzNDUuUVdFUlRZU29tZXRoaW5nRWxzZUhlcmU"


    @Test
    fun testCreateJsonAuthRequestFromChallengeObject_AllFieldsFilled() {
        val result = WebAuthnJsonUtil.createJsonAuthRequest(
            challenge = challengeStr,
            relyingPartyIdentifier = relyingPartyIdentifier,
            allowedCredentials = listOf(allowCredentials1, allowCredentials2),
            userVerificationPolicy = userVerificationPolicy
        )
        JSONAssert.assertEquals(expectedJsonAllFieldsFilled, result, JSONCompareMode.LENIENT)
    }

    @Test
    fun testCreateJsonAuthRequestFromChallengeObject_OnlyRequiredFields() {
        val result = WebAuthnJsonUtil.createJsonAuthRequest(
            challenge = challengeStr,
            relyingPartyIdentifier = relyingPartyIdentifier,
            allowedCredentials = null,
            userVerificationPolicy = userVerificationPolicy
        )
        JSONAssert.assertEquals(expectedJsonOnlyRequiredFields, result, JSONCompareMode.LENIENT)
    }

    //No tests created for missing required fields because
    // the AuthFidoChallenge's required fields are non-null.

    @Test
    fun testExtractAuthenticatorAssertionResponseJson_AllFieldsFilled() {
        val result = WebAuthnJsonUtil.extractAuthenticatorAssertionResponseJson(demoAuthenticationResponseJsonAllFieldsFilled)
        JSONAssert.assertEquals(expectedAuthenticationAssertionResponseJsonAllFieldsFilled, result, JSONCompareMode.LENIENT)
    }

    @Test
    fun testExtractAuthenticatorAssertionResponseJson_OnlyRequiredFields() {
        val result = WebAuthnJsonUtil.extractAuthenticatorAssertionResponseJson(demoAuthenticationResponseJsonOnlyRequiredFields)
        JSONAssert.assertEquals(expectedAuthenticationAssertionResponseOnlyRequiredFields, result, JSONCompareMode.LENIENT)
    }

    //This should never happen, but in this scenario, an exception will indeed be thrown.
    @Test(expected = JSONException::class)
    fun testExtractAuthenticatorAssertionResponseJson_MissingSignature() {
        val result = WebAuthnJsonUtil.extractAuthenticatorAssertionResponseJson(demoAuthenticationResponseJsonMissingSignature)
    }

    @Test
    fun testCreateAssertionString_allFieldsFilled() {
        val result = WebAuthnJsonUtil.createAssertionString(
            authenticatorData = authenticatorData,
            clientDataJson = clientDataJSON,
            id = idAssertionResponse,
            signature = signature,
            userHandle = userHandle
        )
        JSONAssert.assertEquals(expectedAuthenticationAssertionResponseJsonAllFieldsFilled, result, JSONCompareMode.LENIENT)
    }

    // No test for null parameters because the method only takes in nonnull strings.

    @Test
    fun testBase64UrlEncoded_JWTFromServer() {
        assertEquals(expectedEncodedJWT, demoJWT.base64UrlEncoded())
    }

    @Test
    fun testBase64UrlEncoded_EmptyString() {
        assertEquals("", "".base64UrlEncoded())
    }

    @Test
    fun testBase64UrlEncoded_RandomString() {
        assertEquals(expectedEncodedRandomString, randomString.base64UrlEncoded())
    }

    // createCredentialResponse.json test data
    // clientDataJSON decodes to:
    // {"type":"webauthn.create","challenge":"...","origin":"android:apk-key-hash:E8lSX1zJMPBZ9G_zpnfxfmfh-d0q2qEYz-2bgNeUKCU","androidPackageName":"com.microsoft.identity.testuserapp"}
    val createCredentialResponseJson = """{"rawId":"_mqxqFyisYcQDo7sTf5Ggw","authenticatorAttachment":"platform","type":"public-key","id":"_mqxqFyisYcQDo7sTf5Ggw","response":{"clientDataJSON":"eyJ0eXBlIjoid2ViYXV0aG4uY3JlYXRlIiwiY2hhbGxlbmdlIjoiTFVOeGMwZ3FXaXBwYTBacmRGTmpiMU5OUkc1UmNqaHZlRFUxWkVOUFZGWkZVbVJrV0hSNFFXSnRWbWN6Wld4UldGVkxZblpvUWpkalJrNXRaa2g1VkhNek9FUjNaM05pVG5oVFVtTnRjRzgwZVVSRVUzQk5SVlpuYzBKc2QxUnlNVkZ4TlhOcVFqSndTMnBSS2ciLCJvcmlnaW4iOiJhbmRyb2lkOmFway1rZXktaGFzaDpFOGxTWDF6Sk1QQlo5R196cG5meGZtZmgtZDBxMnFFWXotMmJnTmVVS0NVIiwiYW5kcm9pZFBhY2thZ2VOYW1lIjoiY29tLm1pY3Jvc29mdC5pZGVudGl0eS50ZXN0dXNlcmFwcCJ9","attestationObject":"o2NmbXRkbm9uZWdhdHRTdG10oGhhdXRoRGF0YViUNWye1KCTIblpXx6vkYID8bVfaJ2mH7yWGEwVfdpoDIFdAAAAAOqbjWZNAR0hPOS2tIy1ddQAEP5qsahcorGHEA6O7E3-RoOlAQIDJiABIVgg2wYC7isQOus7OjKigGo_J37T42oJq0SROrLhqn-53AgiWCAN3Z596TH_Lh9BeAdZinza_vXPWfb90QzUcK-vpipqKQ","transports":["internal","hybrid"],"authenticatorData":"NWye1KCTIblpXx6vkYID8bVfaJ2mH7yWGEwVfdpoDIFdAAAAAOqbjWZNAR0hPOS2tIy1ddQAEP5qsahcorGHEA6O7E3-RoOlAQIDJiABIVgg2wYC7isQOus7OjKigGo_J37T42oJq0SROrLhqn-53AgiWCAN3Z596TH_Lh9BeAdZinza_vXPWfb90QzUcK-vpipqKQ","publicKeyAlgorithm":-7,"publicKey":"MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE2wYC7isQOus7OjKigGo_J37T42oJq0SROrLhqn-53AgN3Z596TH_Lh9BeAdZinza_vXPWfb90QzUcK-vpipqKQ"},"clientExtensionResults":{"credProps":{"rk":true}}}"""
    val expectedOriginFromCreateCredentialResponse = "android:apk-key-hash:E8lSX1zJMPBZ9G_zpnfxfmfh-d0q2qEYz-2bgNeUKCU"

    // attestationObject from createCredentialResponseJson decodes to AAGUID ea9b8d66-4d01-1d21-3ce4-b6b48cb575d4
    val expectedAaguidFromCreateCredentialResponse = "ea9b8d66-4d01-1d21-3ce4-b6b48cb575d4"
    val attestationObjectFromCreateCredential = "o2NmbXRkbm9uZWdhdHRTdG10oGhhdXRoRGF0YViUNWye1KCTIblpXx6vkYID8bVfaJ2mH7yWGEwVfdpoDIFdAAAAAOqbjWZNAR0hPOS2tIy1ddQAEP5qsahcorGHEA6O7E3-RoOlAQIDJiABIVgg2wYC7isQOus7OjKigGo_J37T42oJq0SROrLhqn-53AgiWCAN3Z596TH_Lh9BeAdZinza_vXPWfb90QzUcK-vpipqKQ"

    @Test
    fun testExtractOriginFromRegistrationResponse_fromCreateCredentialResponse() {
        val result = WebAuthnJsonUtil.extractOriginFromRegistrationResponse(createCredentialResponseJson)
        assertEquals(expectedOriginFromCreateCredentialResponse, result)
    }

    @Test
    fun testExtractOriginFromRegistrationResponse_invalidJson_returnsNull() {
        val result = WebAuthnJsonUtil.extractOriginFromRegistrationResponse("not valid json")
        assertNull(result)
    }

    @Test
    fun testExtractOriginFromRegistrationResponse_missingResponseKey_returnsNull() {
        val result = WebAuthnJsonUtil.extractOriginFromRegistrationResponse("""{"id":"abc","type":"public-key"}""")
        assertNull(result)
    }

    @Test
    fun testExtractAaguidFromRegistrationResponse_fromCreateCredentialResponse() {
        val result = WebAuthnJsonUtil.extractAaguidFromRegistrationResponse(createCredentialResponseJson)
        assertEquals(expectedAaguidFromCreateCredentialResponse, result)
    }

    @Test
    fun testExtractAaguidFromRegistrationResponse_invalidJson_returnsNull() {
        val result = WebAuthnJsonUtil.extractAaguidFromRegistrationResponse("not valid json")
        assertNull(result)
    }

    @Test
    fun testExtractAaguidFromRegistrationResponse_missingResponseKey_returnsNull() {
        val result = WebAuthnJsonUtil.extractAaguidFromRegistrationResponse("""{"id":"abc","type":"public-key"}""")
        assertNull(result)
    }

    @Test
    fun testExtractAaguidFromAttestationObject_validAttestationObject_returnsExpectedAaguid() {
        val result = WebAuthnJsonUtil.extractAaguidFromAttestationObject(attestationObjectFromCreateCredential)
        assertEquals(expectedAaguidFromCreateCredentialResponse, result)
    }

    @Test(expected = Exception::class)
    fun testExtractAaguidFromAttestationObject_invalidBase64_throws() {
        WebAuthnJsonUtil.extractAaguidFromAttestationObject("!!!not-valid-base64!!!")
    }

    @Test(expected = Exception::class)
    fun testExtractAaguidFromAttestationObject_missingAuthData_throws() {
        // Valid base64url but CBOR with no authData key
        WebAuthnJsonUtil.extractAaguidFromAttestationObject("oA")
    }
}
