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
import com.microsoft.identity.labapi.utilities.authentication.msal4j.Msal4jAuthClient;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;
import com.microsoft.identity.labapi.utilities.exception.LabError;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import lombok.NonNull;

/**
 * A an authentication client that can acquire access tokens for the Microsoft Identity Lab Api.
 */
public class LabApiAuthenticationClient implements IAccessTokenSupplier {
    private final static String SCOPE = "https://msidlab.com/.default";
    private final static String TENANT_ID = "72f988bf-86f1-41af-91ab-2d7cd011db47";
    private final static String AUTHORITY = "https://login.microsoftonline.com/" + TENANT_ID;
    private final static String CLIENT_ID = "f62c5ae3-bf3a-4af5-afa8-a68b800396e9";
    private final static String KEYSTORE_TYPE = "Windows-MY";
    private final static String KEYSTORE_PROVIDER = "SunMSCAPI";
    private final static String CERTIFICATE_ALIAS = "LabVaultAccessCert";
    private final String mLabCredential;

    public LabApiAuthenticationClient(@NonNull final String labSecret) {
        mLabCredential = labSecret;
    }

    @Override
    public String getAccessToken() throws LabApiException {
        final IConfidentialAuthClient confidentialAuthClient = new Msal4jAuthClient();
        final TokenParameters tokenParameters = TokenParameters.builder()
                .clientId(CLIENT_ID)
                .authority(AUTHORITY)
                .scope(SCOPE)
                .build();

        final IAuthenticationResult authenticationResult;
        if (mLabCredential != null && mLabCredential.trim().length() > 0) {
            if(mLabCredential.endsWith(".pfx")) {
                try (final InputStream inputStream = new FileInputStream(mLabCredential)) {
                    authenticationResult = confidentialAuthClient.acquireToken(inputStream, "", tokenParameters);
                } catch (final IOException e) {
                    throw new LabApiException(LabError.FAILED_TO_LOAD_CERTIFICATE, e);
                }
            } else {
                authenticationResult = confidentialAuthClient.acquireToken(mLabCredential, tokenParameters);
            }
        } else {
            // Create ClientCertificateCredential from the certificate store on the device
            // Expects the LabVaultAccessCert to be already installed on the device
            final KeyStoreConfiguration keyStoreConfiguration = new KeyStoreConfiguration(KEYSTORE_TYPE, KEYSTORE_PROVIDER, null);
            final ClientCertificateMetadata certificateMetadata = new ClientCertificateMetadata(CERTIFICATE_ALIAS, null);

            final CertificateCredential certificateCredential = CertificateCredential.create(keyStoreConfiguration, certificateMetadata);
            authenticationResult = confidentialAuthClient.acquireToken(certificateCredential, tokenParameters);
        }

        return authenticationResult.getAccessToken();
    }
}
