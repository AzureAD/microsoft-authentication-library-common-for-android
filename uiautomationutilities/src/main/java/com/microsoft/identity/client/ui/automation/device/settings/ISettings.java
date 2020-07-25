package com.microsoft.identity.client.ui.automation.device.settings;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.ui.automation.broker.ITestBroker;

/**
 * An interface describing the Settings app on an Android device during UI Automation
 */
public interface ISettings {

    /**
     * Launch the device admin page in Settings app
     */
    void launchDeviceAdminSettingsPage();

    /**
     * Disable the supplied admin app on the device
     *
     * @param adminName the admin app to be disabled
     */
    void disableAdmin(@NonNull final String adminName);

    /**
     * Launch the add account page in Settings app
     */
    void launchAddAccountPage();

    /**
     * Launch the account list page in Settings app
     */
    void launchAccountListPage();

    /**
     * Remove the supplied account from the Account Manager
     *
     * @param username the username of the account to be removed
     */
    void removeAccount(@NonNull final String username);

    /**
     * Add the supplied account to the device via Account Manager
     *
     * @param expectedBroker the broker expected to be used for the account type
     * @param username       the username of the account to add
     * @param password       the password of the account to add
     */
    void addWorkAccount(final ITestBroker expectedBroker,
                        final String username,
                        final String password);

    /**
     * Launch the date & time page in Settings app
     */
    void launchDateTimeSettingsPage();

    /**
     * Change the time on the device by advancing the clock by 24 hours
     */
    void changeDeviceTime();

    /**
     * Activate this admin app. This method is supposed to be called when the Activate Device Admin
     * UI appears on the device. It will activate the admin for whichever admin requested the activation.
     */
    void activateAdmin();
}
