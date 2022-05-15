package com.microsoft.identity.labapi.utilities.client;

import com.microsoft.identity.internal.test.labapi.model.ConfigInfo;
import com.microsoft.identity.internal.test.labapi.model.UserInfo;
import com.microsoft.identity.labapi.utilities.authentication.LabApiAuthenticationClient;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to facilitate loading guest accounts from the lab api
 * Takes a lab config object and creates a {@link LabGuest} object to facilitate performing
 * operations on guest accounts
 */
public class LabGuestAccountHelper {

    public static LabGuest loadGuestAccountFromLab(final LabQuery query) {
        final LabApiAuthenticationClient authenticationClient = new LabApiAuthenticationClient(
                "Uav7Q~g06Hwymk8I5WVc1iMLv4UieVDa4XDpB"
        );
        final LabClient labClient = new LabClient(authenticationClient);
        try {
            final List<ConfigInfo> configInfos = labClient.fetchConfigsFromLab(query);

            List<String> guestLabTenants = new ArrayList<>();

            for (ConfigInfo configInfo : configInfos) {
                guestLabTenants.add(configInfo.getUserInfo().getTenantID());
            }

            // pick one config info object to obtain home tenant information
            // doesn't matter which one as all have the same home tenant
            final ConfigInfo configInfo = configInfos.get(0);

            final UserInfo userInfo = configInfo.getUserInfo();

            return new LabGuest(
                    userInfo.getHomeUPN(),
                    userInfo.getHomeDomain(),
                    userInfo.getHomeTenantID(),
                    guestLabTenants
            );
        } catch (LabApiException e) {
            throw new AssertionError(e);
        }
    }

    public static String getPasswordForGuestUser(final LabGuest guestUser) {
        final LabApiAuthenticationClient authenticationClient = new LabApiAuthenticationClient(
                "Uav7Q~g06Hwymk8I5WVc1iMLv4UieVDa4XDpB"
        );
        final LabClient labClient = new LabClient(authenticationClient);
        final String labName = guestUser.getHomeDomain().split("\\.")[0];
        try {
            return labClient.getSecret(labName);
        } catch (LabApiException e){
            throw new AssertionError(e);
        }
    }
}
