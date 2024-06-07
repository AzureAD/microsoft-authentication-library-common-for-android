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

import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.internal.test.labapi.ApiException;
import com.microsoft.identity.internal.test.labapi.api.AppApi;
import com.microsoft.identity.internal.test.labapi.api.ConfigApi;
import com.microsoft.identity.internal.test.labapi.api.CreateTempUserApi;
import com.microsoft.identity.internal.test.labapi.api.ResetApi;
import com.microsoft.identity.internal.test.labapi.model.AppInfo;
import com.microsoft.identity.internal.test.labapi.model.ConfigInfo;
import com.microsoft.identity.internal.test.labapi.model.LabInfo;
import com.microsoft.identity.internal.test.labapi.model.TempUser;
import com.microsoft.identity.internal.test.labapi.model.UserInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LabUserHelper {

    private static final Map<LabUserQuery, LabConfig> sLabConfigCache = new HashMap<>();
    private volatile static ConfidentialClientHelper instance = LabAuthenticationHelper.getInstance();

    private static final int TEMP_USER_API_READ_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(15);

    /**
     * Reset the secret in use by the lab authentication helper.  This will rewrite the instance
     * in use to use a specified version of the key vault secret.
     * @param secret the key vault secret to use for access to the lab API.
     */
    public static void resetWithSecret(final String secret) {
        instance = LabAuthenticationHelper.getInstance(secret);
        instance.setupApiClientWithAccessToken();
    }

    static List<ConfigInfo> getConfigInfos(LabUserQuery query) {
        instance.setupApiClientWithAccessToken();
        ConfigApi api = new ConfigApi();
        List<ConfigInfo> configInfos;

        try {
            configInfos = api.apiConfigGet(
                    query.userType,
                    query.userRole,
                    query.mfa,
                    query.protectionPolicy,
                    query.homeDomain,
                    query.homeUpn,
                    query.b2cProvider,
                    query.federationProvider,
                    query.azureEnvironment,
                    query.guestHomeAzureEnvironment,
                    query.appType,
                    query.publicClient,
                    query.signInAudience,
                    query.guestHomedIn,
                    query.hasAltId,
                    query.altIdSource,
                    query.altIdType,
                    query.passwordPolicyValidityPeriod,
                    query.passwordPolicyNotificationDays,
                    query.tokenLifetimePolicy,
                    query.tokenType,
                    query.tokenLifetime);

        } catch (com.microsoft.identity.internal.test.labapi.ApiException ex) {
            throw new RuntimeException("Error retrieving lab user", ex);
        }

        return configInfos;
    }

    public enum UserType {
        CLOUD(LabConstants.UserType.CLOUD),
        B2C(LabConstants.UserType.B2C),
        FEDERATED(LabConstants.UserType.FEDERATED),
        GUEST(LabConstants.UserType.GUEST),
        MSA(LabConstants.UserType.MSA),
        ON_PREM(LabConstants.UserType.ON_PREM);
        String constant;

        UserType(String constant) {
            this.constant = constant;
        }

        String getValue() {
            return constant;
        }

    }

    public enum SignInAudience {
        AZURE_AD_AND_PERSONAL_MICROSOFT_ACCOUNT(LabConstants.SignInAudience.AZURE_AD_AND_PERSONAL_MICROSOFT_ACCOUNT),
        AZURE_AD_MULTIPLE_ORGS(LabConstants.SignInAudience.AZURE_AD_MULTIPLE_ORGS),
        AZURE_AD_MY_ORG(LabConstants.SignInAudience.AZURE_AD_MY_ORG),
        ;
        String constant;

        SignInAudience(String constant) {
            this.constant = constant;
        }

        String getValue() {
            return constant;
        }

    }

    public enum AzureEnvironment {
        AZURE_B2C_CLOUD(LabConstants.AzureEnvironment.AZURE_B2C_CLOUD),
        AZURE_CHINA_CLOUD(LabConstants.AzureEnvironment.AZURE_CHINA_CLOUD),
        AZURE_CLOUD(LabConstants.AzureEnvironment.AZURE_CLOUD),
        AZURE_GERMANY_CLOUD(LabConstants.AzureEnvironment.AZURE_GERMANY_CLOUD),
        AZURE_PPE(LabConstants.AzureEnvironment.AZURE_PPE),
        AZURE_US_GOVERNMENT(LabConstants.AzureEnvironment.AZURE_US_GOVERNMENT);
        String constant;

        AzureEnvironment(String constant) {
            this.constant = constant;
        }

        String getValue() {
            return constant;
        }
    }

    public enum IsAdminConsented {
        YES(LabConstants.IsAdminConsented.YES),
        NO(LabConstants.IsAdminConsented.NO);
        String constant;

        IsAdminConsented(String constant) {
            this.constant = constant;
        }

        String getValue() {
            return constant;
        }
    }

    public enum PublicClient {
        YES(LabConstants.PublicClient.YES),
        NO(LabConstants.PublicClient.NO);
        String constant;

        PublicClient(String constant) {
            this.constant = constant;
        }

        String getValue() {
            return constant;
        }
    }

    public static ConfigInfo getConfigInfo(LabUserQuery query) {
        LabConfig labConfig;
        labConfig = sLabConfigCache.get(query);

        if (labConfig == null) {
            final ConfigInfo pickedConfig = getConfigInfos(query).get(0);
            labConfig = new LabConfig(pickedConfig);
            sLabConfigCache.put(query, labConfig);
        }

        LabConfig.setCurrentLabConfig(labConfig);

        return labConfig.getConfigInfo();

    }

    public static ConfigInfo getConfigInfoFromUpn(final String upn) {
        instance.setupApiClientWithAccessToken();
        ConfigApi api = new ConfigApi();
        List<ConfigInfo> configInfos;

        try {
            configInfos = api.apiConfigUpnGet(upn);
        } catch (com.microsoft.identity.internal.test.labapi.ApiException ex) {
            throw new RuntimeException("Error retrieving lab user", ex);
        }

        final ConfigInfo pickedConfig = configInfos.get(0);

        LabConfig labConfig = new LabConfig(pickedConfig);

        LabConfig.setCurrentLabConfig(labConfig);

        return pickedConfig;
    }

    static List<LabConfig> loadUsersForTest(LabUserQuery query) {
        instance.setupApiClientWithAccessToken();
        List<LabConfig> labConfigs = new ArrayList<>();
        final List<ConfigInfo> configInfos = getConfigInfos(query);
        for (ConfigInfo configInfo : configInfos) {
            final String password = LabHelper.getPasswordForLab(configInfo.getLabInfo().getCredentialVaultKeyName());
            labConfigs.add(new LabConfig(configInfo, password));
        }

        return labConfigs;
    }

    public static String loadUserForTest(LabUserQuery query) {
        instance.setupApiClientWithAccessToken();
        LabConfig labConfig;
        labConfig = sLabConfigCache.get(query);

        if (labConfig == null) {
            labConfig = loadUsersForTest(query).get(0);
            sLabConfigCache.put(query, labConfig);
        }

        LabConfig.setCurrentLabConfig(labConfig);
        UserInfo userInfo = labConfig.getConfigInfo().getUserInfo();
        String upn = userInfo.getHomeUPN();
        if (StringUtil.isEmpty(upn) || upn.equalsIgnoreCase("None")) {
            upn = userInfo.getUpn();
        }

        return upn;
    }

    public static String loadTempUser(final String userType) {
        instance.setupApiClientWithAccessToken();
        CreateTempUserApi createTempUserApi = new CreateTempUserApi();
        createTempUserApi.getApiClient().setReadTimeout(TEMP_USER_API_READ_TIMEOUT);

        TempUser tempUser;

        try {
            tempUser = createTempUserApi.apiCreateTempUserPost(userType);
            final String password = LabHelper.getPasswordForLab(tempUser.getCredentialVaultKeyName());
            LabConfig labConfig = new LabConfig(tempUser, password);
            LabConfig.setCurrentLabConfig(labConfig);
        } catch (ApiException e) {
            throw new RuntimeException("Error retrieving lab user", e);
        }

        return tempUser.getUpn();
    }

    public static TempUser loadTempUserForTest(final String userType) {
        instance.setupApiClientWithAccessToken();
        CreateTempUserApi createTempUserApi = new CreateTempUserApi();
        createTempUserApi.getApiClient().setReadTimeout(TEMP_USER_API_READ_TIMEOUT);

        try {
            return createTempUserApi.apiCreateTempUserPost(userType);
        } catch (ApiException e) {
            throw new RuntimeException("Error retrieving lab user", e);
        }
    }

    public static String getPasswordForUser(final String username) {
        final ConfigInfo configInfo = getConfigInfoFromUpn(username);
        return LabHelper.getPasswordForLab(configInfo.getLabInfo().getCredentialVaultKeyName());
    }

    public static String getPasswordForUser(final LabInfo labInfo) {
        return LabHelper.getPasswordForLab(labInfo.getCredentialVaultKeyName());
    }

    public static Credential getCredentials(LabUserQuery query) {
        LabConfig labConfig;
        labConfig = sLabConfigCache.get(query);
        Credential credential = new Credential();
        ConfigInfo configInfo;

        if (labConfig == null) {
            String password;
            configInfo = getConfigInfo(query);
            password = getPasswordForUser(configInfo.getLabInfo());
            labConfig = new LabConfig(configInfo, password);
            sLabConfigCache.put(query, labConfig);
        } else {
            configInfo = labConfig.getConfigInfo();
        }

        LabConfig.setCurrentLabConfig(labConfig);
        credential.userName = configInfo.getUserInfo().getUpn();
        credential.password = labConfig.getLabUserPassword();
        return credential;
    }

    public static AppInfo getDefaultAppInfo() {
        instance.setupApiClientWithAccessToken();
        return getAppInfo(UserType.CLOUD, AzureEnvironment.AZURE_CLOUD, SignInAudience.AZURE_AD_MULTIPLE_ORGS,
                IsAdminConsented.YES, PublicClient.YES);
    }

    public static AppInfo getAppInfo(UserType userType, AzureEnvironment azureEnvironment, SignInAudience audience,
                                     IsAdminConsented isAdminConsented, PublicClient publicClient) {
        instance.setupApiClientWithAccessToken();
        AppApi api = new AppApi();
        try {
            return api.apiAppGet(userType.getValue(), azureEnvironment.getValue(), audience.getValue(),
                    isAdminConsented.getValue(), publicClient.getValue()).get(0);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public static void resetPassword(final String upn) {
        instance.setupApiClientWithAccessToken();
        ResetApi resetApi = new ResetApi();
        try {
            resetApi.apiResetPut(upn, "Password");
        } catch (ApiException e) {
            throw new RuntimeException("Error resetting lab user password", e);
        }
    }

}
