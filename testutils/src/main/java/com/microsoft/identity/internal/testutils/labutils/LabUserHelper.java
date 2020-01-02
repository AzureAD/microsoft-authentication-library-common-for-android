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

import com.microsoft.identity.internal.test.labapi.ApiException;
import com.microsoft.identity.internal.test.labapi.api.ConfigApi;
import com.microsoft.identity.internal.test.labapi.api.ResetApi;
import com.microsoft.identity.internal.test.labapi.model.ConfigInfo;
import com.microsoft.identity.internal.test.labapi.model.LabInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LabUserHelper {

    private static final Map<LabUserQuery, LabConfig> sLabConfigCache = new HashMap<>();

    static List<ConfigInfo> getConfigInfos(LabUserQuery query) {

        LabAuthenticationHelper.getInstance().setupApiClientWithAccessToken();
        ConfigApi api = new ConfigApi();
        List<ConfigInfo> configInfos;

        try {
            configInfos = api.getConfig(
                    query.userType,
                    query.mfa,
                    query.protectionPolicy,
                    query.homeDomain,
                    query.homeUpn,
                    query.b2cProvider,
                    query.federationProvider,
                    query.azureEnvironment,
                    query.signInAudience,
                    query.guestHomedIn);
        } catch (com.microsoft.identity.internal.test.labapi.ApiException ex) {
            throw new RuntimeException("Error retrieving lab user", ex);
        }

        return configInfos;
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
        LabAuthenticationHelper.getInstance().setupApiClientWithAccessToken();
        ConfigApi api = new ConfigApi();
        List<ConfigInfo> configInfos;

        try {
            configInfos = api.getConfigByUPN(upn);
        } catch (com.microsoft.identity.internal.test.labapi.ApiException ex) {
            throw new RuntimeException("Error retrieving lab user", ex);
        }

        final ConfigInfo pickedConfig = configInfos.get(0);

        LabConfig labConfig = new LabConfig(pickedConfig);

        LabConfig.setCurrentLabConfig(labConfig);

        return pickedConfig;
    }

    static List<LabConfig> loadUsersForTest(LabUserQuery query) {
        List<LabConfig> labConfigs = new ArrayList<>();
        final List<ConfigInfo> configInfos = getConfigInfos(query);
        for (ConfigInfo configInfo : configInfos) {
            final String password = LabHelper.getPasswordForLab(configInfo.getLabInfo().getLabName());
            labConfigs.add(new LabConfig(configInfo, password));
        }

        return labConfigs;
    }

    public static String loadUserForTest(LabUserQuery query) {
        LabConfig labConfig;
        labConfig = sLabConfigCache.get(query);

        if (labConfig == null) {
            labConfig = loadUsersForTest(query).get(0);
            sLabConfigCache.put(query, labConfig);
        }

        LabConfig.setCurrentLabConfig(labConfig);

        return labConfig.getConfigInfo().getUserInfo().getUpn();
    }

    public static String getPasswordForUser(final String username) {
        final ConfigInfo configInfo = getConfigInfoFromUpn(username);
        return LabHelper.getPasswordForLab(configInfo.getUserInfo().getLabName());
    }

    public static String getPasswordForUser(final LabInfo labInfo) {
        return LabHelper.getPasswordForLab(labInfo.getLabName());
    }

    public static Credential getCredentials(LabUserQuery query) {
        LabConfig labConfig;
        labConfig = sLabConfigCache.get(query);
        Credential credential = new Credential();
        ConfigInfo configInfo = null;

        if (labConfig == null) {
            String password;
            configInfo = getConfigInfo(query);
            password = getPasswordForUser(configInfo.getLabInfo());
            labConfig = new LabConfig(configInfo, password);
            sLabConfigCache.put(query, labConfig);
        }

        LabConfig.setCurrentLabConfig(labConfig);
        credential.userName = configInfo.getUserInfo().getUpn();
        credential.password = labConfig.getLabUserPassword();
        return credential;
    }

    public static void resetPassword(final String upn) {
        ResetApi resetApi = new ResetApi();

        try {
            resetApi.putResetInfo(upn, "Password");
        } catch (ApiException e) {
            throw new RuntimeException("Error resetting lab user password", e);
        }
    }

}
