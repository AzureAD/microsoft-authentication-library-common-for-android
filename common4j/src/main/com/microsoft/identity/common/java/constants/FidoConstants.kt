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
         * Extra query parameter field to declare WebAuthn capability for a host app.
         */
        const val WEBAUTHN_QUERY_PARAMETER_FIELD = "webauthn"

        /**
         * Extra query parameter value to declare WebAuthn capability for a host app.
         */
        const val WEBAUTHN_QUERY_PARAMETER_VALUE = "1"

        /**
         * Header name to signal that the custom passkey protocol should be used.
         */
        const val PASSKEY_PROTOCOL_HEADER = "x-ms-PassKeyAuth"

        /**
         * Version of the passkey protocol that we want to use.
         */
        const val PASSKEY_PROTOCOL_VERSION = "1.0"

        /**
         * Key types that we support with the passkey protocol.
         * Possible values: securitykey, passkey, ngc
         * String should be delimited with commas and no spaces.
         * Ex. "securitykey,passkey"
         */
        const val PASSKEY_PROTOCOL_KEY_TYPES = "passkey"

        /**
         * Corresponding value to the passkey protocol header.
         */
        const val PASSKEY_PROTOCOL_HEADER_VALUE = "$PASSKEY_PROTOCOL_VERSION/$PASSKEY_PROTOCOL_KEY_TYPES"

        /**
         * Used to disable passkey logic until the feature is ready.
         */
        const val IS_PASSKEY_SUPPORT_READY = false
    }
}
