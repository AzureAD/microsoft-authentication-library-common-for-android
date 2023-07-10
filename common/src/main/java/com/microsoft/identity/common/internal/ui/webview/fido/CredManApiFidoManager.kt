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

import android.content.Context

/**
 * Makes calls to the Android Credential Manager API in order to return an attestation.
 */
class CredManApiFidoManager (val context: Context) : IFidoManager {
    /**
     * Interacts with the FIDO credential provider and puts the authentication result in a header format.
     *
     * @param challenge AuthFidoChallenge received from the server.
     * @return header fields for response.
     */
    override suspend fun authenticate(challenge: AuthFidoChallenge): Map<String, String> {
        try {
            //val json = WebAuthnJsonUtil.createWebAuthnPublicKeyCredentialRequestJsonStringFromChallenge(challenge)
            //val providers: Set<ComponentName> = setOf(The ComponentName representation of Microsoft Authenticator)

            //val publicKeyCredentialOption = GetPublicKeyCredentialOption(requestJson = json, allowedProviders = providers)
            //val getRequest = GetCredentialRequest()
            //val response = mCredentialManager.getCredential(mContext, getRequest)
            //return WebAuthnJsonUtil.extractWebAuthnJsonResponseIntoMap(response.credential.authenticationResponseJson)
        } catch (e: Exception) {
            //Do some sort of error logging and handling here, then return empty headers, so that the server gets a response.
        }
    }

    /**
     * Interacts with the FIDO credential provider and puts the registration result in a header format.
     *
     * @param challenge RegFidoChallenge received from the server.
     * @return header fields for response.
     */
    override suspend fun register(challenge: RegFidoChallenge): Map<String, String> {
        try {
            //val json = WebAuthnJsonUtil.createWebAuthnPublicKeyCreationRequestJsonStringFromChallenge(challenge)

            //val createRequest = CreatePublicKeyCredentialRequest(requestJson = json, preferImmediatelyAvailableCredentials = true)
            //val response = mCredentialManager.createCredential(mContext, createRequest)
            //return WebAuthnJsonUtil.extractWebAuthnJsonResponseIntoMap(response.credential.authenticationResponseJson)
        } catch (e: Exception) {
            //Do some sort of error logging and handling here, then return empty headers, so that the server gets a response.
        }
    }
}