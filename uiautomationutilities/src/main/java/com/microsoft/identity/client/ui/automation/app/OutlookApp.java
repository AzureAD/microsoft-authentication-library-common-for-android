package com.microsoft.identity.client.ui.automation.app;

import androidx.annotation.NonNull;
import androidx.test.uiautomator.UiObject;

import com.microsoft.identity.client.ui.automation.installer.PlayStore;
import com.microsoft.identity.client.ui.automation.interaction.FirstPartyAppPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandler;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

public class OutlookApp extends App implements IFirstPartyApp {

    private static final String OUTLOOK_PACKAGE_NAME = "com.microsoft.office.outlook";
    private static final String OUTLOOK_APP_NAME = "Microsoft Outlook";

    public OutlookApp() {
        super(OUTLOOK_PACKAGE_NAME, OUTLOOK_APP_NAME, new PlayStore());
    }

    @Override
    public void handleFirstRun() {
        // nothing required
    }

    public void addFirstAccount(@NonNull final String username,
                                @NonNull final String password,
                                @NonNull final FirstPartyAppPromptHandlerParameters promptHandlerParameters) {
        UiAutomatorUtils.handleButtonClick("com.microsoft.office.outlook:id/btn_splash_start");

        signIn(username, password, promptHandlerParameters);
    }

    public void onAccountAdded() {
        final UiObject addAnotherAccountScreen = UiAutomatorUtils.obtainUiObjectWithText("Add another account");
        Assert.assertTrue(addAnotherAccountScreen.exists());

        // click may be later
        UiAutomatorUtils.handleButtonClick("com.microsoft.office.outlook:id/bottom_flow_navigation_start_button");

        // Skip through account added optional UI
        UiAutomatorUtils.handleButtonClick("com.microsoft.office.outlook:id/product_tour_skip_btn");
    }

    public void addAnotherAccount(final String username,
                                  final String password,
                                  final FirstPartyAppPromptHandlerParameters promptHandlerParameters) {
        // Click the account drawer
        UiAutomatorUtils.handleButtonClick("com.microsoft.office.outlook:id/account_button");

        // click the add account btn
        UiAutomatorUtils.handleButtonClick("com.microsoft.office.outlook:id/btn_add_account");

        // Click add normal account
        UiAutomatorUtils.handleButtonClick("com.microsoft.office.outlook:id/add_normal_account");

        signIn(username, password, promptHandlerParameters);
    }

    public void confirmAccount(@NonNull final String username) {
        // Click the account drawer
        UiAutomatorUtils.handleButtonClick("com.microsoft.office.outlook:id/account_button");

        // Make sure our account is listed in the account drawer
        final UiObject testAccountLabel = UiAutomatorUtils.obtainUiObjectWithText(username);
        Assert.assertTrue(testAccountLabel.exists());
    }

    public void signIn(@NonNull final String username,
                       @NonNull final String password,
                       @NonNull final FirstPartyAppPromptHandlerParameters promptHandlerParameters) {
        UiAutomatorUtils.handleInput("com.microsoft.office.outlook:id/edit_text_email", username);

        UiAutomatorUtils.handleButtonClick("com.microsoft.office.outlook:id/btn_continue");

        MicrosoftStsPromptHandler microsoftStsPromptHandler = new MicrosoftStsPromptHandler(promptHandlerParameters);
        microsoftStsPromptHandler.handlePrompt(username, password);
    }
}
