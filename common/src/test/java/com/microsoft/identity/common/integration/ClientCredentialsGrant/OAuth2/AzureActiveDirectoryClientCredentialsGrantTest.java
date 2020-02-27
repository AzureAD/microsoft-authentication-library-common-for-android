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
package com.microsoft.identity.common.integration.ClientCredentialsGrant.OAuth2;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.authscheme.BearerAuthenticationSchemeInternal;
import com.microsoft.identity.common.internal.providers.keys.CertificateCredential;
import com.microsoft.identity.common.internal.providers.keys.ClientCertificateMetadata;
import com.microsoft.identity.common.internal.providers.keys.KeyStoreConfiguration;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftClientAssertion;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryOAuth2Configuration;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryTokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2StrategyParameters;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(JUnit4.class)
public class AzureActiveDirectoryClientCredentialsGrantTest {

    private final static String CLIENT_ID = "4bc6e96f-bd23-408f-8ecb-a7a7145463f9";
    private final static String RESOURCE = "https://management.core.windows.net";
    private final static String GRANT_TYPE = "client_credentials";
    private final static String CERTIFICATE_ALIAS = "AutomationRunner";
    private final static String KEYSTORE_TYPE = "Windows-MY";
    private final static String KEYSTORE_PROVIDER = "SunMSCAPI";
    private final static String AAD_CLIENT_ASSERTION_AUDIENCE = "https://login.microsoftonline.com/microsoft.com/oauth2/token";

    @Test
    public void test_ClientCredentials() throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, IOException {

        final CertificateCredential credential = new CertificateCredential.CertificateCredentialBuilder(CLIENT_ID)
                .clientCertificateMetadata(new ClientCertificateMetadata(CERTIFICATE_ALIAS, null))
                .keyStoreConfiguration(new KeyStoreConfiguration(KEYSTORE_TYPE, KEYSTORE_PROVIDER, null))
                .build();

        final String audience = AAD_CLIENT_ASSERTION_AUDIENCE;

        final MicrosoftClientAssertion assertion = new MicrosoftClientAssertion(audience, credential);

        final AzureActiveDirectoryTokenRequest tr = new AzureActiveDirectoryTokenRequest();

        tr.setClientAssertionType(assertion.getClientAssertionType());
        tr.setClientAssertion(assertion.getClientAssertion());
        tr.setClientId(CLIENT_ID);
        tr.setResourceId(RESOURCE);
        tr.setGrantType(GRANT_TYPE);

        final OAuth2StrategyParameters options = new OAuth2StrategyParameters();
        final OAuth2Strategy strategy = new AzureActiveDirectoryOAuth2Strategy(
                new AzureActiveDirectoryOAuth2Configuration(),
                options
        );

        try {
            final TokenResult tokenResult = strategy.requestToken(tr);

            assertEquals(true, tokenResult.getSuccess());
        } catch (final ClientException exception) {
            fail("Unexpected exception.");
        }
    }


}
