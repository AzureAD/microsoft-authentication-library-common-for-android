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

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.ArrayList;
import java.util.List;

/**
 * A utility class to help convert to and from strings in WebAuthn json format.
 */
public class WebAuthnJsonUtil {
    /**
     * Takes applicable parameters from an AuthFidoChallenge object and creates a string
     * representation of PublicKeyCredentialRequestOptionsJSON (https://w3c.github.io/webauthn/#dictdef-publickeycredentialrequestoptionsjson)
     * @param challengeObject AuthFidoChallenge
     * @return a string representation of PublicKeyCredentialRequestOptionsJSON.
     */
    public static String createJsonAuthRequestFromChallengeObject(@NonNull final AuthFidoChallenge challengeObject) {
        //Create classes
        final List<PublicKeyCredentialDescriptor> publicKeyCredentialDescriptorList = new ArrayList<>();
        final List<String> allowedCredentials = challengeObject.getAllowedCredentials();
        if (allowedCredentials != null) {
            for (final String id : allowedCredentials) {
                publicKeyCredentialDescriptorList.add(
                        new PublicKeyCredentialDescriptor("public-key", id)
                );
            }
        }
        final JsonAdapter<PublicKeyCredentialRequestOptions> jsonAdapter = new Moshi.Builder()
                .build()
                .adapter(PublicKeyCredentialRequestOptions.class);
        final PublicKeyCredentialRequestOptions options = new PublicKeyCredentialRequestOptions(
                challengeObject.getChallenge(),
                challengeObject.getRelyingPartyIdentifier(),
                publicKeyCredentialDescriptorList,
                challengeObject.getUserVerificationPolicy()
        );
        return jsonAdapter.toJson(options);
    }
}
