package com.microsoft.identity.internal.testutils.labutils;

import com.microsoft.identity.internal.test.labapi.model.ConfigInfo;

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

        List<LabGuest.GuestLabTenant> guestLabTenants = new ArrayList<>();

        for (LabConfig labConfig : labConfigs) {
            guestLabTenants.add(getGuestLabTenant(labConfig.getConfigInfo()));
        }

        // pick one config info object to obtain home tenant information
        // doesn't matter which one as all have the same home tenant
        final ConfigInfo configInfo = labConfigs.get(0).getConfigInfo();

        return new LabGuest(
                configInfo.getUserInfo().getHomeUPN(),
                configInfo.getUserInfo().getUpn(),
                configInfo.getLabInfo().getLabName(),
                configInfo.getLabInfo().getTenantId(),
                guestLabTenants
        );
    }

    private static String getGuestTenantLabName(ConfigInfo configInfo) {
        if (configInfo == null || !configInfo.getUserInfo().getUserType().toLowerCase().equals(LabConstants.UserType.GUEST.toLowerCase())) {
            return null;
        }

        final String[] upnParts = configInfo.getUserInfo().getUpn().split("#");
        final String guestTenantName = upnParts[upnParts.length - 1];
        final String[] guestTenantNameParts = guestTenantName.split("\\.");
        return guestTenantNameParts[0].substring(1);
    }

    private static String getGuestLabTenantId(final String labName) {
        return LabHelper.getLabTenantId(labName);
    }

    private static LabGuest.GuestLabTenant getGuestLabTenant(final ConfigInfo configInfo) {
        final String labName = getGuestTenantLabName(configInfo);
        final String tenantId = getGuestLabTenantId(labName);
        return new LabGuest.GuestLabTenant(labName, tenantId);
    }
}
