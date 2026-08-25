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

import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.FIND_UI_ELEMENT_TIMEOUT_LONG;

import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.ui.automation.installer.IAppInstaller;
import com.microsoft.identity.client.ui.automation.installer.PlayStore;
import com.microsoft.identity.client.ui.automation.interaction.FirstPartyAppPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandler;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

import java.util.concurrent.TimeUnit;

/**
 * A model for interacting with the Outlook Android App during UI Test.
 */
public class OutlookApp extends App implements IFirstPartyApp {

    private final static String TAG = OutlookApp.class.getSimpleName();
    public static final String OUTLOOK_PACKAGE_NAME = "com.microsoft.office.outlook";
    public static final String OUTLOOK_APP_NAME = "Microsoft Outlook";
    public static final String OUTLOOK_APK = "Outlook.apk";

    private final static String ACCOUNT_BUTTON_RESOURCE_ID = OUTLOOK_PACKAGE_NAME + ":id/account_button";

    /**
     * Number of times we open the navigation drawer looking for the signed-in account before giving
     * up. Outlook can raise a transient teaching callout over the drawer which hides the drawer from
     * the accessibility tree; dismissing it and re-opening the drawer clears the condition.
     */
    private final static int CONFIRM_ACCOUNT_MAX_ATTEMPTS = 3;
    private static final String ADD_ANOTHER_ACCOUNT_TEXT = "Add another account";
    private static final String M365_ACCOUNT_TYPE_RESOURCE_ID_REGEX =
            "com\\.microsoft\\.office\\.outlook:id/btn_add_account_(m365|o365)_rest";
    private static final long ACCOUNT_TYPE_POLL_INTERVAL_MILLISECONDS =
            TimeUnit.SECONDS.toMillis(1);

    public OutlookApp() {
        super(OUTLOOK_PACKAGE_NAME, OUTLOOK_APP_NAME, new PlayStore());
    }

    public OutlookApp(@NonNull final IAppInstaller appInstaller) {
        super(OUTLOOK_PACKAGE_NAME, OUTLOOK_APP_NAME, appInstaller);
        localApkFileName = OUTLOOK_APK;
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

        handleChooseAccountTypeIfPresent();

        // Make sure we are on add another account (shows up after an account is added)
        final UiObject addAnotherAccountScreen = UiAutomatorUtils.obtainUiObjectWithText(
                ADD_ANOTHER_ACCOUNT_TEXT,
                TimeUnit.SECONDS.toMillis(45)
        );

        Assert.assertTrue(
                "Add another account screen doesn't appear in Outlook.", addAnotherAccountScreen.exists()
        );

        // click may be later
        UiAutomatorUtils.handleButtonClick("com.microsoft.office.outlook:id/bottom_flow_navigation_start_button");
    }

    private void handleChooseAccountTypeIfPresent() {
        Logger.i(TAG, "Checking for the optional Outlook account type screen.");
        final UiSelector accountTypeSelector =
                new UiSelector().resourceIdMatches(M365_ACCOUNT_TYPE_RESOURCE_ID_REGEX);
        final UiObject accountTypeOption =
                UiAutomatorUtils.obtainUiObjectWithUiSelector(accountTypeSelector, 0);
        final UiObject addAnotherAccountScreen =
                UiAutomatorUtils.obtainUiObjectWithText(ADD_ANOTHER_ACCOUNT_TEXT, 0);
        final long timeout = SystemClock.elapsedRealtime() + FIND_UI_ELEMENT_TIMEOUT_LONG;

        while (!accountTypeOption.exists()
                && !addAnotherAccountScreen.exists()
                && SystemClock.elapsedRealtime() < timeout) {
            final long remainingTimeout = timeout - SystemClock.elapsedRealtime();
            accountTypeOption.waitForExists(
                    Math.min(
                            ACCOUNT_TYPE_POLL_INTERVAL_MILLISECONDS,
                            Math.max(0, remainingTimeout)
                    )
            );
        }

        if (addAnotherAccountScreen.exists() || !accountTypeOption.exists()) {
            Logger.i(TAG, "Outlook account type selection is not required.");
            return;
        }

        Logger.i(TAG, "Selecting the Microsoft 365/Office 365 account type.");
        try {
            accountTypeOption.click();
        } catch (final UiObjectNotFoundException exception) {
            Assert.fail(
                    "Microsoft 365/Office 365 account type option could not be clicked."
            );
        }
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

        handleIntroDialogueAfterSignIn();

        for (int attempt = 1; attempt <= CONFIRM_ACCOUNT_MAX_ATTEMPTS; attempt++) {
            // Click the account drawer
            UiAutomatorUtils.handleButtonClick(ACCOUNT_BUTTON_RESOURCE_ID, FIND_UI_ELEMENT_TIMEOUT_LONG);

            // Make sure our account is listed in the account drawer. Give the first attempt the full
            // timeout; retries only need to outlast the drawer animation as the account is either
            // already there or genuinely absent.
            final long lookupTimeout = (attempt == 1)
                    ? FIND_UI_ELEMENT_TIMEOUT_LONG
                    : CommonUtils.FIND_UI_ELEMENT_TIMEOUT_SHORT;

            if (UiAutomatorUtils.obtainUiObjectWithText(username, lookupTimeout).exists()) {
                Logger.i(TAG, "Account confirmed in the Outlook account drawer on attempt " + attempt + ".");
                return;
            }

            Logger.w(TAG, "Account was not listed in the Outlook account drawer on attempt " + attempt
                    + " of " + CONFIRM_ACCOUNT_MAX_ATTEMPTS + ". Dismissing any transient popup and retrying..");

            dismissDrawerAndTransientPopups();
        }

        Assert.fail("Expected account " + username + " to be listed in the Outlook account drawer, "
                + "but it was not found after " + CONFIRM_ACCOUNT_MAX_ATTEMPTS + " attempts.");
    }

