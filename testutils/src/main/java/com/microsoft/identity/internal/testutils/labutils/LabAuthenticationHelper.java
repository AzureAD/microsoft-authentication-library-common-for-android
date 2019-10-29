package com.microsoft.identity.internal.testutils.labutils;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.authorities.AccountsInOneOrganization;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.providers.keys.CertificateCredential;
import com.microsoft.identity.common.internal.providers.keys.ClientCertificateMetadata;
import com.microsoft.identity.common.internal.providers.keys.KeyStoreConfiguration;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftClientAssertion;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsTokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.internal.test.keyvault.Configuration;
import com.microsoft.identity.internal.test.keyvault.api.SecretsApi;
import com.microsoft.identity.internal.test.keyvault.model.SecretBundle;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class LabAuthenticationHelper {
    private static String mAccessToken = null;
    private final static String CLIENT_ID = "4bc6e96f-bd23-408f-8ecb-a7a7145463f9";
    private final static String SCOPE = "https://user.msidlab.com/.default";
    private final static String GRANT_TYPE = "client_credentials";
    private final static String CERTIFICATE_ALIAS = "AutomationRunner";
    private final static String KEYSTORE_TYPE = "Windows-MY";
    private final static String KEYSTORE_PROVIDER = "SunMSCAPI";
    private final static String MSSTS_CLIENT_ASSERTION_AUDIENCE = "https://login.microsoftonline.com/microsoft.com/oauth2/v2.0/token";
    private final static String TENANT_ID = "72f988bf-86f1-41af-91ab-2d7cd011db47";


    public static String getAccessToken() throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, IOException, InterruptedException {
        if (mAccessToken != null) {
            return mAccessToken;
        } else {
            requestAccessTokenForAutomation();
            return mAccessToken;
        }
    }

    /**
     * Yep.  Hardcoding this method to retrieve access token for MSIDLABS
     */
    private static void requestAccessTokenForAutomation() throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, IOException, InterruptedException {

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
        tr.setGrantType(GRANT_TYPE);

        AccountsInOneOrganization aadAudience = new AccountsInOneOrganization(TENANT_ID);
        AzureActiveDirectoryAuthority authority = new AzureActiveDirectoryAuthority(aadAudience);
        OAuth2Strategy strategy = authority.createOAuth2Strategy();

        try {
            TokenResult tokenResult = strategy.requestToken(tr);
            if (tokenResult.getSuccess()) {
                mAccessToken = tokenResult.getTokenResponse().getAccessToken();
            } else {
                throw new RuntimeException(tokenResult.getErrorResponse().getErrorDescription());
            }
        } catch (ClientException e) {
            e.printStackTrace();
        }

    }


    public static Credential GetCredential(String upn, String secretName) throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, IOException, InterruptedException {

        Configuration.getDefaultApiClient().setBasePath("https://msidlabs.vault.azure.net");
        Configuration.getDefaultApiClient().setAccessToken(Secrets.getAccessToken());

        SecretsApi secretsApi = new SecretsApi();
        Credential credential = new Credential();
        credential.userName = upn;

        try {
            SecretBundle secretBundle = secretsApi.getSecret(secretName, "", Secrets.API_VERSION);
            credential.password = secretBundle.getValue();
        } catch (com.microsoft.identity.internal.test.keyvault.ApiException ex) {
            throw new RuntimeException("exception accessing secret", ex);
        }

        return credential;
    }

    public static void setupApiClientWithAccessToken(final String accessToken) {
        Configuration.getDefaultApiClient().setAccessToken(accessToken);
    }

    public static void setupApiClientWithAccessToken() {
        try {
            setupApiClientWithAccessToken(LabAuthenticationHelper.getAccessToken());
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
