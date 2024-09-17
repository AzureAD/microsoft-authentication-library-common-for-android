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

import com.microsoft.identity.client.ui.automation.BuildConfig;
import com.microsoft.identity.client.ui.automation.installer.IAppInstaller;
import com.microsoft.identity.client.ui.automation.installer.LocalApkInstaller;
import com.microsoft.identity.client.ui.automation.installer.PlayStore;
import com.microsoft.identity.client.ui.automation.interaction.FirstPartyAppPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

/**
 * A model for interacting with the OneDrive Android App during UI Test.
 */
public class OneDriveApp extends App implements IFirstPartyApp {

    private final static String TAG = OneDriveApp.class.getSimpleName();
    public static final String ONEDRIVE_PACKAGE_NAME = "com.microsoft.skydrive";
    public static final String ONEDRIVE_APP_NAME = "Microsoft OneDrive";
    public final static String ONEDRIVE_APK = "OneDrive.apk";
    public final static IAppInstaller DEFAULT_WORD_APP_INSTALLER = BuildConfig.INSTALL_SOURCE_LOCAL_APK
            .equalsIgnoreCase(BuildConfig.WORD_APP_INSTALL_SOURCE)
            ? new LocalApkInstaller() : new PlayStore();

    public OneDriveApp() {
        super(ONEDRIVE_PACKAGE_NAME, ONEDRIVE_APP_NAME, DEFAULT_WORD_APP_INSTALLER);
        localApkFileName = ONEDRIVE_APK;
    }

    public OneDriveApp(@NonNull final IAppInstaller appInstaller) {
        super(ONEDRIVE_PACKAGE_NAME, ONEDRIVE_APP_NAME, appInstaller);
        localApkFileName = ONEDRIVE_APK;
    }

    @Override
    protected void initialiseAppImpl() {
        // Not yet implemented
    }

    @Override
    public void handleFirstRun() {
        // Not yet implemented
    }

    @Override
    public void addFirstAccount(@NonNull String username, @NonNull String password, @NonNull FirstPartyAppPromptHandlerParameters promptHandlerParameters) {
        // Not yet implemented
    }

    @Override
    public void addAnotherAccount(@NonNull String username, @NonNull String password, @NonNull FirstPartyAppPromptHandlerParameters promptHandlerParameters) {
        // Not yet implemented
    }

    @Override
    public void onAccountAdded() {
        // Not yet implemented
    }

    @Override
    public void confirmAccount(@NonNull String username) {
        // Not yet implemented
    }

    /**
     * Check that OneDrive has an option for phone sign-up
     * @return true if the option is available, false otherwise
     */
    public boolean checkPhoneSignUpIsAvailable() {
        launch();

        Logger.i(TAG, "Checking that sign-up through phone number is available in OneDrive...");

        // Check for "phone" UI option, we can conclude phone option is available
        return UiAutomatorUtils.obtainUiObjectWithText("phone").exists();
    }
}
