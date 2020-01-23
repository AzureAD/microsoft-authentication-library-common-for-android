package com.microsoft.identity.internal.testutils.labutils;

import java.util.List;

/**
 * A Class to facilitate writing tests for Guest Accounts
 */
public class LabGuest {
    private String homeUpn;
    private String upn;
    private String homeLabName;
    private String homeTenantId;
    private List<GuestLabTenant> guestLabTenants;

    public LabGuest(String homeUpn, String upn, String homeLabName, String homeTenantId, List<GuestLabTenant> guestLabTenants) {
        this.homeUpn = homeUpn;
        this.upn = upn;
        this.homeLabName = homeLabName;
        this.homeTenantId = homeTenantId;
        this.guestLabTenants = guestLabTenants;
    }

    public String getHomeUpn() {
        return homeUpn;
    }

    public String getUpn() {
        return upn;
    }

    public String getHomeLabName() {
        return homeLabName;
    }

    public String getHomeTenantId() {
        return homeTenantId;
    }

    public List<GuestLabTenant> getGuestLabTenants() {
        return guestLabTenants;
    }

    public static class GuestLabTenant {
        private String labName;
        private String tenantId;

        GuestLabTenant(String labName, String tenantId) {
            this.labName = labName;
            this.tenantId = tenantId;
        }

        public String getLabName() {
            return labName;
        }

        public String getTenantId() {
            return tenantId;
        }
    }
}
