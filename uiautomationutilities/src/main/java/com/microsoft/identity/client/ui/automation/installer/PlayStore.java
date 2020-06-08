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
package com.microsoft.identity.client.ui.automation.installer;

import androidx.annotation.NonNull;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import org.junit.Assert;

import java.util.concurrent.TimeUnit;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.FIND_UI_ELEMENT_TIMEOUT;
import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.getResourceId;
import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.isStringPackageName;
import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.launchApp;

public class PlayStore implements IAppInstaller {

    private static final String GOOGLE_PLAY_PACKAGE_NAME = "com.android.vending";

    // wait at least 5 mins for app installation from Play Store
    private static final long PLAY_STORE_INSTALL_APP_TIMEOUT = TimeUnit.MINUTES.toMillis(5);

    private void searchAppOnGooglePlay(@NonNull final String hint) {
        final UiDevice device = UiDevice.getInstance(getInstrumentation());

        launchApp(GOOGLE_PLAY_PACKAGE_NAME);

        final UiObject searchButton = device.findObject(new UiSelector().resourceId(
                getResourceId(GOOGLE_PLAY_PACKAGE_NAME, "search_bar_hint")
        ));
        try {
            searchButton.waitForExists(FIND_UI_ELEMENT_TIMEOUT);
            searchButton.click();
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }

        final UiObject searchTextField = device.findObject(new UiSelector().resourceId(
                getResourceId(GOOGLE_PLAY_PACKAGE_NAME, "search_bar_text_input")
        ));
        try {
            searchTextField.waitForExists(FIND_UI_ELEMENT_TIMEOUT);
            searchTextField.setText(hint);
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }

        device.pressEnter();
    }

    private void selectGooglePlayAppFromAppList(@NonNull final String appName) throws UiObjectNotFoundException {
        final UiDevice device = UiDevice.getInstance(getInstrumentation());
        final UiObject appIconInSearchResult = device.findObject(new UiSelector().resourceId(
                getResourceId(GOOGLE_PLAY_PACKAGE_NAME, "play_card")
        ).descriptionContains(appName));

        appIconInSearchResult.waitForExists(FIND_UI_ELEMENT_TIMEOUT);
        appIconInSearchResult.click();
    }

    private void selectGooglePlayAppFromInstallBar() throws UiObjectNotFoundException {
        final UiDevice device = UiDevice.getInstance(getInstrumentation());
        final UiObject appInstallBar = device.findObject(new UiSelector().resourceId(
                getResourceId(GOOGLE_PLAY_PACKAGE_NAME, "install_bar")
        ));
        appInstallBar.waitForExists(FIND_UI_ELEMENT_TIMEOUT);
        appInstallBar.click();
    }

    private void selectGooglePlayAppFromAppName(@NonNull final String appName) {
        try {
            selectGooglePlayAppFromInstallBar();
        } catch (UiObjectNotFoundException e) {
            try {
                selectGooglePlayAppFromAppList(appName);
            } catch (UiObjectNotFoundException ex) {
                Assert.fail(ex.getMessage());
            }
        }
    }

    private void selectGooglePlayAppFromPackageName(@NonNull final String appName) {
        final UiDevice device = UiDevice.getInstance(getInstrumentation());

        // we will just take the first app in the list
        final UiObject appIconInSearchResult = device.findObject(new UiSelector().resourceId(
                getResourceId(GOOGLE_PLAY_PACKAGE_NAME, "bucket_items")
        ).childSelector(new UiSelector().textContains(appName)));
        try {
            appIconInSearchResult.waitForExists(FIND_UI_ELEMENT_TIMEOUT);
            appIconInSearchResult.click();
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
    }

    private void installOrOpenAppFromGooglePlay() {
        final UiDevice device = UiDevice.getInstance(getInstrumentation());
        final UiObject installButton = device.findObject(new UiSelector().resourceId(
                getResourceId(GOOGLE_PLAY_PACKAGE_NAME, "right_button")
        ));
        try {
            installButton.waitForExists(FIND_UI_ELEMENT_TIMEOUT);
            installButton.click();
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }

        final UiObject openButton = device.findObject(new UiSelector().resourceId(
                getResourceId(GOOGLE_PLAY_PACKAGE_NAME, "right_button")
        ).textContains("Open").enabled(true));
        // if we see uninstall button, then we know that the installation is complete
        openButton.waitForExists(PLAY_STORE_INSTALL_APP_TIMEOUT);
    }

    @Override
    public void installApp(@NonNull final String searchHint) {
        searchAppOnGooglePlay(searchHint);
        if (isStringPackageName(searchHint)) {
            selectGooglePlayAppFromPackageName(searchHint);
        } else {
            selectGooglePlayAppFromAppName(searchHint);
        }
        installOrOpenAppFromGooglePlay();
    }

}
