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
package com.microsoft.identity.common.internal.ui.webview.fido

import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.util.JsonUtil
import org.json.JSONException

/**
 * Helper methods for converting to/from WebAuthn Json.
 */
object WebAuthnJsonUtil : JsonUtil() {

    /**
     * Put response in WebAuthn JSON format into a map.
     * The responses for registration and authentication are similar: [RegistrationResponseJSON and AuthenticationResponseJSON](https://w3c.github.io/webauthn/#dictdef-authenticationresponsejson)
     * @param jsonString WebAuthn JSON response.
     * @return Map of values.
     * @throws JSONException if JSON string is malformed.
     */
    @Throws(JSONException::class)
    fun extractWebAuthnJsonResponseIntoMap(jsonString: String): Map<String, String> {
        return HashMap()
    }

    /**
     * Arrange attributes of FIDO auth challenge into a WebAuthn JSON string.
     * WebAuthn spec: [PublicKeyCredentialRequestOptionsJSON](https://w3c.github.io/webauthn/#dictdef-publickeycredentialrequestoptionsjson)
     * @param challenge FIDO authentication challenge.
     * @return string as PublicKeyCredentialRequestOptionsJSON.
     * @throws ClientException if a required field is missing.
     */
    @Throws(ClientException::class)
    fun createWebAuthnPublicKeyCredentialRequestJsonStringFromChallenge(challenge: AuthFidoChallenge): String {
        return "testing"
    }

    /**
     * Arrange attributes of FIDO reg challenge into a WebAuthn JSON string.
     * WebAuthn spec: [PublicKeyCredentialCreationOptionsJSON](https://w3c.github.io/webauthn/#dictdef-publickeycredentialcreationoptionsjson)
     * @param challenge FIDO registration challenge.
     * @return string as PublicKeyCredentialCreationOptionsJSON.
     * @throws ClientException if a required field is missing.
     */
    @Throws(ClientException::class)
    fun createWebAuthnPublicKeyCreationRequestJsonStringFromChallenge(challenge: RegFidoChallenge): String {
        return "testing"
    }
}