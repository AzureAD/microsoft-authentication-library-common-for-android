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

import android.Manifest;
import android.os.Build;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.ui.automation.installer.IAppInstaller;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AdfsPromptHandler;
import com.microsoft.identity.client.ui.automation.powerlift.IPowerLiftIntegratedApp;
import com.microsoft.identity.client.ui.automation.constants.DeviceAdmin;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

import lombok.Getter;

import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.FIND_UI_ELEMENT_TIMEOUT;

/**
 * A model for interacting with the Microsoft Authenticator Broker App during UI Test.
 */
@Getter
public class BrokerMicrosoftAuthenticator extends AbstractTestBroker implements ITestBroker, IPowerLiftIntegratedApp {

    public final static String AUTHENTICATOR_APP_PACKAGE_NAME = "com.azure.authenticator";
    public final static String AUTHENTICATOR_APP_NAME = "Microsoft Authenticator";
    public final static String AUTHENTICATOR_APK = "Authenticator.apk";

    private final static String INCIDENT_MSG = "Broker Automation Incident";

    public static final String TAG = BrokerMicrosoftAuthenticator.class.getSimpleName();

    private boolean isInSharedDeviceMode = false;

    public BrokerMicrosoftAuthenticator() {
        super(AUTHENTICATOR_APP_PACKAGE_NAME, AUTHENTICATOR_APP_NAME);
        localApkFileName = AUTHENTICATOR_APK;
    }

    public BrokerMicrosoftAuthenticator(@NonNull final IAppInstaller appInstaller) {
        super(AUTHENTICATOR_APP_PACKAGE_NAME, AUTHENTICATOR_APP_NAME, appInstaller);
        localApkFileName = AUTHENTICATOR_APK;
    }

    @Override
    public void performDeviceRegistration(@NonNull final String username,
                                          @NonNull final String password) {
        performDeviceRegistration(username, password, false);
    }

