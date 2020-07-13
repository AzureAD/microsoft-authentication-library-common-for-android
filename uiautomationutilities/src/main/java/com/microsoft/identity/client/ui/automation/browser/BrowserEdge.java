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
        sleep();
        // cancel sharing data
        UiAutomatorUtils.handleButtonClick("com.microsoft.emmx:id/not_now");
        sleep();
        // cancel personalization
        UiAutomatorUtils.handleButtonClick("com.microsoft.emmx:id/fre_share_not_now");
        sleep();
        // avoid setting default
        UiAutomatorUtils.handleButtonClick("com.microsoft.emmx:id/no");
        sleep();
    }

    public void browse(final String url) {
        final UiDevice device =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        UiAutomatorUtils.handleButtonClick("com.microsoft.emmx:id/search_box_text");

        final UiObject inputField = UiAutomatorUtils.obtainUiObjectWithResourceId(
                "com.microsoft.emmx:id/url_bar"
        );

        try {
            inputField.setText(url);
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }

        device.pressEnter();
    }

    private void sleep() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
    }

    public void signIn(@NonNull final String username,
                       @NonNull final String password,
                       @NonNull final FirstPartyAppPromptHandlerParameters promptHandlerParameters) {
        try {
            if (promptHandlerParameters.isExpectingProvidedAccountInTSL()) {
                final String expectedText = "Sign in as " + username;

                final UiObject signInAsBtn = UiAutomatorUtils.obtainUiObjectWithText(expectedText);
                signInAsBtn.click();

                final AadPromptHandler aadPromptHandler = new AadPromptHandler(promptHandlerParameters);
                aadPromptHandler.handlePrompt(username, password);

                handleFirstRun();
            } else if (promptHandlerParameters.isExpectingNonZeroAccountsInTSL()) {
                final UiObject signInWithAnotherAccount = UiAutomatorUtils.obtainUiObjectWithText(
                        "Sign in with another account"
                );

                signInWithAnotherAccount.click();
                signInWithWorkOrSchoolAccount(username, password, promptHandlerParameters);
            } else {
                signInWithWorkOrSchoolAccount(username, password, promptHandlerParameters);
            }
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }

        //todo implement MSA sign in for Microsoft Edge
    }

    private void signInWithWorkOrSchoolAccount(@NonNull final String username,
                                               @NonNull final String password,
                                               @NonNull final FirstPartyAppPromptHandlerParameters promptHandlerParameters) throws UiObjectNotFoundException {
        final UiObject signInWithWorkAccountBtn = UiAutomatorUtils.obtainUiObjectWithText(
                "Sign in with a work or school account"
        );

        signInWithWorkAccountBtn.click();

        final AadPromptHandler aadPromptHandler = new AadPromptHandler(promptHandlerParameters);
        aadPromptHandler.handlePrompt(username, password);

        handleFirstRun();
    }
}
