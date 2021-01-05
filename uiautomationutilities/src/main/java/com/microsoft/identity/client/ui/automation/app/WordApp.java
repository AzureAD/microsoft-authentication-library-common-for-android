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

import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;

import com.microsoft.identity.client.ui.automation.installer.PlayStore;
import com.microsoft.identity.client.ui.automation.interaction.FirstPartyAppPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandler;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

/**
 * A model for interacting with the Word Android App during UI Test.
 */
public class WordApp extends App implements IFirstPartyApp {

    private final static String TAG = WordApp.class.getSimpleName();
    public static final String WORD_PACKAGE_NAME = "com.microsoft.office.word";
    public static final String WORD_APP_NAME = "Microsoft Word";

    public WordApp() {
        super(WORD_PACKAGE_NAME, WORD_APP_NAME, new PlayStore());
    }

    @Override
    public void handleFirstRun() {
        CommonUtils.grantPackagePermission(); // grant permission to access storage
    }

    @Override
    public void addFirstAccount(@NonNull final String username,
                                @NonNull final String password,
                                @NonNull final FirstPartyAppPromptHandlerParameters promptHandlerParameters) {
        Logger.i(TAG, "Adding First Account..");
        // Enter email
        UiAutomatorUtils.handleInput("com.microsoft.office.word:id/OfcEditText", username);
        // Click continue
        UiAutomatorUtils.handleButtonClick("com.microsoft.office.word:id/OfcActionButton2");

        Logger.i(TAG, "Handle First Account Sign-In Prompt on the APP..");
        // handle prompt
        final MicrosoftStsPromptHandler microsoftStsPromptHandler = new MicrosoftStsPromptHandler(promptHandlerParameters);
        microsoftStsPromptHandler.handlePrompt(username, password);
    }

    @Override
    public void addAnotherAccount(@NonNull final String username,
                                  @NonNull final String password,
                                  @NonNull final FirstPartyAppPromptHandlerParameters promptHandlerParameters) {
        Logger.i(TAG, "Adding Another Account..");
        // Click account drawer
        UiAutomatorUtils.handleButtonClick("com.microsoft.office.word:id/docsui_me_image");
        // Click add account
        UiAutomatorUtils.handleButtonClick("com.microsoft.office.word:id/docsui_account_list_add_account");
        // sing in with supplied username/password
        signIn(username, password, promptHandlerParameters);
    }

    @Override
    public void onAccountAdded() {
        return;
    }

    private void signIn(@NonNull final String username,
                        @NonNull final String password,
                        @NonNull final FirstPartyAppPromptHandlerParameters promptHandlerParameters) {
        Logger.i(TAG, "Sign-In on the APP..");
        try {
            // Word has very interesting sign in UI. They show a custom WebView to accept email
            // No resource id available on anything :(
            final UiObject emailField = UiAutomatorUtils.obtainUiObjectWithTextAndClassType(
                    "", EditText.class
            );

            emailField.setText(username);

            final UiObject nextBtn = UiAutomatorUtils.obtainUiObjectWithTextAndClassType(
                    "Next", Button.class
            );

            nextBtn.click();
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }

        Logger.i(TAG, "Handle Sign-In Prompt on the APP..");
        // handle prompt
        final MicrosoftStsPromptHandler microsoftStsPromptHandler = new MicrosoftStsPromptHandler(promptHandlerParameters);
        microsoftStsPromptHandler.handlePrompt(username, password);
    }

    @Override
    public void confirmAccount(@NonNull final String username) {
        Logger.i(TAG, "Confirm Account with Username on the APP..");
        UiAutomatorUtils.handleButtonClick("com.microsoft.office.word:id/docsui_me_image");

        final UiObject testAccountLabelWord = UiAutomatorUtils.obtainUiObjectWithText(username);
        Assert.assertTrue(
                "Provided user account exists in Word App.",
                testAccountLabelWord.exists()
        );
    }
}
