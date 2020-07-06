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
package com.microsoft.identity.client.ui.automation.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.ui.automation.app.App;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;

import org.junit.Assert;

import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.FIND_UI_ELEMENT_TIMEOUT;
import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.getResourceId;

public class SettingsUtils {

    final static String SETTINGS_PKG = "com.android.settings";

    private static void launchIntent(final String action) {
        final Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(action);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void launchDeviceAdminSettingsPage() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(
                SETTINGS_PKG,
                SETTINGS_PKG + ".DeviceAdminSettings")
        );
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final Context context = ApplicationProvider.getApplicationContext();
        context.startActivity(intent);

        final UiObject deviceAdminPage = UiAutomatorUtils.obtainUiObjectWithText("device admin");
        Assert.assertTrue(deviceAdminPage.exists());
    }

    public static void disableAdmin(final String adminName) {
        launchDeviceAdminSettingsPage();

        try {
            // scroll down the recycler view to find the list item for this admin
            final UiObject adminAppListItem = UiAutomatorUtils.obtainChildInScrollable(
                    "android:id/list",
                    adminName
            );

            assert adminAppListItem != null;
            adminAppListItem.click();

            // scroll down the recycler view to find btn to deactivate admin
            final UiObject deactivateBtn = UiAutomatorUtils.obtainChildInScrollable(
                    android.widget.ScrollView.class,
                    "Deactivate this device admin app"
            );

            deactivateBtn.click();

            UiAutomatorUtils.handleButtonClick("android:id/button1");
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }

    }

    public static void launchAddAccountPage() {
        launchIntent(Settings.ACTION_ADD_ACCOUNT);
    }

    public static void addWorkAccount(final ITestBroker expectedBroker,
                                      final String username,
                                      final String password) {
        launchAddAccountPage();

        try {
            // scroll down the recycler view to find the list item for this account type
            final UiObject workAccount = UiAutomatorUtils.obtainChildInScrollable(
                    "com.android.settings:id/list",
                    "Work account"
            );

            workAccount.click();

            UiAutomatorUtils.handleInput(
                    CommonUtils.getResourceId(
                            ((App) expectedBroker).getPackageName(), "UsernameET"
                    ),
                    username
            );

            UiAutomatorUtils.handleButtonClick(
                    CommonUtils.getResourceId(
                            ((App) expectedBroker).getPackageName(), "JoinButton"
                    )
            );

            final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                    .broker(expectedBroker)
                    .prompt(PromptParameter.SELECT_ACCOUNT)
                    .loginHintProvided(true)
                    .sessionExpected(false)
                    .build();

            final AadPromptHandler aadPromptHandler = new AadPromptHandler(promptHandlerParameters);

            aadPromptHandler.handlePrompt(username, password);

            final UiDevice device =
                    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

            UiObject certInstaller = device.findObject(new UiSelector().packageName("com.android.certinstaller"));
            certInstaller.waitForExists(FIND_UI_ELEMENT_TIMEOUT);
            Assert.assertTrue(certInstaller.exists());

            UiAutomatorUtils.handleButtonClick("android:id/button1");

            final UiObject joinConfirmation = UiAutomatorUtils.obtainUiObjectWithText(
                    "Workplace Joined to " + username
            );

            Assert.assertTrue(joinConfirmation.exists());

            UiAutomatorUtils.handleButtonClick(getResourceId(
                    ((App) expectedBroker).getPackageName(),
                    "JoinButton"
            ));
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
    }

    public static void launchDateTimeSettingsPage() {
        final Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(Settings.ACTION_DATE_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

}
