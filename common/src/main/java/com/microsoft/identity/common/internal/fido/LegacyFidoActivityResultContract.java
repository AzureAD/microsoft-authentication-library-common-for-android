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


public class LegacyFidoActivityResultContract extends ActivityResultContract<LegacyFidoObject, Object> {

    LegacyFidoObject legacyFidoObject;

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, LegacyFidoObject input) {
        legacyFidoObject = input;
        return  new Intent(ACTION_INTENT_SENDER_REQUEST)
                .putExtra(EXTRA_INTENT_SENDER_REQUEST, new IntentSenderRequest.Builder(input.getPendingIntent().getIntentSender()).build());
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public Object parseResult(int resultCode, @Nullable Intent intent) {
        if (intent == null) {
            legacyFidoObject.getCallback().invoke("Intent is null", false);
            return null;
        }
        if (resultCode != Activity.RESULT_OK) {
            legacyFidoObject.getCallback().invoke("Activity cancelled", false);
            return null;
        }
        final byte[] bytes = intent.getByteArrayExtra(Fido.FIDO2_KEY_CREDENTIAL_EXTRA);
        if (bytes == null) {
            legacyFidoObject.getCallback().invoke("Received credential is null", false);
            return null;
        }
        final PublicKeyCredential credential = PublicKeyCredential.deserializeFromBytes(bytes);
        final AuthenticatorResponse response = credential.getResponse();
        if (response instanceof AuthenticatorErrorResponse) {
            legacyFidoObject.getCallback().invoke(((AuthenticatorErrorResponse) response).getErrorMessage(), false);
            return null;
        }
        if (response instanceof AuthenticatorAssertionResponse) {
            legacyFidoObject.getCallback().invoke(
                    WebAuthnJsonUtil.createAssertionString(
                            Base64.encodeToString(response.getClientDataJSON(), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING),
                            Base64.encodeToString(((AuthenticatorAssertionResponse) response).getAuthenticatorData(), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING),
                            Base64.encodeToString(((AuthenticatorAssertionResponse) response).getSignature(), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING),
                            Base64.encodeToString(((AuthenticatorAssertionResponse) response).getUserHandle(), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING),
                            credential.getId()
                    ),
                    true
            );
            return null;
        }
        legacyFidoObject.getCallback().invoke("Something unexpected occurred", false);
        return null;
    }
}
