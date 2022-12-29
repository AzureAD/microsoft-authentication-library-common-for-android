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
import androidx.annotation.Nullable;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.ui.automation.app.App;
import com.microsoft.identity.client.ui.automation.installer.IAppInstaller;
import com.microsoft.identity.client.ui.automation.interaction.FirstPartyAppPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

/**
 * A model for interacting with the Microsoft Edge Browser App during UI Test.
 */
public class BrowserEdge extends App implements IBrowser {

    private final static String TAG = BrowserEdge.class.getSimpleName();
    private static final String EDGE_PACKAGE_NAME = "com.microsoft.emmx";
    private static final String EDGE_APP_NAME = "Microsoft Edge";
    private static final String EDGE_APK = "Edge.apk";
    private boolean shouldHandleAutoFill = true;

    public BrowserEdge() {
        super(EDGE_PACKAGE_NAME, EDGE_APP_NAME);
    }

    public BrowserEdge(@NonNull final IAppInstaller appInstaller) {
        super(EDGE_PACKAGE_NAME, EDGE_APP_NAME, appInstaller);
        localApkFileName = EDGE_APK;
    }

    /**
     * Overriding the launch function to add a check for autofill ui
     */
    @Override
    public void launch() {
        super.launch();
        if (shouldHandleAutoFill) {
            UiAutomatorUtils.handleButtonClickForObjectWithTextSafely("No, thanks");
            shouldHandleAutoFill = false;
        }
    }

    @Override
    public void handleFirstRun() {
        Logger.i(TAG, "Handle First Run of Browser..");
        // cancel sync in Edge
        UiAutomatorUtils.handleButtonClickForObjectWithText("Not now");
        sleep(); // need to use sleep due to Edge animations
    }

    @Override
    public void initialiseAppImpl() {
        // nothing needed here
    }

    public void navigateTo(@NonNull final String url) {
        Logger.i(TAG, "Navigate to the given URL:" + url + " in the browser..");
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
        Logger.i(TAG, "Put Browser on Sleep for 3 sec..");
        try {
            Thread.sleep(3000);
        } catch (final InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    public void signIn(@NonNull final String username,
                       @NonNull final String password,
                       @NonNull final FirstPartyAppPromptHandlerParameters promptHandlerParameters) {
        // The Sign In UI in Edge is different depending on if account(s) are in TSL
        try {
            if (promptHandlerParameters.isExpectingProvidedAccountInTSL()) {
                Logger.i(TAG, "Sign-In on the browser if account is expected to be in TSL..");
                // This case handles the UI if our account is expected to be in TSL
                final String expectedText = "Sign in as " + username;

                // Click the sign in btn pre-populated with our UPN
                final UiObject signInAsBtn = UiAutomatorUtils.obtainUiObjectWithText(expectedText);
                signInAsBtn.click();

                Logger.i(TAG, "Handle Sign-In Prompt for account which is expected to be in TSL..");
                // handle prompt
                final AadPromptHandler aadPromptHandler = new AadPromptHandler(promptHandlerParameters);
                aadPromptHandler.handlePrompt(username, password);

                handleFirstRun();
            } else if (promptHandlerParameters.isExpectingNonZeroAccountsInTSL()) {
                Logger.i(TAG, "Sign-In on the browser if given account is not in TSL but others could be..");
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
                Logger.i(TAG, "Sign-In on the browser if no account is in TSL..");
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
        // click Sign In with work or school account btn
        final UiObject signInWithWorkAccountBtn = UiAutomatorUtils.obtainUiObjectWithText(
                "Add account"
        );

        // Sometimes, we don't see the "Sign in to turn on sync page", so we must navigate to it.
        if (!signInWithWorkAccountBtn.exists()) {
            UiAutomatorUtils.handleButtonClick("com.microsoft.emmx:id/edge_account_image_view");
            UiAutomatorUtils.handleButtonClickForObjectWithText("sign in to sync");
        }
        signInWithWorkAccountBtn.click();

        Logger.i(TAG, "Handle Sign-In Prompt for Work or School account..");
        // handle email field - the email field in Edge UI is missing a resource id, so we find it with EditText class
        final UiObject emailField = UiAutomatorUtils.obtainUiObjectWithUiSelector(new UiSelector().className("android.widget.EditText"), CommonUtils.FIND_UI_ELEMENT_TIMEOUT);
        try {
            emailField.setText(username);
            UiAutomatorUtils.handleButtonClickForObjectWithText("Next");
        }catch(UiObjectNotFoundException ex){
            throw new AssertionError(ex);
        }

        // handle prompt
        final AadPromptHandler aadPromptHandler = new AadPromptHandler(promptHandlerParameters);
        aadPromptHandler.handlePrompt(username, password);

        // Handle confirm page that loads after password prompt
        UiAutomatorUtils.handleButtonClickForObjectWithText("Confirm");
        
        handleFirstRun();
    }

    public boolean confirmSignedIn(@Nullable final String username) {
        Logger.i(TAG, "Checking if account " + username + "is signed in to Edge.");
        launch();

        // Depending on when edge was opened and when account was signed out, we might see this
        if (username == null && UiAutomatorUtils.obtainUiObjectWithText("Add account").exists()){
            return true;
        }

        UiAutomatorUtils.handleButtonClick("com.microsoft.emmx:id/edge_account_image_view");

        // If we're checking that no account is signed in, we can check for specific text suggesting that the user sign in
        if (username == null) {
            final UiObject signInToSyncObject = UiAutomatorUtils.obtainUiObjectWithText("sign in to sync");
            return signInToSyncObject.exists();
        }

        final UiObject usernameObject = UiAutomatorUtils.obtainUiObjectWithExactText(username);
        return usernameObject.exists();
    }
}
