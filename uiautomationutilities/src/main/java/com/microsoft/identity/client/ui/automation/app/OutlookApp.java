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

import androidx.annotation.NonNull;
import androidx.test.uiautomator.UiObject;

import com.microsoft.identity.client.ui.automation.installer.PlayStore;
import com.microsoft.identity.client.ui.automation.interaction.FirstPartyAppPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandler;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

/**
 * A model for interacting with the Outlook Android App during UI Test.
 */
public class OutlookApp extends App implements IFirstPartyApp {

    private final static String TAG = OutlookApp.class.getSimpleName();
    private static final String OUTLOOK_PACKAGE_NAME = "com.microsoft.office.outlook";
    private static final String OUTLOOK_APP_NAME = "Microsoft Outlook";

    public OutlookApp() {
        super(OUTLOOK_PACKAGE_NAME, OUTLOOK_APP_NAME, new PlayStore());
    }

    @Override
    public void handleFirstRun() {
        // nothing required
    }

    @Override
    public void initialiseAppImpl() {
        // nothing required
    }

    @Override
    public void addFirstAccount(@NonNull final String username,
                                @NonNull final String password,
                                @NonNull final FirstPartyAppPromptHandlerParameters promptHandlerParameters) {
        Logger.i(TAG, "Adding First Account..");
        // Click start btn
        UiAutomatorUtils.handleButtonClick("com.microsoft.office.outlook:id/btn_primary_button");

        // sign in with supplied username/password
        signIn(username, password, promptHandlerParameters);
    }

    @Override
    public void onAccountAdded() {
        Logger.i(TAG, "Handling UI after account is added on the App..");
        // Make sure we are on add another account (shows up after an account is added)
        final UiObject addAnotherAccountScreen = UiAutomatorUtils.obtainUiObjectWithText("Add another account");
        Assert.assertTrue(
                "Add another account screen appears in Outlook account.",
                addAnotherAccountScreen.exists()
        );

        // click may be later
        UiAutomatorUtils.handleButtonClick("com.microsoft.office.outlook:id/bottom_flow_navigation_start_button");

    }

    @Override
    public void addAnotherAccount(final String username,
                                  final String password,
                                  final FirstPartyAppPromptHandlerParameters promptHandlerParameters) {
        Logger.i(TAG, "Adding Another Account..");
        // Click the account drawer
        UiAutomatorUtils.handleButtonClick("com.microsoft.office.outlook:id/account_button");

        // click the add account btn
        UiAutomatorUtils.handleButtonClick("com.microsoft.office.outlook:id/btn_add_account");

        // Click add normal account
        UiAutomatorUtils.handleButtonClick("com.microsoft.office.outlook:id/add_normal_account");

        // sign in with this account
        signIn(username, password, promptHandlerParameters);
    }

    @Override
    public void confirmAccount(@NonNull final String username) {
        Logger.i(TAG, "Confirming account with supplied username is signed in..");
        // Click the account drawer
        UiAutomatorUtils.handleButtonClickLongTimeout("com.microsoft.office.outlook:id/account_button");

        // Make sure our account is listed in the account drawer
        final UiObject testAccountLabel = UiAutomatorUtils.obtainUiObjectWithText(username);
        Assert.assertTrue(
                "Provided user account exists in Outlook App.",
                testAccountLabel.exists()
        );
    }

    private void signIn(@NonNull final String username,
                        @NonNull final String password,
                        @NonNull final FirstPartyAppPromptHandlerParameters promptHandlerParameters) {
        Logger.i(TAG, "Sign-In on the APP..");
        // enter email in edit text email field
        UiAutomatorUtils.handleInput("com.microsoft.office.outlook:id/auto_complete_input_email", username);

        // click continue
        UiAutomatorUtils.handleButtonClick("com.microsoft.office.outlook:id/btn_primary_button");

        Logger.i(TAG, "Handle Sign-In Prompt on the APP..");
        // handle login prompt
        final MicrosoftStsPromptHandler microsoftStsPromptHandler = new MicrosoftStsPromptHandler(promptHandlerParameters);
        microsoftStsPromptHandler.handlePrompt(username, password);
    }
}
