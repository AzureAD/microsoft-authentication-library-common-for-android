package com.microsoft.identity.common.internal.fido

import android.content.Context

class CredManApiFidoManager(val context: Context) : IFidoManager {
    //val credentialManager = CredentialManager.create(context)

    override suspend fun authenticate(challenge: AuthFidoChallenge): String {
        /*        val publicKeyCredentialOption = GetPublicKeyCredentialOption(requestJson)
        val getRequest = GetCredentialRequest(listOf(publicKeyCredentialOption))
        //This could throw an exception, which should later be handled in getExceptionMessage.
        val result = credentialManager.getCredential(
            request = getRequest,
            context = context,
        )
        val credential = result.credential as PublicKeyCredential
        return credential.authenticationResponseJson*/
        //Making sure util method is called so we can appropriately test sizing.
        return WebAuthnJsonUtil.createJsonAuthRequestFromChallengeObject(challenge)
    }

    override fun getExceptionMessage(exception: Exception): String {
        return "foo for now"
    }
}
