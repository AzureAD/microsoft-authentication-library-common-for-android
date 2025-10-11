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
package com.microsoft.identity.common.internal.providers.oauth2

import android.app.Activity
import android.os.Build
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
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
                return (mCredMan.createCredential(activity, createRequest) as CreatePublicKeyCredentialResponse).also {
                    Logger.info(methodTag, "Passkey created successfully.")
                }
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
        val getRequest = GetCredentialRequest(listOf(GetPublicKeyCredentialOption(request)))
        try {
            return mCredMan.getCredential(activity, getRequest).also {
                Logger.info(methodTag, "Passkey retrieved successfully.")
            }
        } catch (e: GetCredentialException) {
            // For error handling use guidance from https://developer.android.com/training/sign-in/passkeys
            Logger.error(TAG, "Error retrieving credential: ${e.message}", e)
            throw e
        }
    }
}