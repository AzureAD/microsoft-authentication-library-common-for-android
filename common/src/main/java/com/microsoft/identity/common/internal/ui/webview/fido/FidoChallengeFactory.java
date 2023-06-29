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
package com.microsoft.identity.common.internal.ui.webview.fido;

import com.microsoft.identity.common.java.exception.ClientException;

import java.util.Map;

import lombok.NonNull;

/**
 * Instantiates FidoChallenge objects.
 */
public class FidoChallengeFactory {
    private static final String TAG = FidoChallengeFactory.class.getSimpleName();

    /**
     * Creates a FidoChallenge from a WebView passkey redirect url.
     * @param redirectUri passkey protocol redirect url.
     */
    public AbstractFidoChallenge createFidoChallengeFromRedirect(@NonNull final String redirectUri) {
        //Create parameters using UrlUtil
        //Check size of Map. If there are more than 8 entries, then it's a reg request. Otherwise, check for an auth request.
    }

    /**
     * Creates a RegFidoChallenge.
     * @param parameters fields from redirect url.
     * @return RegFidoChallenge
     * @throws ClientException if a required parameter is missing.
     */
    @NonNull
    RegFidoChallenge createRegFidoChallenge(@NonNull Map<String, String> parameters) throws ClientException {
        validateMainParameters(parameters);
        //Build challenge. Start with the "main" parameters, then validate and add the other ones.
    }

    /**
     * Creates an AuthFidoChallenge.
     * @param parameters fields from redirect url.
     * @return AuthFidoChallenge
     * @throws ClientException if a required parameter is missing.
     */
    @NonNull
    AuthFidoChallenge createAuthFidoChallenge(@NonNull Map<String, String> parameters) throws ClientException {
        validateMainParameters(parameters);
        //Build challenge. Start with the "main" parameters, then validate and add the other ones.
    }

    /**
     * Validates that the fields enumerated in FidoRequestField are present.
     * @param parameters fields from redirect url.
     * @throws ClientException if a required parameter is missing.
     */
    void validateMainParameters(Map<String, String> parameters) throws ClientException {}
}
