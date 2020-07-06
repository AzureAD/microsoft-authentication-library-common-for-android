package com.microsoft.identity.client.ui.automation.browser;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;

import com.microsoft.identity.client.ui.automation.app.App;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

import static org.junit.Assert.fail;

public class BrowserEdge extends App {

    private static final String EDGE_PACKAGE_NAME = "com.microsoft.emmx";
    private static final String EDGE_APP_NAME = "Microsoft Edge";

    public BrowserEdge() {
        super(EDGE_PACKAGE_NAME, EDGE_APP_NAME);
    }

    @Override
    public void handleFirstRun() {
        // cancel sync in Edge
        UiAutomatorUtils.handleButtonClick("com.microsoft.emmx:id/not_now");
        sleep();
        // cancel sharing data
        UiAutomatorUtils.handleButtonClick("com.microsoft.emmx:id/not_now");
        sleep();
        // cancel personalization
        UiAutomatorUtils.handleButtonClick("com.microsoft.emmx:id/fre_share_not_now");
        sleep();
        // avoid setting default
        UiAutomatorUtils.handleButtonClick("com.microsoft.emmx:id/no");
        sleep();
    }

    public void browse(final String url) {
        final UiDevice device =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        UiAutomatorUtils.handleButtonClick("com.microsoft.emmx:id/search_box_text");

        final UiObject inputField = UiAutomatorUtils.obtainUiObjectWithResourceId(
                "com.microsoft.emmx:id/url_bar"
        );

        try {
            inputField.setText(url);
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }

        device.pressEnter();
    }

    private void sleep() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
    }
}
