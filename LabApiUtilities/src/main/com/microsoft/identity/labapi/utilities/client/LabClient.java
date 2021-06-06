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
import com.microsoft.identity.labapi.utilities.constants.UserType;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;
import com.microsoft.identity.labapi.utilities.exception.LabError;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor(access = AccessLevel.PUBLIC)
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

        return new LabAccount(username, password, UserType.fromName(
                configInfo.getUserInfo().getUserType())
        );
    }

    private List<ConfigInfo> fetchConfigsFromLab(@NonNull final LabQuery query) throws LabApiException {
        Configuration.getDefaultApiClient().setAccessToken(
                mLabApiAuthenticationClient.getAccessToken()
        );
        try {
            final ConfigApi api = new ConfigApi();
            return api.apiConfigGet(
                    query.getUserType() != null ? query.getUserType().getValue() : null,
                    query.getUserRole() != null ? query.getUserRole().getValue() : null,
                    query.getMfa() != null ? query.getMfa().getValue() : null,
                    query.getProtectionPolicy() != null ? query.getProtectionPolicy().getValue() : null,
                    query.getHomeDomain() != null ? query.getHomeDomain().getValue() : null,
                    query.getHomeUpn() != null ? query.getHomeUpn().getValue() : null,
                    query.getB2cProvider() != null ? query.getB2cProvider().getValue() : null,
                    query.getFederationProvider() != null ? query.getFederationProvider().getValue() : null,
                    query.getAzureEnvironment() != null ? query.getAzureEnvironment().getValue() : null,
                    query.getGuestHomeAzureEnvironment() != null ? query.getGuestHomeAzureEnvironment().getValue() : null,
                    query.getAppType() != null ? query.getAppType().getValue() : null,
                    query.getPublicClient() != null ? query.getPublicClient().getValue() : null,
                    query.getSignInAudience() != null ? query.getSignInAudience().getValue() : null,
                    query.getGuestHomedIn() != null ? query.getGuestHomedIn().getValue() : null,
                    query.getHasAltId() != null ? query.getHasAltId().getValue() : null,
                    query.getAltIdSource() != null ? query.getAltIdSource().getValue() : null,
                    query.getAltIdType() != null ? query.getAltIdType().getValue() : null,
                    query.getPasswordPolicyValidityPeriod() != null ? query.getPasswordPolicyValidityPeriod().getValue() : null,
                    query.getPasswordPolicyNotificationDays() != null ? query.getPasswordPolicyNotificationDays().getValue() : null,
                    query.getTokenLifetimePolicy() != null ? query.getTokenLifetimePolicy().getValue() : null,
                    query.getTokenType() != null ? query.getTokenType().getValue() : null,
                    query.getTokenLifetime() != null ? query.getTokenLifetime().getValue() : null
            );
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

        // all temp users created by Lab Api are currently cloud users
        return new LabAccount(tempUser.getUpn(), password, UserType.CLOUD);
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
