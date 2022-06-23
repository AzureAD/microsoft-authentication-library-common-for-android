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
package com.microsoft.identity.internal.testutils.labutils;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenRequest;
import com.microsoft.identity.common.java.providers.oauth2.TokenRequest;
import com.microsoft.identity.internal.test.keyvault.Configuration;
import com.microsoft.identity.labapi.utilities.authentication.common.CertificateCredential;
import com.microsoft.identity.labapi.utilities.authentication.common.ClientCertificateMetadata;
import com.microsoft.identity.labapi.utilities.authentication.common.KeyStoreConfiguration;
import com.microsoft.identity.labapi.utilities.authentication.common.MicrosoftClientAssertion;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;

class KeyVaultAuthHelper extends ConfidentialClientHelper {

    private final static String CLIENT_ID = "4bc6e96f-bd23-408f-8ecb-a7a7145463f9";
    private final static String SCOPE = "https://vault.azure.net/.default";
    private final static String CERTIFICATE_ALIAS = "AutomationRunner";
    private final static String KEYSTORE_TYPE = "Windows-MY";
    private final static String KEYSTORE_PROVIDER = "SunMSCAPI";
    private final static String MSSTS_CLIENT_ASSERTION_AUDIENCE = "https://login.microsoftonline.com/microsoft.com/oauth2/v2.0/token";

    private static KeyVaultAuthHelper sKeyVaultAuthHelper;
    private final String mSecret;

    private KeyVaultAuthHelper(final String secret) {
        mSecret = secret;
    }

    private KeyVaultAuthHelper() {
        mSecret = com.microsoft.identity.internal.testutils.BuildConfig.LAB_CLIENT_SECRET;
    }

    public static ConfidentialClientHelper getInstanceWithSecret(String secret) {
        if (sKeyVaultAuthHelper == null) {
            sKeyVaultAuthHelper = new KeyVaultAuthHelper(secret);
        }
        return sKeyVaultAuthHelper;
    }

    @Deprecated
    public static ConfidentialClientHelper getInstance() {
        if (sKeyVaultAuthHelper == null) {
            sKeyVaultAuthHelper = new KeyVaultAuthHelper();
        }
        return sKeyVaultAuthHelper;
    }

    @Override
    public void setupApiClientWithAccessToken(final String accessToken) {
        Configuration.getDefaultApiClient().setAccessToken(accessToken);
    }

    @Override
    public TokenRequest createTokenRequest() throws LabApiException {
        if (TextUtils.isEmpty(mSecret)) {
            return createTokenRequestWithClientAssertion();
        } else {
            return createTokenRequestWithClientSecret(mSecret);
        }
    }

    private TokenRequest createTokenRequestWithClientSecret() {
        TokenRequest tr = new MicrosoftStsTokenRequest();

        tr.setClientSecret(mSecret);
        tr.setClientId(CLIENT_ID);
        tr.setScope(SCOPE);
        return tr;
    }

    private TokenRequest createTokenRequestWithClientSecret(@NonNull final String secret) {
        TokenRequest tr = new MicrosoftStsTokenRequest();

        tr.setClientSecret(secret);
        tr.setClientId(CLIENT_ID);
        tr.setScope(SCOPE);
        return tr;
    }

    private TokenRequest createTokenRequestWithClientAssertion() throws LabApiException {
        CertificateCredential certificateCredential = CertificateCredential.create(
                new KeyStoreConfiguration(KEYSTORE_TYPE, KEYSTORE_PROVIDER, null),
                new ClientCertificateMetadata(CERTIFICATE_ALIAS, null));

        MicrosoftClientAssertion assertion = MicrosoftClientAssertion.builder()
                .clientId(CLIENT_ID)
                .audience(MSSTS_CLIENT_ASSERTION_AUDIENCE)
                .certificateCredential(certificateCredential)
                .build();

        TokenRequest tr = new MicrosoftStsTokenRequest();

        tr.setClientAssertionType(assertion.getClientAssertionType());
        tr.setClientAssertion(assertion.getClientAssertion());
        tr.setClientId(CLIENT_ID);
        tr.setScope(SCOPE);
        return tr;
    }
}
