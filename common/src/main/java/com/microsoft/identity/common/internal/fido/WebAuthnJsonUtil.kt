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

import com.microsoft.identity.common.java.logging.Logger
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A utility class to help convert to and from strings in WebAuthn json format.
 */
class WebAuthnJsonUtil {
    companion object {
        private val TAG = WebAuthnJsonUtil::class.simpleName
        /**
         * Takes applicable parameters from an AuthFidoChallenge object and creates a string
         * representation of PublicKeyCredentialRequestOptionsJSON (https://w3c.github.io/webauthn/#dictdef-publickeycredentialrequestoptionsjson)
         * @param challengeObject AuthFidoChallenge
         * @return a string representation of PublicKeyCredentialRequestOptionsJSON.
         */
        @JvmStatic
        fun createJsonAuthRequestFromChallengeObject(challengeObject: AuthFidoChallenge): String {
            val methodTag = "$TAG:CreateJsonAuthRequestFromChallengeObject"
            //Create classes
            val publicKeyCredDescriptorList = ArrayList<PublicKeyCredentialDescriptor>()
            challengeObject.allowedCredentials?.let { allowedCredentials ->
                for (id in allowedCredentials) {
                    publicKeyCredDescriptorList.add(
                        PublicKeyCredentialDescriptor(
                            type = "public-key",
                            id = id
                        )
                    )
                }
            }

            val request = PublicKeyCredentialRequestOptions(
                challenge = challengeObject.challenge,
                rpId = challengeObject.relyingPartyIdentifier,
                allowCredentials = publicKeyCredDescriptorList,
                userVerification = challengeObject.userVerificationPolicy
            )

            try {
                return Json.encodeToString(request)
            } catch (e: Exception) {
                //This would be either SerializationException or IllegalArgumentException,
                // and both are very unlikely to be thrown since we're only working with one class,
                // but this catch block is here in case I'm wrong about that, or somethings changes
                // in the future.
                val errorMessage = "Error while encoding PublicKeyCredentialRequestOptions to string."
                Logger.error(methodTag, errorMessage, e)
                throw Exception(errorMessage + " " + e::class.simpleName + ": " + e.message)
            }
        }
    }
}