    /**
     * Taps the scrim to the right of the Outlook navigation drawer to dismiss any transient popup
     * and close the drawer.
     * <p>
     * Outlook intermittently raises a teaching callout (for example the "Now your folders on mobile
     * match the same order you have in other Outlook apps" tip) in its own popup window shortly
     * after the drawer opens. While that popup is up, UiAutomator resolves selectors against the
     * popup's window, so the drawer's account label is unreachable even though it is plainly visible
     * on screen. Tapping outside dismisses the callout and closes the drawer so the next attempt
     * starts from a clean state; the callout is only shown once, so it does not reappear.
     */
    private void dismissDrawerAndTransientPopups() {
        final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // The drawer and the callout occupy the left portion of the screen, so a tap near the right
        // edge lands on the scrim rather than on any drawer or callout content.
        final int x = (int) (device.getDisplayWidth() * 0.95);
        final int y = device.getDisplayHeight() / 2;

        Logger.i(TAG, "Dismissing the Outlook navigation drawer and any transient popup..");
        device.click(x, y);
        device.waitForIdle(CommonUtils.FIND_UI_ELEMENT_TIMEOUT_SHORT);
    }

    private void handleIntroDialogueAfterSignIn() {
        UiAutomatorUtils.handleButtonClickSafely("com.microsoft.office.outlook:id/btn_primary_button", CommonUtils.FIND_UI_ELEMENT_TIMEOUT_SHORT);
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

    /**
     * Add an account to outlook that would show up in the "accounts found" page
     * @param username username of the acount to be added
     */
    public void addExistingFirstAccount(@NonNull final String username) {
        Logger.i(TAG, "Adding Existing Account..");
        // Click start btn
        UiAutomatorUtils.handleButtonClick("com.microsoft.office.outlook:id/btn_primary_button");

        Assert.assertTrue("Not on Accounts found page", UiAutomatorUtils.obtainUiObjectWithExactText("Accounts found", CommonUtils.FIND_UI_ELEMENT_TIMEOUT_SHORT).exists());
        Assert.assertTrue("Couldn't find account:" + username, UiAutomatorUtils.obtainUiObjectWithText(username).exists());

        // Click Continue btn
        UiAutomatorUtils.handleButtonClick("com.microsoft.office.outlook:id/btn_primary_button");
    }

    /**
     * Sign in through the SIGN IN button shown through snackbar after a token expires.
     *
     * @param username username to be signed in
     * @param password password to be used
     * @param promptHandlerParameters prompt handling parameters
     */
    public void signInThroughSnackBar(@NonNull final String username,
                                      @NonNull final String password,
                                      @NonNull final FirstPartyAppPromptHandlerParameters promptHandlerParameters) {
        handleIntroDialogueAfterSignIn();

        // Click SIGN IN Button in snackBar
        UiAutomatorUtils.handleButtonClick("com.microsoft.office.outlook:id/snackbar_action");

        // handle login prompt
        final MicrosoftStsPromptHandler microsoftStsPromptHandler = new MicrosoftStsPromptHandler(promptHandlerParameters);
        microsoftStsPromptHandler.handlePrompt(username, password);
    }

    /**
     * Check to see if the sign in snackbar is present in outlook
     * @return whether or not snackbar is present
     */
    public boolean isSignInSnackBarPresent() {
        // Check if the sign in SnackBar is present
        return UiAutomatorUtils.obtainUiObjectWithResourceId("com.microsoft.office.outlook:id/snackbar_action", TimeUnit.SECONDS.toMillis(5)).exists();
    }

    /**
     * Check that outlook does not have an option for phone sign-up
     * @return true if the option is not available, false otherwise
     */
    public boolean checkPhoneSignUpIsNotAvailable() {
        launch();

        Logger.i(TAG, "Checking that sign-up through phone number is not available in Outlook...");
        // Click start btn
        UiAutomatorUtils.handleButtonClick("com.microsoft.office.outlook:id/btn_secondary_button");

        // Check for "phone" UI option
        final Boolean check1 = UiAutomatorUtils.obtainUiObjectWithText("phone").exists();

        // Check for "Phone" UI option
        final Boolean check2 = UiAutomatorUtils.obtainUiObjectWithText("Phone").exists();

        // Check for "PHONE" UI option
        final Boolean check3 = UiAutomatorUtils.obtainUiObjectWithText("PHONE").exists();

        // If none of those options are found, we can conclude phone option is not available
        return !(check1 || check2 || check3);
    }
}
