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
package com.microsoft.identity.client.ui.automation.app;

import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.FIND_UI_ELEMENT_TIMEOUT;
import androidx.annotation.NonNull;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;
import com.microsoft.identity.client.ui.automation.installer.LocalApkInstaller;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * A model for interacting with the Msal Test App for MSAL Android during UI Test.
 */
public class MsalTestApp extends App {

    private final static String TAG = MsalTestApp.class.getSimpleName();
    private static final String MSAL_TEST_APP_PACKAGE_NAME = "com.msft.identity.client.sample.local";
    private static final String MSAL_TEST_APP_NAME = "MSAL Test App";
    public static final String MSAL_TEST_APP_APK = "MsalTestApp.apk";
    public static final String OLD_MSAL_TEST_APP_APK = "OldMsalTestApp.apk";

    // constructors
    public MsalTestApp() {
        super(MSAL_TEST_APP_PACKAGE_NAME, MSAL_TEST_APP_NAME, new LocalApkInstaller());
        localApkFileName = MSAL_TEST_APP_APK;
        localUpdateApkFileName = MSAL_TEST_APP_APK;
    }

    /**
     * Use this install method,
     * While testing for update scenario, or need to install only old Apk.
     * Otherwise use regular install method for installing latest apk.
     */
    public void installOldApk() {
        localApkFileName = OLD_MSAL_TEST_APP_APK;
        install();
    }

    // click on button acquire token interactive
    public String acquireToken(@NonNull final String username,
                               @NonNull final String password,
                               @NonNull final PromptHandlerParameters promptHandlerParameters,
                               @NonNull final boolean shouldHandlePrompt) throws UiObjectNotFoundException, InterruptedException {
        // handle loginHint input if needed
        if (shouldHandlePrompt) {
            UiAutomatorUtils.handleInput("com.msft.identity.client.sample.local:id/loginHint", username);
        }

        final UiObject acquireTokenButton = UiAutomatorUtils.obtainUiObjectWithResourceId("com.msft.identity.client.sample.local:id/btn_acquiretoken");
        scrollToElement(acquireTokenButton);
        acquireTokenButton.click();

        // handle prompt if needed
        if (shouldHandlePrompt) {
            final MicrosoftStsPromptHandler microsoftStsPromptHandler = new MicrosoftStsPromptHandler((MicrosoftStsPromptHandlerParameters) promptHandlerParameters);
            microsoftStsPromptHandler.handlePrompt(username, password);
        }

        // get token and return
        final UiObject result = UiAutomatorUtils.obtainUiObjectWithResourceId("com.msft.identity.client.sample.local:id/txt_result");
        return result.getText();
    }


    // click on button acquire token silent
    public String acquireTokenSilent() throws UiObjectNotFoundException, InterruptedException {
        final UiObject acquireTokenSilentButton = UiAutomatorUtils.obtainUiObjectWithResourceId("com.msft.identity.client.sample.local:id/btn_acquiretokensilent");
        scrollToElement(acquireTokenSilentButton);
        acquireTokenSilentButton.click();
        final UiObject result = UiAutomatorUtils.obtainUiObjectWithResourceId("com.msft.identity.client.sample.local:id/txt_result");
        return result.getText();
    }

    // click on button getUsers
    public List<String> getUsers() throws UiObjectNotFoundException, InterruptedException {
        final UiObject getUsersButton = UiAutomatorUtils.obtainUiObjectWithResourceId("com.msft.identity.client.sample.local:id/btn_getUsers");
        scrollToElement(getUsersButton);
        getUsersButton.click();
        // wait for ui to update
        Thread.sleep(2000);
        // get each user information in the user list
        final List<String> users = new ArrayList<>();
        final UiObject userList = UiAutomatorUtils.obtainUiObjectWithResourceId("com.msft.identity.client.sample.local:id/user_list");
        for (int i = 0; i < userList.getChildCount(); i++) {
            final UiObject user = userList.getChild(new UiSelector().index(i));
            users.add(user.getText());
        }
        return users;
    }

    public void selectFromAuthScheme(@NonNull String text) throws UiObjectNotFoundException {
        final UiObject authSchemeSpinner = UiAutomatorUtils.obtainUiObjectWithResourceId("com.msft.identity.client.sample.local:id/authentication_scheme");
        authSchemeSpinner.click();
        final UiObject authScheme = UiAutomatorUtils.obtainUiObjectWithText(text);
        authScheme.click();
    }

    public String generateSHR() throws UiObjectNotFoundException {
        final UiObject generateSHRButton = UiAutomatorUtils.obtainUiObjectWithResourceId("com.msft.identity.client.sample.local:id/btn_generate_shr");
        scrollToElement(generateSHRButton);
        generateSHRButton.click();
        final UiObject result = UiAutomatorUtils.obtainUiObjectWithResourceId("com.msft.identity.client.sample.local:id/txt_result");
        return result.getText();
    }

    public UiObject removeUser(@NonNull String msg) throws UiObjectNotFoundException {
        final UiObject removeUserButton = UiAutomatorUtils.obtainUiObjectWithResourceId("com.msft.identity.client.sample.local:id/btn_clearCache");
        scrollToElement(removeUserButton);
        removeUserButton.click();
        final UiObject toast = UiAutomatorUtils.obtainUiObjectWithText(msg);
        return toast;
    }

    public UiObject getPackageName(@NonNull String packageName) throws UiObjectNotFoundException, InterruptedException {
        final UiObject getPackageNameButton = UiAutomatorUtils.obtainUiObjectWithResourceId("com.msft.identity.client.sample.local:id/btnGetActiveBroker");
        scrollToElement(getPackageNameButton);
        getPackageNameButton.click();
        final UiObject toast = UiAutomatorUtils.obtainUiObjectWithText(packageName);
        return toast;
    }

    private void scrollToElement(UiObject obj) throws UiObjectNotFoundException {
        UiScrollable scrollable = new UiScrollable(new UiSelector().scrollable(true));
        scrollable.scrollIntoView(obj);
        obj.waitForExists(FIND_UI_ELEMENT_TIMEOUT);
    }

    public void handleBackButton() {
        UiAutomatorUtils.pressBack();
    }

    @Override
    protected void initialiseAppImpl() {}

    @Override
    public void handleFirstRun() {
        UiAutomatorUtils.handleButtonClick("com.msft.identity.client.sample.local:id/btnStartTask");
    }
}
