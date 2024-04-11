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
package com.microsoft.identity.labapi.utilities.authentication;

import com.microsoft.identity.labapi.utilities.authentication.client.ConfidentialAuthClientFactory;
import com.microsoft.identity.labapi.utilities.authentication.client.IConfidentialAuthClient;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;

import lombok.NonNull;

/**
 * A an authentication client that can acquire access tokens for the Microsoft Identity Lab Api.
 */
public class LabApiAuthenticationClient implements IAccessTokenSupplier {

    private final static String SCOPE = "https://msidlab.com/.default";

    private final static String TENANT_ID = "72f988bf-86f1-41af-91ab-2d7cd011db47";

    private final static String AUTHORITY = "https://login.microsoftonline.com/" + TENANT_ID;

    private final IConfidentialAuthClient mConfidentialAuthClient;
    private final static String CLIENT_ID = "f62c5ae3-bf3a-4af5-afa8-a68b800396e9";
    private final String mClientSecret;

    public LabApiAuthenticationClient(@NonNull final IConfidentialAuthClient confidentialAuthClient) {
        mConfidentialAuthClient = confidentialAuthClient;
        mClientSecret = "";
    }

    public LabApiAuthenticationClient(@NonNull final IConfidentialAuthClient confidentialAuthClient,
                                      @NonNull final String clientSecret) {
        mConfidentialAuthClient = confidentialAuthClient;
        mClientSecret = clientSecret;
    }

    public LabApiAuthenticationClient() {
        this(ConfidentialAuthClientFactory.INSTANCE.getConfidentialAuthClient());
    }

    public LabApiAuthenticationClient(@NonNull final String clientSecret) {
        this(ConfidentialAuthClientFactory.INSTANCE.getConfidentialAuthClient(), clientSecret);
    }

    @Override
    public String getAccessToken() throws LabApiException {
        if (mClientSecret != null && mClientSecret.trim().length() > 0) {
            final TokenParameters tokenParameters = TokenParameters
                    .builder()
                    .clientId(CLIENT_ID)
                    .authority(AUTHORITY)
                    .scope(SCOPE)
                    .build();

            // obtain token for Lab Api using the client secret retrieved from KeyVault
            final IAuthenticationResult authenticationResult = mConfidentialAuthClient.acquireToken(
                    mClientSecret, tokenParameters
            );

            return authenticationResult.getAccessToken();
        } else {
            final KeyVaultAuthenticationClient keyVaultAuthenticationClient = new KeyVaultAuthenticationClient(mConfidentialAuthClient);
            return keyVaultAuthenticationClient.getAccessToken();
        }
    }
}
