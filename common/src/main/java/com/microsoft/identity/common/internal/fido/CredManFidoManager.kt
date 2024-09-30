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
import android.os.Build
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.NoCredentialException
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.logging.Logger
import io.opentelemetry.api.trace.Span

/**
 * Makes calls to the Android Credential Manager API in order to return an attestation.
 */
class CredManFidoManager (val context: Context,
                          private val legacyManager: IFidoManager?) : IFidoManager {

    companion object {
        val TAG = CredManFidoManager::class.simpleName.toString()
    }

    private val credentialManager = CredentialManager.create(context)

    /**
     * Interacts with the FIDO credential provider and returns an assertion.
     *
     * @param challenge AuthFidoChallenge received from the server.
     * @param relyingPartyIdentifier rpId received from the server.
     * @param allowedCredentials List of allowed credentials to filter by.
     * @param userVerificationPolicy User verification policy string.
     * @param span OpenTelemetry span.
     * @return assertion
     */
    override suspend fun authenticate(challenge: String,
                                      relyingPartyIdentifier: String,
                                      allowedCredentials: List<String>?,
                                      userVerificationPolicy: String,
                                      span: Span): String {
        val methodTag = "$TAG:authenticate"
        span.setAttribute(
            AttributeName.fido_manager.name,
            TAG
        )
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
            credentialOptions = listOf(publicKeyCredentialOption),
            // We're setting preferImmediatelyAvailableCredentials in the CredMan getCredentialRequest object to true if the device OS is Android 13 or lower.
            // This ensures the behavior where no dialog from CredMan is shown if no passkey cred is present.
            // The end goal is for an Android <= 13 user who only has a security key to see one dialog which will allow them to authenticate.
            preferImmediatelyAvailableCredentials = (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        )
        try {
            Logger.info(methodTag, "Calling Credential Manager with a GetCredentialRequest.")
            val result = credentialManager.getCredential(
                context = context,
                request = getCredRequest
            )
            val credential: PublicKeyCredential = result.credential as PublicKeyCredential
            return WebAuthnJsonUtil.extractAuthenticatorAssertionResponseJson(credential.authenticationResponseJson)
        } catch (e: NoCredentialException) {
            // For version lower than Android 14, if NoCredentialException is returned,
            // this means a UI dialog wasn't even shown to allow usage of a security key.
            // Thus we need to call the legacy FIDO2 API here, if present.
            if (legacyManager != null) {
                 return legacyManager.authenticate(
                     challenge = challenge,
                     relyingPartyIdentifier = relyingPartyIdentifier,
                     allowedCredentials = allowedCredentials,
                     userVerificationPolicy = userVerificationPolicy,
                     span = span)
            } else {
                throw e
            }
        }
    }
}
