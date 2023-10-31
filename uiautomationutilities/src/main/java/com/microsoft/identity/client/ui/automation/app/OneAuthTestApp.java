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

import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.ui.automation.installer.IAppInstaller;
import com.microsoft.identity.client.ui.automation.installer.LocalApkInstaller;
import com.microsoft.identity.client.ui.automation.interaction.FirstPartyAppPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2516682

/**
 * Infrastructure for testing OneAuthTest app
 */
public class OneAuthTestApp extends App implements IFirstPartyApp {
    private final static String TAG = "OneAuthTestApp";
    public final static String ONEAUTH_TESTAPP_PACKAGE_NAME = "com.microsoft.oneauth.testapp";
    public final static String ONEAUTH_TESTAPP_NAME = "OneAuth Testapp";
    public final static String ONEAUTH_TESTAPP_APK = "OneAuthTestApp.apk";
    public final static String OLD_ONEAUTH_TESTAPP_APK = "OldOneAuthTestApp.apk";

    public OneAuthTestApp() {
        super(ONEAUTH_TESTAPP_PACKAGE_NAME, ONEAUTH_TESTAPP_NAME, new LocalApkInstaller());
        localApkFileName = ONEAUTH_TESTAPP_APK;
        localUpdateApkFileName = ONEAUTH_TESTAPP_APK;
    }

    /**
     * Use this install method,
     * While testing for update scenario, or need to install only old Apk.
     * Otherwise use regular install method for installing latest apk.
     */
    public void installOldApk() {
        localApkFileName = OLD_ONEAUTH_TESTAPP_APK;
        install();
    }

    @Override
    protected void initialiseAppImpl() {
        // nothing needed here
    }

    @Override
    public void handleFirstRun() {
        CommonUtils.grantPackagePermission();
        // Because switching the App Configuration will decide whether to truly enable the Broker,
        // it's essential to turn on the Broker beforehand.
        handlePreferBrokerSwitchButton();
        try {
            selectFromAppConfiguration("com.microsoft.identity.LabsApi.Guest");
        } catch (UiObjectNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addFirstAccount(@NonNull final String username,
                                @NonNull final String password,
                                @NonNull final FirstPartyAppPromptHandlerParameters promptHandlerParameters) {
        Logger.i(TAG, "Adding First Account..");
        // sign in with supplied username/password
        signIn(username, password, promptHandlerParameters);
    }

    @Override
    public void addAnotherAccount(@NonNull final String username,
                                  @NonNull final String password,
                                  @NonNull final FirstPartyAppPromptHandlerParameters promptHandlerParameters) {
        // nothing needed here
    }

    public String acquireTokenInteractive(@NonNull final String username,
                                          @NonNull final String password,
                                          @NonNull final FirstPartyAppPromptHandlerParameters promptHandlerParameters) {
        signIn(username, password, promptHandlerParameters);
        return getTokenSecret();
    }

    public String acquireTokenSilent() {
        // Click Get Access token button
        UiAutomatorUtils.handleButtonClick("com.microsoft.oneauth.testapp:id/get_access_token_button");
        try {
            // Add a delay so that UI is updated with the token successfully
            Thread.sleep(5000);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        return getTokenSecret();
    }

    private void signIn(@NonNull final String username,
                        @NonNull final String password,
                        @NonNull final FirstPartyAppPromptHandlerParameters promptHandlerParameters) {
        UiAutomatorUtils.handleButtonClick("com.microsoft.oneauth.testapp:id/sign_in_button");

        Logger.i(TAG, "Adding username and password on login screen..");
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

        Logger.i(TAG, "Handle AAD Login page prompt..");
        // handle prompt in AAD login page
        new AadPromptHandler(promptHandlerParameters).handlePrompt(username, password);
    }

    public void handleSignInWithoutPrompt() {
        UiAutomatorUtils.handleButtonClick("com.microsoft.oneauth.testapp:id/sign_in_button");
        assertSuccess();
    }

    public void handleBackButton() {
        UiAutomatorUtils.pressBack();
    }

    public void handleUserNameInput(@NonNull final String input) {
        UiAutomatorUtils.handleInput("com.microsoft.oneauth.testapp:id/account_hints_edittext", input);
    }

    public void handlePreferBrokerSwitchButton() {
        UiAutomatorUtils.handleButtonClick("com.microsoft.oneauth.testapp:id/prefer_broker_switch_button");
    }

    public void selectFromAppConfiguration(@NonNull final String text) throws UiObjectNotFoundException {
        final UiObject appConfigurationSpinner = UiAutomatorUtils.obtainUiObjectWithResourceId("com.microsoft.oneauth.testapp:id/app_configuration_spinner");
        appConfigurationSpinner.click();
        final UiObject appConfiguration = UiAutomatorUtils.obtainUiObjectWithText(text);
        appConfiguration.click();
    }

    @Override
    public void onAccountAdded() {
        // nothing needed here
    }

    @Override
    public void confirmAccount(@NonNull final String username) {
        // Make sure we are seeing the output text view
        final UiObject resultUIObject = UiAutomatorUtils.obtainUiObjectWithResourceId("com.microsoft.oneauth.testapp:id/txtGeneralInfo");
        try {
            Assert.assertTrue(resultUIObject.getText().contains("Result: Success"));
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError("Could not click on object with txt general info text");
        }
    }

    public void assertSuccess() {
        try {
            final UiObject resultUIObject = UiAutomatorUtils.obtainUiObjectWithResourceId("com.microsoft.oneauth.testapp:id/txtGeneralInfo");
            resultUIObject.waitForExists(FIND_UI_ELEMENT_TIMEOUT);

            final UiObject accountIdObject = UiAutomatorUtils.obtainUiObjectWithResourceId("com.microsoft.oneauth.testapp:id/txtAccountId");
            accountIdObject.waitForExists(FIND_UI_ELEMENT_TIMEOUT);

            Assert.assertTrue(resultUIObject.getText().contains("Result: Success"));
            Assert.assertFalse(TextUtils.isEmpty(resultUIObject.getText()));
            Assert.assertFalse(TextUtils.isEmpty(accountIdObject.getText()));
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError("Could not click on object general text and account id");
        }
    }

    /**
     * Returns string list of all accounts available
     */
    public List<String> getAllAccounts() {
        final List<String> accountsList = new ArrayList<>();
        UiAutomatorUtils.handleButtonClick("com.microsoft.oneauth.testapp:id/get_all_accounts_button");
        final UiObject resultUIObject = UiAutomatorUtils.obtainUiObjectWithResourceId("com.microsoft.oneauth.testapp:id/all_accounts_list");
        try {
            int childCount = resultUIObject.getChildCount();
            for (int i = 0; i < childCount; i++) {
                UiObject object = resultUIObject.getChild(new UiSelector().clickable(true).index(i));
                accountsList.add(object.getText());
            }
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError("Could not find object showing list of accounts");
        }
        return accountsList;
    }

    public String getTokenSecret() {
        final UiObject resultUIObject = UiAutomatorUtils.obtainUiObjectWithResourceId("com.microsoft.oneauth.testapp:id/txtSecret");
        try {
            return resultUIObject.getText();
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }
}
