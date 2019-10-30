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
