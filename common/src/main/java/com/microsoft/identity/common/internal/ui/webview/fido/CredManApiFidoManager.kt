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
class CredManApiFidoManager internal constructor(val mContext: Context) : IFidoManager {
    /**
     * Interacts with the FIDO credential provider and puts the authentication result in a header format.
     *
     * @param challenge AuthFidoChallenge received from the server.
     * @return header fields for response.
     */
    override suspend fun getAuthResponse(challenge: AuthFidoChallenge): Map<String, String> {
        try {
            val json = convertChallengeToStandardWebAuthnJson(challenge)
            //val providers: Set<ComponentName> = setOf(The ComponentName representation of Microsoft Authenticator)

            //val publicKeyCredentialOption = GetPublicKeyCredentialOption(requestJson = json, allowedProviders = providers)
            //val getRequest = GetCredentialRequest()
            //val response = mCredentialManager.getCredential(mContext, getRequest)
        } catch (e: Exception) {

        }
    }

    /**
     * Interacts with the FIDO credential provider and puts the registration result in a header format.
     *
     * @param challenge RegFidoChallenge received from the server.
     * @return header fields for response.
     */
    override suspend fun getRegResponse(challenge: RegFidoChallenge): Map<String, String> {
        try {
            val json = convertChallengeToStandardWebAuthnJson(challenge)
            //val createRequest = CreatePublicKeyCredentialRequest(requestJson = json, preferImmediatelyAvailableCredentials = true)
            //val response = mCredentialManager.createCredential(mContext, createRequest)
            //response is a json string. So we can try to parse it and put it into a map
        } catch (e: Exception) {
            //Handle this exception in some way
            return emptyMap<String, String>()
        }
    }

    fun convertChallengeToStandardWebAuthnJson(challenge: AbstractFidoChallenge): String? {
        /*
        Use Moshi here
        val moshi: Moshi = Moshi.Builder().build()
        if (challenge is RegFidoChallenge) {
            val jsonAdapter: JsonAdapter<RegFidoChallenge> = moshi.adapter<RegFidoChallenge>
            return jsonAdapter.toJson(challenge)
        } else if (challenge is AuthFidoChallenge) {
            val jsonAdapter: JsonAdapter<AuthFidoChallenge> = moshi.adapter<AuthFidoChallenge>
            return jsonAdapter.toJson(challenge)
        } else {
            return null;
        }
        */
    }

    fun convertStandardWebAuthnJsonResponseToHeader(json: String): Map<String, String> {
        /*
        Use a json parser to get the values we need.
         */
    }
}