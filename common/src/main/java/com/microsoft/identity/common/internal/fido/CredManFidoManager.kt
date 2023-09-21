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
    override suspend fun authenticate(challenge: AuthFidoChallenge): String {
        TODO("Not yet implemented")
    }
}