package com.microsoft.identity.client.ui.automation.device.settings;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.ui.automation.broker.ITestBroker;

public interface ISettings {

    void launchDeviceAdminSettingsPage();

    void disableAdmin(@NonNull final String adminName);

    void launchAddAccountPage();

    void launchAccountListPage();

    void removeAccount(@NonNull final String username);

    void addWorkAccount(final ITestBroker expectedBroker,
                        final String username,
                        final String password);

    void launchDateTimeSettingsPage();

    void changeDeviceTime();

    void activateAdmin();
}
