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
package com.microsoft.identity.common.internal.msafederation.google

import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCustomCredentialOption
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.microsoft.identity.common.internal.msafederation.IFederatedSignInProvider
import com.microsoft.identity.common.java.base64.Base64Util
import com.microsoft.identity.common.java.exception.ClientException
import java.security.SecureRandom

internal class GoogleSignInProvider(private val credentialManager: CredentialManager,
                           private val parameters: SignInWithGoogleParameters,
                           private val webClientId: String
) : IFederatedSignInProvider {

    companion object {
        private const val TAG = "GoogleSignInProvider"

        @JvmStatic
        fun create(parameters: SignInWithGoogleParameters, webClientId: String): GoogleSignInProvider {
            return GoogleSignInProvider(CredentialManager.create(parameters.activity.applicationContext), parameters, webClientId)
        }
    }

    override suspend fun signIn(): Result<SignInWithGoogleCredential> {
        return if (parameters.useBottomSheet) {
            signInWithGoogleBottomSheet()
        } else {
            signInWithGoogle()
        }
    }

    private suspend fun signInWithGoogleBottomSheet(): Result<SignInWithGoogleCredential> {
        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .setNonce(generateNonce())
            .build()

        return getCredential(googleIdOption)
    }

    private suspend fun signInWithGoogle(): Result<SignInWithGoogleCredential> {
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
            .setNonce(generateNonce())
            .build()

        return getCredential(signInWithGoogleOption) // why not GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL?
    }

    override suspend fun signOut() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }

    private suspend fun getCredential(
        option: GetCustomCredentialOption
    ) : Result<SignInWithGoogleCredential> {
        val getCredentialRequest: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        try {
            val getCredentialResponse = credentialManager.getCredential(
                request = getCredentialRequest,
                context = parameters.activity
            )

            // handle the result
            val credential = getCredentialResponse.credential

            if (credential is CustomCredential) {
                // TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL is documented but currently TYPE_GOOGLE_ID_TOKEN_CREDENTIAL is returned
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ||
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        return Result.success(
                            SignInWithGoogleCredential(
                                googleIdTokenCredential.idToken
                            )
                        )
                    } catch (e: GoogleIdTokenParsingException) {
                        // error parsing Google ID Token
                        return Result.failure(e)
                    }
                } else {
                    // unsupported credential type
                    val clientException = ClientException(
                        ClientException.SIGN_IN_WITH_GOOGLE_FAILED,
                        "Unsupported credential type" + credential.type
                    )
                    return Result.failure(clientException)
                }
            } else {
                // Unexpected credential type
                val clientException = ClientException(
                    ClientException.SIGN_IN_WITH_GOOGLE_FAILED,
                    "Unexpected credential type" + credential.javaClass.simpleName
                )
                return Result.failure(clientException)
            }
        } catch (e: GetCredentialException) {
            // failure
            val clientException = ClientException(
                ClientException.SIGN_IN_WITH_GOOGLE_FAILED,
                e.message,
                e
            )
            return Result.failure(clientException)
        }
    }

    private fun generateNonce(size: Int = 16): String {
        val secureRandom = SecureRandom()
        val nonceBytes = ByteArray(size)
        secureRandom.nextBytes(nonceBytes)
        return Base64Util.encodeUrlSafeString(nonceBytes)
    }
}