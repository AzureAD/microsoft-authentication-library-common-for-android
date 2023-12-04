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
package com.microsoft.identity.common.java.constants

/**
 * Constants for FIDO related logic.
 */
class FidoConstants {
    companion object {
        /**
         * Redirect origin for passkey protocol.
         */
        const val PASSKEY_PROTOCOL_REDIRECT = "urn:http-auth:PassKey"

        /**
         * Extra query parameter field to declare WebAuthn capability for a host app.
         */
        const val WEBAUTHN_QUERY_PARAMETER_FIELD = "webauthn"

        /**
         * Extra query parameter value to declare WebAuthn capability for a host app.
         */
        const val WEBAUTHN_QUERY_PARAMETER_VALUE = "1"

        /**
         * Header name for the passkey assertion that is passed back to the server.
         */
        const val PASSKEY_RESPONSE_ASSERTION_HEADER = "Assertion"

        /**
         * Header name for the passkey protocol context that we're relaying back to the server.
         */
        const val PASSKEY_RESPONSE_CONTEXT_HEADER = "x-ms-ctx"

        /**
         * Header name for the passkey protocol flow token that we're relaying back to the server.
         */
        const val PASSKEY_RESPONSE_FLOWTOKEN_HEADER = "x-ms-flowToken"

        /**
         * Delimiter for server context query parameter value, which can contain a context value and flow token value.
         */
        const val PASSKEY_CONTEXT_DELIMITER = " "

        /**
         * Header name to signal that the custom passkey protocol should be used.
         */
        const val PASSKEY_PROTOCOL_HEADER_NAME = "x-ms-PassKeyAuth"

        /**
         * Version of the passkey protocol that we want to use.
         */
        const val PASSKEY_PROTOCOL_VERSION = "1.0"

        /**
         * Constant to put in PASSKEY_PROTOCOL_KEY_TYPES_SUPPORTED if we support passkeys.
         */
        const val PASSKEY_PROTOCOL_KEY_TYPES_PASSKEY_OPTION = "passkey"

        /**
         * Constant to put in PASSKEY_PROTOCOL_KEY_TYPES_SUPPORTED if we support securitykeys.
         */
        const val PASSKEY_PROTOCOL_KEY_TYPES_SECURITYKEY_OPTION = "securitykey"

        /**
         * We shouldn't ever use this constant, but putting here for documentation purposes.
         */
        const val PASSKEY_PROTOCOL_KEY_TYPES_NGC_OPTION = "ngc"

        /**
         * Delimiter for PASSKEY_PROTOCOL_KEY_TYPES_SUPPORTED.
         */
        const val PASSKEY_PROTOCOL_KEY_TYPES_DELIMITER = ","

        /**
         * Key types that we support with the passkey protocol.
         * Possible values: PASSKEY_PROTOCOL_KEY_TYPES_PASSKEY_OPTION, PASSKEY_PROTOCOL_KEY_TYPES_SECURITYKEY_OPTION, PASSKEY_PROTOCOL_KEY_TYPES_NGC_OPTION
         * String should be delimited with commas and no spaces, or the value of  PASSKEY_PROTOCOL_KEY_TYPES_DELIMITER.
         * Ex. "securitykey,passkey"
         */
        const val PASSKEY_PROTOCOL_KEY_TYPES_SUPPORTED = PASSKEY_PROTOCOL_KEY_TYPES_PASSKEY_OPTION

        /**
         * Corresponding value to the passkey protocol header.
         */
        const val PASSKEY_PROTOCOL_HEADER_VALUE = "$PASSKEY_PROTOCOL_VERSION/$PASSKEY_PROTOCOL_KEY_TYPES_SUPPORTED"

        /**
         * JSON key value of assertion response of authentication response JSON object.
         */
        const val WEBAUTHN_AUTHENTICATION_ASSERTION_RESPONSE_JSON_KEY = "response"

        /**
         * Used to disable passkey logic until the feature is ready.
         */
        const val IS_PASSKEY_SUPPORT_READY = false
    }
}
