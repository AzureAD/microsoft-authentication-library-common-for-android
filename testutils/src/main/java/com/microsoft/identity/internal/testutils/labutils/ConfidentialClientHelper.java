package com.microsoft.identity.internal.testutils.labutils;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.authorities.AccountsInOneOrganization;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

abstract class ConfidentialClientHelper {

    private final static String GRANT_TYPE = "client_credentials";
    private final static String TENANT_ID = "72f988bf-86f1-41af-91ab-2d7cd011db47";

    abstract String getAccessToken() throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, IOException, InterruptedException;

    abstract TokenRequest createTokenRequest() throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, IOException;

    abstract void setupApiClientWithAccessToken(String accessToken);

    /**
     * Yep.  Hardcoding this method to retrieve access token for MSIDLABS
     */
    String requestAccessTokenForAutomation() throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, IOException, InterruptedException {
        String accessToken = null;

        TokenRequest tokenRequest = this.createTokenRequest();

        tokenRequest.setGrantType(GRANT_TYPE);
        AccountsInOneOrganization aadAudience = new AccountsInOneOrganization(TENANT_ID);
        AzureActiveDirectoryAuthority authority = new AzureActiveDirectoryAuthority(aadAudience);
        OAuth2Strategy strategy = authority.createOAuth2Strategy();

        try {
            TokenResult tokenResult = strategy.requestToken(tokenRequest);
            if (tokenResult.getSuccess()) {
                accessToken = tokenResult.getTokenResponse().getAccessToken();
            } else {
                throw new RuntimeException(tokenResult.getErrorResponse().getErrorDescription());
            }
        } catch (ClientException e) {
            e.printStackTrace();
        }

        return accessToken;
    }

    void setupApiClientWithAccessToken() {
        try {
            setupApiClientWithAccessToken(this.getAccessToken());
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
