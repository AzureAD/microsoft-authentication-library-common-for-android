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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Base64
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import com.microsoft.identity.common.internal.fido.WebAuthnJsonUtil.Companion.createAssertionString
import com.microsoft.identity.common.java.util.StringUtil
import com.microsoft.identity.common.logging.Logger

/**
 * An implementation of an ActivityResultContract which helps run the PendingIntent from the legacy FIDO2 API and connect its results to the FIDO challenge handlers and managers.
 */
class LegacyFidoActivityResultContract : ActivityResultContract<LegacyFido2ApiObject, Void?>() {
    private val TAG = LegacyFidoActivityResultContract::class.simpleName.toString()
    private var assertionCallback: (result: String) -> Unit = {Logger.info(TAG, "Assertion callback not set.")}
    private var errorCallback: (exception: LegacyFido2ApiException) -> Unit = {Logger.info(TAG, "Error callback not set.")}
    override fun createIntent(context: Context, input: LegacyFido2ApiObject): Intent {
        assertionCallback = input.assertionCallback
        errorCallback = input.errorCallback
        return Intent(ActivityResultContracts.StartIntentSenderForResult.ACTION_INTENT_SENDER_REQUEST)
            .putExtra(
                ActivityResultContracts.StartIntentSenderForResult.EXTRA_INTENT_SENDER_REQUEST,
                IntentSenderRequest.Builder(
                    input.pendingIntent.intentSender
                ).build()
            )
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    override fun parseResult(resultCode: Int, intent: Intent?): Void? {
        if (intent == null) {
            errorCallback.invoke(
                LegacyFido2ApiException(
                    LegacyFido2ApiException.NULL_OBJECT,
                    "Result intent from legacy FIDO2 API was null."
                )
            )
            return null
        }
        if (resultCode != Activity.RESULT_OK) {
            errorCallback.invoke(
                LegacyFido2ApiException(
                    LegacyFido2ApiException.BAD_ACTIVITY_RESULT_CODE,
                    "Activity closed with result code: $resultCode"
                )
            )
            return null
        }
        val bytes = intent.getByteArrayExtra(Fido.FIDO2_KEY_CREDENTIAL_EXTRA)
        if (bytes == null) {
            errorCallback.invoke(
                LegacyFido2ApiException(
                    LegacyFido2ApiException.NULL_OBJECT,
                    "Credential result from Intent is null."
                )
            )
            return null
        }
        val credential = PublicKeyCredential.deserializeFromBytes(bytes)
        val response = credential.response
        if (response is AuthenticatorErrorResponse) {
            val errorMessage = response.errorMessage
            val errorCode = response.errorCode.toString()
            errorCallback.invoke(
                LegacyFido2ApiException(
                    errorCode,
                    if (StringUtil.isNullOrEmpty(errorMessage)) "AuthenticatorResponse has a null error message." else errorMessage
                )
            )
            return null
        }
        if (response is AuthenticatorAssertionResponse) {
            // While it's not expected to be null, the userHandle variable of AuthenticatorAssertionResponse is nullable.
            // Since ESTS requires a user handle in the assertion, return an exception if it's null.
            if (response.userHandle == null) {
                errorCallback.invoke(
                    LegacyFido2ApiException(
                        LegacyFido2ApiException.NULL_OBJECT,
                        "UserHandle value in AuthenticatorAssertionResponse is null."
                    )
                )
                return null
            }
            assertionCallback.invoke(
                createAssertionString(
                    Base64.encodeToString(
                        response.getClientDataJSON(),
                        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
                    ),
                    Base64.encodeToString(
                        response.authenticatorData,
                        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
                    ),
                    Base64.encodeToString(
                        response.signature,
                        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
                    ),
                    Base64.encodeToString(
                        response.userHandle,
                        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
                    ),
                    credential.id
                )
            )
            return null
        }
        errorCallback.invoke(
            LegacyFido2ApiException(
                LegacyFido2ApiException.UNKNOWN_ERROR,
                "The legacy FIDO2 API response value is something unexpected which we currently cannot handle."
            )
        )
        return null
    }
}