    @Override
    public void performDeviceRegistration(String username, String password, boolean isFederatedUser) {
        Logger.i(TAG, "Performing Device Registration for the given account..");
        performDeviceRegistrationHelper(
                username,
                password,
                "com.azure.authenticator:id/email_input",
                "com.azure.authenticator:id/register_button",
                isFederatedUser
        );


        try {
            // after device registration, make sure that we see the unregister btn to confirm successful
            // registration
            final UiObject unRegisterBtn = UiAutomatorUtils.obtainUiObjectWithResourceId(
                    "com.azure.authenticator:id/unregister_button"
            );
            Assert.assertTrue(
                    "Microsoft Authenticator - Unregister Button appears.",
                    unRegisterBtn.exists()
            );

            Assert.assertTrue(
                    "Microsoft Authenticator - Unregister Button is clickable.",
                    unRegisterBtn.isClickable()
            );

            // after device registration, make sure that the current registration upn matches with
            // with what was passed in
            final UiObject currentRegistration = UiAutomatorUtils.obtainUiObjectWithResourceId(
                    "com.azure.authenticator:id/current_registered_email"
            );

            Assert.assertTrue(
                    "Microsoft Authenticator - Registered account info appears.",
                    currentRegistration.exists()
            );

            Assert.assertTrue(
                    "Microsoft Authenticator - Registered account upn matches provided upn.",
                    currentRegistration.getText().equalsIgnoreCase(username)
            );
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void performSharedDeviceRegistration(@NonNull final String username,
                                                @NonNull final String password) {
        Logger.i(TAG, "Performing Shared Device Registration for the given account..");
        performDeviceRegistrationHelper(
                username,
                password,
                "com.azure.authenticator:id/shared_device_registration_email_input",
                "com.azure.authenticator:id/shared_device_registration_button",
                false
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

    @Nullable
    @Override
    public String obtainDeviceId() {
        Logger.i(TAG, "Obtain Device Id..");
        openDeviceRegistrationPage();

        try {
            final UiObject deviceIdElement = UiAutomatorUtils.obtainUiObjectWithResourceId(
                    "com.azure.authenticator:id/device_id_text"
            );

            final String deviceIdText = deviceIdElement.getText();
            final int colonIndex = deviceIdText.indexOf(":");
            return deviceIdText.substring(colonIndex + 1);
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void enableBrowserAccess() {
        Logger.i(TAG, "Enable Browser Access..");
        // open device registration page
        openDeviceRegistrationPage();

        // Click enable browser access
        UiAutomatorUtils.handleButtonClick(
                "com.azure.authenticator:id/enable_browser_access_button"
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

    @Override
    public String createPowerLiftIncident() {
        Logger.i(TAG, "Creating Power Lift Incident..");
        launch();
        if (shouldHandleFirstRun) {
            handleFirstRun();
        }

        if (isInSharedDeviceMode) {
            return createPowerLiftIncidentInSharedDeviceMode();
        } else {
            return createPowerLiftIncidentInNonSharedMode();
        }
    }

    private String createPowerLiftIncidentInNonSharedMode() {
        // click the 3 dot menu icon in top right
        UiAutomatorUtils.handleButtonClick("com.azure.authenticator:id/menu_overflow");

        try {
            // select Help from drop down
            final UiObject settings = UiAutomatorUtils.obtainUiObjectWithText("Send Feedback");
            settings.click();

            final UiObject sendLogs = UiAutomatorUtils.obtainUiObjectWithClassAndDescription(
                    Button.class,
                    "Having trouble?Report it"
            );

            Assert.assertTrue(sendLogs.exists());

            // click the send logs button
            sendLogs.click();

            UiAutomatorUtils.handleButtonClickForObjectWithText("Select an option");

            UiAutomatorUtils.handleButtonClickForObjectWithText("Other");

            final UiObject describeIssueBox = UiAutomatorUtils.obtainUiObjectWithTextAndClassType(
                    "Please don't include your name, phone number, or other personal information.",
                    EditText.class
            );

            describeIssueBox.setText(INCIDENT_MSG);

            final UiObject sendBtn = UiAutomatorUtils.obtainUiObjectWithDescription("Send feedback");
            sendBtn.click();

            final UiObject postLogSubmissionMsg = UiAutomatorUtils.obtainUiObjectWithResourceId(
                    "android:id/parentPanel"
            );

            Assert.assertTrue(postLogSubmissionMsg.exists());

            final UiObject incidentDetails = UiAutomatorUtils.obtainUiObjectWithResourceId("android:id/message");
            Assert.assertTrue(incidentDetails.exists());

            final String incidentIdText = incidentDetails.getText();

            // This will post the incident id in text logs
            Logger.w(TAG, incidentIdText);

            return incidentIdText;
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    private String createPowerLiftIncidentInSharedDeviceMode() {
        try {
            final UiObject settingsBtn = UiAutomatorUtils.obtainUiObjectWithClassAndDescription(
                    Button.class,
                    "Settings"
            );
            settingsBtn.click();

            UiAutomatorUtils.handleButtonClickForObjectWithText("Send logs");
            UiAutomatorUtils.handleInput(
                    "com.azure.authenticator:id/send_feedback_message_input", INCIDENT_MSG

            );
            UiAutomatorUtils.handleButtonClick("com.azure.authenticator:id/send_feedback_button");
            final UiObject postLogSubmissionText = UiAutomatorUtils.obtainUiObjectWithResourceId(
                    "com.azure.authenticator:id/send_feedback_result"
            );

            Assert.assertTrue(postLogSubmissionText.exists());

            final String incidentIdText = postLogSubmissionText.getText();
            // This will post the incident id in text logs
            Logger.w(TAG, incidentIdText);

            return incidentIdText;
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public DeviceAdmin getAdminName() {
        Logger.i(TAG, "Get Admin Name..");
        return DeviceAdmin.MICROSOFT_AUTHENTICATOR;
    }

    /**
     * Open the device registration page in the Authenticator App
     */
    public void openDeviceRegistrationPage() {
        Logger.i(TAG, "Open the device registration page in the Authenticator App..");
        launch(); // launch Authenticator app

        if (shouldHandleFirstRun) {
            handleFirstRun(); // handle first run experience
        }

        // click the 3 dot menu icon in top right
        UiAutomatorUtils.handleButtonClick("com.azure.authenticator:id/menu_overflow");

        try {
            // select Settings from drop down
            final UiObject settings = UiAutomatorUtils.obtainUiObjectWithText("Settings");
            settings.click();

            // scroll down the recycler view to find device registration btn
            final UiObject deviceRegistration = UiAutomatorUtils.obtainChildInScrollable(
                    "com.azure.authenticator:id/recycler_view",
                    "Device registration"
            );

            assert deviceRegistration != null;

            // click the device registration button
            deviceRegistration.click();

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                // grant the GET ACCOUNTS permission if needed
                grantPermission(Manifest.permission.GET_ACCOUNTS);
            }
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    private void performDeviceRegistrationHelper(@NonNull final String username,
                                                 @NonNull final String password,
                                                 @NonNull final String emailInputResourceId,
                                                 @NonNull final String registerBtnResourceId,
                                                 final boolean isFederatedUser) {
        Logger.i(TAG, "Execution of Helper for Device Registration..");
        // open device registration page
        openDeviceRegistrationPage();

        // enter email
        UiAutomatorUtils.handleInput(
                emailInputResourceId,
                username
        );

        // click register
        UiAutomatorUtils.handleButtonClick(registerBtnResourceId);

        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                .prompt(PromptParameter.LOGIN)
                .broker(this)
                .consentPageExpected(false)
                .expectingBrokerAccountChooserActivity(false)
                .expectingLoginPageAccountPicker(false)
                .sessionExpected(false)
                .loginHint(username)
                .build();

        if (isFederatedUser) {
            final AdfsPromptHandler adfsPromptHandler = new AdfsPromptHandler(promptHandlerParameters);
            Logger.i(TAG, "Handle prompt of ADFS login page for Device Registration..");
            // handle ADFS login page
            adfsPromptHandler.handlePrompt(username, password);
        } else {
            final AadPromptHandler aadPromptHandler = new AadPromptHandler(promptHandlerParameters);

            Logger.i(TAG, "Handle AAD Login page prompt for Device Registration..");
            // handle AAD login page
            aadPromptHandler.handlePrompt(username, password);
        }
    }

    @Override
    public void handleFirstRun() {
        Logger.i(TAG, "Handle First Run of the APP..");
        // privacy dialog
        UiAutomatorUtils.handleButtonClick("com.azure.authenticator:id/privacy_consent_button");
        // the skip button
        UiAutomatorUtils.handleButtonClick("com.azure.authenticator:id/frx_skip_button");
        shouldHandleFirstRun = false;
    }
}
