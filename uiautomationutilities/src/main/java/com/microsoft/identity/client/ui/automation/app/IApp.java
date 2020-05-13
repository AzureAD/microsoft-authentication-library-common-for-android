package com.microsoft.identity.client.ui.automation.app;

public interface IApp {

    void install();

    void launch();

    void clear();

    void uninstall();

    void handleFirstRun();
}
