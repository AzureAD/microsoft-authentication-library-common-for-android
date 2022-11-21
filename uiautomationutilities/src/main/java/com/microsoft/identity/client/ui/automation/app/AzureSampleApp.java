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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;

import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.browser.IBrowser;
import com.microsoft.identity.client.ui.automation.installer.LocalApkInstaller;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

import java.util.concurrent.TimeUnit;

/**
 * A model for interacting with the Azure Sample App for MSAL Android during UI Test.
 * This refers to app stored in Azure-Samples/ms-identity-android-java repository.
 * See this: https://github.com/Azure-Samples/ms-identity-android-java.
 */
public class AzureSampleApp extends App {

    private final static String TAG = AzureSampleApp.class.getSimpleName();
    private static final String AZURE_SAMPLE_PACKAGE_NAME = "com.azuresamples.msalandroidapp";
    private static final String AZURE_SAMPLE_APP_NAME = "Azure Sample";
    public final static String AZURE_SAMPLE_APK = "AzureSample.apk";
    public final static String OLD_AZURE_SAMPLE_APK = "OldAzureSample.apk";

    public AzureSampleApp() {
        super(AZURE_SAMPLE_PACKAGE_NAME, AZURE_SAMPLE_APP_NAME, new LocalApkInstaller());
        localApkFileName = AZURE_SAMPLE_APK;
    }

    public AzureSampleApp(@NonNull final String azureSampleApk,
                                        @NonNull final String updateAzureSampleApk) {
        super(AZURE_SAMPLE_PACKAGE_NAME, AZURE_SAMPLE_APP_NAME, new LocalApkInstaller());
        localApkFileName = azureSampleApk;
        localUpdateApkFileName = updateAzureSampleApk;
    }

    @Override
    public void handleFirstRun() {
        // nothing required
    }

    @Override
    public void initialiseAppImpl() {
       // nothing required
    }

    /**
     * Sign in into the Azure Sample App. Please note that this method performs sign in into the
     * Single Account Mode Fragment in the Sample App.
     *
     * @param username                    the username of the account to sign in
     * @param password                    the password of the account to sign in
     * @param browser                     the browser that is expected to be used during sign in flow
     * @param shouldHandleBrowserFirstRun whether this is the first time the browser being run
     * @param promptHandlerParameters     the prompt handler parameters indicating how to handle prompt
     */
    public void signInWithSingleAccountFragment(@NonNull final String username,
                                                @NonNull final String password,
                                                @Nullable final IBrowser browser,
                                                final boolean shouldHandleBrowserFirstRun,
                                                @NonNull final MicrosoftStsPromptHandlerParameters promptHandlerParameters) {
        Logger.i(TAG, "Signing in into Azure Sample App with Single Account Mode Fragment..");
        // Click Sign In in Single Account Fragment
        UiAutomatorUtils.handleButtonClick("com.azuresamples.msalandroidapp:id/btn_signIn");

        if (promptHandlerParameters.getBroker() == null && browser != null && shouldHandleBrowserFirstRun) {
            // handle browser first run as applicable
            ((IApp) browser).handleFirstRun();
        }

        Logger.i(TAG, "Handle AAD Login page prompt..");
        // handle prompt in AAD login page
        final MicrosoftStsPromptHandler microsoftStsPromptHandler =
                new MicrosoftStsPromptHandler(promptHandlerParameters);

        microsoftStsPromptHandler.handlePrompt(username, password);

        // sleep as it can take a bit for UPN to appear in Azure Sample app
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(5));
        } catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    /**
     * Sign in into the Azure Sample App. Please note that this method performs sign in into the
     * Single Account Mode Fragment in the Sample App.
     *
     * @param browser                     the browser that is expected to be used during sign in flow
     * @param broker                      the broker used in the test case
     * @param shouldHandleBrowserFirstRun whether this is the first time the browser being run
     */
    public void signInSilentlyWithSingleAccountFragment(@Nullable final IBrowser browser,
                                                        @NonNull final ITestBroker broker,
                                                        final boolean shouldHandleBrowserFirstRun) {
        Logger.i(TAG, "Signing in into Azure Sample App with Single Account Mode Fragment..");
        // Click Sign In in Single Account Fragment
        UiAutomatorUtils.handleButtonClick("com.azuresamples.msalandroidapp:id/btn_signIn");

        if (broker == null && browser != null && shouldHandleBrowserFirstRun) {
            // handle browser first run as applicable
            ((IApp) browser).handleFirstRun();
        }

        // sleep as it can take a bit for UPN to appear in Azure Sample app
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(5));
        } catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    /**
     * Sign out of the Azure Sample App. Please note that this method performs sign out of the
     * Single Account mode fragment in the Azure Sample App.
     */
    public void signOutFromSingleAccountFragment() {
        Logger.i(TAG, "Signing out of Azure Sample App from Single Account Mode Fragment..");
        UiAutomatorUtils.handleButtonClick("com.azuresamples.msalandroidapp:id/btn_removeAccount");
    }

    /**
     * Makes sure that the provided username is already signed into the Azure Sample App.
     *
     * @param username the username of the account for which to confirm sign in
     */
    public void confirmSignedIn(@NonNull final String username) {
        Logger.i(TAG, "Confirming account with supplied username is signed in..");
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(5));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        final UiObject signedInUser = UiAutomatorUtils.obtainUiObjectWithResourceId("com.azuresamples.msalandroidapp:id/current_user");
        try {
            Assert.assertEquals("User is signed into Azure Sample App", username, signedInUser.getText());
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }
}
