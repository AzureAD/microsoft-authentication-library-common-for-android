package com.microsoft.identity.common.internal.fido

/**
 * Representation of a manager that handles interactions with a passkey provider (usually through an API).
 */
interface IFidoManager {
    /**
     * Interacts with the FIDO credential provider and returns an assertion.
     * @param challenge AuthFidoChallenge received from the server.
     * @return assertion
     */
    suspend fun authenticate(challenge: AuthFidoChallenge): String
}