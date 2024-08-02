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

import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.FIND_UI_ELEMENT_TIMEOUT;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.ui.automation.TestContext;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

import java.util.concurrent.TimeUnit;

/**
 * A model for interacting with the Microsoft Authenticator Broker App during UI Test
 * when version number of Authenticator app under test is >= "6.2206.3949"
 * contains changes for new resourceIds for device registration UI
 */
public class BrokerAuthenticatorUpdatedVersionImpl extends BrokerMicrosoftAuthenticator {

    private static final String TAG = BrokerAuthenticatorUpdatedVersionImpl.class.getSimpleName();
    public static boolean shouldUseDeviceSettingsPage = true;

    @Override
    public void performDeviceRegistration(@NonNull final String username,
                                          @NonNull final String password,
                                          final boolean isFederatedUser) {

        Logger.i(TAG, "Performing Device Registration for the given account..");
        if (shouldUseDeviceSettingsPage) {
            // TO-DO after authenticator app removes the MFA prompt during registration,
            // we can remove this flag (isExpectingMFA) or set it to false
            TestContext.getTestContext().getTestDevice().getSettings().addWorkAccount(
                    this,
                    username,
                    password,
                    isFederatedUser
            );
        }
        else {
            performDeviceRegistrationHelperWithButtonText(
                    username,
                    password,
                    "Register a new organization",
                    "Register device",
                    isFederatedUser,
                    AUTHENTICATOR_IS_REGISTER_EXPECTED
            );


            // after device registration, make sure that we see the unregister btn to confirm successful
            // registration

            // relaunch device registration page
            openDeviceRegistrationPage();

            // Click the registered account domain
            UiAutomatorUtils.handleButtonClickForObjectWithText(
                    username.split("@")[1]
            );

            final UiObject unRegisterBtn = UiAutomatorUtils.obtainUiObjectWithExactText(
                    "Unregister device", TimeUnit.SECONDS.toMillis(20)
            );
            Assert.assertTrue(
                    "Microsoft Authenticator - Unregister Button appears.",
                    unRegisterBtn.exists()
            );
        }
    }

    @Override
    public void performSharedDeviceRegistration(@NonNull final String username,
                                                @NonNull final String password) {
        Logger.i(TAG, "Performing Shared Device Registration for the given account..");
        performDeviceRegistrationHelperWithButtonText(
                username,
                password,
                "Register as shared device",
                "Register",
                false,
                AUTHENTICATOR_IS_REGISTER_EXPECTED_SHARED
        );

        final UiDevice device =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        final UiSelector sharedDeviceConfirmationSelector = new UiSelector()
                .descriptionContains("Shared Device Mode")
                .className("android.widget.ImageView");

        //confirm that we are in Shared Device Mode inside Authenticator
        final UiObject sharedDeviceConfirmation = device.findObject(sharedDeviceConfirmationSelector);
        sharedDeviceConfirmation.waitForExists(FIND_UI_ELEMENT_TIMEOUT);
        Assert.assertTrue(
                "Microsoft Authenticator - Shared Device Confirmation page appears.",
                sharedDeviceConfirmation.exists());

        isInSharedDeviceMode = true;
    }

    @Override
    public void performSharedDeviceRegistrationDontValidate(@NonNull final String username,
                                                @NonNull final String password) {
        Logger.i(TAG, "Performing Shared Device Registration for the given account without validating we are in shared device mode.");
        performDeviceRegistrationHelperWithButtonText(
                username,
                password,
                "Register as shared device",
                "Register",
                false,
                AUTHENTICATOR_IS_REGISTER_EXPECTED_SHARED
        );
    }
    
    @Override
    protected void goToDeviceRegistrationPage() {
        // scroll down the recycler view to find device registration btn
        try {
            // click the 3 dot menu icon in top right
            UiAutomatorUtils.handleButtonClick("com.azure.authenticator:id/menu_overflow");

            // select Settings from drop down
            final UiObject settings = UiAutomatorUtils.obtainUiObjectWithText("Settings");
            settings.click();

            final UiObject deviceRegistration = UiAutomatorUtils.obtainChildInScrollable(
                    "settingsScrollView",
                    "Device Registration"
            );

            assert deviceRegistration != null;

            // click the device registration button
            deviceRegistration.click();
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void enableBrowserAccess(@NonNull final String username) {
        Logger.i(TAG, "Enable Browser Access..");
        // open device registration page
        openDeviceRegistrationPage();

        // Click the registered account domain
        UiAutomatorUtils.handleButtonClickForObjectWithText(
                username.split("@")[1]
        );

        // Click enable browser access
        UiAutomatorUtils.handleButtonClickForObjectWithText(
                "Enable browser access"
        );

        // click continue in Dialog
        UiAutomatorUtils.handleButtonClick("android:id/button1");

        final UiDevice device =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Install cert
        final UiObject certInstaller = device.findObject(new UiSelector().packageName("com.android.certinstaller"));
        certInstaller.waitForExists(FIND_UI_ELEMENT_TIMEOUT);
        Assert.assertTrue(
                "Microsoft Authenticator - cert installer dialog appears.",
                certInstaller.exists()
        );

        UiAutomatorUtils.handleButtonClick("android:id/button1");
    }
}
