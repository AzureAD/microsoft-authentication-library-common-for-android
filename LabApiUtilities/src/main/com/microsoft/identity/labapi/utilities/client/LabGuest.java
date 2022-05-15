package com.microsoft.identity.labapi.utilities.client;

import java.util.List;

/**
 * A Class to facilitate writing tests for Guest Accounts
 */
public class LabGuest {
    private final String homeUpn;
    private final String homeTenantId;
    private final String homeDomain;
    private final List<String> guestLabTenants;

    public LabGuest(String homeUpn, String homeDomain, String homeTenantId, List<String> guestLabTenants) {
        this.homeUpn = homeUpn;
        this.homeDomain = homeDomain;
        this.homeTenantId = homeTenantId;
        this.guestLabTenants = guestLabTenants;
    }

    public String getHomeUpn() {
        return homeUpn;
    }

    public String getHomeDomain() {
        return homeDomain;
    }

    public String getHomeTenantId() {
        return homeTenantId;
    }

    public List<String> getGuestLabTenants() {
        return guestLabTenants;
    }
}
