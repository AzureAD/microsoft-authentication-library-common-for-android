package com.microsoft.identity.client.ui.automation.broker;

public class DeviceLimitReachedException extends RuntimeException {

    private BrokerCompanyPortal companyPortal;

    public DeviceLimitReachedException(String message, BrokerCompanyPortal companyPortal) {
        super(message);
        this.companyPortal = companyPortal;
    }

    public BrokerCompanyPortal getCompanyPortal() {
        return companyPortal;
    }
}
