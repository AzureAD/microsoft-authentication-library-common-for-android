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

import android.util.Base64
import com.microsoft.identity.common.internal.util.CommonMoshiJsonAdapter
import com.microsoft.identity.common.java.constants.FidoConstants.Companion.WEBAUTHN_AUTHENTICATION_ASSERTION_RESPONSE_JSON_KEY
import com.microsoft.identity.common.java.constants.FidoConstants.Companion.WEBAUTHN_AUTHDATA_AAGUID_LENGTH
import com.microsoft.identity.common.java.constants.FidoConstants.Companion.WEBAUTHN_AUTHDATA_AAGUID_OFFSET
import com.microsoft.identity.common.java.constants.FidoConstants.Companion.WEBAUTHN_AUTHDATA_ATTESTED_CREDENTIAL_DATA_FLAG
import com.microsoft.identity.common.java.constants.FidoConstants.Companion.WEBAUTHN_AUTHDATA_FLAGS_OFFSET
import com.microsoft.identity.common.java.constants.FidoConstants.Companion.WEBAUTHN_REGISTRATION_ATTESTATION_OBJECT_JSON_KEY
import com.microsoft.identity.common.java.constants.FidoConstants.Companion.WEBAUTHN_REGISTRATION_ORIGIN_JSON_KEY
import com.microsoft.identity.common.java.constants.FidoConstants.Companion.WEBAUTHN_RESPONSE_AUTHENTICATOR_DATA_JSON_KEY
import com.microsoft.identity.common.java.constants.FidoConstants.Companion.WEBAUTHN_RESPONSE_CLIENT_DATA_JSON_KEY
import com.microsoft.identity.common.java.constants.FidoConstants.Companion.WEBAUTHN_RESPONSE_ID_JSON_KEY
import com.microsoft.identity.common.java.constants.FidoConstants.Companion.WEBAUTHN_RESPONSE_SIGNATURE_JSON_KEY
import com.microsoft.identity.common.java.constants.FidoConstants.Companion.WEBAUTHN_RESPONSE_USER_HANDLE_JSON_KEY
import com.microsoft.identity.common.logging.Logger
import okio.ByteString.Companion.decodeBase64
import org.json.JSONException
import org.json.JSONObject
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.text.toByteArray

/**
 * A utility class to help convert to and from strings in WebAuthn json format.
 */
object WebAuthnJsonUtil {

    private val TAG = WebAuthnJsonUtil::class.simpleName.toString()

    /**
     * Takes applicable parameters and creates a string representation of
     *  PublicKeyCredentialRequestOptionsJSON (https://w3c.github.io/webauthn/#dictdef-publickeycredentialrequestoptionsjson)
     * @param challenge challenge string
     * @param relyingPartyIdentifier rpId string
     * @param allowedCredentials allowedCredentials string
     * @param userVerificationPolicy userVerificationPolicy string
     * @return a string representation of PublicKeyCredentialRequestOptionsJSON.
     */
    fun createJsonAuthRequest(
        challenge: String,
        relyingPartyIdentifier: String,
        allowedCredentials: List<String>?,
        userVerificationPolicy: String
    ): String {
        //Create classes
        val publicKeyCredentialDescriptorList = ArrayList<PublicKeyCredentialDescriptor>()
        allowedCredentials?.let {
            for (id in allowedCredentials) {
                publicKeyCredentialDescriptorList.add(
                    PublicKeyCredentialDescriptor("public-key", id)
                )
            }
        }
        val options = PublicKeyCredentialRequestOptions(
            challenge.base64UrlEncoded(),
            relyingPartyIdentifier,
            publicKeyCredentialDescriptorList,
            userVerificationPolicy
        )
        return CommonMoshiJsonAdapter().toJson(options)
    }

