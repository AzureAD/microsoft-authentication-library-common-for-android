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
package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory;

import android.support.annotation.NonNull;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsPromptBehavior;
import com.microsoft.identity.common.internal.providers.oauth2.PkceChallenge;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Set;
import java.util.UUID;

public class AzureActiveDirectoryBrokerAuthorizationRequest extends AzureActiveDirectoryAuthorizationRequest {
    private String mCallingPackage;
    private String mSignatureDigest;

    public AzureActiveDirectoryBrokerAuthorizationRequest(final String responseType,
                                                          @NonNull final String clientId,
                                                          final String redirectUri,
                                                          final String state,
                                                          final Set<String> scope,
                                                          @NonNull final URL authority,
                                                          @NonNull final String authorizationEndpoint,
                                                          final String loginHint,
                                                          final UUID correlationId,
                                                          final PkceChallenge pkceChallenge,
                                                          final String extraQueryParam,
                                                          final String libraryVersion,
                                                          @NonNull final String resource,
                                                          final AzureActiveDirectoryPromptBehavior promptBehavior,
                                                          final String claimsChallenge,
                                                          final String callingPackage,
                                                          final String signatureDigest) {
        super(responseType, clientId, redirectUri, state, scope, authority, authorizationEndpoint,
                loginHint, correlationId, pkceChallenge, extraQueryParam, libraryVersion,
                resource, promptBehavior, claimsChallenge);
        mCallingPackage = callingPackage;
        mSignatureDigest = signatureDigest;
    }

    @Override
    public String getAuthorizationStartUrl() throws ClientException, UnsupportedEncodingException{
        final String startUrl = super.getAuthorizationStartUrl();
        if (!StringExtensions.isNullOrBlank(mCallingPackage)
                && !StringExtensions.isNullOrBlank(mSignatureDigest)) {
            return startUrl + "&package_name="
                    + URLEncoder.encode(mCallingPackage, ENCODING_UTF8)
                    + "&signature="
                    + URLEncoder.encode(mSignatureDigest, ENCODING_UTF8);
        }

        return startUrl;
    }
}
