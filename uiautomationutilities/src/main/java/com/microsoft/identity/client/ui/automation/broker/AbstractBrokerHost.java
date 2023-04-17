//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.client.ui.automation.broker;

import androidx.annotation.NonNull;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;

import com.microsoft.identity.client.ui.automation.utils.CommonUtils;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

import java.util.concurrent.TimeUnit;

import lombok.AllArgsConstructor;
import lombok.Getter;

abstract class AbstractBrokerHost {
    public final static String BROKER_HOST_APP_PACKAGE_NAME = "com.microsoft.identity.testuserapp";
    public final static String BROKER_HOST_APP_NAME = "Broker Host App";
    private final static long APP_LAUNCH_TIMEOUT = TimeUnit.SECONDS.toMillis(5);
    private final static String HEADER_RESOURCE_ID = CommonUtils.getResourceId(BROKER_HOST_APP_PACKAGE_NAME, "text_header");

    // Shared items
    protected final static String USERNAME_EDIT_TEXT = "edit_text_username";
    // Dialog box resources id
    protected final static String DIALOG_BOX_RESOURCE_ID = "android:id/message";
    protected final static String DIALOG_BOX_OK_BUTTON_RESOURCE_ID = "android:id/button1";


    @AllArgsConstructor
    protected enum BrokerHostNavigationMenuItem {
        MULTIPLE_WPJ_API("multiple_wpj_api", "Multiple WPJ API"),
        SINGLE_WPJ_API("single_wpj_api", "Single WPJ API"),
        BROKER_API("broker_api", "Broker API"),
        FLIGHTS_API("broker_flights", "Broker Flights API");

        @Getter
        private final String resourceId;
        @Getter
        private final String title;
    }

    public void clickButton(@NonNull final String resourceIdButton) {
        final String resourceId = CommonUtils.getResourceId(
                BROKER_HOST_APP_PACKAGE_NAME,
                resourceIdButton
        );
        UiAutomatorUtils.handleButtonClick(resourceId);
    }

    public void fillTextBox(@NonNull final String resourceEditText, @NonNull final String text) {
        final String resourceId = CommonUtils.getResourceId(
                BROKER_HOST_APP_PACKAGE_NAME,
                resourceEditText
        );
        UiAutomatorUtils.handleInput(resourceId, text);
    }

    public String readTextBox(@NonNull final String resourceEditText) {
        final String resourceId = CommonUtils.getResourceId(
                BROKER_HOST_APP_PACKAGE_NAME,
                resourceEditText
        );
        final UiObject textBox = UiAutomatorUtils.obtainUiObjectWithResourceId(resourceId);
        try {
            return textBox.getText();
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError("Could not read text from text box", e);
        }
    }

    //This method dismisses the dialog box and get the text from it
    static public String dismissDialogBoxAndGetText() {
        // Look for the dialog box
        final UiObject dialogBox = UiAutomatorUtils.obtainUiObjectWithResourceId(
                DIALOG_BOX_RESOURCE_ID
        );
        Assert.assertTrue("Assert dialog box", dialogBox.exists());
        try {
            final String dialogBoxText = dialogBox.getText();
            final String[] dialogBoxSplit = dialogBoxText.split(":");
            return dialogBoxSplit.length > 1 ? dialogBoxSplit[1] : dialogBoxText;
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        } finally {
            // dismiss dialog
            UiAutomatorUtils.handleButtonClick(DIALOG_BOX_OK_BUTTON_RESOURCE_ID);
        }
    }

    static public void delay(final long delayInSeconds) {
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(delayInSeconds));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    //this method validates the actual text contains the expected text
        //AssertMessageDialogContainsText
    static protected void dismissDialogBoxAndAssertContainsText(@NonNull final String expectedText) {
        Assert.assertTrue(
                "Could not find the string '" + expectedText + "' in the msg displayed in the dialog",
                dismissDialogBoxAndGetText().contains(expectedText)
        );
    }

    public abstract void launch();

    static protected void launch(@NonNull final BrokerHostNavigationMenuItem navigationMenuItem) {
        final UiObject appHeader = UiAutomatorUtils.obtainUiObjectWithExactText(
                BROKER_HOST_APP_NAME,
                APP_LAUNCH_TIMEOUT
        );
        // If the app is not launched, launch it
        if (!appHeader.exists()) {
            CommonUtils.launchApp(BROKER_HOST_APP_PACKAGE_NAME);
            AbstractBrokerHost.delay(1);
        }
        try {
            final UiObject header = UiAutomatorUtils.obtainUiObjectWithResourceId(
                    HEADER_RESOURCE_ID,
                    APP_LAUNCH_TIMEOUT
            );
            if (header.exists() && navigationMenuItem.getTitle().equalsIgnoreCase(header.getText())) {
                // We're already on the correct fragment, exit
                return;
            }
            // Open the navigation drawer
            final UiObject drawerButton = UiAutomatorUtils.obtainUiObjectWithDescription("Open");
            drawerButton.click();
            // Click on the navigation menu item
            final UiObject menuItem = UiAutomatorUtils.obtainUiObjectWithResourceId(
                    CommonUtils.getResourceId(BROKER_HOST_APP_PACKAGE_NAME, navigationMenuItem.getResourceId())
            );
            menuItem.click();
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

}