    /**
     * Extracts the AuthenticatorAssertionResponse from the overall AuthenticationResponse string received from the authenticator.
     * @param fullResponseJson AuthenticationResponse Json string.
     * @throws JSONException if a value is not present that should be.
     */
    fun extractAuthenticatorAssertionResponseJson(fullResponseJson: String): String {
        val methodTag = "$TAG:extractAuthenticatorAssertionResponseJson"
        val fullResponseJsonObject = JSONObject(fullResponseJson)
        val authResponseJsonObject = fullResponseJsonObject
            .getJSONObject(WEBAUTHN_AUTHENTICATION_ASSERTION_RESPONSE_JSON_KEY)
        // ESTS expects a custom object with clientDataJSON, authenticatorData, signature, userHandle, and id.
        val assertionResult = JSONObject()
        assertionResult.put(
            WEBAUTHN_RESPONSE_ID_JSON_KEY, fullResponseJsonObject.get(
                WEBAUTHN_RESPONSE_ID_JSON_KEY
            )
        )
        assertionResult.put(
            WEBAUTHN_RESPONSE_AUTHENTICATOR_DATA_JSON_KEY, authResponseJsonObject.get(
                WEBAUTHN_RESPONSE_AUTHENTICATOR_DATA_JSON_KEY
            )
        )
        assertionResult.put(
            WEBAUTHN_RESPONSE_CLIENT_DATA_JSON_KEY, authResponseJsonObject.get(
                WEBAUTHN_RESPONSE_CLIENT_DATA_JSON_KEY
            )
        )
        assertionResult.put(
            WEBAUTHN_RESPONSE_SIGNATURE_JSON_KEY, authResponseJsonObject.get(
                WEBAUTHN_RESPONSE_SIGNATURE_JSON_KEY
            )
        )
        // UserHandle is optional if allowCredentials was provided in the request (username flow).
        if (authResponseJsonObject.isNull(WEBAUTHN_RESPONSE_USER_HANDLE_JSON_KEY)) {
            Logger.info(methodTag, "UserHandle not found in assertion response.")
        } else {
            Logger.info(methodTag, "UserHandle was included in assertion response.")
            assertionResult.put(
                WEBAUTHN_RESPONSE_USER_HANDLE_JSON_KEY, authResponseJsonObject.get(
                    WEBAUTHN_RESPONSE_USER_HANDLE_JSON_KEY
                )
            )
        }
        return assertionResult.toString()
    }

    /**
     * Given WebAuthn response values, create a string representation of the JSON assertion response that ESTS is expecting.
     * @clientDataJson clientDataJson string
     * @authenticatorData authenticatorData string
     * @signature signature string
     * @userHandle userHandle string
     * @id id string
     */
    @JvmStatic
    fun createAssertionString(
        clientDataJson: String,
        authenticatorData: String,
        signature: String,
        userHandle: String,
        id: String
    ): String {
        val assertionResult = JSONObject()
        assertionResult.put(WEBAUTHN_RESPONSE_ID_JSON_KEY, id)
        assertionResult.put(WEBAUTHN_RESPONSE_AUTHENTICATOR_DATA_JSON_KEY, authenticatorData)
        assertionResult.put(WEBAUTHN_RESPONSE_CLIENT_DATA_JSON_KEY, clientDataJson)
        assertionResult.put(WEBAUTHN_RESPONSE_SIGNATURE_JSON_KEY, signature)
        assertionResult.put(WEBAUTHN_RESPONSE_USER_HANDLE_JSON_KEY, userHandle)
        return assertionResult.toString()
    }

