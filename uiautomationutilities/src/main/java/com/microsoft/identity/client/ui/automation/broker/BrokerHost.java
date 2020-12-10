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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.ui.automation.constants.DeviceAdmin;
import com.microsoft.identity.client.ui.automation.installer.LocalApkInstaller;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.FIND_UI_ELEMENT_TIMEOUT;

public class BrokerHost extends AbstractTestBroker {

    public final static String BROKER_HOST_APP_PACKAGE_NAME = "com.microsoft.identity.testuserapp";
    public final static String BROKER_HOST_APP_NAME = "Broker Host App";
    public final static String BROKER_HOST_APK = "BrokerHost.apk";

    public BrokerHost() {
        super(BROKER_HOST_APP_PACKAGE_NAME, BROKER_HOST_APP_NAME, new LocalApkInstaller());
        localApkFileName = BROKER_HOST_APK;
    }

    /**
     * allows you to install different versions of BrokerHost.
     *
     * @param appName
     */
    public BrokerHost(String appName) {
        super(BROKER_HOST_APP_PACKAGE_NAME, appName, new LocalApkInstaller());
        localApkFileName = appName;
    }

    @Override
    public void performDeviceRegistration(@NonNull final String username,
                                          @NonNull final String password) {
        performDeviceRegistrationHelper(username);

        // Click the join btn
        final UiObject joinBtn = UiAutomatorUtils.obtainUiObjectWithResourceIdAndEnabledFlag(
                CommonUtils.getResourceId(
                        getPackageName(), "buttonJoin"
                ), true
        );

        try {
            joinBtn.click();
        } catch (UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }

        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                .prompt(PromptParameter.LOGIN)
                .broker(this)
                .consentPageExpected(false)
                .expectingBrokerAccountChooserActivity(false)
                .expectingLoginPageAccountPicker(false)
                .sessionExpected(false)
                .loginHint(username)
                .build();

        final AadPromptHandler aadPromptHandler = new AadPromptHandler(promptHandlerParameters);

        // handle AAD login page
        aadPromptHandler.handlePrompt(username, password);

        postJoinConfirmHelper(username);
    }

    @Override
    public void performSharedDeviceRegistration(String username, String password) {
        performDeviceRegistrationHelper(username);

        // Click the join shared device btn
        UiObject joinBtn = UiAutomatorUtils.obtainUiObjectWithResourceIdAndEnabledFlag(
                "com.microsoft.identity.testuserapp:id/buttonJoinSharedDevice", true
        );

        try {
            joinBtn.click();
        } catch (UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }


        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                .prompt(PromptParameter.LOGIN)
                .broker(this)
                .consentPageExpected(false)
                .expectingBrokerAccountChooserActivity(false)
                .expectingLoginPageAccountPicker(false)
                .sessionExpected(false)
                .loginHint(username)
                .build();

        final AadPromptHandler aadPromptHandler = new AadPromptHandler(promptHandlerParameters);

        // handle AAD login page
        aadPromptHandler.handlePrompt(username, password);

        postJoinConfirmHelper(username);
    }

    private void performDeviceRegistrationHelper(@NonNull final String username) {
        launch(); // launch Broker Host app

        if (shouldHandleFirstRun) {
            handleFirstRun(); // handle first run experience
        }

        // enter upn in text box
        UiAutomatorUtils.handleInput(
                "com.microsoft.identity.testuserapp:id/editTextUsername",
                username
        );
    }

    private void postJoinConfirmHelper(@NonNull final String expectedUpn) {
        // Look for join op completion dialog
        final UiObject joinFinishDialog = UiAutomatorUtils.obtainUiObjectWithResourceId(
                "android:id/message"
        );

        Assert.assertTrue(joinFinishDialog.exists());

        try {
            // Obtain the text from the dialog box
            final String joinFinishDialogText = joinFinishDialog.getText();
            final String joinStatus = joinFinishDialogText.split(":")[1];
            // The status should be successful
            Assert.assertTrue("SUCCESSFUL".equalsIgnoreCase(joinStatus));

            // dismiss the dialog
            UiAutomatorUtils.handleButtonClick("android:id/button1");

            // compare the UPN to make sure joined with the expected account
            final String joinedUpn = getAccountUpn();
            Assert.assertTrue(expectedUpn.equalsIgnoreCase(joinedUpn));
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @Nullable
    @Override
    public String obtainDeviceId() {
        launch(); // launch Broker Host app

        if (shouldHandleFirstRun) {
            handleFirstRun(); // handle first run experience
        }

        UiAutomatorUtils.handleButtonClick("com.microsoft.identity.testuserapp:id/buttonDeviceId");

        // Look for the device id dialog box
        final UiObject deviceIdDialog = UiAutomatorUtils.obtainUiObjectWithResourceId(
                "android:id/message"
        );

        Assert.assertTrue(deviceIdDialog.exists());

        try {
            // get the text on the device id dialog box
            final String[] deviceIdDialogText = deviceIdDialog.getText().split(":");
            // look for the device id if present
            if (deviceIdDialogText[0].equalsIgnoreCase("DeviceId")) {
                return deviceIdDialogText[1];
            } else {
                return null;
            }
        } catch (UiObjectNotFoundException e) {
            throw new AssertionError(e);
        } finally {
            // dismiss the dialog
            UiAutomatorUtils.handleButtonClick("android:id/button1");
        }
    }

    @Override
    public void enableBrowserAccess() {
        launch();

        if (shouldHandleFirstRun) {
            handleFirstRun();
        }

        // Click enable browser access
        UiAutomatorUtils.handleButtonClick(
                "com.microsoft.identity.testuserapp:id/buttonInstallCert"
        );

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

    @Override
    public DeviceAdmin getAdminName() {
        return DeviceAdmin.BROKER_HOST;
    }

    @Override
    public void handleFirstRun() {
        // nothing needed here
    }

    @Nullable
    public String getAccountUpn() {
        launch(); // launch Broker Host app

        if (shouldHandleFirstRun) {
            handleFirstRun(); // handle first run experience
        }

        UiAutomatorUtils.handleButtonClick("com.microsoft.identity.testuserapp:id/buttonGetWpjUpn");

        // Look for the UPN dialog box
        final UiObject showUpnDialog = UiAutomatorUtils.obtainUiObjectWithResourceId(
                "android:id/message"
        );

        Assert.assertTrue(showUpnDialog.exists());

        try {
            // Obtain the text on the UPN dialog box
            final String[] upnDialogTextParts = showUpnDialog.getText().split(":");

            // get the UPN if it is there, else return null (in case of error)
            if ("UPN".equalsIgnoreCase(upnDialogTextParts[0])) {
                return upnDialogTextParts[1];
            } else {
                return null;
            }
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        } finally {
            // dismiss dialog
            UiAutomatorUtils.handleButtonClick("android:id/button1");
        }
    }
}
