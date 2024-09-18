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
package com.microsoft.identity.common.internal.fido

import android.content.Context
import android.os.CancellationSignal
import com.google.android.gms.fido.fido2.Fido2ApiClient
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import com.google.android.gms.tasks.OnSuccessListener
import com.microsoft.identity.common.internal.providers.oauth2.WebViewAuthorizationFragment
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.ArrayList
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LegacyFido2ApiManager (val context: Context, val fragment: WebViewAuthorizationFragment) : IFidoManager {

    private val legacyApi = Fido2ApiClient(context)

    override suspend fun authenticate(challenge: String,
                                      relyingPartyIdentifier: String,
                                      allowedCredentials: List<String>?,
                                      userVerificationPolicy: String): String = suspendCancellableCoroutine { continuation ->
        val canceller = CancellationSignal()
        continuation.invokeOnCancellation { canceller.cancel() }

        val publicKeyCredentialDescriptorList = ArrayList<PublicKeyCredentialDescriptor>()
        allowedCredentials?.let {
            for (id in allowedCredentials) {
                publicKeyCredentialDescriptorList.add(
                    PublicKeyCredentialDescriptor("public-key", id.toByteArray(), ArrayList())
                )
            }
        }
        val requestOptions = com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions.Builder()
            .setChallenge(challenge.toByteArray())
            .setRpId(relyingPartyIdentifier)
            .setAllowList(publicKeyCredentialDescriptorList)
            .build()
        val result = legacyApi.getSignPendingIntent(requestOptions)

        result.addOnSuccessListener(OnSuccessListener { pendingIntent ->
            if (pendingIntent != null) {
                fragment.fidoLauncher.launch(
                    LegacyFido2ApiObject(
                        callback = { assertion, succeeded ->
                            if (succeeded) {
                                if (continuation.isActive) {
                                    continuation.resume(assertion)
                                }
                            } else {
                                if (continuation.isActive) {
                                    continuation.resumeWithException(Exception(assertion))
                                }
                            }
                        },
                        pendingIntent = pendingIntent
                    ))
            }
        })
    }

}