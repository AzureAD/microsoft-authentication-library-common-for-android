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
import com.google.android.gms.fido.fido2.Fido2ApiClient
import com.google.android.gms.tasks.OnCanceledListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.microsoft.identity.common.internal.providers.oauth2.WebViewAuthorizationFragment
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.logging.Logger
import io.opentelemetry.api.trace.Span
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Utilizes the legacy Android FIDO2 API in order to return an attestation.
 * Note that the legacy FIDO2 API should only be used for older Android versions which need it,
 * such as Android 13 and below for FIDO2 security key support.
 */
class LegacyFido2ApiManager (val context: Context, val fragment: WebViewAuthorizationFragment) : IFidoManager {

    val TAG = LegacyFido2ApiManager::class.simpleName.toString()

    private val legacyApi = Fido2ApiClient(context)

    /**
     * Interacts with the FIDO credential provider and returns an assertion.
     *
     * @param challenge AuthFidoChallenge received from the server.
     * @param relyingPartyIdentifier rpId received from the server.
     * @param allowedCredentials List of allowed credentials to filter by.
     * @param userVerificationPolicy User verification policy string.
     * @param span OpenTelemetry span.
     * @return assertion
     */
    override suspend fun authenticate(challenge: String,
                                      relyingPartyIdentifier: String,
                                      allowedCredentials: List<String>?,
                                      userVerificationPolicy: String,
                                      span: Span
    ): String = suspendCancellableCoroutine { continuation ->
        val methodTag = "$TAG:authenticate"
        span.setAttribute(
            AttributeName.fido_manager.name,
            TAG
        )
        val requestOptions = com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions.Builder()
            .setChallenge(challenge.toByteArray(Charsets.UTF_8))
            .setRpId(relyingPartyIdentifier)
            .build()
        Logger.info(methodTag, "Calling the legacy FIDO2 API with public key credential options to get a PendingIntent.")
        val result = legacyApi.getSignPendingIntent(requestOptions)

        result.addOnSuccessListener(OnSuccessListener { pendingIntent ->
            if (pendingIntent != null) {
                Logger.info(methodTag, "Launching the legacy FIDO2 API PendingIntent.")
                val fidoLauncher = fragment.fidoLauncher
                if (fidoLauncher != null) {
                    fidoLauncher.launch(
                        LegacyFido2ApiObject(
                            assertionCallback = { assertion ->
                                if (continuation.isActive) {
                                    continuation.resume(assertion)
                                }
                            },
                            errorCallback = { exception ->
                                if (continuation.isActive) {
                                    continuation.resumeWithException(exception)
                                }
                            },
                            pendingIntent = pendingIntent
                        ))
                } else {
                    createAndThrowException(
                        continuation = continuation,
                        methodTag = methodTag,
                        errorCode = LegacyFido2ApiException.NULL_OBJECT,
                        message = "fidoLauncher is null, which indicates that the legacy FIDO2 API is being used where it shouldn't be."
                    )
                }
            } else {
                createAndThrowException(
                    continuation = continuation,
                    methodTag = methodTag,
                    errorCode = LegacyFido2ApiException.NULL_OBJECT,
                    message = "Returned PendingIntent from legacy API is null."
                )
            }
        })
        result.addOnFailureListener(OnFailureListener { exception ->
            createAndThrowException(
                continuation = continuation,
                methodTag = methodTag,
                errorCode = LegacyFido2ApiException.GET_PENDING_INTENT_ERROR,
                message = "Failed to get a PendingIntent from the legacy FIDO2 API.",
                exception = exception
            )
        })
        result.addOnCanceledListener(OnCanceledListener {
            createAndThrowException(
                continuation = continuation,
                methodTag = methodTag,
                errorCode = LegacyFido2ApiException.GET_PENDING_INTENT_CANCELED,
                message = "The operation to get a PendingIntent from the legacy FIDO2 API was canceled."
            )
        })
    }

    /**
     * Helper method to create a LegacyFido2ApiException, log, and resume with the exception.
     *
     * @param continuation
     * @param methodTag
     * @param errorCode
     * @param message
     * @param exception optional, if there is a root exception.
     */
    private fun createAndThrowException(continuation: CancellableContinuation<String>,
                                        methodTag: String,
                                        errorCode : String,
                                        message: String,
                                        exception: Exception? = null) {
        val e = if (exception != null) {
            LegacyFido2ApiException(
                errorCode,
                message,
                exception
            )
        } else {
            LegacyFido2ApiException(
                errorCode,
                message
            )
        }
        Logger.error(methodTag, message, e)
        if (continuation.isActive) {
            continuation.resumeWithException(e)
        }
    }
}
