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
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.fido.common.Transport
import com.google.android.gms.fido.fido2.Fido2ApiClient
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions
import com.google.android.gms.tasks.OnSuccessListener

/**
 * Makes calls to the Android Credential Manager API in order to return an attestation.
 */
class CredManFidoManager (val context: Context) : IFidoManager {

    val credentialManager = CredentialManager.create(context)

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
        val requestJson = WebAuthnJsonUtil.createJsonAuthRequest(
            challenge,
            relyingPartyIdentifier,
            allowedCredentials,
            userVerificationPolicy
        )
        val publicKeyCredentialOption = GetPublicKeyCredentialOption(
            requestJson = requestJson
        )
        val getCredRequest = GetCredentialRequest(
            listOf(publicKeyCredentialOption)
        )
        try {
            val result = credentialManager.getCredential(
                context = context,
                request = getCredRequest
            )
            val credential: PublicKeyCredential = result.credential as PublicKeyCredential
            return WebAuthnJsonUtil.extractAuthenticatorAssertionResponseJson(credential.authenticationResponseJson)
        } catch (e: NoCredentialException) {
            val legacyApi = Fido2ApiClient(context)
            val publicKeyCredentialDescriptorList = ArrayList<PublicKeyCredentialDescriptor>()
            allowedCredentials?.let {
                for (id in allowedCredentials) {
                    publicKeyCredentialDescriptorList.add(
                        PublicKeyCredentialDescriptor("public-key", id.toByteArray(), ArrayList<Transport>())
                    )
                }
            }
            val requestOptions = PublicKeyCredentialRequestOptions.Builder()
                .setChallenge(challenge.toByteArray())
                .setRpId(relyingPartyIdentifier)
                .setAllowList(publicKeyCredentialDescriptorList)
                .build()
            val result = legacyApi.getSignPendingIntent(requestOptions);
            result.addOnSuccessListener(OnSuccessListener { pendingIntent ->
                if (pendingIntent != null) {
                    // Start the pending intent
                    //context.startIntentSender(pendingIntent.intentSender, pendingIntent, 0, 0, 0)
                }
            })
        }
    }
}
