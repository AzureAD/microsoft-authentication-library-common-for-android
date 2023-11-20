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

import java.util.Map;

import lombok.Getter;

/**
 * Representation of WebAuthn's AuthenticationResponseJson.
 * https://w3c.github.io/webauthn/#dictdef-authenticationresponsejson
 */

public class AuthenticationResponse {

    @Json(name = "id")
    public final String id;

    @Json(name = "rawId")
    public final String rawId;

    @Json(name = "response")
    public final AuthenticationAssertionResponse response;

    @Json(name = "authenticatorAttachment")
    public final String authenticatorAttachment;

    @Json(name = "clientExtensionResults")
    public final Map<String,String> clientExtensionResults;

    @Json(name = "type")
    public final String type;

    public AuthenticationResponse(@NonNull final String id,
                                  @NonNull final String rawId,
                                  @NonNull final AuthenticationAssertionResponse response,
                                  @Nullable final String authenticatorAttachment,
                                  @NonNull final Map<String,String> clientExtensionResults,
                                  @NonNull final String type) {
        this.id = id;
        this.rawId = rawId;
        this.response = response;
        this.authenticatorAttachment = authenticatorAttachment;
        this.clientExtensionResults = clientExtensionResults;
        this.type = type;
    }
}
