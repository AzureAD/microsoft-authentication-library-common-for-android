package com.microsoft.identity.client.ui.automation.broker;

/**
 * This exception indicates device enrollment failure in Company Portal due to the device limit
 * for enrollment being reached.
 */
public class DeviceLimitReachedException extends RuntimeException {

    private BrokerCompanyPortal companyPortal;

    public DeviceLimitReachedException(String message, BrokerCompanyPortal companyPortal) {
        super(message);
        this.companyPortal = companyPortal;
    }

    /**
     * Obtain the instance of {@link BrokerCompanyPortal} that was being used for enrollment
     *
     * @return instance of {@link BrokerCompanyPortal}
     */
    public BrokerCompanyPortal getCompanyPortal() {
        return companyPortal;
    }
}
