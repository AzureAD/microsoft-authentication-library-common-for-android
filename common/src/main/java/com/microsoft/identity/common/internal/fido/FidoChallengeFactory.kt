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
                challenge = validateParameter(
                    parameters[FidoRequestField.Challenge.name],
                    FidoRequestField.Challenge.name
                ),
                relyingPartyIdentifier = validateParameter(
                    parameters[FidoRequestField.RelyingPartyIdentifier.name],
                    FidoRequestField.RelyingPartyIdentifier.name
                ),
                userVerificationPolicy = validateParameter(
                    parameters[FidoRequestField.UserVerificationPolicy.name],
                    FidoRequestField.UserVerificationPolicy.name
                ),
                version = validateParameter(
                    parameters[FidoRequestField.Version.name],
                    FidoRequestField.Version.name
                ),
                submitUrl = validateParameter(
                    parameters[FidoRequestField.SubmitUrl.name],
                    FidoRequestField.SubmitUrl.name
                ),
                keyTypes = validateListParameter(
                    parameters[FidoRequestField.KeyTypes.name],
                    FidoRequestField.KeyTypes.name
                ),
                context = validateParameter(
                    parameters[FidoRequestField.Context.name],
                    FidoRequestField.Context.name
                ),
                allowedCredentials = validateListParameter(
                    parameters[AuthFidoRequestField.AllowedCredentials.name],
                    AuthFidoRequestField.AllowedCredentials.name
                )
            )
        }

        /**
         * Validates that the given parameter is not null.
         * @param parameter value for a passkey protocol field
         * @param field passkey protocol field
         * @throws ClientException if the parameter is null.
         */
        @Throws(ClientException::class)
        private fun validateParameter(parameter: String?, field: String): String {
            if (parameter == null) {
                throw ClientException("FIDO request is invalid", "$field is empty")
            }
            return parameter;
        }

        /**
         * Validates that the given parameter is not null.
         * @param parameter value for a passkey protocol field
         * @param field passkey protocol field (list)
         * @throws ClientException if the parameter is null
         */
        @Throws(ClientException::class)
        private fun validateListParameter(parameter: String?, field: String): List<String> {
            if (parameter == null) {
                throw ClientException("FIDO request is invalid", "$field is empty")
            } else if (parameter.isBlank()) {
                return listOf()
            }
            return parameter.split(DELIMITER).toList()
        }
    }
}