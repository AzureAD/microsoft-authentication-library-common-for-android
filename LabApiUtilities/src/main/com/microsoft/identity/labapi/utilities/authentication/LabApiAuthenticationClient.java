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
import com.microsoft.identity.labapi.utilities.constants.LabConstants;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;
import com.microsoft.identity.labapi.utilities.exception.LabError;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import lombok.NonNull;

/**
 * A an authentication client that can acquire access tokens for the Microsoft Identity Lab Api.
 */
public class LabApiAuthenticationClient implements IAccessTokenSupplier {
    private final static String TENANT_ID = "72f988bf-86f1-41af-91ab-2d7cd011db47";
    private final static String AUTHORITY = "https://login.microsoftonline.com/" + TENANT_ID;
    private final static String KEYSTORE_TYPE = "Windows-MY";
    private final static String KEYSTORE_PROVIDER = "SunMSCAPI";
    private final int DEFAULT_ACCESS_TOKEN_RETRIES = 2;
    private final int ATTEMPT_RETRY_WAIT = 3;
    private final String mLabCredential;
    private final String mLabCertPassword;
    private final String mScope;
    private final String mClientId;


    public LabApiAuthenticationClient(@NonNull final String labSecret) {
        this(labSecret, null, null, null);
    }

    public LabApiAuthenticationClient(@NonNull final String labSecret, final String labCertPassword) {
        this(labSecret, labCertPassword, null, null);
    }

    public LabApiAuthenticationClient(@NonNull final String labSecret, @NonNull final String scope, @NonNull final String clientId) {
        this(labSecret, null, scope, clientId);
    }

    public LabApiAuthenticationClient(@NonNull final String labSecret, final String labCertPassword, final String scope, final String clientId) {
        mLabCredential = labSecret;
        mLabCertPassword = labCertPassword;
        mScope = scope != null ? scope : LabConstants.DEFAULT_LAB_SCOPE;
        mClientId = clientId != null ? clientId : LabConstants.DEFAULT_LAB_CLIENT_ID;
    }

    @Override
    public String getAccessToken() throws LabApiException {
        return getAccessToken(DEFAULT_ACCESS_TOKEN_RETRIES);
    }

    public String getAccessToken(final int retries) throws LabApiException {

        // Do this in a loop, if we get an exception or null result, try again
        for (int i = 0; i < retries; i++) {
            System.out.printf(Locale.ENGLISH, "getAccessToken attempt #%d%n", (i + 1));

            try {
                final String result = getAccessTokenInternal();
                if (result != null) {
                    return result;
                }
            } catch (final LabApiException labApiException) {
                if (i < (retries - 1)) {
                    System.out.printf(
                            Locale.ENGLISH,
                            "getAccessToken attempt #%d%n failed: %s", (i + 1),
                            labApiException
                    );

                    try {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(ATTEMPT_RETRY_WAIT));
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    // last attempt, just throw the exception back
                    throw labApiException;
                }
            }
        }

        // Retries exhausted, no error, but still a null result
        return null;
    }

    private String getAccessTokenInternal() throws LabApiException {
        final IConfidentialAuthClient confidentialAuthClient = new Msal4jAuthClient();
        final TokenParameters tokenParameters = TokenParameters.builder()
                .clientId(mClientId)
                .authority(AUTHORITY)
                .scope(mScope)
                .build();

        final IAuthenticationResult authenticationResult;
        if (mLabCredential != null && mLabCredential.trim().length() > 0) {
            if(mLabCredential.endsWith(".pfx")) {
                try (final InputStream inputStream = new FileInputStream(mLabCredential)) {
                    final String certPass = mLabCertPassword == null ? "" : mLabCertPassword;
                    authenticationResult = confidentialAuthClient.acquireToken(inputStream, certPass, tokenParameters);
                } catch (final IOException e) {
                    throw new LabApiException(LabError.FAILED_TO_LOAD_CERTIFICATE);
                }
            } else {
                authenticationResult = confidentialAuthClient.acquireToken(mLabCredential, tokenParameters);
            }
        } else {
            // Create ClientCertificateCredential from the certificate store on the device
            // Expects the LabAuth to be already installed on the device
            final KeyStoreConfiguration keyStoreConfiguration = new KeyStoreConfiguration(KEYSTORE_TYPE, KEYSTORE_PROVIDER, null);
            final ClientCertificateMetadata certificateMetadata = new ClientCertificateMetadata(LabConstants.DEFAULT_LAB_CERT_ALIAS, null);

            final CertificateCredential certificateCredential = CertificateCredential.create(keyStoreConfiguration, certificateMetadata);
            authenticationResult = confidentialAuthClient.acquireToken(certificateCredential, tokenParameters);
        }

        return authenticationResult.getAccessToken();
    }
}
