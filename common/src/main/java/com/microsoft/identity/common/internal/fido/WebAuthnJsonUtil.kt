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
import com.microsoft.identity.common.java.constants.FidoConstants
import com.microsoft.identity.common.java.constants.FidoConstants.Companion.WEBAUTHN_RESPONSE_AUTHENTICATOR_DATA_JSON_KEY
import com.microsoft.identity.common.java.constants.FidoConstants.Companion.WEBAUTHN_RESPONSE_CLIENT_DATA_JSON_KEY
import com.microsoft.identity.common.java.constants.FidoConstants.Companion.WEBAUTHN_RESPONSE_ID_JSON_KEY
import com.microsoft.identity.common.java.constants.FidoConstants.Companion.WEBAUTHN_RESPONSE_SIGNATURE_JSON_KEY
import com.microsoft.identity.common.java.constants.FidoConstants.Companion.WEBAUTHN_RESPONSE_USER_HANDLE_JSON_KEY
import org.json.JSONObject

/**
 * A utility class to help convert to and from strings in WebAuthn json format.
 */
class WebAuthnJsonUtil {
    companion object {
        /**
         * Takes applicable parameters and creates a string representation of
         *  PublicKeyCredentialRequestOptionsJSON (https://w3c.github.io/webauthn/#dictdef-publickeycredentialrequestoptionsjson)
         * @param challenge challenge string
         * @param relyingPartyIdentifier rpId string
         * @param allowedCredentials allowedCredentials string
         * @param userVerificationPolicy yserVerificationPolicy string
         * @return a string representation of PublicKeyCredentialRequestOptionsJSON.
         */
        fun createJsonAuthRequest(challenge: String,
                                  relyingPartyIdentifier: String,
                                  allowedCredentials: List<String>?,
                                  userVerificationPolicy: String): String {
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
        fun extractAuthenticatorAssertionResponseJson(fullResponseJson : String): String {
            val fullResponseJsonObject = JSONObject(fullResponseJson);
            val authResponseJsonObject = fullResponseJsonObject
                .getJSONObject(FidoConstants.WEBAUTHN_AUTHENTICATION_ASSERTION_RESPONSE_JSON_KEY)
            // ESTS expects a custom object with clientDataJSON, authenticatorData, signature, userHandle, and id.
            val assertionResult = JSONObject();
            assertionResult.put(WEBAUTHN_RESPONSE_ID_JSON_KEY, fullResponseJsonObject.get(
                WEBAUTHN_RESPONSE_ID_JSON_KEY))
            assertionResult.put(WEBAUTHN_RESPONSE_AUTHENTICATOR_DATA_JSON_KEY, authResponseJsonObject.get(
                WEBAUTHN_RESPONSE_AUTHENTICATOR_DATA_JSON_KEY))
            assertionResult.put(WEBAUTHN_RESPONSE_CLIENT_DATA_JSON_KEY, authResponseJsonObject.get(
                WEBAUTHN_RESPONSE_CLIENT_DATA_JSON_KEY))
            assertionResult.put(WEBAUTHN_RESPONSE_SIGNATURE_JSON_KEY, authResponseJsonObject.get(
                WEBAUTHN_RESPONSE_SIGNATURE_JSON_KEY))
            assertionResult.put(WEBAUTHN_RESPONSE_USER_HANDLE_JSON_KEY, authResponseJsonObject.get(
                WEBAUTHN_RESPONSE_USER_HANDLE_JSON_KEY))
            return assertionResult.toString()
        }

        /**
         * Returns a base64URL encoding of the string.
         * @return String
         */
        fun String.base64UrlEncoded(): String {
            val data: ByteArray = this.toByteArray(Charsets.UTF_8)
            return Base64.encodeToString(data, (Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
        }
    }
}
