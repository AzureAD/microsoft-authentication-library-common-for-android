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

import com.microsoft.identity.internal.test.labapi.api.UserApi;
import com.microsoft.identity.internal.test.labapi.model.UserInfo;

import java.util.List;

public class LabUserHelper {
    public static UserInfo getUserInfo(LabUserQuery query) {
        LabAuthenticationHelper.setupApiClientWithAccessToken();
        UserApi api = new UserApi();
        List<UserInfo> userInfos;

        try {
            userInfos = api.get(query.userType, query.mfa, query.protectionPolicy, query.homeDomain, query.homeUpn, query.b2cProvider, query.federationProvider, query.azureEnvironment, query.signInAudience);
        } catch (com.microsoft.identity.internal.test.labapi.ApiException ex) {
            throw new RuntimeException("Error retrieving lab user", ex);
        }

        final UserInfo pickedUser = userInfos.get(0);
        CurrentLabUser.userInfo = pickedUser;

        return pickedUser;

    }

    public static UserInfo getUserInfoFromUpn(final String upn) {
        LabAuthenticationHelper.setupApiClientWithAccessToken();
        UserApi api = new UserApi();
        List<UserInfo> userInfos;

        try {
            userInfos = api.getUserByUPN(upn);
        } catch (com.microsoft.identity.internal.test.labapi.ApiException ex) {
            throw new RuntimeException("Error retrieving lab user", ex);
        }

        final UserInfo pickedUser = userInfos.get(0);
        CurrentLabUser.userInfo = pickedUser;

        return pickedUser;
    }

    public static String getUpnForTest(LabUserQuery query) {
        final UserInfo userInfo = getUserInfo(query);
        return userInfo.getUpn();
    }

    public static String getPasswordForUser(final String username) {
        final UserInfo userInfo = getUserInfoFromUpn(username);
        return LabSecretHelper.getPasswordForLab(userInfo.getLabName());
    }

    public static String getPasswordForUser(final UserInfo userInfo) {
        return LabSecretHelper.getPasswordForLab(userInfo.getLabName());
    }

    public static Credential getCredentials(LabUserQuery query) {
        UserInfo userInfo;
        Credential credential = new Credential();

        userInfo = getUserInfo(query);
        credential.userName = userInfo.getUpn();
        credential.password = getPasswordForUser(userInfo);

        return credential;
    }

}
