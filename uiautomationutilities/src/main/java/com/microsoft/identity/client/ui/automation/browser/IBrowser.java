package com.microsoft.identity.client.ui.automation.browser;

/**
 * An interface describing a browser app on an Android device during UI Automated test
 */
public interface IBrowser {

    /**
     * Browse to the supplied url using this browser
     *
     * @param url the url to open
     */
    void browse(final String url);

}
