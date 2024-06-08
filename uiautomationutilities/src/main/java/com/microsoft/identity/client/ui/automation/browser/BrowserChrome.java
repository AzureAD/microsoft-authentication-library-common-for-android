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
package com.microsoft.identity.client.ui.automation.browser;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;

import com.microsoft.identity.client.ui.automation.app.App;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import java.util.concurrent.TimeUnit;


/**
 * A model for interacting with the Google Chrome Browser App during UI Test.
 */
public class BrowserChrome extends App implements IBrowser {

    private static final boolean LITE_MODE_EXPECTED = false;
    private static final String TAG = BrowserChrome.class.getSimpleName();
    public static final String CHROME_PACKAGE_NAME = "com.android.chrome";
    public static final String CHROME_APP_NAME = "Google Chrome";

    public BrowserChrome() {
        super(CHROME_PACKAGE_NAME, CHROME_APP_NAME);
    }

    @Override
    public void handleFirstRun() {
        // Make Chrome handleFirstRun safe
        try {
            Logger.i(TAG, "Handle First Run of Browser..");
            UiAutomatorUtils.handleButtonClick("com.android.chrome:id/terms_accept", CommonUtils.FIND_UI_ELEMENT_TIMEOUT_SHORT);
            if (LITE_MODE_EXPECTED) {
                UiAutomatorUtils.handleButtonClickForObjectWithText("Next");
            }
            UiAutomatorUtils.handleButtonClick("com.android.chrome:id/negative_button", CommonUtils.FIND_UI_ELEMENT_TIMEOUT_SHORT);
        } catch (AssertionError e) {
            if (e.toString().contains("UiObjectNotFoundException")){
                UiAutomatorUtils.handleButtonClickSafely("com.android.chrome:id/signin_fre_dismiss_button", CommonUtils.FIND_UI_ELEMENT_TIMEOUT_SHORT);
                Logger.i(TAG, "Handle First Run had a UIObjectNotFoundException, do not throw AssertionError");
            } else {
                throw e;
            }
        }
    }

    @Override
    public void initialiseAppImpl() {
        // nothing needed here
    }

    @Override
    public void navigateTo(@NonNull final String url) {
        Logger.i(TAG, "Navigate to the URL in the browser..");
        UiAutomatorUtils.handleButtonClick("com.android.chrome:id/search_box_text");

        final UiObject inputField = UiAutomatorUtils.obtainUiObjectWithResourceId(
                "com.android.chrome:id/url_bar"
        );

        try {
            // enter the URL
            inputField.setText(url);
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }

        final UiDevice device =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // press enter on the Keyboard
        device.pressEnter();
    }

    /**
     * Method used to reload a page in chrome.
     */
    public void reloadPage() {
        UiAutomatorUtils.handleButtonClick("com.android.chrome:id/menu_button");
        UiAutomatorUtils.handleButtonClick("com.android.chrome:id/button_five");
    }
}
