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
     * @param userVerificationPolicy yserVerificationPolicy string
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
     * @return The AAGUID as a UUID string, or null if attested credential data is missing or the payload is truncated.
     * @throws Exception if the attestation object cannot be decoded.
     */
    fun extractAaguidFromAttestationObject(attestationString: String): String {
        // 1. Decode Base64URL.
        val attestationObject = attestationString.decodeBase64()?.toByteArray()
            ?: throw Exception("Failed to base64url-decode the attestation object.")
        val key = "authData".toByteArray(Charsets.UTF_8)
        val keyIndex = indexOf(attestationObject, key)
        if (keyIndex == -1) throw Exception("'authData' key not found in attestation object (size: ${attestationObject.size} bytes).")

        // 2. Determine where the authData byte string starts.
        // CBOR uses the low 5 bits of the initial byte to describe how the byte-string length is encoded:
        // 0..23 = inline length, 24 = next 1 byte, 25 = next 2 bytes, 26 = next 4 bytes.
        val pointer = keyIndex + key.size
        if (pointer >= attestationObject.size) {
            throw Exception("Attestation object truncated after 'authData' key (offset: $keyIndex, size: ${attestationObject.size} bytes).")
        }

        val initialByte = attestationObject[pointer].toInt() and 0xFF
        val headerSize = when (initialByte and 0x1F) {
            in 0..23 -> 1
            24 -> 2
            25 -> 3
            26 -> 5
            else -> throw Exception("Unsupported CBOR length encoding for 'authData': initial byte 0x${initialByte.toString(16).uppercase()} at offset $pointer.")
        }

        val authDataStart = pointer + headerSize

        // 3. Check the WebAuthn flags byte to confirm attested credential data is present.
        val flagsByteIndex = authDataStart + WEBAUTHN_AUTHDATA_FLAGS_OFFSET
        if (flagsByteIndex >= attestationObject.size)
        {
            throw Exception("Attestation object truncated: flags byte missing at offset $flagsByteIndex (size: ${attestationObject.size} bytes).")
        }

        val flags = attestationObject[flagsByteIndex].toInt()
        val hasAttestedCredentialData =
            (flags and WEBAUTHN_AUTHDATA_ATTESTED_CREDENTIAL_DATA_FLAG) != 0
        if (!hasAttestedCredentialData) {
            throw Exception("AT flag not set in authData flags, attested credential data is absent, AAGUID cannot be extracted.")
        }

        // 4. Extract the fixed-length AAGUID from authData.
        val aaguidStart = authDataStart + WEBAUTHN_AUTHDATA_AAGUID_OFFSET
        if (aaguidStart + WEBAUTHN_AUTHDATA_AAGUID_LENGTH > attestationObject.size) {
            throw Exception("Attestation object truncated: expected $WEBAUTHN_AUTHDATA_AAGUID_LENGTH AAGUID bytes at offset $aaguidStart (size: ${attestationObject.size} bytes).")
        }

        val aaguidBytes = attestationObject.copyOfRange(
            aaguidStart,
            aaguidStart + WEBAUTHN_AUTHDATA_AAGUID_LENGTH
        )
        return formatToUuid(aaguidBytes)
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
