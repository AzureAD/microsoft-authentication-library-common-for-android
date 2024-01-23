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

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.Locale

/**
 * Makes calls to the Android Credential Manager API in order to return an attestation.
 */
class CredManFidoManager (val context: Context) : IFidoManager {
    val RADIX_BASE_HEXADECIMAL = 16 // Integer.toString(26, 16) outputs "1A"

    val credentialManager = CredentialManager.create(context)

    /**
     * Interacts with the FIDO credential provider and returns an assertion.
     *
     * @param challenge AuthFidoChallenge received from the server.
     * @return assertion
     */
    override suspend fun authenticate(challenge: String,
                                      relyingPartyIdentifier: String,
                                      allowedCredentials: List<String>?,
                                      userVerificationPolicy: String): String {
        val requestJson = WebAuthnJsonUtil.createJsonAuthRequest(
            challenge,
            relyingPartyIdentifier,
            allowedCredentials,
            userVerificationPolicy
        )
        //val clientDataHash = getSha256Hash(jsonSerializeCollectedClientData(challenge))
        val publicKeyCredentialOption = GetPublicKeyCredentialOption(
            requestJson = requestJson,
            //clientDataHash = clientDataHash
        )
        val getCredRequest = GetCredentialRequest(
            credentialOptions = listOf(publicKeyCredentialOption)
        )
        val result = credentialManager.getCredential(
            context = context,
            request = getCredRequest
        )
        val credential: PublicKeyCredential = result.credential as PublicKeyCredential
        return WebAuthnJsonUtil.extractAuthenticatorAssertionResponseJson(credential.authenticationResponseJson)
    }

    /**
     * Serialization of the [CollectedClientData] following:
     * https://www.w3.org/TR/webauthn-2/#clientdatajson-serialization
     */
    fun jsonSerializeCollectedClientData(challenge: String): ByteArray {
        var result: ByteArray = emptyArray<Byte>().toByteArray()
        // 2. Append 0x7b2274797065223a ({"type":) to result.
        result += "7b2274797065223a".decodeHex()
        // 3. Append CCDToString(type) to result.
        result += ccdToString("webauthn.get")
        // 4. Append 0x2c226368616c6c656e6765223a (,"challenge":) to result.
        result += "2c226368616c6c656e6765223a".decodeHex()
        // 5. Append CCDToString(challenge) to result.
        result += ccdToString(WebAuthnJsonUtil.convertToBase64UrlString(challenge))
        // 6. Append 0x2c226f726967696e223a (,"origin":) to result.
        result += "2c226f726967696e223a".decodeHex()
        // 7. Append CCDToString(origin) to result.
        val origin = "android:apk-key-hash:" + WebAuthnJsonUtil.convertFromHexToBase64UrlString( "".replace(":", "").lowercase(
            Locale.getDefault()
        ))
        result += ccdToString(origin)
        // 8. Append 0x2c2263726f73734f726967696e223a (,"crossOrigin":) to result.
        result += "2c2263726f73734f726967696e223a".decodeHex()
        // 9.10. Append false("66616c7365")/true("74727565") based on crossOrigin value:
        // Currently crossOrigin is false, token binding is not supported.
        result += "66616c7365".decodeHex()
        // 11. Add additional fields: Passkey path does not pass in additional client data.
        result += "7d".decodeHex()
        Log.i("hello", ("serialization result: " + result.toString(Charset.forName("UTF-8"))))
        return result
    }

    /**
     * Helper function to aid [jsonSerializeCollectedClientData].
     * https://www.w3.org/TR/webauthn-2/#clientdatajson-serialization
     */
    fun ccdToString(value: String): ByteArray {
        var encoded: ByteArray = emptyArray<Byte>().toByteArray()
        encoded += "22".decodeHex() // 2. Append 0x22 (") to encoded.
        // 4. For each code point in the string:
        value.forEach { char ->
            encoded += when {
                isAllowedCharForCcdToString(char) -> char.code.toHex().convertHexToByteArray() // Append its UTF-8 encoding to encoded
                char == '\u0022' -> "5c22".decodeHex() // Append 0x5c22(\") to encoded
                char == '\u005C' -> "5c5c".decodeHex() // Append 0x5c5c(\\) to encoded
                else -> convertCodePointForOtherwise(char)
            }
        }
        encoded += "22".decodeHex() // 5. Append 0x22 (") to encoded.
        return encoded
    }

    /**
     * Helper function to aid [ccdToString]. Check whether the given code point is within the set
     * Return true if the code point is within the set.
     * https://www.w3.org/TR/webauthn-2/#clientdatajson-serialization
     */
    private fun isAllowedCharForCcdToString(char: Char): Boolean {
        val code = char.code
        // Check if in the set {U+0020, U+0021, U+0023–U+005B, U+005D–U+10FFFF}:
        return code in 0x20..0x21 || code in 0x23..0x5B || code >= 0x5D
    }

    /**
     * Helper function to aid [ccdToString]. Convert the given code point based on instruction on
     * "otherwise", as explained in:
     * https://www.w3.org/TR/webauthn-2/#clientdatajson-serialization
     */
    private fun convertCodePointForOtherwise(char: Char): ByteArray {
        var result: ByteArray = emptyArray<Byte>().toByteArray()
        // Append 0x5c75 to encoded:
        result += "5c75".decodeHex()
        // Append four, lower-case hex digits that represent that code point when interpreted as a base-16 number.
        result += char.code.toString(16).padStart(4, '0').decodeHex()
        return result
    }

    fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }

        return chunked(2)
            .map { it.toInt(RADIX_BASE_HEXADECIMAL).toByte() }
            .toByteArray()
    }

    /**
     * Extension function for Int to convert decimal to hexadecimal.
     * @return hex string form on integer.
     */
    fun Int.toHex(): String {
        var hexStr = Integer.toHexString(this)
        if (hexStr.length == 1) {
            hexStr = "0$hexStr"
        }
        return hexStr
    }

    fun String.convertHexToByteArray(): ByteArray {

        if (this.length % 2 != 0) {
            throw IllegalArgumentException("Conversion to Hex string failed. String must have an even number of characters.")
        }

        val byteList = mutableListOf<Byte>()
        for (i in this.indices step 2) {
            val byteStr = this.substring(i, i + 2)
            val num = byteStr.toInt(RADIX_BASE_HEXADECIMAL).toByte()
            byteList.add(num)
        }
        return byteList.toByteArray()
    }

    fun getSha256Hash(clientDataBytes: ByteArray): ByteArray {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(clientDataBytes)
        return messageDigest.digest()
    }
}
