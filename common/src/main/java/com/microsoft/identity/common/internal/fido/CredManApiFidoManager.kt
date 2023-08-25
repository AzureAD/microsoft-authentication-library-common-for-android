package com.microsoft.identity.common.internal.fido

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential

class CredManApiFidoManager(val context: Context) : IFidoManager {
    val credentialManager = CredentialManager.create(context)

    override suspend fun authenticate(challenge: AuthFidoChallenge): String {
        val requestJson = WebAuthnJsonUtil.createJsonAuthRequestFromChallengeObject(challenge)
        val publicKeyCredentialOption = GetPublicKeyCredentialOption(requestJson)
        val getRequest = GetCredentialRequest(listOf(publicKeyCredentialOption))
        //This could throw an exception, which should later be handled in getExceptionMessage.
        val result = credentialManager.getCredential(
            request = getRequest,
            context = context,
        )
        val credential = result.credential as PublicKeyCredential
        return credential.authenticationResponseJson
    }

    override fun getExceptionMessage(exception: Exception): String {
        return "foo for now"
    }
}
