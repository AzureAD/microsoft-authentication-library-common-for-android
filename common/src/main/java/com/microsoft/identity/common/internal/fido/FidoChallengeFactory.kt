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
        const val DELIMITER = ","
        const val DEFAULT_USER_VERIFICATION_POLICY = "required"

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
                userVerificationPolicy = validateParameterOrReturnDefault(
                    FidoRequestField.UserVerificationPolicy.name,
                    parameters[FidoRequestField.UserVerificationPolicy.name],
                    DEFAULT_USER_VERIFICATION_POLICY
                ),
                version = validateRequiredParameter(
                    FidoRequestField.Version.name,
                    parameters[FidoRequestField.Version.name]
                ),
                submitUrl = validateRequiredParameter(
                    FidoRequestField.SubmitUrl.name,
                    parameters[FidoRequestField.SubmitUrl.name]
                ),
                keyTypes = validateOptionalListParameter(
                    AuthFidoRequestField.KeyTypes.name,
                    parameters[AuthFidoRequestField.KeyTypes.name]
                ),
                context = validateRequiredParameter(
                    FidoRequestField.Context.name,
                    parameters[FidoRequestField.Context.name]
                ),
                allowedCredentials = validateOptionalListParameter(
                    AuthFidoRequestField.AllowedCredentials.name,
                    parameters[AuthFidoRequestField.AllowedCredentials.name]
                )
            )
        }

        /**
         * Validates that the given required parameter is not null or empty.
         * @param field passkey protocol field
         * @param value value for a passkey protocol parameter
         * @return validated parameter value
         * @throws ClientException if the parameter is null or empty.
         */
        @JvmStatic
        @Throws(ClientException::class)
        fun validateRequiredParameter(field: String, value: String?): String {
            if (value == null) {
                throw ClientException(ClientException.PASSKEY_PROTOCOL_REQUEST_PARSING_ERROR, "$field not provided")
            } else if (value.isBlank()) {
                throw ClientException(ClientException.PASSKEY_PROTOCOL_REQUEST_PARSING_ERROR, "$field is empty")
            }
            return value
        }

        /**
         * Validates that the given optional parameter is not empty.
         * @param field passkey protocol field
         * @param value value for a passkey protocol parameter
         * @return validated parameter value, or null if not provided.
         * @throws ClientException if the parameter is empty.
         */
        @JvmStatic
        @Throws(ClientException::class)
        internal fun validateOptionalParameter(field: String, value: String?): String? {
            if (value != null && value.isBlank()) {
                throw ClientException(ClientException.PASSKEY_PROTOCOL_REQUEST_PARSING_ERROR, "$field is empty")
            }
            return value
        }

        /**
         * Validates that the given optional parameter is not empty and turns into a list.
         * @param value value for a passkey protocol parameter
         * @param field passkey protocol field
         * @return validated parameter value, or null if not provided.
         * @throws ClientException if the parameter is empty
         */
        @JvmStatic
        @Throws(ClientException::class)
        fun validateOptionalListParameter(field: String, value: String?): List<String>? {
            val param = validateOptionalParameter(field, value)
            if (param != null) {
                return param.split(DELIMITER).toList()
            }
            return param
        }

        /**
         * If parameter is empty, replace parameter value with given default value.
         * @param field passkey protocol field
         * @param value value for a passkey protocol parameter
         * @param defaultValue value to be used as default
         * @return validated parameter value, or default value if initial value is null.
         * @throws ClientException if the parameter is empty
         */
        @JvmStatic
        @Throws(ClientException::class)
        fun validateParameterOrReturnDefault(field: String, value: String?, defaultValue: String): String {
            if (value == null) {
                return defaultValue
            } else if (value.isBlank()) {
                throw ClientException(ClientException.PASSKEY_PROTOCOL_REQUEST_PARSING_ERROR, "$field is empty")
            }
            return value
        }
    }
}
