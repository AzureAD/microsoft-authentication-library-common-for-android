package com.microsoft.identity.client.ui.automation.broker;

import com.microsoft.identity.client.ui.automation.app.IApp;

public interface ITestBroker extends IApp {

    void handleAccountPicker(String username);

    void performDeviceRegistration(String username, String password);

    void performSharedDeviceRegistration(String username, String password);
}
