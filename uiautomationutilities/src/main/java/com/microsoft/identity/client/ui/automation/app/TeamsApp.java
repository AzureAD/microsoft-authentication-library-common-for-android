package com.microsoft.identity.client.ui.automation.app;

import androidx.annotation.NonNull;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;

import com.microsoft.identity.client.ui.automation.installer.PlayStore;
import com.microsoft.identity.client.ui.automation.interaction.FirstPartyAppPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

public class TeamsApp extends App implements IFirstPartyApp {

    private static final String TEAMS_PACKAGE_NAME = "com.microsoft.teams";
    private static final String TEAMS_APP_NAME = "Microsoft Teams";

    public TeamsApp() {
        super(TEAMS_PACKAGE_NAME, TEAMS_APP_NAME, new PlayStore());
    }


    @Override
    public void handleFirstRun() {
        // nothing needed here
    }

    public void addFirstAccount(@NonNull final String username,
                                @NonNull final String password,
                                @NonNull final FirstPartyAppPromptHandlerParameters promptHandlerParameters) {
        // looks like this is no longer needed
        //UiAutomatorUtils.handleButtonClick("com.microsoft.teams:id/welcome_sign_in_button");

        try {
            if (promptHandlerParameters.isExpectingProvidedAccountInTSL()) {

                final UiObject email = UiAutomatorUtils.obtainUiObjectWithResourceIdAndText(
                        "com.microsoft.teams:id/email",
                        username
                );

                email.click();

                final MicrosoftStsPromptHandler microsoftStsPromptHandler = new MicrosoftStsPromptHandler(promptHandlerParameters);
                microsoftStsPromptHandler.handlePrompt(username, password);
            } else if (promptHandlerParameters.isExpectingNonZeroAccountsInTSL()) {
                UiAutomatorUtils.handleButtonClick("com.microsoft.teams:id/sign_in_another_account_button");
                signInWithEmail(username, password, promptHandlerParameters);
            } else {
                signInWithEmail(username, password, promptHandlerParameters);
            }
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Override
    public void addAnotherAccount(String username, String password, FirstPartyAppPromptHandlerParameters promptHandlerParameters) {
        throw new UnsupportedOperationException("Not implemented");
    }

    private void signInWithEmail(@NonNull final String username,
                                 @NonNull final String password,
                                 @NonNull final MicrosoftStsPromptHandlerParameters promptHandlerParameters) {
        UiAutomatorUtils.handleInput(
                "com.microsoft.teams:id/edit_email",
                username
        );
        UiAutomatorUtils.handleButtonClick("com.microsoft.teams:id/sign_in_button");

        final MicrosoftStsPromptHandler microsoftStsPromptHandler = new MicrosoftStsPromptHandler(promptHandlerParameters);
        microsoftStsPromptHandler.handlePrompt(username, password);
    }

    public void onAccountAdded() {
        UiAutomatorUtils.handleButtonClick("com.microsoft.teams:id/action_next_button");
        UiAutomatorUtils.handleButtonClick("com.microsoft.teams:id/action_next_button");
        UiAutomatorUtils.handleButtonClick("com.microsoft.teams:id/action_last_button");
    }

    @Override
    public void confirmAccount(@NonNull String username) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
