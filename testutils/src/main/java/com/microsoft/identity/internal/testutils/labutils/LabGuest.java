package com.microsoft.identity.internal.testutils.labutils;

import java.util.List;

/**
 * A Class to facilitate writing tests for Guest Accounts
 */
public class LabGuest {
    private String homeUpn;
    private String homeLabName;
    private String homeTenantId;
    private List<String> guestLabTenants;

    public LabGuest(String homeUpn, String homeLabName, String homeTenantId, List<String> guestLabTenants) {
        this.homeUpn = homeUpn;
        this.homeLabName = homeLabName;
        this.homeTenantId = homeTenantId;
        this.guestLabTenants = guestLabTenants;
    }

    public String getHomeUpn() {
        return homeUpn;
    }

    public String getHomeLabName() {
        return homeLabName;
    }

    public String getHomeTenantId() {
        return homeTenantId;
    }

    public List<String> getGuestLabTenants() {
        return guestLabTenants;
    }
}
