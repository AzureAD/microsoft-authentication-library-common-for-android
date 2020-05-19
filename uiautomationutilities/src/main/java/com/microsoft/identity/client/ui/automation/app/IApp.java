package com.microsoft.identity.client.ui.automation.app;

public interface IApp {

    /**
     * Install this app on the device
     */
    void install();

    /**
     * Launch this app on the device
     */
    void launch();

    /**
     * Clear (storage) associated to this app on the device
     */
    void clear();

    /**
     * Remove this app from the device
     */
    void uninstall();

    /**
     * Handle the first run experience for this app on first time launch
     */
    void handleFirstRun();
}
