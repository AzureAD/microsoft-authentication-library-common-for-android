package com.microsoft.identity.client.ui.automation.app;

import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;

import com.microsoft.identity.client.ui.automation.installer.PlayStore;
import com.microsoft.identity.client.ui.automation.interaction.FirstPartyAppPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandler;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

public class WordApp extends App implements IFirstPartyApp {

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
    public void addFirstAccount(@NonNull String username, @NonNull String password, @NonNull FirstPartyAppPromptHandlerParameters promptHandlerParameters) {
        UiAutomatorUtils.handleInput("com.microsoft.office.word:id/OfcEditText", username);
        UiAutomatorUtils.handleButtonClick("com.microsoft.office.word:id/OfcActionButton2");
        MicrosoftStsPromptHandler microsoftStsPromptHandler = new MicrosoftStsPromptHandler(promptHandlerParameters);
        microsoftStsPromptHandler.handlePrompt(username, password);
    }

    public void addAnotherAccount(final String username,
                                  final String password,
                                  final FirstPartyAppPromptHandlerParameters promptHandlerParameters) {
        // Click account drawer
        UiAutomatorUtils.handleButtonClick("com.microsoft.office.word:id/docsui_me_image");
        // Click add account
        UiAutomatorUtils.handleButtonClick("com.microsoft.office.word:id/docsui_account_list_add_account");

        signIn(username, password, promptHandlerParameters);
    }

    @Override
    public void onAccountAdded() {

    }

    private void signIn(final String username,
                        final String password,
                        final FirstPartyAppPromptHandlerParameters promptHandlerParameters) {

        try {
            final UiObject emailField = UiAutomatorUtils.obtainUiObjectWithTextAndClassType(
                    "", EditText.class
            );

            emailField.setText(username);

            final UiObject nextBtn = UiAutomatorUtils.obtainUiObjectWithTextAndClassType(
                    "Next", Button.class
            );

            nextBtn.click();
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }


        MicrosoftStsPromptHandler microsoftStsPromptHandler = new MicrosoftStsPromptHandler(promptHandlerParameters);
        microsoftStsPromptHandler.handlePrompt(username, password);
    }

    public void confirmAccount(final String username) {
        UiAutomatorUtils.handleButtonClick("com.microsoft.office.word:id/docsui_me_image");

        final UiObject testAccountLabelWord = UiAutomatorUtils.obtainUiObjectWithText(username);
        Assert.assertTrue(testAccountLabelWord.exists());
    }
}
