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

import com.microsoft.identity.common.java.constants.FidoConstants
import com.microsoft.identity.common.java.exception.ClientException
import java.net.MalformedURLException
import java.net.URL

/**
 * Represents a FidoChallenge field.
 *
 * @param field             FidoRequestField, either required or optional field.
 * @param value             value corresponding with field.
 * @param throwIfInvalid    validation method to be called when getting value (getOrThrow).
 */
data class FidoChallengeField<K>(private val field: FidoRequestField,
                                 private val value: K?,
                                 private val throwIfInvalid: (FidoRequestField, K?) -> K) {
    /**
     * Validates value (and throws exception if invalid), then returns value.
     *
     * @return value
     */
    @Throws(ClientException::class)
    fun getOrThrow(): K {
        return throwIfInvalid(field, value)
    }

    companion object {
        /**
         * Validates that the given required parameter is not null or empty.
         *
         * @param field passkey protocol field
         * @param value value for a passkey protocol parameter
         * @return validated parameter value
         * @throws ClientException if the parameter is null or empty.
         */
        @JvmStatic
        @Throws(ClientException::class)
        fun throwIfInvalidRequiredParameter(field: FidoRequestField, value: String?): String {
            if (value == null) {
                throw ClientException(ClientException.PASSKEY_PROTOCOL_REQUEST_PARSING_ERROR, "${field.fieldName} not provided")
            } else if (value.isBlank()) {
                throw ClientException(ClientException.PASSKEY_PROTOCOL_REQUEST_PARSING_ERROR, "${field.fieldName} is empty")
            }
            return value
        }

        /**
         * Validates that the submitUrl parameter is not null, empty, or malformed.
         *
         * @param value value for the submitUrl passkey protocol parameter.
         * @return validated parameter value
         * @throws ClientException if the parameter is null, empty, or malformed.
         */
        @JvmStatic
        @Throws(ClientException::class)
        fun throwIfInvalidSubmitUrl(field: FidoRequestField, value: String?): String {
            val submitUrl = throwIfInvalidRequiredParameter(field, value)
            try {
                URL(submitUrl)
            } catch (e : MalformedURLException) {
                throw ClientException(ClientException.PASSKEY_PROTOCOL_REQUEST_PARSING_ERROR, "submitUrl value is malformed.")
            }
            return submitUrl
        }

        /**
         * Validates that the relyingPartyIdentifier parameter is not null or empty.
         *
         * @param value value for the relyingPartyIdentifier passkey protocol parameter.
         * @return validated parameter value
         * @throws ClientException if the parameter is null or empty.
         */
        @JvmStatic
        @Throws(ClientException::class)
        fun throwIfInvalidRelyingPartyIdentifier(field: FidoRequestField, value: String?): String {
            val rpId = throwIfInvalidRequiredParameter(field, value)
            // Server team is making a change to not include scheme, but until that change is in prod, we'll need to remove it ourselves.
            return rpId.removePrefix("https://")
        }

        /**
         * Validates that the protocol version is not null or empty, and is a version that we currently support.
         *
         * @param value value for the version passkey protocol parameter.
         * @return validated parameter value
         * @throws ClientException if the parameter is null, empty, or an unsupported version.
         */
        @JvmStatic
        @Throws(ClientException::class)
        fun throwIfInvalidProtocolVersion(field: FidoRequestField, value: String?): String {
            val version = throwIfInvalidRequiredParameter(field, value)
            if (version != FidoConstants.PASSKEY_PROTOCOL_VERSION) {
                throw ClientException(ClientException.PASSKEY_PROTOCOL_REQUEST_PARSING_ERROR, "Provided protocol version is not currently supported.")
            }
            return version
        }

        /**
         * Validates that the given optional parameter is not empty.
         *
         * @param value value for a passkey protocol parameter
         * @param field passkey protocol field
         * @return validated parameter value, or null if not provided.
         * @throws ClientException if the parameter is empty
         */
        @JvmStatic
        @Throws(ClientException::class)
        fun throwIfInvalidOptionalListParameter(field: FidoRequestField, value: List<String>?): List<String>? {
            if (value != null && (value.isEmpty() || value.first() == "")) {
                throw ClientException(ClientException.PASSKEY_PROTOCOL_REQUEST_PARSING_ERROR, "${field.fieldName} is empty")
            }
            return value
        }
    }
}
