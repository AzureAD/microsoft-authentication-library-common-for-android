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
    private final static String ONEAUTH_TESTAPP_PACKAGE_NAME = "com.microsoft.oneauth.testapp";
    private final static String ONEAUTH_TESTAPP_NAME = "OneAuth Testapp";
    private final static String ONEAUTH_TESTAPP_APK = "OneAuth.apk";

    public OneAuthTestApp() {
        super(ONEAUTH_TESTAPP_PACKAGE_NAME, ONEAUTH_TESTAPP_NAME, new LocalApkInstaller());
    }

    public OneAuthTestApp(@NonNull final IAppInstaller appInstaller) {
        super(ONEAUTH_TESTAPP_PACKAGE_NAME, ONEAUTH_TESTAPP_NAME, appInstaller);
        localApkFileName = ONEAUTH_TESTAPP_APK;
    }

    @Override
    protected void initialiseAppImpl() {

    }

    public void clickSignIn() {
        Logger.i(TAG, "Clicking Sign In Button..");
        UiAutomatorUtils.handleButtonClick("com.microsoft.oneauth.testapp:id/sign_in_button");
    }

    @Override
    public void handleFirstRun() {
        CommonUtils.grantPackagePermission();
    }

    @Override
    public void addFirstAccount(@NonNull String username, @NonNull String password, @NonNull FirstPartyAppPromptHandlerParameters promptHandlerParameters) {
        Logger.i(TAG, "Adding First Account..");
        clickSignIn();

        // sign in with supplied username/password
        signIn(username, password, promptHandlerParameters);
    }

    public String acquireTokenInteractive(@NonNull final String username,
                                          @NonNull final String password,
                                          @NonNull final FirstPartyAppPromptHandlerParameters promptHandlerParameters) {
        clickSignIn();
        signIn(username, password, promptHandlerParameters);

        return getTokeSecret();
    }

    public String acquireTokenSilent() {
        // Click Get Access token button
        UiAutomatorUtils.handleButtonClick("com.microsoft.oneauth.testapp:id/get_access_token_button");
        try {
            // Add a delay so that token can be retrived successfully
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return getTokeSecret();
    }

    private void signIn(@NonNull final String username,
                        @NonNull final String password,
                        @NonNull final FirstPartyAppPromptHandlerParameters promptHandlerParameters) {
        Logger.i(TAG, "Adding username and password on login screen..");

        try {
            final UiObject emailField = UiAutomatorUtils.obtainUiObjectWithTextAndClassType(
                    "", EditText.class);
            emailField.setText(username);
            final UiObject nextBtn = UiAutomatorUtils.obtainUiObjectWithTextAndClassType(
                    "Next", Button.class);
            nextBtn.click();
        } catch (UiObjectNotFoundException e) {
            throw new RuntimeException(e);
        }

        Logger.i(TAG, "Handle AAD Login page prompt..");
        // handle prompt in AAD login page
        new AadPromptHandler(promptHandlerParameters).handlePrompt(username, password);
    }

    public void handleBackButton() {
        UiAutomatorUtils.pressBack();
    }

    @Override
    public void addAnotherAccount(String username, String password, FirstPartyAppPromptHandlerParameters promptHandlerParameters) {

    }

    @Override
    public void onAccountAdded() {

    }

    @Override
    public void confirmAccount(@NonNull String username) {
        // Make sure we are seeing the output text view
        final UiObject resultUIObject = UiAutomatorUtils.obtainUiObjectWithResourceId("com.microsoft.oneauth.testapp:id/txtGeneralInfo");
        try {
            Assert.assertTrue(resultUIObject.getText().contains("Result: Success"));
        } catch (UiObjectNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void assertSuccess() {
        final UiObject resultUIObject = UiAutomatorUtils.obtainUiObjectWithResourceId("com.microsoft.oneauth.testapp:id/txtGeneralInfo");
        final UiObject accountIdObject = UiAutomatorUtils.obtainUiObjectWithResourceId("com.microsoft.oneauth.testapp:id/txtAccountId");
        try {
            resultUIObject.waitForExists(FIND_UI_ELEMENT_TIMEOUT);
            accountIdObject.waitForExists(FIND_UI_ELEMENT_TIMEOUT);
            Assert.assertTrue(resultUIObject.getText().contains("Result: Success"));
            Assert.assertFalse(TextUtils.isEmpty(resultUIObject.getText()));
            Assert.assertFalse(TextUtils.isEmpty(accountIdObject.getText()));
        } catch (UiObjectNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void getAllAccounts() {
        List<String> accountsList = new ArrayList<>();
        UiAutomatorUtils.handleButtonClick("com.microsoft.oneauth.testapp:id/get_all_accounts_button");
        final UiObject resultUIObject = UiAutomatorUtils.obtainUiObjectWithResourceId("com.microsoft.oneauth.testapp:id/all_accounts_list");
        try {
            int size = resultUIObject.getChildCount();
            final UiSelector textView = new UiSelector()
                    .className("android.widget.TextView");
            for (int i = 0; i < size; i++) {
                UiObject object = resultUIObject.getChild(textView);
                accountsList.add(object.getText());
            }
        } catch (UiObjectNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public String getTokeSecret() {
        final UiObject resultUIObject = UiAutomatorUtils.obtainUiObjectWithResourceId("com.microsoft.oneauth.testapp:id/txtSecret");
        try {
            return resultUIObject.getText();
        } catch (UiObjectNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
