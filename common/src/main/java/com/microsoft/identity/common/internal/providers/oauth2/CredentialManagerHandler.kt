package com.microsoft.identity.common.internal.providers.oauth2

import android.app.Activity
import android.os.Build
import android.util.Log
import androidx.credentials.*
import androidx.credentials.exceptions.*

class CredentialManagerHandler(private val activity: Activity) {

    companion object {
        const val TAG = "CredentialManagerHandler"
    }

    private val mCredMan = CredentialManager.create(activity.applicationContext)

    /**
     * Encapsulates the create passkey API for credential manager in a less error-prone manner.
     *
     * @param request a create public key credential request JSON required by [CreatePublicKeyCredentialRequest].
     * @return [CreatePublicKeyCredentialResponse] containing the result of the credential creation.
     */
    suspend fun createPasskey(request: String): CreatePublicKeyCredentialResponse {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val createRequest = CreatePublicKeyCredentialRequest(request)
            try {
                return mCredMan.createCredential(activity, createRequest) as CreatePublicKeyCredentialResponse
            } catch (e: CreateCredentialException) {
                // For error handling use guidance from https://developer.android.com/training/sign-in/passkeys
                Log.i(TAG, "Error creating credential: ErrMessage: ${e.errorMessage}, ErrType: ${e.type}")
                throw e
            }
        } else {
            throw UnsupportedOperationException("Passkey creation requires Android 9 or higher.")
        }
    }

    /**
     * Encapsulates the get passkey API for credential manager in a less error-prone manner.
     *
     * @param request a get public key credential request JSON required by [GetCredentialRequest].
     * @return [GetCredentialResponse] containing the result of the credential retrieval.
     */
    suspend fun getPasskey(request: String): GetCredentialResponse {
        val getRequest = GetCredentialRequest(listOf(GetPublicKeyCredentialOption(request, null)))
        try {
            return mCredMan.getCredential(activity, getRequest)
        } catch (e: GetCredentialException) {
            // For error handling use guidance from https://developer.android.com/training/sign-in/passkeys
            Log.i(TAG, "Error retrieving credential: ${e.message}")
            throw e
        }
    }
}