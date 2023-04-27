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
package com.microsoft.identity.client.ui.automation.broker;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.ui.automation.BuildConfig;
import com.microsoft.identity.client.ui.automation.TestContext;
import com.microsoft.identity.client.ui.automation.app.App;
import com.microsoft.identity.client.ui.automation.installer.IAppInstaller;
import com.microsoft.identity.client.ui.automation.installer.LocalApkInstaller;
import com.microsoft.identity.client.ui.automation.installer.PlayStore;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AdfsLoginComponentHandler;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AdfsPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.FIND_UI_ELEMENT_TIMEOUT;
import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.getResourceId;

/**
 * A model for interacting with a Broker App during UI Test.
 */
public abstract class AbstractTestBroker extends App implements ITestBroker {

    private final static String TAG = AbstractTestBroker.class.getSimpleName();
    public final static IAppInstaller DEFAULT_BROKER_APP_INSTALLER = BuildConfig.INSTALL_SOURCE_LOCAL_APK
            .equalsIgnoreCase(BuildConfig.BROKER_INSTALL_SOURCE)
            ? new LocalApkInstaller() : new PlayStore();
    public final static IAppInstaller DEFAULT_BROKER_APP_UPDATE_INSTALLER = BuildConfig.UPDATE_SOURCE_LOCAL_APK
            .equalsIgnoreCase(BuildConfig.BROKER_UPDATE_SOURCE)
            ? new LocalApkInstaller() : new PlayStore();

    @Override
    public void uninstall() {
        super.uninstall();

        if (isInstalled()) {
            // The broker app will still be installed on the device if it is enabled
            // as a device admin. In this case, we need to disable the admin and then
            // uninstall.
            Logger.w(TAG, "Unable to uninstall broker " + getAppName() + " from device..." +
                    "the broker is potentially enabled as an active device admin.");
            Logger.i(TAG, "Disabling admin for " + getAppName());
            TestContext.getTestContext().getTestDevice().getSettings().disableAdmin(getAdminName());
            Logger.i(TAG, "Reattempting uninstall for " + getAppName());
            super.uninstall();
        }
    }

    public AbstractTestBroker(@NonNull final String packageName,
                              @NonNull final String appName) {
        super(packageName, appName, DEFAULT_BROKER_APP_INSTALLER, DEFAULT_BROKER_APP_UPDATE_INSTALLER);
    }

    public AbstractTestBroker(@NonNull final String packageName,
                              @NonNull final String appName,
                              @NonNull final IAppInstaller appInstaller) {
        super(packageName, appName, appInstaller, DEFAULT_BROKER_APP_UPDATE_INSTALLER);
    }

    public AbstractTestBroker(@NonNull final String packageName,
                              @NonNull final String appName,
                              @NonNull final IAppInstaller appInstaller,
                              @NonNull final IAppInstaller updateAppInstaller) {
        super(packageName, appName, appInstaller, updateAppInstaller);
    }

    @Override
    public void handleAccountPicker(@Nullable final String username) {
        Logger.i(TAG, "Pick account associated with given username, otherwise choose \"Use Another account\"..");
        final UiDevice device = UiDevice.getInstance(getInstrumentation());

        // find the object associated to this username in account picker.
        // if the username is not provided, then click on the "Use another account" option
        final UiObject accountSelected = device.findObject(new UiSelector().resourceId(
                getResourceId(getPackageName(), "account_chooser_listView")
        ).childSelector(new UiSelector().textContains(
                // This String is pulled from
                // R.string.broker_account_chooser_choose_another_account
                TextUtils.isEmpty(username) ? "Use another account" : username
        )));

        try {
            accountSelected.waitForExists(FIND_UI_ELEMENT_TIMEOUT);
            accountSelected.click();
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void performJoinViaJoinActivity(@NonNull final String username,
                                           @NonNull final String password, final boolean isFederatedUser) {
        Logger.i(TAG, "Perform Join Via Join Activity for the given account..");
        // Enter username
        UiAutomatorUtils.handleInput(
                CommonUtils.getResourceId(
                        getPackageName(), "UsernameET"
                ),
                username
        );

        // Click Join
        UiAutomatorUtils.handleButtonClick(
                CommonUtils.getResourceId(
                        getPackageName(), "JoinButton"
                )
        );

        if (isFederatedUser) {
            final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                    .prompt(PromptParameter.LOGIN)
                    .consentPageExpected(false)
                    .expectingLoginPageAccountPicker(false)
                    .sessionExpected(false)
                    .loginHint(null)
                    .build();

            final AdfsPromptHandler adfsPromptHandler = new AdfsPromptHandler(promptHandlerParameters);
            Logger.i(TAG, "Handle prompt of ADFS login page for Device Registration..");
            // handle ADFS login page
            adfsPromptHandler.handlePrompt(username, password);
        } else {
            final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                    .broker(this)
                    .prompt(PromptParameter.SELECT_ACCOUNT)
                    .loginHint(username)
                    .sessionExpected(false)
                    .build();

            final AadPromptHandler aadPromptHandler = new AadPromptHandler(promptHandlerParameters);

            Logger.i(TAG, "Handle prompt in AAD login page for Join Via Join Activity..");
            // Handle prompt in AAD login page
            aadPromptHandler.handlePrompt(username, password);
        }
    }

    @Override
    public void performJoinViaJoinActivity(@NonNull final String username,
                                           @NonNull final String password) {
        performJoinViaJoinActivity(username, password, false);
    }

    @Override
    public void confirmJoinInJoinActivity(@NonNull final String username) {
        Logger.i(TAG, "Confirm Join Via Join Activity for the given account..");
        final UiObject joinConfirmation = UiAutomatorUtils.obtainUiObjectWithText(
                "Workplace Joined to " + username
        );

        Assert.assertTrue(joinConfirmation.exists());

        UiAutomatorUtils.handleButtonClick(getResourceId(
                getPackageName(),
                "JoinButton"
        ));
    }

    @Override
    public void overwriteFlights(@NonNull final String flightsJson) {
        // Default implementation, Do nothing.
    }

    @Override
    public void setFlights(@NonNull final String flightKey, @NonNull final String flightValue) {
        // Default implementation, Do nothing.
    }

    @Override
    public String getFlights() {
        // Default implementation, Do nothing.
        return "";
    }
}
