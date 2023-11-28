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

import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;
import com.microsoft.identity.client.ui.automation.browser.IBrowser;
import com.microsoft.identity.client.ui.automation.installer.LocalApkInstaller;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.labapi.utilities.constants.UserType;

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

    public String acquireToken(@NonNull final String username,
                               @NonNull final String password,
                               @NonNull final PromptHandlerParameters promptHandlerParameters,
                               @NonNull final boolean shouldHandlePrompt) throws UiObjectNotFoundException, InterruptedException {
        return acquireToken(username, password, promptHandlerParameters, null, false, shouldHandlePrompt);
    }
    // click on button acquire token interactive
    public String acquireToken(@NonNull final String username,
                               @NonNull final String password,
                               @NonNull final PromptHandlerParameters promptHandlerParameters,
                               @Nullable final IBrowser browser,
                               final boolean shouldHandleBrowserFirstRun,
                               @NonNull final boolean shouldHandlePrompt) throws UiObjectNotFoundException, InterruptedException {

        final UiObject acquireTokenButton = UiAutomatorUtils.obtainUiObjectWithResourceId("com.msft.identity.client.sample.local:id/btn_acquiretoken");
        scrollToElement(acquireTokenButton);
        acquireTokenButton.click();

        if (promptHandlerParameters.getBroker() == null && browser != null && shouldHandleBrowserFirstRun) {
            // handle browser first run as applicable
            browser.handleFirstRun();
        }
        // handle prompt if needed
        if (shouldHandlePrompt) {
            try {
                final UiObject emailField = UiAutomatorUtils.obtainUiObjectWithTextAndClassType(
                        "", EditText.class);
                emailField.setText(username);
                final UiObject nextBtn = UiAutomatorUtils.obtainUiObjectWithTextAndClassType(
                        "Next", Button.class);
                nextBtn.click();
            } catch (final UiObjectNotFoundException e) {
                throw new AssertionError("Could not click on object with txt Next");
            }
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

    // select from Auth Scheme dropdown
    public void selectFromAuthScheme(@NonNull final String text) throws UiObjectNotFoundException {
        final UiObject authSchemeSpinner = UiAutomatorUtils.obtainUiObjectWithResourceId("com.msft.identity.client.sample.local:id/authentication_scheme");
        authSchemeSpinner.click();
        final UiObject authScheme = UiAutomatorUtils.obtainUiObjectWithText(text);
        authScheme.click();
    }

    // Select configuration to be used from dropdown.
    public void selectFromConfigFile(@NonNull final String text) throws UiObjectNotFoundException {
        final UiObject configFileSpinner = UiAutomatorUtils.obtainUiObjectWithResourceId("com.msft.identity.client.sample.local:id/configFile");
        configFileSpinner.click();
        final UiObject configFile = UiAutomatorUtils.obtainUiObjectWithText(text);
        configFile.click();
    }

    // click on button generateSHR
    public String generateSHR() throws UiObjectNotFoundException {
        final UiObject generateSHRButton = UiAutomatorUtils.obtainUiObjectWithResourceId("com.msft.identity.client.sample.local:id/btn_generate_shr");
        scrollToElement(generateSHRButton);
        generateSHRButton.click();
        final UiObject result = UiAutomatorUtils.obtainUiObjectWithResourceId("com.msft.identity.client.sample.local:id/txt_result");
        return result.getText();
    }

    // click on button removeUser
    public String removeUser() throws UiObjectNotFoundException {
        final UiObject removeUserButton = UiAutomatorUtils.obtainUiObjectWithResourceId("com.msft.identity.client.sample.local:id/btn_clearCache");
        scrollToElement(removeUserButton);
        removeUserButton.click();
        final UiObject textView = UiAutomatorUtils.obtainUiObjectWithResourceId("com.msft.identity.client.sample.local:id/dialog_message");
        final String text = textView.getText();
        return text;
    }

    // click on button removeUser on Legacy MsalTestApp
    public String removeUserLegacy() throws UiObjectNotFoundException {
        final UiObject removeUserButton = UiAutomatorUtils.obtainUiObjectWithResourceId("com.msft.identity.client.sample.local:id/btn_clearCache");
        scrollToElement(removeUserButton);
        removeUserButton.click();
        final UiObject textView = UiAutomatorUtils.obtainUiObjectWithResourceId("com.msft.identity.client.sample.local:id/status");
        final String text = textView.getText();
        return text;
    }

    // click on button getActiveBroker
    public String getActiveBrokerPackageName() throws UiObjectNotFoundException {
        final UiObject getPackageNameButton = UiAutomatorUtils.obtainUiObjectWithResourceId("com.msft.identity.client.sample.local:id/btnGetActiveBroker");
        scrollToElement(getPackageNameButton);
        getPackageNameButton.click();
        final UiObject textView = UiAutomatorUtils.obtainUiObjectWithResourceId("com.msft.identity.client.sample.local:id/dialog_message");
        final String text = textView.getText();
        return text;
    }

    // check MsalTestApp mode
    public String checkMode() throws UiObjectNotFoundException {
        final UiObject modeText = UiAutomatorUtils.obtainUiObjectWithResourceId("com.msft.identity.client.sample.local:id/public_application_mode");
        return modeText.getText();
    }

    private void scrollToElement(UiObject obj) throws UiObjectNotFoundException {
        UiScrollable scrollable = new UiScrollable(new UiSelector().scrollable(true));
        scrollable.scrollIntoView(obj);
        obj.waitForExists(FIND_UI_ELEMENT_TIMEOUT);
    }

    public void handleBackButton() {
        UiAutomatorUtils.pressBack();
    }

    public void handleUserNameInput(@NonNull final String input) {
        UiAutomatorUtils.handleInput("com.msft.identity.client.sample.local:id/loginHint", input);
    }

    @Override
    protected void initialiseAppImpl() {}

    @Override
    public void handleFirstRun() {
        UiAutomatorUtils.handleButtonClick("com.msft.identity.client.sample.local:id/btnStartTask");
    }

    // Handles first run of the app based on the user account type to be used.
    public void handleFirstRunBasedOnUserType(UserType userType) throws UiObjectNotFoundException {
        handleFirstRun();
        if (userType == UserType.MSA) {
            selectFromConfigFile("MSA");
        }
    }
}
