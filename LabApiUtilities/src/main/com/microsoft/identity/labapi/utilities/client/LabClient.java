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
package com.microsoft.identity.labapi.utilities.client;

import com.microsoft.identity.internal.test.labapi.ApiException;
import com.microsoft.identity.internal.test.labapi.Configuration;
import com.microsoft.identity.internal.test.labapi.api.ConfigApi;
import com.microsoft.identity.internal.test.labapi.api.CreateTempUserApi;
import com.microsoft.identity.internal.test.labapi.api.LabSecretApi;
import com.microsoft.identity.internal.test.labapi.model.ConfigInfo;
import com.microsoft.identity.internal.test.labapi.model.SecretResponse;
import com.microsoft.identity.internal.test.labapi.model.TempUser;
import com.microsoft.identity.labapi.utilities.authentication.LabApiAuthenticationClient;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;
import com.microsoft.identity.labapi.utilities.exception.LabError;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LabClient implements ILabClient {

    private final LabApiAuthenticationClient mLabApiAuthenticationClient;

    private static final int TEMP_USER_API_READ_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(15);

    @Override
    public LabAccount fetchUser(@NonNull final LabQuery labQuery) throws LabApiException {
        final List<ConfigInfo> configInfos = fetchConfigsFromLab(labQuery);
        final ConfigInfo configInfo = configInfos.get(0);
        return createAccount(configInfo);
    }

    @Override
    public List<LabAccount> fetchUsers(@NonNull final LabQuery labQuery) throws LabApiException {
        final List<ConfigInfo> configInfos = fetchConfigsFromLab(labQuery);

        final List<LabAccount> labAccounts = new ArrayList<>(configInfos.size());

        for (final ConfigInfo configInfo : configInfos) {
            labAccounts.add(createAccount(configInfo));
        }

        return labAccounts;
    }

    private LabAccount createAccount(@NonNull final ConfigInfo configInfo) throws LabApiException {
        String username = configInfo.getUserInfo().getHomeUPN();
        if (username == null || username.equals("") || username.equalsIgnoreCase("None")) {
            username = configInfo.getUserInfo().getUpn();
        }

        final String password = getPassword(configInfo);

        return new LabAccount(username, password);
    }

    private List<ConfigInfo> fetchConfigsFromLab(@NonNull final LabQuery labQuery) throws LabApiException {
        Configuration.getDefaultApiClient().setAccessToken(
                mLabApiAuthenticationClient.getAccessToken()
        );
        try {
            ConfigApi api = new ConfigApi();
            final List<ConfigInfo> configInfos = api.apiConfigGet(
                    labQuery.getUserType().getValue(),
                    labQuery.getUserRole().getValue(),
                    labQuery.getMfa().getValue(),
                    labQuery.getProtectionPolicy().getValue(),
                    labQuery.getHomeDomain().getValue(),
                    labQuery.getHomeUpn().getValue(),
                    labQuery.getB2cProvider().getValue(),
                    labQuery.getFederationProvider().getValue(),
                    labQuery.getAzureEnvironment().getValue(),
                    labQuery.getGuestHomeAzureEnvironment().getValue(),
                    labQuery.getAppType().getValue(),
                    labQuery.getPublicClient().getValue(),
                    labQuery.getSignInAudience().getValue(),
                    labQuery.getGuestHomedIn().getValue(),
                    labQuery.getHasAltId().getValue(),
                    labQuery.getAltIdSource().getValue(),
                    labQuery.getAltIdType().getValue(),
                    labQuery.getPasswordPolicyValidityPeriod().getValue(),
                    labQuery.getPasswordPolicyNotificationDays().getValue(),
                    labQuery.getTokenLifetimePolicy().getValue(),
                    labQuery.getTokenType().getValue(),
                    labQuery.getTokenLifetime().getValue());
            return configInfos;
        } catch (final com.microsoft.identity.internal.test.labapi.ApiException ex) {
            throw new LabApiException(LabError.FAILED_TO_GET_ACCOUNT_FROM_LAB, ex);
        }
    }


    @Override
    public LabAccount createTempUser(@NonNull final TempUserType tempUserType) throws LabApiException {
        Configuration.getDefaultApiClient().setAccessToken(
                mLabApiAuthenticationClient.getAccessToken()
        );
        final CreateTempUserApi createTempUserApi = new CreateTempUserApi();
        createTempUserApi.getApiClient().setReadTimeout(TEMP_USER_API_READ_TIMEOUT);
        final TempUser tempUser;

        try {
            tempUser = createTempUserApi.apiCreateTempUserPost(tempUserType.getValue());
        } catch (final ApiException e) {
            throw new LabApiException(LabError.FAILED_TO_CREATE_TEMP_USER, e);
        }

        final String password = getPassword(tempUser);

        return new LabAccount(tempUser.getUpn(), password);
    }

    @Override
    public String getSecret(@NonNull final String secretName) throws LabApiException {
        Configuration.getDefaultApiClient().setAccessToken(
                mLabApiAuthenticationClient.getAccessToken()
        );
        final LabSecretApi labSecretApi = new LabSecretApi();

        try {
            final SecretResponse secretResponse = labSecretApi.apiLabSecretGet(secretName);
            return secretResponse.getValue();
        } catch (final com.microsoft.identity.internal.test.labapi.ApiException ex) {
            throw new LabApiException(LabError.FAILED_TO_GET_SECRET_FROM_LAB, ex);
        }
    }

    private String getPassword(@NonNull final ConfigInfo configInfo) throws LabApiException {
        return getPassword(configInfo.getLabInfo().getCredentialVaultKeyName());
    }

    private String getPassword(@NonNull final TempUser tempUser) throws LabApiException {
        return getPassword(tempUser.getCredentialVaultKeyName());
    }

    private String getPassword(final String credentialVaultKeyName) throws LabApiException {
        final String secretName = getLabSecretName(credentialVaultKeyName);
        return getSecret(secretName);
    }

    private String getLabSecretName(final String credentialVaultKeyName) {
        final String[] parts = credentialVaultKeyName.split("/");
        return parts[parts.length - 1];
    }
}
