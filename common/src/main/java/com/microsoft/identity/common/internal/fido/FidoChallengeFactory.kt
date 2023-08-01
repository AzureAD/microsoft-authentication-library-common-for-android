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

import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.util.UrlUtil

/**
 * Instantiates FidoChallenge objects.
 */
class FidoChallengeFactory {
    companion object {
        val DELIMITER = ","
        val PASSKEY_PROTOCOL_REQUEST_INVALID = "Passkey protocol request is invalid"

        /**
         * Creates a FidoChallenge from a WebView passkey redirect url.
         * @param redirectUri passkey protocol redirect url.
         * @return IFidoChallenge
         * @throws ClientException if a required parameter is missing.
         */
        @JvmStatic
        @Throws(ClientException::class)
        fun createFidoChallengeFromRedirect(redirectUri: String): IFidoChallenge {
            val parameters = UrlUtil.getParameters(redirectUri)
            //At the moment, only auth FIDO requests will be sent by the server.
            return AuthFidoChallenge(
                challenge = validateRequiredParameter(
                    FidoRequestField.Challenge.name,
                    parameters[FidoRequestField.Challenge.name]
                ),
                relyingPartyIdentifier = validateRequiredParameter(
                    FidoRequestField.RelyingPartyIdentifier.name,
                    parameters[FidoRequestField.RelyingPartyIdentifier.name]
                ),
                userVerificationPolicy = validateRequiredParameter(
                    FidoRequestField.UserVerificationPolicy.name,
                    parameters[FidoRequestField.UserVerificationPolicy.name]
                ),
                version = validateRequiredParameter(
                    FidoRequestField.Version.name,
                    parameters[FidoRequestField.Version.name]
                ),
                submitUrl = validateRequiredParameter(
                    FidoRequestField.SubmitUrl.name,
                    parameters[FidoRequestField.SubmitUrl.name]
                ),
                keyTypes = validateRequiredListParameter(
                    FidoRequestField.KeyTypes.name,
                    parameters[FidoRequestField.KeyTypes.name]
                ),
                context = validateRequiredParameter(
                    FidoRequestField.Context.name,
                    parameters[FidoRequestField.Context.name]
                ),
                allowedCredentials = validateRequiredListParameter(
                    AuthFidoRequestField.AllowedCredentials.name,
                    parameters[AuthFidoRequestField.AllowedCredentials.name]
                )
            )
        }

        /**
         * Validates that the given parameter is not null or empty.
         * @param field passkey protocol field
         * @param value value for a passkey protocol parameter
         * @throws ClientException if the parameter is null or empty.
         */
        @Throws(ClientException::class)
        private fun validateRequiredParameter(field: String, value: String?): String {
            if (value == null) {
                throw ClientException(PASSKEY_PROTOCOL_REQUEST_INVALID, "$field not provided")
            } else if (value.isBlank()) {
                throw ClientException(PASSKEY_PROTOCOL_REQUEST_INVALID, "$field is empty")
            }
            return value;
        }

        /**
         * Validates that the given parameter is not null or empty.
         * @param value list value for a passkey protocol parameter
         * @param field passkey protocol field
         * @throws ClientException if the parameter is null or empty
         */
        @Throws(ClientException::class)
        private fun validateRequiredListParameter(field: String, value: String?): List<String> {
            return validateRequiredParameter(field, value).split(DELIMITER).toList()
        }

        //TODO: Create validateOptionalParameter method (checks for empty fields) once server team finishes design
    }
}