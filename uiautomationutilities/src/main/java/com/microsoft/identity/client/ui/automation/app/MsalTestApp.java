package com.microsoft.identity.client.ui.automation.app;

import androidx.annotation.NonNull;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;

import com.microsoft.identity.client.ui.automation.installer.LocalApkInstaller;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.providers.oauth2.IDToken;

import org.junit.Assert;

import java.util.Map;

/**
 * A model for interacting with the Msal Test App for MSAL Android during UI Test.
 */

public class MsalTestApp extends App{

    private final static String TAG = MsalTestApp.class.getSimpleName();
    private static final String MSAL_TEST_APP_PACKAGE_NAME = "com.microsoft.identity.client.testapp";
    private static final String MSAL_TEST_APP_NAME = "MSAL Test App";
    public static final String MSAL_TEST_APP_APK = "MsalTestApp.apk";
    public static final String OLD_MSAL_TEST_APP_APK = "OldMsalTestApp.apk";



    // constructors
    public MsalTestApp() {
        super(MSAL_TEST_APP_PACKAGE_NAME, MSAL_TEST_APP_NAME, new LocalApkInstaller());
        localApkFileName = MSAL_TEST_APP_APK;
    }

    public MsalTestApp(@NonNull final String msalTestAppApk, @NonNull final String updateMsalTestAppApk) {
        super(MSAL_TEST_APP_PACKAGE_NAME, MSAL_TEST_APP_NAME, new LocalApkInstaller());
        localApkFileName = msalTestAppApk;
        localUpdateApkFileName = updateMsalTestAppApk;
    }

    // click on button acquire token interactive
    public String acquireToken(@NonNull final String username,
                                        @NonNull final String password,
                                        @NonNull final PromptHandlerParameters promptHandlerParameters) throws UiObjectNotFoundException {
        Logger.i(TAG, "Acquiring Token..");
        UiAutomatorUtils.handleInput("com.microsoft.identity.client.testapp:id/loginHint", username);
        UiAutomatorUtils.handleButtonClick("com.microsoft.identity.client.testapp:id/btn_acquiretoken");
        final MicrosoftStsPromptHandler microsoftStsPromptHandler = new MicrosoftStsPromptHandler((MicrosoftStsPromptHandlerParameters) promptHandlerParameters);
        microsoftStsPromptHandler.handlePrompt(username, password);

        final UiObject result = UiAutomatorUtils.obtainUiObjectWithResourceId("com.microsoft.identity.client.testapp:id/txt_result");

        return result.getText();
    }


    // click on button acquire token silent
    public String acquireTokenSilent(@NonNull final String username,
                               @NonNull final String password,
                               @NonNull final PromptHandlerParameters promptHandlerParameters) throws UiObjectNotFoundException {
        Logger.i(TAG, "Acquiring Token Silent..");
        UiAutomatorUtils.handleButtonClick("com.microsoft.identity.client.testapp:id/btn_acquiretokensilent");

        final UiObject result = UiAutomatorUtils.obtainUiObjectWithResourceId("com.microsoft.identity.client.testapp:id/txt_result");

        return result.getText();
    }

    // click on button getUsers
    public String getUsers() throws UiObjectNotFoundException {
        UiAutomatorUtils.handleButtonClick("com.microsoft.identity.client.testapp:id/btn_getUsers");
        final UiObject result = UiAutomatorUtils.obtainUiObjectWithResourceId("com.microsoft.identity.client.testapp:id/txt_result");
        return result.getText();
    }

    // validate acquired token
    public Map<String, ?> validateToken(String resultToken) throws ServiceException {
        Logger.i(TAG, "Confirming Valid Result..");
        Map<String, ?> tokens = IDToken.parseJWT(resultToken);
        Assert.assertNotNull(tokens);
        return tokens;
    }

    @Override
    protected void initialiseAppImpl() {

    }

    @Override
    public void handleFirstRun() {
        UiAutomatorUtils.handleButtonClick("com.microsoft.identity.client.testapp:id/btnStartTask");
    }

}
