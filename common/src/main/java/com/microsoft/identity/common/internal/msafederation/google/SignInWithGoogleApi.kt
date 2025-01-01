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

import android.app.Activity
import com.microsoft.identity.common.internal.msafederation.FederatedSignInProviderFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Entry point for signing in with Google into MSA.
 */
class SignInWithGoogleApi internal constructor(
    private val federatedSignInProviderFactory: FederatedSignInProviderFactory){

    companion object {
        private const val TAG = "SignInWithGoogleApi"

        @Volatile
        private var instance: SignInWithGoogleApi? = null

        @JvmStatic
        fun getInstance(): SignInWithGoogleApi {
            return instance ?: synchronized(this) {
                instance ?: SignInWithGoogleApi(FederatedSignInProviderFactory).also { instance = it }
            }
        }
    }

    /**
     * Entry method to perform sign in with google.
     * It creates a [GoogleSignInProvider] and calls [GoogleSignInProvider.signIn]
     * to launch the sign in google flow.
     * It return [SignInWithGoogleCredential] which represents the credentials as result of sign in
     * and caller can use it further to authorization flow with MSA.
     * @param signInWithGoogleParameters Parameters for signing in with Google.
     * @return [SignInWithGoogleCredential] which represents credentials as result of successful sign in with google.
     */
    suspend fun signIn(signInWithGoogleParameters: SignInWithGoogleParameters): SignInWithGoogleCredential {
        val googleSignInProvider = federatedSignInProviderFactory.getProvider(
            signInWithGoogleParameters
        )
        val result = googleSignInProvider.signIn() as Result<SignInWithGoogleCredential>

        val signInWithGoogleCredential = result.getOrElse { throw it }

        return signInWithGoogleCredential
    }

    /**
     * Entry method to perform sign in with google synchronously.
     * Refer [signIn] for more details.
     */
    fun signInSync(
        signInWithGoogleParameters: SignInWithGoogleParameters
    ): SignInWithGoogleCredential {
        return runBlocking {
            signIn(signInWithGoogleParameters)
        }
    }

    /**
     * Entry method to perform sign in with google asynchronously for java.
     * Refer [signIn] for more details.
     */
    fun signInAsync(
        signInWithGoogleParameters: SignInWithGoogleParameters,
        callback: ISignInWithGoogleCredentialCallback
    ) {
        CoroutineScope(Dispatchers.Default).launch {
            val credential = signIn(signInWithGoogleParameters)
            withContext(coroutineContext) {
                callback.onSuccess(credential)
            }
        }
    }

    suspend fun signOut(activity: Activity) {
        val signInWithGoogleParameters = SignInWithGoogleParameters(activity)
        val googleSignInProvider = federatedSignInProviderFactory.getProvider(
            signInWithGoogleParameters
        )
        googleSignInProvider.signOut()
    }
}
