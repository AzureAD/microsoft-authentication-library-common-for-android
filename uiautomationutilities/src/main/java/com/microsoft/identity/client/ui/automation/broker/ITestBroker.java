package com.microsoft.identity.client.ui.automation.broker;

import com.microsoft.identity.client.ui.automation.app.IApp;

public interface ITestBroker extends IApp {

    /**
     * Handle the broker account picker. Clicks on the list item associated to the supplied upn
     *
     * @param username upn for the account to select in account picker
     */
    void handleAccountPicker(String username);

    /**
     * Perform device registration with supplied username
     *
     * @param username username of the account to use for registration
     * @param password password of the account to use for registration
     */
    void performDeviceRegistration(String username, String password);

    /**
     * Perform shared device registration with supplied username. This user must be a cloud device
     * admin for the registration to actually succeed.
     *
     * @param username username of the account to use for registration
     * @param password password of the account to use for registration
     */
    void performSharedDeviceRegistration(String username, String password);
}
