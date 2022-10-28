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
import androidx.test.uiautomator.UiObjectNotFoundException;

import com.microsoft.identity.client.ui.automation.installer.PlayStore;
import com.microsoft.identity.client.ui.automation.interaction.FirstPartyAppPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

/**
 * A model for interacting with the Teams Android App during UI Test.
 */
public class TeamsApp extends App implements IFirstPartyApp {

    private final static String TAG = TeamsApp.class.getSimpleName();
    private static final String TEAMS_PACKAGE_NAME = "com.microsoft.teams";
    private static final String TEAMS_APP_NAME = "Microsoft Teams";

    public TeamsApp() {
        super(TEAMS_PACKAGE_NAME, TEAMS_APP_NAME, new PlayStore());
    }

    @Override
    public void handleFirstRun() {
        // nothing needed here
    }

    @Override
    public void initialiseAppImpl() {
        // nothing needed here
    }

    @Override
    public void addFirstAccount(@NonNull final String username,
                                @NonNull final String password,
                                @NonNull final FirstPartyAppPromptHandlerParameters promptHandlerParameters) {
        // The Sign In UI in Teams changes depending on if the account(s) are in TSL
        try {
            if (promptHandlerParameters.isExpectingProvidedAccountInTSL()) {
                Logger.i(TAG, "Adding First Account which is in TSL..");
                // This case handles UI if our account (supplied username) is expected to be in TSL
                final UiObject email = UiAutomatorUtils.obtainUiObjectWithResourceIdAndText(
                        "com.microsoft.teams:id/title",
                        username
                );

                email.waitForExists(CommonUtils.FIND_UI_ELEMENT_TIMEOUT);

                email.click();

                Logger.i(TAG, "Handle Sign-In Prompt on the APP for account which is in TSL..");
                final MicrosoftStsPromptHandler microsoftStsPromptHandler = new MicrosoftStsPromptHandler(promptHandlerParameters);
                microsoftStsPromptHandler.handlePrompt(username, password);
            } else if (promptHandlerParameters.isExpectingNonZeroAccountsInTSL()) {
                Logger.i(TAG, "Adding First Account which is not in TSL but some other accounts could be in TSL..");
                // This case handles UI if our account isn't in TSL, however, there are some other
                // accounts expected to be in TSL
                UiAutomatorUtils.handleButtonClick("com.microsoft.teams:id/sign_in_another_account_button");
                signInWithEmail(username, password, promptHandlerParameters);
            } else {
                Logger.i(TAG, "Adding First Account where no account is in TSL..");
                // This case handles UI when there are no accounts in TSL
                signInWithEmail(username, password, promptHandlerParameters);
            }
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void addAnotherAccount(@NonNull final String username,
                                  @NonNull final String password,
                                  @NonNull final FirstPartyAppPromptHandlerParameters promptHandlerParameters) {
        throw new UnsupportedOperationException("Not implemented");
    }

    private void signInWithEmail(@NonNull final String username,
                                 @NonNull final String password,
                                 @NonNull final MicrosoftStsPromptHandlerParameters promptHandlerParameters) {
        Logger.i(TAG, "Sign-In on the APP..");
        // Enter email in email field
        UiAutomatorUtils.handleInput(
                "com.microsoft.teams:id/edit_email",
                username
        );

        // Click Sign in btn
        UiAutomatorUtils.handleButtonClick("com.microsoft.teams:id/sign_in_button");

        Logger.i(TAG, "Handle Sign-In with Email Prompt on the APP..");
        // handle prompt
        final MicrosoftStsPromptHandler microsoftStsPromptHandler = new MicrosoftStsPromptHandler(promptHandlerParameters);
        microsoftStsPromptHandler.handlePrompt(username, password);
    }

    @Override
    public void onAccountAdded() {
        Logger.i(TAG, "Handling UI after account is added on the App..");
        UiAutomatorUtils.handleButtonClick("com.microsoft.teams:id/action_next_button");
        UiAutomatorUtils.handleButtonClick("com.microsoft.teams:id/action_next_button");
        UiAutomatorUtils.handleButtonClick("com.microsoft.teams:id/action_last_button");
    }

    @Override
    public void confirmAccount(@NonNull final String username) {
        Logger.w(TAG, "confirmAccount function Not Implemented..");
        throw new UnsupportedOperationException("Not implemented");
    }
}
