package com.microsoft.identity.client.ui.automation.app;

import com.microsoft.identity.client.ui.automation.installer.PlayStore;

public class WordApp extends App {

    private static final String WORD_PACKAGE_NAME = "com.microsoft.office.word";
    private static final String WORD_APP_NAME = "Microsoft Word";

    public WordApp() {
        super(WORD_PACKAGE_NAME, WORD_APP_NAME, new PlayStore());
    }

    @Override
    public void handleFirstRun() {

    }
}
