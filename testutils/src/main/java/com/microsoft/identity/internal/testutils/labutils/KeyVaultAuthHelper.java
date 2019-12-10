package com.microsoft.identity.internal.testutils.labutils;

import com.microsoft.identity.common.internal.providers.keys.CertificateCredential;
import com.microsoft.identity.common.internal.providers.keys.ClientCertificateMetadata;
import com.microsoft.identity.common.internal.providers.keys.KeyStoreConfiguration;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftClientAssertion;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsTokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.internal.test.keyvault.Configuration;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

class KeyVaultAuthHelper extends ConfidentialClientHelper {

    private final static String CLIENT_ID = "4bc6e96f-bd23-408f-8ecb-a7a7145463f9";
    private final static String SCOPE = "https://vault.azure.net/.default";
    private final static String CERTIFICATE_ALIAS = "AutomationRunner";
    private final static String KEYSTORE_TYPE = "Windows-MY";
    private final static String KEYSTORE_PROVIDER = "SunMSCAPI";
    private final static String MSSTS_CLIENT_ASSERTION_AUDIENCE = "https://login.microsoftonline.com/microsoft.com/oauth2/v2.0/token";

    private static KeyVaultAuthHelper sKeyVaultAuthHelper;

    private KeyVaultAuthHelper() {
    }

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
    public TokenRequest createTokenRequest() throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, IOException {
        CertificateCredential certificateCredential = new CertificateCredential.CertificateCredentialBuilder(CLIENT_ID)
                .clientCertificateMetadata(new ClientCertificateMetadata(CERTIFICATE_ALIAS, null))
                .keyStoreConfiguration(new KeyStoreConfiguration(KEYSTORE_TYPE, KEYSTORE_PROVIDER, null))
                .build();

        MicrosoftClientAssertion assertion = new MicrosoftClientAssertion(MSSTS_CLIENT_ASSERTION_AUDIENCE, certificateCredential);

        TokenRequest tr = new MicrosoftStsTokenRequest();

        tr.setClientAssertionType(assertion.getClientAssertionType());
        tr.setClientAssertion(assertion.getClientAssertion());
        tr.setClientId(CLIENT_ID);
        tr.setScope(SCOPE);
        return tr;
    }
}
