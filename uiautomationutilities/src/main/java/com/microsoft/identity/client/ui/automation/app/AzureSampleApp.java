package com.microsoft.identity.client.ui.automation.app;

import androidx.annotation.NonNull;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;

import com.microsoft.identity.client.ui.automation.browser.IBrowser;
import com.microsoft.identity.client.ui.automation.installer.LocalApkInstaller;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

public class AzureSampleApp extends App {

    private static final String AZURE_SAMPLE_PACKAGE_NAME = "com.azuresamples.msalandroidapp";
    private static final String AZURE_SAMPLE_APP_NAME = "Azure Sample";
    public final static String AZURE_SAMPLE_APK = "AzureSample.apk";

    public AzureSampleApp() {
        super(AZURE_SAMPLE_PACKAGE_NAME, AZURE_SAMPLE_APP_NAME, new LocalApkInstaller());
        localApkFileName = AZURE_SAMPLE_APK;
    }

    @Override
    public void handleFirstRun() {

    }

    public void signIn(final String username,
                       final String password,
                       final IBrowser browser,
                       final boolean shouldHandleFirstRun,
                       final MicrosoftStsPromptHandlerParameters promptHandlerParameters) {
        UiAutomatorUtils.handleButtonClick("com.azuresamples.msalandroidapp:id/btn_signIn");

        if (promptHandlerParameters.getBroker() == null && browser != null && shouldHandleFirstRun) {
            ((IApp) browser).handleFirstRun();
        }

        MicrosoftStsPromptHandler microsoftStsPromptHandler =
                new MicrosoftStsPromptHandler(promptHandlerParameters);

        microsoftStsPromptHandler.handlePrompt(username, password);
    }

    public void signOut() {
        UiAutomatorUtils.handleButtonClick("com.azuresamples.msalandroidapp:id/btn_removeAccount");
    }

    public void confirmSignedIn(@NonNull final String username) {
        final UiObject signedInUser = UiAutomatorUtils.obtainUiObjectWithResourceId("com.azuresamples.msalandroidapp:id/current_user");
        try {
            Assert.assertEquals(signedInUser.getText(), username);
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
    }
}
