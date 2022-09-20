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

import com.microsoft.identity.internal.test.keyvault.ApiException;
import com.microsoft.identity.internal.test.keyvault.Configuration;
import com.microsoft.identity.internal.test.keyvault.api.SecretsApi;
import com.microsoft.identity.labapi.utilities.authentication.client.ConfidentialAuthClientFactory;
import com.microsoft.identity.labapi.utilities.authentication.client.IConfidentialAuthClient;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;
import com.microsoft.identity.labapi.utilities.exception.LabError;

import lombok.NonNull;

/**
 * A an authentication client that can acquire access tokens for the Microsoft Identity Lab Api.
 */
public class LabApiAuthenticationClient implements IAccessTokenSupplier {

    private final static String SECRET_NAME_LAB_APP_ID = "LabVaultAppID";
    private final static String SECRET_NAME_LAB_APP_SECRET = "LabVaultAppSecret";
    private final static String SECRET_VERSION = "";
    private final static int SOCKET_ATTEMPT_COUNT = 3;
    private final static String KEY_VAULT_API_VERSION = "2016-10-01";
    private final static String SCOPE = "https://msidlab.com/.default";

    private final static String TENANT_ID = "72f988bf-86f1-41af-91ab-2d7cd011db47";

    private final static String AUTHORITY = "https://login.microsoftonline.com/" + TENANT_ID;

    private final IConfidentialAuthClient mConfidentialAuthClient;
    private final KeyVaultAuthenticationClient mKeyVaultAuthenticationClient;

    public LabApiAuthenticationClient(@NonNull final IConfidentialAuthClient confidentialAuthClient) {
        mConfidentialAuthClient = confidentialAuthClient;
        mKeyVaultAuthenticationClient = new KeyVaultAuthenticationClient(confidentialAuthClient);
    }

    public LabApiAuthenticationClient(@NonNull final IConfidentialAuthClient confidentialAuthClient,
                                      @NonNull final String clientSecret) {
        mConfidentialAuthClient = confidentialAuthClient;
        mKeyVaultAuthenticationClient = new KeyVaultAuthenticationClient(confidentialAuthClient, clientSecret);
    }

    public LabApiAuthenticationClient() {
        this(ConfidentialAuthClientFactory.INSTANCE.getConfidentialAuthClient());
    }

    public LabApiAuthenticationClient(@NonNull final String clientSecret) {
        this(ConfidentialAuthClientFactory.INSTANCE.getConfidentialAuthClient(), clientSecret);
    }

    @Override
    public String getAccessToken() throws LabApiException {
        for (int i = 0; i < SOCKET_ATTEMPT_COUNT; i++) {
            try {
                return getAccessTokenInternal();
            } catch (Exception generalException) {
                if (!generalException.getCause().toString().contains("java.net.SocketTimeoutException: timeout") || i == SOCKET_ATTEMPT_COUNT - 1) {
                    throw generalException;
                }
            }
        }

        // There were no exceptions, but getAccessToken still failed.
        return null;
    }

    private String getAccessTokenInternal() throws LabApiException {
        // first get token for KeyVault...because we find lab app id and secret from there
        final String accessTokenForKeyVault = mKeyVaultAuthenticationClient.getAccessToken();
        Configuration.getDefaultApiClient().setAccessToken(accessTokenForKeyVault);

        final String labAppId, labAppSecret;

        try {
            // we are going to use the KeyVault API to obtain the Lab App client id and
            // client secret
            final SecretsApi secretsApi = new SecretsApi();
            labAppId = secretsApi.getSecret(
                    SECRET_NAME_LAB_APP_ID, SECRET_VERSION, KEY_VAULT_API_VERSION
            ).getValue();

            labAppSecret = secretsApi.getSecret(
                    SECRET_NAME_LAB_APP_SECRET, SECRET_VERSION, KEY_VAULT_API_VERSION
            ).getValue();
        } catch (final ApiException e) {
            throw new LabApiException(LabError.FAILED_TO_GET_SECRET_FROM_KEYVAULT, e);
        }

        final TokenParameters tokenParameters = TokenParameters
                .builder()
                .clientId(labAppId)
                .authority(AUTHORITY)
                .scope(SCOPE)
                .build();

        // obtain token for Lab Api using the client secret retrieved from KeyVault
        final IAuthenticationResult authenticationResult = mConfidentialAuthClient.acquireToken(
                labAppSecret, tokenParameters
        );

        return authenticationResult.getAccessToken();
    }
}
