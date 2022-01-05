package com.microsoft.identity.internal.testutils.labutils;

import com.microsoft.identity.internal.test.labapi.model.ConfigInfo;
import com.microsoft.identity.internal.test.labapi.model.UserInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to facilitate loading guest accounts from the lab api
 * Takes a lab config object and creates a {@link LabGuest} object to facilitate performing
 * operations on guest accounts
 */
public class LabGuestAccountHelper {

    public static LabGuest loadGuestAccountFromLab(final LabUserQuery query) {
        final List<LabConfig> labConfigs = LabUserHelper.loadUsersForTest(query);

        // set one as the current config; doesn't matter which one
        LabConfig.setCurrentLabConfig(labConfigs.get(0));

        List<String> guestLabTenants = new ArrayList<>();

        for (LabConfig labConfig : labConfigs) {
            guestLabTenants.add(labConfig.getConfigInfo().getUserInfo().getTenantID());
        }

        // pick one config info object to obtain home tenant information
        // doesn't matter which one as all have the same home tenant
        final ConfigInfo configInfo = labConfigs.get(0).getConfigInfo();

        final UserInfo userInfo = configInfo.getUserInfo();

        return new LabGuest(
                userInfo.getHomeUPN(),
                userInfo.getHomeDomain(),
                userInfo.getHomeTenantID(),
                guestLabTenants);
    }

    public static String getPasswordForGuestUser(final LabGuest guestUser) {
        final String labName = guestUser.getHomeDomain().split("\\.")[0];
        return LabHelper.getSecret(labName);
    }
}
