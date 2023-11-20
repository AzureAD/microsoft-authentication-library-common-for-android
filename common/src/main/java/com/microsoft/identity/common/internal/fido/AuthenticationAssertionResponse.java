//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.internal.fido;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.squareup.moshi.Json;

/**
 * Representation of WebAuthn's AuthenticatorAssertionResponseJSON.
 * https://w3c.github.io/webauthn/#dictdef-authenticatorassertionresponsejson
 */
public class AuthenticationAssertionResponse {

    @Json(name = "clientDataJSON")
    public final String clientDataJSON;

    @Json(name = "authenticatorData")
    public final String authenticatorData;

    @Json(name = "signature")
    public final String signature;

    @Json(name = "userHandle")
    public final String userHandle;

    @Json(name = "id")
    public final String id;

    @Json(name = "attestationObject")
    public final String attestationObject;

    public AuthenticationAssertionResponse(@NonNull final String clientDataJSON,
                                           @NonNull final String authenticatorData,
                                           @NonNull final String signature,
                                           @NonNull final String userHandle,
                                           @NonNull final String id,
                                           @Nullable final String attestationObject) {
        this.clientDataJSON = clientDataJSON;
        this.authenticatorData = authenticatorData;
        this.signature = signature;
        this.userHandle = userHandle;
        this.id = id;
        this.attestationObject = attestationObject;
    }
}
