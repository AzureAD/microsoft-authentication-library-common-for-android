package com.microsoft.identity.common.internal.fido

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import com.microsoft.identity.common.logging.Logger

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
    override suspend fun authenticate(challenge: AuthFidoChallenge): String {
        val requestJson = WebAuthnJsonUtil.createJsonAuthRequestFromChallengeObject(challenge)
        val publicKeyCredentialOption = GetPublicKeyCredentialOption(
            requestJson = requestJson
        )
        val getCredRequest = GetCredentialRequest(
            listOf(publicKeyCredentialOption)
        )
        val result = credentialManager.getCredential(
            context = context,
            request = getCredRequest
        )
        val credential: PublicKeyCredential = result.credential as PublicKeyCredential
        return credential.authenticationResponseJson
    }
}