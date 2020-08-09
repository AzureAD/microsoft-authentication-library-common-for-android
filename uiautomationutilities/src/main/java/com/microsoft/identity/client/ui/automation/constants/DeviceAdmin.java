package com.microsoft.identity.client.ui.automation.constants;

/**
 * Admins that can be present and interacted with on a device during a UI Automated Test.
 */
public enum DeviceAdmin {

    COMPANY_PORTAL("Company Portal"),
    MICROSOFT_AUTHENTICATOR("Authenticator");

    private String adminName;

    DeviceAdmin(String adminName) {
        this.adminName = adminName;
    }

    public String getAdminName() {
        return this.adminName;
    }

}