    /**
     * Returns a base64URL encoding of the string.
     * @return String
     */
    fun String.base64UrlEncoded(): String {
        val data: ByteArray = this.toByteArray(Charsets.UTF_8)
        return Base64.encodeToString(
            data,
            (Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        )
    }

    /**
     * Extracts the origin from a WebAuthn passkey registration response JSON.
     *
     * Intended for use with the `registrationResponseJson` returned by
     * `CredentialManagerHandler.createPasskey()`. Navigates to `response.clientDataJSON`,
     * base64url-decodes it, parses it as JSON, and returns the `"origin"` field.
     *
     * @param registrationResponseJson The `registrationResponseJson` string from
     *   `CreatePublicKeyCredentialResponse`.
     * @return The origin string, or null if extraction fails.
     */
    fun extractOriginFromRegistrationResponse(registrationResponseJson: String): String? {
        return try {
            val responseObj = JSONObject(registrationResponseJson)
                .getJSONObject(WEBAUTHN_AUTHENTICATION_ASSERTION_RESPONSE_JSON_KEY)
            val clientDataB64 = responseObj
                .getString(WEBAUTHN_RESPONSE_CLIENT_DATA_JSON_KEY)
            val decodedClientDataBytes = Base64.decode(clientDataB64, Base64.URL_SAFE)
            val jsonString = String(decodedClientDataBytes, Charsets.UTF_8)
            JSONObject(jsonString).getString(WEBAUTHN_REGISTRATION_ORIGIN_JSON_KEY)
        } catch (e: Exception) {
            Logger.warn(TAG, "Failed to extract origin from passkey registration response: ${e.message}")
            null
        }
    }

    /**
     * Extracts the AAGUID from a WebAuthn passkey registration response JSON.
     *
     * Intended for use with the `registrationResponseJson` returned by
     * `CredentialManagerHandler.createPasskey()`. Navigates to `response.attestationObject`,
     * base64url-decodes it, parses the CBOR-encoded authenticator data, and returns the AAGUID
     * as a formatted UUID string.
     *
     * @param registrationResponseJson The `registrationResponseJson` string from
     *   `CreatePublicKeyCredentialResponse`.
     * @return The AAGUID as a UUID string, or null if extraction fails.
     */
    fun extractAaguidFromRegistrationResponse(registrationResponseJson: String): String? {
        return try {
            val responseObj = JSONObject(registrationResponseJson)
                .getJSONObject(WEBAUTHN_AUTHENTICATION_ASSERTION_RESPONSE_JSON_KEY)
            val attestationObject = responseObj
                .getString(WEBAUTHN_REGISTRATION_ATTESTATION_OBJECT_JSON_KEY)
            extractAaguidFromAttestationObject(attestationObject)
        } catch (e: Exception) {
            Logger.warn(TAG, "Failed to extract AAGUID from passkey registration response: ${e.message}")
            null
        }
    }

    /**
     * Parses a base64url-encoded CBOR attestation object and extracts the AAGUID.
     *
     * The AAGUID is read from the attested credential data section of `authData`.
     * This method first verifies the attested credential data flag at
     * [WEBAUTHN_AUTHDATA_FLAGS_OFFSET], then reads the AAGUID from
     * [WEBAUTHN_AUTHDATA_AAGUID_OFFSET] for [WEBAUTHN_AUTHDATA_AAGUID_LENGTH] bytes.
     *
     * @param attestationString String representation of the attestation object, as received in the WebAuthn registration response JSON.
     * @return The AAGUID as a UUID string.
     * @throws Exception if the attestation object is malformed or cannot be decoded.
     */
    fun extractAaguidFromAttestationObject(attestationString: String): String {
        // 1. Base64URL-decode the attestation object.
        val attestationObject = attestationString.decodeBase64()?.toByteArray()
            ?: throw Exception("Failed to base64url-decode the attestation object.")

        // 2. Locate the 'authData' CBOR key and read its byte-string payload.
        val key = "authData".toByteArray(Charsets.UTF_8)
        val keyIndex = indexOf(attestationObject, key)
        if (keyIndex == -1) throw Exception("'authData' key not found in attestation object (size: ${attestationObject.size} bytes).")

        val valueOffset = keyIndex + key.size
        if (valueOffset >= attestationObject.size) {
            throw Exception("Attestation object truncated after 'authData' key (offset: $keyIndex, size: ${attestationObject.size} bytes).")
        }

        val initialByte = attestationObject[valueOffset].toInt() and 0xFF
        val majorType = (initialByte shr 5) and 0x07
        if (majorType != 2) {
            throw Exception("Invalid CBOR major type for 'authData': expected byte string (major type 2) but found $majorType at offset $valueOffset.")
        }

        val (headerSize, authDataLength) = parseCborByteStringHeader(attestationObject, valueOffset, initialByte)
        val authDataStart = valueOffset + headerSize
        val authDataEnd = authDataStart + authDataLength
        if (authDataEnd > attestationObject.size) {
            throw Exception("Attestation object truncated: declared 'authData' length $authDataLength at offset $authDataStart exceeds buffer size ${attestationObject.size} bytes.")
        }

        // 3. Verify the AT flag — confirms attested credential data (and therefore AAGUID) is present.
        val flagsByteIndex = authDataStart + WEBAUTHN_AUTHDATA_FLAGS_OFFSET
        if (flagsByteIndex >= authDataEnd) {
            throw Exception("authData truncated: flags byte missing at offset $flagsByteIndex (authData length: $authDataLength bytes).")
        }
        if ((attestationObject[flagsByteIndex].toInt() and WEBAUTHN_AUTHDATA_ATTESTED_CREDENTIAL_DATA_FLAG) == 0) {
            throw Exception("AT flag not set in authData flags, attested credential data is absent, AAGUID cannot be extracted.")
        }

        // 4. Extract the fixed-length AAGUID.
        val aaguidStart = authDataStart + WEBAUTHN_AUTHDATA_AAGUID_OFFSET
        if (aaguidStart + WEBAUTHN_AUTHDATA_AAGUID_LENGTH > authDataEnd) {
            throw Exception("authData truncated: expected $WEBAUTHN_AUTHDATA_AAGUID_LENGTH AAGUID bytes at offset $aaguidStart (authData length: $authDataLength bytes).")
        }
        return formatToUuid(attestationObject.copyOfRange(aaguidStart, aaguidStart + WEBAUTHN_AUTHDATA_AAGUID_LENGTH))
    }

    /**
     * Reads the CBOR byte-string length starting at [offset] in [buf].
     *
     * CBOR encodes the length in the low 5 bits of the initial byte:
     * - 0–23  → length is the value itself (1-byte header)
     * - 24    → next 1 byte holds the length (2-byte header)
     * - 25    → next 2 bytes hold the length, big-endian (3-byte header)
     * - 26    → next 4 bytes hold the length, big-endian (5-byte header)
     *
     * @param buf         The raw bytes of the attestation object.
     * @param offset      Index of the initial CBOR byte (already verified to be major type 2).
     * @param initialByte The byte at [offset], pre-read by the caller.
     * @return Pair of (headerSize, byteStringLength).
     * @throws Exception if the buffer is truncated or the length encoding is unsupported.
     */
    private fun parseCborByteStringHeader(buf: ByteArray, offset: Int, initialByte: Int): Pair<Int, Int> {
        val additionalInfo = initialByte and 0x1F
        return when (additionalInfo) {
            in 0..23 -> 1 to additionalInfo
            24 -> {
                val li = offset + 1
                if (li >= buf.size) throw Exception("Attestation object truncated while reading 'authData' length (need 1 byte at offset $li, size: ${buf.size} bytes).")
                2 to (buf[li].toInt() and 0xFF)
            }
            25 -> {
                val li = offset + 1
                if (li + 1 >= buf.size) throw Exception("Attestation object truncated while reading 'authData' length (need 2 bytes starting at offset $li, size: ${buf.size} bytes).")
                val length = ((buf[li].toInt() and 0xFF) shl 8) or (buf[li + 1].toInt() and 0xFF)
                3 to length
            }
            26 -> {
                val li = offset + 1
                if (li + 3 >= buf.size) throw Exception("Attestation object truncated while reading 'authData' length (need 4 bytes starting at offset $li, size: ${buf.size} bytes).")
                val length = ((buf[li].toInt() and 0xFF) shl 24) or
                    ((buf[li + 1].toInt() and 0xFF) shl 16) or
                    ((buf[li + 2].toInt() and 0xFF) shl 8) or
                    (buf[li + 3].toInt() and 0xFF)
                5 to length
            }
            else -> throw Exception("Unsupported CBOR length encoding for 'authData': initial byte 0x${initialByte.toString(16).uppercase()} at offset $offset.")
        }
    }

    /**
     * Returns the starting index of the first occurrence of [target] within [outer],
     * or -1 if not found.
     */
    private fun indexOf(outer: ByteArray, target: ByteArray): Int {
        for (i in 0 until outer.size - target.size + 1) {
            if (outer.sliceArray(i until i + target.size).contentEquals(target)) return i
        }
        return -1
    }

    /**
     * Converts a 16-byte AAGUID byte array into a formatted UUID string (e.g. "550e8400-e29b-41d4-a716-446655440000").
     */
    private fun formatToUuid(bytes: ByteArray): String {
        val bb = ByteBuffer.wrap(bytes)
        return UUID(bb.long, bb.long).toString()
    }
}
