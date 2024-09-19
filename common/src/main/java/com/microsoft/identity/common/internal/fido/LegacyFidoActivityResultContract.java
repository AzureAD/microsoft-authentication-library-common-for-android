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
package com.microsoft.identity.common.internal.fido;

import static androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult.ACTION_INTENT_SENDER_REQUEST;
import static androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult.EXTRA_INTENT_SENDER_REQUEST;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Base64;

import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.android.gms.fido.Fido;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorResponse;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;


public class LegacyFidoActivityResultContract extends ActivityResultContract<LegacyFido2ApiObject, Void> {
    Function1<String, Unit> assertionCallback;
    Function1<LegacyFido2ApiException, Unit> errorCallback;

    @NonNull
    @Override
    public Intent createIntent(@NonNull final Context context, @NonNull final LegacyFido2ApiObject input) {
        assertionCallback = input.getAssertionCallback();
        errorCallback = input.getErrorCallback();
        return  new Intent(ACTION_INTENT_SENDER_REQUEST)
                .putExtra(
                        EXTRA_INTENT_SENDER_REQUEST,
                        new IntentSenderRequest.Builder(
                                input.getPendingIntent().getIntentSender()
                        ).build());
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public Void parseResult(int resultCode, @Nullable Intent intent) {
        if (intent == null) {
            errorCallback.invoke(
                    new LegacyFido2ApiException(
                            LegacyFido2ApiException.NULL_OBJECT,
                            "Result intent from legacy FIDO2 API was null."
                    ));
            return null;
        }
        if (resultCode != Activity.RESULT_OK) {
            errorCallback.invoke(
                    new LegacyFido2ApiException(
                            LegacyFido2ApiException.BAD_ACTIVITY_RESULT_CODE,
                            "Activity closed with result code: " + resultCode
                    ));
            return null;
        }

        final byte[] bytes = intent.getByteArrayExtra(Fido.FIDO2_KEY_CREDENTIAL_EXTRA);
        if (bytes == null) {
            errorCallback.invoke(
                    new LegacyFido2ApiException(LegacyFido2ApiException.NULL_OBJECT,
                            "Credential result from Intent is null."
                    ));
            return null;
        }
        final PublicKeyCredential credential = PublicKeyCredential.deserializeFromBytes(bytes);
        final AuthenticatorResponse response = credential.getResponse();
        if (response instanceof AuthenticatorErrorResponse) {
            final String errorMessage = ((AuthenticatorErrorResponse) response).getErrorMessage();
            final String errorCode = String.valueOf(((AuthenticatorErrorResponse) response).getErrorCode());
            errorCallback.invoke(
                    new LegacyFido2ApiException(
                            errorCode,
                            errorMessage != null ? errorMessage : "AuthenticatorResponse has a null error message."
                    ));
            return null;
        }
        if (response instanceof AuthenticatorAssertionResponse) {
            assertionCallback.invoke(
                    WebAuthnJsonUtil.createAssertionString(
                            Base64.encodeToString(response.getClientDataJSON(), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING),
                            Base64.encodeToString(((AuthenticatorAssertionResponse) response).getAuthenticatorData(), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING),
                            Base64.encodeToString(((AuthenticatorAssertionResponse) response).getSignature(), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING),
                            Base64.encodeToString(((AuthenticatorAssertionResponse) response).getUserHandle(), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING),
                            credential.getId()
                    )
            );
            return null;
        }
        errorCallback.invoke(
                new LegacyFido2ApiException(
                        LegacyFido2ApiException.UNKNOWN_ERROR,
                        "The legacy FIDO2 API response value is something unexpected which we currently cannot handle."
                ));
        return null;
    }
}
