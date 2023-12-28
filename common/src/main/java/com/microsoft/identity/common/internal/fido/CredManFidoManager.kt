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

import android.content.Context

/**
 * Makes calls to the Android Credential Manager API in order to return an attestation.
 */
class CredManFidoManager (val context: Context) : IFidoManager {
    /**
     * Interacts with the FIDO credential provider and returns an assertion.
     *
     * @param challenge AuthFidoChallenge received from the server.
     * @return assertion
     */
    override suspend fun authenticate(challenge: String,
                                      relyingPartyIdentifier: String,
                                      allowedCredentials: List<String>?,
                                      userVerificationPolicy: String): String {
        WebAuthnJsonUtil.createJsonAuthRequest(
            challenge,
            relyingPartyIdentifier,
            allowedCredentials,
            userVerificationPolicy
        )
        // TODO: Implementation will include Credential Manager calls; we aren't able to add the dependency yet,
        //  so this method will only be filled once we can finish up the passkey work.
        //  ADO Item: https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2630696
        return WebAuthnJsonUtil.extractAuthenticatorAssertionResponseJson("")
    }
}
