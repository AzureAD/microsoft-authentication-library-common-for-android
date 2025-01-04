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
import com.microsoft.identity.common.logging.Logger
import java.security.SecureRandom

/**
 * GoogleSignInProvider is an implementation of the IFederatedSignInProvider interface
 * that handles sign-in operations with Google using credential manager in Android.
 * The class is internal and should not be used outside of the SDK. OneAuth should use
 * [SignInWithGoogleApi]
 * Call [GoogleSignInProvider.create] to create an instance of GoogleSignInProvider.
 */
internal class GoogleSignInProvider(private val credentialManager: CredentialManager,
                           private val parameters: SignInWithGoogleParameters,
                           private val webClientId: String
) : IFederatedSignInProvider {

    companion object {
        private const val TAG = "GoogleSignInProvider"

        /**
         * Creates an instance of GoogleSignInProvider.
         *
         * @param parameters The parameters required for signing in with Google.
         * @param webClientId The web client ID for Google sign-in.
         * @return A new instance of GoogleSignInProvider. Prod must use MSA client ID.
         */
        @JvmStatic
        fun create(parameters: SignInWithGoogleParameters, webClientId: String): GoogleSignInProvider {
            return GoogleSignInProvider(CredentialManager.create(parameters.activity.applicationContext), parameters, webClientId)
        }
    }

    /**
     * Signs in with Google using the specified parameters.
     * if useBottomSheet is true, it will use GetGoogleIdOption based flow as
     * suggested by google. Otherwise, it will use GetSignInWithGoogleOption based flow.
     *
     * @return A Result containing the SignInWithGoogleCredential on success,
     * or an exception on failure.
     */
    override suspend fun signIn(): Result<SignInWithGoogleCredential> {
        return if (parameters.useBottomSheet) {
            signInWithGoogleBottomSheet()
        } else {
            signInWithGoogle()
        }
    }

    /**
     * Signs in with Google using a bottom sheet UI.
     *
     * @return A Result containing the SignInWithGoogleCredential on success, or an exception on failure.
     */
    private suspend fun signInWithGoogleBottomSheet(): Result<SignInWithGoogleCredential> {
        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .setNonce(generateNonce())
            .build()

        return getCredential(googleIdOption)
    }

    /**
     * Signs in with Google using a standard UI.
     *
     * @return A Result containing the SignInWithGoogleCredential on success, or an exception on failure.
     */
    private suspend fun signInWithGoogle(): Result<SignInWithGoogleCredential> {
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
            .setNonce(generateNonce())
            .build()

        return getCredential(signInWithGoogleOption)
    }

    /**
     * Signs out from Google for this app.
     */
    override suspend fun signOut() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }

    /**
     * Retrieves the credential using the specified option.
     *
     * @param option The GetCustomCredentialOption to use for retrieving the credential.
     * @return A Result containing the SignInWithGoogleCredential on success, or an exception on failure.
     */
    private suspend fun getCredential(
        option: GetCustomCredentialOption
    ) : Result<SignInWithGoogleCredential> {
        val methodTag = "$TAG:getCredential"
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
                        Logger.warn(TAG, "Error parsing Google ID Token, $e.message")
                        val clientException = ClientException(
                            ClientException.SIGN_IN_WITH_GOOGLE_FAILED,
                            e.message,
                            e
                        )
                        return Result.failure(clientException)
                    }
                } else {
                    // unsupported credential type
                    val errorMessage = "Unsupported credential type, " + credential.type
                    Logger.warn(TAG, errorMessage)
                    val clientException = ClientException(
                        ClientException.SIGN_IN_WITH_GOOGLE_FAILED,
                        errorMessage
                    )
                    return Result.failure(clientException)
                }
            } else {
                // Unexpected credential type
                val errorMessage = "Unexpected credential type" + credential.javaClass.simpleName
                Logger.warn(TAG, errorMessage)
                val clientException = ClientException(
                    ClientException.SIGN_IN_WITH_GOOGLE_FAILED,
                    errorMessage
                )
                return Result.failure(clientException)
            }
        } catch (e: GetCredentialException) {
            // failure
            Logger.warn(TAG, "Error getting google id token credential, $e.message")
            val clientException = ClientException(
                ClientException.SIGN_IN_WITH_GOOGLE_FAILED,
                e.message,
                e
            )
            return Result.failure(clientException)
        }
    }

    /**
     * Generates a nonce for the sign-in request.
     *
     * @param size The size of the nonce to generate. Default is 16.
     * @return A URL-safe base64 encoded nonce.
     */
    private fun generateNonce(size: Int = 16): String {
        val secureRandom = SecureRandom()
        val nonceBytes = ByteArray(size)
        secureRandom.nextBytes(nonceBytes)
        return Base64Util.encodeUrlSafeString(nonceBytes)
    }
}
