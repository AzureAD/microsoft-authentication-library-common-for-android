package com.microsoft.identity.common.internal.fido

import android.content.Context

/**
 * Instantiates IFidoManager objects.
 */
class FidoManagerFactory {
    companion object {
        /**
         * Create a FidoManager instance.
         * @param context current context.
         * @return an implementation of IFidoManager
         */
        @JvmStatic
        fun createFidoManager(context: Context): IFidoManager {
            //Since we're only using the Credential Manager API, just return the manager for that one.
            return CredManApiFidoManager(context)
        }
    }
}
