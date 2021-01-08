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
import com.microsoft.identity.client.ui.automation.interaction.FirstPartyAppPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

import static org.junit.Assert.fail;

/**
 * A model for interacting with the Microsoft Edge Browser App during UI Test.
 */
public class BrowserEdge extends App implements IBrowser {

    private static final String EDGE_PACKAGE_NAME = "com.microsoft.emmx";
    private static final String EDGE_APP_NAME = "Microsoft Edge";

    public BrowserEdge() {
        super(EDGE_PACKAGE_NAME, EDGE_APP_NAME);
    }

    @Override
    public void handleFirstRun() {
        // cancel sync in Edge
        UiAutomatorUtils.handleButtonClick("com.microsoft.emmx:id/not_now");
        sleep(); // need to use sleep due to Edge animations
        // cancel sharing data
        UiAutomatorUtils.handleButtonClick("com.microsoft.emmx:id/not_now");
        sleep(); // need to use sleep due to Edge animations
        // cancel personalization
        UiAutomatorUtils.handleButtonClick("com.microsoft.emmx:id/fre_share_not_now");
        sleep();// need to use sleep due to Edge animations
        // avoid setting default
        UiAutomatorUtils.handleButtonClick("com.microsoft.emmx:id/no");
        sleep();// need to use sleep due to Edge animations
    }

    public void navigateTo(@NonNull final String url) {
        //  Click on the search bar in the browser UI
        UiAutomatorUtils.handleButtonClick("com.microsoft.emmx:id/search_box_text");

        final UiObject inputField = UiAutomatorUtils.obtainUiObjectWithResourceId(
                "com.microsoft.emmx:id/url_bar"
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

    private void sleep() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    public void signIn(@NonNull final String username,
                       @NonNull final String password,
                       @NonNull final FirstPartyAppPromptHandlerParameters promptHandlerParameters) {
        // The Sign In UI in Edge is different depending on if account(s) are in TSL
        try {
            if (promptHandlerParameters.isExpectingProvidedAccountInTSL()) {
                // This case handles the UI if our account is expected to be in TSL
                final String expectedText = "Sign in as " + username;

                // Click the sign in btn pre-populated with our UPN
                final UiObject signInAsBtn = UiAutomatorUtils.obtainUiObjectWithText(expectedText);
                signInAsBtn.click();

                // handle prompt
                final AadPromptHandler aadPromptHandler = new AadPromptHandler(promptHandlerParameters);
                aadPromptHandler.handlePrompt(username, password);

                handleFirstRun();
            } else if (promptHandlerParameters.isExpectingNonZeroAccountsInTSL()) {
                // This case handles UI when our account is not in TSL, however, there are other
                // accounts in TSL

                // Click sign in with another account
                final UiObject signInWithAnotherAccount = UiAutomatorUtils.obtainUiObjectWithText(
                        "Sign in with another account"
                );

                signInWithAnotherAccount.click();

                // now select sign in with work or school account
                signInWithWorkOrSchoolAccount(username, password, promptHandlerParameters);
            } else {
                signInWithWorkOrSchoolAccount(username, password, promptHandlerParameters);
            }
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }

        //todo implement MSA sign in for Microsoft Edge
    }

    private void signInWithWorkOrSchoolAccount(@NonNull final String username,
                                               @NonNull final String password,
                                               @NonNull final FirstPartyAppPromptHandlerParameters promptHandlerParameters) throws UiObjectNotFoundException {
        final UiObject signInWithWorkAccountBtn = UiAutomatorUtils.obtainUiObjectWithText(
                "Sign in with a work or school account"
        );

        // click Sign In with work or school account btn
        signInWithWorkAccountBtn.click();

        // handle prompt
        final AadPromptHandler aadPromptHandler = new AadPromptHandler(promptHandlerParameters);
        aadPromptHandler.handlePrompt(username, password);

        handleFirstRun();
    }
}
