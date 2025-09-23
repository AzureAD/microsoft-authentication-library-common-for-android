package com.microsoft.identity.common.internal.providers.oauth2

import android.app.Activity
import android.os.Build
import android.util.Log
import androidx.credentials.*
import androidx.credentials.exceptions.*
import com.microsoft.identity.common.logging.Logger

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
        val methodTag = "$TAG:createPasskey"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Logger.info(methodTag, "Creating passkey with request: $request")
            val createRequest = CreatePublicKeyCredentialRequest(request)
            try {
                Logger.info(methodTag, "Invoking CredentialManager.createCredential")
                val response =  mCredMan.createCredential(activity, createRequest) as CreatePublicKeyCredentialResponse
                Logger.info(methodTag, "Passkey created successfully.")
                return response
            } catch (e: CreateCredentialException) {
                // For error handling use guidance from https://developer.android.com/training/sign-in/passkeys
                Logger.error(TAG, "Error creating credential: ErrMessage: ${e.errorMessage}, ErrType: ${e.type}", e)
                throw e
            }
        } else {
            Logger.warn(methodTag, "Passkey creation is not supported on Android versions below 9 (Pie). Current version: ${Build.VERSION.SDK_INT}")
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
        val methodTag = "$TAG:getPasskey"
        Logger.info(methodTag, "Getting passkey with request: $request")
        val getRequest = GetCredentialRequest(listOf(GetPublicKeyCredentialOption(request, null)))
        try {
            Logger.info(methodTag, "Invoking CredentialManager.getCredential")
            return mCredMan.getCredential(activity, getRequest)
        } catch (e: GetCredentialException) {
            // For error handling use guidance from https://developer.android.com/training/sign-in/passkeys
            Logger.error(TAG, "Error retrieving credential: ${e.message}", e)
            throw e
        }
    }
}