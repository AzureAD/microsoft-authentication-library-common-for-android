package com.microsoft.identity.client.ui.automation.browser;

import com.microsoft.identity.client.ui.automation.app.App;

public class BrowserDuckDuckGo extends App implements IBrowser {

    private static final String DUCKDUCKGO_PACAKGE_NAME = "com.duckduckgo.mobile.android";
    private static final String DUCKDUCKGO_APP_NAME = "DuckDuckGo";

    public BrowserDuckDuckGo() {
        super(DUCKDUCKGO_PACAKGE_NAME, DUCKDUCKGO_APP_NAME);
    }

    @Override
    public void navigateTo(String url) {
        //TODO: needs to implement navigateTo.
    }

    @Override
    public String BrowserName() {
        return "DuckDuckGo";
    }

    @Override
    public void handleFirstRun() {
        // nothing to do.
    }
}
