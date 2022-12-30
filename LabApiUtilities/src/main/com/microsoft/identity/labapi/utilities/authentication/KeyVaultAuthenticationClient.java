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

import com.microsoft.identity.labapi.utilities.authentication.client.IConfidentialAuthClient;
import com.microsoft.identity.labapi.utilities.authentication.common.CertificateCredential;
import com.microsoft.identity.labapi.utilities.authentication.common.ClientCertificateMetadata;
import com.microsoft.identity.labapi.utilities.authentication.common.KeyStoreConfiguration;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;
import com.microsoft.identity.labapi.utilities.exception.LabError;

import lombok.NonNull;

/**
 * A an authentication client that can acquire access tokens for the KeyVault that hosts resources
 * used by the MSIDLAB API.
 */
public class KeyVaultAuthenticationClient implements IAccessTokenSupplier {

    private final static String CLIENT_ID = "4bc6e96f-bd23-408f-8ecb-a7a7145463f9";
    private final static String SCOPE = "https://vault.azure.net/.default";
    private final static String CERTIFICATE_ALIAS = "AutomationRunner";
    private final static String KEYSTORE_TYPE = "Windows-MY";
    private final static String KEYSTORE_PROVIDER = "SunMSCAPI";

    // tenant id where lab api and key vault api is registered
    private final static String TENANT_ID = "72f988bf-86f1-41af-91ab-2d7cd011db47";

    private final static String AUTHORITY = "https://login.microsoftonline.com/" + TENANT_ID;

    private final IConfidentialAuthClient mConfidentialAuthClient;
    private final String mClientSecret;

    public KeyVaultAuthenticationClient(@NonNull final IConfidentialAuthClient confidentialAuthClient) {
        mConfidentialAuthClient = confidentialAuthClient;
        mClientSecret = null;
    }

    public KeyVaultAuthenticationClient(@NonNull final IConfidentialAuthClient confidentialAuthClient,
                                        @NonNull final String clientSecret) {
        mConfidentialAuthClient = confidentialAuthClient;
        mClientSecret = clientSecret;
    }

    private ITokenParameters getTokenParametersForKeyVault() {
        return TokenParameters
                .builder()
                .clientId(CLIENT_ID)
                .authority(AUTHORITY)
                .scope(SCOPE)
                .build();
    }

    @Override
    public String getAccessToken() throws LabApiException {
        final IAuthenticationResult authenticationResult;

        if (mClientSecret != null && mClientSecret.trim().length() > 0) {
            // if client secret is provided then we would use that to acquire token
            authenticationResult = mConfidentialAuthClient.acquireToken(
                    mClientSecret, getTokenParametersForKeyVault()
            );

            if (authenticationResult != null) {
                return authenticationResult.getAccessToken();
            } else {
                throw new LabApiException(LabError.FAILED_TO_GET_TOKEN_FOR_KEYVAULT_USING_CLIENT_SECRET);
            }
        } else {
            // client secret was not provided...so we would try to use the Certificate
            // the cert must be in store for it to succeed..otherwise it would just fail
            final KeyStoreConfiguration keyStoreConfiguration = new KeyStoreConfiguration(
                    KEYSTORE_TYPE, KEYSTORE_PROVIDER, null
            );

            final ClientCertificateMetadata certificateMetadata = new ClientCertificateMetadata(
                    CERTIFICATE_ALIAS, null
            );

            final CertificateCredential certificateCredential = CertificateCredential.create(
                    keyStoreConfiguration,
                    certificateMetadata
            );

            authenticationResult = mConfidentialAuthClient.acquireToken(
                    certificateCredential, getTokenParametersForKeyVault()
            );

            if (authenticationResult != null) {
                return authenticationResult.getAccessToken();
            } else {
                throw new LabApiException(LabError.FAILED_TO_GET_TOKEN_FOR_KEYVAULT_USING_CERTIFICATE);
            }
        }
    }
}
