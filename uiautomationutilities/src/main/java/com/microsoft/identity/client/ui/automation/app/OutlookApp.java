package com.microsoft.identity.client.ui.automation.app;

import com.microsoft.identity.client.ui.automation.installer.PlayStore;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

public class OutlookApp extends App {

    private static final String OUTLOOK_PACKAGE_NAME = "com.microsoft.office.outlook";
    private static final String OUTLOOK_APP_NAME = "Microsoft Outlook";

    public OutlookApp() {
        super(OUTLOOK_PACKAGE_NAME, OUTLOOK_APP_NAME, new PlayStore());
    }

    @Override
    public void handleFirstRun() {
        UiAutomatorUtils.handleButtonClick("com.microsoft.office.outlook:id/btn_splash_start");
    }
}
